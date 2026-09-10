package ai.orchestree.backend.scheduler

import ai.orchestree.backend.database.repositories.scheduler.DeadLetterQueueRepository
import ai.orchestree.backend.database.repositories.scheduler.DeadLetterRecord
import ai.orchestree.backend.database.repositories.scheduler.SchedulerJobQueueRecord
import ai.orchestree.backend.database.repositories.scheduler.SchedulerJobQueueRepository
import ai.orchestree.backend.modelrouter.HealthCheckEngine
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.parseJsonToMap
import ai.orchestree.backend.orchestration.toJson
import ai.orchestree.backend.resilience.executeWithRetry
import ai.orchestree.backend.scheduler.jobs.CompetitorCrawlJob
import ai.orchestree.backend.scheduler.jobs.ConfidenceCalibrationJob
import ai.orchestree.backend.scheduler.jobs.MemoryDecayJob
import ai.orchestree.backend.scheduler.jobs.PaymentReconciliationJob
import ai.orchestree.backend.scheduler.jobs.ProactiveDailyReportJob
import ai.orchestree.backend.scheduler.jobs.ScheduledSelectionAnalysisJob
import ai.orchestree.backend.scheduler.jobs.SyncNvidiaNimCatalogJob
import ai.orchestree.backend.scheduler.jobs.TrialExpiryJob
import ai.orchestree.backend.scheduler.jobs.CreditExpirationJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class ScheduledJobRunLog(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val jobName: String,
    val tenantId: String?,
    val status: String,
    val resultSummary: String,
    val executedAt: Long = System.currentTimeMillis()
)

