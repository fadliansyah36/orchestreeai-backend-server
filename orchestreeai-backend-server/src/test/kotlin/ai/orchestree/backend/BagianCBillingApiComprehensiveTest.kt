package ai.orchestree.backend

import ai.orchestree.backend.billing.CommercialCreditEngine
import ai.orchestree.backend.billing.CreditRepositoryManager
import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.billing.EntitlementEngine
import ai.orchestree.backend.billing.MidtransWebhookPayload
import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.repositories.workforce.AgentModel
import ai.orchestree.backend.database.repositories.workforce.User
import ai.orchestree.backend.database.repositories.workforce.AgentRepository
import ai.orchestree.backend.database.repositories.workforce.UserRepository
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Connection
import java.util.Date
import java.util.UUID

/**
 * BAGIAN C — DEFINITION OF DONE & COMPREHENSIVE AUTOMATED VERIFICATION TEST:
 * 1. Public Commercial Plans API: GET /api/v1/plans, GET /api/v1/plans/{id} without auth.
 * 2. Tenant Entitlements API & Gate Middleware: GET /api/v1/tenant/entitlements and enforceEntitlementGate.
 * 3. Subscription Lifecycle with Overage Check:
 *    - Start -> Upgrade (prorated) -> Downgrade (BLOCKED when staff/agent exceed limit, SUCCESS after resolving) -> Cancel.
 * 4. Credits Balance & Ledger Pagination:
 *    - GET /api/v1/billing/credits ({available, reserved, used, total}).
 *    - POST /api/v1/billing/credits/topup.
 *    - GET /api/v1/billing/credits/ledger (paginated & filterable).
 * 5. Workforce Seats & AI Agents Management with Limit Enforcement:
 *    - GET, POST (limit check), DELETE /api/v1/billing/seats.
 *    - GET, POST (limit check), DELETE /api/v1/billing/agents.
 * 6. Invoices & Midtrans Payment / Webhook:
 *    - POST /api/v1/billing/payment, GET /api/v1/billing/invoices, GET /api/v1/billing/invoices/{id}.
 *    - POST /api/v1/billing/payment/webhook (signature verified).
 * 7. Exhaustive Role & Condition Matrix:
 *    - Unauthenticated (401) vs SUPER_ADMIN, TENANT_OWNER, TENANT_ADMIN, DEPT_MANAGER, STAFF_HUMAN (200 OK).
 * 8. Raw SQL verification and query outputs attached directly.
 */
class BagianCBillingApiComprehensiveTest {

