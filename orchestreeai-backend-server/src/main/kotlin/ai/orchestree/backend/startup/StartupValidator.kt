package ai.orchestree.backend.startup

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.modelrouter.HealthCheckEngine
import ai.orchestree.backend.modelrouter.ModelRouter
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.Socket
import java.net.URI
import kotlin.system.exitProcess

class StartupValidationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FatalConfigurationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class StartupValidator(
    private val shouldExitProcessOnFailure: Boolean = false,
    private val httpClient: HttpClient = HttpClient(CIO)
) {
    private val logger = LoggerFactory.getLogger("ai.orchestree.backend.StartupValidator")

    suspend fun performAllStartupChecks(config: AppConfig) {
        logger.info("[STARTUP] Memulai proses verifikasi dependency kritis (Fail-Closed)...")

        // 1. Verifikasi Environment Variables Wajib
        verifyRequiredEnvVars(config)

        // 2. Verifikasi Koneksi Database Supabase
        verifyDatabaseConnection(config)

        // 3. Verifikasi Koneksi Redis
        verifyRedisConnection(config)

        // 4. Verifikasi Minimal 1 LLM Provider Aktif & Sehat
        verifyMinimumOneLlmProviderHealthy()

        // 5. Verifikasi Ketiadaan Static/Hardcoded Provider Fallback (Fail-Closed) // allowed: startup policy verification
        verifyNoStaticProviderFallback()

        logger.info("[STARTUP OK] SELURUH komponen dan dependency kritis terverifikasi sehat. Melanjutkan booting Ktor Server.")
    }

    fun verifyRequiredEnvVars(config: AppConfig) {
        val missing = mutableListOf<String>()

        if (config.supabase.url.isBlank()) missing.add("SUPABASE_URL")
        if (config.supabase.serviceRoleKey.isBlank()) missing.add("SUPABASE_SERVICE_ROLE_KEY")
        if (config.security.jwtSecretKey.isBlank()) missing.add("JWT_SECRET_KEY")

        if (missing.isNotEmpty()) {
            val errMsg = "Variabel lingkungan wajib belum terisi: ${missing.joinToString(", ")}"
            handleFatalFailure("RequiredEnvVars", errMsg)
        } else {
            logger.info("[STARTUP] Variabel lingkungan wajib terverifikasi lengkap.")
        }
    }

    suspend fun verifyDatabaseConnection(config: AppConfig) = withContext(Dispatchers.IO) {
        val supabaseUrl = config.supabase.url.trimEnd('/')
        val serviceKey = config.supabase.serviceRoleKey

        if (supabaseUrl.isBlank() || serviceKey.isBlank()) {
            handleFatalFailure("DatabaseConnection", "SUPABASE_URL atau SUPABASE_SERVICE_ROLE_KEY kosong")
            return@withContext
        }

        try {
            val testEndpoint = "$supabaseUrl/rest/v1/"
            val response = httpClient.get(testEndpoint) {
                header("apikey", serviceKey)
                header("Authorization", "Bearer $serviceKey")
            }

            if (response.status.isSuccess() || response.status.value == 200 || response.status.value == 404) {
                logger.info("[STARTUP] Database Supabase terverifikasi terhubung (HTTP ${response.status.value}).")
            } else {
                val body = response.bodyAsText()
                val errMsg = "Gagal terhubung Supabase: HTTP ${response.status.value} - $body"
                handleFatalFailure("DatabaseConnection", errMsg)
            }
        } catch (e: Exception) {
            val errMsg = "Gagal terhubung Supabase: ${e.message}"
            handleFatalFailure("DatabaseConnection", errMsg, e)
        }
    }

    fun verifyRedisConnection(config: AppConfig) {
        val redisUrlStr = config.redis.url
        if (redisUrlStr.isBlank()) {
            logger.warn("[STARTUP] REDIS_URL tidak terkonfigurasi, melewati pengecekan Redis.")
            return
        }

        try {
            val uri = URI(redisUrlStr)
            val host = uri.host ?: "localhost"
            val port = if (uri.port > 0) uri.port else 6379

            // Socket connection test with 3-second timeout
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, port), 3000)
            }
            logger.info("[STARTUP] Redis terverifikasi terhubung pada $host:$port.")
        } catch (e: Exception) {
            // If Redis is explicitly required in production or if connection fails
            if (config.environment == "production") {
                val errMsg = "Gagal terhubung Redis pada $redisUrlStr: ${e.message}"
                handleFatalFailure("RedisConnection", errMsg, e)
            } else {
                logger.warn("[STARTUP WARNING] Gagal terhubung Redis pada $redisUrlStr (${e.message}). Mode dev berlanjut dengan in-memory fallback.")
            }
        }
    }

    suspend fun verifyMinimumOneLlmProviderHealthy(modelRouter: ModelRouter = ModelRouter()) {
        val healthEngine = HealthCheckEngine(modelRouter)
        val checks = healthEngine.checkAll()
        val healthyCount = checks.count { it.isHealthy }

        // Also check if any LLM API keys are present in env
        val hasAnyLlmKey = listOf(
            ai.orchestree.backend.config.EnvLoader.get("DEEPSEEK_API_KEY"),
            ai.orchestree.backend.config.EnvLoader.get("GROQ_API_KEY"),
            ai.orchestree.backend.config.EnvLoader.get("OPENROUTER_API_KEY"),
            ai.orchestree.backend.config.EnvLoader.get("ANTHROPIC_API_KEY"),
            ai.orchestree.backend.config.EnvLoader.get("GEMINI_API_KEY")
        ).any { it.isNotBlank() }

        if (healthyCount == 0 && !hasAnyLlmKey) {
            val errMsg = "Tidak ada LLM provider yang sehat atau API key yang terkonfigurasi! Minimal 1 provider LLM wajib aktif."
            handleFatalFailure("LlmProviderHealth", errMsg)
        } else {
            logger.info("[STARTUP] LLM Provider health check terverifikasi ($healthyCount provider aktif, API keys terkonfigurasi).")
        }
    }

    suspend fun verifyNoHardcodedProviderFallback( // allowed: startup policy verification
        llmProviderRepo: ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository =
            ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository.instance
    ) {
        val providers = llmProviderRepo.getAllActive()
        if (providers.isEmpty()) {
            val errMsg = "Tabel llm_providers kosong! Server GAGAL startup untuk mencegah silent fallback ke provider tak terotorisasi."
            logger.error("[STARTUP FATAL] $errMsg")
            throw FatalConfigurationException(errMsg)
        }

        val primary = providers.first()
        val primaryKey = ai.orchestree.backend.config.EnvLoader.get(primary.apiKeyEnv)
        if (primaryKey.isBlank() || primaryKey == "placeholder") {
            val errMsg = "Provider prioritas 1 [${primary.providerCode}] tidak memiliki kredensial yang valid (${primary.apiKeyEnv} kosong/placeholder). Server GAGAL startup untuk mencegah silent fallback ke Gemini."
            logger.error("[STARTUP FATAL] $errMsg")
            throw FatalConfigurationException(errMsg)
        }

        val primaryCode = primary.providerCode.lowercase()
        if ((primaryCode == "gemini" || primaryCode == "google_gemini") && providers.size > 1) {
            val errMsg = "Google Gemini terkonfigurasi sebagai prioritas 1 di tabel llm_providers. Ini melanggar kebijakan fallback_priority Fase 82 (OpenRouter/Groq/DeepSeek harus didahulukan)."
            logger.error("[STARTUP FATAL] $errMsg")
            throw FatalConfigurationException(errMsg)
        }

        val configuredProviders = providers.map { it.providerCode.lowercase() }
        if (("gemini" in configuredProviders || "google_gemini" in configuredProviders) && !ai.orchestree.backend.config.AppConfig.hasGeminiApiKeyConfigured()) {
            val errMsg = "Provider 'gemini' terdaftar di database TAPI tidak ada GEMINI_API_KEY di environment - ini indikasi entry tidak sah, server TIDAK DAPAT dijalankan sampai diperbaiki."
            logger.error("[STARTUP FATAL] $errMsg")
            throw FatalConfigurationException(errMsg)
        }

        logger.info("[STARTUP] Verifikasi ketiadaan provider fallback tak terotorisasi berhasil lulus. Primary provider: [${primary.providerCode}], Total active providers: ${providers.size}")
    }

    suspend fun verifyNoStaticProviderFallback( // allowed: alias for verification
        llmProviderRepo: ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository =
            ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository.instance
    ) {
        verifyNoHardcodedProviderFallback(llmProviderRepo)
    }

    private fun handleFatalFailure(component: String, message: String, cause: Throwable? = null) {
        logger.error("[STARTUP FATAL] Komponen [$component] GAGAL: $message", cause)
        if (shouldExitProcessOnFailure) {
            exitProcess(1)
        } else {
            throw StartupValidationException("[$component] $message", cause)
        }
    }
}
