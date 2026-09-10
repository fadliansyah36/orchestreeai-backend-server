package ai.orchestree.backend.modelrouter

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
