package ai.orchestree.backend.security

import kotlinx.serialization.json.Json

class EncryptionService(
    private val envelopeService: EnvelopeEncryptionService = EnvelopeEncryptionService()
) {
    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return plainText
        val envelope = envelopeService.encrypt(plainText)
        return "enc:v1:" + java.util.Base64.getEncoder().encodeToString(
            Json.encodeToString(EncryptedEnvelope.serializer(), envelope).toByteArray(Charsets.UTF_8)
        )
    }

    fun decrypt(cipherText: String): String {
        if (!cipherText.startsWith("enc:v1:")) return cipherText
        return try {
            val jsonBytes = java.util.Base64.getDecoder().decode(cipherText.removePrefix("enc:v1:"))
            val envelope = Json.decodeFromString(EncryptedEnvelope.serializer(), String(jsonBytes, Charsets.UTF_8))
            envelopeService.decrypt(envelope)
        } catch (e: Exception) {
            cipherText
        }
    }
}

