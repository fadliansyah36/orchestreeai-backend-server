package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.database.repositories.orchestration.WorkflowExecutionRepository
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.orchestration.WorkflowExecutionResult
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureCORS
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import ai.orchestree.backend.scheduler.SchedulerEngine
import ai.orchestree.backend.security.configureAppAttestation
import ai.orchestree.backend.security.configureRequestValidation
import ai.orchestree.backend.security.configureServerRateLimiting
import ai.orchestree.backend.startup.StartupValidator
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ai.orchestree.backend.Application")
private var backgroundScheduler: SchedulerEngine? = null

/**
 * Scan workflow_executions with status='running' that timed out (indicating previous server crash)
 * and resume them from their last completed checkpoint.
 */
suspend fun recoverInterruptedWorkflows(
    workflowExecutionRepo: WorkflowExecutionRepository = WorkflowExecutionRepository(),
    orchestrationEngine: OrchestrationEngine = OrchestrationEngine(),
    olderThanMinutes: Long = 5
): List<WorkflowExecutionResult> {
    val orphaned = workflowExecutionRepo.findStaleRunning(olderThanMinutes = olderThanMinutes)
    val results = mutableListOf<WorkflowExecutionResult>()
    for (execution in orphaned) {
        logger.warn("[RECOVERY] Melanjutkan workflow ${execution.id} dari node ${execution.lastCompletedNodeId}")
        workflowExecutionRepo.updateStatus(execution.id, "crashed_recoverable")
        val res = orchestrationEngine.resumeFromCheckpoint(execution)
        results.add(res)
    }
    return results
}

fun main() {
    val config = AppConfig.load()
    logger.info("Starting OrchestreeAI Enterprise Backend Server in [${config.environment}] mode on port ${config.port}...")

    // 1. Eksekusi fail-closed startup validation sebelum membuka port HTTP
    runBlocking {
        StartupValidator(shouldExitProcessOnFailure = true).performAllStartupChecks(config)
    }

    // 2. Jika seluruh dependency kritis lolos, jalankan server Ktor
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}

fun Application.module(config: AppConfig = AppConfig.load()) {
    logger.info("Initializing OrchestreeAI Ktor Modules & Pipelines...")
    configureCORS(config)
    configureSerialization()
    configureRequestValidation()
    configureServerRateLimiting()
    configureAppAttestation()
    configureHTTPS()
    configureAuthentication(config)
    configureRouting()

    // 3. Start Background Autonomous Scheduler (LANGKAH 6)
    backgroundScheduler = SchedulerEngine().also { scheduler ->
        scheduler.start()
        logger.info("[STARTUP] Background Autonomous Scheduler Engine started successfully.")
    }

    // 4. Autonomous Recovery Job: Scan and resume interrupted/crashed workflows (Fase 106)
    launch {
        try {
            val recovered = recoverInterruptedWorkflows()
            if (recovered.isNotEmpty()) {
                logger.info("[STARTUP] Recovered ${recovered.size} interrupted workflows from checkpoint.")
            }
        } catch (e: Exception) {
            logger.error("[STARTUP] Error during workflow recovery: ${e.message}", e)
        }
    }

    // 5. Dynamic Model & Provider Catalog Sync from Database
    launch {
        try {
            ai.orchestree.backend.database.repositories.modelrouter.LlmProviderModelRepository.instance.syncFromDatabase()
            ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository.instance.syncFromDatabase()
            logger.info("[STARTUP] Dynamic model catalog synchronized from database.")
        } catch (e: Exception) {
            logger.warn("[STARTUP] Error during initial model catalog sync: ${e.message}")
        }
    }

    // Graceful shutdown hook
    monitor.subscribe(ApplicationStopped) {
        logger.info("Server stopping: shutting down Background Scheduler Engine...")
        backgroundScheduler?.stop()
    }

    logger.info("OrchestreeAI Server initialization complete. Ready to receive requests.")
}
