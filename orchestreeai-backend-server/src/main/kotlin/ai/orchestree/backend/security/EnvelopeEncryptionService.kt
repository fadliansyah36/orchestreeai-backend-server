package ai.orchestree.backend.security

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class EncryptedEnvelope(
    val encryptedData: String,
    val encryptedKey: String,
    val iv: String
)

class EnvelopeEncryptionService(
    masterKeyBase64: String? = null
) {
    private val logger = LoggerFactory.getLogger(EnvelopeEncryptionService::class.java)
    private val masterKey: SecretKey

    init {
        masterKey = if (!masterKeyBase64.isNullOrBlank()) {
            val decoded = Base64.getDecoder().decode(masterKeyBase64)
            SecretKeySpec(decoded, 0, decoded.size, "AES")
        } else {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            keyGen.generateKey()
        }
    }

    fun encrypt(plainText: String): EncryptedEnvelope {
        // 1. Generate ephemeral DEK (Data Encryption Key)
        val dekGen = KeyGenerator.getInstance("AES")
        dekGen.init(256)
        val dek = dekGen.generateKey()

        // 2. Encrypt plaintext with DEK via AES-GCM
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, dek, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // 3. Encrypt DEK with KEK (Master Key)
        val kekCipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        kekCipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val encryptedDek = kekCipher.doFinal(dek.encoded)

        return EncryptedEnvelope(
            encryptedData = Base64.getEncoder().encodeToString(cipherText),
            encryptedKey = Base64.getEncoder().encodeToString(encryptedDek),
            iv = Base64.getEncoder().encodeToString(iv)
        )
    }

    fun decrypt(envelope: EncryptedEnvelope): String {
        // 1. Decrypt DEK with KEK
        val kekCipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        kekCipher.init(Cipher.DECRYPT_MODE, masterKey)
        val rawDek = kekCipher.doFinal(Base64.getDecoder().decode(envelope.encryptedKey))
        val dek = SecretKeySpec(rawDek, "AES")

        // 2. Decrypt data with DEK
        val iv = Base64.getDecoder().decode(envelope.iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, dek, GCMParameterSpec(128, iv))
        val plainBytes = cipher.doFinal(Base64.getDecoder().decode(envelope.encryptedData))

        return String(plainBytes, Charsets.UTF_8)
    }
}

class PromptInjectionGuard {
    private val jailbreakPatterns = listOf(
        "ignore previous instructions",
        "system prompt override",
        "disregard all prior directives",
        "you are now DAN",
        "jailbreak mode",
        "reveal secret key",
        "show internal system prompt"
    )

    fun inspect(prompt: String): Pair<Boolean, String?> {
        val lower = prompt.lowercase()
        for (pattern in jailbreakPatterns) {
            if (lower.contains(pattern)) {
                return false to "Prompt blocked by PromptInjectionGuard: matched forbidden pattern '$pattern'"
            }
        }
        return true to null
    }
}

class InputValidationAndEncoding {
    fun sanitizeSql(input: String): String {
        return input.replace("'", "''").replace(";", "")
    }

    fun sanitizeHtml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }
}
