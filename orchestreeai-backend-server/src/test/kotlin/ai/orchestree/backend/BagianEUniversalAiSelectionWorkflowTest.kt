package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.selection.SelectionRepository
import ai.orchestree.backend.database.repositories.selection.SelectionRequestRecord
import ai.orchestree.backend.intelligence.DataRow
import ai.orchestree.backend.intelligence.ExtractedDataset
import ai.orchestree.backend.intelligence.SelectionEngine
import ai.orchestree.backend.intelligence.WeightedCriterion
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.WorkflowExecution
import ai.orchestree.backend.orchestration.definitions.WorkflowDefinitions
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID

class BagianEUniversalAiSelectionWorkflowTest {

    private val supabase = SupabaseClientProvider.fromEnv()
    private val modelRouter = ModelRouter()
    private val selectionRepo = SelectionRepository(supabase, modelRouter)
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
    fun test1_WorkflowDefinitionRegistryExact10Nodes() {
        println("\n=== SUB-TEST 1: VERIFIKASI WORKFLOW DEFINITION PERSIS 10 NODE ===")
        val wfDef = WorkflowDefinitions.getById("universal_ai_selection")
        assertNotNull(wfDef, "Workflow universal_ai_selection harus terdaftar di WorkflowDefinitions")
        assertEquals("universal_ai_selection", wfDef!!.id)
        assertEquals(10, wfDef.nodes.size, "Workflow wajib memiliki PERSIS 10 tahapan node")

        val expectedOrder = listOf(
            "n1-read-data" to "READ_DATA",
            "n2-understand" to "UNDERSTAND",
            "n3-validate" to "VALIDATE",
            "n4-select" to "SELECT",
            "n5-score" to "SCORE",
            "n6-rank" to "RANK",
            "n7-analyze" to "ANALYZE",
            "n8-visualize" to "VISUALIZE",
            "n9-recommend" to "RECOMMEND",
            "n10-result" to "RESULT"
        )

        wfDef.nodes.forEachIndexed { index, nodeDef ->
            val expected = expectedOrder[index]
            assertEquals(expected.first, nodeDef.nodeId, "Urutan node #$index harus ${expected.first}")
            assertEquals(expected.second, nodeDef.name, "Nama node #$index harus ${expected.second}")
            println(" [OK] Node ${index + 1}: ${nodeDef.nodeId} (${nodeDef.name}) [${nodeDef.type}]")
        }
    }

