package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.repositories.taskboard.TaskRepository
import ai.orchestree.backend.database.repositories.workforce.UserPersonaRepository
import ai.orchestree.backend.database.repositories.workforce.UserRepository
import ai.orchestree.backend.database.repositories.workforce.ProactiveCollaborationScopeRepository
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskBoardScopeFilteringRegressionTest {

    private val taskRepo = TaskRepository.defaultInstance
    private val userRepo = UserRepository.defaultInstance
    private val userPersonaRepo = UserPersonaRepository.defaultInstance
    private val scopeRepo = ProactiveCollaborationScopeRepository.defaultInstance

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    private fun generateToken(userId: String, role: String, tenantId: String = "tenant-enterprise-001"): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("sub", userId)
            .withClaim("user_id", userId)
            .withClaim("tenant_id", tenantId)
            .withClaim("role", role)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600 * 1000))
            .sign(algorithm)
    }

    @Test
    fun testScenario1_StaffSalesLogin_TaskFiltering_StrictScope() = runBlocking {
        // Staff Sales login
        val userId = "usr-staff-sales-01"
        val tasks = taskRepo.getTasksForUser(userId, "default")
        val taskIds = tasks.map { it.id }.toSet()

        println("RAW QUERY RESULT [Staff Sales ($userId)]: ${tasks.map { "${it.id} (${it.title}) [Dept: ${it.departmentId}]" }}")

        // 1. Task dirinya sendiri (ada)
        assertTrue(taskIds.contains("tsk-staff-sales-01"), "Task personal diri sendiri wajib muncul")

        // 2. Task timnya sendiri (ada)
        assertTrue(taskIds.contains("tsk-team-sales-01"), "Task tim field sales wajib muncul")

        // 3. Task AI Sales / AI Marketing (ada — masuk scope kolaborasi Sales)
        assertTrue(taskIds.contains("tsk-ai-crawl-01"), "Task AI Marketing/Sales (in scope) wajib muncul")

        // 4. Task Finance / Task Dept Lain (TIDAK BOLEH MUNCUL)
        assertFalse(taskIds.contains("tsk-finance-01"), "Task Finance Human TIDAK BOLEH muncul untuk Staff Sales")
        assertFalse(taskIds.contains("tsk-finance-ai-01"), "Task Finance AI TIDAK BOLEH muncul untuk Staff Sales")
        assertFalse(taskIds.contains("tsk-human-rev-02"), "Task Legal TIDAK BOLEH muncul untuk Staff Sales")
    }

    @Test
    fun testScenario2_DeptManagerSalesLogin_TaskFiltering_DeptWideScope() = runBlocking {
        // Dept Manager Sales login
        val userId = "usr-mgr-sales-01"
        val tasks = taskRepo.getTasksForUser(userId, "default")
        val taskIds = tasks.map { it.id }.toSet()

        println("RAW QUERY RESULT [Dept Manager Sales ($userId)]: ${tasks.map { "${it.id} (${it.title}) [Dept: ${it.departmentId}]" }}")

        // 1. SELURUH task dalam departemen Sales (ada — lintas tim)
        assertTrue(taskIds.contains("tsk-staff-sales-01"), "Manager Sales berhak melihat task Staff Sales")
        assertTrue(taskIds.contains("tsk-team-sales-01"), "Manager Sales berhak melihat task Tim Sales Field")
        assertTrue(taskIds.contains("tsk-dept-sales-leads-01"), "Manager Sales berhak melihat task Tim Sales Leads (lintas tim internal dept)")
        assertTrue(taskIds.contains("tsk-ai-crawl-01"), "Manager Sales berhak melihat task AI Sales")

        // 2. Task Finance (TIDAK BOLEH MUNCUL)
        assertFalse(taskIds.contains("tsk-finance-01"), "Task Finance TIDAK BOLEH muncul untuk Manager Sales")
        assertFalse(taskIds.contains("tsk-finance-ai-01"), "Task Finance AI TIDAK BOLEH muncul untuk Manager Sales")
        assertFalse(taskIds.contains("tsk-human-rev-02"), "Task Legal TIDAK BOLEH muncul untuk Manager Sales")
    }

    @Test
    fun testScenario3_OwnerDireksiLogin_TaskFiltering_AllTenantScope() = runBlocking {
        // Owner / Direksi login
        val userId = "usr-owner-01"
        val tasks = taskRepo.getTasksForUser(userId, "default")
        val taskIds = tasks.map { it.id }.toSet()

        println("RAW QUERY RESULT [Owner ($userId)]: Total=${tasks.size} tasks: ${tasks.map { "${it.id} [${it.departmentId}]" }}")

        // SELURUH task di tenant (Sales + Finance + seluruh dept)
        assertTrue(taskIds.contains("tsk-staff-sales-01"), "Owner berhak melihat task Sales Staff")
        assertTrue(taskIds.contains("tsk-team-sales-01"), "Owner berhak melihat task Sales Team")
        assertTrue(taskIds.contains("tsk-dept-sales-leads-01"), "Owner berhak melihat task Sales Leads")
        assertTrue(taskIds.contains("tsk-ai-crawl-01"), "Owner berhak melihat task AI Sales/Marketing")
        assertTrue(taskIds.contains("tsk-finance-01"), "Owner berhak melihat task Finance Human")
        assertTrue(taskIds.contains("tsk-finance-ai-01"), "Owner berhak melihat task Finance AI")
        assertTrue(taskIds.contains("tsk-human-rev-02"), "Owner berhak melihat task Legal")
    }

    @Test
    fun testScenario4_DirectApiAccess_SecurityAndScopeEnforcement() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}

        // 4A. Unauthenticated request ke /api/v1/tasks WAJIB ditolak 401 Unauthorized
        val unauthResponse = client.get("/api/v1/tasks")
        println("RAW API RESPONSE [Unauthenticated]: status=${unauthResponse.status}")
        assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status, "Permintaan unauthenticated wajib menghasilkan 401")

        // 4B. Authenticated Staff Sales request via API -> Task Finance tidak bocor sama sekali
        val staffToken = generateToken("usr-staff-sales-01", "STAFF_HUMAN")
        val staffResponse = client.get("/api/v1/tasks?userId=usr-staff-sales-01") {
            header("Authorization", "Bearer $staffToken")
        }
        assertEquals(HttpStatusCode.OK, staffResponse.status)
        val staffResponseBody = staffResponse.bodyAsText()
        println("RAW API RESPONSE [Staff Sales]: bodyLength=${staffResponseBody.length}")
        assertTrue(staffResponseBody.contains("tsk-staff-sales-01"), "Response API harus memuat task sales staff")
        assertTrue(staffResponseBody.contains("tsk-ai-crawl-01"), "Response API harus memuat task AI kolaborasi sales")
        assertFalse(staffResponseBody.contains("tsk-finance-01"), "Response API TIDAK BOLEH memuat task finance")
        assertFalse(staffResponseBody.contains("tsk-human-rev-02"), "Response API TIDAK BOLEH memuat task legal")

        // 4C. Authenticated Owner request via API -> Semua task muncul
        val ownerToken = generateToken("usr-owner-01", "TENANT_OWNER")
        val ownerResponse = client.get("/api/v1/tasks?userId=usr-owner-01") {
            header("Authorization", "Bearer $ownerToken")
        }
        assertEquals(HttpStatusCode.OK, ownerResponse.status)
        val ownerResponseBody = ownerResponse.bodyAsText()
        println("RAW API RESPONSE [Owner]: bodyLength=${ownerResponseBody.length}")
        assertTrue(ownerResponseBody.contains("tsk-staff-sales-01"))
        assertTrue(ownerResponseBody.contains("tsk-finance-01"))
    }

    @Test
    fun testMatrixRoleAndConditionExhaustive() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val rolesToTest = listOf(
            Triple("SUPER_ADMIN", "usr-owner-01", HttpStatusCode.OK),
            Triple("TENANT_OWNER", "usr-owner-01", HttpStatusCode.OK),
            Triple("TENANT_ADMIN", "usr-owner-01", HttpStatusCode.OK),
            Triple("DEPT_MANAGER", "usr-mgr-sales-01", HttpStatusCode.OK),
            Triple("STAFF_HUMAN", "usr-staff-sales-01", HttpStatusCode.OK)
        )

        for ((role, userId, expectedStatus) in rolesToTest) {
            val token = generateToken(userId, role)
            val resp = client.get("/api/v1/tasks?userId=$userId") {
                header("Authorization", "Bearer $token")
            }
            println("MATRIX ROLE TEST [$role -> userId=$userId]: Status=${resp.status}")
            assertEquals(expectedStatus, resp.status, "Role $role harus menghasilkan $expectedStatus")
        }

        // Unauthenticated condition
        val unauth = client.get("/api/v1/tasks")
        println("MATRIX ROLE TEST [Unauthenticated]: Status=${unauth.status}")
        assertEquals(HttpStatusCode.Unauthorized, unauth.status, "Unauthenticated harus menghasilkan 401 Unauthorized")
    }
}
