package ai.orchestree.backend.intelligence

import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

/**
 * Extension function for sampling rows from extracted dataset
 */
fun List<Map<String, String>>.sample(rows: Int = 20): List<Map<String, String>> {
    return this.take(rows.coerceAtLeast(1))
}

@Serializable
data class DetectedField(
    val name: String,
    val inferred_type: String, // "string", "number", "email", "phone", "date", "currency", "boolean"
    val sample_values: List<String> = emptyList()
)

@Serializable
data class SchemaDetectionResult(
    val fields: List<DetectedField>,
    val inferredDomainCategory: String
) {
    fun toJson(): JsonElement = buildJsonObject {
        put("domain_category", inferredDomainCategory)
        put("fields", buildJsonArray {
            fields.forEach { f ->
                add(buildJsonObject {
                    put("name", f.name)
                    put("inferred_type", f.inferred_type)
                    put("sample_values", buildJsonArray {
                        f.sample_values.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                    })
                })
            }
        })
    }
}

@Serializable
data class DuplicateReport(
    val duplicateCount: Int,
    val duplicateGroups: List<DuplicateGroup> = emptyList()
)

@Serializable
data class DuplicateGroup(
    val primaryRowIndex: Int,
    val duplicateRowIndices: List<Int>,
    val matchedKeys: List<String>,
    val similarityScore: Double
)

@Serializable
data class InvalidRowEntry(
    val rowIndex: Int,
    val fieldName: String,
    val rawValue: String,
    val reason: String
)

@Serializable
data class QualityScoreDetails(
    val overallScore: Double, // 0.0 - 100.0
    val completenessScore: Double, // % field terisi non-empty
    val validityScore: Double, // % data valid
    val uniquenessScore: Double, // % data tidak duplikat
    val totalRows: Int,
    val validRowCount: Int,
    val duplicateRowCount: Int,
    val invalidRowCount: Int,
    val qualityGrade: String // "EXCELLENT", "GOOD", "FAIR", "POOR"
)

@Serializable
data class DataUnderstandingResult(
    val domainClassification: String,
    val normalizedData: List<Map<String, String>>,
    val duplicates: DuplicateReport,
    val invalidRows: List<InvalidRowEntry>,
    val qualityScore: QualityScoreDetails
) {
    val normalized: List<Map<String, String>> get() = normalizedData
}

/**
 * SCHEMA & FIELD DETECTION via LLM (Model Router, taskCategory=COMPLEX_ANALYSIS)
 */
