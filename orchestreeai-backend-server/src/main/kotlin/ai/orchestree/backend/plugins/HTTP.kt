package ai.orchestree.backend.plugins

import ai.orchestree.backend.config.AppConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCORS(config: AppConfig = AppConfig.load()) {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Admin-Role")
        allowHeader("X-Tenant-ID")
        allowHeader("X-Tenant-Id")
        allowHeader("X-Requested-With")
        allowHeader("X-Client-Platform")
        allowHeader("X-CSRF-Protection")
        allowHeader("X-CSRF-Token")
        allowHeader("X-Request-Id")
        allowHeader("Idempotency-Key")
        allowHeader("X-Operator-Id")
        allowHeader("X-Forwarded-For")
        allowHeader("X-Play-Integrity-Token")
        allowHeader("X-App-Signature")
        allowHeader("X-Request-Timestamp")

        exposeHeader("X-CSRF-Token")

        // Allowed hosts - Super Admin Dashboard & local dev
        allowHost("admin.orchestree.biz.id", schemes = listOf("https"))
        allowHost("localhost:3000", schemes = listOf("http", "https"))
        allowHost("localhost:5173", schemes = listOf("http", "https"))
        allowHost("localhost:8080", schemes = listOf("http", "https"))

        val additionalHosts = ai.orchestree.backend.config.EnvLoader.get("CORS_ALLOWED_HOSTS")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (host in additionalHosts) {
            val cleanHost = host.removePrefix("https://").removePrefix("http://")
            allowHost(cleanHost, schemes = listOf("http", "https"))
        }

        allowCredentials = true
    }
}
