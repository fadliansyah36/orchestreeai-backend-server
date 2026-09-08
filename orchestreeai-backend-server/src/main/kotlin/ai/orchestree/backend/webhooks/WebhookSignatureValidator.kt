package ai.orchestree.backend.webhooks

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebhookSignatureValidator {
    fun verifyHmacSha256(payload: String, secretKey: String, expectedSignature: String?): Boolean {
        if (expectedSignature.isNullOrBlank() || secretKey.isBlank()) return false
        try {
            val keySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)
            val hashBytes = mac.doFinal(payload.toByteArray())
            val hexString = hashBytes.joinToString("") { "%02x".format(it) }
            return hexString.equals(expectedSignature.trim(), ignoreCase = true)
        } catch (e: Exception) {
            return false
        }
    }

    fun verifyMidtransSha512(orderId: String, statusCode: String, grossAmount: String, serverKey: String, signatureKey: String?): Boolean {
        if (signatureKey.isNullOrBlank() || serverKey.isBlank()) return false
        try {
            val raw = "$orderId$statusCode$grossAmount$serverKey"
            val md = MessageDigest.getInstance("SHA-512")
            val digest = md.digest(raw.toByteArray())
            val calculated = digest.joinToString("") { "%02x".format(it) }
            return calculated.equals(signatureKey.trim(), ignoreCase = true)
        } catch (e: Exception) {
            return false
        }
    }

    fun verifyMetaHmacSha256(payload: String, appSecret: String, hubSignatureHeader: String?): Boolean {
        if (hubSignatureHeader.isNullOrBlank() || appSecret.isBlank()) return false
        val cleanSignature = if (hubSignatureHeader.startsWith("sha256=")) {
            hubSignatureHeader.removePrefix("sha256=")
        } else {
            hubSignatureHeader
        }
        return verifyHmacSha256(payload, appSecret, cleanSignature)
    }

    fun verifyTelegramSecretToken(headerToken: String?, expectedSecretToken: String): Boolean {
        if (headerToken.isNullOrBlank() || expectedSecretToken.isBlank()) return false
        return try {
            MessageDigest.isEqual(headerToken.trim().toByteArray(), expectedSecretToken.trim().toByteArray())
        } catch (e: Exception) {
            false
        }
    }
}
