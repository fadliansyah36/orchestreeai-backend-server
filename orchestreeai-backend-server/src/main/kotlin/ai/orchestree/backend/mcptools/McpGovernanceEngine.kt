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
        // 1. Emergency Kill-Switch Check (Database & Admin Store)
        if (isKillSwitched(tool.name)) {
            val msg = "Tool '${tool.name}' is disabled by emergency kill-switch"
            logger.warn(msg)
            return false to msg
        }

        // 2. High/Critical risk tools require Manager/Admin role
        if (tool.riskLevel == McpRiskLevel.CRITICAL) {
            if (callerRole != "TENANT_ADMIN" && callerRole != "SUPER_ADMIN") {
                val msg = "Critical tool ${tool.name} requires TENANT_ADMIN or SUPER_ADMIN role (current: $callerRole)"
                logger.warn(msg)
                return false to msg
            }
        }
        return true to null
    }

    fun isKillSwitched(toolName: String): Boolean {
        try {
            ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                conn.prepareStatement("SELECT is_kill_switched FROM mcp_tools WHERE name = ? OR tool_code = ? OR id = ? LIMIT 1").use { ps ->
                    ps.setString(1, toolName)
                    ps.setString(2, toolName)
                    ps.setString(3, toolName)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            return rs.getBoolean("is_kill_switched")
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback check against in-memory admin store
        val toolInStore = ai.orchestree.backend.api.AdminDomainStores.mcpTools.find { it.name == toolName || it.id == toolName }
        if (toolInStore != null && (toolInStore.status == "DISABLED_BY_KILLSWITCH" || toolInStore.status == "DISABLED")) {
            return true
        }
        return false
    }
}
