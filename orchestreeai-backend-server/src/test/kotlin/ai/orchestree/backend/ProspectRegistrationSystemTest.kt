package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import ai.orchestree.backend.prospect.ProspectRegistrationRequest
import ai.orchestree.backend.prospect.SelectTrialRequest
import ai.orchestree.backend.prospect.ScheduleMeetingRequest
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.sql.DriverManager
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProspectRegistrationSystemTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val json = Json { ignoreUnknownKeys = true }

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
        return ai.orchestree.backend.config.EnvLoader.get("DATABASE_POOL_URL").ifBlank {
            ai.orchestree.backend.config.EnvLoader.get("DATABASE_DIRECT_URL").ifBlank {
                ai.orchestree.backend.config.EnvLoader.get("DATABASE_URL")
            }
        }
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

    private fun generateToken(userId: String, role: String, tenantId: String = "tenant-orchestreeai"): String {
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
    fun test01_verifyMigrationFileAndDatabaseSchema() {
        val v116File = File("db/migrations/V116__prospect_registrations.sql")
        assertTrue(v116File.exists(), "Migration V116 must exist in db/migrations")
        val sql = v116File.readText()
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS prospect_registrations"))
        assertTrue(sql.contains("ENABLE ROW LEVEL SECURITY"))

        createLiveConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val inspectRs = stmt.executeQuery("""
                    SELECT table_name, column_name, data_type, character_maximum_length, udt_name 
                    FROM information_schema.columns 
                    WHERE table_name IN ('tenants', 'commercial_plans', 'industry_catalog') AND column_name = 'id'
                """.trimIndent())
                while (inspectRs.next()) {
                    println("LIVE SCHEMA: ${inspectRs.getString("table_name")}.id -> data_type=${inspectRs.getString("data_type")}, max_length=${inspectRs.getString("character_maximum_length")}, udt=${inspectRs.getString("udt_name")}")
                }
                stmt.execute("DROP TABLE IF EXISTS prospect_registrations CASCADE;")
                // Ensure migration applied
                stmt.execute(sql)

                // Query information_schema
                val rs = stmt.executeQuery("""
                    SELECT column_name, data_type 
                    FROM information_schema.columns 
                    WHERE table_name = 'prospect_registrations'
                """.trimIndent())

                val columns = mutableMapOf<String, String>()
                while (rs.next()) {
                    columns[rs.getString("column_name")] = rs.getString("data_type")
                }

                println("=== PROSPECT REGISTRATIONS SCHEMA COLUMNS ===")
                columns.forEach { (k, v) -> println("COLUMN: $k -> $v") }

                assertTrue(columns.containsKey("id"))
                assertTrue(columns.containsKey("full_name"))
                assertTrue(columns.containsKey("email"))
                assertTrue(columns.containsKey("phone_number"))
                assertTrue(columns.containsKey("company_name"))
                assertTrue(columns.containsKey("job_title"))
                assertTrue(columns.containsKey("interest_option"))
                assertTrue(columns.containsKey("trial_selection_status"))
                assertTrue(columns.containsKey("meeting_status"))
            }
        }
        println("TEST 01 PASSED: Database schema and table verified.")
    }

    @Test
    fun test02_publicProspectRegistrationEndpointsAndValidation() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val testEmail = "lead_${UUID.randomUUID().toString().substring(0, 8)}@corp-nusantara.id"
        val validReq = ProspectRegistrationRequest(
            fullName = "Budi Hartono",
            email = testEmail,
            phoneNumber = "+6281234567890",
            whatsappNumber = "+6281234567890",
            address = "Jl. Sudirman Kav 21, Jakarta",
            companyName = "PT Nusantara Digital Logistik",
            jobTitle = "Chief Technology Officer",
            companySizeRange = "51-200",
            interestOption = "direct_trial_or_subscription"
        )

        // 1. Valid Submission
        val res = client.post("/api/v1/public/prospect-registration") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ProspectRegistrationRequest.serializer(), validReq))
        }
        assertEquals(HttpStatusCode.Created, res.status)
        val bodyText = res.bodyAsText()
        println("RESPONSE 201: $bodyText")
        assertTrue(bodyText.contains("Terima kasih! Tim kami akan menghubungi Anda segera"))
        assertTrue(bodyText.contains("36 slot Trial 7 Hari"))

        // 2. Validation: Invalid Email
        val invalidEmailRes = client.post("/api/v1/public/prospect-registration") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ProspectRegistrationRequest.serializer(), validReq.copy(email = "bukan_email")))
        }
        assertEquals(HttpStatusCode.BadRequest, invalidEmailRes.status)
        assertTrue(invalidEmailRes.bodyAsText().contains("Format alamat email tidak valid"))

        // 3. Validation: Invalid Phone
        val invalidPhoneRes = client.post("/api/v1/public/prospect-registration") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ProspectRegistrationRequest.serializer(), validReq.copy(phoneNumber = "123")))
        }
        assertEquals(HttpStatusCode.BadRequest, invalidPhoneRes.status)
        assertTrue(invalidPhoneRes.bodyAsText().contains("nomor telepon"))

        // 4. Validation: Invalid Interest Option
        val invalidOptionRes = client.post("/api/v1/public/prospect-registration") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ProspectRegistrationRequest.serializer(), validReq.copy(interestOption = "invalid_option")))
        }
        assertEquals(HttpStatusCode.BadRequest, invalidOptionRes.status)
        assertTrue(invalidOptionRes.bodyAsText().contains("Opsi pilihan tidak valid"))

        println("TEST 02 PASSED: Public registration and validation passed.")
    }

    @Test
    fun test03_rbacMatrixAndAdminLifecycleOperations() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        // 1. Insert a test prospect to operate on
        val testEmail = "admin_test_${UUID.randomUUID().toString().substring(0, 8)}@bumn-test.co.id"
        val req = ProspectRegistrationRequest(
            fullName = "Dewi Sartika",
            email = testEmail,
            phoneNumber = "+6281122334455",
            companyName = "PT Energi Mandiri Bersama",
            jobTitle = "VP of People & AI Operations",
            companySizeRange = "201-500",
            interestOption = "schedule_meeting_presentation"
        )
        val createRes = client.post("/api/v1/public/prospect-registration") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ProspectRegistrationRequest.serializer(), req))
        }
        assertEquals(HttpStatusCode.Created, createRes.status)
        val createdJson = json.parseToJsonElement(createRes.bodyAsText()).jsonObject
        val prospectId = createdJson["data"]!!.jsonObject["id"]!!.jsonPrimitive.content
        assertNotNull(prospectId)

        // 2. RBAC Matrix Testing for GET /api/v1/admin/prospect-registrations
        // A. Unauthenticated -> 401
        val unauthRes = client.get("/api/v1/admin/prospect-registrations")
        assertEquals(HttpStatusCode.Unauthorized, unauthRes.status)

        // B. Authenticated as STAFF_HUMAN -> 403 Forbidden
        val staffToken = generateToken("user-staff-001", "STAFF_HUMAN")
        val staffRes = client.get("/api/v1/admin/prospect-registrations") {
            header("Authorization", "Bearer $staffToken")
        }
        assertEquals(HttpStatusCode.Forbidden, staffRes.status)

        // C. Authenticated as TENANT_ADMIN -> 403 Forbidden
        val tenantAdminToken = generateToken("user-tadmin-001", "TENANT_ADMIN")
        val tadminRes = client.get("/api/v1/admin/prospect-registrations") {
            header("Authorization", "Bearer $tenantAdminToken")
        }
        assertEquals(HttpStatusCode.Forbidden, tadminRes.status)

        // D. Authenticated as SUPER_ADMIN -> 200 OK
        val superAdminToken = generateToken("00000000-0000-0000-0000-000000000001", "SUPER_ADMIN")
        val adminRes = client.get("/api/v1/admin/prospect-registrations") {
            header("Authorization", "Bearer $superAdminToken")
        }
        assertEquals(HttpStatusCode.OK, adminRes.status)
        assertTrue(adminRes.bodyAsText().contains("PT Energi Mandiri Bersama"))

        // 3. Super Admin Action: Select for Trial (36 slots control)
        val selectTrialRes = client.patch("/api/v1/admin/prospect-registrations/$prospectId/select-trial") {
            header("Authorization", "Bearer $superAdminToken")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SelectTrialRequest.serializer(), SelectTrialRequest(
                status = "selected_for_trial",
                adminNotes = "Memenuhi kriteria enterprise autonomous trial Q3"
            )))
        }
        assertEquals(HttpStatusCode.OK, selectTrialRes.status)
        assertTrue(selectTrialRes.bodyAsText().contains("selected_for_trial"))

        // 4. Super Admin Action: Schedule Meeting Presentation
        val scheduleMeetingRes = client.patch("/api/v1/admin/prospect-registrations/$prospectId/schedule-meeting") {
            header("Authorization", "Bearer $superAdminToken")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ScheduleMeetingRequest.serializer(), ScheduleMeetingRequest(
                meetingScheduledAt = "2026-09-15T10:00:00Z",
                meetingStatus = "scheduled",
                adminNotes = "Presentasi demo arsitektur multi-agent via Google Meet"
            )))
        }
        assertEquals(HttpStatusCode.OK, scheduleMeetingRes.status)
        assertTrue(scheduleMeetingRes.bodyAsText().contains("scheduled"))

        // 5. Super Admin Action: Activate 7-Day Trial Tenant
        val activateTrialRes = client.post("/api/v1/admin/prospect-registrations/$prospectId/activate-trial") {
            header("Authorization", "Bearer $superAdminToken")
        }
        assertEquals(HttpStatusCode.Created, activateTrialRes.status)
        val activateBody = activateTrialRes.bodyAsText()
        println("ACTIVATE TRIAL RESULT: $activateBody")
        assertTrue(activateBody.contains("tenant-trial-"))
        assertTrue(activateBody.contains("initialCredits"))

        // 6. Analytics Endpoint
        val analyticsRes = client.get("/api/v1/admin/prospect-registrations/analytics") {
            header("Authorization", "Bearer $superAdminToken")
        }
        assertEquals(HttpStatusCode.OK, analyticsRes.status)
        val analyticsBody = analyticsRes.bodyAsText()
        println("ANALYTICS RESULT: $analyticsBody")
        assertTrue(analyticsBody.contains("totalRegistered"))
        assertTrue(analyticsBody.contains("maxTrialQuota"))
        assertTrue(analyticsBody.contains("36"))

        // 7. Raw SQL Database Verification (Mandatory Proof)
        createLiveConnection().use { conn ->
            conn.prepareStatement("""
                SELECT id, full_name, email, company_name, trial_selection_status, 
                       meeting_status, activated_tenant_id, admin_notes
                FROM prospect_registrations 
                WHERE id = ?::uuid
            """.trimIndent()).use { ps ->
                ps.setString(1, prospectId)
                val rs = ps.executeQuery()
                assertTrue(rs.next(), "Record must exist in PostgreSQL")
                println("=== RAW SQL QUERY VERIFICATION RESULT ===")
                println("id: ${rs.getString("id")}")
                println("company_name: ${rs.getString("company_name")}")
                println("trial_selection_status: ${rs.getString("trial_selection_status")}")
                println("meeting_status: ${rs.getString("meeting_status")}")
                println("activated_tenant_id: ${rs.getString("activated_tenant_id")}")
                println("admin_notes: ${rs.getString("admin_notes")}")

                assertEquals("trial_activated", rs.getString("trial_selection_status"))
                assertEquals("scheduled", rs.getString("meeting_status"))
                assertNotNull(rs.getString("activated_tenant_id"))
            }
        }
        println("TEST 03 PASSED: Full RBAC matrix, admin lifecycle operations, and raw SQL proof verified.")
    }
}
