package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.repositories.presence.PresenceCheckLogRepository
import ai.orchestree.backend.database.repositories.presence.StaffWorkScheduleRepository
import ai.orchestree.backend.database.repositories.presence.UserPresenceEnrollmentRepository
import ai.orchestree.backend.models.LoginRiskLevel
import ai.orchestree.backend.models.PresenceCheckLog
import ai.orchestree.backend.models.PresenceEnrollRequest
import ai.orchestree.backend.models.PresenceEnrollResponse
import ai.orchestree.backend.models.PresenceRequirementCheckResponse
import ai.orchestree.backend.models.PresenceRequirementState
import ai.orchestree.backend.models.PresenceVerifyRequest
import ai.orchestree.backend.models.PresenceVerifyResponse
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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class Fase112PresenceSystemTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    private fun generateToken(userId: String, role: String, tenantId: String = "tenant-enterprise-001"): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("sub", userId)
            .withClaim("tenant_id", tenantId)
            .withClaim("role", role)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600 * 1000))
            .sign(algorithm)
    }

    // Helper face embedding vectors
    private val sampleEmbeddingA = listOf(0.12f, 0.45f, 0.88f, -0.32f, 0.65f, 0.11f, -0.05f, 0.77f)
    private val sampleEmbeddingSimilarToA = listOf(0.13f, 0.44f, 0.87f, -0.31f, 0.64f, 0.12f, -0.04f, 0.76f) // ~0.99 similarity
    private val sampleEmbeddingDissimilar = listOf(-0.85f, -0.22f, 0.05f, 0.91f, -0.44f, -0.65f, 0.88f, -0.55f) // negative/low similarity

    @Test
    @Order(1)
    fun test01_cosineSimilarity_calculation() {
        val service = PresenceService.defaultInstance
        val selfSim = service.cosineSimilarity(sampleEmbeddingA, sampleEmbeddingA)
        assertTrue(selfSim >= 0.999f, "Self similarity should be 1.0, got $selfSim")

        val highSim = service.cosineSimilarity(sampleEmbeddingA, sampleEmbeddingSimilarToA)
        assertTrue(highSim > 0.98f, "Similar vector should have high similarity, got $highSim")

        val lowSim = service.cosineSimilarity(sampleEmbeddingA, sampleEmbeddingDissimilar)
        assertTrue(lowSim < 0.50f, "Dissimilar vector should have low similarity, got $lowSim")
    }

    @Test
    @Order(2)
    fun test02_scenario1_notRequired_whenNotEnrolledOrDisabled() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        // User usr-unenrolled has no enrollment
        val response = client.get("/api/v1/presence/requirement-check?userId=usr-unenrolled&deviceId=dev-01&ip=10.0.0.1")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = json.decodeFromString<PresenceRequirementCheckResponse>(response.bodyAsText())
        assertEquals(PresenceRequirementState.NOT_REQUIRED, body.state)
        assertFalse(body.checkedInToday)
    }

    @Test
    @Order(3)
    fun test03_enrollment_faceAndFingerprint() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val enrollReq = PresenceEnrollRequest(
            userId = "usr-test-staff-01",
            method = "FACE",
            faceEmbedding = sampleEmbeddingA,
            deviceId = "dev-pixel-7a",
            isEnabled = true
        )

        val response = client.post("/api/v1/presence/enroll") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(enrollReq))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PresenceEnrollResponse>(response.bodyAsText())
        assertTrue(body.success)
        assertTrue(body.enrolledMethods.contains("FACE"))
        assertTrue(body.enrolledMethods.contains("FINGERPRINT"))
        assertTrue(body.isEnabled)

        // Verify stored enrollment in repo has encrypted embedding (NOT raw float array)
        val repoEnrollment = runBlocking { UserPresenceEnrollmentRepository.defaultInstance.get("usr-test-staff-01") }
        assertNotNull(repoEnrollment)
        assertNotNull(repoEnrollment.faceEmbeddingRef)
        assertTrue(repoEnrollment.faceEmbeddingRef!!.contains("encryptedData"), "Embedding must be stored encrypted")
        assertTrue(repoEnrollment.fingerprintRegisteredDeviceIds.contains("dev-pixel-7a"))
    }

    @Test
    @Order(4)
    fun test04_scenario2_firstLoginToday_lowRisk_requiresLoginCheckin() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        // dev-pixel-7a is a registered/trusted device for usr-test-staff-01
        val response = client.get("/api/v1/presence/requirement-check?userId=usr-test-staff-01&deviceId=dev-pixel-7a&ip=192.168.1.50")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = json.decodeFromString<PresenceRequirementCheckResponse>(response.bodyAsText())
        assertEquals(PresenceRequirementState.REQUIRED_LOGIN_CHECKIN, body.state)
        assertEquals(LoginRiskLevel.LOW, body.loginRiskLevel)
        assertFalse(body.checkedInToday)
    }

    @Test
    @Order(5)
    fun test05_scenario3_newDeviceOrAnomalousIp_requiresLoginCheckinWithRisk() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        // New, unrecognized device ID
        val response = client.get("/api/v1/presence/requirement-check?userId=usr-test-staff-01&deviceId=unknown-hacker-phone&ip=198.51.100.99")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = json.decodeFromString<PresenceRequirementCheckResponse>(response.bodyAsText())
        assertEquals(PresenceRequirementState.REQUIRED_LOGIN_CHECKIN, body.state)
        assertTrue(body.loginRiskLevel != LoginRiskLevel.LOW, "Risk level must not be LOW for unknown device/anomalous IP")
    }

    @Test
    @Order(6)
    fun test06_serverSideFaceVerification_and_scenario4_alreadyCheckedInToday() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}

        // 1. Attempt verification with mismatched face embedding
        val failVerifyReq = PresenceVerifyRequest(
            userId = "usr-test-staff-01",
            checkType = "CHECK_IN",
            methodUsed = "FACE",
            faceEmbedding = sampleEmbeddingDissimilar,
            deviceId = "dev-pixel-7a",
            ipAddress = "192.168.1.50"
        )
        val failResponse = client.post("/api/v1/presence/verify") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(failVerifyReq))
        }
        assertEquals(HttpStatusCode.OK, failResponse.status)
        val failBody = json.decodeFromString<PresenceVerifyResponse>(failResponse.bodyAsText())
        assertFalse(failBody.success)
        assertEquals("FAILED_SIMILARITY_BELOW_THRESHOLD", failBody.verificationResult)

        // 2. Perform verification with matching face embedding (Server-side cosine similarity check)
        val successVerifyReq = PresenceVerifyRequest(
            userId = "usr-test-staff-01",
            checkType = "CHECK_IN",
            methodUsed = "FACE",
            faceEmbedding = sampleEmbeddingSimilarToA,
            deviceId = "dev-pixel-7a",
            ipAddress = "192.168.1.50"
        )
        val successResponse = client.post("/api/v1/presence/verify") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(successVerifyReq))
        }
        assertEquals(HttpStatusCode.OK, successResponse.status)
        val successBody = json.decodeFromString<PresenceVerifyResponse>(successResponse.bodyAsText())
        assertTrue(successBody.success)
        assertEquals("SUCCESS", successBody.verificationResult)
        assertTrue((successBody.similarityScore ?: 0f) >= 0.80f)

        // Ensure schedule is regular (checkout at 23:59 so we are within working hours)
        StaffWorkScheduleRepository.defaultInstance.setSchedule("usr-test-staff-01", LocalTime.of(23, 59), ZoneId.of("Asia/Jakarta"))

        // 3. Check requirement now: Should be ALREADY_CHECKED_IN_TODAY (Scenario 4)
        val checkResponse = client.get("/api/v1/presence/requirement-check?userId=usr-test-staff-01&deviceId=dev-pixel-7a&ip=192.168.1.50")
        val checkBody = json.decodeFromString<PresenceRequirementCheckResponse>(checkResponse.bodyAsText())
        assertEquals(PresenceRequirementState.ALREADY_CHECKED_IN_TODAY, checkBody.state)
        assertTrue(checkBody.checkedInToday)
        assertFalse(checkBody.checkedOutToday)
    }

    @Test
    @Order(7)
    fun test07_scenario5_checkoutTimeReached_requiresCheckout() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}

        // Set checkout time to 00:01 AM (so current time is definitely >= checkout time)
        StaffWorkScheduleRepository.defaultInstance.setSchedule("usr-test-staff-01", LocalTime.of(0, 1), ZoneId.of("Asia/Jakarta"))

        val checkResponse = client.get("/api/v1/presence/requirement-check?userId=usr-test-staff-01&deviceId=dev-pixel-7a&ip=192.168.1.50")
        assertEquals(HttpStatusCode.OK, checkResponse.status)
        val checkBody = json.decodeFromString<PresenceRequirementCheckResponse>(checkResponse.bodyAsText())
        assertEquals(PresenceRequirementState.REQUIRED_CHECKOUT, checkBody.state)
        assertTrue(checkBody.checkedInToday)
        assertFalse(checkBody.checkedOutToday)
    }

    @Test
    @Order(8)
    fun test08_fingerprintVerification_and_scenario6_shiftCompleted() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}

        // 1. Unregistered device fingerprint attempt should fail
        val unregReq = PresenceVerifyRequest(
            userId = "usr-test-staff-01",
            checkType = "CHECK_OUT",
            methodUsed = "FINGERPRINT",
            biometricSuccess = true,
            deviceId = "unregistered-device-xyz"
        )
        val unregResp = client.post("/api/v1/presence/verify") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(unregReq))
        }
        val unregBody = json.decodeFromString<PresenceVerifyResponse>(unregResp.bodyAsText())
        assertFalse(unregBody.success)
        assertEquals("FAILED_DEVICE_NOT_REGISTERED", unregBody.verificationResult)

        // 2. Successful fingerprint checkout on registered device
        val validCheckoutReq = PresenceVerifyRequest(
            userId = "usr-test-staff-01",
            checkType = "CHECK_OUT",
            methodUsed = "FINGERPRINT",
            biometricSuccess = true,
            deviceId = "dev-pixel-7a"
        )
        val validCheckoutResp = client.post("/api/v1/presence/verify") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(validCheckoutReq))
        }
        val validBody = json.decodeFromString<PresenceVerifyResponse>(validCheckoutResp.bodyAsText())
        assertTrue(validBody.success)
        assertEquals("SUCCESS", validBody.verificationResult)

        // 3. Scenario 6: Requirement check after checkout completed -> ALREADY_CHECKED_IN_TODAY
        val finalCheck = client.get("/api/v1/presence/requirement-check?userId=usr-test-staff-01&deviceId=dev-pixel-7a&ip=192.168.1.50")
        val finalBody = json.decodeFromString<PresenceRequirementCheckResponse>(finalCheck.bodyAsText())
        assertEquals(PresenceRequirementState.ALREADY_CHECKED_IN_TODAY, finalBody.state)
        assertTrue(finalBody.checkedInToday)
        assertTrue(finalBody.checkedOutToday)
    }

    @Test
    @Order(9)
    fun test09_auditLogs_and_enrollment_getEndpoints() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}

        // Check GET /api/v1/presence/enrollment
        val enrollResp = client.get("/api/v1/presence/enrollment?userId=usr-test-staff-01")
        assertEquals(HttpStatusCode.OK, enrollResp.status)
        val enrollBody = json.decodeFromString<UserPresenceEnrollment>(enrollResp.bodyAsText())
        assertEquals("usr-test-staff-01", enrollBody.userId)
        assertTrue(enrollBody.isEnabled)

        // Check GET /api/v1/presence/logs
        val logsResp = client.get("/api/v1/presence/logs?userId=usr-test-staff-01")
        assertEquals(HttpStatusCode.OK, logsResp.status)
        val logs = json.decodeFromString<List<PresenceCheckLog>>(logsResp.bodyAsText())
        assertTrue(logs.isNotEmpty(), "Presence check audit logs must contain recorded verification attempts")
        assertTrue(logs.any { it.checkType == "CHECK_IN" })
        assertTrue(logs.any { it.checkType == "CHECK_OUT" })
    }

    @Test
    @Order(10)
    fun test10_matrixRoleExhaustiveTesting() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val roles = listOf("SUPER_ADMIN", "TENANT_OWNER", "TENANT_ADMIN", "DEPT_MANAGER", "STAFF_HUMAN")

        for (role in roles) {
            val token = generateToken("usr-$role", role)
            val response = client.get("/api/v1/presence/requirement-check?userId=usr-$role&deviceId=dev-known-01&ip=192.168.1.1") {
                header("Authorization", "Bearer $token")
            }
            assertEquals(HttpStatusCode.OK, response.status, "Role $role should successfully check presence requirement")
        }

        // Unauthenticated check with query params is also allowed for preliminary login check
        val unauthResponse = client.get("/api/v1/presence/requirement-check?userId=usr-unauth-test&deviceId=dev-01&ip=10.0.0.1")
        assertEquals(HttpStatusCode.OK, unauthResponse.status)
    }
}
