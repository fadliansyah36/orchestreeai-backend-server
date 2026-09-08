package ai.orchestree.backend.models

import java.time.Instant

/**
 * Super Admin Control Plane Domain Models (PRD Section 25)
 * Covers Multi-Tenant Fleet, Support Mode Impersonation, Multi-LLM Management,
 * Agent Templates, MCP Registry & Kill-Switch, Telemetry, and Master Audit.
 */

data class SuperAdminOverviewDto(
    val totalTenants: Int,
    val activeTenants: Int,
    val totalAgents: Int,
    val activeTasks: Int,
    val monthlyRecurringRevenueUsd: Double,
    val totalTokensProcessed: Long,
    val platformHealthScore: Double,
    val activeIncidentCount: Int,
    val activeImpersonationSessions: Int,
    val isEmergencyKillSwitchActive: Boolean
)

data class TenantDetailDto(
    val id: String,
    val name: String,
    val domain: String,
    val tier: String,
    val status: String,
    val monthlyLlmBudget: Double,
    val usedLlmBudget: Double,
    val healthScore: Double,
    val activeUsersCount: Int,
    val activeAgentsCount: Int,
    val totalTasksCount: Int,
    val errorRatePct: Double,
    val createdAt: Instant = Instant.now()
)

data class StartImpersonationRequest(
    val targetTenantId: String,
    val targetUserId: String? = null,
    val reason: String,
    val ticketReference: String,
    val durationMinutes: Int = 30
)

data class ImpersonationSessionDto(
    val id: String,
    val superAdminId: String,
    val superAdminName: String,
    val targetTenantId: String,
    val targetTenantName: String,
    val targetUserId: String,
    val targetUserName: String,
    val reason: String,
    val ticketReference: String,
    val sessionToken: String,
    val status: String,
    val startedAt: Long,
    val expiresAt: Long,
    val endedAt: Long? = null
)

data class LlmProviderDto(
    val id: String,
    val name: String,
    val providerType: String,
    val isEnabled: Boolean,
    val priority: Int,
    val monthlyBudgetUsd: Double,
    val usedBudgetUsd: Double,
    val baseUrl: String,
    val hasEncryptedApiKey: Boolean
)

data class LlmModelCatalogDto(
    val id: String,
    val providerId: String,
    val modelId: String,
    val displayName: String,
    val contextWindowTokens: Int,
    val costPer1kInputTokensUsd: Double,
    val costPer1kOutputTokensUsd: Double,
    val capabilities: List<String>,
    val isEnabled: Boolean = true
)

data class ModelRoutingRuleDto(
    val id: String,
    val tenantId: String,
    val taskCategory: String,
    val preferredModelId: String,
    val fallbackModelId: String,
    val routingStrategy: String,
    val maxCostPerTaskUsd: Double,
    val maxLatencyMs: Int,
    val maxRetries: Int = 3
)

data class AgentTemplateDto(
    val id: String,
    val category: String,
    val defaultName: String,
    val defaultRoleTitle: String,
    val description: String,
    val baseSystemPrompt: String,
    val defaultSkills: List<String>,
    val allowedMcpTools: List<String>,
    val defaultRiskTier: String,
    val avatarIcon: String,
    val version: String = "1.0.0"
)

data class SkillDto(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val promptSnippet: String,
    val requiredMcpTools: List<String>,
    val defaultRiskTier: String,
    val rolloutStage: String, // "DRAFT", "ALPHA", "BETA", "GA"
    val version: String = "1.0.0",
    val previousVersion: String? = null
)

data class McpToolDetailDto(
    val id: String,
    val name: String,
    val description: String,
    val protocol: String = "mcp/v1",
    val riskLevel: String, // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val requiredSkill: String,
    val timeoutSeconds: Int = 30,
    val isKillSwitched: Boolean = false,
    val restrictedTenants: List<String> = emptyList(),
    val allowedAgentRoles: List<String> = emptyList(),
    val pingLatencyMs: Long = 45,
    val availabilityStatus: String = "ONLINE",
    val totalInvocations: Int = 0
)

data class LlmUsageTelemetryDto(
    val id: String,
    val tenantId: String,
    val provider: String,
    val modelName: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val estimatedCostUsd: Double,
    val latencyMs: Long,
    val status: String,
    val timestamp: Long
)

data class PlatformAnalyticsDto(
    val totalWorkflowsExecuted: Int,
    val averageWorkflowLatencyMs: Long,
    val overallErrorRatePct: Double,
    val humanAgentCollaborationScore: Double,
    val featureAdoptionScores: Map<String, Double>,
    val monthlyRetentionRatePct: Double,
    val netRevenueRetentionPct: Double
)

data class AuditLedgerEntryDto(
    val id: String,
    val tenantId: String,
    val actorName: String,
    val actorRole: String,
    val action: String,
    val entityTarget: String,
    val details: String,
    val impersonationSessionId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
