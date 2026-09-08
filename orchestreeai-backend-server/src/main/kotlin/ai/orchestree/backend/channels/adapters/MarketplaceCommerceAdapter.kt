package ai.orchestree.backend.channels.adapters

import ai.orchestree.backend.channels.InboundMessage
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class NormalizedMarketplaceOrder(
    val orderId: String,
    val marketplace: String, // SHOPEE, TIKTOK, TOKOPEDIA
    val shopId: String,
    val buyerUsername: String,
    val totalAmount: Double,
    val items: List<String>,
    val shippingCourier: String
)

object MarketplaceCommerceAdapter {

    fun verifyShopeeSignature(rawBody: String, partnerKey: String, signatureHeader: String): Boolean {
        return try {
            val sha256Hmac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(partnerKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
            sha256Hmac.init(secretKey)
            val signedBytes = sha256Hmac.doFinal(rawBody.toByteArray(Charsets.UTF_8))
            val calculated = signedBytes.joinToString("") { "%02x".format(it) }
            calculated.equals(signatureHeader, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun parseOrderWebhook(marketplace: String, rawJson: String): NormalizedMarketplaceOrder {
        val orderId = Regex(""""order_sn"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1)
            ?: Regex(""""order_id"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1)
            ?: "MP-ORD-${System.currentTimeMillis()}"

        val shopId = Regex(""""shop_id"\s*:\s*"?([0-9a-zA-Z_-]+)"?""").find(rawJson)?.groupValues?.get(1) ?: "shop-default"
        val buyer = Regex(""""buyer_username"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1) ?: "buyer_anonymous"
        val total = Regex(""""total_amount"\s*:\s*([0-9.]+)""").find(rawJson)?.groupValues?.get(1)?.toDoubleOrNull() ?: 150000.0

        return NormalizedMarketplaceOrder(
            orderId = orderId,
            marketplace = marketplace.uppercase(),
            shopId = shopId,
            buyerUsername = buyer,
            totalAmount = total,
            items = listOf("Item Catalog"),
            shippingCourier = "J&T"
        )
    }
}
