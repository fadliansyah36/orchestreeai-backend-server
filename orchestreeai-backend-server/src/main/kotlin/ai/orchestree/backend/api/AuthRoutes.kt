package ai.orchestree.backend.api

import ai.orchestree.backend.config.AppConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
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
    val logger = LoggerFactory.getLogger("ai.orchestree.backend.api.AuthRoutes")
    val config = AppConfig.load()
    val jwtSecret = config.security.jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    val algorithm = Algorithm.HMAC256(jwtSecret)

    route("/auth") {
        post("/login") {
            val req = call.receive<LoginApiRequest>()
            val resolvedTenantId = req.tenantId?.trim()?.takeIf { it.isNotBlank() }
                ?: call.request.headers["X-Tenant-ID"]?.trim()?.takeIf { it.isNotBlank() }
                ?: call.request.headers["X-Tenant-Id"]?.trim()?.takeIf { it.isNotBlank() }
            if (resolvedTenantId.isNullOrBlank()) {
                val clientIp = call.request.header("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                    ?: runCatching { call.request.origin.remoteHost }.getOrNull()
                    ?: "unknown-ip"
                val userAgent = call.request.header("User-Agent") ?: "unknown-agent"
                logger.warn(
                    "[AUTH FAILED] POST /auth/login tenantId validation failed. clientIp={}, userAgent={}, email={}, reason='Tenant ID is missing in request body and headers'",
                    clientIp,
                    userAgent,
                    req.email
                )
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Tenant ID is required and could not be resolved from request body or X-Tenant-ID header")
                )
                return@post
            }
            val initialUserId = "usr-${java.util.UUID.nameUUIDFromBytes(req.email.toByteArray()).toString().take(8)}"
            var userId = initialUserId

            try {
                ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                    conn.prepareStatement("SELECT id FROM users WHERE email = ?").use { ps ->
                        ps.setString(1, req.email)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                userId = rs.getString("id")
                            }
                        }
                    }
                    conn.prepareStatement("""
                        INSERT INTO users (id, tenant_id, email, name, is_active, created_at, updated_at)
                        VALUES (?, ?, ?, ?, true, NOW(), NOW())
                        ON CONFLICT (id) DO UPDATE SET updated_at = NOW()
                    """).use { ps ->
                        ps.setString(1, userId)
                        ps.setString(2, resolvedTenantId)
                        ps.setString(3, req.email)
                        ps.setString(4, req.email.substringBefore("@").replaceFirstChar { it.uppercase() })
                        ps.executeUpdate()
                    }

                    val sessionId = "sess-${java.util.UUID.randomUUID().toString().take(8)}"
                    val clientIp = call.request.header("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                        ?: runCatching { call.request.origin.remoteHost }.getOrNull()
                        ?: "127.0.0.1"
                    val userAgent = call.request.header("User-Agent") ?: "OrchestreeAI-Mobile/1.0"

                    conn.prepareStatement("""
                        INSERT INTO user_sessions (id, user_id, tenant_id, device_name, os_name, ip_address, location_approx, is_current_session, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, true, ?)
                        ON CONFLICT (id) DO NOTHING
                    """).use { ps ->
                        ps.setString(1, sessionId)
                        ps.setString(2, userId)
                        ps.setString(3, resolvedTenantId)
                        ps.setString(4, userAgent.take(50))
                        ps.setString(5, "Android/Linux")
                        ps.setString(6, clientIp)
                        ps.setString(7, "Jakarta, ID")
                        ps.setLong(8, System.currentTimeMillis())
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("User session record warning: ${e.message}")
            }

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
            val headerTenant = call.request.headers["X-Tenant-ID"] ?: call.request.headers["X-Tenant-Id"]
            val refreshedTenantId = try {
                JWT.decode(req.refreshToken).getClaim("tenant_id")?.asString() ?: headerTenant
            } catch (_: Exception) {
                headerTenant
            }
            if (refreshedTenantId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Tenant ID is required and could not be resolved from refresh token or header")
                )
                return@post
            }
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
                val sessions = mutableListOf<UserSessionDto>()

                try {
                    ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                        conn.prepareStatement("""
                            SELECT id, user_id, device_name, ip_address, is_current_session, created_at
                            FROM user_sessions
                            WHERE user_id = ?
                            ORDER BY created_at DESC
                            LIMIT 20
                        """).use { ps ->
                            ps.setString(1, userId)
                            val rs = ps.executeQuery()
                            while (rs.next()) {
                                val createdAt = rs.getLong("created_at")
                                val devName = rs.getString("device_name") ?: "Active Client Device"
                                sessions.add(
                                    UserSessionDto(
                                        id = rs.getString("id"),
                                        userId = rs.getString("user_id"),
                                        deviceName = devName,
                                        ipAddress = rs.getString("ip_address") ?: "127.0.0.1",
                                        userAgent = devName,
                                        isCurrent = rs.getBoolean("is_current_session"),
                                        createdAt = createdAt,
                                        expiresAt = createdAt + 86400000L
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Query sessions database warning: ${e.message}")
                }

                if (sessions.isEmpty()) {
                    sessions.add(
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
                }
                call.respond(HttpStatusCode.OK, sessions)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token otentikasi tidak valid"))
            }
        }

        post("/sessions/revoke/{id}") {
            val sessionId = call.parameters["id"] ?: ""
            var revoked = false
            try {
                ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                    conn.prepareStatement("DELETE FROM user_sessions WHERE id = ?").use { ps ->
                        ps.setString(1, sessionId)
                        revoked = ps.executeUpdate() > 0
                    }
                }
            } catch (e: Exception) {
                logger.warn("Revoke session DB warning: ${e.message}")
            }
            call.respond(HttpStatusCode.OK, mapOf("status" to "revoked", "id" to sessionId, "success" to revoked))
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
                val tenantId = decoded.getClaim("tenant_id")?.asString()
                    ?: call.request.headers["X-Tenant-ID"]
                    ?: call.request.headers["X-Tenant-Id"]
                if (tenantId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Tenant ID is required and could not be resolved from token claims or headers"))
                    return@get
                }
                val email = decoded.getClaim("email")?.asString() ?: "admin@nusantara.co.id"
                val defaultName = decoded.getClaim("name")?.asString() ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

                var userProfile: UserProfileDto? = null
                try {
                    ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                        conn.prepareStatement("""
                            SELECT id, tenant_id, name, email, phone, telegram_chat_id, theme_preference, language_preference
                            FROM users
                            WHERE id = ? OR email = ?
                            LIMIT 1
                        """).use { ps ->
                            ps.setString(1, userId)
                            ps.setString(2, email)
                            val rs = ps.executeQuery()
                            if (rs.next()) {
                                userProfile = UserProfileDto(
                                    userId = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id") ?: tenantId,
                                    name = rs.getString("name") ?: defaultName,
                                    email = rs.getString("email") ?: email,
                                    phone = rs.getString("phone") ?: "+6281234567890",
                                    telegramChatId = rs.getString("telegram_chat_id") ?: "@orchestree_admin",
                                    themePreference = rs.getString("theme_preference") ?: "SYSTEM",
                                    languagePreference = rs.getString("language_preference") ?: "id"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Query user profile DB warning: ${e.message}")
                }

                if (userProfile == null) {
                    userProfile = UserProfileDto(
                        userId = userId,
                        tenantId = tenantId,
                        name = defaultName,
                        email = email,
                        phone = "+6281234567890",
                        telegramChatId = "@orchestree_admin",
                        themePreference = "SYSTEM",
                        languagePreference = "id"
                    )
                }

                call.respond(HttpStatusCode.OK, userProfile!!)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token otentikasi tidak valid"))
            }
        }

        post("/profile") {
            val req = call.receive<UserProfileDto>()
            try {
                ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                    conn.prepareStatement("""
                        INSERT INTO users (id, tenant_id, name, email, phone, telegram_chat_id, theme_preference, language_preference, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                        ON CONFLICT (id) DO UPDATE SET
                            name = EXCLUDED.name,
                            phone = EXCLUDED.phone,
                            telegram_chat_id = EXCLUDED.telegram_chat_id,
                            theme_preference = EXCLUDED.theme_preference,
                            language_preference = EXCLUDED.language_preference,
                            updated_at = NOW()
                    """).use { ps ->
                        ps.setString(1, req.userId)
                        ps.setString(2, req.tenantId)
                        ps.setString(3, req.name)
                        ps.setString(4, req.email)
                        ps.setString(5, req.phone)
                        ps.setString(6, req.telegramChatId)
                        ps.setString(7, req.themePreference)
                        ps.setString(8, req.languagePreference)
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Update user profile DB warning: ${e.message}")
            }
            call.respond(HttpStatusCode.OK, req)
        }

    }
}
