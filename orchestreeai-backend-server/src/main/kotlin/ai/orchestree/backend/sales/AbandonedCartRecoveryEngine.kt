package ai.orchestree.backend.sales

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class AbandonedCartRecord(
    val cartId: String,
    val tenantId: String,
    val customerId: String?,
    val customerName: String?,
    val customerPhone: String?,
    val totalAmount: Double,
    val hoursAbandoned: Double,
    val recoveryMessageSent: Boolean = false,
    val recoveryChannel: String = "WHATSAPP"
)

object AbandonedCartRecoveryEngine {
    private val logger = LoggerFactory.getLogger(AbandonedCartRecoveryEngine::class.java)

    /**
     * Identifikasi keranjang terbengkalai (> 2 jam tanpa checkout) dan jadwalkan recovery.
     */
    suspend fun identifyAbandonedCarts(tenantId: String, thresholdHours: Double = 2.0): List<AbandonedCartRecord> = withContext(Dispatchers.IO) {
        val abandoned = mutableListOf<AbandonedCartRecord>()
        val conn = DatabaseManager.getConnection()
        val now = System.currentTimeMillis()
        val cutoffMs = now - (thresholdHours * 3600 * 1000).toLong()

        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        SELECT c.id, c.tenant_id, c.customer_id, c.total_amount, c.created_at,
                               cust.name AS customer_name, cust.phone AS customer_phone
                        FROM carts c
                        LEFT JOIN customers cust ON c.customer_id = cust.id
                        WHERE c.tenant_id = ? 
                          AND c.created_at <= ? 
                          AND c.id NOT IN (SELECT cart_id FROM orders WHERE cart_id IS NOT NULL)
                        LIMIT 50
                    """.trimIndent()).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setLong(2, cutoffMs)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                val createdAt = rs.getLong("created_at")
                                val hours = ((now - createdAt) / (1000.0 * 3600.0)).coerceAtLeast(thresholdHours)
                                abandoned.add(
                                    AbandonedCartRecord(
                                        cartId = rs.getString("id"),
                                        tenantId = rs.getString("tenant_id"),
                                        customerId = rs.getString("customer_id"),
                                        customerName = rs.getString("customer_name") ?: "Pelanggan",
                                        customerPhone = rs.getString("customer_phone"),
                                        totalAmount = rs.getDouble("total_amount"),
                                        hoursAbandoned = hours,
                                        recoveryMessageSent = false
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not query carts table: ${e.message}")
            }
        }

        abandoned
    }

    /**
     * Kirim template pesan follow-up pemulihan keranjang secara proaktif.
     */
    fun composeRecoveryMessage(customerName: String, cartTotal: Double, discountVoucherCode: String = "KEMBALI5"): String {
        val formattedAmount = String.format("%,.0f", cartTotal)
        return """
            Halo $customerName! Kami melihat Anda masih menyimpan barang di keranjang belanja senilai Rp $formattedAmount.
            Selesaikan pesanan Anda sekarang dan gunakan voucher khusus *$discountVoucherCode* untuk mendapatkan potongan tambahan 5%!
            Klik tautan ini untuk melanjutkan: https://app.orchestree.ai/checkout
        """.trimIndent()
    }
}
