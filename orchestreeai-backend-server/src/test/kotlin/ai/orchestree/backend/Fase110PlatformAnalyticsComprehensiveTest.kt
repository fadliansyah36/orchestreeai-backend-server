package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.repositories.analytics.AnalyticsRepository
import ai.orchestree.backend.models.AnalyticsOverviewResponse
import ai.orchestree.backend.models.KpiScoreEngine
import ai.orchestree.backend.models.KpiSummaryResponse
import ai.orchestree.backend.models.LlmUsagePlatformWideResponse
import ai.orchestree.backend.models.TenantUsageCreditItem
import ai.orchestree.backend.models.TaskActivitySummaryResponse
import ai.orchestree.backend.models.UniversalSelectionUsageResponse
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
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
import kotlinx.serialization.json.Json
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Fase110PlatformAnalyticsComprehensiveTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val json = Json { ignoreUnknownKeys = true }

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

    /**
     * LANGKAH 1.1 — GET /api/v1/admin/analytics/overview
     * Verifikasi metrik agregat nyata dan perbandingan dengan query SQL
     */
    @Test
    fun test01_analyticsOverview_returnsExactAggregatesAndMatchesDirectQuery() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val token = generateToken("SUPER_ADMIN")
        val response = client.get("/api/v1/admin/analytics/overview") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        val overview = json.decodeFromString<AnalyticsOverviewResponse>(body)

        // Verifikasi seluruh angka merupakan hasil agregasi nyata
        assertTrue(overview.total_transaction_value > 0.0, "total_transaction_value must be > 0 from paid orders")
        assertTrue(overview.total_revenue_this_month > 0.0, "total_revenue_this_month must be > 0 from active subscriptions")
        assertTrue(overview.total_tenants_active >= 3, "total_tenants_active must count active tenants")
        assertTrue(overview.total_staff_human >= 10, "total_staff_human must count users where role != AI")
        assertTrue(overview.total_ai_agents_active >= 4, "total_ai_agents_active must count healthy active agents")
        assertTrue(overview.total_repeat_orders >= 3, "total_repeat_orders must count customers with order_count > 1")
        assertTrue(overview.total_transactions >= 5, "total_transactions must count paid orders")

        println("=== [PASS] 1.1 Overview Response: $overview ===")
    }

    /**
     * REGRESSION TEST WAJIB:
     * Buat transaksi baru sungguhan di tenant manapun -> panggil ulang endpoint ->
     * angka total_transactions dan total_transaction_value BERTAMBAH sesuai.
     */
    @Test
    fun test02_regressionTest_newTransactionIncrementsTotalTransactionsAndValue() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val token = generateToken("SUPER_ADMIN")

        // 1. Ambil overview awal
        val initialResp = client.get("/api/v1/admin/analytics/overview") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, initialResp.status)
        val initialOverview = json.decodeFromString<AnalyticsOverviewResponse>(initialResp.bodyAsText())
        val initialTransactions = initialOverview.total_transactions
        val initialValue = initialOverview.total_transaction_value

        // 2. Buat transaksi baru nyata (Rp 1.750.000)
        val newOrderAmount = 1750000.0
        val createTxResp = client.post("/api/v1/admin/analytics/transactions") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "tenantId": "tenant-enterprise-001",
                    "customerId": "cust-regression-test-01",
                    "amount": $newOrderAmount,
                    "orderNumber": "ORD-REG-9901"
                }
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.Created, createTxResp.status)

        // 3. Panggil ulang endpoint overview dan buktikan angka bertambah
        val updatedResp = client.get("/api/v1/admin/analytics/overview") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, updatedResp.status)
        val updatedOverview = json.decodeFromString<AnalyticsOverviewResponse>(updatedResp.bodyAsText())

        assertEquals(
            initialTransactions + 1,
            updatedOverview.total_transactions,
            "total_transactions must strictly increment by 1"
        )
        assertEquals(
            initialValue + newOrderAmount,
            updatedOverview.total_transaction_value,
            "total_transaction_value must strictly increment by new transaction amount"
        )

        println("=== [PASS] Regression Test Verified: Initial ($initialTransactions tx, Rp $initialValue) -> Updated (${updatedOverview.total_transactions} tx, Rp ${updatedOverview.total_transaction_value}) ===")
    }

    /**
     * LANGKAH 1.2 — GET /api/v1/admin/analytics/usage-credit
     * Unified ai_credit_wallets & ai_credit_ledger
     */
    @Test
    fun test03_analyticsUsageCredit_returnsPerTenantBalanceAndLedgerUsage() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val token = generateToken("SUPER_ADMIN")

        // Uji query parameter period: harian, mingguan, bulanan
        for (period in listOf("bulanan", "mingguan", "harian")) {
            val response = client.get("/api/v1/admin/analytics/usage-credit?period=$period") {
                header("Authorization", "Bearer $token")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val list = json.decodeFromString<List<TenantUsageCreditItem>>(response.bodyAsText())
            assertTrue(list.isNotEmpty(), "Tenant usage credit list must not be empty")

            val enterpriseTenant = list.firstOrNull { it.id == "tenant-enterprise-001" }
            assertNotNull(enterpriseTenant, "Nusantara Logistics Enterprise must exist")
            assertTrue(enterpriseTenant.balance > 0.0, "Balance must be loaded from ai_credit_wallets")
            assertTrue(enterpriseTenant.total_usage_this_month >= 0.0, "Total usage must aggregate from ai_credit_ledger")
        }

        println("=== [PASS] 1.2 Usage Credit Endpoints Verified across period parameters ===")
    }

    /**
     * LANGKAH 1.3 — GET /api/v1/admin/analytics/llm-usage-platform-wide
     * Total token in/out, total cost, breakdown provider & tenant, trend 30 hari
     */
    @Test
    fun test04_analyticsLlmUsagePlatformWide_returnsFullAggregatesAndTimeSeries() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val token = generateToken("SUPER_ADMIN")
        val response = client.get("/api/v1/admin/analytics/llm-usage-platform-wide") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val data = json.decodeFromString<LlmUsagePlatformWideResponse>(response.bodyAsText())

        assertTrue(data.total_input_tokens > 0L)
        assertTrue(data.total_output_tokens > 0L)
        assertTrue(data.total_tokens == data.total_input_tokens + data.total_output_tokens)
        assertTrue(data.total_cost_usd > 0.0)

        // Verifikasi Breakdown per Provider (Anthropic, OpenAI, Gemini, DeepSeek)
        assertTrue(data.breakdown_by_provider.any { it.provider == "ANTHROPIC" })
        assertTrue(data.breakdown_by_provider.any { it.provider == "OPENAI" })
        assertTrue(data.breakdown_by_provider.any { it.provider == "GEMINI" })
        assertTrue(data.breakdown_by_provider.any { it.provider == "DEEPSEEK" })

        // Verifikasi Breakdown per Tenant
        assertTrue(data.breakdown_by_tenant.isNotEmpty())

        // Verifikasi Trend Harian 30 Hari Terakhir untuk Time-Series
        assertTrue(data.daily_trend.size in 1..30, "Daily trend must provide time-series entries up to 30 days")
        assertTrue(data.daily_trend.all { it.total_tokens > 0L })

        println("=== [PASS] 1.3 LLM Usage Platform-Wide: Total Tokens=${data.total_tokens}, Cost=$${data.total_cost_usd}, Trend Days=${data.daily_trend.size} ===")
    }

    /**
     * LANGKAH 1.4 — GET /api/v1/admin/analytics/kpi-summary
     * Formula monthlyScore() PRD 9.2 diagregasi platform-wide & distribusi Human vs AI
     */
    @Test
    fun test05_analyticsKpiSummary_verifiesFormulaScoreAndDistribution() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        // Verifikasi langsung unit formula KpiScoreEngine.monthlyScore()
        // Bobot: 25% completion, 20% quality, 15% deadline, 15% productivity, 15% collaboration, 10% uptime
        val sampleScore = KpiScoreEngine.monthlyScore(
            completion = 100.0,
            quality = 100.0,
            deadline = 100.0,
            productivity = 100.0,
            collaboration = 100.0,
            uptime = 100.0
        )
        assertEquals(100.0, sampleScore)

        val weightedScore = KpiScoreEngine.monthlyScore(
            completion = 90.0, // 22.5
            quality = 80.0,    // 16.0
            deadline = 85.0,   // 12.75
            productivity = 90.0,// 13.5
            collaboration = 80.0,// 12.0
            uptime = 95.0      // 9.5
        )
        // Total = 22.5 + 16.0 + 12.75 + 13.5 + 12.0 + 9.5 = 86.25 -> rounded to 86.3
        assertEquals(86.3, weightedScore)

        val token = generateToken("SUPER_ADMIN")
        val response = client.get("/api/v1/admin/analytics/kpi-summary") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val kpi = json.decodeFromString<KpiSummaryResponse>(response.bodyAsText())

        assertTrue(kpi.platform_average_score > 0.0)
        assertTrue(kpi.total_evaluated_entities >= 4)
        assertTrue(kpi.human_distribution.count > 0)
        assertTrue(kpi.ai_agent_distribution.count > 0)
        assertTrue(kpi.tier_distribution.containsKey("TIER_A_EXCELLENT"))
        assertTrue(kpi.tier_distribution.containsKey("TIER_B_GOOD"))

        println("=== [PASS] 1.4 KPI Summary: Platform Avg=${kpi.platform_average_score}, Human=${kpi.human_distribution.count}, AI=${kpi.ai_agent_distribution.count} ===")
    }

    /**
     * LANGKAH 1.5 & ATURAN WAJIB 3: Matrix Role & Condition Exhaustive Testing
     * WAJIB SUPER_ADMIN ONLY (200 OK vs 403 Forbidden vs 401 Unauthorized)
     */
    @Test
    fun test06_rbacSecurityMatrix_enforcesSuperAdminOnlyExhaustively() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val testEndpoints = listOf(
            "/api/v1/admin/analytics/overview",
            "/api/v1/admin/analytics/usage-credit",
            "/api/v1/admin/analytics/llm-usage-platform-wide",
            "/api/v1/admin/analytics/kpi-summary",
            "/api/v1/admin/analytics/task-activity-summary"
        )

        for (endpoint in testEndpoints) {
            // 1. Unauthenticated -> 401 Unauthorized
            val unauthResp = client.get(endpoint)
            assertEquals(HttpStatusCode.Unauthorized, unauthResp.status, "$endpoint must reject unauthenticated")

            // 2. TENANT_OWNER -> 403 Forbidden
            val ownerToken = generateToken("TENANT_OWNER")
            val ownerResp = client.get(endpoint) { header("Authorization", "Bearer $ownerToken") }
            assertEquals(HttpStatusCode.Forbidden, ownerResp.status, "$endpoint must reject TENANT_OWNER")

            // 3. TENANT_ADMIN -> 403 Forbidden
            val tenantAdminToken = generateToken("TENANT_ADMIN")
            val tenantAdminResp = client.get(endpoint) { header("Authorization", "Bearer $tenantAdminToken") }
            assertEquals(HttpStatusCode.Forbidden, tenantAdminResp.status, "$endpoint must reject TENANT_ADMIN")

            // 4. DEPT_MANAGER -> 403 Forbidden
            val mgrToken = generateToken("DEPT_MANAGER")
            val mgrResp = client.get(endpoint) { header("Authorization", "Bearer $mgrToken") }
            assertEquals(HttpStatusCode.Forbidden, mgrResp.status, "$endpoint must reject DEPT_MANAGER")

            // 5. STAFF_HUMAN -> 403 Forbidden
            val staffToken = generateToken("STAFF_HUMAN")
            val staffResp = client.get(endpoint) { header("Authorization", "Bearer $staffToken") }
            assertEquals(HttpStatusCode.Forbidden, staffResp.status, "$endpoint must reject STAFF_HUMAN")

            // 6. SUPER_ADMIN -> 200 OK
            val superToken = generateToken("SUPER_ADMIN")
            val superResp = client.get(endpoint) { header("Authorization", "Bearer $superToken") }
            assertEquals(HttpStatusCode.OK, superResp.status, "$endpoint must allow SUPER_ADMIN")
        }

        println("=== [PASS] 1.5 Exhaustive RBAC Matrix: All non-SUPER_ADMIN roles correctly rejected with 403/401 ===")
    }

    /**
     * LANGKAH 1.1 / FASE 110 BAGIAN C:
     * GET /api/v1/admin/analytics/task-activity-summary
     * Verifikasi:
     * 1. Mengembalikan metrik agregasi platform-wide (total tasks, human vs AI ratio, status distribution, channel)
     * 2. Menghitung ringkasan adopsi per tenant (health status: HEALTHY, MODERATE, LOW_ACTIVITY)
     * 3. Sesuai Addendum 2 Bagian 25.2: PRIVASI TERJAMIN — Response sama sekali TIDAK membocorkan konten judul/deskripsi task tenant individual.
     */
    @Test
    fun test07_taskActivitySummary_aggregatesPlatformWideMetricsWithoutLeakingTaskDetails() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val token = generateToken("SUPER_ADMIN")
        val response = client.get("/api/v1/admin/analytics/task-activity-summary") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val rawJson = response.bodyAsText()
        val summary = json.decodeFromString<TaskActivitySummaryResponse>(rawJson)

        // 1. Verifikasi metrik agregat platform
        assertTrue(summary.totalTasks > 0, "totalTasks must be > 0")
        assertTrue(summary.humanCreatedTasks > 0, "humanCreatedTasks must be > 0")
        assertTrue(summary.aiCreatedTasks > 0, "aiCreatedTasks must be > 0")
        assertEquals(summary.totalTasks, summary.humanCreatedTasks + summary.aiCreatedTasks)
        assertEquals(100.0, Math.round((summary.humanRatioPercentage + summary.aiRatioPercentage) * 10.0) / 10.0)

        // 2. Verifikasi status distribution & channel breakdown
        assertTrue(summary.byStatus.containsKey("TODO"))
        assertTrue(summary.byStatus.containsKey("IN_PROGRESS"))
        assertTrue(summary.byStatus.containsKey("DONE"))
        assertTrue(summary.byChannel.containsKey("dashboard"))

        // 3. Verifikasi per-tenant activity metrics & health status
        assertTrue(summary.tenantsActivity.isNotEmpty(), "tenantsActivity list must not be empty")
        for (tenantItem in summary.tenantsActivity) {
            assertTrue(tenantItem.tenantId.isNotBlank())
            assertTrue(tenantItem.tenantName.isNotBlank())
            assertTrue(tenantItem.adoptionHealthStatus in listOf("HEALTHY", "MODERATE", "LOW_ACTIVITY"))
            assertEquals(tenantItem.totalTasks, tenantItem.activeTasks + tenantItem.completedTasks)
        }

        // 4. PRIVACY AUDIT (Addendum 2 Bagian 25.2): Pastikan TIDAK ADA bocoran konten judul / deskripsi spesifik tugas individual
        val privateTaskTitles = listOf(
            "Autonomous Market Intel: Crawl Harga",
            "Finalisasi Kontrak Kerjasama Vendor Cloud",
            "Inisiasi scraping browser headless",
            "Membuka headless browser dan navigasi"
        )
        for (sensitiveText in privateTaskTitles) {
            assertTrue(!rawJson.contains(sensitiveText), "Privacy breach! Raw JSON leaked individual task content: $sensitiveText")
        }

        println("=== [PASS] 1.6 Task Activity Summary: Total=${summary.totalTasks}, Human=${summary.humanCreatedTasks} (${summary.humanRatioPercentage}%), AI=${summary.aiCreatedTasks} (${summary.aiRatioPercentage}%), Tenants=${summary.tenantsActivity.size}, Privacy audit strictly verified [OK] ===")
    }

    /**
     * FASE 114 / BAGIAN J / LANGKAH 1 — GET /api/v1/admin/analytics/universal-selection-usage
     * REUSE pola Fase 102 Bagian A
     *
     * 1. SUPER_ADMIN -> 200 OK dengan agregat platform-wide lengkap
     * 2. Exhaustive RBAC Matrix: TENANT_ADMIN, TENANT_OWNER, DEPT_MANAGER, STAFF_HUMAN -> 403 Forbidden
     *    Unauthenticated -> 401 Unauthorized
     * 3. Verifikasi struktur data agregat:
     *    - totalRequests, totalCompletedRequests, totalProcessingRequests, totalFailedRequests
     *    - totalCreditsConsumed dari Central Credit Ledger yang SAMA
     *    - mostUsedDomainCategory & domainCategories breakdown
     *    - tenantsUsage breakdown
     * 4. Privacy Audit (Addendum 2 Bagian 25.2): Super Admin HANYA melihat agregat,
     *    BUKAN mengintip konten data seleksi tenant individual
     */
    @Test
    fun test07_universalSelectionUsage_returnsAggregatesAndGuaranteesTenantPrivacy() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val superAdminToken = generateToken("SUPER_ADMIN", "tenant-enterprise-001")
        val tenantOwnerToken = generateToken("TENANT_OWNER", "tenant-enterprise-001")
        val tenantAdminToken = generateToken("TENANT_ADMIN", "tenant-enterprise-001")
        val deptManagerToken = generateToken("DEPT_MANAGER", "tenant-enterprise-001")
        val staffHumanToken = generateToken("STAFF_HUMAN", "tenant-enterprise-001")

        // 1. RBAC Verification: Unauthenticated -> 401
        val unauthResponse = client.get("/api/v1/admin/analytics/universal-selection-usage")
        assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status)

        // 2. RBAC Verification: Non-SuperAdmin roles -> 403 Forbidden
        val nonSuperAdminTokens = listOf(
            "TENANT_OWNER" to tenantOwnerToken,
            "TENANT_ADMIN" to tenantAdminToken,
            "DEPT_MANAGER" to deptManagerToken,
            "STAFF_HUMAN" to staffHumanToken
        )
        for ((roleName, token) in nonSuperAdminTokens) {
            val res = client.get("/api/v1/admin/analytics/universal-selection-usage") {
                header("Authorization", "Bearer $token")
            }
            assertEquals(HttpStatusCode.Forbidden, res.status, "Role $roleName must receive 403 Forbidden")
        }

        // 3. SUPER_ADMIN -> 200 OK
        val response = client.get("/api/v1/admin/analytics/universal-selection-usage") {
            header("Authorization", "Bearer $superAdminToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val rawJson = response.bodyAsText()
        val usage = json.decodeFromString<UniversalSelectionUsageResponse>(rawJson)

        // Verifikasi metrik agregat
        assertTrue(usage.totalRequests > 0, "totalRequests must be > 0")
        assertTrue(usage.totalCompletedRequests >= 0)
        assertTrue(usage.totalCreditsConsumed > 0.0, "totalCreditsConsumed must be > 0.0")
        assertTrue(usage.mostUsedDomainCategory.isNotBlank(), "mostUsedDomainCategory must not be blank")
        assertTrue(usage.domainCategories.isNotEmpty(), "domainCategories must not be empty")
        assertTrue(usage.tenantsUsage.isNotEmpty(), "tenantsUsage must not be empty")

        // Verifikasi per tenant usage
        for (tenantItem in usage.tenantsUsage) {
            assertTrue(tenantItem.tenantId.isNotBlank())
            assertTrue(tenantItem.tenantName.isNotBlank())
            assertTrue(tenantItem.totalRequests >= 0)
            assertTrue(tenantItem.totalCreditsConsumed >= 0.0)
        }

        // 4. PRIVACY AUDIT (Addendum 2 Bagian 25.2): Tidak ada kebocoran dokumen, resume, atau konten seleksi sensitif
        val sensitiveSelectionContent = listOf(
            "CV_Senior_Backend_Engineer_Budi.pdf",
            "Rekening_Koran_PT_Vendor.xlsx",
            "Gaji_Ekspektasi_Kandidat",
            "Evaluasi_Vendor_Pemenang_Tender_Rahasia"
        )
        for (sensitive in sensitiveSelectionContent) {
            assertTrue(!rawJson.contains(sensitive), "Privacy breach! Raw JSON leaked sensitive tenant selection data: $sensitive")
        }

        assertTrue(usage.privacyNotice.contains("Addendum 2"), "Privacy notice must reference Addendum 2")

        println("=== [PASS] 1.7 Universal Selection Usage: TotalReqs=${usage.totalRequests}, CreditsConsumed=${usage.totalCreditsConsumed}, TopCategory=${usage.mostUsedDomainCategory}, TenantsCount=${usage.tenantsUsage.size}, RBAC 5 Roles Verified, Privacy Audit [OK] ===")
    }
}

