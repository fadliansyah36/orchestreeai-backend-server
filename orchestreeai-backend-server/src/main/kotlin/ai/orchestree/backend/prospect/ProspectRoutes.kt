package ai.orchestree.backend.prospect

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

object ProspectRateLimiter {
    private val submissionTimestamps = ConcurrentHashMap<String, MutableList<Long>>()
    private const val MAX_SUBMISSIONS_PER_DAY = 5
    private const val WINDOW_MILLIS = 24 * 3600 * 1000L

    fun isAllowed(ip: String): Boolean {
        val now = System.currentTimeMillis()
        val list = submissionTimestamps.computeIfAbsent(ip) { mutableListOf() }
        synchronized(list) {
            list.removeIf { now - it > WINDOW_MILLIS }
            if (list.size >= MAX_SUBMISSIONS_PER_DAY) {
                return false
            }
            list.add(now)
            return true
        }
    }
}

fun Route.prospectPublicRoutes(repo: ProspectRepository = ProspectRepository()) {
    val logger = LoggerFactory.getLogger("ProspectPublicRoutes")
    val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    val phoneRegex = Regex("^(\\+62|62|0)[0-9]{8,15}$")

    route("/public") {
        // POST /api/v1/public/prospect-registration
        post("/prospect-registration") {
            val clientIp = call.request.origin.remoteHost
            if (!ProspectRateLimiter.isAllowed(clientIp)) {
                logger.warn("Rate limit exceeded for IP: $clientIp")
                call.respond(
                    HttpStatusCode.TooManyRequests,
                    mapOf("error" to "Batas pengajuan per IP telah tercapai (maksimal 5 submission/hari). Silakan coba lagi nanti.")
                )
                return@post
            }

            val req = try {
                call.receive<ProspectRegistrationRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Format payload tidak valid: ${e.message}"))
                return@post
            }

            // Server-side validations
            if (req.fullName.isBlank() || req.fullName.length < 2) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Nama lengkap wajib diisi minimal 2 karakter."))
                return@post
            }

            if (!emailRegex.matches(req.email.trim())) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Format alamat email tidak valid."))
                return@post
            }

            val cleanPhone = req.phoneNumber.trim().replace(Regex("[\\s-]"), "")
            if (!phoneRegex.matches(cleanPhone)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Format nomor telepon/WhatsApp tidak valid (gunakan format Indonesia)."))
                return@post
            }

            if (req.companyName.isBlank() || req.companyName.length < 2) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Nama perusahaan wajib diisi minimal 2 karakter."))
                return@post
            }

            if (req.jobTitle.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Jabatan / Posisi wajib diisi."))
                return@post
            }

            val validOptions = setOf("schedule_meeting_presentation", "direct_trial_or_subscription")
            if (!validOptions.contains(req.interestOption)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Opsi pilihan tidak valid."))
                return@post
            }

            try {
                val created = repo.insertProspect(req, clientIp)
                logger.info("Prospect registration created successfully: ${created.id} for ${created.companyName}")
                call.respond(
                    HttpStatusCode.Created,
                    ProspectRegistrationResponse(
                        success = true,
                        data = created,
                        confirmationMessage = "Terima kasih! Tim kami akan menghubungi Anda segera. Khusus 36 slot Trial 7 Hari akan dipilih oleh tim OrchestreeAI berdasarkan kesesuaian kebutuhan bisnis Anda."
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to save prospect registration: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Gagal menyimpan pendaftaran: ${e.message}"))
            }
        }
    }
}

fun Route.prospectAdminRoutes(repo: ProspectRepository = ProspectRepository()) {
    val logger = LoggerFactory.getLogger("ProspectAdminRoutes")

    route("/admin/prospect-registrations") {
        // GET /api/v1/admin/prospect-registrations
        get {
            val (authorized, _) = enforceProspectSuperAdmin(call)
            if (!authorized) return@get

            val search = call.request.queryParameters["search"]
            val interestOption = call.request.queryParameters["interest_option"]
            val trialStatus = call.request.queryParameters["trial_status"]
            val meetingStatus = call.request.queryParameters["meeting_status"]

            try {
                val list = repo.getProspects(search, interestOption, trialStatus, meetingStatus)
                call.respond(HttpStatusCode.OK, list)
            } catch (e: Exception) {
                logger.error("Failed to fetch prospects: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to fetch prospects")))
            }
        }

        // GET /api/v1/admin/prospect-registrations/analytics
        get("/analytics") {
            val (authorized, _) = enforceProspectSuperAdmin(call)
            if (!authorized) return@get

            try {
                val analytics = repo.getAnalytics()
                call.respond(HttpStatusCode.OK, analytics)
            } catch (e: Exception) {
                logger.error("Failed to fetch prospect analytics: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to fetch analytics")))
            }
        }

        // PATCH /api/v1/admin/prospect-registrations/{id}/select-trial
        patch("/{id}/select-trial") {
            val (authorized, adminId) = enforceProspectSuperAdmin(call)
            if (!authorized) return@patch

            val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest, "Missing prospect ID")
            val body = try { call.receive<SelectTrialRequest>() } catch (_: Exception) { SelectTrialRequest() }

            try {
                val updated = repo.selectTrial(id, body.status, adminId, body.adminNotes)
                call.respond(HttpStatusCode.OK, updated)
            } catch (e: Exception) {
                logger.error("Failed to update trial selection for prospect $id: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Update failed")))
            }
        }

        // PATCH /api/v1/admin/prospect-registrations/{id}/schedule-meeting
        patch("/{id}/schedule-meeting") {
            val (authorized, adminId) = enforceProspectSuperAdmin(call)
            if (!authorized) return@patch

            val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest, "Missing prospect ID")
            val req = call.receive<ScheduleMeetingRequest>()

            try {
                val updated = repo.scheduleMeeting(id, req.meetingScheduledAt, req.meetingStatus, req.adminNotes, adminId)
                call.respond(HttpStatusCode.OK, updated)
            } catch (e: Exception) {
                logger.error("Failed to schedule meeting for prospect $id: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Update failed")))
            }
        }

        // POST /api/v1/admin/prospect-registrations/{id}/activate-trial
        post("/{id}/activate-trial") {
            val (authorized, adminId) = enforceProspectSuperAdmin(call)
            if (!authorized) return@post

            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing prospect ID")

            try {
                val response = repo.activateTrialTenant(id, adminId)
                call.respond(HttpStatusCode.Created, response)
            } catch (e: Exception) {
                logger.error("Failed to activate trial tenant for prospect $id: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Activation failed")))
            }
        }
    }
}

private suspend fun enforceProspectSuperAdmin(call: io.ktor.server.application.ApplicationCall): Pair<Boolean, String> {
    val principal = call.principal<JWTPrincipal>()
    if (principal == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Autentikasi dibutuhkan"))
        return Pair(false, "")
    }
    val role = principal.payload.getClaim("role")?.asString()
    if (role != "SUPER_ADMIN") {
        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Akses dibatasi hanya untuk SUPER_ADMIN"))
        return Pair(false, "")
    }
    val adminId = principal.payload.getClaim("user_id")?.asString() ?: principal.payload.subject ?: "admin"
    return Pair(true, adminId)
}
