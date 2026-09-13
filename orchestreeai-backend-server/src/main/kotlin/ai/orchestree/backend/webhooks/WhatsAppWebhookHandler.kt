package ai.orchestree.backend.webhooks

import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.channels.ChannelGateway
import ai.orchestree.backend.channels.InboundMessage
import ai.orchestree.backend.channels.OutboundMessage
import ai.orchestree.backend.conversation.SalesIntentClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class WebhookExecutionResult(
    val verified: Boolean,
    val senderId: String? = null,
    val senderName: String? = null,
    val messageText: String? = null,
    val intent: String? = null,
    val persona: String? = null,
    val replyText: String? = null,
    val conversationId: String? = null,
    val persisted: Boolean = false,
    val errorMessage: String? = null
)

class WhatsAppWebhookHandler(
    private val validator: WebhookSignatureValidator = WebhookSignatureValidator(),
    private val channelGateway: ChannelGateway = ChannelGateway()
) {
    private val logger = LoggerFactory.getLogger(WhatsAppWebhookHandler::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    fun handlePayload(payload: String, signatureHeader: String?, appSecret: String): Boolean {
        val isValid = validator.verifyMetaHmacSha256(
            payload = payload,
            appSecret = appSecret,
            hubSignatureHeader = signatureHeader
        )

        if (!isValid) {
            logger.warn("WhatsApp/Meta webhook rejected: invalid X-Hub-Signature-256")
            return false
        }

        logger.info("WhatsApp/Meta webhook signature verified successfully")
        return true
    }

    suspend fun processInboundWebhook(
        payload: String,
        signatureHeader: String?,
        appSecret: String,
        defaultTenantId: String = "tenant-default"
    ): WebhookExecutionResult = withContext(Dispatchers.IO) {
        val isValid = handlePayload(payload, signatureHeader, appSecret)
        if (!isValid) {
            return@withContext WebhookExecutionResult(
                verified = false,
                errorMessage = "Invalid WhatsApp signature X-Hub-Signature-256"
            )
        }

        // Parse payload (Supports Meta Cloud API JSON or simplified Direct JSON)
        var senderId = "628111222333"
        var senderName = "WhatsApp User"
        var messageText = ""
        var tenantId = defaultTenantId

        try {
            val root = json.parseToJsonElement(payload).jsonObject
            // Case 1: Direct JSON format
            if (root.containsKey("messageText") || root.containsKey("message")) {
                messageText = root["messageText"]?.jsonPrimitive?.content
                    ?: root["message"]?.jsonPrimitive?.content ?: ""
                senderId = root["senderId"]?.jsonPrimitive?.content ?: senderId
                senderName = root["senderName"]?.jsonPrimitive?.content ?: senderName
                tenantId = root["tenantId"]?.jsonPrimitive?.content ?: tenantId
            }
            // Case 2: Meta Cloud API webhook format
            else if (root.containsKey("entry")) {
                val entryArray = root["entry"]?.jsonArray
                val firstEntry = entryArray?.firstOrNull()?.jsonObject
                val changesArray = firstEntry?.get("changes")?.jsonArray
                val firstChange = changesArray?.firstOrNull()?.jsonObject
                val value = firstChange?.get("value")?.jsonObject

                val contacts = value?.get("contacts")?.jsonArray
                val firstContact = contacts?.firstOrNull()?.jsonObject
                val profile = firstContact?.get("profile")?.jsonObject
                senderName = profile?.get("name")?.jsonPrimitive?.content ?: senderName

                val messages = value?.get("messages")?.jsonArray
                val firstMsg = messages?.firstOrNull()?.jsonObject
                senderId = firstMsg?.get("from")?.jsonPrimitive?.content ?: senderId
                val textObj = firstMsg?.get("text")?.jsonObject
                messageText = textObj?.get("body")?.jsonPrimitive?.content ?: ""
            }
        } catch (e: Exception) {
            logger.warn("Could not parse WhatsApp webhook JSON payload: ${e.message}")
            messageText = payload.take(200)
        }

        if (messageText.isBlank()) {
            return@withContext WebhookExecutionResult(
                verified = true,
                senderId = senderId,
                senderName = senderName,
                messageText = "",
                replyText = "Empty message received"
            )
        }

        // 1. Classify intent
        val classifiedIntent = SalesIntentClassifier.classify(messageText)

        // 2. Delegate to ChannelGateway (calls SalesPersonaEngine and ModelRouter with fallback chain)
        val inbound = InboundMessage(
            tenantId = tenantId,
            channelType = "WHATSAPP",
            senderId = senderId,
            text = messageText,
            metadata = mapOf("senderName" to senderName)
        )
        val outbound = channelGateway.processInbound(inbound)
        val replyText = outbound?.text ?: "Terima kasih atas pesan Anda. Tim kami akan segera menindaklanjuti."

        // 3. Persist to conversations and conversation_messages tables
        val convId = "conv-wa-${senderId.filter { it.isLetterOrDigit() }.take(16)}"
        var persisted = false

        try {
            val conn = DatabaseManager.getConnection()
            conn?.use { c ->
                c.autoCommit = false
                try {
                    // 3.1 Upsert conversation
                    c.prepareStatement("""
                        INSERT INTO conversations (
                            id, tenant_id, channel_account_id, customer_id, customer_channel_identifier,
                            channel_type, assigned_persona, status, last_message_snippet, current_intent,
                            sales_stage, last_activity_at, created_at
                        ) VALUES (?, ?, 'ca-wa-default', 'cust-wa-01', ?, 'WHATSAPP', 'RECEPTIONIST', 'OPEN', ?, ?, 'INQUIRY', NOW(), NOW())
                        ON CONFLICT (id) DO UPDATE SET
                            last_message_snippet = EXCLUDED.last_message_snippet,
                            current_intent = EXCLUDED.current_intent,
                            last_activity_at = NOW()
                    """.trimIndent()).use { psConv ->
                        psConv.setString(1, convId)
                        psConv.setString(2, tenantId)
                        psConv.setString(3, senderId)
                        psConv.setString(4, replyText.take(120))
                        psConv.setString(5, classifiedIntent.intent.name)
                        psConv.executeUpdate()
                    }

                    // 3.2 Insert inbound customer message
                    val inMsgId = "msg-in-${UUID.randomUUID().toString().take(8)}"
                    c.prepareStatement("""
                        INSERT INTO conversation_messages (
                            id, conversation_id, tenant_id, sender_type, sender_id, sender_name, message_text, message_type, intent_detected, created_at
                        ) VALUES (?, ?, ?, 'CUSTOMER', ?, ?, ?, 'TEXT', ?, NOW())
                    """.trimIndent()).use { psInMsg ->
                        psInMsg.setString(1, inMsgId)
                        psInMsg.setString(2, convId)
                        psInMsg.setString(3, tenantId)
                        psInMsg.setString(4, senderId)
                        psInMsg.setString(5, senderName)
                        psInMsg.setString(6, messageText)
                        psInMsg.setString(7, classifiedIntent.intent.name)
                        psInMsg.executeUpdate()
                    }

                    // 3.3 Insert outbound AI agent message
                    val outMsgId = "msg-out-${UUID.randomUUID().toString().take(8)}"
                    c.prepareStatement("""
                        INSERT INTO conversation_messages (
                            id, conversation_id, tenant_id, sender_type, sender_id, sender_name, message_text, message_type, intent_detected, created_at
                        ) VALUES (?, ?, ?, 'AI_AGENT', 'agent-sales', 'AI Sales Assistant', ?, 'TEXT', ?, NOW())
                    """.trimIndent()).use { psOutMsg ->
                        psOutMsg.setString(1, outMsgId)
                        psOutMsg.setString(2, convId)
                        psOutMsg.setString(3, tenantId)
                        psOutMsg.setString(4, replyText)
                        psOutMsg.setString(5, classifiedIntent.intent.name)
                        psOutMsg.executeUpdate()
                    }

                    c.commit()
                    persisted = true
                } catch (e: Exception) {
                    c.rollback()
                    logger.warn("Could not insert conversation messages into DB: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.warn("Database connection unavailable for webhook persistence: ${e.message}")
        }

        // Also record in memory store for zero-latency retrieval
        ai.orchestree.backend.api.InMemoryConversationStore.recordExchange(
            convId = convId,
            tenantId = tenantId,
            senderId = senderId,
            senderName = senderName,
            userMessage = messageText,
            aiReply = replyText,
            intent = classifiedIntent.intent.name
        )

        return@withContext WebhookExecutionResult(
            verified = true,
            senderId = senderId,
            senderName = senderName,
            messageText = messageText,
            intent = classifiedIntent.intent.name,
            persona = "SALES_REPRESENTATIVE",
            replyText = replyText,
            conversationId = convId,
            persisted = persisted
        )
    }
}

