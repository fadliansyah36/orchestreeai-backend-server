package ai.orchestree.backend.mcptools

import org.slf4j.LoggerFactory

class McpGovernanceEngine {
    private val logger = LoggerFactory.getLogger(McpGovernanceEngine::class.java)

    fun evaluateGovernance(
        tool: McpToolDefinition,
        tenantId: String,
        callerRole: String,
        parameters: Map<String, Any>
    ): Pair<Boolean, String?> {
        // High/Critical risk tools require Manager/Admin role
        if (tool.riskLevel == McpRiskLevel.CRITICAL) {
            if (callerRole != "TENANT_ADMIN" && callerRole != "SUPER_ADMIN") {
                val msg = "Critical tool ${tool.name} requires TENANT_ADMIN or SUPER_ADMIN role (current: $callerRole)"
                logger.warn(msg)
                return false to msg
            }
        }
        return true to null
    }
}