class SchedulerEngine(
    private val modelRouter: ModelRouter = ModelRouter(),
    private val proactiveReportJob: ProactiveDailyReportJob = ProactiveDailyReportJob(modelRouter),
    private val competitorCrawlJob: CompetitorCrawlJob = CompetitorCrawlJob(modelRouter),
    private val trialExpiryJob: TrialExpiryJob = TrialExpiryJob(),
    private val healthCheckEngine: HealthCheckEngine = HealthCheckEngine(modelRouter),
    private val confidenceCalibrationJob: ConfidenceCalibrationJob = ConfidenceCalibrationJob(),
    private val memoryDecayJob: MemoryDecayJob = MemoryDecayJob(),
    val paymentReconciliationJob: PaymentReconciliationJob = PaymentReconciliationJob(),
    val scheduledSelectionJob: ScheduledSelectionAnalysisJob = ScheduledSelectionAnalysisJob(),
    val deadLetterQueueRepo: DeadLetterQueueRepository = DeadLetterQueueRepository(),
    val creditExpirationJob: CreditExpirationJob = CreditExpirationJob(),
    val syncNvidiaNimCatalogJob: SyncNvidiaNimCatalogJob = SyncNvidiaNimCatalogJob(),
    val schedulerJobQueueRepo: SchedulerJobQueueRepository = SchedulerJobQueueRepository(),
    private val tenantRepo: ai.orchestree.backend.database.repositories.identity.TenantRepository = ai.orchestree.backend.database.repositories.identity.TenantRepository.instance
) {
    private val logger = LoggerFactory.getLogger(SchedulerEngine::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val executionLogs = CopyOnWriteArrayList<ScheduledJobRunLog>()

    @Volatile
    private var isRunning = false

    /**
     * Executes any scheduled job with automatic retry (maxAttempts = 3).
     * If all attempts fail, moves the failed job to dead_letter_queue.
     */
    suspend fun executeWithDlq(
        jobType: String,
        tenantId: String? = null,
        payload: Map<String, Any> = emptyMap(),
        maxAttempts: Int = 3,
        block: suspend () -> String
    ): ScheduledJobRunLog {
        try {
            val summary = executeWithRetry(
                maxAttempts = maxAttempts,
                initialDelayMs = 100L,
                backoffMultiplier = 1.5,
                retryableExceptions = setOf(Exception::class)
            ) {
                block()
            }
            val log = ScheduledJobRunLog(
                jobName = jobType,
                tenantId = tenantId,
                status = "SUCCESS",
                resultSummary = summary
            )
            recordLog(jobType, tenantId, "SUCCESS", summary)
            return log
        } catch (e: Throwable) {
            logger.error("[SCHEDULER:DLQ] Job $jobType failed after $maxAttempts attempts: ${e.message}", e)
            val fullPayload = payload.toMutableMap()
            if (tenantId != null && !fullPayload.containsKey("tenantId")) {
                fullPayload["tenantId"] = tenantId
            }
            val dlqId = UUID.randomUUID().toString()
            val dlqRecord = DeadLetterRecord(
                id = dlqId,
                jobType = jobType,
                originalPayload = fullPayload.toJson(),
                failureReason = e.message ?: e.javaClass.simpleName,
                failedAt = System.currentTimeMillis(),
                reprocessed = false
            )
            deadLetterQueueRepo.insert(dlqRecord)

            val summary = "Job failed after $maxAttempts attempts. Moved to Dead-Letter Queue (ID: $dlqId): ${e.message}"
            val log = ScheduledJobRunLog(
                jobName = jobType,
                tenantId = tenantId,
                status = "DEAD_LETTER_QUEUE",
                resultSummary = summary
            )
            recordLog(jobType, tenantId, "DEAD_LETTER_QUEUE", summary)
            return log
        }
    }

    /**
     * LANGKAH 6.1: Start background scheduling loop.
     * Runs continuously as long as the server is running, independent of whether client app is open.
     */
    fun start(
        proactiveIntervalMs: Long = 60_000 * 60 * 24, // 24 hours
        healthCheckIntervalMs: Long = 60_000 * 15,    // 15 minutes
        trialCheckIntervalMs: Long = 60_000 * 60 * 12, // 12 hours
        paymentReconciliationIntervalMs: Long = 60_000 * 15 // 15 minutes (Fase 110 Bagian B)
    ) {
        if (isRunning) return
        isRunning = true
        logger.info("[SCHEDULER] Starting OrchestreeAI Enterprise Background Scheduler Engine with DLQ protection...")

        // 1. Proactive Daily Report Background Loop
        activeJobs["PROACTIVE_BRIEFING"] = scope.launch {
            while (isActive) {
                val activeTenants = tenantRepo.getActiveTenantIds()
                if (activeTenants.isEmpty()) {
                    logger.info("[SCHEDULER] No active tenants found for proactive briefing")
                } else {
                    for (tenantId in activeTenants) {
                        executeWithDlq(
                            jobType = "PROACTIVE_DAILY",
                            tenantId = tenantId,
                            payload = mapOf("tenantId" to tenantId)
                        ) {
                            proactiveReportJob.execute(tenantId)
                        }
                    }
                }
                delay(proactiveIntervalMs)
            }
        }

        // 2. Health Check Background Loop
        activeJobs["HEALTH_CHECK"] = scope.launch {
            while (isActive) {
                executeWithDlq(jobType = "HEALTH_CHECK", tenantId = null, payload = emptyMap()) {
                    val healthResults = healthCheckEngine.checkAll()
                    val healthyCount = healthResults.count { it.isHealthy }
                    "$healthyCount/${healthResults.size} providers healthy"
                }
                delay(healthCheckIntervalMs)
            }
        }

        // 3. Trial Expiry Background Loop
        activeJobs["TRIAL_EXPIRY"] = scope.launch {
            while (isActive) {
                executeWithDlq(jobType = "TRIAL_EXPIRY", tenantId = null, payload = emptyMap()) {
                    val expiredCount = trialExpiryJob.execute()
                    "Processed $expiredCount expired subscriptions"
                }
                delay(trialCheckIntervalMs)
            }
        }

        // 4. Memory Relevance Decay Loop (PRD Master Bagian 17.2)
        activeJobs["MEMORY_DECAY"] = scope.launch {
            while (isActive) {
                executeWithDlq(jobType = "MEMORY_DECAY", tenantId = null, payload = emptyMap()) {
                    memoryDecayJob.execute()
                }
                delay(60_000 * 60 * 24) // 24 hours
            }
        }

        // 5. Payment Reconciliation Anomaly Detection Loop (Fase 110 Bagian B, runs every 15 minutes)
        activeJobs["PAYMENT_RECONCILIATION"] = scope.launch {
            while (isActive) {
                executeWithDlq(jobType = "PAYMENT_RECONCILIATION", tenantId = null, payload = emptyMap()) {
                    paymentReconciliationJob.execute()
                }
                delay(paymentReconciliationIntervalMs)
            }
        }

        // 6. Credit Expiration Loop (Fase 114, runs daily to expire subscription balances)
        activeJobs["CREDIT_EXPIRATION"] = scope.launch {
            while (isActive) {
                executeWithDlq(jobType = "CREDIT_EXPIRATION", tenantId = null, payload = emptyMap()) {
                    val count = creditExpirationJob.execute()
                    "Expired subscription balances for $count tenant(s)"
                }
                delay(60_000 * 60 * 24) // 24 hours
            }
        }

        // 7. Dynamic Catalog Sync Loop (NVIDIA NIM, runs daily to sync model catalog)
        activeJobs["NVIDIA_NIM_CATALOG_SYNC"] = scope.launch {
            while (isActive) {
                executeWithDlq(jobType = "NVIDIA_NIM_CATALOG_SYNC", tenantId = null, payload = emptyMap()) {
                    val count = syncNvidiaNimCatalogJob.execute()
                    "Synced $count models from NVIDIA NIM catalog"
                }
                delay(60_000 * 60 * 24) // 24 hours
            }
        }
    }

    /**
     * LANGKAH 3.2: DATABASE-LEVEL LOCKING (SELECT ... FOR UPDATE SKIP LOCKED)
     * Mengklaim job scheduler berikutnya secara atomik untuk mencegah race condition antar pod.
     */
    suspend fun claimNextSchedulerJob(podIdentifier: String = System.getenv("HOSTNAME") ?: "pod-local"): SchedulerJobQueueRecord? {
        return schedulerJobQueueRepo.claimNextJob(podIdentifier)
    }

    /**
     * Memproses satu job dari scheduler queue yang berhasil diklaim secara aman.
     */
    suspend fun processNextClaimedJob(podIdentifier: String = System.getenv("HOSTNAME") ?: "pod-local"): ScheduledJobRunLog? {
        val job = claimNextSchedulerJob(podIdentifier) ?: return null
        logger.info("[SCHEDULER:LOCK] Pod '$podIdentifier' safely claimed job ${job.id} (type: ${job.jobType}) via FOR UPDATE SKIP LOCKED")
        val payload = parseJsonToMap(job.payload)
        val tenantId = job.tenantId ?: payload["tenantId"]?.toString() ?: "system"

        val runLog = executeWithDlq(jobType = job.jobType, tenantId = tenantId, payload = payload) {
            when (job.jobType) {
                "PROACTIVE_BRIEFING", "PROACTIVE_DAILY" -> proactiveReportJob.execute(tenantId)
                "COMPETITOR_CRAWL" -> {
                    val url = payload["url"]?.toString() ?: "https://example.com"
                    competitorCrawlJob.execute(tenantId, url)
                }
                "HEALTH_CHECK" -> {
                    val results = healthCheckEngine.checkAll()
                    val healthyCount = results.count { it.isHealthy }
                    "$healthyCount/${results.size} providers healthy"
                }
                "TRIAL_EXPIRY" -> "Expired: ${trialExpiryJob.execute()}"
                "CONFIDENCE_CALIBRATION" -> {
                    val records = confidenceCalibrationJob.calibrateConfidenceScores(tenantId)
                    "Calibrated ${records.size} buckets"
                }
                "MEMORY_DECAY" -> memoryDecayJob.execute(tenantId)
                "PAYMENT_RECONCILIATION" -> paymentReconciliationJob.execute()
                "SCHEDULED_SELECTION_ANALYSIS", "AUTO_SELECTION_BATCH" -> scheduledSelectionJob.execute(tenantId, payload)
                "CREDIT_EXPIRATION" -> {
                    val count = creditExpirationJob.execute()
                    "Expired subscription balances for $count tenant(s)"
                }
                else -> throw IllegalArgumentException("Unsupported queue job type: ${job.jobType}")
            }
        }

        if (runLog.status == "SUCCESS") {
            schedulerJobQueueRepo.markCompleted(job.id, runLog.resultSummary)
        } else {
            schedulerJobQueueRepo.markFailed(job.id, runLog.resultSummary, requeue = false)
        }
        return runLog
    }

    /**
     * Trigger immediate execution for testing or on-demand requests
     */
    suspend fun triggerJobManually(
        jobName: String,
        tenantId: String = "tenant-manual",
        payload: Map<String, Any> = emptyMap(),
        forceTestFailure: Boolean = false,
        simulateFailure: Boolean = false
    ): ScheduledJobRunLog {
        val shouldFail = forceTestFailure || simulateFailure
        logger.info("[SCHEDULER] Manually triggering job: $jobName for tenant $tenantId (forceTestFailure: $shouldFail)")
        return executeWithDlq(jobType = jobName, tenantId = tenantId, payload = payload) {
            if (shouldFail) {
                throw java.io.IOException("Simulated repeated failure for job $jobName (exhausted 3 attempts)")
            }
            when (jobName) {
                "PROACTIVE_BRIEFING", "PROACTIVE_DAILY" -> {
                    proactiveReportJob.execute(tenantId)
                }
                "COMPETITOR_CRAWL" -> {
                    val url = payload["url"]?.toString() ?: "https://example.com"
                    competitorCrawlJob.execute(tenantId, url)
                }
                "TRIAL_EXPIRY" -> {
                    val count = trialExpiryJob.execute()
                    "Expired: $count"
                }
                "HEALTH_CHECK" -> {
                    val results = healthCheckEngine.checkAll()
                    val healthyCount = results.count { it.isHealthy }
                    "$healthyCount healthy"
                }
                "CONFIDENCE_CALIBRATION" -> {
                    val records = confidenceCalibrationJob.calibrateConfidenceScores(tenantId)
                    val uncalibratedCount = records.count { !it.isCalibrated }
                    if (uncalibratedCount > 0) {
                        "Calibrated ${records.size} buckets ($uncalibratedCount miscalibrated with deviation > 15%)"
                    } else {
                        "Calibrated ${records.size} buckets (all within acceptable deviation <= 15%)"
                    }
                }
                "MEMORY_DECAY" -> {
                    memoryDecayJob.execute(tenantId)
                }
                "PAYMENT_RECONCILIATION" -> {
                    paymentReconciliationJob.execute()
                }
                "SCHEDULED_SELECTION_ANALYSIS", "AUTO_SELECTION_BATCH" -> {
                    scheduledSelectionJob.execute(tenantId, payload)
                }
                "CREDIT_EXPIRATION" -> {
                    val count = creditExpirationJob.execute()
                    "Expired subscription balances for $count tenant(s)"
                }
                else -> {
                    throw IllegalArgumentException("Job $jobName not found")
                }
            }
        }
    }

    /**
     * Reprocesses a failed job from the dead letter queue by ID.
     */
    suspend fun reprocessDeadLetterItem(id: String): Result<String> {
        val record = deadLetterQueueRepo.getById(id)
            ?: return Result.failure(IllegalArgumentException("Dead-letter item $id not found"))

        val payload = parseJsonToMap(record.originalPayload)
        val tenantId = payload["tenantId"]?.toString() ?: "tenant-reprocess"

        logger.info("[SCHEDULER:DLQ] Reprocessing item $id (jobType: ${record.jobType}, tenantId: $tenantId)...")

        return try {
            val resultSummary = when (record.jobType) {
                "PROACTIVE_BRIEFING", "PROACTIVE_DAILY" -> proactiveReportJob.execute(tenantId)
                "COMPETITOR_CRAWL" -> {
                    val url = payload["url"]?.toString() ?: "https://example.com"
                    competitorCrawlJob.execute(tenantId, url)
                }
                "HEALTH_CHECK" -> {
                    val results = healthCheckEngine.checkAll()
                    val healthyCount = results.count { it.isHealthy }
                    "$healthyCount/${results.size} providers healthy"
                }
                "TRIAL_EXPIRY" -> "Expired: ${trialExpiryJob.execute()}"
                "CONFIDENCE_CALIBRATION" -> {
                    val records = confidenceCalibrationJob.calibrateConfidenceScores(tenantId)
                    "Calibrated ${records.size} buckets"
                }
                "MEMORY_DECAY" -> memoryDecayJob.execute(tenantId)
                "PAYMENT_RECONCILIATION" -> paymentReconciliationJob.execute()
                "SCHEDULED_SELECTION_ANALYSIS", "AUTO_SELECTION_BATCH" -> scheduledSelectionJob.execute(tenantId, payload)
                else -> throw IllegalArgumentException("Cannot reprocess unsupported job type: ${record.jobType}")
            }

            deadLetterQueueRepo.markReprocessed(id, resultSummary)
            recordLog(record.jobType, tenantId, "REPROCESSED_FROM_DLQ", "Item $id successfully reprocessed: $resultSummary")
            logger.info("[SCHEDULER:DLQ] Reprocessing SUCCEEDED for item $id: $resultSummary")
            Result.success(resultSummary)
        } catch (e: Exception) {
            logger.error("[SCHEDULER:DLQ] Failed reprocessing dead letter item $id: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getLogs(): List<ScheduledJobRunLog> = executionLogs.toList()

    private fun recordLog(jobName: String, tenantId: String?, status: String, summary: String) {
        val log = ScheduledJobRunLog(
            jobName = jobName,
            tenantId = tenantId,
            status = status,
            resultSummary = summary
        )
        executionLogs.add(log)
        while (executionLogs.size > 100) {
            executionLogs.removeAt(0)
        }
    }

    fun stop() {
        isRunning = false
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        scope.cancel()
        logger.info("[SCHEDULER] Scheduler Engine stopped.")
    }
}
