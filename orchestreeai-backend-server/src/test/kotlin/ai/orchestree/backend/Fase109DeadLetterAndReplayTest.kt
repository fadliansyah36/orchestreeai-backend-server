package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.repositories.orchestration.WorkflowExecutionRepository
import ai.orchestree.backend.database.repositories.scheduler.DeadLetterQueueRepository
import ai.orchestree.backend.database.repositories.scheduler.DeadLetterRecord
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.NodeExecutionResult
import ai.orchestree.backend.orchestration.NodeExecutionStatus
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.orchestration.WorkflowExecution
import ai.orchestree.backend.orchestration.WorkflowNodeType
import ai.orchestree.backend.orchestration.nodes.GenericStepWorkflowNode
import ai.orchestree.backend.orchestration.toJson
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import ai.orchestree.backend.scheduler.SchedulerEngine
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Fase109DeadLetterAndReplayTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    private fun generateValidToken(
        userId: String = "usr-admin-test",
        tenantId: String = "tenant-enterprise-001",
        role: String = "SUPER_ADMIN"
    ): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("sub", userId)
            .withClaim("tenant_id", tenantId)
            .withClaim("role", role)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600 * 1000))
            .sign(algorithm)
    }

    /**
     * LANGKAH 1 — DEAD-LETTER QUEUE UNTUK JOB YANG GAGAL BERULANG
     * 1.1. Untuk SETIAP Scheduler Job (Proactive Daily, Competitor Crawl, Health Check),
     *      JIKA gagal melebihi maxAttempts (3x), WAJIB masuk dead_letter_queue.
     */
    @Test
    fun testSchedulerJobsExhaustingAttemptsLandInDeadLetterQueue() = runBlocking {
        val dlqRepo = DeadLetterQueueRepository()
        dlqRepo.clearInMemory()

        val scheduler = SchedulerEngine(deadLetterQueueRepo = dlqRepo)

        // 1. Uji Proactive Daily Job gagal 3x berturut-turut
        val proactiveLog = scheduler.triggerJobManually(
            jobName = "PROACTIVE_DAILY",
            tenantId = "tenant-fail-dlq-001",
            payload = mapOf("reportType" to "EXECUTIVE_MORNING"),
            simulateFailure = true
        )
        assertEquals("DEAD_LETTER_QUEUE", proactiveLog.status)
        assertTrue(proactiveLog.resultSummary.contains("Moved to Dead-Letter Queue"))

        // 2. Uji Competitor Crawl Job gagal 3x berturut-turut
        val crawlLog = scheduler.triggerJobManually(
            jobName = "COMPETITOR_CRAWL",
            tenantId = "tenant-fail-dlq-001",
            payload = mapOf("url" to "https://competitor-unresponsive.com"),
            simulateFailure = true
        )
        assertEquals("DEAD_LETTER_QUEUE", crawlLog.status)
        assertTrue(crawlLog.resultSummary.contains("Moved to Dead-Letter Queue"))

        // 3. Uji Health Check Job gagal 3x berturut-turut
        val healthLog = scheduler.triggerJobManually(
            jobName = "HEALTH_CHECK",
            tenantId = "tenant-fail-dlq-001",
            simulateFailure = true
        )
        assertEquals("DEAD_LETTER_QUEUE", healthLog.status)
        assertTrue(healthLog.resultSummary.contains("Moved to Dead-Letter Queue"))

        // Verifikasi seluruh job tersimpan di dead_letter_queue
        val dlqItems = dlqRepo.listAll(includeReprocessed = true)
        assertEquals(3, dlqItems.size)

        val proactiveDlq = dlqItems.firstOrNull { it.jobType == "PROACTIVE_DAILY" }
        assertNotNull(proactiveDlq)
        assertFalse(proactiveDlq.reprocessed)
        assertTrue(proactiveDlq.failureReason!!.contains("Simulated repeated failure"))
        assertTrue(proactiveDlq.originalPayload.contains("tenant-fail-dlq-001"))

        val crawlDlq = dlqItems.firstOrNull { it.jobType == "COMPETITOR_CRAWL" }
        assertNotNull(crawlDlq)
        assertFalse(crawlDlq.reprocessed)

        val healthDlq = dlqItems.firstOrNull { it.jobType == "HEALTH_CHECK" }
        assertNotNull(healthDlq)
        assertFalse(healthDlq.reprocessed)
    }

    /**
     * LANGKAH 1.2 — Reprocess Manual dari Super Admin
     * Item di dead_letter_queue dapat di-reprocess, status menjadi reprocessed = true,
     * dan reprocessResult tercatat dengan benar.
     */
    @Test
    fun testSuperAdminManualReprocessFromDeadLetterQueue() = runBlocking {
        val dlqRepo = DeadLetterQueueRepository()
        dlqRepo.clearInMemory()

        val scheduler = SchedulerEngine(deadLetterQueueRepo = dlqRepo)

        // Masukkan item gagal ke DLQ
        val failedLog = scheduler.triggerJobManually(
            jobName = "PROACTIVE_DAILY",
            tenantId = "tenant-reprocess-test",
            payload = mapOf("tenantId" to "tenant-reprocess-test"),
            simulateFailure = true
        )
        assertEquals("DEAD_LETTER_QUEUE", failedLog.status)

        val itemsBefore = dlqRepo.listAll(includeReprocessed = false)
        assertEquals(1, itemsBefore.size)
        val dlqItem = itemsBefore.first()
        assertFalse(dlqItem.reprocessed)

        // Super Admin melakukan Reprocess manual
        val reprocessResult = scheduler.reprocessDeadLetterItem(dlqItem.id)
        assertTrue(reprocessResult.isSuccess, "Reprocess should succeed")
        val summary = reprocessResult.getOrThrow()
        assertTrue(summary.isNotEmpty())

        // Verifikasi item di repository telah ditandai reprocessed = true
        val updatedItem = dlqRepo.getById(dlqItem.id)
        assertNotNull(updatedItem)
        assertTrue(updatedItem.reprocessed)
        assertNotNull(updatedItem.reprocessedAt)
        assertNotNull(updatedItem.reprocessResult)
        assertEquals(summary, updatedItem.reprocessResult)

        // Verifikasi filter includeReprocessed = false tidak lagi menyertakan item ini
        val pendingItems = dlqRepo.listAll(includeReprocessed = false)
        assertTrue(pendingItems.isEmpty())
    }

    /**
     * LANGKAH 2 — DETERMINISTIC REPLAY UNTUK DEBUGGING (2.1)
     * - Mengambil current_state_snapshot dari eksekusi LAMA
     * - Jalankan ULANG dengan input yang PERSIS SAMA di environment terisolasi
     * - Menghasilkan OUTPUT YANG SAMA PERSIS (bukti determinism)
     * - Tanpa efek samping nyata (side effects prevented)
     */
    @Test
    fun testDeterministicReplayInIsolatedSandboxProducesIdenticalOutput() = runBlocking {
        val wfRepo = WorkflowExecutionRepository()
        val engine = OrchestrationEngine(workflowExecutionRepo = wfRepo)

        val tenantId = "tenant-enterprise-debug"
        val prompt = "Generate Q3 comprehensive executive briefing"

        // 1. Eksekusi pertama (LAMA)
        val initialRun = engine.runWorkflow(
            tenantId = tenantId,
            workflowDefId = "wf-chief-of-staff-briefing",
            prompt = prompt
        )
        assertEquals("COMPLETED", initialRun.status)
        val originalOutput = initialRun.finalOutput
        assertTrue(originalOutput.isNotEmpty())

        val originalExecution = wfRepo.getById(initialRun.executionId)
        assertNotNull(originalExecution)
        assertNotNull(originalExecution.currentStateSnapshot)

        // 2. Jalankan DETERMINISTIC REPLAY via Replay Sandbox Engine
        val replayResult = engine.replayWorkflow(initialRun.executionId)

        // Verifikasi Determinism & Hasil Identik
        assertEquals("COMPLETED", replayResult.status)
        assertTrue(replayResult.isDeterministicMatch, "Replay must produce identical deterministic match")
        assertEquals(originalOutput.trim(), replayResult.replayOutput.trim())
        assertEquals(originalRunOutputSanitized(originalOutput), originalRunOutputSanitized(replayResult.replayOutput))

        // Verifikasi Sandbox Isolation (Tidak mengirim WhatsApp / Telegram / Slack sungguhan)
        val sandbox = replayResult.sandboxDetails
        assertEquals("ISOLATED_SANDBOX", sandbox["environment"])
        assertEquals("true", sandbox["side_effects_prevented"])
        assertEquals("SUPPRESSED_NO_EXTERNAL_CALL", sandbox["whatsapp_dispatch"])
        assertEquals("SUPPRESSED_NO_EXTERNAL_CALL", sandbox["telegram_dispatch"])
        assertEquals("SUPPRESSED_NO_EXTERNAL_CALL", sandbox["slack_dispatch"])
        assertEquals("SUPPRESSED_NO_EXTERNAL_CALL", sandbox["payment_gateway"])
        assertEquals(initialRun.executionId, sandbox["replayed_from_execution"])

        // Verifikasi nodeRuns pada Replay tetap tereksekusi lengkap 5 node
        assertEquals(5, replayResult.nodeRuns.size)
        val deliverNodeRun = replayResult.nodeRuns.last()
        assertEquals("n5-distribute-channels", deliverNodeRun.nodeId)
        assertTrue(
            deliverNodeRun.output!!.contains("[SANDBOX_REPLAY]") ||
            deliverNodeRun.output!!.contains("External broadcast suppressed")
        )
    }

    private fun originalRunOutputSanitized(out: String): String {
        return out.trim().replace("\r\n", "\n")
    }

    /**
     * 1.2 & 2.1 HTTP Endpoint Test:
     * - GET /api/v1/admin/dead-letter-queue (SuperAdmin only, RBAC enforced)
     * - POST /api/v1/admin/dead-letter-queue/{id}/reprocess (SuperAdmin reprocess)
     * - GET /api/v1/admin/workflow-executions
     * - POST /api/v1/admin/workflow-executions/{id}/replay (Deterministic replay sandbox)
     */
    @Test
    fun testAdminApiEndpointsForDeadLetterQueueAndReplaySandbox() = testApplication {
        application {
            configureSerialization()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        val superAdminToken = generateValidToken(role = "SUPER_ADMIN")

        // 1. Unauthenticated -> 401 Unauthorized (Fail-Closed)
        val unauthRes = client.get("/api/v1/admin/dead-letter-queue")
        assertEquals(HttpStatusCode.Unauthorized, unauthRes.status)

        // 2. Non-SuperAdmin (Staff Human) -> 403 Forbidden (RBAC Enforced)
        val staffToken = generateValidToken(role = "STAFF_HUMAN")
        val forbiddenRes = client.get("/api/v1/admin/dead-letter-queue") {
            header("Authorization", "Bearer $staffToken")
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenRes.status)

        // 3. SuperAdmin -> 200 OK & List items
        val dlqRes = client.get("/api/v1/admin/dead-letter-queue?includeReprocessed=true") {
            header("Authorization", "Bearer $superAdminToken")
        }
        assertEquals(HttpStatusCode.OK, dlqRes.status)
        val dlqBody = dlqRes.bodyAsText()
        assertTrue(dlqBody.contains("dlq-sample-proactive-001") || dlqBody.contains("PROACTIVE_DAILY"))

        // 4. SuperAdmin reprocess dead-letter item -> 200 OK
        val reprocessRes = client.post("/api/v1/admin/dead-letter-queue/dlq-sample-proactive-001/reprocess") {
            header("Authorization", "Bearer $superAdminToken")
        }
        assertEquals(HttpStatusCode.OK, reprocessRes.status)
        assertTrue(reprocessRes.bodyAsText().contains("REPROCESSED"))

        // 5. SuperAdmin list workflow executions -> 200 OK
        val wfRes = client.get("/api/v1/admin/workflow-executions") {
            header("Authorization", "Bearer $superAdminToken")
        }
        assertEquals(HttpStatusCode.OK, wfRes.status)

        // 6. SuperAdmin replay sample execution -> 200 OK & isolated sandbox flags
        val replayRes = client.post("/api/v1/admin/workflow-executions/wf-exec-sample-001/replay") {
            header("Authorization", "Bearer $superAdminToken")
        }
        assertEquals(HttpStatusCode.OK, replayRes.status)
        val replayBody = replayRes.bodyAsText()
        assertTrue(replayBody.contains("ISOLATED_SANDBOX"))
        assertTrue(replayBody.contains("SUPPRESSED_NO_EXTERNAL_CALL"))
    }
}
