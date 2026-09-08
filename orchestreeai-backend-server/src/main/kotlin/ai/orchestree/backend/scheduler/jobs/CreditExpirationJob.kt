package ai.orchestree.backend.scheduler.jobs

import ai.orchestree.backend.billing.CommercialCreditEngine
import org.slf4j.LoggerFactory

/**
 * CreditExpirationJob (Fase 114)
 * Mengeksekusi kadaluarsa kredit subscription balance di setiap akhir siklus penagihan.
 */
class CreditExpirationJob(
    private val creditEngine: CommercialCreditEngine = CommercialCreditEngine()
) {
    private val logger = LoggerFactory.getLogger(CreditExpirationJob::class.java)

    suspend fun execute(): Int {
        logger.info("[SCHEDULER_JOB] Running CreditExpirationJob for expiring subscription balances...")
        return try {
            val count = creditEngine.runCreditExpirationJob()
            logger.info("[SCHEDULER_JOB] CreditExpirationJob completed. Expired balances for $count tenant(s).")
            count
        } catch (e: Exception) {
            logger.error("[SCHEDULER_JOB] Error running CreditExpirationJob: ${e.message}", e)
            0
        }
    }
}
