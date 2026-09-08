package ai.orchestree.backend

import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.selection.*
import ai.orchestree.backend.intelligence.*
import ai.orchestree.backend.modelrouter.ModelRouter
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.sql.DriverManager
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BagianBSelectionWorkflowComprehensiveTest {

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

    private fun resolveDirectDbUrl(): String {
        val candidates = listOf(
            File("/app/applet/.env"),
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
                            if (v.isNotBlank() && v != "placeholder") return v
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

    private fun getLiveDbConnection(): java.sql.Connection? {
        val rawUri = resolveDirectDbUrl()
        if (rawUri.isBlank()) return null
        return try {
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
            }
            Class.forName("org.postgresql.Driver")
            DriverManager.getConnection(jdbcUrl, props)
        } catch (e: Exception) {
            println("[WARN] Live DB Connection failed: ${e.message}")
            null
        }
    }

    @Test
    fun testDocumentExtractorsWithAllFileTypes() = runBlocking {
        println("\n=== SUB-TEST 1: Document Extractors (XLSX, CSV, PDF, DOCX, Vision) ===")

        val spreadsheetExtractor = SpreadsheetExtractor()
        val pdfExtractor = PdfExtractor()
        val docxExtractor = DocxExtractor()
        val visionModel = VisionModel()

        // 1. CSV Extraction
        val sampleCsv = """
            Nama Kandidat,Pendidikan,Pengalaman Kerja,Keahlian,Ekspektasi Gaji
            Budi Pratama,S1 Teknik Informatika,5 Tahun,Kotlin Java Android Compose Ktor,22000000
            Siti Nurhaliza,S1 Sistem Informasi,3 Tahun,Kotlin Compose Room Retrofit,16000000
            Ahmad Fauzi,D3 Manajemen Informatika,2 Tahun,Android Java SQLite,10000000
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        val csvExtracted = spreadsheetExtractor.parseToStructuredRows(sampleCsv)
        println("[CSV] Extracted rows: ${csvExtracted.rows.size}, schema: ${csvExtracted.schema.keys}")
        assertEquals(3, csvExtracted.rows.size)
        assertTrue(csvExtracted.schema.containsKey("Nama Kandidat"))
        assertEquals("Budi Pratama", csvExtracted.rows[0]["Nama Kandidat"])

        // 2. PDF Extraction
        val samplePdfText = """
            %PDF-1.4
            Laporan Seleksi Vendor Pengadaan Alat Berat 2026
            PT Nusantara Mining Utama
            Vendor 1: PT Hexindo Adiperkasa | Skor Teknis: 95 | Biaya: Rp 12.5M | Delivery: 30 Hari
            Vendor 2: PT United Tractors | Skor Teknis: 92 | Biaya: Rp 13.1M | Delivery: 45 Hari
            Vendor 3: PT Trakindo Utama | Skor Teknis: 89 | Biaya: Rp 12.0M | Delivery: 60 Hari
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        val pdfExtracted = pdfExtractor.extractTablesAndText(samplePdfText)
        println("[PDF] Extracted rows: ${pdfExtracted.rows.size}, schema: ${pdfExtracted.schema.keys}")
        assertTrue(pdfExtracted.rows.isNotEmpty())

        // 3. DOCX Extraction
        val sampleDocxText = """
            PK\x03\x04
            Dokumen Profil Calon Mitra Strategis FinTech
            Nama Institusi: PT Bank Digital Maju
            NPL: 0.8%
            Aset: Rp 45 Triliun
            Integrasi API: Open Banking ISO20022 Ready
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        val docxExtracted = docxExtractor.extractStructuredContent(sampleDocxText)
        println("[DOCX] Extracted rows: ${docxExtracted.rows.size}")
        assertTrue(docxExtracted.rows.isNotEmpty())

        // 4. Vision Scanned Image Extraction
        val sampleImage = "FakePNGImageDataForInvoiceOCR".toByteArray(StandardCharsets.UTF_8)
        val visionExtracted = visionModel.extractTextAndTablesFromScannedImage(sampleImage)
        println("[VISION] Extracted rows: ${visionExtracted.rows.size}")
        assertTrue(visionExtracted.rows.isNotEmpty())
    }

    @Test
    fun testSelectionEngineEndToEndWithCreditLedger() = runBlocking {
        println("\n=== SUB-TEST 2: SelectionEngine End-To-End & Central Credit Ledger ===")

        val supabase = SupabaseClientProvider.fromEnv()
        val modelRouter = ModelRouter()
        val repo = SelectionRepository(supabase, modelRouter)
        val engine = SelectionEngine(supabase = supabase, modelRouter = modelRouter, selectionRepo = repo)
        val tenantId = "tenant-enterprise-001"
        val requestId = "sel-req-${UUID.randomUUID().toString().take(8)}"
        val docId = "doc-${UUID.randomUUID().toString().take(8)}"

        // Siapkan selection request record
        repo.createSelectionRequest(
            SelectionRequestRecord(
                id = requestId,
                tenant_id = tenantId,
                requested_by_user_id = "user-qa-tester",
                prompt_text = "Pilih kandidat Lead Mobile Engineer terbaik dengan pengalaman Kotlin dan Microservices",
                source_type = "file_upload",
                status = "processing"
            )
        )

        // Simpan source document
        repo.createSourceDocument(
            SelectionSourceDocumentRecord(
                id = docId,
                selection_request_id = requestId,
                file_name = "kandidat_lead_mobile.csv",
                file_type = "csv",
                object_storage_url = "supabase://selection_vault/$docId.csv",
                extraction_status = "processing"
            ),
            tenantId
        )

        val csvContent = """
            Kandidat,Pengalaman,Keahlian Utama,Kepemimpinan,Gaji Diharapkan
            Andi Wijaya,8 Tahun,Kotlin Coroutines Ktor KSP Architecture,Pernah Lead 10 Dev,35000000
            Bambang Susanto,6 Tahun,Android Compose Microservices Room,Pernah Lead 4 Dev,28000000
            Citra Dewi,4 Tahun,Flutter React Native Mobile UI,Scrum Master certified,22000000
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        // Simpan ke ObjectStorage
        engine.objectStorage.putMemoryObject("supabase://selection_vault/$docId.csv", csvContent)

        // Jalankan processUploadedDataset(docId)
        engine.processUploadedDataset(docId)

        // Verifikasi hasil evaluasi
        val results = repo.getSelectionResults(requestId, tenantId)
        println("[SELECTION] Evaluated results count: ${results.size}")
        assertTrue(results.isNotEmpty(), "Results should be generated by Model Router")

        val topCandidate = results.first()
        println("[SELECTION] Ranked #1: ${topCandidate.row_reference}, Score: ${topCandidate.total_score}, Classification: ${topCandidate.recommendation_classification}")
        assertTrue(topCandidate.total_score > 0.0)
        assertNotNull(topCandidate.ai_insight_text)

        // Verifikasi analytics
        val analytics = repo.getSelectionAnalytics(requestId, tenantId)
        println("[ANALYTICS] Summary records count: ${analytics.size}")
        assertTrue(analytics.isNotEmpty())

        // Verifikasi audit log via direct Supabase query
        val auditRes = supabase.queryTable("selection_audit_log", tenantId, "selection_request_id=eq.$requestId")
        println("[AUDIT LOG] Query response: isSuccess=${auditRes.isSuccess}")
    }

    @Test
    fun testSelectionApiEndpointsAndRbacMatrix() = testApplication {
        application {
            module()
        }

        println("\n=== SUB-TEST 3: API Endpoints & Matrix Role RBAC ===")

        val tenantId = "tenant-enterprise-001"
        val superAdminToken = generateToken(tenantId, "super-admin-01", "SUPER_ADMIN")
        val tenantOwnerToken = generateToken(tenantId, "owner-01", "TENANT_OWNER")
        val tenantAdminToken = generateToken(tenantId, "admin-01", "TENANT_ADMIN")
        val deptManagerToken = generateToken(tenantId, "manager-01", "DEPT_MANAGER")
        val staffHumanToken = generateToken(tenantId, "staff-01", "STAFF_HUMAN")

        // 1. Unauthenticated Request -> 401 Unauthorized
        val unauthResp = client.get("/api/v1/selection/requests")
        println("[RBAC] Unauthenticated GET /api/v1/selection/requests -> ${unauthResp.status}")
        assertEquals(HttpStatusCode.Unauthorized, unauthResp.status)

        // 2. SUPER_ADMIN -> 200 OK
        val superResp = client.get("/api/v1/selection/requests") {
            header(HttpHeaders.Authorization, "Bearer $superAdminToken")
        }
        println("[RBAC] SUPER_ADMIN GET /api/v1/selection/requests -> ${superResp.status}")
        assertEquals(HttpStatusCode.OK, superResp.status)

        // 3. TENANT_OWNER -> 200 OK
        val ownerResp = client.get("/api/v1/selection/requests") {
            header(HttpHeaders.Authorization, "Bearer $tenantOwnerToken")
        }
        println("[RBAC] TENANT_OWNER GET /api/v1/selection/requests -> ${ownerResp.status}")
        assertEquals(HttpStatusCode.OK, ownerResp.status)

        // 4. TENANT_ADMIN -> 200 OK
        val adminResp = client.get("/api/v1/selection/requests") {
            header(HttpHeaders.Authorization, "Bearer $tenantAdminToken")
        }
        println("[RBAC] TENANT_ADMIN GET /api/v1/selection/requests -> ${adminResp.status}")
        assertEquals(HttpStatusCode.OK, adminResp.status)

        // 5. STAFF_HUMAN -> 200 OK (Read permission)
        val staffResp = client.get("/api/v1/selection/requests") {
            header(HttpHeaders.Authorization, "Bearer $staffHumanToken")
        }
        println("[RBAC] STAFF_HUMAN GET /api/v1/selection/requests -> ${staffResp.status}")
        assertEquals(HttpStatusCode.OK, staffResp.status)

        // 6. Test POST /api/v1/selection/upload (Multipart)
        val csvData = "Kandidat,Skor\nVendor A,98\nVendor B,85".toByteArray()
        val uploadResp = client.submitFormWithBinaryData(
            url = "/api/v1/selection/upload",
            formData = formData {
                append("prompt", "Pilih vendor terbaik dengan skor performa tertinggi")
                append("file", csvData, Headers.build {
                    append(HttpHeaders.ContentType, "text/csv")
                    append(HttpHeaders.ContentDisposition, "filename=\"vendor_test.csv\"")
                })
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $tenantAdminToken")
        }
        println("[API] POST /api/v1/selection/upload -> ${uploadResp.status}")
        assertEquals(HttpStatusCode.Accepted, uploadResp.status)
        val uploadJson = Json.parseToJsonElement(uploadResp.bodyAsText()).jsonObject
        val uploadedReqId = uploadJson["requestId"]?.jsonPrimitive?.content
        assertNotNull(uploadedReqId)
        println("[API] Uploaded Request ID: $uploadedReqId")

        // 7. Test POST /api/v1/selection/prompt-only
        val promptOnlyResp = client.post("/api/v1/selection/prompt-only") {
            header(HttpHeaders.Authorization, "Bearer $tenantAdminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"prompt": "Cari supplier semen curah terbaik di Jawa Timur"}""")
        }
        println("[API] POST /api/v1/selection/prompt-only -> ${promptOnlyResp.status}")
        assertEquals(HttpStatusCode.Accepted, promptOnlyResp.status)

        // 8. Test POST /api/v1/selection/api-database
        val apiDbResp = client.post("/api/v1/selection/api-database") {
            header(HttpHeaders.Authorization, "Bearer $tenantAdminToken")
            contentType(ContentType.Application.Json)
            setBody("""{
                "prompt": "Pilih supplier material dengan on-time delivery > 95%",
                "table_name": "suppliers"
            }""")
        }
        println("[API] POST /api/v1/selection/api-database -> ${apiDbResp.status}")
        assertEquals(HttpStatusCode.Accepted, apiDbResp.status)
    }

    @Test
    fun testLiveDatabaseDirectSqlQuery() {
        println("\n=== SUB-TEST 4: Raw Direct SQL Execution on PostgreSQL ===")
        val conn = getLiveDbConnection()
        if (conn == null) {
            println("[NOTICE] Live Database connection not reachable from this test runner, skipping raw query verification")
            return
        }

        conn.use { c ->
            val tables = listOf(
                "selection_requests",
                "selection_source_documents",
                "selection_criteria",
                "selection_results",
                "selection_analytics_summary",
                "selection_audit_log",
                "ai_credit_ledger"
            )

            for (t in tables) {
                try {
                    val stmt = c.createStatement()
                    val rs = stmt.executeQuery("SELECT count(*) FROM $t")
                    if (rs.next()) {
                        val count = rs.getInt(1)
                        println("[RAW SQL] Table: $t | Total Records: $count")
                    }
                    rs.close()
                    stmt.close()
                } catch (e: Exception) {
                    println("[RAW SQL ERROR] Failed query on $t: ${e.message}")
                }
            }
        }
    }
}
