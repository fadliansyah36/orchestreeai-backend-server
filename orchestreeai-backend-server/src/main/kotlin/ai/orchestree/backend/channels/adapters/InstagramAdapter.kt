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
data class InstagramSendResult(
    val success: Boolean,
    val messageId: String? = null,
    val statusCode: Int = 200,
    val error: String? = null
)

class InstagramAdapter(
    private val defaultAccessToken: String = ai.orchestree.backend.config.EnvLoader.get("INSTAGRAM_ACCESS_TOKEN", ""),
    private val appSecret: String = ai.orchestree.backend.config.EnvLoader.get("INSTAGRAM_APP_SECRET", "")
) {
    private val logger = LoggerFactory.getLogger(InstagramAdapter::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build()

    /**
     * Normalizes generic Map payload to standard InboundMessage.
     */
    fun normalize(payload: Map<String, Any>): InboundMessage {
        return InboundMessage(
            tenantId = payload["tenant_id"]?.toString() ?: "tenant-default",
            channelType = "INSTAGRAM",
            senderId = payload["ig_user_id"]?.toString() ?: payload["sender_id"]?.toString() ?: "unknown",
            text = payload["message"]?.toString() ?: payload["text"]?.toString() ?: ""
        )
    }

    /**
     * Parses standard Meta Instagram Messaging Webhook payload.
     */
    fun parseWebhookJson(rawJson: String, defaultTenantId: String = "tenant-default"): InboundMessage? {
        return try {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val entry = root["entry"]?.jsonArray?.firstOrNull()?.jsonObject
            val messaging = entry?.get("messaging")?.jsonArray?.firstOrNull()?.jsonObject

            if (messaging != null) {
                val senderId = messaging["sender"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: "unknown"
                val text = messaging["message"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                InboundMessage(
                    tenantId = defaultTenantId,
                    channelType = "INSTAGRAM",
                    senderId = senderId,
                    text = text
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse Instagram webhook JSON: ${e.message}")
            null
        }
    }

    /**
     * Verifies Meta X-Hub-Signature-256 for Instagram webhooks.
     */
    fun verifyWebhookSignature(rawBody: String, signatureHeader: String, secret: String = appSecret): Boolean {
        if (secret.isBlank() || signatureHeader.isBlank()) return false
        return try {
            val cleanSignature = signatureHeader.removePrefix("sha256=")
            val hmac = Mac.getInstance("HmacSHA256")
            val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            hmac.init(key)
            val computed = hmac.doFinal(rawBody.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            computed.equals(cleanSignature.trim(), ignoreCase = true)
        } catch (e: Exception) {
            logger.error("Error verifying Instagram webhook signature: ${e.message}")
            false
        }
    }

    /**
     * Sends an outbound Direct Message via Instagram Graph API with retry.
     */
    suspend fun sendMessage(
        recipientId: String,
        text: String,
        token: String? = null
    ): InstagramSendResult = withContext(Dispatchers.IO) {
        val activeToken = token ?: defaultAccessToken

        if (activeToken.isBlank()) {
            logger.warn("Instagram access token not configured. Message to $recipientId logged locally.")
            return@withContext InstagramSendResult(
                success = true,
                messageId = "ig-mock-${System.currentTimeMillis()}",
                error = "SIMULATED_LOCAL_ONLY"
            )
        }

        val url = "https://graph.facebook.com/v20.0/me/messages"
        val payload = """
            {
                "recipient": { "id": "$recipientId" },
                "message": { "text": "${text.replace("\"", "\\\"").replace("\n", "\\n")}" }
            }
        """.trimIndent()

        var lastError: String? = null
        var lastStatusCode = 500

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
                    val msgId = Regex(""""message_id"\s*:\s*"([^"]+)"""").find(response.body())?.groupValues?.get(1)
                        ?: "ig-mid-${System.currentTimeMillis()}"
                    logger.info("Instagram direct message delivered to $recipientId (mid: $msgId)")
                    return@withContext InstagramSendResult(success = true, messageId = msgId, statusCode = response.statusCode())
                } else {
                    lastError = "HTTP ${response.statusCode()}: ${response.body()}"
                    logger.warn("Instagram send attempt $attempt failed with: $lastError")
                }
            } catch (e: Exception) {
                lastError = e.message
                logger.warn("Instagram send attempt $attempt exception: ${e.message}")
            }

            if (attempt < 3) {
                kotlinx.coroutines.delay(attempt * 600L)
            }
        }

        InstagramSendResult(
            success = false,
            statusCode = lastStatusCode,
            error = lastError ?: "Delivery failed after 3 attempts"
        )
    }
}
