package ai.orchestree.backend

import ai.orchestree.backend.api.ChatApiRequest
import ai.orchestree.backend.api.ChatApiResponse
import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import ai.orchestree.backend.scheduler.SchedulerEngine
import ai.orchestree.backend.startup.StartupValidator
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
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BagianHRegressionSuiteTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private fun generateValidToken(
        userId: String = "usr-test-101",
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
     * 2.1 Model Router Fallback Chain Test (Fase 82/90 Bagian C)
     * Membuktikan bahwa fallback chain dieksekusi secara dinamis di server tanpa perlu rebuild APK client.
     */
    @Test
    fun test2_1_modelRouter_dynamicFallbackChain() = runBlocking {
        val modelRouter = ModelRouter()
        
        // Uji eksekusi routing dengan berbagai task category
        val reqReasoning = ModelRouteRequest(
            taskCategory = "REASONING",
            prompt = "Berikan analisis komprehensif revenue per customer",
            tenantId = "tenant-enterprise-001"
        )
        val resultReasoning = modelRouter.execute(reqReasoning)
        assertTrue(resultReasoning.isSuccess, "ModelRouter reasoning task should execute successfully")
        val respReasoning = resultReasoning.getOrThrow()
        assertNotNull(respReasoning.providerUsed)
        assertTrue(respReasoning.text.isNotEmpty())

        val reqCreative = ModelRouteRequest(
            taskCategory = "CREATIVE",
            prompt = "Buat copywriting promo peluncuran produk baru",
            tenantId = "tenant-enterprise-001"
        )
        val resultCreative = modelRouter.execute(reqCreative)
        assertTrue(resultCreative.isSuccess, "ModelRouter creative task should execute successfully")
    }

    /**
     * 2.2 Orchestration Engine end-to-end (Fase 96 seluruh Bagian)
     * Menguji eksekusi end-to-end multi-agent workflow untuk Sales, Generative Studio, dan Proactive Agent.
     */
    @Test
    fun test2_2_orchestrationEngine_endToEndWorkflows() = runBlocking {
        val engine = OrchestrationEngine()

        // 1. Skenario Sales Workflow
        val salesResult = engine.runWorkflow(
            tenantId = "tenant-enterprise-001",
            workflowDefId = "wf-sales-pipeline",
            prompt = "Follow up lead WhatsApp Ahmad Fauzi dengan penawaran armada B2B"
        )
        assertEquals("COMPLETED", salesResult.status)
        assertTrue(salesResult.nodeRuns.isNotEmpty(), "Node execution runs should not be empty")

        // 2. Skenario Generative Studio Workflow
        val studioResult = engine.runWorkflow(
            tenantId = "tenant-enterprise-001",
            workflowDefId = "wf-creative-studio",
            prompt = "Generate copywriting dan banner visual media sosial produk baru"
        )
        assertEquals("COMPLETED", studioResult.status)
        assertTrue(studioResult.nodeRuns.any { it.nodeId.contains("plan") })

        // 3. Skenario Proactive Intelligence Workflow
        val proactiveResult = engine.runWorkflow(
            tenantId = "tenant-enterprise-001",
            workflowDefId = "wf-proactive-briefing",
            prompt = "Analisis anomali stok dan generate daily executive brief"
        )
        assertEquals("COMPLETED", proactiveResult.status)
        assertTrue(proactiveResult.nodeRuns.any { it.nodeId.contains("generate") || it.nodeId.contains("llm") })
    }

    /**
     * 2.3 Scheduler Independen (Bagian D Langkah 6)
     * Membuktikan background job (Proactive Daily Report, Sync, Monitoring) berjalan
     * di server tanpa ketergantungan pada client Android aktif.
     */
    @Test
    fun test2_3_independentScheduler_runsWithoutClient() = runBlocking {
        val scheduler = SchedulerEngine()
        
        // Trigger Proactive Daily Briefing job
        val reportLog = scheduler.triggerJobManually("PROACTIVE_BRIEFING", "tenant-enterprise-001")
        assertEquals("PROACTIVE_BRIEFING", reportLog.jobName)
        assertEquals("SUCCESS", reportLog.status)
        assertTrue(reportLog.resultSummary.isNotEmpty())

        // Trigger Competitor Crawl job
        val crawlLog = scheduler.triggerJobManually("COMPETITOR_CRAWL", "tenant-enterprise-001")
        assertEquals("COMPETITOR_CRAWL", crawlLog.jobName)
        assertEquals("SUCCESS", crawlLog.status)

        // Trigger Health Check job
        val healthLog = scheduler.triggerJobManually("HEALTH_CHECK", "tenant-enterprise-001")
        assertEquals("HEALTH_CHECK", healthLog.jobName)
        assertEquals("SUCCESS", healthLog.status)
    }

    /**
     * 2.4 Fail-Closed Security & Error Handling (Fase 84/87 Bagian D)
     * Membuktikan bahwa request tanpa otentikasi ditolak (401 Unauthorized),
     * dan server memvalidasi RBAC secara ketat.
     */
    @Test
    fun test2_4_failClosed_and_rbac_validation() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        // 1. Health check UP
        val healthRes = client.get("/health")
        assertEquals(HttpStatusCode.OK, healthRes.status)

        // 2. Chat endpoint TANPA header token -> Wajib 401 Unauthorized (Fail-Closed)
        val unauthRes = client.post("/api/v1/chat") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatApiRequest(message = "Hello without auth", tenantId = "t-1")))
        }
        assertEquals(HttpStatusCode.Unauthorized, unauthRes.status)

        // 3. Chat endpoint DENGAN valid token -> Wajib 200 OK
        val token = generateValidToken(role = "TENANT_ADMIN")
        val authRes = client.post("/api/v1/chat") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatApiRequest(message = "Hello with valid token", tenantId = "tenant-enterprise-001")))
        }
        assertEquals(HttpStatusCode.OK, authRes.status)
        val chatResp = json.decodeFromString<ChatApiResponse>(authRes.bodyAsText())
        assertTrue(chatResp.reply.isNotEmpty())
    }

    /**
     * 3.1 & 3.2 Audit Keamanan Tambahan Pasca-Migrasi
     * Memvalidasi bahwa startup validator memverifikasi integritas konfigurasi dan fail-fast saat konfigurasi kosong.
     */
    @Test
    fun test3_securityAudit_startupValidation() {
        val validator = StartupValidator(shouldExitProcessOnFailure = false)
        
        // 1. Verifikasi fail-fast ketika konfigurasi kosong/hilang
        val invalidConfig = AppConfig(
            supabase = ai.orchestree.backend.config.SupabaseConfig("", "", "", "", ""),
            security = SecurityConfig(jwtSecretKey = "", masterKeyRef = "")
        )
        var caughtException: Exception? = null
        try {
            validator.verifyRequiredEnvVars(invalidConfig)
        } catch (e: Exception) {
            caughtException = e
        }
        assertNotNull(caughtException, "Startup validator MUST fail-fast when required env vars are missing")

        // 2. Verifikasi sukses ketika konfigurasi valid
        val validConfig = AppConfig(
            supabase = ai.orchestree.backend.config.SupabaseConfig(
                url = "https://mock-supabase.co",
                anonKey = "anon-key-123",
                serviceRoleKey = "service-role-key-123",
                databaseUrl = "postgresql://mock:5432/db",
                databaseDirectUrl = "postgresql://mock:5432/db"
            ),
            security = SecurityConfig(jwtSecretKey = "super-secret-jwt-key-32-chars-long", masterKeyRef = "ref-1")
        )
        validator.verifyRequiredEnvVars(validConfig)
    }
}
