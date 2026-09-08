package ai.orchestree.backend.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.request.header
import io.ktor.server.request.host
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respondRedirect

val HttpsEnforcementPlugin = createApplicationPlugin(name = "HttpsEnforcementPlugin") {
    onCall { call ->
        val proto = call.request.header("X-Forwarded-Proto")
        if (proto != null && proto.equals("http", ignoreCase = true)) {
            val host = call.request.host()
            val uri = call.request.uri
            call.response.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
            call.respondRedirect("https://$host$uri", permanent = true)
        }
    }
}

fun Application.configureHTTPS() {
    install(DefaultHeaders) {
        header("X-Engine", "OrchestreeAI-Ktor-Server")
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
        header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header("Content-Security-Policy", "default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none';")
        header("Permissions-Policy", "geolocation=(), camera=(), microphone=()")
    }

    install(HttpsEnforcementPlugin)

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-Tenant-Id")
        allowHeader("X-Request-Id")
        allowHeader("Idempotency-Key")
        allowHeader("X-CSRF-Token")
        allowHeader("X-Admin-Role")
        allowHeader("X-Operator-Id")
        allowHeader("X-Forwarded-For")
        exposeHeader("X-CSRF-Token")
        anyHost()
    }
}

