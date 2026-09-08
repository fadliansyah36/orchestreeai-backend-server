package ai.orchestree.backend.scheduler.jobs

import ai.orchestree.backend.database.repositories.intelligence.AgentDecisionOutcomeRepository
import ai.orchestree.backend.database.repositories.intelligence.ConfidenceCalibrationRecord
import ai.orchestree.backend.database.repositories.intelligence.ConfidenceCalibrationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import kotlin.math.abs

/**
 * Confidence Calibration Job (LANGKAH 2.2 & Addendum 2 Bagian 75.3).
 * Runs periodically (weekly) or on-demand:
 * Aggregates actual historical agent decisions, computes empirical accuracy per 10% confidence bucket,
 * records calibration metrics, and warns on significant miscalibrations (> 15% deviation).
 */
class ConfidenceCalibrationJob(
    private val agentDecisionOutcomeRepo: AgentDecisionOutcomeRepository = AgentDecisionOutcomeRepository(),
    private val confidenceCalibrationRepo: ConfidenceCalibrationRepository = ConfidenceCalibrationRepository()
) {
    private val log = LoggerFactory.getLogger(ConfidenceCalibrationJob::class.java)

    suspend fun calibrateConfidenceScores(tenantId: String): List<ConfidenceCalibrationRecord> = withContext(Dispatchers.IO) {
        val predictions = agentDecisionOutcomeRepo.getWithConfidenceLastMonth(tenantId)
        if (predictions.isEmpty()) {
            log.info("Tidak ada data prediksi pada bulan terakhir untuk tenant $tenantId")
            return@withContext emptyList()
        }

        val buckets = predictions.groupBy { ((it.confidence / 10).toInt() * 10).coerceIn(0, 100) } // bucket per 10%
        val recordedCalibrations = mutableListOf<ConfidenceCalibrationRecord>()

        buckets.toSortedMap().forEach { (bucket, items) ->
            val actualAccuracy = items.count { it.outcome == "REINFORCE" }.toDouble() / items.size * 100
            val record = confidenceCalibrationRepo.record(tenantId, bucket, actualAccuracy, sampleSize = items.size)
            recordedCalibrations.add(record)

            if (abs(bucket - actualAccuracy) > 15) { // deviasi signifikan
                log.warn("Confidence score TIDAK terkalibrasi: klaim $bucket%, aktual $actualAccuracy%")
            }
        }
        recordedCalibrations
    }
}