    private val appConfig = AppConfig.load()
    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    private fun generateToken(tenantId: String, userId: String, role: String): String {
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

    private fun getLiveConnection(): Connection? {
        return DatabaseManager.getConnection()
    }

    private fun ensureTestTenantExists(tenantId: String, conn: Connection) {
        conn.prepareStatement(
            "INSERT INTO tenants (id, name, domain, tier, status, monthly_llm_budget, used_llm_budget, created_at, updated_at, allow_public_web_research) " +
                    "VALUES (?, ?, ?, 'STARTER', 'ACTIVE', 500.0, 0.0, now(), now(), false) ON CONFLICT (id) DO NOTHING"
        ).use { ps ->
            ps.setString(1, tenantId)
            ps.setString(2, "Test Tenant $tenantId")
            ps.setString(3, "$tenantId.orchestree.local")
            ps.executeUpdate()
        }
    }

    private fun sha512(input: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val bytes = md.digest(input.toByteArray(StandardCharsets.UTF_8))
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun io.ktor.server.testing.ApplicationTestBuilder.setupApp() {
        application {
            configureSerialization()
            configureAuthentication(appConfig)
            configureRouting()
        }
    }

    @Test
    fun test1_PublicCommercialPlansApi() = testApplication {
        setupApp()

        println("\n==========================================================================")
        println("=== TEST 1: Public Commercial Plans API (Without Auth) ===")
        println("==========================================================================")

        // 1. GET /api/v1/plans (public, no auth)
        val responseAll = client.get("/api/v1/plans")
        println("GET /api/v1/plans Status: ${responseAll.status}")
        val bodyAll = responseAll.bodyAsText()
        println("Plans Response: $bodyAll")
        assertEquals(HttpStatusCode.OK, responseAll.status)
        assertTrue(bodyAll.contains("Starter") || bodyAll.contains("STARTER") || bodyAll.contains("planCode"))

        // 2. GET /api/v1/plans/{id}
        val responseSingle = client.get("/api/v1/plans/plan-starter-001")
        println("GET /api/v1/plans/plan-starter-001 Status: ${responseSingle.status}")
        val bodySingle = responseSingle.bodyAsText()
        println("Single Plan Response: $bodySingle")
        assertEquals(HttpStatusCode.OK, responseSingle.status)
        assertTrue(bodySingle.contains("STARTER") || bodySingle.contains("starter"))
    }

    @Test
    fun test2_TenantEntitlementsApi_AndGate() = testApplication {
        setupApp()

        val testTenant = "tenant-test-c2-" + UUID.randomUUID().toString().take(6)
        val conn = getLiveConnection()
        assertNotNull(conn, "Database connection is required")
        conn!!.use { c ->
            ensureTestTenantExists(testTenant, c)
            val repo = CreditRepositoryManager()
            repo.upsertSubscription(testTenant, "plan-starter-001", "active", System.currentTimeMillis(), System.currentTimeMillis() + 86400000, c)
        }

        println("\n==========================================================================")
        println("=== TEST 2: Tenant Entitlements API & Gate Middleware ===")
        println("==========================================================================")

        val token = generateToken(testTenant, "user-owner", "TENANT_OWNER")

        // 1. GET /api/v1/tenant/entitlements
        val response = client.get("/api/v1/tenant/entitlements") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
        }
        println("GET /api/v1/tenant/entitlements Status: ${response.status}")
        val body = response.bodyAsText()
        println("Entitlements Response: $body")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body.contains("entitlements"))

        // 2. Verify EntitlementEngine evaluation
        val entitlementEngine = EntitlementEngine.defaultInstance
        val isAllowedIncluded = runBlocking { entitlementEngine.enforceEntitlement(testTenant, "universal_selection") }
        println("Is 'universal_selection' allowed for Starter plan? -> $isAllowedIncluded")
        assertTrue(isAllowedIncluded)

        // Custom override test: override a feature to 'not_included'
        getLiveConnection()!!.use { c ->
            c.prepareStatement(
                "UPDATE tenant_subscriptions SET custom_entitlement_override = '{\"advanced_generative_ai\": \"not_included\"}' WHERE tenant_id = ?"
            ).use { ps ->
                ps.setString(1, testTenant)
                ps.executeUpdate()
            }
        }
        val isBlockedByOverride = runBlocking { entitlementEngine.enforceEntitlement(testTenant, "advanced_generative_ai") }
        println("Is 'advanced_generative_ai' blocked after custom override? -> ${!isBlockedByOverride}")
        assertFalse(isBlockedByOverride)
    }

    @Test
    fun test3_SubscriptionLifecycle_Start_Upgrade_DowngradeOverageCheck_Cancel() = testApplication {
        setupApp()

        val testTenant = "tenant-test-c3-" + UUID.randomUUID().toString().take(6)
        val conn = getLiveConnection()
        assertNotNull(conn, "Database connection is required")
        conn!!.use { c -> ensureTestTenantExists(testTenant, c) }

        val token = generateToken(testTenant, "user-owner", "TENANT_OWNER")

        println("\n==========================================================================")
        println("=== TEST 3: Subscription Lifecycle & Overage Check on Downgrade ===")
        println("==========================================================================")

        // 1. POST /api/v1/billing/subscription (Start subscription on PRO)
        val startResponse = client.post("/api/v1/billing/subscription") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
            contentType(ContentType.Application.Json)
            setBody("""{"planId":"plan-pro-002","billingInterval":"monthly"}""")
        }
        println("Start Subscription Status: ${startResponse.status}")
        val startBody = startResponse.bodyAsText()
        println("Start Response: $startBody")
        assertEquals(HttpStatusCode.Created, startResponse.status)
        assertTrue(startBody.contains("active"))

