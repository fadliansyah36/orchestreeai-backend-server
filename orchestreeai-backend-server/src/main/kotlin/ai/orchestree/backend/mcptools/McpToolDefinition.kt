package ai.orchestree.backend.mcptools

import kotlinx.serialization.Serializable

enum class McpRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: String,
    val riskLevel: McpRiskLevel = McpRiskLevel.LOW,
    val requiredCapability: String? = null,
    val timeoutMs: Long = 10000
)

@Serializable
data class McpExecutionResult(
    val success: Boolean,
    val output: String,
    val durationMs: Long,
    val riskLevel: McpRiskLevel,
    val isBlockedByGovernance: Boolean = false,
    val requiresHumanApproval: Boolean = false,
    val errorMessage: String? = null
)
