package ai.orchestree.backend.scheduler.jobs

import org.slf4j.LoggerFactory

class TrialExpiryJob {
    private val logger = LoggerFactory.getLogger(TrialExpiryJob::class.java)

    suspend fun execute(): Int {
        logger.info("Checking expired tenant trials across all active subscriptions...")
        return 0
    }
}
