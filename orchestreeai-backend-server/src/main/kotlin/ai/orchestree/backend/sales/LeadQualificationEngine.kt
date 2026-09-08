package ai.orchestree.backend.sales

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class BantScoreResult(
    val leadId: String,
    val tenantId: String,
    val budgetScore: Int,      // 0-20
    val authorityScore: Int,   // 0-20
    val needScore: Int,        // 0-20
    val timelineScore: Int,    // 0-20
    val fitScore: Int,         // 0-10
    val velocityScore: Int,    // 0-10
    val totalScore: Int,       // 0-100
    val qualificationStage: String, // UNQUALIFIED, INITIAL_INTEREST, PRODUCT_EVALUATION, QUALIFIED_OPPORTUNITY
    val reasoning: String
)

object LeadQualificationEngine {
    private val logger = LoggerFactory.getLogger(LeadQualificationEngine::class.java)

    fun calculateBantScore(
        budget: Double,
        hasDecisionMaker: Boolean,
        explicitNeedIdentified: Boolean,
        purchaseDays: Int,
        companyFitTier: String = "MEDIUM",
        engagementVelocity: Double = 1.0
    ): Pair<Int, String> {
        var budgetScore = when {
            budget >= 50_000_000.0 -> 20
            budget >= 20_000_000.0 -> 16
            budget >= 5_000_000.0 -> 12
            budget > 0.0 -> 8
            else -> 2
        }

        var authorityScore = if (hasDecisionMaker) 20 else 8
        var needScore = if (explicitNeedIdentified) 20 else 6

        var timelineScore = when {
            purchaseDays <= 14 -> 20
            purchaseDays <= 30 -> 16
            purchaseDays <= 60 -> 12
            purchaseDays <= 90 -> 8
            else -> 4
        }

        var fitScore = when (companyFitTier.uppercase()) {
            "ENTERPRISE", "HIGH" -> 10
            "MEDIUM" -> 7
            else -> 4
        }

        var velocityScore = when {
            engagementVelocity >= 2.0 -> 10
            engagementVelocity >= 1.0 -> 8
            else -> 4
        }

        val totalScore = (budgetScore + authorityScore + needScore + timelineScore + fitScore + velocityScore).coerceIn(0, 100)

        val stage = when {
            totalScore >= 80 -> "QUALIFIED_OPPORTUNITY"
            totalScore >= 60 -> "PRODUCT_EVALUATION"
            totalScore >= 40 -> "INITIAL_INTEREST"
            else -> "UNQUALIFIED"
        }

        return Pair(totalScore, stage)
    }

    suspend fun qualifyLead(
        tenantId: String,
        leadId: String,
        budget: Double,
        hasDecisionMaker: Boolean,
        explicitNeedIdentified: Boolean,
        purchaseDays: Int,
        companyFitTier: String = "MEDIUM",
        engagementVelocity: Double = 1.0
    ): BantScoreResult = withContext(Dispatchers.IO) {
        val budgetScore = when {
            budget >= 50_000_000.0 -> 20
            budget >= 20_000_000.0 -> 16
            budget >= 5_000_000.0 -> 12
            budget > 0.0 -> 8
            else -> 2
        }
        val authorityScore = if (hasDecisionMaker) 20 else 8
        val needScore = if (explicitNeedIdentified) 20 else 6
        val timelineScore = when {
            purchaseDays <= 14 -> 20
            purchaseDays <= 30 -> 16
            purchaseDays <= 60 -> 12
            purchaseDays <= 90 -> 8
            else -> 4
        }
        val fitScore = when (companyFitTier.uppercase()) {
            "ENTERPRISE", "HIGH" -> 10
            "MEDIUM" -> 7
            else -> 4
        }
        val velocityScore = when {
            engagementVelocity >= 2.0 -> 10
            engagementVelocity >= 1.0 -> 8
            else -> 4
        }

        val total = (budgetScore + authorityScore + needScore + timelineScore + fitScore + velocityScore).coerceIn(0, 100)
        val stage = when {
            total >= 80 -> "QUALIFIED_OPPORTUNITY"
            total >= 60 -> "PRODUCT_EVALUATION"
            total >= 40 -> "INITIAL_INTEREST"
            else -> "UNQUALIFIED"
        }

        val reasoning = "BANT Breakdown: Budget=$budgetScore, Authority=$authorityScore, Need=$needScore, Timeline=$timelineScore, Fit=$fitScore, Velocity=$velocityScore -> Total $total/100 ($stage)"

        // Update database if exists
        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        UPDATE leads 
                        SET qualification_score = ?, status = ?, updated_at = ? 
                        WHERE id = ? AND tenant_id = ?
                    """.trimIndent()).use { ps ->
                        ps.setDouble(1, total.toDouble())
                        ps.setString(2, stage)
                        ps.setLong(3, System.currentTimeMillis())
                        ps.setString(4, leadId)
                        ps.setString(5, tenantId)
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not persist lead score in leads table: ${e.message}")
            }
        }

        BantScoreResult(
            leadId = leadId,
            tenantId = tenantId,
            budgetScore = budgetScore,
            authorityScore = authorityScore,
            needScore = needScore,
            timelineScore = timelineScore,
            fitScore = fitScore,
            velocityScore = velocityScore,
            totalScore = total,
            qualificationStage = stage,
            reasoning = reasoning
        )
    }
}
