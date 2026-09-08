package ai.orchestree.backend.api

import ai.orchestree.backend.models.PresenceEnrollRequest
import ai.orchestree.backend.models.PresenceVerifyRequest
import ai.orchestree.backend.security.PresenceService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory

fun Route.presenceRoutes(
    presenceService: PresenceService = PresenceService.defaultInstance
) {
    val logger = LoggerFactory.getLogger("PresenceRoutes")

    route("/presence") {
        /**
         * 2.1. POST /api/v1/presence/enroll
         * Enrolls face embedding (encrypted server-side) or registers device_id for fingerprint.
         * Raw biometric fingerprint data is NEVER accepted or stored.
         */
        post("/enroll") {
            try {
                val req = call.receive<PresenceEnrollRequest>()
                val authenticatedUserId = call.principal<JWTPrincipal>()?.payload?.subject
                val targetUserId = if (req.userId.isNotBlank()) req.userId else authenticatedUserId

                if (targetUserId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                    return@post
                }

                val response = presenceService.enroll(req.copy(userId = targetUserId))
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                logger.error("Failed to process presence enrollment: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Internal server error")))
            }
        }

        /**
         * 2.2. GET /api/v1/presence/requirement-check
         * Evaluates presence requirements across 6 state scenarios:
         * - NOT_REQUIRED
         * - REQUIRED_LOGIN_CHECKIN (anomaly/new device OR no checkin today)
         * - REQUIRED_CHECKOUT (shift end reached & not checked out yet)
         * - ALREADY_CHECKED_IN_TODAY (shift ongoing OR checked out)
         */
        get("/requirement-check") {
            try {
                val queryUserId = call.request.queryParameters["userId"]
                val headerUserId = call.request.header("X-User-Id")
                val jwtUserId = call.principal<JWTPrincipal>()?.payload?.subject
                val userId = queryUserId ?: headerUserId ?: jwtUserId

                if (userId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Query parameter 'userId' or 'X-User-Id' header is required"))
                    return@get
                }

                val deviceId = call.request.queryParameters["deviceId"]
                    ?: call.request.header("X-Device-Id")
                    ?: "device-default"

                val remoteIp = call.request.queryParameters["ip"]
                    ?: call.request.header("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                    ?: call.request.origin.remoteHost

                val result = presenceService.getRequirementCheckDetails(
                    userId = userId,
                    deviceId = deviceId,
                    ip = remoteIp
                )

                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                logger.error("Failed to check presence requirement: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Internal server error")))
            }
        }

        /**
         * 2.3. POST /api/v1/presence/verify
         * Verifies presence check server-side.
         * Server-side compares face embeddings using Cosine Similarity (threshold >= 0.80).
         * Verifies fingerprint registered device_id and biometric success boolean.
         * Records result to presence_check_log.
         */
        post("/verify") {
            try {
                val req = call.receive<PresenceVerifyRequest>()
                val authenticatedUserId = call.principal<JWTPrincipal>()?.payload?.subject
                val targetUserId = if (req.userId.isNotBlank()) req.userId else authenticatedUserId

                if (targetUserId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                    return@post
                }

                val resolvedDeviceId = req.deviceId ?: call.request.header("X-Device-Id")
                val resolvedIp = req.ipAddress ?: call.request.origin.remoteHost

                val response = presenceService.verify(
                    req.copy(
                        userId = targetUserId,
                        deviceId = resolvedDeviceId,
                        ipAddress = resolvedIp
                    )
                )

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                logger.error("Failed to verify presence: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Internal server error")))
            }
        }

        /**
         * GET /api/v1/presence/enrollment
         * Returns presence enrollment status for a user.
         */
        get("/enrollment") {
            val userId = call.request.queryParameters["userId"]
                ?: call.request.header("X-User-Id")
                ?: call.principal<JWTPrincipal>()?.payload?.subject

            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                return@get
            }

            val enrollment = presenceService.userPresenceEnrollmentRepo.get(userId)
            if (enrollment == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Enrollment not found for user $userId"))
            } else {
                call.respond(HttpStatusCode.OK, enrollment)
            }
        }

        /**
         * GET /api/v1/presence/logs
         * Returns audit logs of presence checks for a user.
         */
        get("/logs") {
            val userId = call.request.queryParameters["userId"]
                ?: call.request.header("X-User-Id")
                ?: call.principal<JWTPrincipal>()?.payload?.subject

            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                return@get
            }

            val logs = presenceService.presenceCheckLogRepo.getLogs(userId)
            call.respond(HttpStatusCode.OK, logs)
        }

        /**
         * GET /api/v1/presence/security-audit-stats
         * PRD Fase 112 / Bagian C: Returns platform-wide aggregate presence security audit summary.
         * Privacy-safe aggregated data (NO individual biometrics).
         */
        get("/security-audit-stats") {
            try {
                val stats = presenceService.getSecurityAuditSummary()
                call.respond(HttpStatusCode.OK, stats)
            } catch (e: Exception) {
                logger.error("Failed to generate presence security audit stats: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Internal server error")))
            }
        }
    }
}
