package ai.orchestree.backend

import ai.orchestree.backend.channels.ChannelGateway
import ai.orchestree.backend.channels.ChannelHandlingResult
import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.repositories.analytics.AnalyticsRepository
import ai.orchestree.backend.database.repositories.taskboard.TaskChecklistRecord
import ai.orchestree.backend.database.repositories.taskboard.TaskColumn
import ai.orchestree.backend.database.repositories.taskboard.TaskRepository
import ai.orchestree.backend.database.repositories.workforce.StaffProfile
import ai.orchestree.backend.database.repositories.workforce.StaffProfileRepository
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import ai.orchestree.backend.scheduler.jobs.CompetitorCrawlJob
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Fase110TaskLifecycleComprehensiveTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val json = Json { ignoreUnknownKeys = true }

    private fun generateToken(role: String, tenantId: String = "tenant-enterprise-001", userId: String = "usr-01"): String {
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
     * TEST 1: LANGKAH 2 — createOrUpdateAiSelfTask()
     * Membuktikan pembuatan self-initiated task oleh AI Agent,
     * status line update, dan activity log tercatat secara akurat.
     */
    @Test
    fun test01_createOrUpdateAiSelfTask_createsAndUpdatesCorrectly() = runBlocking {
        val taskRepo = TaskRepository.defaultInstance
        val engine = OrchestrationEngine(taskRepo = taskRepo)

        val agentId = "agent-radar-competitor"
        val execId = "exec-wf-test-001"
        val desc1 = "Memulai pemantauan harga kompetitor e-commerce"
        val target = "https://competitor-store.com/catalog"

        // 1. Create initial self task
        val task = engine.createOrUpdateAiSelfTask(
            agentId = agentId,
            workflowExecutionId = execId,
            activityDescription = desc1,
            targetInfo = target
        )

        assertNotNull(task)
        assertEquals("ai_agent_self_initiated", task.createdByType)
        assertEquals(TaskColumn.IN_PROGRESS.name, task.columnName)
        assertEquals(desc1, task.aiActivityStatusLine)
        assertEquals(target, task.thirdPartyMonitoringTarget)

        // Verifikasi activity log tercatat
        val logs = taskRepo.getActivityLogsForTask(task.id)
        assertTrue(logs.isNotEmpty(), "Activity log harus tercatat")
        assertEquals("created", logs.first().action)

        // 2. Update status line pada task yang sama via workflowExecutionId
        val desc2 = "Berhasil mengekstrak 42 produk, menganalisis selisih diskon"
        val updatedTask = engine.createOrUpdateAiSelfTask(
            agentId = agentId,
            workflowExecutionId = execId,
            activityDescription = desc2,
            targetInfo = target
        )

        assertEquals(task.id, updatedTask.id)
        assertEquals(desc2, updatedTask.aiActivityStatusLine)

        val updatedLogs = taskRepo.getActivityLogsForTask(task.id)
        assertTrue(updatedLogs.size >= 2, "Harus ada log created dan status_line_updated")
        assertTrue(updatedLogs.any { it.action == "status_line_updated" })
    }

    /**
     * TEST 2: LANGKAH 3 — Integrasi CompetitorCrawlJob ke Task Lifecycle
     * Membuktikan background job membuat task, mengisi checklist, dan mencatat activity log.
     */
    @Test
    fun test02_competitorCrawlJob_executesAndGeneratesKanbanTask() = runBlocking {
        val taskRepo = TaskRepository.defaultInstance
        val engine = OrchestrationEngine(taskRepo = taskRepo)
        val crawlJob = CompetitorCrawlJob(orchestrationEngine = engine)

        val result = crawlJob.execute(tenantId = "tenant-enterprise-001", competitorUrl = "https://tokopedia.com/demo-store")
        assertTrue(result.isNotBlank(), "Crawl job harus menghasilkan ringkasan atau status")

        // Verifikasi task tercipta di taskRepo
        val tasks = taskRepo.listByTenant("tenant-enterprise-001")
        val crawlTask = tasks.find { it.thirdPartyMonitoringTarget?.contains("tokopedia") == true || it.title.contains("Crawl") }
        assertNotNull(crawlTask, "Task monitoring kompetitor harus tercipta")
        assertEquals("ai_agent_self_initiated", crawlTask.createdByType)

        // Verifikasi checklists
        val checklists = taskRepo.getChecklistsForTask(crawlTask.id)
        assertTrue(checklists.isNotEmpty(), "Checklists harus tercipta untuk workflow task")

        // Verifikasi activity log
        val logs = taskRepo.getActivityLogsForTask(crawlTask.id)
        assertTrue(logs.isNotEmpty(), "Activity log harus tercatat")
    }

    /**
     * TEST 3: LANGKAH 4 — Inbound Telegram TASK_CREATION_COMMAND
     * Membuktikan bahwa pesan inbound dari Telegram diproses dengan benar:
     * - Resolusi staff via StaffProfileRepository
     * - Task dibuat dengan created_by_type='human', source_channel='telegram'
     * - Checklist dibuat
     * - Activity log tercatat
     * - Balasan konfirmasi terformat
     */
    @Test
    fun test03_inboundTelegram_createsTaskWithCompleteAttributes() = runBlocking {
        val taskRepo = TaskRepository.defaultInstance
        val staffRepo = StaffProfileRepository()
        val gateway = ChannelGateway(staffProfileRepo = staffRepo)

        // Simulasikan pendaftaran staff dengan chatId Telegram
        staffRepo.save(
            StaffProfile(
                id = "staff-tg-01",
                userId = "usr-budi-01",
                tenantId = "tenant-enterprise-001",
                departmentId = "dept-marketing",
                jobTitle = "Marketing Manager",
                telegramChatId = "12345678"
            )
        )

        val inboundMessage = "/task Analisis Kampanye Promo Ramadhan urgent deadline besok"
        val res: ChannelHandlingResult = gateway.handleInboundProactiveChannelMessage(
            tenantId = "tenant-enterprise-001",
            channelType = "telegram",
            senderId = "12345678",
            senderName = "Budi Hartono",
            messageText = inboundMessage,
            taskRepo = taskRepo
        )

        assertTrue(res.processed)
        assertTrue(res.success)
        assertNotNull(res.taskId)
        val createdTask = res.taskCreated
        assertNotNull(createdTask)

        // Verifikasi atribut spesifik PRD
        assertEquals("human", createdTask.createdByType)
        assertEquals("telegram", createdTask.sourceChannel)
        assertEquals("HIGH", createdTask.priority)
        assertEquals("Besok", createdTask.dueDate)
        assertTrue(createdTask.title.contains("Analisis Kampanye Promo Ramadhan"))

        // Verifikasi checklist
        val checklists = taskRepo.getChecklistsForTask(createdTask.id)
        assertEquals(3, checklists.size)

        // Verifikasi activity log
        val logs = taskRepo.getActivityLogsForTask(createdTask.id)
        assertTrue(logs.any { it.action == "created" && it.actorId == "usr-budi-01" })

        // Verifikasi konfirmasi reply format
        assertTrue(res.replyText.contains("✅ Task *"))
        assertTrue(res.replyText.contains("berhasil dibuat di Kanban Board"))
        assertTrue(res.replyText.contains("Priority: HIGH"))
    }

    /**
     * TEST 4: LANGKAH 5 — Agregasi Metrik Performa Task Harian
     * Membuktikan AnalyticsRepository.getDailyTaskPerformance mengelompokkan
     * ai_agent_self_initiated vs human dan menghitung metrik secara akurat.
     */
    @Test
    fun test04_dailyTaskPerformance_aggregatesAiVsHumanMetrics() = runBlocking {
        val taskRepo = TaskRepository.defaultInstance
        val analyticsRepo = AnalyticsRepository()

        // Buat task buatan AI
        taskRepo.create(
            title = "Task AI Self 1",
            assigneeId = "agent-radar-competitor",
            createdByType = "ai_agent_self_initiated",
            column = TaskColumn.DONE,
            tenantId = "tenant-perf-001"
        )
        taskRepo.create(
            title = "Task AI Self 2",
            assigneeId = "agent-chief-of-staff",
            createdByType = "ai_agent_self_initiated",
            column = TaskColumn.IN_PROGRESS,
            tenantId = "tenant-perf-001"
        )

        // Buat task buatan Human via Telegram
        taskRepo.create(
            title = "Task Human 1",
            assigneeId = "usr-01",
            createdByType = "human",
            column = TaskColumn.DONE,
            sourceChannel = "telegram",
            tenantId = "tenant-perf-001"
        )

        val reportList = analyticsRepo.getDailyTaskPerformance("tenant-perf-001")
        assertNotNull(reportList)
        assertTrue(reportList.isNotEmpty())
        val todayReport = reportList.first()
        assertEquals(3, todayReport.totalTasks)
        assertEquals(2, todayReport.completedTasks) // 2 tasks in DONE
        assertEquals(2, todayReport.aiSelfInitiatedTasksCount)
        assertEquals(1, todayReport.humanTasksCount)
        assertEquals(1, todayReport.telegramTasksCount)
        assertTrue(todayReport.completionRate > 60.0)
    }

    /**
     * TEST 5: Matrix Role & Condition Exhaustive Testing
     * Menguji akses endpoint tugas inbound dan checklists untuk seluruh role:
     * SUPER_ADMIN, TENANT_OWNER, TENANT_ADMIN, DEPT_MANAGER, STAFF_HUMAN, Unauthenticated
     */
    @Test
    fun test05_rbacMatrix_enforcesProperAccessControl() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val roles = listOf("SUPER_ADMIN", "TENANT_OWNER", "TENANT_ADMIN", "DEPT_MANAGER", "STAFF_HUMAN")

        for (role in roles) {
            val token = generateToken(role)
            val response = client.post("/api/v1/tenants/tasks/inbound-channel-message") {
                header("Authorization", "Bearer $token")
                header("X-Tenant-Id", "tenant-enterprise-001")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "tenantId": "tenant-enterprise-001",
                        "channel": "telegram",
                        "senderId": "12345678",
                        "senderName": "Staff Matrix Test",
                        "message": "/task Task dari Role $role"
                    }
                    """.trimIndent()
                )
            }
            assertEquals(
                HttpStatusCode.OK,
                response.status,
                "Role $role harus dapat mengirim inbound channel task message"
            )
            val body = response.bodyAsText()
            assertTrue(body.contains("Task dari Role $role"))
        }

        // Unauthenticated request must fail with 401 Unauthorized
        val unauthResponse = client.post("/api/v1/tenants/tasks/inbound-channel-message") {
            header("X-Tenant-Id", "tenant-enterprise-001")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "tenantId": "tenant-enterprise-001",
                    "channel": "telegram",
                    "senderId": "12345678",
                    "message": "Unauthenticated task"
                }
                """.trimIndent()
            )
        }
        assertEquals(
            HttpStatusCode.Unauthorized,
            unauthResponse.status,
            "Permintaan unauthenticated wajib menghasilkan 401 Unauthorized"
        )
    }
}
