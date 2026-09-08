package ai.orchestree.backend

import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.selection.SelectionRepository
import ai.orchestree.backend.database.repositories.selection.SelectionRequestRecord
import ai.orchestree.backend.database.repositories.selection.SelectionSourceDocumentRecord
import ai.orchestree.backend.intelligence.CalibrationItem
import ai.orchestree.backend.intelligence.CalibrationItemRequest
import ai.orchestree.backend.intelligence.CalibrationRequest
import ai.orchestree.backend.intelligence.DetectedField
import ai.orchestree.backend.intelligence.SelectionCalibrationService
import ai.orchestree.backend.intelligence.SelectionEngine
import ai.orchestree.backend.intelligence.SpreadsheetExtractor
import ai.orchestree.backend.modelrouter.ModelRouter
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
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID

class BagianDSelectionCalibrationTest {

    private val supabase = SupabaseClientProvider.fromEnv()
    private val selectionRepo = SelectionRepository(supabase)
    private val modelRouter = ModelRouter()
    private val calibrationService = SelectionCalibrationService(selectionRepo, modelRouter)
    private val selectionEngine = SelectionEngine(supabase = supabase, modelRouter = modelRouter, selectionRepo = selectionRepo)
    private val json = Json { ignoreUnknownKeys = true }

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
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(algorithm)
    }

    @Test
    fun testValidateAndSaveCalibration_Exact100Percent() = runBlocking {
        println("\n=== SUB-TEST 1: KALIBRASI EXACT 100% PERSISTENCE ===")
        val tenantId = "tenant-test-d-001"
        val userId = "user-hr-01"

        val request = CalibrationRequest(
            calibration_name = "Kalibrasi Rekrutmen Standar",
            items = listOf(
                CalibrationItemRequest("Pengalaman Kerja", 40.0),
                CalibrationItemRequest("Skill Teknis", 35.0),
                CalibrationItemRequest("Pendidikan", 25.0)
            )
        )

        val result = calibrationService.validateAndSaveCalibration(tenantId, userId, request)

        assertNotNull(result.calibrationId)
        assertEquals("Kalibrasi Rekrutmen Standar", result.calibrationName)
        assertFalse(result.wasNormalized)
        assertEquals(100.0, result.originalTotal, 0.001)
        assertEquals(3, result.normalizedItems.size)

        val sum = result.normalizedItems.sumOf { it.percentage }
        assertEquals(100.0, sum, 0.001)

        // Verifikasi tersimpan di repository
        val savedSetting = selectionRepo.getCalibrationSetting(result.calibrationId, tenantId)
        assertNotNull(savedSetting)
        assertEquals("Kalibrasi Rekrutmen Standar", savedSetting?.calibration_name)

        val savedItems = selectionRepo.getCalibrationItems(result.calibrationId, tenantId)
        assertEquals(3, savedItems.size)
        assertEquals("Pengalaman Kerja", savedItems[0].field_type_name)
        assertEquals(40.0, savedItems[0].percentage, 0.001)
        println("[OK] Sub-Test 1 Berhasil: Kalibrasi pas 100% disimpan tanpa normalisasi.")
    }

    @Test
    fun testValidateAndSaveCalibration_AutoNormalizeNon100Percent() = runBlocking {
        println("\n=== SUB-TEST 2: AUTO-NORMALISASI PROPORSIONAL (TOTAL != 100%) ===")
        val tenantId = "tenant-test-d-002"
        val userId = "user-hr-02"

        // Input 50 + 50 + 50 = 150% (tidak 100%)
        val request = CalibrationRequest(
            calibration_name = "Kalibrasi Bobot Lebih",
            items = listOf(
                CalibrationItemRequest("Kriteria A", 50.0),
                CalibrationItemRequest("Kriteria B", 50.0),
                CalibrationItemRequest("Kriteria C", 50.0)
            )
        )

        val result = calibrationService.validateAndSaveCalibration(tenantId, userId, request)

        assertTrue(result.wasNormalized)
        assertEquals(150.0, result.originalTotal, 0.001)

        // Auto-normalisasi proporsional ke 100%
        val totalNormalized = result.normalizedItems.sumOf { it.percentage }
        assertEquals(100.0, totalNormalized, 0.01)

        // Verifikasi pesan transparan ke user
        assertTrue(result.message?.contains("150") == true)
        assertTrue(result.message?.contains("auto-normalisasi") == true)
        println("[OK] Sub-Test 2 Berhasil: Total 150% sukses dinormalisasi proporsional ke 100%.")
    }

    @Test
    fun testFuzzyMatchingAndDatasetValidation() = runBlocking {
        println("\n=== SUB-TEST 3: FUZZY MATCHING & DATASET FIELD VALIDATION ===")
        val detectedFields = listOf(
            DetectedField(name = "nama_lengkap", inferred_type = "string", sample_values = listOf("Budi", "Siti")),
            DetectedField(name = "pengalaman_tahun", inferred_type = "number", sample_values = listOf("5", "3")),
            DetectedField(name = "keahlian_teknis", inferred_type = "string", sample_values = listOf("Kotlin, Android")),
            DetectedField(name = "pendidikan_terakhir", inferred_type = "string", sample_values = listOf("S1 Informatika"))
        )

        // Kasus 1: Seluruh kriteria cocok fuzzy
        val validCalibration = listOf(
            CalibrationItem("Pengalaman Kerja", 40.0),
            CalibrationItem("Skill Teknis", 35.0),
            CalibrationItem("Pendidikan Formal", 25.0)
        )

        val validationSuccess = calibrationService.validateCalibrationAgainstDataset(validCalibration, detectedFields)
        assertTrue(validationSuccess.isSuccess)
        assertEquals(0, validationSuccess.unmappedCriteria.size)
        assertEquals("pengalaman_tahun", validationSuccess.mappedCriteria["Pengalaman Kerja"])
        assertEquals("keahlian_teknis", validationSuccess.mappedCriteria["Skill Teknis"])
        assertEquals("pendidikan_terakhir", validationSuccess.mappedCriteria["Pendidikan Formal"])

        // Kasus 2: Ada kriteria yang TIDAK ADA di dataset (misal Kesesuaian Budget Gaji)
        val invalidCalibration = listOf(
            CalibrationItem("Pengalaman Kerja", 40.0),
            CalibrationItem("Kesesuaian Budget Gaji", 35.0), // Tidak ada di dataset!
            CalibrationItem("Pendidikan Formal", 25.0)
        )

        val validationFail = calibrationService.validateCalibrationAgainstDataset(invalidCalibration, detectedFields)
        assertFalse(validationFail.isSuccess)
        assertEquals(1, validationFail.unmappedCriteria.size)
        assertEquals("Kesesuaian Budget Gaji", validationFail.unmappedCriteria[0])
        assertNotNull(validationFail.warningMessage)
        assertTrue(validationFail.warningMessage?.contains("Kesesuaian Budget Gaji") == true)
        assertTrue(validationFail.warningMessage?.contains("tidak ditemukan") == true)
        println("[OK] Sub-Test 3 Berhasil: Fuzzy matching mendeteksi field cocok & menolak field yang tidak ada dengan pesan jelas.")
    }

    @Test
    fun testDetermineAiDefaultWeighting_WhenNoCalibrationProvided() = runBlocking {
        println("\n=== SUB-TEST 4: AI DEFAULT WEIGHTING (EXPLAINABLE & RECORDED) ===")
        val detectedFields = listOf(
            DetectedField(name = "nama_kandidat", inferred_type = "string"),
            DetectedField(name = "pengalaman_kerja_tahun", inferred_type = "number"),
            DetectedField(name = "tingkat_pendidikan", inferred_type = "string"),
            DetectedField(name = "ekspektasi_gaji", inferred_type = "number")
        )

        val aiCriteria = calibrationService.determineAiDefaultWeighting(
            promptText = "Pilih kandidat Software Engineer terbaik untuk tim mobile",
            detectedFields = detectedFields,
            agentSkillContext = "agent-hr-recruiter"
        )

        assertTrue(aiCriteria.isNotEmpty())
        val totalWeight = aiCriteria.sumOf { it.weightPercentage }
        assertEquals(100.0, totalWeight, 0.01)

        // Harus tercatat sebagai ai_generated dengan rationale transparan
        for (criterion in aiCriteria) {
            assertEquals("ai_generated", criterion.source)
            assertNotNull(criterion.rationale)
            assertTrue(criterion.rationale!!.isNotBlank())
        }
        println("[OK] Sub-Test 4 Berhasil: AI default weighting menghasilkan kriteria berbobot total 100% dan alasan transparan.")
    }

    @Test
    fun testKtorCalibrationApiEndpoints_AndRoleRbac() = testApplication {
        application {
            module()
        }

        println("\n=== SUB-TEST 5: KTOR REST API & RBAC ROLE MATRIX TESTING ===")
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val tenantId = "tenant-enterprise-001"
        val staffToken = generateToken(tenantId, "staff-01", "STAFF_HUMAN")
        val adminToken = generateToken(tenantId, "admin-01", "TENANT_ADMIN")
        val ownerToken = generateToken(tenantId, "owner-01", "TENANT_OWNER")

        // 1. RBAC Test: STAFF_HUMAN Ditolak (403 Forbidden)
        val staffResponse = client.post("/api/v1/selection/calibration") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $staffToken")
            header("X-Tenant-Id", tenantId)
            setBody("""
                {
                    "calibration_name": "Unauthorized Staff Calib",
                    "items": [{"field_type_name": "Test", "percentage": 100.0}]
                }
            """.trimIndent())
        }
        println("[RBAC] STAFF_HUMAN post calibration: ${staffResponse.status}")
        assertEquals(HttpStatusCode.Forbidden, staffResponse.status)

        // 2. TENANT_ADMIN Berhasil (201 Created)
        val adminResponse = client.post("/api/v1/selection/calibration") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $adminToken")
            header("X-Tenant-Id", tenantId)
            setBody("""
                {
                    "calibration_name": "Kalibrasi Admin Q1",
                    "items": [
                        {"field_type_name": "Pengalaman", "percentage": 60.0},
                        {"field_type_name": "Keahlian", "percentage": 40.0}
                    ]
                }
            """.trimIndent())
        }
        println("[RBAC] TENANT_ADMIN post calibration: ${adminResponse.status}")
        assertEquals(HttpStatusCode.Created, adminResponse.status)
        val createdBody = adminResponse.bodyAsText()
        val createdJson = json.parseToJsonElement(createdBody).jsonObject
        val calibId = createdJson["calibrationId"]?.jsonPrimitive?.content
        assertNotNull(calibId)

        // 3. GET Calibration List
        val listResponse = client.get("/api/v1/selection/calibration") {
            header("Authorization", "Bearer $adminToken")
            header("X-Tenant-Id", tenantId)
        }
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val listJson = json.parseToJsonElement(listResponse.bodyAsText()).jsonArray
        assertTrue(listJson.any { it.jsonObject["id"]?.jsonPrimitive?.content == calibId })

        // 4. GET Calibration Detail
        val detailResponse = client.get("/api/v1/selection/calibration/$calibId") {
            header("Authorization", "Bearer $ownerToken")
            header("X-Tenant-Id", tenantId)
        }
        assertEquals(HttpStatusCode.OK, detailResponse.status)
        val detailJson = json.parseToJsonElement(detailResponse.bodyAsText()).jsonObject
        assertNotNull(detailJson["setting"])
        assertNotNull(detailJson["items"])
        println("[OK] Sub-Test 5 Berhasil: REST API calibration & RBAC verified!")
    }
}