    @Test
    fun test2_ScoreAndRankRowAlgorithmAndManualRecalculationProof() = runBlocking {
        println("\n=== SUB-TEST 2: EKSEKUSI ALGORITMA SKOR & BUKTI HASIL IDENTIK HITUNG MANUAL ===")

        val criteria = listOf(
            WeightedCriterion(
                criterionName = "Harga Penawaran",
                weightPercentage = 40.0,
                dataType = "numeric",
                mappedField = "harga_penawaran",
                direction = "lower_is_better"
            ),
            WeightedCriterion(
                criterionName = "Pengalaman Vendor",
                weightPercentage = 30.0,
                dataType = "numeric",
                mappedField = "pengalaman_tahun",
                direction = "higher_is_better"
            ),
            WeightedCriterion(
                criterionName = "Sertifikasi ISO",
                weightPercentage = 30.0,
                dataType = "boolean",
                mappedField = "sertifikasi_iso"
            )
        )

        val rows = listOf(
            DataRow(
                id = "row-vendor-1",
                fields = mapOf(
                    "nama_vendor" to "PT Alpha Mandiri",
                    "harga_penawaran" to "70",
                    "pengalaman_tahun" to "90",
                    "sertifikasi_iso" to "true"
                ),
                qualityScore = 100.0
            ),
            DataRow(
                id = "row-vendor-2",
                fields = mapOf(
                    "nama_vendor" to "CV Beta Lestari",
                    "harga_penawaran" to "95",
                    "pengalaman_tahun" to "60",
                    "sertifikasi_iso" to "false"
                ),
                qualityScore = 95.0
            ),
            DataRow(
                id = "row-vendor-3",
                fields = mapOf(
                    "nama_vendor" to "PT Gamma Utama",
                    "harga_penawaran" to "40",
                    "pengalaman_tahun" to "85",
                    "sertifikasi_iso" to "true"
                ),
                qualityScore = 100.0
            )
        )

        val scoredResults = rows.map { row ->
            val res = selectionEngine.scoreAndRankRow(row, criteria)
            println("\nCandidate: ${row.fields["nama_vendor"]}")
            println(" Raw Score Breakdown: ${res.scoreBreakdown.map { "${it.first.criterionName}: ${it.second} (weight: ${it.first.weightPercentage}%)" }}")
            println(" Total Score: ${res.totalScore}, Risk Score: ${res.riskScore}, Confidence: ${res.confidenceScore}")

            // BUKTI HITUNG MANUAL:
            var manualSum = 0.0
            res.scoreBreakdown.forEach { (criterion, rawScore) ->
                val weighted = rawScore * (criterion.weightPercentage / 100.0)
                manualSum += weighted
            }
            val manualRounded = Math.round(manualSum * 100.0) / 100.0

            assertEquals(manualRounded, res.totalScore, "Total score (${res.totalScore}) harus IDENTIK dengan hitung manual ($manualRounded)")
            println(" [PROVEN IDENTICAL] Hitung manual ($manualRounded) == Algorithm Total Score (${res.totalScore})")
            res
        }

        // Rank All Results
        val ranked = selectionEngine.rankAllResults(scoredResults)
        println("\n=== HASIL RANKING & KLASIFIKASI ===")
        ranked.forEach { r ->
            println(" Rank #${r.rankPosition}: ${r.result.row?.fields?.get("nama_vendor")} - Score: ${r.result.totalScore} | Priority: ${r.priorityLevel} | Classification: ${r.classification}")
        }

        // Verifikasi urutan peringkat terurut descending
        for (i in 0 until ranked.size - 1) {
            assertTrue(ranked[i].result.totalScore >= ranked[i + 1].result.totalScore, "Rank #${i + 1} harus >= Rank #${i + 2}")
        }
        assertEquals(1, ranked[0].rankPosition)
        assertEquals(3, ranked[2].rankPosition)
    }

