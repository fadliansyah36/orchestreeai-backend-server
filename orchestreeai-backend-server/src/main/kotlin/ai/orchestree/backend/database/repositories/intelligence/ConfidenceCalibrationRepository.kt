package ai.orchestree.backend.database.repositories.intelligence

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.orchestration.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

@Serializable
data class ConfidenceCalibrationRecord(
    val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val confidenceBucket: Int, // 0, 10, 20, 30, ..., 90, 100
    val claimedConfidence: Double,
    val actualAccuracy: Double,
    val sampleSize: Int,
    val deviation: Double,
    val isCalibrated: Boolean,
    val calibrationDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Repository for Confidence Calibration Records (LANGKAH 2).
 * Stores empirical calibration curves mapping AI predicted confidence to actual real-world accuracy.
 */
class ConfidenceCalibrationRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(ConfidenceCalibrationRepository::class.java)
    private val inMemoryStore = ConcurrentHashMap<String, CopyOnWriteArrayList<ConfidenceCalibrationRecord>>()

    suspend fun record(
        tenantId: String,
        bucket: Int,
        actualAccuracy: Double,
        sampleSize: Int
    ): ConfidenceCalibrationRecord = withContext(Dispatchers.IO) {
        val deviation = abs(bucket.toDouble() - actualAccuracy)
        val isCalibrated = deviation <= 15.0
        val record = ConfidenceCalibrationRecord(
            id = "calib-${UUID.randomUUID().toString().take(8)}",
            tenantId = tenantId,
            confidenceBucket = bucket,
            claimedConfidence = bucket.toDouble(),
            actualAccuracy = actualAccuracy,
            sampleSize = sampleSize,
            deviation = deviation,
            isCalibrated = isCalibrated,
            calibrationDate = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )

        inMemoryStore.computeIfAbsent(tenantId) { CopyOnWriteArrayList() }.add(record)

        if (supabase.isConfigured()) {
            try {
                val payload = mapOf(
                    "id" to record.id,
                    "tenant_id" to record.tenantId,
                    "confidence_bucket" to record.confidenceBucket,
                    "claimed_confidence" to record.claimedConfidence,
                    "actual_accuracy" to record.actualAccuracy,
                    "sample_size" to record.sampleSize,
                    "deviation" to record.deviation,
                    "is_calibrated" to record.isCalibrated,
                    "created_at" to record.createdAt
                )
                supabase.insertRecord("confidence_calibrations", tenantId, payload.toJson())
            } catch (e: Exception) {
                logger.debug("Supabase insert calibration notice: ${e.message}")
            }
        }
        record
    }

    suspend fun getLatestCalibration(tenantId: String): List<ConfidenceCalibrationRecord> = withContext(Dispatchers.IO) {
        val records = inMemoryStore[tenantId] ?: emptyList()
        records.groupBy { it.confidenceBucket }
            .mapValues { (_, list) -> list.maxByOrNull { it.calibrationDate }!! }
            .values
            .sortedBy { it.confidenceBucket }
    }

    suspend fun getAllCalibrations(tenantId: String): List<ConfidenceCalibrationRecord> = withContext(Dispatchers.IO) {
        inMemoryStore[tenantId]?.toList() ?: emptyList()
    }

    fun clear() {
        inMemoryStore.clear()
    }
}
