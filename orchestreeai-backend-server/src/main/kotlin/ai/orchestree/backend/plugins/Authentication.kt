package ai.orchestree.backend.plugins

import ai.orchestree.backend.config.AppConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal

fun Application.configureAuthentication(config: AppConfig) {
    val jwtSecret = config.security.jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    val algorithm = Algorithm.HMAC256(jwtSecret)
    val verifier = JWT.require(algorithm).build()

    install(Authentication) {
        jwt("supabase-auth") {
            realm = "OrchestreeAI Server"
            verifier(verifier)
            validate { credential: JWTCredential ->
                val userId = credential.payload.getClaim("sub")?.asString() ?: credential.payload.subject
                if (!userId.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }

        jwt("auth-jwt") {
            realm = "OrchestreeAI Server"
            verifier(verifier)
            validate { credential: JWTCredential ->
                val userId = credential.payload.getClaim("sub")?.asString() ?: credential.payload.subject
                if (!userId.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

