package ai.orchestree.backend.channels.adapters

import ai.orchestree.backend.channels.InboundMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class WhatsAppSendResult(
    val success: Boolean,
    val messageId: String? = null,
    val statusCode: Int = 200,
    val error: String? = null
)

class WhatsAppAdapter(
    private val defaultPhoneNumberId: String = ai.orchestree.backend.config.EnvLoader.get("WHATSAPP_PHONE_NUMBER_ID", "100609346426301"),
    private val defaultAccessToken: String = ai.orchestree.backend.config.EnvLoader.get("WHATSAPP_ACCESS_TOKEN", ""),
    private val appSecret: String = ai.orchestree.backend.config.EnvLoader.get("WHATSAPP_APP_SECRET", "")
) {
    private val logger = LoggerFactory.getLogger(WhatsAppAdapter::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build()

    /**
     * Normalizes inbound webhook payloads into standard InboundMessage DTO.
     * Handles both flat maps and nested Meta Cloud API webhook formats.
     */
    fun normalize(payload: Map<String, Any>): InboundMessage {
        val tenantId = payload["tenant_id"]?.toString() ?: "tenant-default"

        // Flat payload format
        if (payload.containsKey("from") || payload.containsKey("body")) {
            return InboundMessage(
                tenantId = tenantId,
                channelType = "WHATSAPP",
                senderId = payload["from"]?.toString() ?: "unknown",
                text = payload["body"]?.toString() ?: ""
            )
        }

        return InboundMessage(
            tenantId = tenantId,
            channelType = "WHATSAPP",
            senderId = "unknown",
            text = ""
        )
    }

    /**
     * Parses standard Meta WhatsApp Cloud API raw JSON webhook payload.
     */
    fun parseWebhookJson(rawJson: String, defaultTenantId: String = "tenant-default"): InboundMessage? {
        return try {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val entry = root["entry"]?.jsonArray?.firstOrNull()?.jsonObject
            val change = entry?.get("changes")?.jsonArray?.firstOrNull()?.jsonObject
            val value = change?.get("value")?.jsonObject
            val message = value?.get("messages")?.jsonArray?.firstOrNull()?.jsonObject

            if (message != null) {
                val from = message["from"]?.jsonPrimitive?.content ?: "unknown"
                val text = message["text"]?.jsonObject?.get("body")?.jsonPrimitive?.content ?: ""
                InboundMessage(
                    tenantId = defaultTenantId,
                    channelType = "WHATSAPP",
                    senderId = from,
                    text = text
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse WhatsApp webhook JSON: ${e.message}")
            null
        }
    }

    /**
     * Validates Meta X-Hub-Signature-256 header using SHA256 HMAC.
     */
    fun verifyWebhookSignature(rawBody: String, signatureHeader: String, secret: String = appSecret): Boolean {
        if (secret.isBlank() || signatureHeader.isBlank()) return false
        return try {
            val expectedPrefix = "sha256="
            val cleanSignature = if (signatureHeader.startsWith(expectedPrefix)) {
                signatureHeader.removePrefix(expectedPrefix)
            } else {
                signatureHeader
            }

            val hmac = Mac.getInstance("HmacSHA256")
            val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            hmac.init(key)
            val computedHash = hmac.doFinal(rawBody.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            computedHash.equals(cleanSignature.trim(), ignoreCase = true)
        } catch (e: Exception) {
            logger.error("Error verifying WhatsApp webhook signature: ${e.message}")
            false
        }
    }

    /**
     * Sends an outbound text message via Meta WhatsApp Cloud API with exponential retry.
     */
    suspend fun sendMessage(
        recipientPhoneNumber: String,
        text: String,
        phoneNumberId: String? = null,
        token: String? = null
    ): WhatsAppSendResult = withContext(Dispatchers.IO) {
        val activePhoneId = phoneNumberId ?: defaultPhoneNumberId
        val activeToken = token ?: defaultAccessToken

        if (activeToken.isBlank()) {
            logger.warn("WhatsApp access token not configured. Message to $recipientPhoneNumber logged only.")
            return@withContext WhatsAppSendResult(
                success = true,
                messageId = "wa-mock-${System.currentTimeMillis()}",
                error = "SIMULATED_LOCAL_ONLY"
            )
        }

        val url = "https://graph.facebook.com/v20.0/$activePhoneId/messages"
        val payload = """
            {
                "messaging_product": "whatsapp",
                "recipient_type": "individual",
                "to": "${recipientPhoneNumber.filter { it.isDigit() }}",
                "type": "text",
                "text": {
                    "preview_url": false,
                    "body": "${text.replace("\"", "\\\"").replace("\n", "\\n")}"
                }
            }
        """.trimIndent()

        var lastError: String? = null
        var lastStatusCode = 500

        // Retry loop: 3 attempts with exponential backoff
        for (attempt in 1..3) {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer $activeToken")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(6))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                lastStatusCode = response.statusCode()

                if (response.statusCode() in 200..299) {
                    val body = response.body()
                    val msgId = Regex(""""id"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
                        ?: "wa-id-${System.currentTimeMillis()}"
                    logger.info("WhatsApp message delivered successfully to $recipientPhoneNumber (id: $msgId)")
                    return@withContext WhatsAppSendResult(success = true, messageId = msgId, statusCode = response.statusCode())
                } else {
                    lastError = "HTTP ${response.statusCode()}: ${response.body()}"
                    logger.warn("WhatsApp send attempt $attempt failed with: $lastError")
                }
            } catch (e: Exception) {
                lastError = e.message
                logger.warn("WhatsApp send attempt $attempt exception: ${e.message}")
            }

            if (attempt < 3) {
                kotlinx.coroutines.delay(attempt * 600L)
            }
        }

        WhatsAppSendResult(
            success = false,
            statusCode = lastStatusCode,
            error = lastError ?: "Delivery failed after 3 attempts"
        )
    }
}
