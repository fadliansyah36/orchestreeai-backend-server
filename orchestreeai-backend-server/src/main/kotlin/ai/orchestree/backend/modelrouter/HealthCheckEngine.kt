package ai.orchestree.backend.modelrouter

import ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class ProviderHealth(
    val providerName: String,
    val isHealthy: Boolean,
    val latencyMs: Long,
    val lastCheckedAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

class HealthCheckEngine(
    private val modelRouter: ModelRouter = ModelRouter()
) {
    private val logger = LoggerFactory.getLogger(HealthCheckEngine::class.java)

    /**
     * Validates that all active LLM providers in ProviderRegistryRepository have a valid model name configured.
     * Logs a clear error for any registered provider missing a model name.
     */
    fun validateRegisteredProviderModels(): Boolean {
        val providers = ProviderRegistryRepository.instance.getAll()
        var allValid = true
        for (provider in providers) {
            if (provider.isEnabled) {
                if (provider.defaultModel.isBlank()) {
                    logger.error("[LLM ROUTER CONFIG ERROR] Registered provider '${provider.name}' (${provider.providerCode}) has no defaultModel configured! This will cause HTTP 400 (missing_required_field 'model') on OpenAI-compatible gateways.")
                    allValid = false
                } else {
                    logger.info("[LLM ROUTER] Provider '${provider.name}' (${provider.providerCode}) configured with defaultModel='${provider.defaultModel}'")
                }
            }
        }
        return allValid
    }

    suspend fun checkAll(): List<ProviderHealth> {
        val results = mutableListOf<ProviderHealth>()
        val testReq = LlmRequest(prompt = "ping", maxTokens = 5)

        // Test NVIDIA NIM (Tier 1 Primary)
        val startNim = System.currentTimeMillis()
        val nimRes = modelRouter.nvidiaNimClient.complete(testReq)
        results.add(
            ProviderHealth(
                providerName = "NVIDIA NIM",
                isHealthy = nimRes.isSuccess,
                latencyMs = System.currentTimeMillis() - startNim,
                errorMessage = nimRes.exceptionOrNull()?.message
            )
        )

        // Test OpenRouter (Tier 2 Fallback)
        val startOr = System.currentTimeMillis()
        val orRes = modelRouter.openRouterClient.complete(testReq)
        results.add(
            ProviderHealth(
                providerName = "OpenRouter",
                isHealthy = orRes.isSuccess,
                latencyMs = System.currentTimeMillis() - startOr,
                errorMessage = orRes.exceptionOrNull()?.message
            )
        )

        // Test Groq (Tier 3 Fallback)
        val startGroq = System.currentTimeMillis()
        val groqRes = modelRouter.groqClient.complete(testReq)
        results.add(
            ProviderHealth(
                providerName = "Groq",
                isHealthy = groqRes.isSuccess,
                latencyMs = System.currentTimeMillis() - startGroq,
                errorMessage = groqRes.exceptionOrNull()?.message
            )
        )

        logger.info("HealthCheckEngine completed health check for ${results.size} providers.")
        return results
    }
}
