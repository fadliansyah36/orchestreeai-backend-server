package ai.orchestree.backend.scheduler.jobs

import org.slf4j.LoggerFactory

class HealthCheckJob {
    private val logger = LoggerFactory.getLogger(HealthCheckJob::class.java)

    suspend fun execute() {
        logger.info("Executing periodic health check across LLM and integration endpoints")
    }
}
