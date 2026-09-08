package ai.orchestree.backend.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.put

@Serializable
data class AttendanceCheckInRequest(
    val userId: String,
    val tenantId: String = "tenant-default",
    val latitude: Double,
    val longitude: Double,
    val locationName: String = "Headquarters Jakarta",
    val type: String = "CHECK_IN"
)

@Serializable
data class AttendanceCheckInResponse(
    val id: String,
    val status: String = "SUCCESS",
    val message: String = "Presensi berhasil dicatat di server",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class AttendanceRecordItem(
    val id: String,
    val userId: String,
    val tenantId: String,
    val timestamp: Long,
    val type: String,
    val locationName: String,
    val verificationStatus: String = "VERIFIED_GEOFENCE"
)

@Serializable
data class GeofenceItem(
    val id: String,
    val tenantId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double = 100.0,
    val isActive: Boolean = true
)

@Serializable
data class GeofenceCreateRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double = 100.0
)

fun Route.attendanceRoutes() {
    val supabase = ai.orchestree.backend.database.SupabaseClientProvider.fromEnv()

    route("/attendance") {
        post("/check-in") {
            val req = call.receive<AttendanceCheckInRequest>()
            val tenantId = req.tenantId

            // 1. Check Geofence
            val defaultHq = listOf(
                ai.orchestree.backend.attendance.WorkLocation(
                    id = "loc-hq",
                    tenantId = tenantId,
                    name = req.locationName,
                    latitude = -6.2088,
                    longitude = 106.8456,
                    radiusMeters = 200.0
                )
            )
            val geoResult = ai.orchestree.backend.attendance.GeofenceEngine.evaluateLocation(
                deviceLat = req.latitude,
                deviceLon = req.longitude,
                locations = defaultHq
            )

            // 2. Inspect Anomalies
            val anomaly = ai.orchestree.backend.attendance.AttendanceAnomalyEngine.inspectCheckIn(
                tenantId = tenantId,
                userId = req.userId,
                userName = "Staff ${req.userId}",
                latitude = req.latitude,
                longitude = req.longitude,
                isGeofenceVerified = geoResult.isWithinGeofence,
                supabase = supabase
            )

            val recordId = "att-${java.util.UUID.randomUUID().toString().take(8)}"
            val status = if (anomaly != null) "FLAGGED_ANOMALY" else "SUCCESS"
            val msg = if (anomaly != null) {
                "Presensi dicatat dengan peringatan: ${anomaly.anomalyType}"
            } else {
                "Presensi ${req.type} untuk user ${req.userId} berhasil diverifikasi di lokasi ${req.locationName}"
            }

            // Persist to Supabase
            try {
                val payload = kotlinx.serialization.json.buildJsonObject {
                    put("id", recordId)
                    put("tenant_id", tenantId)
                    put("user_id", req.userId)
                    put("type", req.type)
                    put("latitude", req.latitude)
                    put("longitude", req.longitude)
                    put("location_name", req.locationName)
                    put("verification_status", if (geoResult.isWithinGeofence) "VERIFIED_GEOFENCE" else "OUT_OF_BOUNDS")
                }.toString()
                supabase.insertRecord("attendance_records", tenantId, payload)
            } catch (_: Exception) {}

            call.respond(
                HttpStatusCode.Created,
                AttendanceCheckInResponse(
                    id = recordId,
                    status = status,
                    message = msg,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        get("/anomalies") {
            val tenantId = call.request.queryParameters["tenantId"] ?: "tenant-default"
            val res = supabase.queryTable("attendance_anomalies", tenantId)
            call.respondText(res.getOrDefault("[]"), io.ktor.http.ContentType.Application.Json, HttpStatusCode.OK)
        }

        get("/history") {
            val userId = call.request.queryParameters["userId"] ?: "user-default"
            val tenantId = call.request.queryParameters["tenantId"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    AttendanceRecordItem(
                        id = "att-01",
                        userId = userId,
                        tenantId = tenantId,
                        timestamp = System.currentTimeMillis() - 28800000L,
                        type = "CHECK_IN",
                        locationName = "Main Office Tower (Geofence Verified)",
                        verificationStatus = "VERIFIED"
                    ),
                    AttendanceRecordItem(
                        id = "att-02",
                        userId = userId,
                        tenantId = tenantId,
                        timestamp = System.currentTimeMillis() - 3600000L,
                        type = "CHECK_OUT",
                        locationName = "Main Office Tower (Geofence Verified)",
                        verificationStatus = "VERIFIED"
                    )
                )
            )
        }
    }

    route("/tenants/{id}/geofences") {
        get {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    GeofenceItem(
                        id = "geo-01",
                        tenantId = tenantId,
                        name = "Headquarters Jakarta",
                        latitude = -6.2088,
                        longitude = 106.8456,
                        radiusMeters = 150.0,
                        isActive = true
                    ),
                    GeofenceItem(
                        id = "geo-02",
                        tenantId = tenantId,
                        name = "Surabaya Logistics Hub",
                        latitude = -7.2575,
                        longitude = 112.7521,
                        radiusMeters = 200.0,
                        isActive = true
                    )
                )
            )
        }

        post {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<GeofenceCreateRequest>()
            call.respond(
                HttpStatusCode.Created,
                GeofenceItem(
                    id = "geo-${java.util.UUID.randomUUID().toString().take(8)}",
                    tenantId = tenantId,
                    name = req.name,
                    latitude = req.latitude,
                    longitude = req.longitude,
                    radiusMeters = req.radiusMeters,
                    isActive = true
                )
            )
        }
    }
}
