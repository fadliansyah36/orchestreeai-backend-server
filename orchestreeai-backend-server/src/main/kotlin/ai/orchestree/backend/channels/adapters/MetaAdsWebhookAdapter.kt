package ai.orchestree.backend.channels.adapters

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class MetaLeadAdPayload(
    val leadgenId: String,
    val pageId: String,
    val formId: String,
    val adId: String?,
    val fullName: String?,
    val email: String?,
    val phoneNumber: String?,
    val createdTime: Long = System.currentTimeMillis()
)

object MetaAdsWebhookAdapter {
    private val logger = LoggerFactory.getLogger(MetaAdsWebhookAdapter::class.java)

    fun verifyMetaSignature(payload: String, appSecret: String, signatureHeader: String): Boolean {
        return try {
            val hmac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(appSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            hmac.init(secretKey)
            val expectedHash = hmac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            val cleanSig = signatureHeader.removePrefix("sha256=")
            expectedHash.equals(cleanSig, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun parseLeadPayload(rawJson: String): MetaLeadAdPayload {
        val leadgenId = Regex(""""leadgen_id"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1) ?: "lead-${System.currentTimeMillis()}"
        val pageId = Regex(""""page_id"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1) ?: "page-default"
        val formId = Regex(""""form_id"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1) ?: "form-default"
        val email = Regex(""""email"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1)
        val phone = Regex(""""phone_number"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1)
        val name = Regex(""""full_name"\s*:\s*"([^"]+)"""").find(rawJson)?.groupValues?.get(1) ?: "Prospek Meta"

        return MetaLeadAdPayload(
            leadgenId = leadgenId,
            pageId = pageId,
            formId = formId,
            adId = "ad-meta-01",
            fullName = name,
            email = email,
            phoneNumber = phone
        )
    }
}
