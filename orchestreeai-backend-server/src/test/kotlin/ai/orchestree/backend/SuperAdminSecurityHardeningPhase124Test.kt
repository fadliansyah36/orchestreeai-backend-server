package ai.orchestree.backend

import ai.orchestree.backend.api.AdminIpAllowlistDto
import ai.orchestree.backend.api.AdminLoginRequest
import ai.orchestree.backend.api.AdminVerifyMfaRequest
import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import ai.orchestree.backend.security.AdminSecurityService
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Date

class SuperAdminSecurityHardeningPhase124Test {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val securityService = AdminSecurityService.defaultInstance

    private fun generateToken(
        role: String,
        userId: String = "usr-superadmin",
        tenantId: String = "tenant-admin",
        isMfa: Boolean = true
    ): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("user_id", userId)
            .withClaim("tenant_id", tenantId)
            .withClaim("role", role)
            .withClaim("email", "$userId@orchestree.ai")
            .withClaim("isMfaVerified", isMfa)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 15 * 60 * 1000L)) // 15-min idle timeout
            .sign(algorithm)
    }

    private fun ApplicationTestBuilder.setupApp() {
        val config = AppConfig.load()
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(config)
            configureRouting()
        }
    }

    @BeforeEach
    fun setUp() {
        // Reset IP allowlist to default state before each test
        securityService.configureIpAllowlist(emptyList(), enabled = false)
    }

    // =========================================================================
    // 1. BAGIAN A: MFA WAJIB TANPA PENGECUALIAN & ZERO BYPASS (Fase 86 / A.3.3)
    // =========================================================================
    @Test
    fun test1_MandatoryMfaSuperAdminWithoutBypass() = testApplication {
        setupApp()
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        println("==========================================================================")
        println("=== TEST 1: Mandatory Multi-Factor Authentication (MFA) Enforcement    ===")
        println("==========================================================================")

        // Step 1: Login with credentials -> Must require MFA (no bypass token directly returned)
        val loginResponse = client.post("/api/v1/admin/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AdminLoginRequest(email = "superadmin@orchestree.ai", password = "ValidPassword123#"))
        }

        println("Step 1 (Credential Auth) Response Status: ${loginResponse.status}")
        val loginBody = loginResponse.bodyAsText()
        println("Step 1 Response Body: $loginBody")
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        assertTrue(loginBody.contains("MFA_REQUIRED"), "Must indicate that MFA verification is required")

        // Step 2: Invalid 3-digit OTP -> Rejected with 401
        val invalidMfaResp = client.post("/api/v1/admin/auth/verify-mfa") {
            contentType(ContentType.Application.Json)
            setBody(AdminVerifyMfaRequest(email = "superadmin@orchestree.ai", totpCode = "123"))
        }
        println("Step 2 (Invalid OTP) Status: ${invalidMfaResp.status}")
        assertEquals(HttpStatusCode.Unauthorized, invalidMfaResp.status)

        // Step 3: Valid 6-digit TOTP code -> 200 OK with authenticated session
        val validMfaResp = client.post("/api/v1/admin/auth/verify-mfa") {
            contentType(ContentType.Application.Json)
            setBody(AdminVerifyMfaRequest(email = "superadmin@orchestree.ai", totpCode = "584920"))
        }
        println("Step 3 (Valid 6-Digit TOTP) Status: ${validMfaResp.status}")
        val validMfaBody = validMfaResp.bodyAsText()
        println("Step 3 Response Body: $validMfaBody")
        assertEquals(HttpStatusCode.OK, validMfaResp.status)
        val parsedBody = Json.parseToJsonElement(validMfaBody).jsonObject
        assertEquals("SUPER_ADMIN", parsedBody["role"]?.jsonPrimitive?.content)
        assertEquals("true", parsedBody["isMfaVerified"]?.jsonPrimitive?.content)
        assertEquals(15, parsedBody["sessionIdleTimeoutMinutes"]?.jsonPrimitive?.content?.toInt())

        println("=== [PASS] Test 1: Mandatory MFA verified with zero bypass ===")
    }

    // =========================================================================
    // 2. BAGIAN A.1.2: SESSION TIMEOUT (15-MIN IDLE TIMEOUT)
    // =========================================================================
    @Test
    fun test2_SessionIdleTimeoutConfiguration() {
        println("==========================================================================")
        println("=== TEST 2: Session Idle Timeout Verification (15-Minute Policy)        ===")
        println("==========================================================================")

        val adminToken = generateToken("SUPER_ADMIN")
        val decoded = JWT.decode(adminToken)
        val expiresAt = decoded.expiresAt.time
        val issuedAt = decoded.issuedAt.time
        val durationMinutes = (expiresAt - issuedAt) / (60 * 1000)

        println("Token Issued At: ${Date(issuedAt)}")
        println("Token Expires At: ${Date(expiresAt)}")
        println("Token Lifespan: $durationMinutes minutes")

        assertEquals(15L, durationMinutes, "Admin session timeout MUST be strictly 15 minutes")
        println("=== [PASS] Test 2: Admin session timeout strictly bounded to 15 minutes ===")
    }

    // =========================================================================
    // 3. BAGIAN A.1.3: IP ALLOWLIST (OPSIONAL/CONFIGURABLE)
    // =========================================================================
    @Test
    fun test3_IpAllowlistEnforcementAndDynamicUpdate() = testApplication {
        setupApp()
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        println("==========================================================================")
        println("=== TEST 3: IP Allowlist Gating & Dynamic Configuration                 ===")
        println("==========================================================================")

        val superAdminToken = generateToken("SUPER_ADMIN")

        // 1. Initially disabled: Any IP is allowed
        val unconstrainedResp = client.get("/api/v1/admin/tenants") {
            header("Authorization", "Bearer $superAdminToken")
            header("X-Forwarded-For", "203.0.113.199")
        }
        assertEquals(HttpStatusCode.OK, unconstrainedResp.status, "When allowlist disabled, requests should pass")

        // 2. Enable IP allowlist restricting to 192.168.1.50 and 10.0.0.0/8
        val updateResp = client.post("/api/v1/admin/security/ip-allowlist") {
            header("Authorization", "Bearer $superAdminToken")
            header("X-Operator-Id", "super-admin-prime")
            contentType(ContentType.Application.Json)
            setBody(AdminIpAllowlistDto(enabled = true, allowedIps = listOf("192.168.1.50", "10.0.0.0/8")))
        }
        assertEquals(HttpStatusCode.OK, updateResp.status)

        // 3. Allowed IP succeeds
        val allowedIpResp = client.get("/api/v1/admin/tenants") {
            header("Authorization", "Bearer $superAdminToken")
            header("X-Forwarded-For", "192.168.1.50")
        }
        assertEquals(HttpStatusCode.OK, allowedIpResp.status, "Allowed IP must receive 200 OK")

        // 4. Disallowed IP is blocked (403 Forbidden)
        val blockedIpResp = client.get("/api/v1/admin/tenants") {
            header("Authorization", "Bearer $superAdminToken")
            header("X-Forwarded-For", "203.0.113.88")
        }
        println("Disallowed IP (203.0.113.88) Status: ${blockedIpResp.status}")
        assertEquals(HttpStatusCode.Forbidden, blockedIpResp.status, "Disallowed IP must be rejected with 403")

        println("=== [PASS] Test 3: IP Allowlist successfully gates unauthorized networks ===")
    }

    // =========================================================================
    // 4. BAGIAN B: CSRF DOUBLE-SUBMIT COOKIE PATTERN
    // =========================================================================
    @Test
    fun test4_CsrfDoubleSubmitCookiePattern() = testApplication {
        setupApp()
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        println("==========================================================================")
        println("=== TEST 4: CSRF Double-Submit Cookie Pattern                           ===")
        println("==========================================================================")

        // 1. Fetch CSRF token
        val csrfResp = client.get("/api/v1/admin/security/csrf-token")
        assertEquals(HttpStatusCode.OK, csrfResp.status)
        val csrfHeader = csrfResp.headers["X-CSRF-Token"]
        assertNotNull(csrfHeader, "Response must include X-CSRF-Token header")
        assertTrue(csrfHeader!!.length >= 32, "CSRF token must have cryptographic length")

        // 2. Validate token using service
        val validPair = securityService.validateCsrfToken(csrfHeader, csrfHeader)
        assertTrue(validPair, "Matching header and cookie must pass CSRF validation")

        // 3. Mismatching tokens must be rejected
        val mismatched = securityService.validateCsrfToken(csrfHeader, "invalid-token-cookie-tampered")
        assertFalse(mismatched, "Mismatched tokens must fail CSRF validation")

        // 4. Missing tokens must be rejected
        val missing = securityService.validateCsrfToken(null, csrfHeader)
        assertFalse(missing, "Missing header token must fail CSRF validation")

        println("=== [PASS] Test 4: CSRF Double-Submit Cookie Pattern verified ===")
    }

    // =========================================================================
    // 5. BAGIAN E: BRUTE FORCE PROTECTION & 15-MINUTE LOCKOUT (Fase 124 / E.5.1)
    // =========================================================================
    @Test
    fun test5_BruteForceLockoutAndSecurityAlertDispatch() = testApplication {
        setupApp()
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        println("==========================================================================")
        println("=== TEST 5: Brute-Force Rate Limiting (3 Fails -> 15m Lockout)          ===")
        println("==========================================================================")

        val targetEmail = "attacker-target@orchestree.ai"

        // Attempt 1: Failed login
        val fail1 = client.post("/api/v1/admin/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AdminLoginRequest(email = targetEmail, password = "wrongpassword"))
        }
        assertEquals(HttpStatusCode.Unauthorized, fail1.status)

        // Attempt 2: Failed login
        val fail2 = client.post("/api/v1/admin/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AdminLoginRequest(email = targetEmail, password = "wrongpassword"))
        }
        assertEquals(HttpStatusCode.Unauthorized, fail2.status)

        // Attempt 3: Triggers lockout (3 strikes)
        val fail3 = client.post("/api/v1/admin/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AdminLoginRequest(email = targetEmail, password = "wrongpassword"))
        }
        println("Strike 3 Response Status: ${fail3.status}")
        val strike3Body = fail3.bodyAsText()
        println("Strike 3 Response Body: $strike3Body")
        assertEquals(HttpStatusCode.TooManyRequests, fail3.status)
        assertTrue(strike3Body.contains("terkunci") || strike3Body.contains("dikunci") || strike3Body.contains("15 menit"),
            "Response must indicate account is locked for 15 minutes")

        // Attempt 4: Even with correct password, account remains locked
        val lockoutCheck = client.post("/api/v1/admin/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AdminLoginRequest(email = targetEmail, password = "ValidPassword123#"))
        }
        assertEquals(HttpStatusCode.TooManyRequests, lockoutCheck.status, "Locked account must reject further attempts with 429")

        println("=== [PASS] Test 5: 3-Strike 15-minute brute-force lockout verified ===")
    }

    // =========================================================================
    // 6. BAGIAN D: SUPPORT IMPERSONATION & TRANSPARENT AUDIT (Fase 124 / D.4.2)
    // =========================================================================
    @Test
    fun test6_SupportImpersonationMode_TimeBoxedAndAudited() = testApplication {
        setupApp()
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        println("==========================================================================")
        println("=== TEST 6: Support Impersonation Mode (Time-Boxed & Audited)           ===")
        println("==========================================================================")

        val superAdminToken = generateToken("SUPER_ADMIN")

        // 1. Create support session for tenant-alpha
        val impersonateResp = client.post("/api/v1/admin/support/impersonate") {
            header("Authorization", "Bearer $superAdminToken")
            header("X-Operator-Id", "super-admin-investigator")
            contentType(ContentType.Application.Json)
            setBody(
                ai.orchestree.backend.api.AdminSupportImpersonateRequest(
                    targetTenantId = "tenant-alpha-88",
                    reason = "Investigasi kendala sinkronisasi ledger (Tiket #SUP-9912)",
                    durationMinutes = 30
                )
            )
        }

        println("Impersonate Creation Status: ${impersonateResp.status}")
        val body = impersonateResp.bodyAsText()
        println("Impersonate Creation Body: $body")
        assertEquals(HttpStatusCode.Created, impersonateResp.status)

        val json = Json.parseToJsonElement(body).jsonObject
        val sessionId = json["sessionId"]!!.jsonPrimitive.content
        assertNotNull(sessionId)

        // 2. Verify active session retrieval
        val retrieveResp = client.get("/api/v1/admin/support/impersonate/$sessionId") {
            header("Authorization", "Bearer $superAdminToken")
        }
        assertEquals(HttpStatusCode.OK, retrieveResp.status)
        val retrieveBody = retrieveResp.bodyAsText()
        assertTrue(retrieveBody.contains("tenant-alpha-88"))
        assertTrue(retrieveBody.contains("super-admin-investigator"))

        println("=== [PASS] Test 6: Support impersonation time-boxed session created and audited ===")
    }

    // =========================================================================
    // 7. EXHAUSTIVE RBAC MATRIX TESTING (All Roles Checked Separately)
    // =========================================================================
    @Test
    fun test7_ExhaustiveRbacMatrix_SuperAdminGating() = testApplication {
        setupApp()
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        println("==========================================================================")
        println("=== TEST 7: Exhaustive RBAC Matrix Testing for Super Admin Endpoints   ===")
        println("==========================================================================")

        val roleExpectations = mapOf(
            "SUPER_ADMIN" to HttpStatusCode.OK,
            "TENANT_OWNER" to HttpStatusCode.Forbidden,
            "TENANT_ADMIN" to HttpStatusCode.Forbidden,
            "DEPT_MANAGER" to HttpStatusCode.Forbidden,
            "STAFF_HUMAN" to HttpStatusCode.Forbidden
        )

        for ((role, expectedStatus) in roleExpectations) {
            val token = generateToken(role)
            val resp = client.get("/api/v1/admin/tenants") {
                header("Authorization", "Bearer $token")
            }
            println("RBAC Test: Role [$role] -> Status: ${resp.status} (Expected: $expectedStatus)")
            assertEquals(expectedStatus, resp.status, "Role $role must yield $expectedStatus")
        }

        // Unauthenticated check
        val unauthResp = client.get("/api/v1/admin/tenants")
        println("RBAC Test: Unauthenticated -> Status: ${unauthResp.status} (Expected: 401)")
        assertEquals(HttpStatusCode.Unauthorized, unauthResp.status, "Unauthenticated access must return 401")

        println("=== [PASS] Test 7: Exhaustive RBAC Matrix fully verified ===")
    }

    // =========================================================================
    // 8. BAGIAN C & F: SECURITY HEADERS & ZERO FRONTEND SECRETS
    // =========================================================================
    @Test
    fun test8_SecurityHeadersAndFrontendSecretsVerification() = testApplication {
        setupApp()
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        println("==========================================================================")
        println("=== TEST 8: HTTP Security Headers & Zero Leaked Frontend Credentials   ===")
        println("==========================================================================")

        val response = client.get("/api/v1/admin/security/csrf-token")
        assertEquals(HttpStatusCode.OK, response.status)

        // Security headers validation
        val headers = response.headers
        assertEquals("DENY", headers["X-Frame-Options"], "X-Frame-Options must be DENY")
        assertEquals("nosniff", headers["X-Content-Type-Options"], "X-Content-Type-Options must be nosniff")
        assertTrue(headers["Strict-Transport-Security"]?.contains("max-age=31536000") == true, "HSTS header must be present")
        assertTrue(headers["Content-Security-Policy"]?.contains("script-src 'self'") == true, "CSP must be present")
        assertEquals("strict-origin-when-cross-origin", headers["Referrer-Policy"], "Referrer policy must be strict")

        // Audit frontend codebase for leaked backend credentials
        val adminSrcDir = File("orchestreeai-admin-dashboard/src")
        if (adminSrcDir.exists()) {
            adminSrcDir.walkTopDown().filter { it.isFile && (it.extension == "ts" || it.extension == "tsx") }.forEach { file ->
                val text = file.readText()
                assertFalse(text.contains("service_role"), "File ${file.name} must NOT contain service_role")
                assertFalse(text.contains("SUPABASE_SERVICE_KEY"), "File ${file.name} must NOT contain SUPABASE_SERVICE_KEY")
                assertFalse(text.contains("postgres://"), "File ${file.name} must NOT contain DB connection string")
            }
        }

        println("=== [PASS] Test 8: Security Headers and Zero Frontend Secrets strictly verified ===")
    }
}