        // 2. GET /api/v1/billing/subscription
        val getSubResponse = client.get("/api/v1/billing/subscription") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
        }
        assertEquals(HttpStatusCode.OK, getSubResponse.status)
        println("Current Subscription: ${getSubResponse.bodyAsText()}")

        // 3. POST /api/v1/billing/subscription/upgrade (Upgrade to ENTERPRISE)
        val upgradeResponse = client.post("/api/v1/billing/subscription/upgrade") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
            contentType(ContentType.Application.Json)
            setBody("""{"targetPlanId":"plan-enterprise-003"}""")
        }
        println("Upgrade Status: ${upgradeResponse.status}")
        val upgradeBody = upgradeResponse.bodyAsText()
        println("Upgrade Response: $upgradeBody")
        assertEquals(HttpStatusCode.OK, upgradeResponse.status)
        assertTrue(upgradeBody.contains("upgraded"))

        // 4. DOWNGRADE WITH OVERAGE CHECK (LANGKAH 1 DOKUMEN SUMBER):
        // Retrieve target plan limits dynamically from database
        val repo = CreditRepositoryManager()
        val starterPlan = repo.getCommercialPlan("plan-starter-001")
        val starterSeatLimit = starterPlan.humanSeatLimit ?: 10
        val starterAgentLimit = starterPlan.aiAgentLimit ?: 10
        val userRepo = UserRepository.defaultInstance
        val agentRepo = AgentRepository.defaultInstance

        // Seed overages: (starterSeatLimit + 2) staff and (starterAgentLimit + 2) agents
        for (i in 1..(starterSeatLimit + 2)) {
            userRepo.save(User(id = "user-$testTenant-$i", tenantId = testTenant, name = "Staff $i", email = "staff$i@test.com", role = "STAFF_HUMAN"))
        }
        for (i in 1..(starterAgentLimit + 2)) {
            agentRepo.save(AgentModel(id = "agent-$testTenant-$i", tenantId = testTenant, name = "Agent $i", role = "AI_AGENT", personaCode = "SALES_PRO", status = "ACTIVE"))
        }

        println("\n>>> Attempting Downgrade to Starter when usage exceeds limits (${starterSeatLimit + 2} staff vs $starterSeatLimit, ${starterAgentLimit + 2} agents vs $starterAgentLimit)...")
        val downgradeBlockedResponse = client.post("/api/v1/billing/subscription/downgrade") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
            contentType(ContentType.Application.Json)
            setBody("""{"targetPlanId":"plan-starter-001"}""")
        }
        println("Downgrade Blocked Status: ${downgradeBlockedResponse.status}")
        val downgradeBlockedBody = downgradeBlockedResponse.bodyAsText()
        println("Downgrade Blocked Response: $downgradeBlockedBody")
        assertEquals(HttpStatusCode.Conflict, downgradeBlockedResponse.status)
        assertTrue(downgradeBlockedBody.contains("blocked"))
        assertTrue(downgradeBlockedBody.contains("Your current usage exceeds the target plan limit"))

        // Verify users and agents were NOT deleted
        assertEquals(starterSeatLimit + 2, userRepo.countActiveByTenant(testTenant))
        assertEquals(starterAgentLimit + 2, agentRepo.countActiveByTenant(testTenant))
        println("Verified: Staff (${starterSeatLimit + 2}) and AI Agents (${starterAgentLimit + 2}) were preserved without deletion!")

        // Now remove excess staff and agents to comply with Starter limits
        userRepo.delete("user-$testTenant-${starterSeatLimit + 1}")
        userRepo.delete("user-$testTenant-${starterSeatLimit + 2}")
        agentRepo.delete("agent-$testTenant-${starterAgentLimit + 1}")
        agentRepo.delete("agent-$testTenant-${starterAgentLimit + 2}")

        println("\n>>> Re-attempting Downgrade to Starter after resolving overages (now $starterSeatLimit staff, $starterAgentLimit agents)...")
        val downgradeSuccessResponse = client.post("/api/v1/billing/subscription/downgrade") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
            contentType(ContentType.Application.Json)
            setBody("""{"targetPlanId":"plan-starter-001"}""")
        }
        println("Downgrade Success Status: ${downgradeSuccessResponse.status}")
        val downgradeSuccessBody = downgradeSuccessResponse.bodyAsText()
        println("Downgrade Success Response: $downgradeSuccessBody")
        assertEquals(HttpStatusCode.OK, downgradeSuccessResponse.status)
        assertTrue(downgradeSuccessBody.contains("downgraded"))

        // 5. POST /api/v1/billing/subscription/cancel
        val cancelResponse = client.post("/api/v1/billing/subscription/cancel") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
        }
        assertEquals(HttpStatusCode.OK, cancelResponse.status)
        println("Cancelled Subscription Response: ${cancelResponse.bodyAsText()}")
    }

    @Test
    fun test4_CreditsBalanceAndLedgerPagination() = testApplication {
        setupApp()

        val testTenant = "tenant-test-c4-" + UUID.randomUUID().toString().take(6)
        val conn = getLiveConnection()
        assertNotNull(conn, "Database connection is required")
        conn!!.use { c ->
            ensureTestTenantExists(testTenant, c)
            val repo = CreditRepositoryManager()
            repo.upsertSubscription(testTenant, "plan-starter-001", "active", System.currentTimeMillis(), System.currentTimeMillis() + 86400000, c)
            repo.recordLedgerEntry(testTenant, "TOPUP", 200.0, 200.0, "TASK-1", "test_topup", null, c)
            repo.recordLedgerEntry(testTenant, "CONSUMED", -50.0, 150.0, "TASK-2", "test_consume", "gpt-4o", c)
        }

        val token = generateToken(testTenant, "user-owner", "TENANT_OWNER")

        println("\n==========================================================================")
        println("=== TEST 4: Credits Balance Display & Ledger Pagination ===")
        println("==========================================================================")

        // 1. GET /api/v1/billing/credits (Exact format: {available, reserved, used, total})
        val creditsResponse = client.get("/api/v1/billing/credits") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
        }
        println("GET /api/v1/billing/credits Status: ${creditsResponse.status}")
        val creditsBody = creditsResponse.bodyAsText()
        println("Credits Response: $creditsBody")
        assertEquals(HttpStatusCode.OK, creditsResponse.status)
        assertTrue(creditsBody.contains("available"))
        assertTrue(creditsBody.contains("reserved"))
        assertTrue(creditsBody.contains("used"))
        assertTrue(creditsBody.contains("total"))

        // 2. POST /api/v1/billing/credits/topup
        val topupResponse = client.post("/api/v1/billing/credits/topup") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
            contentType(ContentType.Application.Json)
            setBody("""{"amount":500.0,"amountPaid":500000.0,"currency":"IDR","reference":"INV-TOPUP-001"}""")
        }
        println("Topup Status: ${topupResponse.status}")
        val topupBody = topupResponse.bodyAsText()
        println("Topup Response: $topupBody")
        assertEquals(HttpStatusCode.OK, topupResponse.status)
        assertTrue(topupBody.contains("success"))

        // 3. GET /api/v1/billing/credits/ledger (Paginated)
        val ledgerResponse = client.get("/api/v1/billing/credits/ledger?limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
        }
        println("Ledger Status: ${ledgerResponse.status}")
        val ledgerBody = ledgerResponse.bodyAsText()
        println("Ledger Response: $ledgerBody")
        assertEquals(HttpStatusCode.OK, ledgerResponse.status)
        assertTrue(ledgerBody.contains("entries"))
        assertTrue(ledgerBody.contains("total"))
    }

    @Test
    fun test5_WorkforceSeatsAndAgentsManagementWithLimits() = testApplication {
        setupApp()

        val testTenant = "tenant-test-c5-" + UUID.randomUUID().toString().take(6)
        val conn = getLiveConnection()
        assertNotNull(conn, "Database connection is required")
        conn!!.use { c ->
            ensureTestTenantExists(testTenant, c)
            val repo = CreditRepositoryManager()
            // Starter plan: 3 human seats, 2 AI agents
            repo.upsertSubscription(testTenant, "plan-starter-001", "active", System.currentTimeMillis(), System.currentTimeMillis() + 86400000, c)
        }

        val token = generateToken(testTenant, "user-owner", "TENANT_OWNER")

        println("\n==========================================================================")
        println("=== TEST 5: Workforce Seats & AI Agents with Plan Limits ===")
        println("==========================================================================")

        // 1. GET /api/v1/billing/seats
        val getSeats = client.get("/api/v1/billing/seats") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
        }
        assertEquals(HttpStatusCode.OK, getSeats.status)
        println("Initial Seats: ${getSeats.bodyAsText()}")

        val plan = CreditRepositoryManager().getCommercialPlan("plan-starter-001")
        val seatLimit = plan.humanSeatLimit ?: 10
        val agentLimit = plan.aiAgentLimit ?: 10

        // 2. POST /api/v1/billing/seats up to limit
        var lastSeatId = ""
        for (i in 1..seatLimit) {
            val res = client.post("/api/v1/billing/seats") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("X-Tenant-Id", testTenant)
                contentType(ContentType.Application.Json)
                setBody("""{"name":"Staff $i","email":"staff$i@$testTenant.com","role":"STAFF_HUMAN","departmentId":"dept-sales"}""")
            }
            assertEquals(HttpStatusCode.Created, res.status)
            lastSeatId = "user-$testTenant-$i"
        }

        // 3. POST (seatLimit + 1)th seat -> should be blocked by SEAT_LIMIT_EXCEEDED
        val seatOverLimit = client.post("/api/v1/billing/seats") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Excess Staff","email":"excess@$testTenant.com","role":"STAFF_HUMAN","departmentId":"dept-sales"}""")
        }
        println("Excess Seat Status: ${seatOverLimit.status}")
        assertEquals(HttpStatusCode.Forbidden, seatOverLimit.status)
        assertTrue(seatOverLimit.bodyAsText().contains("SEAT_LIMIT_EXCEEDED"))

        // 4. POST /api/v1/billing/agents up to limit
        var lastAgentId = ""
        for (i in 1..agentLimit) {
            val res = client.post("/api/v1/billing/agents") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("X-Tenant-Id", testTenant)
                contentType(ContentType.Application.Json)
                setBody("""{"name":"Agent $i","role":"AI_AGENT","personaCode":"SALES_PRO"}""")
            }
            assertEquals(HttpStatusCode.Created, res.status)
            lastAgentId = "agt-$testTenant-$i"
        }

        // 5. POST (agentLimit + 1)th agent -> should be blocked by AGENT_LIMIT_EXCEEDED
        val agentOverLimit = client.post("/api/v1/billing/agents") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Excess Agent","role":"AI_AGENT","personaCode":"SALES_PRO"}""")
        }
        println("Excess Agent Status: ${agentOverLimit.status}")
        assertEquals(HttpStatusCode.Forbidden, agentOverLimit.status)
        assertTrue(agentOverLimit.bodyAsText().contains("AGENT_LIMIT_EXCEEDED"))
    }

    @Test
    fun test6_InvoicesAndMidtransPaymentFlow() = testApplication {
        setupApp()

        val testTenant = "tenant-test-c6-" + UUID.randomUUID().toString().take(6)
        val conn = getLiveConnection()
        assertNotNull(conn, "Database connection is required")
        conn!!.use { c -> ensureTestTenantExists(testTenant, c) }

        val token = generateToken(testTenant, "user-owner", "TENANT_OWNER")

        println("\n==========================================================================")
        println("=== TEST 6: Invoices & Midtrans Payment / Webhook ===")
        println("==========================================================================")

        // 1. POST /api/v1/billing/payment (Initiate payment)
        val payResponse = client.post("/api/v1/billing/payment") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
            contentType(ContentType.Application.Json)
            setBody("""{"planId":"Professional Subscription","amount":990000.0}""")
        }
        println("Payment Initiate Status: ${payResponse.status}")
        val payBody = payResponse.bodyAsText()
        println("Payment Initiate Body: $payBody")
        assertEquals(HttpStatusCode.OK, payResponse.status)
        assertTrue(payBody.contains("payment_url"))
        assertTrue(payBody.contains("snap_token"))

        // 2. GET /api/v1/billing/invoices
        val getInvoices = client.get("/api/v1/billing/invoices") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Tenant-Id", testTenant)
        }
        assertEquals(HttpStatusCode.OK, getInvoices.status)
        println("Invoices List: ${getInvoices.bodyAsText()}")

        // 3. POST /api/v1/billing/payment/webhook with valid signature
        val serverKey = ai.orchestree.backend.config.EnvLoader.get("MIDTRANS_SERVER_KEY", "SB-Mid-server-TEST-KEY-ORCHESTREE-2026")
        val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val parsedPay = jsonParser.decodeFromString<ai.orchestree.backend.billing.PaymentInitiateResponse>(payBody)
        val orderId = parsedPay.invoiceId
        val statusCode = "200"
        val grossAmount = "990000.00"
        val signatureRaw = orderId + statusCode + grossAmount + serverKey
        val signatureKey = sha512(signatureRaw)

        val webhookResponse = client.post("/api/v1/billing/payment/webhook") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "order_id": "$orderId",
                    "status_code": "$statusCode",
                    "gross_amount": "$grossAmount",
                    "signature_key": "$signatureKey",
                    "transaction_status": "settlement",
                    "fraud_status": "accept",
                    "custom_field1": "$testTenant",
                    "custom_field2": "plan-pro-002"
                }
                """.trimIndent()
            )
        }
        println("Webhook Status: ${webhookResponse.status}")
        val webhookBody = webhookResponse.bodyAsText()
        println("Webhook Response: $webhookBody")
        assertEquals(HttpStatusCode.OK, webhookResponse.status)
        assertTrue(webhookBody.contains("processed"))
    }

    @Test
    fun test7_ExhaustiveRoleAndConditionMatrix() = testApplication {
        setupApp()

        val testTenant = "tenant-test-c7-" + UUID.randomUUID().toString().take(6)
        val conn = getLiveConnection()
        assertNotNull(conn, "Database connection is required")
        conn!!.use { c ->
            ensureTestTenantExists(testTenant, c)
            val repo = CreditRepositoryManager()
            repo.upsertSubscription(testTenant, "plan-starter-001", "active", System.currentTimeMillis(), System.currentTimeMillis() + 86400000, c)
        }

        println("\n==========================================================================")
        println("=== TEST 7: Exhaustive Role & Condition Matrix ===")
        println("==========================================================================")

        // 1. Unauthenticated request to protected endpoint -> MUST BE 401 Unauthorized
        val unauthResponse = client.get("/api/v1/billing/subscription")
        println("Condition [Unauthenticated] -> HTTP Status: ${unauthResponse.status}")
        assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status)

        // 2. Matrix of 5 Roles
        val roles = listOf(
            "SUPER_ADMIN",
            "TENANT_OWNER",
            "TENANT_ADMIN",
            "DEPT_MANAGER",
            "STAFF_HUMAN"
        )

        for (role in roles) {
            val token = generateToken(testTenant, "user-$role", role)
            val response = client.get("/api/v1/billing/subscription") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("X-Tenant-Id", testTenant)
            }
            println("Role Matrix [$role] -> HTTP Status: ${response.status}")
            assertEquals(HttpStatusCode.OK, response.status, "Role $role should have access to billing subscription")
        }
    }

    @Test
    fun test8_RawSqlVerification() = runBlocking {
        println("\n==========================================================================")
        println("=== TEST 8: Raw SQL Verification on Supabase PostgreSQL ===")
        println("==========================================================================")

        val conn = getLiveConnection()
        assertNotNull(conn, "Database connection is required")

        conn!!.use { c ->
            // Query 1: Commercial Plans
            println("\n[SQL 1]: SELECT id, plan_code, plan_name, price, currency, credit_allocation, human_seat_limit, ai_agent_limit FROM commercial_plans LIMIT 5;")
            c.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT id, plan_code, plan_name, price, currency, credit_allocation, human_seat_limit, ai_agent_limit FROM commercial_plans ORDER BY price ASC LIMIT 5;")
                var count = 0
                while (rs.next()) {
                    count++
                    println("  Row $count -> id: ${rs.getString("id")}, code: ${rs.getString("plan_code")}, name: ${rs.getString("plan_name")}, price: ${rs.getDouble("price")}, credits: ${rs.getDouble("credit_allocation")}, seats: ${rs.getInt("human_seat_limit")}, agents: ${rs.getInt("ai_agent_limit")}")
                }
                assertTrue(count >= 3, "At least 3 commercial plans must exist")
            }

            // Query 2: Plan Feature Entitlements
            println("\n[SQL 2]: SELECT plan_id, feature_key, entitlement_value FROM plan_feature_entitlements LIMIT 5;")
            c.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT plan_id, feature_key, entitlement_value FROM plan_feature_entitlements LIMIT 5;")
                var count = 0
                while (rs.next()) {
                    count++
                    println("  Row $count -> plan: ${rs.getString("plan_id")}, feature: ${rs.getString("feature_key")}, value: ${rs.getString("entitlement_value")}")
                }
                assertTrue(count > 0, "Feature entitlements must exist")
            }

            // Query 3: Wallets
            println("\n[SQL 3]: SELECT tenant_id, subscription_balance, topup_balance, bonus_balance, reserved_balance, used_balance FROM ai_credit_wallets LIMIT 5;")
            c.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT tenant_id, subscription_balance, topup_balance, bonus_balance, reserved_balance, used_balance FROM ai_credit_wallets LIMIT 5;")
                var count = 0
                while (rs.next()) {
                    count++
                    println("  Row $count -> tenant: ${rs.getString("tenant_id")}, sub: ${rs.getDouble("subscription_balance")}, topup: ${rs.getDouble("topup_balance")}, used: ${rs.getDouble("used_balance")}")
                }
            }

            // Query 4: Central Credit Ledger
            println("\n[SQL 4]: SELECT tenant_id, ledger_type, amount, balance_after, reference_type, created_at FROM ai_credit_ledger ORDER BY created_at DESC LIMIT 5;")
            c.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT tenant_id, ledger_type, amount, balance_after, reference_type, created_at FROM ai_credit_ledger ORDER BY created_at DESC LIMIT 5;")
                var count = 0
                while (rs.next()) {
                    count++
                    println("  Row $count -> tenant: ${rs.getString("tenant_id")}, type: ${rs.getString("ledger_type")}, amount: ${rs.getDouble("amount")}, balance: ${rs.getDouble("balance_after")}, refType: ${rs.getString("reference_type")}")
                }
            }
        }
        println("\n>>> All Raw SQL verification queries completed successfully against Supabase PostgreSQL! <<<")
    }
}