    @Test
    fun test3_EndToEnd10NodeWorkflowExecutionOnRealDataset() = runBlocking {
        println("\n=== SUB-TEST 3: EKSEKUSI WORKFLOW 10-NODE LENGKAP & VERIFIKASI SUPABASE POSTGRESQL ===")
        val tenantId = "tenant-test-selection-e"
        val requestId = "req-e-${UUID.randomUUID().toString().take(8)}"

        // 1. Setup Request di Database
        val requestRecord = SelectionRequestRecord(
            id = requestId,
            tenant_id = tenantId,
            user_id = "usr-tester-e",
            prompt_text = "Pilih vendor penyedia suku cadang terbaik dengan harga efisien dan keandalan tinggi",
            status = "processing"
        )
        val createdReq = selectionRepo.createSelectionRequest(requestRecord)
        assertNotNull(createdReq, "Selection request harus tersimpan di Supabase")

        // 2. Siapkan dataset nyata dengan 5 vendor
        val dataset = ExtractedDataset(
            schema = mapOf(
                "nama_vendor" to "string",
                "harga_penawaran" to "currency",
                "pengalaman_tahun" to "number",
                "kecepatan_pengiriman_hari" to "number",
                "sertifikasi_resmi" to "boolean"
            ),
            rows = listOf(
                mapOf("nama_vendor" to "PT Nusantara Logistik", "harga_penawaran" to "85", "pengalaman_tahun" to "92", "kecepatan_pengiriman_hari" to "90", "sertifikasi_resmi" to "true"),
                mapOf("nama_vendor" to "CV Sumber Makmur", "harga_penawaran" to "65", "pengalaman_tahun" to "78", "kecepatan_pengiriman_hari" to "80", "sertifikasi_resmi" to "true"),
                mapOf("nama_vendor" to "PT Mega Perkasa", "harga_penawaran" to "95", "pengalaman_tahun" to "55", "kecepatan_pengiriman_hari" to "65", "sertifikasi_resmi" to "false"),
                mapOf("nama_vendor" to "PT Prima Jaya", "harga_penawaran" to "45", "pengalaman_tahun" to "88", "kecepatan_pengiriman_hari" to "85", "sertifikasi_resmi" to "true"),
                mapOf("nama_vendor" to "CV Barokah Teknik", "harga_penawaran" to "70", "pengalaman_tahun" to "60", "kecepatan_pengiriman_hari" to "70", "sertifikasi_resmi" to "false")
            )
        )

        // 3. Eksekusi 10-Node Workflow melalui Orchestration Engine
        val executionId = "exec-wf-e-${UUID.randomUUID().toString().take(8)}"
        val dataRows = dataset.rows.map { DataRow(fields = it) }
        val context = mutableMapOf<String, Any>(
            "selectionRequestId" to requestId,
            "tenantId" to tenantId,
            "promptText" to requestRecord.prompt_text,
            "dataset" to dataset,
            "dataRows" to dataRows
        )

        val execution = WorkflowExecution(
            id = executionId,
            tenantId = tenantId,
            workflowDefId = "universal_ai_selection",
            startNodeId = "n1-read-data",
            executionStatus = "running"
        )
        execution.context = context

        val execResult = selectionEngine.orchestrationEngine.run(execution)
        println("\nExecution Status: ${execResult.status}")
        println("Execution Output: ${execResult.finalOutput}")
        println("Execution Trace Spans / Steps: ${execResult.executionId}")
        assertEquals("COMPLETED", execResult.status, "Status workflow universal_ai_selection harus COMPLETED")

        // 4. Verifikasi Data Tersimpan di Supabase PostgreSQL
        println("\n=== RAW SQL / DATABASE QUERY VERIFICATION ===")
        val savedResults = selectionRepo.getResults(requestId, tenantId)
        println("Raw Records Found in selection_results: ${savedResults.size}")
        assertTrue(savedResults.isNotEmpty(), "selection_results tidak boleh kosong di Supabase")
        assertEquals(5, savedResults.size, "Harus tersimpan 5 hasil seleksi")

        // Verifikasi Tiap Baris Data Mentah & Bukti Hitung Ulang Manual
        savedResults.forEach { record ->
            val rowRef = record.row_reference?.jsonObject
            val vendorName = rowRef?.get("nama_vendor")?.jsonPrimitive?.content ?: "Unknown"
            println("---------------------------------------------------------------------------------")
            println(" [DB RECORD] ID: ${record.id} | Rank #${record.rank_position} | Vendor: $vendorName")
            println(" Total Score: ${record.total_score} | Priority: ${record.priority_level} | Class: ${record.recommendation_classification}")
            println(" Risk Score: ${record.risk_score} | Confidence: ${record.confidence_score}")
            println(" Raw Score Breakdown JSON: ${record.score_breakdown}")
            println(" AI Insight: ${record.ai_insight_text}")

            assertNotNull(record.score_breakdown, "score_breakdown wajib tersimpan di database")
            val breakdownObj = record.score_breakdown!!.jsonObject
            var manualRecalculatedScore = 0.0
            breakdownObj.values.forEach { scoreVal ->
                manualRecalculatedScore += scoreVal.jsonPrimitive.content.toDoubleOrNull() ?: 0.0
            }
            val manualRounded = Math.round(manualRecalculatedScore * 100.0) / 100.0
            // Tolerance of 0.02 for floating sum
            val diff = Math.abs(manualRounded - record.total_score)
            assertTrue(diff <= 0.02, "Manual recalculated score ($manualRounded) harus identik dengan total_score (${record.total_score})")
            println(" => Verified Identical: Recalculated ($manualRounded) == DB total_score (${record.total_score})")
        }

        // 5. Verifikasi Request Status Updated to Completed
        val updatedReq = selectionRepo.getSelectionRequestById(requestId, tenantId)
        assertNotNull(updatedReq)
        assertEquals("completed", updatedReq!!.status, "Status request di database harus 'completed'")
        println(" [OK] Selection Request status di Supabase terbukti 'completed'")

        // 6. Verifikasi Analytics Summary
        val analytics = selectionRepo.getAnalytics(requestId, tenantId)
        assertTrue(analytics.isNotEmpty(), "Data analitik dan visualisasi harus tersimpan di selection_analytics_summary")
        println(" [OK] Analytics Records tersimpan: ${analytics.size} record")
    }