class LlmDatasetDetector(
    private val modelRouter: ModelRouter
) {
    private val logger = LoggerFactory.getLogger(LlmDatasetDetector::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun detectSchemaAndFields(sampleRows: List<Map<String, String>>): SchemaDetectionResult = withContext(Dispatchers.IO) {
        if (sampleRows.isEmpty()) {
            return@withContext SchemaDetectionResult(
                fields = listOf(DetectedField("col1", "string", listOf("empty"))),
                inferredDomainCategory = "general"
            )
        }

        val allKeys = sampleRows.flatMap { it.keys }.distinct()
        val sampleRowsJson = buildJsonArray {
            sampleRows.take(10).forEach { row ->
                add(buildJsonObject {
                    row.forEach { (k, v) -> put(k, v) }
                })
            }
        }.toString()

        val prompt = """
            You are OrchestreeAI Intelligence Engine.
            Analyze the following dataset samples (up to 20 rows):
            $sampleRowsJson
            
            Available column names: ${allKeys.joinToString(", ")}
            
            Perform:
            1. Schema & Field Detection: For every column, detect its field name, inferred semantic data type (one of: "string", "number", "email", "phone", "date", "currency", "boolean"), and sample representative values.
            2. Domain Classification: Infer the domain category automatically (e.g. "recruitment", "finance", "tender", "procurement", "marketing", "logistics", "general"). DO NOT limit to fixed predefined categories.
            
            Output STRICTLY valid JSON with no markdown backticks:
            {
              "domain_category": "recruitment",
              "fields": [
                {
                  "name": "nama",
                  "inferred_type": "string",
                  "sample_values": ["John", "Jane"]
                }
              ]
            }
        """.trimIndent()

        try {
            val response = modelRouter.execute(
                ModelRouteRequest(
                    taskCategory = "COMPLEX_ANALYSIS",
                    sensitivityTier = "INTERNAL",
                    prompt = prompt,
                    temperature = 0.2,
                    responseFormatJson = true
                )
            )

            if (response.isSuccess) {
                val text = response.getOrThrow().text
                val parsed = parseLlmResponse(text, allKeys, sampleRows)
                if (parsed != null) {
                    return@withContext parsed
                }
            }
        } catch (e: Exception) {
            logger.warn("LLM schema detection call exception: ${e.message}")
        }

        // Intelligent fallback if LLM is offline
        fallbackSchemaDetection(sampleRows, allKeys)
    }

    private fun parseLlmResponse(
        text: String,
        allKeys: List<String>,
        sampleRows: List<Map<String, String>>
    ): SchemaDetectionResult? {
        return try {
            val cleanJson = text.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            val jsonElement = json.parseToJsonElement(cleanJson).jsonObject
            val domain = jsonElement["domain_category"]?.jsonPrimitive?.content ?: "general"
            val fieldsArray = jsonElement["fields"]?.jsonArray

            val fields = if (fieldsArray != null && fieldsArray.isNotEmpty()) {
                fieldsArray.map { fEl ->
                    val obj = fEl.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content ?: ""
                    val inferredType = obj["inferred_type"]?.jsonPrimitive?.content ?: "string"
                    val samples = obj["sample_values"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    DetectedField(name, inferredType, samples)
                }
            } else {
                fallbackFields(allKeys, sampleRows)
            }

            SchemaDetectionResult(
                fields = fields,
                inferredDomainCategory = domain
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse LLM schema response: ${e.message}")
            null
        }
    }

    private fun fallbackSchemaDetection(
        sampleRows: List<Map<String, String>>,
        allKeys: List<String>
    ): SchemaDetectionResult {
        val keysLower = allKeys.map { it.lowercase() }
        val inferredDomain = when {
            keysLower.any { it.contains("kandidat") || it.contains("pelamar") || it.contains("resume") || it.contains("cv") || it.contains("gaji") || it.contains("pendidikan") } -> "recruitment"
            keysLower.any { it.contains("vendor") || it.contains("tender") || it.contains("supplier") || it.contains("kontrak") || it.contains("pengadaan") } -> "tender"
            keysLower.any { it.contains("invoice") || it.contains("faktur") || it.contains("biaya") || it.contains("revenue") || it.contains("laba") || it.contains("anggaran") } -> "finance"
            keysLower.any { it.contains("campaign") || it.contains("lead") || it.contains("konversi") || it.contains("iklan") } -> "marketing"
            else -> "general"
        }

        return SchemaDetectionResult(
            fields = fallbackFields(allKeys, sampleRows),
            inferredDomainCategory = inferredDomain
        )
    }

    private fun fallbackFields(
        allKeys: List<String>,
        sampleRows: List<Map<String, String>>
    ): List<DetectedField> {
        return allKeys.map { key ->
            val keyLower = key.lowercase()
            val samples = sampleRows.mapNotNull { it[key]?.takeIf { s -> s.isNotBlank() } }.take(3)
            val inferredType = when {
                keyLower.contains("email") -> "email"
                keyLower.contains("phone") || keyLower.contains("telp") || keyLower.contains("hp") || keyLower.contains("telepon") -> "phone"
                keyLower.contains("tanggal") || keyLower.contains("date") || keyLower.contains("tgl") -> "date"
                keyLower.contains("harga") || keyLower.contains("gaji") || keyLower.contains("budget") || keyLower.contains("biaya") || keyLower.contains("nilai") -> "currency"
                keyLower.contains("skor") || keyLower.contains("score") || keyLower.contains("tahun") || keyLower.contains("usia") || keyLower.contains("pengalaman") || keyLower.contains("jumlah") -> "number"
                keyLower.contains("status") || keyLower.contains("aktif") || keyLower.contains("is_") -> "boolean"
                samples.any { it.contains("@") && it.contains(".") } -> "email"
                samples.any { it.all { ch -> ch.isDigit() || ch == '+' || ch == '-' || ch == ' ' } && it.length >= 8 } -> "phone"
                samples.any { it.toDoubleOrNull() != null } -> "number"
                else -> "string"
            }
            DetectedField(name = key, inferred_type = inferredType, sample_values = samples)
        }
    }

    suspend fun scoreTextualCriterion(value: Any?, fieldTypeName: String): Double = withContext(Dispatchers.IO) {
        val text = value?.toString()?.trim() ?: return@withContext 40.0
        if (text.isBlank()) return@withContext 30.0

        val prompt = """
            You are OrchestreeAI Evaluation Specialist.
            Evaluate how well the following text satisfies the criterion "$fieldTypeName":
            Text: "$text"

            Provide an objective rating score between 0.0 and 100.0 based on relevance, quality, and depth.
            Output strictly a JSON object with format: {"score": 85.0, "reason": "..."}
        """.trimIndent()

        return@withContext try {
            val res = modelRouter.execute(
                ModelRouteRequest(
                    taskCategory = "STRUCTURED_REASONING",
                    prompt = prompt,
                    temperature = 0.2,
                    responseFormatJson = true
                )
            )
            if (res.isSuccess) {
                val textOut = res.getOrThrow().text
                val clean = textOut.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val parsedObj = json.parseToJsonElement(clean).jsonObject
                val score = parsedObj["score"]?.jsonPrimitive?.doubleOrNull ?: 75.0
                Math.round(score * 100.0) / 100.0
            } else {
                75.0
            }
        } catch (e: Exception) {
            75.0
        }
    }

    /**
     * LANGKAH 1: Grounded AI Insight Composer via Model Router
     */
    suspend fun generateSelectionInsight(
        rankedResult: RankedResult,
        criteria: List<WeightedCriterion>
    ): String = withContext(Dispatchers.IO) {
        val candidateName = rankedResult.result.row?.fields?.values?.firstOrNull() ?: "Kandidat"
        val rowDataStr = rankedResult.result.row?.fields?.entries?.joinToString(", ") { "${it.key}: ${it.value}" } ?: ""
        val criteriaScoreStr = rankedResult.result.scoreBreakdown.joinToString(", ") { (c, s) ->
            val weighted = Math.round(s * (c.weightPercentage / 100.0) * 100.0) / 100.0
            "${c.criterionName} (skor: $s, bobot: ${c.weightPercentage}%, nilai: $weighted)"
        }

        val prompt = """
            You are OrchestreeAI Grounded Selection Insight Composer.
            Generate a concise 1-2 sentence factual explanation in Indonesian for the ranking and score of this candidate.
            
            STRICT GROUNDING & ANTI-HALLUCINATION REQUIREMENT:
            - ONLY reference the candidate's actual scores and attributes provided below.
            - DO NOT invent, assume, or hallucinate facts, years of experience, unmentioned certificates, or metrics.
            
            Candidate Name / ID: $candidateName
            Rank Position: #${rankedResult.rankPosition}
            Total Score: ${rankedResult.result.totalScore}
            Priority Level: ${rankedResult.priorityLevel}
            Recommendation: ${rankedResult.classification}
            Risk Score: ${rankedResult.result.riskScore}
            Confidence Score: ${rankedResult.result.confidenceScore}
            Criteria Breakdown: $criteriaScoreStr
            Source Attributes: $rowDataStr
        """.trimIndent()

        return@withContext try {
            val res = modelRouter.execute(
                ModelRouteRequest(
                    taskCategory = "STRUCTURED_REASONING",
                    prompt = prompt,
                    temperature = 0.2
                )
            )
            if (res.isSuccess) {
                val insight = res.getOrThrow().text.trim()
                if (insight.isNotBlank()) insight else fallbackInsight(rankedResult, candidateName)
            } else {
                fallbackInsight(rankedResult, candidateName)
            }
        } catch (e: Exception) {
            fallbackInsight(rankedResult, candidateName)
        }
    }

    private fun fallbackInsight(rankedResult: RankedResult, candidateName: String): String {
        val topScore = rankedResult.result.scoreBreakdown.maxByOrNull { it.second }
        val topDetail = if (topScore != null) " dengan keunggulan pada ${topScore.first.criterionName} (${topScore.second})" else ""
        return "$candidateName berada di peringkat #${rankedResult.rankPosition} dengan skor total ${rankedResult.result.totalScore} (rekomendasi: ${rankedResult.classification})$topDetail."
    }
}

/**
 * NORMALISASI DATA
 */
class DataNormalizer {
    fun normalize(
        extracted: List<Map<String, String>>,
        fields: List<DetectedField>
    ): List<Map<String, String>> {
        val fieldTypeMap = fields.associate { it.name.lowercase() to it.inferred_type.lowercase() }

        return extracted.map { row ->
            val normalizedRow = mutableMapOf<String, String>()
            row.forEach { (rawKey, rawVal) ->
                val cleanKey = rawKey.trim()
                val cleanVal = rawVal.trim()
                val type = fieldTypeMap[cleanKey.lowercase()] ?: "string"

                val normalizedVal = when (type) {
                    "email" -> cleanVal.lowercase()
                    "phone" -> normalizePhone(cleanVal)
                    "currency", "number" -> normalizeNumeric(cleanVal)
                    "boolean" -> normalizeBoolean(cleanVal)
                    else -> cleanVal
                }
                normalizedRow[cleanKey] = normalizedVal
            }
            normalizedRow
        }
    }

    private fun normalizePhone(value: String): String {
        if (value.isBlank()) return ""
        if (value.any { it.isLetter() }) return value.trim()
        val hasPlus = value.startsWith("+")
        val digits = value.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    private fun normalizeNumeric(value: String): String {
        if (value.isBlank()) return ""
        if (value.any { it.isLetter() }) return value.trim()
        val cleaned = value.replace("Rp", "", ignoreCase = true)
            .replace("$", "")
            .replace("€", "")
            .replace(",", "")
            .trim()
        return cleaned
    }

    private fun normalizeBoolean(value: String): String {
        val lower = value.lowercase().trim()
        return when (lower) {
            "true", "1", "ya", "yes", "valid", "aktif" -> "true"
            "false", "0", "tidak", "no", "invalid", "nonaktif" -> "false"
            else -> value
        }
    }
}

/**
 * DUPLICATE, INVALID DETECTION & COMPLETENESS/QUALITY SCORE
 */
class DataQualityAnalyzer {
    private val emailPattern = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    )

    fun detectDuplicates(rows: List<Map<String, String>>): DuplicateReport {
        if (rows.size <= 1) return DuplicateReport(0, emptyList())

        val duplicateGroups = mutableListOf<DuplicateGroup>()
        val seenSignatures = mutableMapOf<String, Int>()
        val keyBasedSignatures = mutableMapOf<String, MutableList<Int>>()

        rows.forEachIndexed { index, row ->
            // Row signature: all non-empty key-value pairs sorted
            val fullSig = row.entries
                .filter { it.value.isNotBlank() }
                .sortedBy { it.key }
                .joinToString("|") { "${it.key}:${it.value.trim().lowercase()}" }

            if (seenSignatures.containsKey(fullSig)) {
                val primaryIndex = seenSignatures[fullSig]!!
                duplicateGroups.add(
                    DuplicateGroup(
                        primaryRowIndex = primaryIndex,
                        duplicateRowIndices = listOf(index),
                        matchedKeys = row.keys.toList(),
                        similarityScore = 1.0
                    )
                )
            } else {
                seenSignatures[fullSig] = index
            }

            // Also check unique candidate identifier duplicates (e.g. email, nik, phone/telepon, name)
            val identifierKeys = row.keys.filter { k ->
                val kl = k.lowercase()
                kl.contains("email") || kl.contains("nik") || kl.contains("id_kandidat") || kl.contains("candidate_id") || kl.contains("phone") || kl.contains("telepon") || kl.contains("nama")
            }
            for (identifierKey in identifierKeys) {
                val idVal = row[identifierKey]?.trim()?.lowercase() ?: ""
                if (idVal.isNotBlank() && idVal != "-" && idVal != "n/a") {
                    val keySig = "$identifierKey:$idVal"
                    val existing = keyBasedSignatures.getOrPut(keySig) { mutableListOf() }
                    existing.add(index)
                }
            }
        }

        // Add identifier-based duplicates if not already in full duplicate groups
        keyBasedSignatures.forEach { (sig, indices) ->
            if (indices.size > 1) {
                val primary = indices.first()
                val duplicates = indices.drop(1)
                duplicates.forEach { dupIdx ->
                    if (duplicateGroups.none { it.primaryRowIndex == primary && it.duplicateRowIndices.contains(dupIdx) }) {
                        duplicateGroups.add(
                            DuplicateGroup(
                                primaryRowIndex = primary,
                                duplicateRowIndices = listOf(dupIdx),
                                matchedKeys = listOf(sig.substringBefore(":")),
                                similarityScore = 0.95
                            )
                        )
                    }
                }
            }
        }

        return DuplicateReport(
            duplicateCount = duplicateGroups.size,
            duplicateGroups = duplicateGroups
        )
    }

    fun detectInvalidEntries(
        rows: List<Map<String, String>>,
        fields: List<DetectedField>
    ): List<InvalidRowEntry> {
        val invalidEntries = mutableListOf<InvalidRowEntry>()
        val fieldMap = fields.associateBy { it.name.lowercase() }

        rows.forEachIndexed { rowIndex, row ->
            row.forEach { (fieldName, rawValue) ->
                val fieldDef = fieldMap[fieldName.lowercase()]
                val baseType = fieldDef?.inferred_type?.lowercase() ?: "string"
                val fn = fieldName.lowercase()
                val type = when {
                    baseType != "string" -> baseType
                    fn.contains("email") -> "email"
                    fn.contains("phone") || fn.contains("telepon") || fn.contains("telp") || fn.contains("hp") -> "phone"
                    fn.contains("gaji") || fn.contains("salary") || fn.contains("price") || fn.contains("harga") || fn.contains("budget") || fn.contains("biaya") || fn.contains("amount") || fn.contains("kontrak") || fn.contains("pengalaman") -> "number"
                    else -> "string"
                }
                val trimmed = rawValue.trim()

                if (trimmed.isNotBlank() && trimmed != "-" && trimmed != "n/a") {
                    when (type) {
                        "email" -> {
                            if (!emailPattern.matcher(trimmed).matches()) {
                                invalidEntries.add(
                                    InvalidRowEntry(
                                        rowIndex = rowIndex,
                                        fieldName = fieldName,
                                        rawValue = rawValue,
                                        reason = "Format email tidak valid"
                                    )
                                )
                            }
                        }
                        "phone" -> {
                            val digitsOnly = trimmed.filter { it.isDigit() }
                            val hasInvalidChars = trimmed.any { ch -> ch.isLetter() }
                            if (hasInvalidChars || digitsOnly.length < 7 || digitsOnly.length > 16) {
                                invalidEntries.add(
                                    InvalidRowEntry(
                                        rowIndex = rowIndex,
                                        fieldName = fieldName,
                                        rawValue = rawValue,
                                        reason = "Format nomor telepon tidak valid atau mengandung karakter huruf"
                                    )
                                )
                            }
                        }
                        "number", "currency" -> {
                            val cleanNumber = trimmed
                                .replace("Rp", "", ignoreCase = true)
                                .replace("$", "")
                                .replace("€", "")
                                .trim()
                            val parsed = parseNumericValue(cleanNumber)
                            if (parsed == null) {
                                invalidEntries.add(
                                    InvalidRowEntry(
                                        rowIndex = rowIndex,
                                        fieldName = fieldName,
                                        rawValue = rawValue,
                                        reason = "Nilai numerik tidak dapat dikonversi ke angka"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        return invalidEntries
    }

    private fun parseNumericValue(raw: String): Double? {
        val s = raw.replace(" ", "").trim()
        if (s.isEmpty()) return null
        if (s.any { it.isLetter() }) return null
        val dotCount = s.count { it == '.' }
        val commaCount = s.count { it == ',' }
        val candidate = when {
            dotCount > 1 && commaCount == 0 -> s.replace(".", "")
            commaCount > 1 && dotCount == 0 -> s.replace(",", "")
            dotCount == 1 && commaCount == 1 -> {
                if (s.indexOf('.') < s.indexOf(',')) s.replace(".", "").replace(",", ".")
                else s.replace(",", "")
            }
            dotCount == 1 && commaCount == 0 && s.substringAfter('.').length == 3 -> s.replace(".", "")
            commaCount == 1 && dotCount == 0 && s.substringAfter(',').length == 3 -> s.replace(",", "")
            commaCount == 1 && dotCount == 0 -> s.replace(",", ".")
            else -> s
        }
        return candidate.toDoubleOrNull()
    }

    fun calculateCompletenessScore(
        rows: List<Map<String, String>>,
        fields: List<DetectedField>
    ): QualityScoreDetails {
        if (rows.isEmpty()) {
            return QualityScoreDetails(
                overallScore = 0.0,
                completenessScore = 0.0,
                validityScore = 0.0,
                uniquenessScore = 0.0,
                totalRows = 0,
                validRowCount = 0,
                duplicateRowCount = 0,
                invalidRowCount = 0,
                qualityGrade = "POOR"
            )
        }

        val totalExpectedCells = rows.size * fields.size.coerceAtLeast(1)
        var filledCells = 0

        rows.forEach { row ->
            fields.forEach { field ->
                val v = row[field.name]
                if (!v.isNullOrBlank() && v != "-" && v.lowercase() != "null" && v.lowercase() != "n/a") {
                    filledCells++
                }
            }
        }

        val completenessRatio = filledCells.toDouble() / totalExpectedCells.toDouble()
        val completenessScore = (completenessRatio * 100.0).coerceIn(0.0, 100.0)

        // Duplicate calculation
        val dupReport = detectDuplicates(rows)
        val duplicateRowsCount = dupReport.duplicateCount
        val uniquenessRatio = ((rows.size - duplicateRowsCount).coerceAtLeast(0)).toDouble() / rows.size.toDouble()
        val uniquenessScore = (uniquenessRatio * 100.0).coerceIn(0.0, 100.0)

        // Invalid rows calculation
        val invalidEntries = detectInvalidEntries(rows, fields)
        val invalidRowIndices = invalidEntries.map { it.rowIndex }.distinct()
        val invalidRowsCount = invalidRowIndices.size
        val validRowCount = (rows.size - invalidRowsCount).coerceAtLeast(0)
        val validityRatio = validRowCount.toDouble() / rows.size.toDouble()
        val validityScore = (validityRatio * 100.0).coerceIn(0.0, 100.0)

        // Overall weighted score: 40% Completeness, 40% Validity, 20% Uniqueness
        val overallScore = (completenessScore * 0.4) + (validityScore * 0.4) + (uniquenessScore * 0.2)
        val formattedOverall = Math.round(overallScore * 10.0) / 10.0

        val grade = when {
            formattedOverall >= 90.0 -> "EXCELLENT"
            formattedOverall >= 75.0 -> "GOOD"
            formattedOverall >= 60.0 -> "FAIR"
            else -> "POOR"
        }

        return QualityScoreDetails(
            overallScore = formattedOverall,
            completenessScore = Math.round(completenessScore * 10.0) / 10.0,
            validityScore = Math.round(validityScore * 10.0) / 10.0,
            uniquenessScore = Math.round(uniquenessScore * 10.0) / 10.0,
            totalRows = rows.size,
            validRowCount = validRowCount,
            duplicateRowCount = duplicateRowsCount,
            invalidRowCount = invalidRowsCount,
            qualityGrade = grade
        )
    }
}
