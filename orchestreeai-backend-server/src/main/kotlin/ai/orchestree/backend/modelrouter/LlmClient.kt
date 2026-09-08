package ai.orchestree.backend.modelrouter

import kotlinx.serialization.Serializable

enum class CircuitBreakerState { CLOSED, OPEN, HALF_OPEN }

enum class SensitivityTier { PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED }

@Serializable
data class LlmRequest(
    val prompt: String,
    val systemInstruction: String? = null,
    val temperature: Double = 0.4,
    val maxTokens: Int = 2048,
    val model: String? = null,
    val responseFormatJson: Boolean = false
)

@Serializable
data class LlmResponse(
    val text: String,
    val totalTokens: Int = 0,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val modelUsed: String = "",
    val providerUsed: String = "",
    val durationMs: Long = 0,
    val costUsd: Double = 0.0
)

data class ModelRouteRequest(
    val taskCategory: String = "GENERAL", // "CLASSIFICATION", "REASONING", "IMAGE_GEN", "PROACTIVE_BRIEF", "GENERAL_CHAT"
    val sensitivityTier: String = "INTERNAL",
    val prompt: String,
    val systemInstruction: String? = null,
    val tenantId: String = "tenant-default",
    val taskId: String? = null,
    val workflowExecutionId: String? = null,
    val maxLatencyMs: Long = 10000,
    val temperature: Double = 0.4,
    val model: String? = null,
    val responseFormatJson: Boolean = false
)

data class ModelRouteDecision(
    val client: LlmClient,
    val selectedModel: String,
    val isFallback: Boolean = false,
    val routingReason: String
)

@Serializable
data class LlmUsageLog(
    val id: String,
    val tenantId: String,
    val provider: String,
    val model: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val costUsd: Double,
    val durationMs: Long,
    val success: Boolean,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

interface LlmClient {
    val providerId: String
    val providerName: String
    suspend fun complete(request: LlmRequest): Result<LlmResponse>
}
