package ai.orchestree.backend.sales

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class GroundingValidationResult(
    val isValid: Boolean,
    val sanitizedText: String,
    val violations: List<String> = emptyList(),
    val confidenceScore: Double = 1.0
)

object GroundingOutputValidator {
    private val logger = LoggerFactory.getLogger(GroundingOutputValidator::class.java)

    /**
     * Memvalidasi output AI terhadap katalog produk nyata agar TIDAK TERJADI HALUSINASI HARGA / DISKON FIKTIF.
     */
    suspend fun validateCommerceResponse(
        tenantId: String,
        aiGeneratedMessage: String,
        mentionedSkusOrProducts: List<String> = emptyList()
    ): GroundingValidationResult = withContext(Dispatchers.IO) {
        val violations = mutableListOf<String>()
        var sanitized = aiGeneratedMessage

        // 1. Guardrail Diskon Tidak Resmi: AI tidak boleh menjanjikan diskon di atas 20% tanpa persetujuan
        val discountRegex = Regex("""(?:diskon|potongan)\s+(\d+)%""", RegexOption.IGNORE_CASE)
        val matches = discountRegex.findAll(aiGeneratedMessage)
        for (m in matches) {
            val pct = m.groupValues[1].toIntOrNull() ?: 0
            if (pct > 15) {
                violations.add("UNAUTHORIZED_DISCOUNT_PROMISE: AI promised $pct% discount exceeding max autonomous threshold (15%)")
                sanitized = sanitized.replace(m.value, "diskon khusus (memerlukan persetujuan manajer)")
            }
        }

        // 2. Guardrail Katalog Produk Real: Periksa apakah SKU/Produk benar ada di database
        if (mentionedSkusOrProducts.isNotEmpty()) {
            val conn = DatabaseManager.getConnection()
            if (conn != null) {
                try {
                    conn.use { c ->
                        for (sku in mentionedSkusOrProducts) {
                            c.prepareStatement("SELECT id FROM products WHERE tenant_id = ? AND (sku = ? OR id = ?) LIMIT 1").use { ps ->
                                ps.setString(1, tenantId)
                                ps.setString(2, sku)
                                ps.setString(3, sku)
                                ps.executeQuery().use { rs ->
                                    if (!rs.next()) {
                                        violations.add("HALLUCINATED_PRODUCT: SKU or Product '$sku' does not exist in tenant catalog")
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Could not verify product catalog grounding: ${e.message}")
                }
            }
        }

        GroundingValidationResult(
            isValid = violations.isEmpty(),
            sanitizedText = sanitized,
            violations = violations,
            confidenceScore = if (violations.isEmpty()) 0.98 else 0.45
        )
    }
}
