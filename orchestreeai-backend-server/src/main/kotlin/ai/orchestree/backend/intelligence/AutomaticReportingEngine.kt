package ai.orchestree.backend.intelligence

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
data class AutomatedReport(
    val id: String,
    val tenantId: String,
    val reportType: String, // DAILY_BRIEF, WEEKLY_EXECUTIVE, MONTHLY_FINANCIAL, ANOMALY_REPORT
    val title: String,
    val summary: String,
    val metricsJson: String,
    val generatedAt: Long = System.currentTimeMillis()
)

object AutomaticReportingEngine {
    private val logger = LoggerFactory.getLogger(AutomaticReportingEngine::class.java)

    /**
     * Menghasilkan laporan otomatis periodik untuk direksi dan manajer departemen.
     */
    suspend fun generatePeriodicReport(tenantId: String, reportType: String): AutomatedReport = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(now))
        val reportId = "rep-" + UUID.randomUUID().toString().take(8)
        val title = "Laporan $reportType - $dateStr"

        var totalOrders = 0
        var totalRevenue = 0.0
        var activeAgents = 0

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("SELECT count(*), coalesce(sum(total_amount), 0) FROM orders WHERE tenant_id = ?").use { ps ->
                        ps.setString(1, tenantId)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                totalOrders = rs.getInt(1)
                                totalRevenue = rs.getDouble(2)
                            }
                        }
                    }

                    c.prepareStatement("SELECT count(*) FROM agents WHERE tenant_id = ? AND status = 'ONLINE'").use { ps ->
                        ps.setString(1, tenantId)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                activeAgents = rs.getInt(1)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not query metrics for automated report: ${e.message}")
            }
        }

        val summary = "Sintesis berkala $reportType untuk tenant: $totalOrders transaksi tercatat dengan total omzet Rp ${String.format("%,.0f", totalRevenue)}. $activeAgents AI agents beroperasi dengan status ONLINE."
        val metricsJson = """{"total_orders":$totalOrders,"total_revenue":$totalRevenue,"active_agents":$activeAgents}"""

        AutomatedReport(
            id = reportId,
            tenantId = tenantId,
            reportType = reportType,
            title = title,
            summary = summary,
            metricsJson = metricsJson,
            generatedAt = now
        )
    }
}
