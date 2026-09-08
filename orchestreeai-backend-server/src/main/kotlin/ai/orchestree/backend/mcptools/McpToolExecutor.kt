package ai.orchestree.backend.mcptools

import ai.orchestree.backend.resilience.executeWithRetry
import org.slf4j.LoggerFactory

class McpToolExecutor(
    private val registry: McpToolRegistry = McpToolRegistry.defaultRegistry(),
    private val governance: McpGovernanceEngine = McpGovernanceEngine()
) {
    private val logger = LoggerFactory.getLogger(McpToolExecutor::class.java)

    suspend fun executeTool(
        toolName: String,
        params: Map<String, Any>,
        tenantId: String = "tenant-default",
        callerRole: String = "STAFF_HUMAN"
    ): McpExecutionResult {
        val startTime = System.currentTimeMillis()
        val tool = registry.get(toolName)
            ?: return McpExecutionResult(
                success = false,
                output = "",
                durationMs = 0,
                riskLevel = McpRiskLevel.LOW,
                errorMessage = "Tool '$toolName' is not registered"
            )

        val (allowed, reason) = governance.evaluateGovernance(tool, tenantId, callerRole, params)
        if (!allowed) {
            return McpExecutionResult(
                success = false,
                output = "",
                durationMs = System.currentTimeMillis() - startTime,
                riskLevel = tool.riskLevel,
                isBlockedByGovernance = true,
                errorMessage = reason
            )
        }

        logger.info("Executing MCP Tool: $toolName with params: $params (risk: ${tool.riskLevel})")

        return try {
            executeWithRetry(
                maxAttempts = 3,
                initialDelayMs = 500L,
                backoffMultiplier = 2.0
            ) {
                val duration = System.currentTimeMillis() - startTime
                val output = when (toolName) {
                    "crm_fetch_lead" -> """{"leadId": "${params["leadId"] ?: "L-101"}", "status": "QUALIFIED", "score": 88}"""
                    "db_query_revenue" -> """{"totalRevenueIdr": 125000000, "growthPercent": 14.5}"""
                    "telegram_send_broadcast" -> """{"delivered": true, "recipientCount": 42}"""
                    else -> """{"result": "SUCCESS", "tool": "$toolName"}"""
                }

                McpExecutionResult(
                    success = true,
                    output = output,
                    durationMs = duration,
                    riskLevel = tool.riskLevel
                )
            }
        } catch (e: Throwable) {
            logger.error("MCP Tool $toolName execution failed after retries: ${e.message}")
            McpExecutionResult(
                success = false,
                output = "",
                durationMs = System.currentTimeMillis() - startTime,
                riskLevel = tool.riskLevel,
                errorMessage = "Execution failed after retry: ${e.message}"
            )
        }
    }
}
