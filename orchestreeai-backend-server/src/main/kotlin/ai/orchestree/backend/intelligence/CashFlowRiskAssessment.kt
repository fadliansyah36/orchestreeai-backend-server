package ai.orchestree.backend.intelligence

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class CashFlowRiskProfile(
    val tenantId: String,
    val burnRateMonthly: Double,
    val runwayMonths: Double,
    val receivablesRiskAmount: Double,
    val riskLevel: String, // LOW, MEDIUM, HIGH, CRITICAL
    val mitigationRecommendation: String
)

object CashFlowRiskAssessment {
    private val logger = LoggerFactory.getLogger(CashFlowRiskAssessment::class.java)

    /**
     * Menghitung profil risiko arus kas dan proyeksi runway enterprise.
     */
    suspend fun assessTenantCashFlow(tenantId: String): CashFlowRiskProfile = withContext(Dispatchers.IO) {
        var totalOutstandingReceivables = 0.0
        var totalSettledLast30Days = 0.0

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("SELECT coalesce(sum(total_amount), 0) FROM orders WHERE tenant_id = ? AND status = 'PENDING_PAYMENT'").use { ps ->
                        ps.setString(1, tenantId)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) totalOutstandingReceivables = rs.getDouble(1)
                        }
                    }

                    c.prepareStatement("SELECT coalesce(sum(total_amount), 0) FROM orders WHERE tenant_id = ? AND status = 'PAID'").use { ps ->
                        ps.setString(1, tenantId)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) totalSettledLast30Days = rs.getDouble(1)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not query cash flow metrics: ${e.message}")
            }
        }

        val estimatedMonthlyBurn = (totalSettledLast30Days * 0.7).coerceAtLeast(10_000_000.0)
        val estimatedReserve = (totalSettledLast30Days * 3.0).coerceAtLeast(30_000_000.0)
        val runwayMonths = if (estimatedMonthlyBurn > 0) estimatedReserve / estimatedMonthlyBurn else 12.0

        val riskLevel = when {
            runwayMonths < 2.0 -> "CRITICAL"
            runwayMonths < 4.0 -> "HIGH"
            runwayMonths < 6.0 -> "MEDIUM"
            else -> "LOW"
        }

        val recommendation = when (riskLevel) {
            "CRITICAL" -> "Peringatan darurat runway: Segera tunda belanja modal non-kritis dan lakukan penagihan intensif pada piutang tertunda."
            "HIGH" -> "Arus kas ketat: Perketat jangka waktu piutang termin (TOP) dan optimalkan konversi keranjang belanja."
            "MEDIUM" -> "Arus kas wajar: Pertahankan efisiensi operasional dan pantau tren konversi mingguan."
            else -> "Kondisi keuangan sehat dengan runway memadai di atas 6 bulan."
        }

        CashFlowRiskProfile(
            tenantId = tenantId,
            burnRateMonthly = estimatedMonthlyBurn,
            runwayMonths = runwayMonths,
            receivablesRiskAmount = totalOutstandingReceivables,
            riskLevel = riskLevel,
            mitigationRecommendation = recommendation
        )
    }
}
