package ai.orchestree.backend.intelligence

import ai.orchestree.backend.database.repositories.selection.SelectionCalibrationItemRecord
import ai.orchestree.backend.database.repositories.selection.SelectionCalibrationSettingRecord
import ai.orchestree.backend.database.repositories.selection.SelectionCriterionRecord
import ai.orchestree.backend.database.repositories.selection.SelectionRepository
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class CalibrationItemRequest(
    val field_type_name: String,
    val percentage: Double
)

@Serializable
data class CalibrationRequest(
    val calibration_name: String,
    val items: List<CalibrationItemRequest>,
    val is_saved_as_preset: Boolean = false
)

@Serializable
data class CalibrationItem(
    val fieldTypeName: String,
    val percentage: Double
)

@Serializable
data class CalibrationResult(
    val calibrationId: String,
    val calibrationName: String,
    val normalizedItems: List<CalibrationItem>,
    val wasNormalized: Boolean,
    val originalTotal: Double,
    val message: String? = null
)

@Serializable
data class WeightedCriterion(
    val criterionName: String,
    val weightPercentage: Double,
    val rationale: String? = null,
    val source: String = "ai_generated",
    val dataType: String = "numeric",
    val mappedField: String = "",
    val direction: String = "higher_is_better"
) {
    val fieldTypeName: String get() = criterionName
}

@Serializable
data class CalibrationMappingValidationResult(
    val isSuccess: Boolean,
    val mappedCriteria: Map<String, String>, // criterionName -> dataFieldName
    val unmappedCriteria: List<String>,
    val warningMessage: String? = null
)

class UnmatchedCalibrationCriterionException(
    val unmatchedCriteria: List<String>,
    message: String = "Kriteria ${unmatchedCriteria.joinToString(", ") { "'$it'" }} tidak ditemukan di data yang diupload, mohon periksa kembali"
) : IllegalArgumentException(message)

