package ai.orchestree.backend.channels.adapters

import kotlinx.serialization.Serializable
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

object TikTokAdapter {

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
}
