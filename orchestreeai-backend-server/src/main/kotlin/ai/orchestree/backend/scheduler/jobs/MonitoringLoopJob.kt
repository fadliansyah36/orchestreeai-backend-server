package ai.orchestree.backend.scheduler.jobs

import ai.orchestree.backend.orchestration.MonitoringLoopEngine
import org.slf4j.LoggerFactory

class MonitoringLoopJob(
    private val monitoringLoopEngine: MonitoringLoopEngine = MonitoringLoopEngine()
) {
    private val logger = LoggerFactory.getLogger(MonitoringLoopJob::class.java)

    suspend fun execute(tenantId: String? = null): String {
        logger.info("[SCHEDULER:MONITORING_LOOP] Executing monitoring loop state machine tick for tenant=${tenantId ?: "all"}")
        val updated = monitoringLoopEngine.monitoringLoopTick()
        val summary = "Ticked ${updated.size} active loops. States: ${updated.map { "${it.id}:${it.state}" }}"
        logger.info("[SCHEDULER:MONITORING_LOOP] Result: $summary")
        return summary
    }
}
