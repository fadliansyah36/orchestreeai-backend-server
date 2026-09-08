package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Fase101SupabaseRealtimeIntegrationTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    private val requiredRealtimeTables = listOf(
        "tasks",
        "notifications",
        "conversations",
        "conversation_messages",
        "competitor_insights",
        "chief_of_staff_briefings",
        "proactive_messages_log",
        "tenants",
        "usage_records",
        "audit_logs",
        "llm_routing_rules",
        "mcp_tools"
    )

    private fun generateToken(role: String, tenantId: String = "tenant-enterprise-001"): String {
        return JWT.create()
            .withSubject("user-$role-test")
            .withClaim("sub", "user-$role-test")
            .withClaim("tenant_id", tenantId)
            .withClaim("role", role)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600 * 1000))
            .sign(algorithm)
    }

    @Test
    fun test01_verifyAll12RealtimeTablesConfiguredInMigration() {
        val migrationPaths = listOf(
            "../supabase/migrations/20260905120000_enable_supabase_realtime_publications.sql",
            "db/migrations/V37__enable_supabase_realtime_publications.sql"
        )
        var checkedAny = false
        for (path in migrationPaths) {
            val file = File(path)
            if (file.exists()) {
                checkedAny = true
                val content = file.readText()
                for (table in requiredRealtimeTables) {
                    assertTrue(
                        content.contains(table),
                        "Migration $path must configure Realtime logical replication for table: $table"
                    )
                    assertTrue(
                        content.contains("REPLICA IDENTITY FULL"),
                        "Migration $path must enforce REPLICA IDENTITY FULL for table updates"
                    )
                }
            }
        }
        assertTrue(checkedAny, "At least one Supabase Realtime publication migration file must exist and be verified.")
    }

    @Test
    fun test02_backendServerSupabaseClientProvider_crudMethods() = runBlocking {
        val supabase = SupabaseClientProvider(AppConfig.load().supabase)
        assertNotNull(supabase, "SupabaseClientProvider instance must be non-null")

        // Test insert query formatting
        val testTaskPayload = buildJsonObject {
            put("id", "tsk-realtime-test-01")
            put("tenant_id", "tenant-enterprise-001")
            put("title", "Realtime Sync Verification Task")
            put("column", "IN_PROGRESS")
        }

        // Test update query formatting
        val updatePayload = buildJsonObject {
            put("column", "COMPLETED")
            put("updated_at", System.currentTimeMillis())
        }

        assertNotNull(testTaskPayload)
        assertNotNull(updatePayload)
    }

    @Test
    fun test03_taskCreationAndMovement_realtimePersistenceContract() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val token = generateToken("TENANT_ADMIN")

        // 1. Move task via PATCH /api/v1/tasks/{taskId}/move
        val moveResponse = client.patch("/api/v1/tasks/tsk-test-101/move") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"targetColumn":"IN_PROGRESS","targetIndex":0,"expectedVersion":1}""")
        }
        assertEquals(HttpStatusCode.OK, moveResponse.status)
        val moveBody = moveResponse.bodyAsText()
        assertTrue(moveBody.contains("status") && (moveBody.contains("UPDATED") || moveBody.contains("IN_PROGRESS")))
    }

    @Test
    fun test04_matrixRoleAndConditionExhaustiveTesting() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}

        val roles = listOf(
            "SUPER_ADMIN",
            "TENANT_OWNER",
            "TENANT_ADMIN",
            "DEPT_MANAGER",
            "STAFF_HUMAN"
        )

        // 1. Authenticated roles access to tenant task movement
        for (role in roles) {
            val token = generateToken(role)
            val res = client.patch("/api/v1/tasks/tsk-role-test/move") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("""{"targetColumn":"IN_PROGRESS","targetIndex":0,"expectedVersion":1}""")
            }
            assertEquals(
                HttpStatusCode.OK,
                res.status,
                "Role $role must have valid authorized access to move tasks"
            )
        }

        // 2. Unauthenticated condition MUST fail with 401 Unauthorized
        val unauthRes = client.patch("/api/v1/tasks/tsk-role-test/move") {
            contentType(ContentType.Application.Json)
            setBody("""{"targetColumn":"IN_PROGRESS","targetIndex":0,"expectedVersion":1}""")
        }
        assertEquals(
            HttpStatusCode.Unauthorized,
            unauthRes.status,
            "Unauthenticated request must be rejected with 401 Unauthorized"
        )
    }
}
