package ai.orchestree.backend.billing

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.UUID

@Serializable
data class DunningExecutionResult(
    val invoiceId: String,
    val tenantId: String,
    val status: String, // 'IN_GRACE_PERIOD', 'SUSPENDED', 'RESOLVED'
    val retryCount: Int,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

object DunningEngine {
    private val logger = LoggerFactory.getLogger(DunningEngine::class.java)
    const val MAX_RETRY_ATTEMPTS = 3
    const val GRACE_PERIOD_MILLIS = 7L * 24 * 3600 * 1000L // 7 Days

    suspend fun handleInvoiceFailure(
        tenantId: String,
        invoiceId: String,
        amount: Double,
        reason: String,
        supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
    ): DunningExecutionResult = withContext(Dispatchers.IO) {
        val currentSub = try { CreditRepositoryManager().getTenantSubscription(tenantId) } catch (e: Exception) { null }
        if (currentSub?.isFounderExclusive == true) {
            logger.info("Skipping dunning for founder exclusive tenant $tenantId")
            return@withContext DunningExecutionResult(
                invoiceId = invoiceId,
                tenantId = tenantId,
                status = "RESOLVED",
                retryCount = 0,
                message = "Founder exclusive tenant exempt from dunning"
            )
        }

        val now = System.currentTimeMillis()
        var retryCount = 1
        val gracePeriodEnds = now + GRACE_PERIOD_MILLIS

        // Check if there's existing dunning in PostgreSQL
        try {
            val conn = DatabaseManager.getConnection()
            if (conn != null) {
                conn.use { c ->
                    c.prepareStatement("SELECT retry_count FROM dunning_logs WHERE invoice_id = ? ORDER BY created_at DESC LIMIT 1").use { ps ->
                        ps.setString(1, invoiceId)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                retryCount = rs.getInt(1) + 1
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Could not query previous dunning retry count: ${e.message}")
        }

        val isExhausted = retryCount >= MAX_RETRY_ATTEMPTS
        val newStatus = if (isExhausted) "SUSPENDED" else "IN_GRACE_PERIOD"
        val logId = "dun-${UUID.randomUUID().toString().take(8)}"

        val msg = if (isExhausted) {
            "Batas percobaan penagihan telah habis ($retryCount/$MAX_RETRY_ATTEMPTS). Tenant dialihkan ke status SUSPENDED (Read-Only)."
        } else {
            "Percobaan penagihan gagal ($retryCount/$MAX_RETRY_ATTEMPTS): $reason. Masa tenggang aktif sampai ${Date(gracePeriodEnds)}."
        }

        // Update tenant status in Supabase if suspended
        if (isExhausted) {
            try {
                val patchPayload = buildJsonObject {
                    put("status", "SUSPENDED")
                    put("suspended_at", now)
                    put("suspension_reason", "INVOICE_PAYMENT_FAILURE")
                }.toString()
                supabase.updateRecord("tenants", tenantId, "id=eq.$tenantId", patchPayload)
            } catch (e: Exception) {
                logger.error("Failed suspending tenant $tenantId: ${e.message}", e)
            }
        }

        // Persist dunning log
        try {
            val dunningPayload = buildJsonObject {
                put("id", logId)
                put("tenant_id", tenantId)
                put("invoice_id", invoiceId)
                put("retry_count", retryCount)
                put("status", newStatus)
                put("amount", amount)
                put("reason", reason)
                put("message", msg)
            }.toString()
            supabase.insertRecord("dunning_logs", tenantId, dunningPayload)
        } catch (e: Exception) {
            logger.error("Failed logging dunning record: ${e.message}", e)
        }

        DunningExecutionResult(
            invoiceId = invoiceId,
            tenantId = tenantId,
            status = newStatus,
            retryCount = retryCount,
            message = msg,
            timestamp = now
        )
    }
}
