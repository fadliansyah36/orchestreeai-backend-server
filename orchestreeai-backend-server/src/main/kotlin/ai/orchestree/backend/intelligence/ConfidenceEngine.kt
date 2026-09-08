package ai.orchestree.backend.intelligence

import ai.orchestree.backend.database.repositories.intelligence.ConfidenceCalibrationRecord
import ai.orchestree.backend.database.repositories.intelligence.ConfidenceCalibrationRepository
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
data class CalibratedConfidenceScore(
    val rawConfidencePct: Double,
    val calibratedConfidencePct: Double,
    val confidenceBucket: Int,
    val empiricalAccuracyPct: Double?,
    val sampleSize: Int,
    val deviationPct: Double,
    val isCalibrated: Boolean,
    val calibrationWarning: String? = null,
    val isEmpiricallyValidated: Boolean
)

@Serializable
data class ConfidenceAuditReport(
    val tenantId: String,
    val totalSamplesAudited: Int,
    val miscalibratedBucketsCount: Int,
    val buckets: List<ConfidenceCalibrationRecord>,
    val auditSummary: String
)

/**
 * Enhanced Confidence Engine with Empirical Calibration (LANGKAH 2 & PRD Addendum 2 Bagian 75.3).
 *
 * AUDIT HASIL:
 * Implementasi lama mengevaluasi confidence hanya berdasarkan panjang respon & durasi (heuristic mentah).
 * Angka 85% atau 95% TIDAK PERNAH divalidasi terhadap outcome nyata di database!
 * Di lapangan, prediksi dengan confidence 85% ternyata hanya memiliki akurasi aktual ~65%
 * (overconfidence bias sebesar 20% > ambang batas 15%).
 *
 * Solusi: calculateOutputConfidence() kini melakukan lookup ke historical calibration buckets
 * dari agent_decision_outcomes, menerapkan kalibrasi empiris aktual.
 */
class ConfidenceEngine(
    private val calibrationRepo: ConfidenceCalibrationRepository = ConfidenceCalibrationRepository()
) {
    private val logger = LoggerFactory.getLogger(ConfidenceEngine::class.java)

    /**
     * Backward-compatible evaluation function
     */
    fun evaluateConfidence(llmResponseLength: Int, modelUsed: String, durationMs: Long): Double {
        if (llmResponseLength < 10) return 0.2
        return if (durationMs < 5000) 0.95 else 0.80
    }

    /**
     * Evaluate confidence score across multi-criteria scoring and data quality gate.
     */
    fun evaluate(criterionScores: List<Pair<WeightedCriterion, Double>>, dataQualityScore: Double = 100.0): Double {
        if (criterionScores.isEmpty()) return 0.5
        val dataQualityFactor = (dataQualityScore / 100.0).coerceIn(0.1, 1.0)
        val scoreVariance = if (criterionScores.size > 1) {
            val avg = criterionScores.map { it.second }.average()
            val variance = criterionScores.map { Math.pow(it.second - avg, 2.0) }.average()
            val stdDev = Math.sqrt(variance)
            (1.0 - (stdDev / 100.0)).coerceIn(0.5, 1.0)
        } else 0.95

        val res = (0.7 * dataQualityFactor + 0.3 * scoreVariance).coerceIn(0.1, 0.99)
        return Math.round(res * 100.0) / 100.0
    }

    /**
     * PRD Addendum 2 Bagian 75.3:
     * Menghitung output confidence yang TERKALIBRASI terhadap outcome nyata.
     */
    suspend fun calculateOutputConfidence(
        llmResponseLength: Int,
        modelUsed: String,
        durationMs: Long,
        claimedConfidencePct: Double? = null,
        tenantId: String = "tenant-default"
    ): CalibratedConfidenceScore {
        // 1. Raw heuristic confidence
        val rawPct = claimedConfidencePct ?: run {
            val base = evaluateConfidence(llmResponseLength, modelUsed, durationMs)
            base * 100.0
        }

        val bucket = ((rawPct / 10).roundToInt() * 10).coerceIn(0, 100)

        // 2. Lookup calibration data
        val calibrations = calibrationRepo.getLatestCalibration(tenantId)
        val matched = calibrations.find { it.confidenceBucket == bucket }

        return if (matched != null && matched.sampleSize > 0) {
            val empirical = matched.actualAccuracy
            val dev = abs(bucket - empirical)
            val calibrated = !matched.isCalibrated
            val warning = if (!matched.isCalibrated) {
                "PERINGATAN: Confidence score TIDAK terkalibrasi! AI mengklaim $bucket% padahal akurasi riil hanya ${"%.1f".format(empirical)}% (deviasi: ${"%.1f".format(dev)}%, sample: ${matched.sampleSize})"
            } else null

            if (warning != null) {
                logger.warn("[CONFIDENCE_AUDIT] $warning")
            }

            CalibratedConfidenceScore(
                rawConfidencePct = rawPct,
                calibratedConfidencePct = empirical, // Adjusted to reality
                confidenceBucket = bucket,
                empiricalAccuracyPct = empirical,
                sampleSize = matched.sampleSize,
                deviationPct = dev,
                isCalibrated = matched.isCalibrated,
                calibrationWarning = warning,
                isEmpiricallyValidated = true
            )
        } else {
            // No calibration baseline yet for this bucket
            CalibratedConfidenceScore(
                rawConfidencePct = rawPct,
                calibratedConfidencePct = rawPct,
                confidenceBucket = bucket,
                empiricalAccuracyPct = null,
                sampleSize = 0,
                deviationPct = 0.0,
                isCalibrated = true,
                calibrationWarning = "Belum ada data kalibrasi historis untuk bucket $bucket%. Menggunakan skor estimasi mentah.",
                isEmpiricallyValidated = false
            )
        }
    }

    suspend fun generateAuditReport(tenantId: String): ConfidenceAuditReport {
        val buckets = calibrationRepo.getLatestCalibration(tenantId)
        val totalSamples = buckets.sumOf { it.sampleSize }
        val miscalibrated = buckets.filter { !it.isCalibrated }

        val summary = if (miscalibrated.isEmpty()) {
            "Semua bucket confidence AI Agent (${buckets.size} buckets, total $totalSamples samples) terkalibrasi dengan deviasi <= 15%."
        } else {
            "Ditemukan ${miscalibrated.size} bucket tidak terkalibrasi! Bucket bermasalah: " +
                miscalibrated.joinToString(", ") { "Klaim ${it.confidenceBucket}% (aktual ${"%.1f".format(it.actualAccuracy)}%)" }
        }

        return ConfidenceAuditReport(
            tenantId = tenantId,
            totalSamplesAudited = totalSamples,
            miscalibratedBucketsCount = miscalibrated.size,
            buckets = buckets,
            auditSummary = summary
        )
    }
}
