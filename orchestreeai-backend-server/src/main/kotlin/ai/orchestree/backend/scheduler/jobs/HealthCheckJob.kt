package ai.orchestree.backend.scheduler.jobs

import ai.orchestree.backend.modelrouter.HealthCheckEngine
import ai.orchestree.backend.modelrouter.ProviderHealth
import org.slf4j.LoggerFactory

class HealthCheckJob(
    private val healthCheckEngine: HealthCheckEngine = HealthCheckEngine()
) {
    private val logger = LoggerFactory.getLogger(HealthCheckJob::class.java)

    suspend fun execute(): List<ProviderHealth> {
        logger.info("[HEALTH_CHECK_JOB] Executing periodic health check across LLM and integration endpoints")
        healthCheckEngine.validateRegisteredProviderModels()
        val results = healthCheckEngine.checkAll()
        results.forEach { health ->
            if (health.isHealthy) {
                logger.info("[HEALTH_CHECK_JOB] Provider '${health.providerName}' is HEALTHY (${health.latencyMs}ms)")
            } else {
                logger.warn("[HEALTH_CHECK_JOB] Provider '${health.providerName}' UNHEALTHY: ${health.errorMessage}")
            }
        }
        return results
    }
}
