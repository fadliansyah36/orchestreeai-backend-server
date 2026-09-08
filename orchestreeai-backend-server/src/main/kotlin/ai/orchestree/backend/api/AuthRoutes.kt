package ai.orchestree.backend.api

import ai.orchestree.backend.config.AppConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class LoginApiRequest(
    val email: String,
    val password: String? = null,
    val tenantId: String? = null
)

@Serializable
data class LoginApiResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long = 3600,
    val userId: String,
    val tenantId: String
)

@Serializable
data class RefreshApiRequest(
    val refreshToken: String
)


@Serializable
data class UserSessionDto(
    val id: String,
    val userId: String,
    val deviceName: String,
    val ipAddress: String,
    val userAgent: String,
    val isCurrent: Boolean,
    val createdAt: Long,
    val expiresAt: Long
)

@Serializable
data class UserProfileDto(
    val userId: String,
    val tenantId: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val telegramChatId: String = "",
    val themePreference: String = "SYSTEM",
    val languagePreference: String = "id"
)

fun Route.authRoutes() {
    val config = AppConfig.load()
    val jwtSecret = config.security.jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    val algorithm = Algorithm.HMAC256(jwtSecret)

    route("/auth") {
        post("/login") {
            val req = call.receive<LoginApiRequest>()
            val resolvedTenantId = req.tenantId ?: "tenant-enterprise-001"
            val userId = "usr-${java.util.UUID.nameUUIDFromBytes(req.email.toByteArray()).toString().take(8)}"

            val now = Date()
            val expiresAt = Date(now.time + 3600 * 1000) // 1 hour

            val token = JWT.create()
                .withSubject(userId)
                .withClaim("sub", userId)
                .withClaim("email", req.email)
                .withClaim("tenant_id", resolvedTenantId)
                .withClaim("role", "TENANT_ADMIN")
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .sign(algorithm)

            val refreshToken = "rt-${java.util.UUID.randomUUID()}"

            call.respond(
                HttpStatusCode.OK,
                LoginApiResponse(
                    accessToken = token,
                    refreshToken = refreshToken,
                    userId = userId,
                    tenantId = resolvedTenantId
                )
            )
        }

        post("/refresh") {
            val req = call.receive<RefreshApiRequest>()
            val refreshedUserId = "usr-refreshed"
            val refreshedTenantId = "tenant-enterprise-001"
            val now = Date()
            val expiresAt = Date(now.time + 3600 * 1000)

            val token = JWT.create()
                .withSubject(refreshedUserId)
                .withClaim("sub", refreshedUserId)
                .withClaim("tenant_id", refreshedTenantId)
                .withClaim("role", "STAFF_HUMAN")
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .sign(algorithm)

            call.respond(
                HttpStatusCode.OK,
                LoginApiResponse(
                    accessToken = token,
                    refreshToken = req.refreshToken,
                    userId = refreshedUserId,
                    tenantId = refreshedTenantId
                )
            )
        }

        get("/sessions") {
            val authHeader = call.request.headers["Authorization"]
            if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token otentikasi dibutuhkan"))
                return@get
            }
            val tokenStr = authHeader.removePrefix("Bearer ").trim()
            try {
                val decoded = JWT.decode(tokenStr)
                val userId = decoded.subject ?: decoded.getClaim("user_id")?.asString() ?: "usr-current"
                val sessions = listOf(
                    UserSessionDto(
                        id = "sess-${java.util.UUID.randomUUID().toString().take(8)}",
                        userId = userId,
                        deviceName = call.request.headers["User-Agent"]?.take(50) ?: "Active Client Device",
                        ipAddress = "127.0.0.1",
                        userAgent = call.request.headers["User-Agent"] ?: "OrchestreeAI-Mobile/1.0",
                        isCurrent = true,
                        createdAt = decoded.issuedAt?.time ?: (System.currentTimeMillis() - 3600000),
                        expiresAt = decoded.expiresAt?.time ?: (System.currentTimeMillis() + 86400000)
                    )
                )
                call.respond(HttpStatusCode.OK, sessions)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token otentikasi tidak valid"))
            }
        }

        post("/sessions/revoke/{id}") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "revoked"))
        }

        get("/profile") {
            val authHeader = call.request.headers["Authorization"]
            if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token otentikasi dibutuhkan"))
                return@get
            }
            val tokenStr = authHeader.removePrefix("Bearer ").trim()
            try {
                val decoded = JWT.decode(tokenStr)
                val userId = decoded.subject ?: decoded.getClaim("user_id")?.asString() ?: "usr-current"
                val tenantId = decoded.getClaim("tenant_id")?.asString() ?: "tenant-enterprise-001"
                val email = decoded.getClaim("email")?.asString() ?: "admin@nusantara.co.id"
                val name = decoded.getClaim("name")?.asString() ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                call.respond(
                    HttpStatusCode.OK,
                    UserProfileDto(
                        userId = userId,
                        tenantId = tenantId,
                        name = name,
                        email = email,
                        phone = "+6281234567890",
                        telegramChatId = "@orchestree_admin",
                        themePreference = "SYSTEM",
                        languagePreference = "id"
                    )
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token otentikasi tidak valid"))
            }
        }

        post("/profile") {
            val req = call.receive<UserProfileDto>()
            call.respond(HttpStatusCode.OK, req)
        }

    }
}
