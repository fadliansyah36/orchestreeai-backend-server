package ai.orchestree.backend.webhooks

import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.channels.ChannelGateway
import ai.orchestree.backend.channels.InboundMessage
import ai.orchestree.backend.conversation.SalesIntentClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.util.UUID

class TelegramWebhookHandler(
    private val validator: WebhookSignatureValidator = WebhookSignatureValidator(),
    private val channelGateway: ChannelGateway = ChannelGateway()
) {
    private val logger = LoggerFactory.getLogger(TelegramWebhookHandler::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    fun handleWebhook(secretTokenHeader: String?, expectedSecretToken: String): Boolean {
        val isValid = validator.verifyTelegramSecretToken(secretTokenHeader, expectedSecretToken)
        if (!isValid) {
            logger.warn("Telegram webhook rejected: invalid X-Telegram-Bot-Api-Secret-Token")
            return false
        }

        logger.info("Telegram webhook secret token verified successfully")
        return true
    }

    suspend fun processInboundWebhook(
        payload: String,
        secretTokenHeader: String?,
        expectedSecretToken: String,
        defaultTenantId: String = "tenant-default"
    ): WebhookExecutionResult = withContext(Dispatchers.IO) {
        val isValid = handleWebhook(secretTokenHeader, expectedSecretToken)
        if (!isValid) {
            return@withContext WebhookExecutionResult(
                verified = false,
                errorMessage = "Invalid Telegram secret token X-Telegram-Bot-Api-Secret-Token"
            )
        }

        var senderId = "tg-1111111"
        var senderName = "Telegram User"
        var messageText = ""
        var tenantId = defaultTenantId

        try {
            val root = json.parseToJsonElement(payload).jsonObject
            // Case 1: Direct JSON format
            if (root.containsKey("messageText") || root.containsKey("message") && root["message"]?.jsonPrimitive != null) {
                messageText = root["messageText"]?.jsonPrimitive?.content
                    ?: root["message"]?.jsonPrimitive?.content ?: ""
                senderId = root["senderId"]?.jsonPrimitive?.content ?: senderId
                senderName = root["senderName"]?.jsonPrimitive?.content ?: senderName
                tenantId = root["tenantId"]?.jsonPrimitive?.content ?: tenantId
            }
            // Case 2: Telegram Bot API Update format
            else if (root.containsKey("message")) {
                val msgObj = root["message"]?.jsonObject
                messageText = msgObj?.get("text")?.jsonPrimitive?.content ?: ""
                val fromObj = msgObj?.get("from")?.jsonObject
                val fromId = fromObj?.get("id")?.jsonPrimitive?.content ?: "unknown"
                val firstName = fromObj?.get("first_name")?.jsonPrimitive?.content ?: "User"
                val lastName = fromObj?.get("last_name")?.jsonPrimitive?.content ?: ""
                senderId = "tg-$fromId"
                senderName = "$firstName $lastName".trim()
            }
        } catch (e: Exception) {
            logger.warn("Could not parse Telegram webhook JSON payload: ${e.message}")
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

        // 1. Classify sales intent
        val classifiedIntent = SalesIntentClassifier.classify(messageText)

        // 2. Delegate to ChannelGateway
        val inbound = InboundMessage(
            tenantId = tenantId,
            channelType = "TELEGRAM",
            senderId = senderId,
            text = messageText,
            metadata = mapOf("senderName" to senderName)
        )
        val outbound = channelGateway.processInbound(inbound)
        val replyText = outbound?.text ?: "Halo! Terima kasih atas pesan Anda di Telegram. Tim kami akan segera menindaklanjuti."

        // 3. Persist to conversations and conversation_messages tables
        val convId = "conv-tg-${senderId.filter { it.isLetterOrDigit() }.take(16)}"
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
                        ) VALUES (?, ?, 'ca-tg-default', 'cust-tg-01', ?, 'TELEGRAM', 'RECEPTIONIST', 'OPEN', ?, ?, 'INQUIRY', NOW(), NOW())
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

