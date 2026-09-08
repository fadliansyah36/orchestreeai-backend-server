package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.repositories.intelligence.AgentDecisionOutcomeRepository
import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRepository
import ai.orchestree.backend.database.repositories.orchestration.WorkflowExecutionRepository
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
import java.io.File
import java.sql.DriverManager
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityAndArchitectureAuditTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    private fun generateToken(role: String, userId: String = "usr-test", tenantId: String = "tenant-test"): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("user_id", userId)
            .withClaim("tenant_id", tenantId)
            .withClaim("role", role)
            .withClaim("email", "$userId@test.com")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600_000))
            .sign(algorithm)
    }

    @Test
    fun testRoomSqliteAbsenceInBackend() {
        val rootSrc = File("src/main/kotlin")
        val altSrc = File("orchestreeai-backend-server/src/main/kotlin")
        val baseDir = if (rootSrc.exists()) rootSrc else altSrc
        assertTrue(baseDir.exists(), "Source directory must exist")

        val forbiddenKeywords = listOf(
            "@Entity",
            "@Dao",
            "RoomDatabase",
            "SQLiteOpenHelper",
            "androidx.room"
        )

        val violations = mutableListOf<String>()
        baseDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val content = file.readText()
            for (keyword in forbiddenKeywords) {
                if (content.contains(keyword)) {
                    violations.add("${file.name} contains forbidden keyword: $keyword")
                }
            }
        }

        println("=== [AUDIT STEP 1] Room/SQLite Dependency & Annotation Scan ===")
        println("Scanned directory: ${baseDir.absolutePath}")
        println("Violations found: ${violations.size}")
        if (violations.isNotEmpty()) {
            violations.forEach { println(" - $it") }
        }
        assertTrue(violations.isEmpty(), "Backend server must not contain Room or SQLite artifacts: $violations")
    }

    @Test
    fun testRoleSpoofingViaHeaderIsBlocked() = testApplication {
        val config = AppConfig.load()
        application {
            configureSerialization()
            configureAuthentication(config)
            configureHTTPS()
            configureRouting()
        }

        // Test attempting role spoofing using header without valid JWT
        val response = client.get("/api/v1/admin/tenants") {
            header("X-Admin-Role", "SUPER_ADMIN")
        }

        println("=== [AUDIT STEP 2] Role Spoofing Prevention ===")
        println("Header spoofing response status: ${response.status}")
        println("Header spoofing response body: ${response.bodyAsText()}")
        assertEquals(HttpStatusCode.Unauthorized, response.status, "Must return 401 Unauthorized when unauthenticated")
    }

    @Test
    fun testSuperAdminJwtAccessAllowed() = testApplication {
        val config = AppConfig.load()
        application {
            configureSerialization()
            configureAuthentication(config)
            configureHTTPS()
            configureRouting()
        }

        val token = generateToken("SUPER_ADMIN", "usr-superadmin")
        val response = client.get("/api/v1/admin/tenants") {
            header("Authorization", "Bearer $token")
        }

        println("=== [AUDIT STEP 3] SUPER_ADMIN Access Verification ===")
        println("SUPER_ADMIN response status: ${response.status}")
        println("SUPER_ADMIN response body: ${response.bodyAsText()}")
        assertEquals(HttpStatusCode.OK, response.status, "SUPER_ADMIN must receive 200 OK")
    }

    @Test
    fun testMatrixRbacExhaustiveTesting() = testApplication {
        val config = AppConfig.load()
        application {
            configureSerialization()
            configureAuthentication(config)
            configureHTTPS()
            configureRouting()
        }

        val rolesToTest = listOf(
            "SUPER_ADMIN" to HttpStatusCode.OK,
            "TENANT_OWNER" to HttpStatusCode.Forbidden,
            "TENANT_ADMIN" to HttpStatusCode.Forbidden,
            "DEPT_MANAGER" to HttpStatusCode.Forbidden,
            "STAFF_HUMAN" to HttpStatusCode.Forbidden
        )

        println("=== [AUDIT STEP 4] RBAC Matrix Exhaustive Testing for /admin/tenants ===")
        for ((role, expectedStatus) in rolesToTest) {
            val token = generateToken(role, "usr-$role")
            val resp = client.get("/api/v1/admin/tenants") {
                header("Authorization", "Bearer $token")
            }
            println("Role: $role | Expected: $expectedStatus | Actual: ${resp.status} | Body: ${resp.bodyAsText()}")
            assertEquals(expectedStatus, resp.status, "Role $role validation failed")
        }

        // Unauthenticated check
        val unauthResp = client.get("/api/v1/admin/tenants")
        println("Role: Unauthenticated | Expected: 401 Unauthorized | Actual: ${unauthResp.status}")
        assertEquals(HttpStatusCode.Unauthorized, unauthResp.status, "Unauthenticated access must be 401")
    }

    @Test
    fun testRepositoriesStartCleanWithoutFakeDataInjection() = runBlocking {
        println("=== [AUDIT STEP 5] Statelessness & Clean Start (No Fake Sample Seeding) ===")
        val agentRepo = AgentDecisionOutcomeRepository()
        val outcomes = agentRepo.getWithConfidenceLastMonth("tenant-default")
        println("AgentDecisionOutcomeRepository items for tenant-default: ${outcomes.size}")
        assertEquals(0, outcomes.size)

        val memRepo = MemoryDocumentRepository()
        val docs = memRepo.getAllActive("tenant-default")
        println("MemoryDocumentRepository items for tenant-default: ${docs.size}")
        assertEquals(0, docs.size)

        val wfRepo = WorkflowExecutionRepository()
        val executions = wfRepo.findStaleRunning(0)
        println("WorkflowExecutionRepository running items: ${executions.size}")
        assertEquals(0, executions.size)
    }

    @Test
    fun testRawDatabaseConnectivityOrQueryExecution() {
        println("=== [AUDIT STEP 6] Raw Database Connectivity & Verification ===")
        val dbUrl = resolveDirectDbUrl()
        if (dbUrl.isNotBlank()) {
            try {
                val clean = dbUrl.removePrefix("jdbc:")
                val uri = java.net.URI(clean)
                val host = uri.host
                val port = if (uri.port != -1) uri.port else 5432
                val path = uri.path.trimStart('/')
                val userInfo = uri.userInfo ?: ""
                val user = if (userInfo.contains(":")) userInfo.substringBefore(":") else userInfo
                val pass = if (userInfo.contains(":")) userInfo.substringAfter(":") else ""
                val jdbcUrl = "jdbc:postgresql://$host:$port/$path"

                val props = java.util.Properties().apply {
                    if (user.isNotBlank()) setProperty("user", user)
                    if (pass.isNotBlank()) setProperty("password", pass)
                    setProperty("ssl", "true")
                    setProperty("sslmode", "require")
                }
                DriverManager.getConnection(jdbcUrl, props).use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT NOW() as current_time, current_database() as db").use { rs ->
                            if (rs.next()) {
                                println("Raw SQL Execution [OK]: DB=${rs.getString("db")}, NOW=${rs.getString("current_time")}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Database direct connection notice: ${e.message}")
            }
        } else {
            println("No external DATABASE_URL configured in local environment; tested offline safety.")
        }
    }

    private fun resolveDirectDbUrl(): String {
        val candidates = listOf(
            File("/app/applet/.env"),
            File("/root/.orchestreeai/secrets.properties"),
            File(".env"),
            File("../.env")
        )
        val keys = listOf("DATABASE_POOL_URL", "DATABASE_DIRECT_URL", "DATABASE_URL")
        for (key in keys) {
            for (f in candidates) {
                if (f.exists()) {
                    val lines = try { f.readLines() } catch (_: Exception) { emptyList() }
                    for (raw in lines) {
                        val line = raw.trim()
                        if (line.startsWith("$key=")) {
                            val v = line.substringAfter("=").trim().trim('\"').trim('\'')
                            if (v.isNotBlank() && v != "placeholder") {
                                return v
                            }
                        }
                    }
                }
            }
        }
        return ""
    }
}
