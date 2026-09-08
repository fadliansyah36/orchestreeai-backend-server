package ai.orchestree.backend.intelligence

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * DataRow represents a single structured row in an evaluation dataset.
 */
@Serializable
data class DataRow(
    val id: String = UUID.randomUUID().toString(),
    val fields: Map<String, String> = emptyMap(),
    val qualityScore: Double = 100.0
) {
    fun getField(fieldName: String): Any? {
        val raw = fields[fieldName] ?: return null
        if (raw.equals("true", ignoreCase = true)) return true
        if (raw.equals("false", ignoreCase = true)) return false
        val clean = raw.replace(",", ".").replace("Rp", "").replace("$", "").replace("%", "").trim()
        val num = clean.toDoubleOrNull()
        if (num != null) return num
        return raw
    }
}

/**
 * Result of scoring a single row across all configured criteria.
 */
@Serializable
data class ScoringResult(
    val totalScore: Double,
    val riskScore: Double,
    val confidenceScore: Double,
    val scoreBreakdown: List<Pair<WeightedCriterion, Double>> = emptyList(),
    val row: DataRow? = null
)

/**
 * Result of ranking and classifying a scored candidate row.
 */
@Serializable
data class RankedResult(
    val rankPosition: Int,
    val priorityLevel: String, // 'high' / 'medium' / 'low'
    val classification: String, // 'selected' / 'review' / 'rejected'
    val result: ScoringResult,
    val aiInsightText: String? = null
)

/**
 * Normalization helper for numeric criteria values.
 */
fun normalizeNumericScore(value: Any?, direction: String? = "higher_is_better"): Double {
    if (value == null) return 50.0
    val num = when (value) {
        is Number -> value.toDouble()
        is String -> value.replace(",", ".").replace("Rp", "").replace("$", "").replace("%", "").trim().toDoubleOrNull() ?: 50.0
        else -> 50.0
    }
    val isLowerBetter = direction?.lowercase()?.let {
        it.contains("lower") || it.contains("min") || it.contains("cost") || it.contains("price")
    } ?: false

    val base = if (num in 0.0..100.0) {
        num
    } else if (num > 100.0) {
        100.0
    } else {
        0.0
    }

    val finalScore = if (isLowerBetter) {
        (100.0 - base).coerceIn(0.0, 100.0)
    } else {
        base.coerceIn(0.0, 100.0)
    }
    return Math.round(finalScore * 100.0) / 100.0
}
