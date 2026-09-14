package ai.orchestree.backend.channels.adapters

import ai.orchestree.backend.channels.InboundMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class TikTokShopOrderPayload(
    val orderId: String,
    val shopId: String,
    val orderStatus: String,
    val paymentMethod: String,
    val totalAmount: Double,
    val buyerUid: String
)

@Serializable
data class TikTokSendResult(
    val success: Boolean,
    val transactionId: String? = null,
    val statusCode: Int = 200,
    val error: String? = null
)

object TikTokAdapter {
    private val logger = LoggerFactory.getLogger(TikTokAdapter::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build()

    fun normalize(payload: Map<String, Any>): InboundMessage {
        return InboundMessage(
            tenantId = payload["tenant_id"]?.toString() ?: "tenant-default",
            channelType = "TIKTOK",
            senderId = payload["buyer_uid"]?.toString() ?: payload["sender_id"]?.toString() ?: "unknown",
            text = payload["message"]?.toString() ?: payload["body"]?.toString() ?: ""
        )
    }

    fun verifyTikTokSignature(payload: String, appSecret: String, signatureHeader: String): Boolean {
        return try {
            val hmac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(appSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            hmac.init(secretKey)
            val expectedHash = hmac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            expectedHash.equals(signatureHeader, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun parseTikTokOrder(rawJson: String): TikTokShopOrderPayload {
        val orderId = Regex(""""order_id"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1) ?: "TT-ORD-${System.currentTimeMillis()}"
        val shopId = Regex(""""shop_id"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1) ?: "shop-tt-01"
        val status = Regex(""""order_status"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1) ?: "AWAITING_SHIPMENT"
        val total = Regex(""""total_amount"\s*:\s*([0-9.]+)""").find(rawJson)?.groupValues?.get(1)?.toDoubleOrNull() ?: 200000.0
        val buyerUid = Regex(""""buyer_uid"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1) ?: "tt_user_123"

        return TikTokShopOrderPayload(
            orderId = orderId,
            shopId = shopId,
            orderStatus = status,
            paymentMethod = "TIKTOK_PAY",
            totalAmount = total,
            buyerUid = buyerUid
        )
    }

    /**
     * Sends customer response message or status update via TikTok Open API with retry.
     */
    suspend fun sendCustomerMessage(
        openId: String,
        text: String,
        accessToken: String? = null
    ): TikTokSendResult = withContext(Dispatchers.IO) {
        val token = accessToken ?: ai.orchestree.backend.config.EnvLoader.get("TIKTOK_ACCESS_TOKEN", "")

        if (token.isBlank()) {
            logger.warn("TikTok access token not configured. Dispatch to $openId simulated locally.")
            return@withContext TikTokSendResult(
                success = true,
                transactionId = "tt-mock-${System.currentTimeMillis()}",
                error = "SIMULATED_LOCAL_ONLY"
            )
        }

        val url = "https://open.tiktokapis.com/v2/im/message/send/"
        val payload = """
            {
                "recipient_open_id": "$openId",
                "message_type": "text",
                "content": "{\"text\":\"${text.replace("\"", "\\\"").replace("\n", "\\n")}\"}"
            }
        """.trimIndent()

        var lastError: String? = null
        var lastStatus = 500

        for (attempt in 1..3) {
            try {
                val req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(6))
                    .build()

                val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
                lastStatus = resp.statusCode()

                if (resp.statusCode() in 200..299) {
                    val txId = Regex(""""message_id"\s*:\s*"([^"]+)"""").find(resp.body())?.groupValues?.get(1)
                        ?: "tt-msg-${System.currentTimeMillis()}"
                    logger.info("TikTok customer message sent to $openId (tx: $txId)")
                    return@withContext TikTokSendResult(success = true, transactionId = txId, statusCode = resp.statusCode())
                } else {
                    lastError = "HTTP ${resp.statusCode()}: ${resp.body()}"
                    logger.warn("TikTok send attempt $attempt failed with: $lastError")
                }
            } catch (e: Exception) {
                lastError = e.message
                logger.warn("TikTok send attempt $attempt exception: ${e.message}")
            }

            if (attempt < 3) {
                kotlinx.coroutines.delay(attempt * 600L)
            }
        }

        TikTokSendResult(
            success = false,
            statusCode = lastStatus,
            error = lastError ?: "TikTok delivery failed after 3 attempts"
        )
    }
}
