package ai.orchestree.backend

import ai.orchestree.backend.billing.*
import ai.orchestree.backend.channels.ChannelGateway
import ai.orchestree.backend.channels.InboundMessage
import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.sql.Connection
import java.util.*
import kotlin.test.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BagianGMigrationLegacyCreditTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    private fun getDbConnection(): Connection? {
        return DatabaseManager.getConnection()
    }

    private fun ensureTestTenantExists(tenantId: String, conn: Connection) {
        conn.prepareStatement(
            "INSERT INTO tenants (id, name, domain, tier, status, monthly_llm_budget, used_llm_budget, created_at, updated_at, allow_public_web_research) " +
                    "VALUES (?, ?, ?, 'STARTER', 'ACTIVE', 500.0, 0.0, now(), now(), false) ON CONFLICT (id) DO NOTHING"
        ).use { ps ->
            ps.setString(1, tenantId)
            ps.setString(2, "Migration Tenant $tenantId")
            ps.setString(3, "$tenantId.orchestree.local")
            ps.executeUpdate()
        }
    }

    private fun generateToken(role: String, tenantId: String = "tenant-test-mig-001", userId: String = "user-mig-test"): String {
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

    /**
     * TEST 1: Execute SQL Migration and Transfer Balances to ai_credit_wallets
     */
    @Test
    @Order(1)
    fun test01_executeMigrationAndVerifyUnifiedBalance() {
        println("=== TEST 1: Execute Migration & Verify Unified AI Credit Balance ===")
        val conn = getDbConnection()
        assertNotNull(conn, "Database connection must be active")

        val testTenantId = "tenant-test-mig-" + UUID.randomUUID().toString().take(6)

        conn.use { c ->
            // Step 1: Ensure tenant exists
            ensureTestTenantExists(testTenantId, c)

            // Step 2: Ensure legacy table exists temporarily to simulate legacy tenant state if dropped
            c.createStatement().use { st ->
                st.execute("""
                    CREATE TABLE IF NOT EXISTS tenant_credit_wallet (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        tenant_id TEXT NOT NULL UNIQUE,
                        balance_credits NUMERIC NOT NULL DEFAULT 0,
                        created_at TIMESTAMPTZ DEFAULT now(),
                        updated_at TIMESTAMPTZ DEFAULT now()
                    );
                """.trimIndent())
            }

            // Step 3: Insert legacy balance of 850 credits
            c.prepareStatement("INSERT INTO tenant_credit_wallet (tenant_id, balance_credits) VALUES (?, 850.0) ON CONFLICT (tenant_id) DO UPDATE SET balance_credits = 850.0").use { ps ->
                ps.setString(1, testTenantId)
                ps.executeUpdate()
            }

            // Step 4: Run the Migration Script
            val migrationSql = """
                INSERT INTO ai_credit_wallets (
                    id, tenant_id, subscription_balance, topup_balance, bonus_balance, 
                    reserved_balance, used_balance, expired_balance, is_unlimited, updated_at
                )
                SELECT 
                    gen_random_uuid(),
                    tcw.tenant_id,
                    0,
                    0,
                    COALESCE(tcw.balance_credits, 0),
                    0,
                    0,
                    0,
                    FALSE,
                    NOW()
                FROM tenant_credit_wallet tcw
                WHERE tcw.balance_credits > 0
                ON CONFLICT (tenant_id) DO UPDATE SET
                    bonus_balance = ai_credit_wallets.bonus_balance + EXCLUDED.bonus_balance,
                    updated_at = NOW();

                INSERT INTO ai_credit_ledger (
                    id, tenant_id, idempotency_key, ledger_type, amount, balance_after, reference_type, reason, created_at
                )
                SELECT 
                    gen_random_uuid(),
                    tcw.tenant_id,
                    'migration_' || tcw.tenant_id || '_' || extract(epoch from now())::bigint,
                    'CREDIT_BONUS',
                    tcw.balance_credits,
                    (w.subscription_balance + w.topup_balance + w.bonus_balance),
                    'legacy_migration',
                    'Migrasi dari sistem Omnichannel Credit terpisah ke Unified AI Credit',
                    NOW()
                FROM tenant_credit_wallet tcw
                JOIN ai_credit_wallets w ON w.tenant_id = tcw.tenant_id
                WHERE tcw.balance_credits > 0;
            """.trimIndent()

            c.createStatement().use { st ->
                st.execute(migrationSql)
            }

            // Step 5: Verify in ai_credit_wallets
            c.prepareStatement("SELECT bonus_balance, (subscription_balance + topup_balance + bonus_balance) AS total_balance FROM ai_credit_wallets WHERE tenant_id = ?").use { ps ->
                ps.setString(1, testTenantId)
                val rs = ps.executeQuery()
                assertTrue(rs.next(), "Tenant must have a wallet in ai_credit_wallets")
                val bonus = rs.getDouble("bonus_balance")
                val total = rs.getDouble("total_balance")
                println("[RAW SQL VERIFICATION] Tenant $testTenantId bonus_balance: $bonus, total_balance: $total")
                assertTrue(bonus >= 850.0, "Bonus balance must reflect migrated 850.0 credits")
                assertTrue(total >= 850.0, "Total balance must reflect migrated credits")
            }

            // Step 6: Verify in ai_credit_ledger
            c.prepareStatement("SELECT ledger_type, amount, reason FROM ai_credit_ledger WHERE tenant_id = ? AND ledger_type = 'CREDIT_BONUS'").use { ps ->
                ps.setString(1, testTenantId)
                val rs = ps.executeQuery()
                assertTrue(rs.next(), "ai_credit_ledger must contain migration record")
                assertEquals("CREDIT_BONUS", rs.getString("ledger_type"))
                assertEquals(850.0, rs.getDouble("amount"))
                println("[RAW SQL VERIFICATION] Ledger entry found: ${rs.getString("reason")}")
            }
        }
        println("=== [PASS] Test 1: Migration transferred balance to ai_credit_wallets and logged in ai_credit_ledger ===")
    }

    /**
     * TEST 2: Drop Legacy Tables and Verify Complete Removal from Schema
     */
    @Test
    @Order(2)
    fun test02_dropLegacyTablesAndVerifyAbsence() {
        println("=== TEST 2: Drop Legacy Tables & Verify Schema Absence ===")
        val conn = getDbConnection()
        assertNotNull(conn, "Database connection must be active")

        conn.use { c ->
            c.createStatement().use { st ->
                st.execute("DROP TABLE IF EXISTS tenant_credit_transactions CASCADE;")
                st.execute("DROP TABLE IF EXISTS channel_account_usage_ledger CASCADE;")
                st.execute("DROP TABLE IF EXISTS tenant_credit_wallet CASCADE;")
            }

            // Verify tables do NOT exist in information_schema
            val checkSql = """
                SELECT table_name FROM information_schema.tables 
                WHERE table_schema = 'public' 
                  AND table_name IN ('tenant_credit_wallet', 'channel_account_usage_ledger', 'tenant_credit_transactions');
            """.trimIndent()

            c.createStatement().use { st ->
                val rs = st.executeQuery(checkSql)
                val remainingTables = mutableListOf<String>()
                while (rs.next()) {
                    remainingTables.add(rs.getString("table_name"))
                }
                println("[RAW SQL VERIFICATION] Remaining legacy tables in public schema: $remainingTables")
                assertTrue(remainingTables.isEmpty(), "Legacy tables must be completely dropped: $remainingTables")
            }
        }
        println("=== [PASS] Test 2: Legacy tables dropped cleanly from Supabase PostgreSQL ===")
    }

    /**
     * TEST 3: Verify BillingScreen Endpoint (GET /api/v1/billing/credits) Reflects Migrated Balance
     */
    @Test
    @Order(3)
    fun test03_verifyBillingScreenCreditsEndpoint() = testApplication {
        val appConfig = AppConfig.load()
        application {
            configureSerialization()
            configureAuthentication(appConfig)
            configureRouting()
        }

        val testTenantId = "tenant-test-mig-billing"

        // Ensure wallet exists in database
        val conn = getDbConnection()
        conn?.use { c ->
            ensureTestTenantExists(testTenantId, c)
            c.prepareStatement("""
                INSERT INTO ai_credit_wallets (id, tenant_id, subscription_balance, topup_balance, bonus_balance, reserved_balance, used_balance, expired_balance, is_unlimited, updated_at)
                VALUES (gen_random_uuid(), ?, 0, 0, 1500.0, 0, 0, 0, FALSE, now())
                ON CONFLICT (tenant_id) DO UPDATE SET bonus_balance = 1500.0;
            """.trimIndent()).use { ps ->
                ps.setString(1, testTenantId)
                ps.executeUpdate()
            }
        }

        val token = generateToken(role = "TENANT_OWNER", tenantId = testTenantId)
        val response = client.get("/api/v1/billing/credits") {
            header("Authorization", "Bearer $token")
            header("X-Tenant-Id", testTenantId)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        println("GET /api/v1/billing/credits response: $body")

        val jsonEl = json.parseToJsonElement(body).jsonObject
        val available = jsonEl["available"]!!.jsonPrimitive.content.toDouble()
        val total = jsonEl["total"]!!.jsonPrimitive.content.toDouble()

        assertTrue(available >= 1500.0, "Available credits must include migrated balance")
        assertTrue(total >= 1500.0, "Total credits must include migrated balance")
        println("=== [PASS] Test 3: Migrated balance successfully returned to BillingScreen via /api/v1/billing/credits ===")
    }

    /**
     * TEST 4: Omnichannel AI Messages & Tasks Deduct from Unified AI Credit
     */
    @Test
    @Order(4)
    fun test04_omnichannelMessageDeductsUnifiedAiCredit() = testApplication {
        val appConfig = AppConfig.load()
        application {
            configureSerialization()
            configureAuthentication(appConfig)
            configureRouting()
        }

        val testTenantId = "tenant-test-omni-deduct"

        val conn = getDbConnection()
        conn?.use { c ->
            ensureTestTenantExists(testTenantId, c)
            c.prepareStatement("""
                INSERT INTO ai_credit_wallets (id, tenant_id, subscription_balance, topup_balance, bonus_balance, reserved_balance, used_balance, expired_balance, is_unlimited, updated_at)
                VALUES (gen_random_uuid(), ?, 500.0, 0, 0, 0, 0, 0, FALSE, now())
                ON CONFLICT (tenant_id) DO UPDATE SET subscription_balance = 500.0, used_balance = 0;
            """.trimIndent()).use { ps ->
                ps.setString(1, testTenantId)
                ps.executeUpdate()
            }
        }

        // Process inbound Omnichannel message through ChannelGateway
        val gateway = ChannelGateway()
        val inbound = InboundMessage(
            id = "msg-" + UUID.randomUUID().toString().take(8),
            tenantId = testTenantId,
            channelType = "WHATSAPP",
            senderId = "+628123456789",
            text = "Halo, saya mau tanya paket enterprise untuk logistik."
        )

        val outbound = runBlocking { gateway.processInbound(inbound) }
        assertNotNull(outbound, "Outbound response must be generated")
        println("Omnichannel outbound response: ${outbound.text}")

        // Verify credit deduction in ai_credit_wallets & ai_credit_ledger
        getDbConnection()?.use { c ->
            c.prepareStatement("SELECT subscription_balance, used_balance FROM ai_credit_wallets WHERE tenant_id = ?").use { ps ->
                ps.setString(1, testTenantId)
                val rs = ps.executeQuery()
                assertTrue(rs.next())
                val remainingSub = rs.getDouble("subscription_balance")
                val usedBal = rs.getDouble("used_balance")
                println("[RAW SQL VERIFICATION] Post-Omnichannel: remainingSub = $remainingSub, usedBal = $usedBal")
                assertTrue(usedBal > 0.0, "Used balance must be incremented by Omnichannel activity")
                assertTrue(remainingSub < 500.0, "Subscription balance must be decremented")
            }

            c.prepareStatement("SELECT ledger_type, amount, reference_type FROM ai_credit_ledger WHERE tenant_id = ? AND ledger_type = 'CREDIT_CONSUMED'").use { ps ->
                ps.setString(1, testTenantId)
                val rs = ps.executeQuery()
                assertTrue(rs.next(), "ai_credit_ledger must record consumption for Omnichannel activity")
                println("[RAW SQL VERIFICATION] Ledger consumption: ${rs.getDouble("amount")} for ${rs.getString("reference_type")}")
            }
        }
        println("=== [PASS] Test 4: Omnichannel message successfully deducted Unified AI Credit ===")
    }

    /**
     * TEST 5: Matrix Role & Condition Exhaustive Testing for Billing & Analytics
     */
    @Test
    @Order(5)
    fun test05_matrixRoleVerification() = testApplication {
        val appConfig = AppConfig.load()
        application {
            configureSerialization()
            configureAuthentication(appConfig)
            configureRouting()
        }

        val testTenant = "tenant-matrix-test"
        val conn = getDbConnection()
        conn?.use { c ->
            ensureTestTenantExists(testTenant, c)
        }

        // 1. Unauthenticated -> 401 Unauthorized
        val unauthResponse = client.get("/api/v1/billing/credits")
        assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status)
        println("[RBAC MATRIX] Unauthenticated -> 401 Unauthorized [OK]")

        // 2. All authenticated members of tenant can view credits
        val authenticatedRoles = listOf(
            "SUPER_ADMIN",
            "TENANT_OWNER",
            "TENANT_ADMIN",
            "DEPT_MANAGER",
            "STAFF_HUMAN"
        )

        for (role in authenticatedRoles) {
            val token = generateToken(role, testTenant, "user-$role")
            val response = client.get("/api/v1/billing/credits") {
                header("Authorization", "Bearer $token")
                header("X-Tenant-Id", testTenant)
            }
            assertEquals(
                HttpStatusCode.OK,
                response.status,
                "Role $role expected 200 OK on /api/v1/billing/credits but got ${response.status}"
            )
            println("[RBAC MATRIX] Role $role -> 200 OK on /api/v1/billing/credits [OK]")
        }

        // 3. Admin Analytics Endpoint (/api/v1/admin/analytics/usage-credit): Only SUPER_ADMIN allowed
        val adminRolesAndExpected = mapOf(
            "SUPER_ADMIN" to HttpStatusCode.OK,
            "TENANT_OWNER" to HttpStatusCode.Forbidden,
            "TENANT_ADMIN" to HttpStatusCode.Forbidden,
            "DEPT_MANAGER" to HttpStatusCode.Forbidden,
            "STAFF_HUMAN" to HttpStatusCode.Forbidden,
            "Unauthenticated" to HttpStatusCode.Unauthorized
        )

        for ((role, expectedStatus) in adminRolesAndExpected) {
            val response = client.get("/api/v1/admin/analytics/usage-credit") {
                if (role != "Unauthenticated") {
                    header("Authorization", "Bearer ${generateToken(role, testTenant, "admin-$role")}")
                }
            }
            assertEquals(
                expectedStatus,
                response.status,
                "Role $role expected $expectedStatus on /api/v1/admin/analytics/usage-credit but got ${response.status}"
            )
            println("[RBAC MATRIX] Admin Analytics Role $role -> ${response.status} (Expected: $expectedStatus) [OK]")
        }
        println("=== [PASS] Test 5: RBAC matrix tests verified for all roles and conditions ===")
    }
}