    @Test
    fun test4_RoleAndConditionExhaustiveMatrix() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val tenantId = "tenant-matrix-001"

        println("\n=== SUB-TEST 4: EXHAUSTIVE RBAC MATRIX & CONDITION TESTING ===")
        val roles = listOf(
            "SUPER_ADMIN" to HttpStatusCode.OK,
            "TENANT_OWNER" to HttpStatusCode.OK,
            "TENANT_ADMIN" to HttpStatusCode.OK,
            "DEPT_MANAGER" to HttpStatusCode.OK,
            "STAFF_HUMAN" to HttpStatusCode.OK,
            "Unauthenticated" to HttpStatusCode.Unauthorized
        )

        for ((role, expectedStatus) in roles) {
            val res = if (role == "Unauthenticated") {
                client.get("/api/v1/selection/requests")
            } else {
                val token = generateToken(tenantId, "usr-$role", role)
                client.get("/api/v1/selection/requests") {
                    header("Authorization", "Bearer $token")
                }
            }

            assertEquals(expectedStatus, res.status, "Role $role harus menghasilkan status $expectedStatus")
            println(" [RBAC MATRIX] Role: $role -> Response Code: ${res.status.value} ${res.status.description} [PASS]")
        }
    }

    @Test
    fun test5_DefinitionOfDoneBagianE_50RowsSupplierDatasetSelectionAndRanking() = runBlocking {
        println("\n==========================================================================================")
        println("=== DEFINITION OF DONE BAGIAN E: 50 BARIS DATA SUPPLIER NYATA, WORKFLOW 10-NODE & BUKTI HITUNG IDENTIK ===")
        println("==========================================================================================")
        val tenantId = "tenant-supplier-50-dod"
        val requestId = "req-dod-50-${UUID.randomUUID().toString().take(8)}"

        // 1. Setup Request di Database
        val requestRecord = SelectionRequestRecord(
            id = requestId,
            tenant_id = tenantId,
            user_id = "usr-procurement-head",
            prompt_text = "Evaluasi dan seleksi 50 supplier komponen industri dengan prioritas efisiensi biaya, reliabilitas pengiriman, dan kepatuhan standar sertifikasi",
            status = "processing"
        )
        val createdReq = selectionRepo.createSelectionRequest(requestRecord)
        assertNotNull(createdReq, "Selection request 50 baris supplier harus tersimpan di Supabase")

        // 2. Generate 50 baris data supplier nyata dengan variasi realistis
        val vendorTypes = listOf("PT", "CV", "Firma")
        val vendorNames = listOf(
            "Adhi Karya Mandiri", "Barokah Teknik Nusantara", "Cipta Perkasa Abadi", "Duta Logistik Terpadu",
            "Elang Surya Semesta", "Fajar Megah Sentosa", "Garuda Presisi Indo", "Harapan Jaya Mandiri",
            "Indo Suku Cadang Utama", "Jaya Raya Ekpres", "Karya Bersama Lestari", "Laju Distribusi Utama",
            "Mitra Industri Sejahtera", "Nusantara Supply Chain", "Omega Fabrikasi Presisi", "Prima Komponen Indo",
            "Quality Part Nusantara", "Roda Maju Gemilang", "Surya Graha Industri", "Tri Kencana Logistik",
            "Utama Tehnik Perkasa", "Varia Niaga Logistik", "Wahana Sinar Terang", "Xpress Cargo Industri",
            "Yasa Prima Manufaktur", "Zenith Mesin Mandiri", "Alfa Logistik Sentosa", "Bina Karya Sejati",
            "Cahaya Baru Teknik", "Delta Presisi Tama", "Eka Sarana Industri", "Focus Komponen Mandiri",
            "Global Mekanik Sejahtera", "Hasti Graha Perkasa", "Inti Mandiri Sukses", "Jagat Tehnik Pratama",
            "Kencana Sukses Terpadu", "Lentera Logistik Indo", "Mega Tehnik Perkasa", "Nusa Graha Mandiri",
            "Optima Manufaktur Prima", "Panca Mitra Perkasa", "Quantum Sukses Teknik", "Rapi Fast Express",
            "Sinar Harapan Indo", "Trans Logistik Mandiri", "United Parts Sejahtera", "Visi Baru Teknik",
            "Wijaya Makmur Terpadu", "Zamrud Distribusi Perkasa"
        )

        val supplierRows = (1..50).map { i ->
            val vName = "${vendorTypes[(i - 1) % vendorTypes.size]} ${vendorNames[i - 1]}"
            // Deterministic, realistic operational metrics
            val hargaPenawaran = (40 + ((i * 7) % 55)).toString() // 40..94
            val pengalamanTahun = (5 + ((i * 3) % 25)).toString() // 5..29 tahun
            val kecepatanPengiriman = (60 + ((i * 11) % 38)).toString() // 60..97 score
            val sertifikasiResmi = if (i % 3 != 0) "true" else "false" // 67% certified
            val kapasitasBulanan = (500 + ((i * 17) % 4500)).toString() // unit/bulan

            mapOf(
                "nama_vendor" to vName,
                "harga_penawaran" to hargaPenawaran,
                "pengalaman_tahun" to pengalamanTahun,
                "kecepatan_pengiriman" to kecepatanPengiriman,
                "sertifikasi_resmi" to sertifikasiResmi,
                "kapasitas_produksi_bulanan" to kapasitasBulanan
            )
        }

        val dataset50 = ExtractedDataset(
            schema = mapOf(
                "nama_vendor" to "string",
                "harga_penawaran" to "currency",
                "pengalaman_tahun" to "number",
                "kecepatan_pengiriman" to "number",
                "sertifikasi_resmi" to "boolean",
                "kapasitas_produksi_bulanan" to "number"
            ),
            rows = supplierRows
        )

        // 3. Eksekusi 10-Node Workflow untuk 50 baris supplier
        val dataRows = dataset50.rows.map { DataRow(fields = it) }
        val executionId = "exec-dod-50-${UUID.randomUUID().toString().take(8)}"
        val context = mutableMapOf<String, Any>(
            "selectionRequestId" to requestId,
            "tenantId" to tenantId,
            "promptText" to requestRecord.prompt_text,
            "dataset" to dataset50,
            "dataRows" to dataRows
        )

        val execution = WorkflowExecution(
            id = executionId,
            tenantId = tenantId,
            workflowDefId = "universal_ai_selection",
            startNodeId = "n1-read-data",
            executionStatus = "running"
        )
        execution.context = context

        println("Memulai eksekusi 10-Node Workflow universal_ai_selection pada 50 baris data supplier...")
        val execResult = selectionEngine.orchestrationEngine.run(execution)
        println("Workflow Result Status: ${execResult.status}")
        assertEquals("COMPLETED", execResult.status, "Workflow universal_ai_selection harus berstatus COMPLETED")

        // 4. Query langsung ke Supabase PostgreSQL database
        val savedResults = selectionRepo.getResults(requestId, tenantId)
        println("Total baris tersimpan di selection_results: ${savedResults.size}")
        assertEquals(50, savedResults.size, "WAJIB tersimpan PERSIS 50 baris hasil seleksi di database selection_results")

        // 5. Verifikasi Peringkat #1 - #50 terurut sempurna descending
        for (i in 0 until savedResults.size) {
            val rec = savedResults[i]
            assertEquals(i + 1, rec.rank_position, "Baris ke-$i harus memiliki rank_position #${i + 1}")
            if (i < savedResults.size - 1) {
                val nextRec = savedResults[i + 1]
                assertTrue(
                    rec.total_score >= nextRec.total_score,
                    "Total score rank #${rec.rank_position} (${rec.total_score}) harus >= rank #${nextRec.rank_position} (${nextRec.total_score})"
                )
            }
        }

        // 6. Verifikasi Distribusi Priority Level (Top 20% High, Next 40% Medium, Remaining 40% Low)
        // 50 * 0.2 = 10 -> Rank 1..10 harus "high"
        // 50 * 0.6 = 30 -> Rank 11..30 harus "medium"
        // Sisanya 31..50 -> harus "low"
        for (i in 0 until 10) {
            assertEquals("high", savedResults[i].priority_level, "Rank #${i + 1} (Top 20%) harus priority high")
        }
        for (i in 10 until 30) {
            assertEquals("medium", savedResults[i].priority_level, "Rank #${i + 1} (Next 40%) harus priority medium")
        }
        for (i in 30 until 50) {
            assertEquals("low", savedResults[i].priority_level, "Rank #${i + 1} (Bottom 40%) harus priority low")
        }
        println(" [OK] Distribusi prioritas terbukti presisi: 10 High (Top 20%), 20 Medium (40%), 20 Low (40%)")

        // 7. BUKTI HITUNG ULANG MANUAL PADA SELURUH 50 BARIS (WAJIB IDENTIK)
        println("\n=== BUKTI HITUNG ULANG MANUAL 50 BARIS SUPPLIER (IDENTIK) ===")
        var identicalCount = 0
        savedResults.forEach { record ->
            val rowRef = record.row_reference?.jsonObject
            val vName = rowRef?.get("nama_vendor")?.jsonPrimitive?.content ?: "Supplier-${record.rank_position}"

            assertNotNull(record.score_breakdown, "score_breakdown wajib tersimpan di database")
            val breakdownObj = record.score_breakdown!!.jsonObject

            var manualSum = 0.0
            breakdownObj.values.forEach { scoreVal ->
                manualSum += scoreVal.jsonPrimitive.content.toDoubleOrNull() ?: 0.0
            }
            val manualRounded = Math.round(manualSum * 100.0) / 100.0
            val diff = Math.abs(manualRounded - record.total_score)
            assertTrue(
                diff <= 0.02,
                "Hitung manual ($manualRounded) untuk Rank #${record.rank_position} ($vName) harus IDENTIK dengan DB total_score (${record.total_score})"
            )
            identicalCount++

            if (record.rank_position <= 5 || record.rank_position >= 48) {
                println(" Rank #${record.rank_position}: $vName | Score: ${record.total_score} | Hitung Manual: $manualRounded | Selisih: $diff | Class: ${record.recommendation_classification} | [IDENTIK]")
            }
        }
        assertEquals(50, identicalCount, "Seluruh 50 baris supplier harus terbukti identik antara hitung manual dan database")
        println(" [PROVEN IDENTICAL] Terbukti 50/50 baris supplier memiliki nilai total_score identik dengan hitung manual!")

        // 8. Verifikasi Request Status Updated to Completed
        val updatedReq = selectionRepo.getSelectionRequestById(requestId, tenantId)
        assertNotNull(updatedReq)
        assertEquals("completed", updatedReq!!.status, "Status request di database harus 'completed'")

        // 9. Verifikasi Analytics Records
        val analytics = selectionRepo.getAnalytics(requestId, tenantId)
        assertTrue(analytics.isNotEmpty(), "Analytics summary untuk 50 baris supplier harus tersimpan di selection_analytics_summary")
        println(" [OK] Analytics Records tersimpan: ${analytics.size} record")
        println("==========================================================================================")
        println("=== DEFINITION OF DONE BAGIAN E: SUKSES 100% DAN SELURUH KRITERIA TERVERIFIKASI ===")
        println("==========================================================================================\n")
    }
}

