package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkforceHubApiTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)

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
    fun test01_getTenantOverview_live() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val token = generateToken("TENANT_ADMIN")

        val response = client.get("/api/v1/tenants/tenant-enterprise-001/dashboard/overview") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("tenant_id") || body.contains("tenantId"))
    }

    @Test
    fun test02_getDepartments_live() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val token = generateToken("TENANT_ADMIN")

        val response = client.get("/api/v1/tenants/tenant-enterprise-001/departments") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.startsWith("["))
    }

    @Test
    fun test03_roleMatrixExhaustiveTesting_overview() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val endpoint = "/api/v1/tenants/tenant-enterprise-001/dashboard/overview"

        // 1. SUPER_ADMIN
        val superAdminResp = client.get(endpoint) {
            header("Authorization", "Bearer ${generateToken("SUPER_ADMIN", "system-platform")}")
        }
        assertEquals(HttpStatusCode.OK, superAdminResp.status, "SUPER_ADMIN should have access")

        // 2. TENANT_OWNER
        val ownerResp = client.get(endpoint) {
            header("Authorization", "Bearer ${generateToken("TENANT_OWNER")}")
        }
        assertEquals(HttpStatusCode.OK, ownerResp.status, "TENANT_OWNER should have access")

        // 3. TENANT_ADMIN
        val adminResp = client.get(endpoint) {
            header("Authorization", "Bearer ${generateToken("TENANT_ADMIN")}")
        }
        assertEquals(HttpStatusCode.OK, adminResp.status, "TENANT_ADMIN should have access")

        // 4. DEPT_MANAGER
        val mgrResp = client.get(endpoint) {
            header("Authorization", "Bearer ${generateToken("DEPT_MANAGER")}")
        }
        assertEquals(HttpStatusCode.OK, mgrResp.status, "DEPT_MANAGER should have access")

        // 5. STAFF_HUMAN
        val staffResp = client.get(endpoint) {
            header("Authorization", "Bearer ${generateToken("STAFF_HUMAN")}")
        }
        assertEquals(HttpStatusCode.OK, staffResp.status, "STAFF_HUMAN should have access")

        // 6. Unauthenticated
        val unauthResp = client.get(endpoint)
        assertEquals(HttpStatusCode.Unauthorized, unauthResp.status, "Unauthenticated request must return 401")
    }

    @Test
    fun test04_getStaff_live() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val token = generateToken("TENANT_ADMIN")

        val response = client.get("/api/v1/tenants/tenant-enterprise-001/staff") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.startsWith("["))
    }

    @Test
    fun test05_getAgents_live() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val token = generateToken("TENANT_ADMIN")

        val response = client.get("/api/v1/tenants/tenant-enterprise-001/agents") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.startsWith("["))
    }

    @Test
    fun test06_deleteDepartmentAndStaff_routes() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val token = generateToken("TENANT_ADMIN")

        // Test delete department endpoint
        val deptDeleteResp = client.delete("/api/v1/tenants/tenant-enterprise-001/departments/dept-non-existent-test") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, deptDeleteResp.status)

        // Test delete staff endpoint
        val staffDeleteResp = client.delete("/api/v1/tenants/tenant-enterprise-001/staff/usr-non-existent-test") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, staffDeleteResp.status)
    }
}
