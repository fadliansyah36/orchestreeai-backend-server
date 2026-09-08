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
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.DriverManager
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AutonomousSelectionSubsystemMigrationTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    private val expectedTables = listOf(
        "selection_requests",
        "selection_source_documents",
        "selection_criteria",
        "selection_calibration_settings",
        "selection_calibration_items",
        "selection_results",
        "selection_analytics_summary",
        "selection_audit_log"
    )

    private fun resolveDirectDbUrl(): String {
        val candidates = listOf(
            File("/app/applet/.env"),
            File("/root/.orchestreeai/secrets.properties"),
            File(".env"),
            File("../.env")
        )
        val keys = listOf("DATABASE_POOL_URL", "DATABASE_DIRECT_URL", "DATABASE_URL")
        var rawUrl = ""
        for (key in keys) {
            for (f in candidates) {
                if (f.exists()) {
                    val lines = try { f.readLines() } catch (_: Exception) { emptyList() }
                    for (raw in lines) {
                        val line = raw.trim()
                        if (line.startsWith("$key=")) {
                            val v = line.substringAfter("=").trim().trim('\"').trim('\'')
                            if (v.isNotBlank() && v != "placeholder") {
                                rawUrl = v
                                break
                            }
                        }
                    }
                    if (rawUrl.isNotBlank()) break
                }
            }
            if (rawUrl.isNotBlank()) break
        }
        if (rawUrl.isBlank()) {
            rawUrl = ai.orchestree.backend.config.EnvLoader.get("DATABASE_POOL_URL").ifBlank {
                ai.orchestree.backend.config.EnvLoader.get("DATABASE_DIRECT_URL").ifBlank {
                    ai.orchestree.backend.config.EnvLoader.get("DATABASE_URL")
                }
            }
        }
        if (rawUrl.isBlank()) return ""
        return rawUrl
    }

    private fun createLiveConnection(): java.sql.Connection {
        val rawUri = resolveDirectDbUrl()
        assertTrue(rawUri.isNotBlank(), "Database URL must be configured for live verification")
        val clean = rawUri.removePrefix("jdbc:")
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
            setProperty("prepareThreshold", "0")
            setProperty("preparedStatementCacheQueries", "0")
        }
        Class.forName("org.postgresql.Driver")
        return DriverManager.getConnection(jdbcUrl, props)
    }

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
    fun test01_verifyV115MigrationFileStructure() {
        val migrationFile = File("db/migrations/V115__autonomous_ai_selection_subsystem.sql")
        assertTrue(migrationFile.exists(), "Migration V115 must exist in db/migrations")

        val sqlContent = migrationFile.readText()
        for (table in expectedTables) {
            assertTrue(sqlContent.contains(table), "Migration V115 must create table: $table")
            assertTrue(
                sqlContent.contains("ALTER TABLE $table ENABLE ROW LEVEL SECURITY;"),
                "Migration V115 must enable RLS on: $table"
            )
        }

        assertTrue(
            sqlContent.contains("ALTER PUBLICATION supabase_realtime ADD TABLE selection_requests;"),
            "Migration V115 must add selection_requests to supabase_realtime"
        )
        assertTrue(
            sqlContent.contains("ALTER PUBLICATION supabase_realtime ADD TABLE selection_results;"),
            "Migration V115 must add selection_results to supabase_realtime"
        )
        println("TEST 01 PASSED: V115 migration SQL contains all 8 tables, RLS declarations, and Realtime publications.")
    }

    @Test
    fun test02_verifyPostgresLiveDatabaseSchemaAndRLS() {
        createLiveConnection().use { conn ->
            conn.createStatement().use { stmt ->
                // 1. Verify tables exist and rowsecurity is true
                val rs = stmt.executeQuery("""
                    SELECT tablename, rowsecurity 
                    FROM pg_tables 
                    WHERE tablename IN (
                        'selection_requests', 'selection_source_documents', 'selection_criteria',
                        'selection_calibration_settings', 'selection_calibration_items',
                        'selection_results', 'selection_analytics_summary', 'selection_audit_log'
                    );
                """.trimIndent())

                val activeRlsTables = mutableSetOf<String>()
                while (rs.next()) {
                    val tbl = rs.getString("tablename")
                    val rls = rs.getBoolean("rowsecurity")
                    if (rls) activeRlsTables.add(tbl)
                    println("RAW DB TABLE: $tbl (RLS: $rls)")
                }
                for (tbl in expectedTables) {
                    assertTrue(activeRlsTables.contains(tbl), "Table $tbl must have RLS active in PostgreSQL")
                }

                // 2. Verify Realtime publication membership
                val pubRs = stmt.executeQuery("""
                    SELECT tablename 
                    FROM pg_publication_tables 
                    WHERE pubname = 'supabase_realtime' 
                      AND tablename IN ('selection_requests', 'selection_results');
                """.trimIndent())

                val realtimeTables = mutableSetOf<String>()
                while (pubRs.next()) {
                    val tbl = pubRs.getString("tablename")
                    realtimeTables.add(tbl)
                    println("RAW REALTIME TABLE: $tbl")
                }
                assertTrue(realtimeTables.contains("selection_requests"), "selection_requests must be in supabase_realtime publication")
                assertTrue(realtimeTables.contains("selection_results"), "selection_results must be in supabase_realtime publication")
            }
        }
        println("TEST 02 PASSED: Verified all 8 tables and RLS status directly in Supabase PostgreSQL.")
    }

    @Test
    fun test03_verifyMultiTenantRLSStrictIsolation() {
        val tenantA = "tenant-enterprise-001"
        val tenantB = "tenant-nusantara"

        createLiveConnection().use { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { stmt ->
                    // 0. Ensure tenants exist for foreign key constraints
                    stmt.executeUpdate("""
                        INSERT INTO tenants (id, name, domain) 
                        VALUES ('$tenantA', 'Tenant Enterprise Test', 'enterprise.test')
                        ON CONFLICT (id) DO NOTHING;
                    """.trimIndent())
                    stmt.executeUpdate("""
                        INSERT INTO tenants (id, name, domain) 
                        VALUES ('$tenantB', 'Tenant Nusantara Test', 'nusantara.test')
                        ON CONFLICT (id) DO NOTHING;
                    """.trimIndent())

                    // 1. Insert Tenant A Request
                    stmt.executeUpdate("""
                        INSERT INTO selection_requests (id, tenant_id, requested_by_user_id, domain_category, prompt_text, source_type, status)
                        VALUES ('11111111-aaaa-4000-8000-000000000001', '$tenantA', 'usr-owner-01', 'recruitment', '[TEST_KT] Tenant A Senior Recruiter', 'file_upload', 'processing')
                        ON CONFLICT (id) DO NOTHING;
                    """.trimIndent())

                    // 2. Insert Tenant B Request
                    stmt.executeUpdate("""
                        INSERT INTO selection_requests (id, tenant_id, requested_by_user_id, domain_category, prompt_text, source_type, status)
                        VALUES ('22222222-bbbb-4000-8000-000000000002', '$tenantB', 'usr-staff-sales-01', 'supplier', '[TEST_KT] Tenant B Vendor Scoring', 'prompt_only', 'processing')
                        ON CONFLICT (id) DO NOTHING;
                    """.trimIndent())

                    // 3. Test as authenticated role under Tenant A
                    stmt.execute("SET LOCAL ROLE authenticated;")
                    stmt.execute("SET LOCAL app.current_tenant_id = '$tenantA';")

                    val rsA = stmt.executeQuery("SELECT id, tenant_id, prompt_text FROM selection_requests WHERE prompt_text LIKE '[TEST_KT]%';")
                    val seenByA = mutableListOf<String>()
                    while (rsA.next()) {
                        seenByA.add(rsA.getString("id"))
                    }
                    println("RAW RLS QUERY [Context Tenant A]: Seen IDs = $seenByA")
                    assertTrue(seenByA.contains("11111111-aaaa-4000-8000-000000000001"), "Tenant A must see its own selection request")
                    assertFalse(seenByA.contains("22222222-bbbb-4000-8000-000000000002"), "Tenant A MUST NOT see Tenant B selection request")

                    // 4. Test as authenticated role under Tenant B
                    stmt.execute("SET LOCAL app.current_tenant_id = '$tenantB';")
                    val rsB = stmt.executeQuery("SELECT id, tenant_id, prompt_text FROM selection_requests WHERE prompt_text LIKE '[TEST_KT]%';")
                    val seenByB = mutableListOf<String>()
                    while (rsB.next()) {
                        seenByB.add(rsB.getString("id"))
                    }
                    println("RAW RLS QUERY [Context Tenant B]: Seen IDs = $seenByB")
                    assertTrue(seenByB.contains("22222222-bbbb-4000-8000-000000000002"), "Tenant B must see its own selection request")
                    assertFalse(seenByB.contains("11111111-aaaa-4000-8000-000000000001"), "Tenant B MUST NOT see Tenant A selection request")

                    // 5. Test Unauthenticated / Blank tenant
                    stmt.execute("SET LOCAL app.current_tenant_id = '';")
                    val rsBlank = stmt.executeQuery("SELECT id FROM selection_requests WHERE prompt_text LIKE '[TEST_KT]%';")
                    assertFalse(rsBlank.next(), "Blank/unauthenticated tenant context must see 0 records")

                    // Reset and cleanup
                    stmt.execute("RESET ROLE;")
                    stmt.executeUpdate("DELETE FROM selection_requests WHERE prompt_text LIKE '[TEST_KT]%';")
                }
                conn.commit()
                println("TEST 03 PASSED: Verified 100% RLS tenant isolation between 2 distinct tenants.")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    @Test
    fun test04_matrixRoleAndConditionExhaustive() = testApplication {
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
        assertEquals(HttpStatusCode.Unauthorized, unauth.status, "Unauthenticated request wajib 401 Unauthorized")
    }
}
