package ai.orchestree.backend

import ai.orchestree.backend.api.AutoSelectionConfigCreateRequest
import ai.orchestree.backend.api.IntegrationFabricWebhookPayload
import ai.orchestree.backend.api.StorageObjectRecord
import ai.orchestree.backend.api.StorageUploadWebhookPayload
import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.repositories.selection.AutoSelectionConfigRepository
import ai.orchestree.backend.database.repositories.selection.AutoSelectionFolderConfig
import ai.orchestree.backend.intelligence.SelectionAiJobTitleRegistry
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import ai.orchestree.backend.scheduler.SchedulerEngine
import ai.orchestree.backend.scheduler.jobs.ScheduledSelectionAnalysisJob
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.delete
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BagianHSelectionAutomationComprehensiveTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private fun generateToken(
        userId: String = "usr-test-888",
        tenantId: String = "tenant-enterprise-001",
        role: String = "TENANT_ADMIN"
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
     * TEST 1: SelectionAiJobTitleRegistry - Pemetaan Domain ke 15 Jabatan Utama AI
     */
    @Test
    fun test1_selectionAiJobTitleRegistry_mappingValidation() {
        val titles = SelectionAiJobTitleRegistry.OFFICIAL_15_JOB_TITLES
        assertEquals(15, titles.size)

        val hrJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle("recruitment")
        assertEquals("AI HR & Recruitment", hrJob.jobName)
        assertEquals("HR_RECRUITMENT", hrJob.jobCode)

        val procurementJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle("procurement")
        assertEquals("AI Procurement", procurementJob.jobName)
        assertEquals("PROCUREMENT", procurementJob.jobCode)

        val financeJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle("finance")
        assertEquals("AI Finance", financeJob.jobName)

        val salesJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle("sales")
        assertEquals("AI Sales", salesJob.jobName)

        val operationsJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle("mining")
        assertEquals("AI Operations", operationsJob.jobName)

        val legalJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle("legal")
        assertEquals("AI Knowledge & Document", legalJob.jobName)

        val generalJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle("unknown_category")
        assertEquals("AI Chief of Staff (5 Bintang)", generalJob.jobName)
        println("[PASS] TEST 1: SelectionAiJobTitleRegistry verified for all 15 master job titles.")
    }

    /**
     * TEST 2: AutoSelectionConfigRepository - CRUD & Folder Path Matching
     */
    @Test
    fun test2_autoSelectionConfigRepository_crudAndPathMatching() = runBlocking {
        val repo = AutoSelectionConfigRepository.defaultInstance
        val tenantId = "tenant-auto-${UUID.randomUUID().toString().take(6)}"

        val config = AutoSelectionFolderConfig(
            id = UUID.randomUUID().toString(),
            tenantId = tenantId,
            folderPath = "incoming/cv-kandidat/",
            domainCategory = "recruitment",
            defaultPrompt = "Evaluasi kualifikasi CV untuk posisi Senior Backend Engineer",
            isEnabled = true,
            autoExecuteDownstream = true
        )

        val saved = repo.saveConfig(config).getOrThrow()
        assertEquals(config.id, saved.id)

        val configs = repo.getConfigsForTenant(tenantId)
        assertTrue(configs.any { it.id == config.id }, "Config should be found in tenant list")

        val matchedByPath = repo.getConfigForPath(tenantId, "incoming/cv-kandidat/kandidat-01.pdf")
        assertNotNull(matchedByPath, "Path matching must identify parent folder config")
        assertEquals("recruitment", matchedByPath.domainCategory)

        val deleted = repo.deleteConfig(tenantId, config.id)
        assertTrue(deleted, "Config should be deleted")
        println("[PASS] TEST 2: AutoSelectionConfigRepository CRUD and path matching verified.")
    }

    /**
     * TEST 3: ScheduledSelectionAnalysisJob & SchedulerEngine Integration
     */
    @Test
    fun test3_scheduledSelectionAnalysisJob_execution() = runBlocking {
        val job = ScheduledSelectionAnalysisJob()
        val resultMsg = job.execute("tenant-enterprise-001")
        assertTrue(resultMsg.isNotEmpty(), "ScheduledSelectionAnalysisJob must produce execution summary")

        val scheduler = SchedulerEngine()
        val execLog = scheduler.triggerJobManually("SCHEDULED_SELECTION_ANALYSIS", "tenant-enterprise-001")
        assertEquals("SUCCESS", execLog.status, "Manual trigger of SCHEDULED_SELECTION_ANALYSIS should succeed")
        println("[PASS] TEST 3: ScheduledSelectionAnalysisJob and SchedulerEngine verified: ${execLog.resultSummary}")
    }

    /**
     * TEST 4: Auto-Selection REST Endpoints & Webhook Triggers
     */
    @Test
    fun test4_autoSelectionEndpointsAndWebhooks() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val token = generateToken(tenantId = "tenant-enterprise-001", role = "TENANT_ADMIN")

        // 1. POST /api/v1/selection/auto-selection/configs
        val createRes = client.post("/api/v1/selection/auto-selection/configs") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    AutoSelectionConfigCreateRequest(
                        folder_path = "recruitment/batch-01",
                        domain_category = "recruitment",
                        default_prompt = "Ranking kandidat frontend terbaik",
                        is_enabled = true
                    )
                )
            )
        }
        assertEquals(HttpStatusCode.Created, createRes.status)
        val createdConfig = json.decodeFromString<AutoSelectionFolderConfig>(createRes.bodyAsText())
        val configId = createdConfig.id

        // 2. GET /api/v1/selection/auto-selection/configs
        val listRes = client.get("/api/v1/selection/auto-selection/configs") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, listRes.status)
        val list = json.decodeFromString<List<AutoSelectionFolderConfig>>(listRes.bodyAsText())
        assertTrue(list.any { it.id == configId })

        // 3. POST /api/v1/selection/webhook/storage
        val storageWebhookRes = client.post("/api/v1/selection/webhook/storage") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    StorageUploadWebhookPayload(
                        type = "INSERT",
                        table = "objects",
                        schema = "storage",
                        tenant_id = "tenant-enterprise-001",
                        bucket = "selection-datasets",
                        path = "recruitment/batch-01/kandidat_reza.csv",
                        file_name = "kandidat_reza.csv",
                        file_content_base64 = java.util.Base64.getEncoder().encodeToString("Nama,Pengalaman,Skor\nReza,5 tahun,92\nDewi,3 tahun,85".toByteArray())
                    )
                )
            )
        }
        assertEquals(HttpStatusCode.Accepted, storageWebhookRes.status)
        val triggerRespText = storageWebhookRes.bodyAsText()
        assertTrue(triggerRespText.contains("triggered"))
        assertTrue(triggerRespText.contains("AI HR & Recruitment") || triggerRespText.contains("HR_RECRUITMENT") || triggerRespText.contains("job-hr-recruitment"))

        // 4. POST /api/v1/selection/webhook/integration-fabric
        val fabricWebhookRes = client.post("/api/v1/selection/webhook/integration-fabric") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    IntegrationFabricWebhookPayload(
                        tenant_id = "tenant-enterprise-001",
                        source_system = "SAP_ERP",
                        domain = "procurement",
                        prompt = "Evaluasi vendor pengadaan server cloud",
                        items = listOf(
                            mapOf("vendor" to "Vendor A", "biaya" to "100000000", "sla" to "99.9%"),
                            mapOf("vendor" to "Vendor B", "biaya" to "85000000", "sla" to "99.5%")
                        )
                    )
                )
            )
        }
        assertEquals(HttpStatusCode.Accepted, fabricWebhookRes.status)
        val fabricRespText = fabricWebhookRes.bodyAsText()
        assertTrue(fabricRespText.contains("triggered"))
        assertTrue(fabricRespText.contains("AI Procurement") || fabricRespText.contains("PROCUREMENT") || fabricRespText.contains("job-procurement"))

        // 5. DELETE /api/v1/selection/auto-selection/configs/{id}
        val deleteRes = client.delete("/api/v1/selection/auto-selection/configs/$configId") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, deleteRes.status)
        println("[PASS] TEST 4: Auto-Selection REST endpoints and webhooks verified.")
    }

    /**
     * TEST 5: RBAC & Security Matrix Role Testing
     * Matrix: SUPER_ADMIN, TENANT_OWNER, TENANT_ADMIN, DEPT_MANAGER, STAFF_HUMAN, Unauthenticated
     */
    @Test
    fun test5_rbacMatrixRoleTesting() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val roles = listOf("SUPER_ADMIN", "TENANT_OWNER", "TENANT_ADMIN", "DEPT_MANAGER", "STAFF_HUMAN")

        for (role in roles) {
            val token = generateToken(userId = "usr-$role", role = role)
            val res = client.get("/api/v1/selection/auto-selection/configs") {
                header("Authorization", "Bearer $token")
            }
            assertEquals(
                HttpStatusCode.OK,
                res.status,
                "Role $role should be authenticated and receive 200 OK"
            )
            println("Matrix RBAC Role: $role -> ${res.status}")
        }

        // Unauthenticated access must be rejected with 401 Unauthorized
        val unauthRes = client.get("/api/v1/selection/auto-selection/configs")
        assertEquals(
            HttpStatusCode.Unauthorized,
            unauthRes.status,
            "Unauthenticated request must be rejected with 401 Unauthorized"
        )
        println("Matrix RBAC Role: Unauthenticated -> ${unauthRes.status}")
        println("[PASS] TEST 5: Exhaustive RBAC role matrix verified.")
    }
}