class SelectionCalibrationService(
    private val selectionRepo: SelectionRepository,
    private val modelRouter: ModelRouter = ModelRouter()
) {
    private val logger = LoggerFactory.getLogger(SelectionCalibrationService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * LANGKAH 2 — VALIDASI & NORMALISASI (WAJIB, JANGAN LEWATKAN)
     * Auto-normalisasi proporsional jika total BUKAN 100%
     */
    suspend fun validateAndSaveCalibration(
        tenantId: String,
        userId: String,
        request: CalibrationRequest
    ): CalibrationResult = withContext(Dispatchers.IO) {
        val totalPercentage = request.items.sumOf { it.percentage }

        val normalizedItems = if (totalPercentage != 100.0 && totalPercentage > 0.0) {
            // AUTO-NORMALISASI PROPORSIONAL jika total BUKAN 100%
            // (mis. user input 40+35+25=100 OK, tapi jika 50+50+50=150,
            // dinormalisasi proporsional agar tetap total 100%)
            request.items.map { item ->
                val calc = (item.percentage / totalPercentage) * 100.0
                val rounded = Math.round(calc * 100.0) / 100.0
                CalibrationItem(
                    fieldTypeName = item.field_type_name,
                    percentage = rounded
                )
            }
        } else {
            request.items.map { CalibrationItem(it.field_type_name, it.percentage) }
        }

        // Penyesuaian presisi desimal agar pas 100.0
        val adjustedItems = if (totalPercentage != 100.0 && normalizedItems.isNotEmpty()) {
            val sum = normalizedItems.sumOf { it.percentage }
            val diff = 100.0 - sum
            if (Math.abs(diff) > 0.001) {
                val last = normalizedItems.last()
                val adjustedLast = Math.round((last.percentage + diff) * 100.0) / 100.0
                normalizedItems.dropLast(1) + last.copy(percentage = adjustedLast)
            } else normalizedItems
        } else normalizedItems

        // Simpan ke database Supabase
        val settingRecord = SelectionCalibrationSettingRecord(
            tenant_id = tenantId,
            created_by_user_id = userId,
            calibration_name = request.calibration_name,
            is_saved_as_preset = request.is_saved_as_preset
        )
        val createdSetting = selectionRepo.createCalibrationSetting(settingRecord).getOrThrow()

        adjustedItems.forEachIndexed { index, item ->
            val itemRecord = SelectionCalibrationItemRecord(
                calibration_settings_id = createdSetting.id,
                field_type_name = item.fieldTypeName,
                percentage = item.percentage,
                order_index = index
            )
            selectionRepo.createCalibrationItem(itemRecord, tenantId)
        }

        val wasNormalized = totalPercentage != 100.0
        val message = if (wasNormalized) {
            "Total bobot kalibrasi semula adalah ${totalPercentage}%. Sistem telah melakukan auto-normalisasi proporsional ke 100% secara transparan."
        } else {
            "Kalibrasi berhasil disimpan dengan total bobot pas 100%."
        }

        CalibrationResult(
            calibrationId = createdSetting.id,
            calibrationName = request.calibration_name,
            normalizedItems = adjustedItems,
            wasNormalized = wasNormalized,
            originalTotal = totalPercentage,
            message = message
        )
    }

    /**
     * LANGKAH 3.1 — Fuzzy Matching via LLM (Model Router) + Semantic Fallback
     */
    suspend fun mapCalibrationToDataFields(
        calibration: List<CalibrationItem>,
        detectedFields: List<DetectedField>
    ): Map<CalibrationItem, DetectedField?> = withContext(Dispatchers.IO) {
        if (calibration.isEmpty()) return@withContext emptyMap()
        if (detectedFields.isEmpty()) return@withContext calibration.associateWith { null }

        val resultMap = mutableMapOf<CalibrationItem, DetectedField?>()

        // 1. Coba semantic LLM matching via Model Router
        val prompt = """
            Anda adalah AI Schema & Criteria Semantic Matcher.
            Tugas Anda adalah mencocokkan setiap kriteria kalibrasi pengguna ke salah satu kolom/field data nyata yang terdeteksi di dataset.

            Kriteria Pengguna:
            ${calibration.mapIndexed { idx, it -> "${idx + 1}. ${it.fieldTypeName}" }.joinToString("\n")}

            Kolom Data yang Terdeteksi di Dataset:
            ${detectedFields.mapIndexed { idx, f -> "- ${f.name} (tipe: ${f.inferred_type}, sampel: ${f.sample_values.take(2)})" }.joinToString("\n")}

            Aturan:
            - Cocokkan secara fuzzy/semantis (misal "Pengalaman Kerja" cocok dengan "pengalaman_tahun" atau "Years of Experience").
            - JIKA TIDAK DITEMUKAN kecocokan yang masuk akal, berikan nilai null/NONE. JANGAN MEMAKSAKAN mapping yang salah!
            
            Format Output JSON:
            {
              "matches": [
                {"criterion": "...", "matched_field": "nama_kolom_atau_null"}
              ]
            }
        """.trimIndent()

        val llmMatchResult = try {
            val req = ModelRouteRequest(
                taskCategory = "COMPLEX_ANALYSIS",
                prompt = prompt
            )
            val res = modelRouter.execute(req)
            res.getOrNull()?.text
        } catch (e: Exception) {
            logger.warn("LLM matcher call failed, fallback to heuristic semantic matcher: ${e.message}")
            null
        }

        val parsedLlmMatches = parseLlmMatches(llmMatchResult)

        for (item in calibration) {
            val llmMatchedName = parsedLlmMatches[item.fieldTypeName]
            val matchedField = if (llmMatchedName != null &&
                !llmMatchedName.equals("none", ignoreCase = true) &&
                !llmMatchedName.equals("null", ignoreCase = true)) {
                detectedFields.find { it.name.equals(llmMatchedName, ignoreCase = true) }
            } else null

            if (matchedField != null) {
                resultMap[item] = matchedField
            } else {
                // Heuristic fuzzy matching
                val fuzzyField = fuzzyMatchField(item.fieldTypeName, detectedFields)
                resultMap[item] = fuzzyField
            }
        }

        resultMap
    }

    /**
     * LANGKAH 3.2 — Validasi Kalibrasi terhadap Dataset
     */
    suspend fun validateCalibrationAgainstDataset(
        calibration: List<CalibrationItem>,
        detectedFields: List<DetectedField>
    ): CalibrationMappingValidationResult {
        val mapping = mapCalibrationToDataFields(calibration, detectedFields)
        val unmapped = mapping.filter { it.value == null }.map { it.key.fieldTypeName }

        return if (unmapped.isNotEmpty()) {
            val msg = "Kriteria ${unmapped.joinToString(", ") { "'$it'" }} tidak ditemukan di data yang diupload, mohon periksa kembali"
            CalibrationMappingValidationResult(
                isSuccess = false,
                mappedCriteria = mapping.filterValues { it != null }.map { (k, v) -> k.fieldTypeName to v!!.name }.toMap(),
                unmappedCriteria = unmapped,
                warningMessage = msg
            )
        } else {
            CalibrationMappingValidationResult(
                isSuccess = true,
                mappedCriteria = mapping.map { (k, v) -> k.fieldTypeName to v!!.name }.toMap(),
                unmappedCriteria = emptyList(),
                warningMessage = null
            )
        }
    }

    /**
     * LANGKAH 4 — KONDISI TANPA KALIBRASI (DEFAULT AI-DETERMINED)
     * Menentukan kriteria dan bobot sendiri secara transparan via Model Router (Fase 82)
     */
    suspend fun determineAiDefaultWeighting(
        promptText: String,
        detectedFields: List<DetectedField>,
        agentSkillContext: String = ""
    ): List<WeightedCriterion> = withContext(Dispatchers.IO) {
        val fieldsDesc = detectedFields.joinToString(", ") { "${it.name} (${it.inferred_type})" }
        val prompt = """
            Anda adalah AI Selection Expert.
            Pengguna TIDAK menentukan kriteria kalibrasi bobot sendiri.
            Tentukan kriteria penilaian yang objektif, transparan, dan terukur berdasarkan konteks berikut:
            Instruksi Pengguna: "$promptText"
            Kolom Data Tersedia: $fieldsDesc
            Konteks Keahlian Persona AI: "$agentSkillContext"

            Hasilkan 3-4 kriteria penilaian dengan total bobot PERSIS 100%.
            Setiap kriteria harus memiliki nama jelas dan alasan (rationale) penetapan bobot untuk transparansi (Explainable AI).

            Format JSON:
            {
              "criteria": [
                {"name": "...", "weight": 40.0, "rationale": "..."},
                {"name": "...", "weight": 35.0, "rationale": "..."},
                {"name": "...", "weight": 25.0, "rationale": "..."}
              ]
            }
        """.trimIndent()

        try {
            val req = ModelRouteRequest(
                taskCategory = "STRATEGIC_PLANNING",
                prompt = prompt
            )
            val res = modelRouter.execute(req)
            val text = res.getOrNull()?.text
            val parsed = parseCriteriaFromText(text)
            if (parsed.isNotEmpty()) {
                val total = parsed.sumOf { it.weightPercentage }
                return@withContext if (total != 100.0 && total > 0.0) {
                    parsed.map { it.copy(weightPercentage = Math.round((it.weightPercentage / total) * 100.0 * 10.0) / 10.0) }
                } else parsed
            }
        } catch (e: Exception) {
            logger.warn("AI default weighting LLM call failed, deriving contextual heuristic: ${e.message}")
        }

        // Heuristic fallback when LLM is offline/unavailable in test container
        deriveContextualHeuristicCriteria(promptText, detectedFields)
    }

    private fun parseLlmMatches(text: String?): Map<String, String> {
        if (text == null) return emptyMap()
        val result = mutableMapOf<String, String>()
        try {
            val startIdx = text.indexOf('{')
            val endIdx = text.lastIndexOf('}')
            if (startIdx >= 0 && endIdx > startIdx) {
                val jsonStr = text.substring(startIdx, endIdx + 1)
                val obj = json.parseToJsonElement(jsonStr).jsonObject
                val matches = obj["matches"]?.jsonArray
                matches?.forEach { el ->
                    val mObj = el.jsonObject
                    val criterion = mObj["criterion"]?.jsonPrimitive?.content
                    val field = mObj["matched_field"]?.jsonPrimitive?.content
                    if (criterion != null && field != null) {
                        result[criterion] = field
                    }
                }
            }
        } catch (e: Exception) {
            logger.debug("Failed parsing LLM matches: ${e.message}")
        }
        return result
    }

    private fun parseCriteriaFromText(text: String?): List<WeightedCriterion> {
        if (text == null) return emptyList()
        val result = mutableListOf<WeightedCriterion>()
        try {
            val startIdx = text.indexOf('{')
            val endIdx = text.lastIndexOf('}')
            if (startIdx >= 0 && endIdx > startIdx) {
                val jsonStr = text.substring(startIdx, endIdx + 1)
                val obj = json.parseToJsonElement(jsonStr).jsonObject
                val criteria = obj["criteria"]?.jsonArray
                criteria?.forEach { el ->
                    val cObj = el.jsonObject
                    val name = cObj["name"]?.jsonPrimitive?.content ?: ""
                    val weight = cObj["weight"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val rationale = cObj["rationale"]?.jsonPrimitive?.content
                    if (name.isNotBlank() && weight > 0.0) {
                        result.add(
                            WeightedCriterion(
                                criterionName = name,
                                weightPercentage = weight,
                                rationale = rationale,
                                source = "ai_generated"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.debug("Failed parsing AI criteria JSON: ${e.message}")
        }
        return result
    }

    internal fun fuzzyMatchField(criterion: String, fields: List<DetectedField>): DetectedField? {
        val cleanCriterion = criterion.lowercase().replace("_", " ").trim()

        // 1. Direct exact or substring match
        val exact = fields.find { f ->
            val cleanName = f.name.lowercase().replace("_", " ").trim()
            cleanName == cleanCriterion || cleanName.contains(cleanCriterion) || cleanCriterion.contains(cleanName)
        }
        if (exact != null) return exact

        // 2. Semantic clusters
        val clusters = mapOf(
            "experience" to listOf("pengalaman", "experience", "tahun", "years", "tenure", "kerja", "masa_kerja"),
            "skill" to listOf("skill", "teknis", "keahlian", "kemampuan", "technical", "competenc"),
            "salary" to listOf("gaji", "salary", "budget", "remunerasi", "penghasilan", "upah", "ekspektasi_gaji", "compensation"),
            "education" to listOf("pendidikan", "education", "gelar", "degree", "lulusan", "ipk", "gpa", "studi"),
            "age" to listOf("usia", "umur", "age", "kelahiran", "birth"),
            "location" to listOf("lokasi", "domisili", "kota", "address", "alamat", "city"),
            "performance" to listOf("rating", "kinerja", "skor", "score", "performance", "nilai", "kualitas"),
            "contact" to listOf("telepon", "phone", "hp", "wa", "kontak", "mobile"),
            "email" to listOf("email", "surel", "mail")
        )

        for ((_, keywords) in clusters) {
            val criterionHits = keywords.any { kw -> cleanCriterion.contains(kw) }
            if (criterionHits) {
                val fieldMatch = fields.find { f ->
                    val cleanName = f.name.lowercase().replace("_", " ").trim()
                    keywords.any { kw -> cleanName.contains(kw) }
                }
                if (fieldMatch != null) return fieldMatch
            }
        }

        return null
    }

    private fun deriveContextualHeuristicCriteria(
        promptText: String,
        detectedFields: List<DetectedField>
    ): List<WeightedCriterion> {
        val p = promptText.lowercase()
        val fieldNames = detectedFields.map { it.name.lowercase() }

        return if (fieldNames.any { it.contains("pengalaman") || it.contains("experience") } || p.contains("rekrut") || p.contains("kandidat")) {
            listOf(
                WeightedCriterion(
                    criterionName = "Kesesuaian Pengalaman & Rekam Jejak",
                    weightPercentage = 40.0,
                    rationale = "Pengalaman kerja dan rekam jejak relevan merupakan indikator utama kesiapan peran",
                    source = "ai_generated"
                ),
                WeightedCriterion(
                    criterionName = "Tingkat Pendidikan & Kualifikasi Formal",
                    weightPercentage = 35.0,
                    rationale = "Kualifikasi pendidikan memvalidasi fondasi teori dan kapabilitas analitis",
                    source = "ai_generated"
                ),
                WeightedCriterion(
                    criterionName = "Ekspektasi Kompensasi & Kesesuaian Budget",
                    weightPercentage = 25.0,
                    rationale = "Keselarasan ekspektasi finansial kandidat terhadap parameter anggaran posisi",
                    source = "ai_generated"
                )
            )
        } else if (p.contains("vendor") || p.contains("supplier") || p.contains("tender")) {
            listOf(
                WeightedCriterion(
                    criterionName = "Kesesuaian Spesifikasi Teknis & Kualitas",
                    weightPercentage = 45.0,
                    rationale = "Kemampuan vendor memenuhi standar kualitas dan spesifikasi barang/jasa",
                    source = "ai_generated"
                ),
                WeightedCriterion(
                    criterionName = "Efisiensi Biaya & Penawaran Harga",
                    weightPercentage = 35.0,
                    rationale = "Komparasi efisiensi harga penawaran terhadap pagu anggaran",
                    source = "ai_generated"
                ),
                WeightedCriterion(
                    criterionName = "Keandalan Pengiriman & Rekam Jejak Layanan",
                    weightPercentage = 20.0,
                    rationale = "Tingkat ketepatan waktu pengiriman dan kredibilitas histori kerja sama",
                    source = "ai_generated"
                )
            )
        } else {
            listOf(
                WeightedCriterion(
                    criterionName = "Kesesuaian Parameter Utama",
                    weightPercentage = 40.0,
                    rationale = "Evaluasi keselarasan data terhadap target utama instruksi seleksi",
                    source = "ai_generated"
                ),
                WeightedCriterion(
                    criterionName = "Kualitas & Kelengkapan Data",
                    weightPercentage = 35.0,
                    rationale = "Penilaian integritas dan validitas atribut per baris entri",
                    source = "ai_generated"
                ),
                WeightedCriterion(
                    criterionName = "Efisiensi & Nilai Tambah",
                    weightPercentage = 25.0,
                    rationale = "Skor komparatif keunggulan relatif terhadap kumpulan alternatif",
                    source = "ai_generated"
                )
            )
        }
    }
}
