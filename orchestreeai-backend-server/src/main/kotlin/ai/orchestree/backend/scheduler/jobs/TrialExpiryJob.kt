package ai.orchestree.backend.scheduler.jobs

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class TrialExpiryJob {
    private val logger = LoggerFactory.getLogger(TrialExpiryJob::class.java)

    suspend fun execute(): Int = withContext(Dispatchers.IO) {
        logger.info("[TRIAL_EXPIRY_JOB] Checking expired tenant trials across all active subscriptions...")
        var expiredCount = 0

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        UPDATE subscriptions 
                        SET status = 'EXPIRED', updated_at = NOW() 
                        WHERE LOWER(status) IN ('trial', 'pending') 
                          AND (
                            (trial_ends_at IS NOT NULL AND trial_ends_at <= NOW()) OR 
                            (current_period_end IS NOT NULL AND current_period_end <= NOW())
                          )
                    """.trimIndent()
                    val stmt = c.prepareStatement(sql)
                    expiredCount = stmt.executeUpdate()
                    stmt.close()
                    logger.info("[TRIAL_EXPIRY_JOB] Successfully updated $expiredCount expired subscription trial(s) to EXPIRED")
                }
            } catch (e: Exception) {
                logger.warn("[TRIAL_EXPIRY_JOB] Database update failed: ${e.message}")
            }
        } else {
            logger.info("[TRIAL_EXPIRY_JOB] Database connection not configured, trial check completed with 0 updates")
        }

        expiredCount
    }
}
