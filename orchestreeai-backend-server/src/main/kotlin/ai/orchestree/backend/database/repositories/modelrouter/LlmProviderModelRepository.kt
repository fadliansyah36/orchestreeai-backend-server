package ai.orchestree.backend.database.repositories.modelrouter

import ai.orchestree.backend.config.EnvLoader
import ai.orchestree.backend.database.SupabaseClientProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class LlmProviderModelEntity(
    val id: String = UUID.randomUUID().toString(),
    val providerId: String,
    val providerCode: String,
    val modelIdentifier: String,
    val complexityTier: String, // 'trivial','simple','moderate','complex','frontier'
    val contextWindow: Int = 131072,
    val supportsToolCalling: Boolean = true,
    val supportsVision: Boolean = false,
    val isActive: Boolean = true,
    val lastVerifiedAt: Long = System.currentTimeMillis()
)

/**
 * LlmProviderModelRepository
 * Single source of truth for dynamically discovered LLM models (NVIDIA NIM, OpenRouter, etc.).
 * Dynamically discovers and registers model identifiers in application code.
 */
open class LlmProviderModelRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val httpClient: HttpClient = HttpClient(CIO)
) {
    private val logger = LoggerFactory.getLogger(LlmProviderModelRepository::class.java)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Key: providerCode:modelIdentifier
    private val modelCache = ConcurrentHashMap<String, LlmProviderModelEntity>()

    init {
        // Dynamic Model Discovery: models are fetched dynamically via syncNvidiaNimModelCatalog()
        // or synced from PostgreSQL database table llm_provider_models.
        // No static model strings are stored in code.
    }

    fun registerModel(model: LlmProviderModelEntity) {
        val key = "${model.providerCode.uppercase()}:${model.modelIdentifier}"
        modelCache[key] = model
    }

    open fun getAllActive(): List<LlmProviderModelEntity> {
        return modelCache.values.filter { it.isActive }
    }

    open fun getActiveModelsForProvider(providerCode: String): List<LlmProviderModelEntity> {
        val code = providerCode.uppercase()
        return modelCache.values.filter { it.isActive && it.providerCode.uppercase() == code }
    }

    open fun getActiveModelsForTier(providerCode: String, tier: String): List<LlmProviderModelEntity> {
        val code = providerCode.uppercase().trim()
        val normalizedTier = tier.lowercase().trim()

        if (modelCache.isEmpty()) {
            try {
                kotlinx.coroutines.runBlocking { syncFromDatabase() }
            } catch (e: Exception) {
                logger.warn("Could not lazily sync models from database: ${e.message}")
            }
        }

        val matching = modelCache.values.filter {
            it.isActive && it.providerCode.equals(code, ignoreCase = true) && it.complexityTier.lowercase() == normalizedTier
        }
        if (matching.isNotEmpty()) return matching

        // Tier fallback within the same provider:
        // 'frontier' / 'complex' fallback to 'moderate'
        // 'trivial' / 'simple' fallback to 'moderate'
        val tierFallback = when (normalizedTier) {
            "frontier" -> modelCache.values.filter { it.isActive && it.providerCode.equals(code, ignoreCase = true) && it.complexityTier in listOf("complex", "moderate") }
            "complex" -> modelCache.values.filter { it.isActive && it.providerCode.equals(code, ignoreCase = true) && it.complexityTier in listOf("frontier", "moderate") }
            "simple", "trivial" -> modelCache.values.filter { it.isActive && it.providerCode.equals(code, ignoreCase = true) && it.complexityTier in listOf("simple", "trivial", "moderate") }
            else -> modelCache.values.filter { it.isActive && it.providerCode.equals(code, ignoreCase = true) }
        }
        if (tierFallback.isNotEmpty()) return tierFallback

        val anyForProvider = modelCache.values.filter { it.isActive && it.providerCode.equals(code, ignoreCase = true) }
        if (anyForProvider.isNotEmpty()) return anyForProvider

        val fallbackId = ai.orchestree.backend.modelrouter.providers.OpenAiCompatibleLlmClient.resolveFallbackModel(code)
        if (fallbackId.isNotBlank()) {
            val synthetic = LlmProviderModelEntity(
                id = "fallback-$code-$fallbackId",
                providerId = "llm-$code",
                providerCode = code,
                modelIdentifier = fallbackId,
                complexityTier = normalizedTier,
                isActive = true
            )
            return listOf(synthetic)
        }

        return emptyList()
    }

    open fun classifyComplexityTier(modelId: String, contextWindow: Int = 131072): String {
        val lower = modelId.lowercase()
        return when {
            lower.contains("405b") || lower.contains("deepseek-r1") || lower.contains("kimi-k2") || lower.contains("nemotron-4-340b") -> "frontier"
            lower.contains("70b") || lower.contains("72b") || lower.contains("r1") || contextWindow >= 128000 -> "complex"
            lower.contains("8b") || lower.contains("14b") || lower.contains("32b") || lower.contains("34b") || lower.contains("mistral") -> "moderate"
            lower.contains("1b") || lower.contains("3b") || lower.contains("7b") || lower.contains("mini") || lower.contains("nano") -> "simple"
            lower.contains("embed") || lower.contains("guard") || lower.contains("rerank") -> "trivial"
            else -> "moderate"
        }
    }

    /**
     * DYNAMIC MODEL DISCOVERY: NVIDIA NIM
     * Fetches model catalog directly from NVIDIA API: https://integrate.api.nvidia.com/v1/models
     * Dynamic model discovery directly from NVIDIA API.
     */
    suspend fun syncNvidiaNimModelCatalog(apiKeyOverride: String? = null): Int = withContext(Dispatchers.IO) {
        val apiKey = apiKeyOverride ?: EnvLoader.get("NVIDIA_API_KEY").ifBlank {
            System.getenv("NVIDIA_API_KEY") ?: ""
        }
        if (apiKey.isBlank()) {
            logger.info("NVIDIA_API_KEY not configured. Skipping remote NVIDIA NIM model discovery.")
            return@withContext 0
        }

        try {
            val endpoint = "https://integrate.api.nvidia.com/v1/models"
            val response = httpClient.get(endpoint) {
                header("Authorization", "Bearer $apiKey")
            }

            if (!response.status.isSuccess()) {
                logger.warn("NVIDIA NIM dynamic discovery failed: HTTP ${response.status.value}")
                return@withContext 0
            }

            val bodyText = response.bodyAsText()
            val parsed = json.parseToJsonElement(bodyText).jsonObject
            val dataArray = parsed["data"]?.jsonArray ?: return@withContext 0

            val discoveredIdentifiers = mutableSetOf<String>()
            var count = 0

            for (item in dataArray) {
                val obj = item.jsonObject
                val modelId = obj["id"]?.jsonPrimitive?.content?.trim() ?: continue
                if (modelId.isBlank()) continue

                discoveredIdentifiers.add(modelId)
                val tier = classifyComplexityTier(modelId)
                val entity = LlmProviderModelEntity(
                    id = "nim-${UUID.nameUUIDFromBytes(modelId.toByteArray())}",
                    providerId = "llm-nvidia-nim",
                    providerCode = "NVIDIA_NIM",
                    modelIdentifier = modelId,
                    complexityTier = tier,
                    contextWindow = 131072,
                    supportsToolCalling = true,
                    supportsVision = modelId.contains("vision") || modelId.contains("multimodal"),
                    isActive = true,
                    lastVerifiedAt = System.currentTimeMillis()
                )
                registerModel(entity)
                count++
            }

            // Deactivate NVIDIA NIM models not present in latest catalog
            modelCache.values.filter { it.providerCode.equals("NVIDIA_NIM", ignoreCase = true) }.forEach { cached ->
                if (!discoveredIdentifiers.contains(cached.modelIdentifier)) {
                    val updated = cached.copy(isActive = false)
                    registerModel(updated)
                }
            }

            persistDiscoveredModelsToDb("NVIDIA_NIM", modelCache.values.filter { it.providerCode.equals("NVIDIA_NIM", ignoreCase = true) })
            logger.info("Dynamic Model Discovery: Successfully discovered and cached $count active models from NVIDIA NIM catalog.")
            count
        } catch (e: Exception) {
            logger.error("Error during NVIDIA NIM dynamic model discovery: ${e.message}", e)
            0
        }
    }

    /**
     * DYNAMIC MODEL DISCOVERY: OpenRouter
     * Fetches model catalog directly from OpenRouter API: https://openrouter.ai/api/v1/models
     */
    suspend fun syncOpenRouterModelCatalog(apiKeyOverride: String? = null): Int = withContext(Dispatchers.IO) {
        val apiKey = apiKeyOverride ?: EnvLoader.get("OPENROUTER_API_KEY").ifBlank {
            System.getenv("OPENROUTER_API_KEY") ?: ""
        }
        if (apiKey.isBlank()) {
            return@withContext 0
        }

        try {
            val endpoint = "https://openrouter.ai/api/v1/models"
            val response = httpClient.get(endpoint) {
                header("Authorization", "Bearer $apiKey")
            }

            if (!response.status.isSuccess()) {
                logger.warn("OpenRouter dynamic discovery failed: HTTP ${response.status.value}")
                return@withContext 0
            }

            val bodyText = response.bodyAsText()
            val parsed = json.parseToJsonElement(bodyText).jsonObject
            val dataArray = parsed["data"]?.jsonArray ?: return@withContext 0

            val discoveredIdentifiers = mutableSetOf<String>()
            var count = 0

            for (item in dataArray) {
                val obj = item.jsonObject
                val modelId = obj["id"]?.jsonPrimitive?.content?.trim() ?: continue
                if (modelId.isBlank()) continue

                val ctx = obj["context_length"]?.jsonPrimitive?.content?.toIntOrNull() ?: 131072
                discoveredIdentifiers.add(modelId)
                val tier = classifyComplexityTier(modelId, ctx)

                val entity = LlmProviderModelEntity(
                    id = "openrouter-${UUID.nameUUIDFromBytes(modelId.toByteArray())}",
                    providerId = "llm-openrouter",
                    providerCode = "OPENROUTER",
                    modelIdentifier = modelId,
                    complexityTier = tier,
                    contextWindow = ctx,
                    supportsToolCalling = true,
                    supportsVision = false,
                    isActive = true,
                    lastVerifiedAt = System.currentTimeMillis()
                )
                registerModel(entity)
                count++
            }

            persistDiscoveredModelsToDb("OPENROUTER", modelCache.values.filter { it.providerCode.equals("OPENROUTER", ignoreCase = true) })
            logger.info("Dynamic Model Discovery: Successfully discovered and cached $count active models from OpenRouter catalog.")
            count
        } catch (e: Exception) {
            logger.error("Error during OpenRouter dynamic model discovery: ${e.message}", e)
            0
        }
    }

    /**
     * Persist discovered catalog models into database table llm_provider_models.
     */
    private suspend fun persistDiscoveredModelsToDb(providerCode: String, models: List<LlmProviderModelEntity>) = withContext(Dispatchers.IO) {
        val dbUrl = EnvLoader.get("DATABASE_URL")
        if (dbUrl.isBlank() || dbUrl == "placeholder") return@withContext

        try {
            val jdbcUrl = if (!dbUrl.startsWith("jdbc:")) "jdbc:$dbUrl" else dbUrl
            Class.forName("org.postgresql.Driver")
            DriverManager.getConnection(jdbcUrl).use { conn ->
                val sql = """
                    INSERT INTO llm_provider_models (id, provider_id, provider_code, model_identifier, complexity_tier, context_window, supports_tool_calling, supports_vision, is_active, last_verified_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                    ON CONFLICT (id) DO UPDATE SET
                        complexity_tier = EXCLUDED.complexity_tier,
                        context_window = EXCLUDED.context_window,
                        is_active = EXCLUDED.is_active,
                        last_verified_at = now()
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    for (m in models) {
                        stmt.setString(1, m.id)
                        stmt.setString(2, m.providerId)
                        stmt.setString(3, m.providerCode)
                        stmt.setString(4, m.modelIdentifier)
                        stmt.setString(5, m.complexityTier)
                        stmt.setInt(6, m.contextWindow)
                        stmt.setBoolean(7, m.supportsToolCalling)
                        stmt.setBoolean(8, m.supportsVision)
                        stmt.setBoolean(9, m.isActive)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }
        } catch (e: Exception) {
            logger.warn("Could not persist discovered models to database (${e.message}). Cached in-memory.")
        }
    }

    /**
     * Sync models from PostgreSQL llm_provider_models table.
     */
    suspend fun syncFromDatabase(): Boolean = withContext(Dispatchers.IO) {
        val dbUrl = EnvLoader.get("DATABASE_URL")
        if (dbUrl.isBlank() || dbUrl == "placeholder") return@withContext false

        try {
            val jdbcUrl = if (!dbUrl.startsWith("jdbc:")) "jdbc:$dbUrl" else dbUrl
            Class.forName("org.postgresql.Driver")
            DriverManager.getConnection(jdbcUrl).use { conn ->
                val stmt = conn.createStatement()
                val rs = stmt.executeQuery("SELECT id, provider_id, provider_code, model_identifier, complexity_tier, context_window, supports_tool_calling, supports_vision, is_active FROM llm_provider_models WHERE is_active = true")
                var found = false
                while (rs.next()) {
                    val entity = LlmProviderModelEntity(
                        id = rs.getString("id"),
                        providerId = rs.getString("provider_id"),
                        providerCode = rs.getString("provider_code"),
                        modelIdentifier = rs.getString("model_identifier"),
                        complexityTier = rs.getString("complexity_tier"),
                        contextWindow = rs.getInt("context_window"),
                        supportsToolCalling = rs.getBoolean("supports_tool_calling"),
                        supportsVision = rs.getBoolean("supports_vision"),
                        isActive = rs.getBoolean("is_active")
                    )
                    registerModel(entity)
                    found = true
                }
                found
            }
        } catch (e: Exception) {
            logger.warn("llm_provider_models database sync skipped (${e.message})")
            false
        }
    }

    companion object {
        val instance by lazy { LlmProviderModelRepository() }
    }
}
