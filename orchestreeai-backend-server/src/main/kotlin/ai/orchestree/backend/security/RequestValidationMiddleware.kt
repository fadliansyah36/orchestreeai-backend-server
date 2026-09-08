package ai.orchestree.backend.security

import ai.orchestree.backend.models.CreateDepartmentRequest
import ai.orchestree.backend.models.CreateTaskRequest
import ai.orchestree.backend.models.LoginRequest
import ai.orchestree.backend.models.RegisterRequest
import ai.orchestree.backend.models.TenantOnboardRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory
import java.net.URI

private val logger = LoggerFactory.getLogger("ai.orchestree.backend.security.RequestValidationMiddleware")

/**
 * Server-Side Input Validation (PRD Fase 123 Bagian A)
 * Re-validates every request DTO before executing any business logic.
 * Enforces strict length, control character, null-byte, and format constraints.
 */
object ServerInputValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateEmail(input: String): Pair<Boolean, String?> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false to "Email wajib diisi"
        if (trimmed.length > 254) return false to "Email melebihi batas 254 karakter"
        if (!EMAIL_REGEX.matches(trimmed)) return false to "Format email tidak valid"
        if (trimmed.contains("\u0000") || trimmed.any { it.isISOControl() }) {
            return false to "Email mengandung karakter ilegal"
        }
        return true to null
    }

    fun validatePassword(input: String): Pair<Boolean, String?> {
        if (input.length < 6) return false to "Kata sandi minimal 6 karakter"
        if (input.length > 128) return false to "Kata sandi maksimal 128 karakter"
        if (input.contains("\u0000") || input.any { it.isISOControl() }) {
            return false to "Kata sandi mengandung karakter kontrol ilegal"
        }
        return true to null
    }

    fun validateUrl(input: String): Pair<Boolean, String?> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false to "URL tidak boleh kosong"
        if (trimmed.length > 2048) return false to "URL melebihi batas 2048 karakter"
        val uri = runCatching { URI(trimmed) }.getOrNull()
            ?: return false to "Format URL tidak valid"
        if (uri.scheme != "https") {
            return false to "URL wajib menggunakan skema HTTPS"
        }
        if (trimmed.contains("\u0000") || trimmed.any { it.isISOControl() }) {
            return false to "URL mengandung karakter kontrol ilegal"
        }
        return true to null
    }

    fun sanitizeText(input: String, maxLength: Int = 5000): String {
        return input
            .replace("\u0000", "")
            .filterNot { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }
            .take(maxLength)
    }
}

fun Application.configureRequestValidation() {
    logger.info("Configuring Server-Side RequestValidation and StatusPages plugins...")

    install(RequestValidation) {
        validate<CreateTaskRequest> { request ->
            val reasons = mutableListOf<String>()
            if (request.title.isBlank()) {
                reasons.add("Judul tugas tidak boleh kosong")
            }
            if (request.title.length > 200) {
                reasons.add("Judul tugas maksimal 200 karakter (diberikan: ${request.title.length})")
            }
            if (request.description.length > 5000) {
                reasons.add("Deskripsi tugas maksimal 5000 karakter (diberikan: ${request.description.length})")
            }
            if (request.title.contains("\u0000") || request.description.contains("\u0000")) {
                reasons.add("Input tugas mengandung karakter null-byte ilegal")
            }
            if (reasons.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(reasons)
        }

        validate<LoginRequest> { request ->
            val reasons = mutableListOf<String>()
            val (emailValid, emailErr) = ServerInputValidator.validateEmail(request.email)
            if (!emailValid) reasons.add(emailErr ?: "Email tidak valid")

            val (passValid, passErr) = ServerInputValidator.validatePassword(request.password)
            if (!passValid) reasons.add(passErr ?: "Password tidak valid")

            if (reasons.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(reasons)
        }

        validate<RegisterRequest> { request ->
            val reasons = mutableListOf<String>()
            if (request.name.isBlank()) reasons.add("Nama lengkap wajib diisi")
            if (request.name.length > 150) reasons.add("Nama lengkap maksimal 150 karakter")

            val (emailValid, emailErr) = ServerInputValidator.validateEmail(request.email)
            if (!emailValid) reasons.add(emailErr ?: "Email tidak valid")

            val (passValid, passErr) = ServerInputValidator.validatePassword(request.password)
            if (!passValid) reasons.add(passErr ?: "Password tidak valid")

            if (reasons.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(reasons)
        }

        validate<TenantOnboardRequest> { request ->
            val reasons = mutableListOf<String>()
            if (request.companyName.isBlank()) reasons.add("Nama perusahaan wajib diisi")
            if (request.companyName.length > 200) reasons.add("Nama perusahaan maksimal 200 karakter")

            val (emailValid, emailErr) = ServerInputValidator.validateEmail(request.adminEmail)
            if (!emailValid) reasons.add(emailErr ?: "Email admin tidak valid")

            val (passValid, passErr) = ServerInputValidator.validatePassword(request.adminPassword)
            if (!passValid) reasons.add(passErr ?: "Password admin tidak valid")

            if (reasons.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(reasons)
        }

        validate<CreateDepartmentRequest> { request ->
            val reasons = mutableListOf<String>()
            if (request.name.isBlank()) reasons.add("Nama departemen wajib diisi")
            if (request.name.length > 100) reasons.add("Nama departemen maksimal 100 karakter")
            if (request.description.length > 2000) reasons.add("Deskripsi departemen maksimal 2000 karakter")
            if (reasons.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(reasons)
        }
    }

    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            logger.warn("Request rejected by RequestValidation: ${cause.reasons}")
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "error",
                    "error" to "Request validation failed",
                    "reasons" to cause.reasons
                )
            )
        }
    }
}
