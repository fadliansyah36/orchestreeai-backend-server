package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.database.repositories.presence.PresenceCheckLogRepository
import ai.orchestree.backend.database.repositories.presence.UserPresenceEnrollmentRepository
import ai.orchestree.backend.models.PresenceCheckLog
import ai.orchestree.backend.models.PresenceSecurityAuditSummary
import ai.orchestree.backend.models.UserPresenceEnrollment
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import ai.orchestree.backend.security.PresenceService
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PresenceSecurityAuditStatsTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val appConfig = AppConfig.load()
    private val jwtSecret = appConfig.security.jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    private fun generateToken(userId: String, role: String, tenantId: String = "tenant-enterprise-001"): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("sub", userId)
            .withClaim("role", role)
            .withClaim("user_metadata", mapOf("role" to role))
            .withClaim("app_metadata", mapOf("role" to role))
            .withClaim("tenant_id", tenantId)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600_000))
            .sign(algorithm)
    }

    @Test
    @Order(1)
    fun `test presence security audit aggregation accurately detects consecutive failures and unauthorized attempts`() = runBlocking {
        val checkLogRepo = PresenceCheckLogRepository()
        val enrollmentRepo = UserPresenceEnrollmentRepository()

        // Enroll 3 test users
        enrollmentRepo.save(UserPresenceEnrollment(id = UUID.randomUUID().toString(), userId = "user-audit-1", isEnabled = true))
        enrollmentRepo.save(UserPresenceEnrollment(id = UUID.randomUUID().toString(), userId = "user-audit-2", isEnabled = true))
        enrollmentRepo.save(UserPresenceEnrollment(id = UUID.randomUUID().toString(), userId = "user-audit-3", isEnabled = true))

        val now = System.currentTimeMillis()

        // 1. User 1 has successful checks
        checkLogRepo.insert(
            PresenceCheckLog(
                id = UUID.randomUUID().toString(),
                userId = "user-audit-1",
                checkType = "CHECK_IN",
                methodUsed = "FACE",
                verificationResult = "SUCCESS",
                checkedAt = now - 100000
            )
        )
        checkLogRepo.insert(
            PresenceCheckLog(
                id = UUID.randomUUID().toString(),
                userId = "user-audit-1",
                checkType = "CHECK_OUT",
                methodUsed = "FINGERPRINT",
                verificationResult = "SUCCESS",
                checkedAt = now - 50000
            )
        )

        // 2. User 2 has 3 consecutive failed verification attempts (potential unauthorized access)
        checkLogRepo.insert(
            PresenceCheckLog(
                id = UUID.randomUUID().toString(),
                userId = "user-audit-2",
                checkType = "LOGIN",
                methodUsed = "FACE",
                verificationResult = "FAILED_SIMILARITY_BELOW_THRESHOLD",
                checkedAt = now - 30000
            )
        )
        checkLogRepo.insert(
            PresenceCheckLog(
                id = UUID.randomUUID().toString(),
                userId = "user-audit-2",
                checkType = "LOGIN",
                methodUsed = "FACE",
                verificationResult = "FAILED_SIMILARITY_BELOW_THRESHOLD",
                checkedAt = now - 20000
            )
        )
        checkLogRepo.insert(
            PresenceCheckLog(
                id = UUID.randomUUID().toString(),
                userId = "user-audit-2",
                checkType = "CHECK_IN",
                methodUsed = "FINGERPRINT",
                verificationResult = "FAILED_DEVICE_NOT_REGISTERED",
                checkedAt = now - 10000
            )
        )

        val presenceService = PresenceService(
            userPresenceEnrollmentRepo = enrollmentRepo,
            presenceCheckLogRepo = checkLogRepo
        )

        val stats = presenceService.getSecurityAuditSummary()

        println("=== PRESENCE SECURITY AUDIT STATS OUTPUT ===")
        println("Total Enrolled Users: ${stats.totalEnrolledUsers}")
        println("Total Verification Checks: ${stats.totalVerificationChecks}")
        println("Total Successful Checks: ${stats.totalSuccessfulChecks}")
        println("Total Failed Checks: ${stats.totalFailedChecks}")
        println("Consecutive Failures: ${stats.consecutiveFailures}")
        println("Potential Unauthorized Attempts: ${stats.potentialUnauthorizedAttempts}")
        println("Method Breakdown: Face=${stats.methodBreakdown.face}, Fingerprint=${stats.methodBreakdown.fingerprint}")
        println("Security Risk Level: ${stats.securityRiskLevel}")

        assertTrue(stats.totalEnrolledUsers >= 3, "Total enrolled users should be at least 3")
        assertTrue(stats.totalVerificationChecks >= 5, "Total verification checks should be at least 5")
        assertTrue(stats.totalSuccessfulChecks >= 2, "Successful checks should be at least 2")
        assertTrue(stats.totalFailedChecks >= 3, "Failed checks should be at least 3")
        assertEquals(3, stats.consecutiveFailures, "Should detect 3 consecutive failures")
        assertTrue(stats.potentialUnauthorizedAttempts >= 3, "Should detect 3 unauthorized attempts")
        assertEquals("HIGH", stats.securityRiskLevel, "Risk level should be HIGH due to 3 consecutive failures")
    }

    @Test
    @Order(2)
    fun `test public presence security-audit-stats endpoint returns 200 with aggregate data`() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(appConfig)
            configureRouting()
        }

        val response = client.get("/api/v1/presence/security-audit-stats")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        val stats = json.decodeFromString<PresenceSecurityAuditSummary>(body)

        println("=== HTTP /api/v1/presence/security-audit-stats RESPONSE ===")
        println(body)

        assertNotNull(stats)
        assertTrue(stats.totalEnrolledUsers >= 0)
        assertNotNull(stats.methodBreakdown)
        assertNotNull(stats.securityRiskLevel)
    }

    @Test
    @Order(3)
    fun `test admin presence security-stats endpoint enforces Super Admin RBAC`() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(appConfig)
            configureRouting()
        }

        // 1. Unauthenticated request should be Unauthorized (401)
        val unauthResponse = client.get("/api/v1/admin/presence/security-stats")
        assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status)

        // 2. Staff user request should be Forbidden (403)
        val staffToken = generateToken("staff-user-01", "STAFF_HUMAN")
        val staffResponse = client.get("/api/v1/admin/presence/security-stats") {
            header("Authorization", "Bearer $staffToken")
        }
        assertEquals(HttpStatusCode.Forbidden, staffResponse.status)

        // 3. Super Admin request should succeed with 200 OK
        val superAdminToken = generateToken("super-admin-01", "SUPER_ADMIN")
        val adminResponse = client.get("/api/v1/admin/presence/security-stats") {
            header("Authorization", "Bearer $superAdminToken")
        }
        assertEquals(HttpStatusCode.OK, adminResponse.status)

        val body = adminResponse.bodyAsText()
        val stats = json.decodeFromString<PresenceSecurityAuditSummary>(body)
        println("=== HTTP /api/v1/admin/presence/security-stats (SUPER_ADMIN) RESPONSE ===")
        println(body)
        assertNotNull(stats)
        assertTrue(stats.totalEnrolledUsers >= 0)
    }
}
