package ai.orchestree.backend.api

import ai.orchestree.backend.billing.DatabaseManager
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

            // 1. Check Geofence from real work_locations
            val locations = mutableListOf<ai.orchestree.backend.attendance.WorkLocation>()
            val conn = DatabaseManager.getConnection()
            if (conn != null) {
                conn.use { c ->
                    try {
                        c.prepareStatement("SELECT id, name, latitude, longitude, radius_meters FROM work_locations WHERE tenant_id = ?").use { ps ->
                            ps.setString(1, tenantId)
                            ps.executeQuery().use { rs ->
                                while (rs.next()) {
                                    locations.add(
                                        ai.orchestree.backend.attendance.WorkLocation(
                                            id = rs.getString("id"),
                                            tenantId = tenantId,
                                            name = rs.getString("name"),
                                            latitude = rs.getDouble("latitude"),
                                            longitude = rs.getDouble("longitude"),
                                            radiusMeters = rs.getDouble("radius_meters")
                                        )
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            val geoResult = ai.orchestree.backend.attendance.GeofenceEngine.evaluateLocation(
                deviceLat = req.latitude,
                deviceLon = req.longitude,
                locations = locations
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
            val conn = DatabaseManager.getConnection()
            val list = mutableListOf<AttendanceRecordItem>()
            if (conn != null) {
                conn.use { c ->
                    try {
                        c.prepareStatement(
                            """
                            SELECT id, user_id, tenant_id, created_at, status, check_in_time
                            FROM attendance_records
                            WHERE (user_id = ? OR ? = 'user-default') AND (tenant_id = ? OR tenant_id = 'tenant-default')
                            ORDER BY created_at DESC LIMIT 50
                            """.trimIndent()
                        ).use { ps ->
                            ps.setString(1, userId)
                            ps.setString(2, userId)
                            ps.setString(3, tenantId)
                            ps.executeQuery().use { rs ->
                                while (rs.next()) {
                                    list.add(
                                        AttendanceRecordItem(
                                            id = rs.getString("id"),
                                            userId = rs.getString("user_id") ?: userId,
                                            tenantId = rs.getString("tenant_id") ?: tenantId,
                                            timestamp = rs.getLong("check_in_time").takeIf { it > 0 } ?: rs.getLong("created_at"),
                                            type = "CHECK_IN",
                                            locationName = "Geofence Verified",
                                            verificationStatus = rs.getString("status") ?: "VERIFIED"
                                        )
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            call.respond(HttpStatusCode.OK, list)
        }
    }

    route("/tenants/{id}/geofences") {
        get {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val conn = DatabaseManager.getConnection()
            val list = mutableListOf<GeofenceItem>()
            if (conn != null) {
                conn.use { c ->
                    try {
                        c.prepareStatement(
                            "SELECT id, tenant_id, name, latitude, longitude, radius_meters, is_active FROM work_locations WHERE tenant_id = ? OR tenant_id = 'tenant-default'"
                        ).use { ps ->
                            ps.setString(1, tenantId)
                            ps.executeQuery().use { rs ->
                                while (rs.next()) {
                                    list.add(
                                        GeofenceItem(
                                            id = rs.getString("id"),
                                            tenantId = rs.getString("tenant_id") ?: tenantId,
                                            name = rs.getString("name") ?: "",
                                            latitude = rs.getDouble("latitude"),
                                            longitude = rs.getDouble("longitude"),
                                            radiusMeters = rs.getDouble("radius_meters"),
                                            isActive = rs.getBoolean("is_active")
                                        )
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            call.respond(HttpStatusCode.OK, list)
        }

        post {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<GeofenceCreateRequest>()
            val newId = "geo-${java.util.UUID.randomUUID().toString().take(8)}"
            val conn = DatabaseManager.getConnection()
            if (conn != null) {
                conn.use { c ->
                    try {
                        c.prepareStatement(
                            """
                            INSERT INTO work_locations (id, tenant_id, name, latitude, longitude, radius_meters, is_active, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, true, ?)
                            """.trimIndent()
                        ).use { ps ->
                            ps.setString(1, newId)
                            ps.setString(2, tenantId)
                            ps.setString(3, req.name)
                            ps.setDouble(4, req.latitude)
                            ps.setDouble(5, req.longitude)
                            ps.setDouble(6, req.radiusMeters)
                            ps.setLong(7, System.currentTimeMillis())
                            ps.executeUpdate()
                        }
                    } catch (_: Exception) {}
                }
            }
            call.respond(
                HttpStatusCode.Created,
                GeofenceItem(
                    id = newId,
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
