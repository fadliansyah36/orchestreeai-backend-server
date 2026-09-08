package ai.orchestree.backend.attendance

import kotlin.math.*

data class WorkLocation(
    val id: String,
    val tenantId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)

/**
 * Workforce Intelligence: Real Geofence & GPS Engine
 * Accurately computes great-circle distance between device GPS coordinate
 * and designated company work locations / geofence zones via Haversine formula.
 */
object GeofenceEngine {

    private const val EARTH_RADIUS_METERS = 6371000.0 // Earth mean radius in meters

    data class GeofenceCheckResult(
        val isWithinGeofence: Boolean,
        val distanceMeters: Double,
        val allowedRadiusMeters: Double,
        val matchedLocation: WorkLocation?,
        val message: String
    )

    /**
     * Calculates distance in meters between two GPS coordinates using the Haversine formula:
     * a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
     * c = 2 * atan2(√a, √(1−a))
     * d = R * c
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val originLat = Math.toRadians(lat1)
        val targetLat = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) +
                cos(originLat) * cos(targetLat) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Evaluates whether device GPS coordinate falls within any assigned work locations.
     */
    fun evaluateLocation(
        deviceLat: Double,
        deviceLon: Double,
        locations: List<WorkLocation>
    ): GeofenceCheckResult {
        if (locations.isEmpty()) {
            return GeofenceCheckResult(
                isWithinGeofence = false,
                distanceMeters = 0.0,
                allowedRadiusMeters = 0.0,
                matchedLocation = null,
                message = "Tidak ada lokasi kerja resmi terdaftar untuk tenant ini."
            )
        }

        var closestLocation: WorkLocation = locations.first()
        var minDistance = Double.MAX_VALUE

        for (loc in locations) {
            val dist = calculateDistanceMeters(deviceLat, deviceLon, loc.latitude, loc.longitude)
            if (dist < minDistance) {
                minDistance = dist
                closestLocation = loc
            }
        }

        val isWithin = minDistance <= closestLocation.radiusMeters

        val message = if (isWithin) {
            "Berada di dalam area ${closestLocation.name} (Jarak: ${minDistance.toInt()}m, Radius: ${closestLocation.radiusMeters.toInt()}m)."
        } else {
            "Di luar radius geofence ${closestLocation.name} (Jarak Anda: ${minDistance.toInt()}m, Maksimum Diizinkan: ${closestLocation.radiusMeters.toInt()}m)."
        }

        return GeofenceCheckResult(
            isWithinGeofence = isWithin,
            distanceMeters = minDistance,
            allowedRadiusMeters = closestLocation.radiusMeters,
            matchedLocation = closestLocation,
            message = message
        )
    }

    fun formatCoordinates(lat: Double, lon: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return String.format("%.6f° %s, %.6f° %s", abs(lat), latDir, abs(lon), lonDir)
    }
}
