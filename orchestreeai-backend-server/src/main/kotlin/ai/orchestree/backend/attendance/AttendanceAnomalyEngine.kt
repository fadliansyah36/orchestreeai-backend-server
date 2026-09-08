package ai.orchestree.backend.attendance

import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class AttendanceAnomalyRecord(
    val id: String,
    val tenantId: String,
    val userId: String,
    val userName: String,
    val anomalyType: String, // 'FACE_MISMATCH', 'GEOFENCE_VIOLATION', 'MOCK_LOCATION', 'IMPOSSIBLE_TRAVEL' // allowed: anti-spoofing
    val severity: String, // 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    val evidenceDetails: String,
    val status: String = "OPEN",
    val detectedAt: Long = System.currentTimeMillis()
)

object AttendanceAnomalyEngine {
    private val logger = LoggerFactory.getLogger(AttendanceAnomalyEngine::class.java)

    /**
     * Inspects a check-in record for GPS spoofing, geofence mismatch, or face verification failure.
     */
    suspend fun inspectCheckIn(
        tenantId: String,
        userId: String,
        userName: String,
        latitude: Double,
        longitude: Double,
        isGeofenceVerified: Boolean,
        isFaceMatched: Boolean = true,
        isMockLocation: Boolean = false, // allowed: anti-spoofing
        supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
    ): AttendanceAnomalyRecord? = withContext(Dispatchers.IO) {
        var anomalyType: String? = null
        var severity = "MEDIUM"
        var evidence = ""

        if (isMockLocation) { // allowed: anti-spoofing
            anomalyType = "MOCK_LOCATION_DETECTED" // allowed: anti-spoofing
            severity = "CRITICAL"
            evidence = "Mock location provider terdeteksi pada device user." // allowed: anti-spoofing
        } else if (!isFaceMatched) {
            anomalyType = "FACE_VERIFICATION_MISMATCH"
            severity = "CRITICAL"
            evidence = "Verifikasi wajah biometrik gagal atau tidak cocok dengan template terdaftar."
        } else if (!isGeofenceVerified) {
            anomalyType = "GEOFENCE_VIOLATION"
            severity = "HIGH"
            evidence = "Check-in berada di luar radius lokasi kantor yang sah (Lat: $latitude, Lon: $longitude)."
        }

        if (anomalyType == null) return@withContext null

        val anomaly = AttendanceAnomalyRecord(
            id = "anom-${UUID.randomUUID().toString().take(8)}",
            tenantId = tenantId,
            userId = userId,
            userName = userName,
            anomalyType = anomalyType,
            severity = severity,
            evidenceDetails = evidence
        )

        try {
            val payload = buildJsonObject {
                put("id", anomaly.id)
                put("tenant_id", tenantId)
                put("user_id", userId)
                put("user_name", userName)
                put("anomaly_type", anomalyType)
                put("severity", severity)
                put("evidence_details", evidence)
                put("status", "OPEN")
            }.toString()
            supabase.insertRecord("attendance_anomalies", tenantId, payload)
            logger.warn("Recorded attendance anomaly $anomalyType for user $userId in tenant $tenantId")
        } catch (e: Exception) {
            logger.error("Failed persisting attendance anomaly: ${e.message}", e)
        }

        anomaly
    }
}
