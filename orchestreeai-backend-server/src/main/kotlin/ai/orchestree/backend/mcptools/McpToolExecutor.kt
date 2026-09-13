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

        val finalResult = try {
            executeWithRetry(
                maxAttempts = 3,
                initialDelayMs = 500L,
                backoffMultiplier = 2.0
            ) {
                val duration = System.currentTimeMillis() - startTime
                val strParams = params.mapValues { it.value.toString() }
                val output = when (toolName) {
                    "crm_fetch_lead" -> {
                        val targetLeadId = params["leadId"]?.toString() ?: "L-101"
                        var foundData = ""
                        try {
                            ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                                conn.prepareStatement("SELECT id, customer_display_name, funnel_stage, lead_score FROM leads WHERE id = ? OR customer_identifier = ? LIMIT 1").use { ps ->
                                    ps.setString(1, targetLeadId)
                                    ps.setString(2, targetLeadId)
                                    ps.executeQuery().use { rs ->
                                        if (rs.next()) {
                                            foundData = """{"leadId": "${rs.getString("id")}", "name": "${rs.getString("customer_display_name") ?: "Customer"}", "status": "${rs.getString("funnel_stage") ?: "QUALIFIED"}", "score": ${rs.getDouble("lead_score")}}"""
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.warn("CRM lead fetch DB query failed: ${e.message}")
                        }
                        if (foundData.isNotBlank()) foundData else """{"leadId": "$targetLeadId", "found": false, "status": "NOT_FOUND", "message": "Lead $targetLeadId not found in database"}"""
                    }
                    "db_query_revenue" -> {
                        var totalRevenue = 0.0
                        var orderCount = 0
                        var recentRevenue = 0.0
                        try {
                            ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                                conn.prepareStatement("SELECT COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders WHERE tenant_id = ? OR tenant_id = 'tenant-default'").use { ps ->
                                    ps.setString(1, tenantId)
                                    ps.executeQuery().use { rs ->
                                        if (rs.next()) {
                                            totalRevenue = rs.getDouble(1)
                                            orderCount = rs.getInt(2)
                                        }
                                    }
                                }
                                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 86400 * 1000)
                                conn.prepareStatement("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE (tenant_id = ? OR tenant_id = 'tenant-default') AND created_at >= ?").use { ps ->
                                    ps.setString(1, tenantId)
                                    ps.setLong(2, thirtyDaysAgo)
                                    ps.executeQuery().use { rs ->
                                        if (rs.next()) recentRevenue = rs.getDouble(1)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.warn("Revenue DB query failed: ${e.message}")
                        }
                        val growth = if (totalRevenue > recentRevenue && recentRevenue > 0) ((recentRevenue / (totalRevenue - recentRevenue)) * 100.0) else 0.0
                        """{"tenantId": "$tenantId", "totalRevenueIdr": ${totalRevenue.toLong()}, "orderCount": $orderCount, "recentRevenueIdr": ${recentRevenue.toLong()}, "growthPercent": ${"%.2f".format(java.util.Locale.US, growth)}}"""
                    }
                    "telegram_send_broadcast" -> {
                        val messageText = params["message"]?.toString() ?: "Broadcast notification from OrchestreeAI"
                        val chatId = params["chatId"]?.toString() ?: ""
                        val telegramService = ai.orchestree.backend.channels.adapters.TelegramOfficialBotService()
                        val sendResult = if (chatId.isNotBlank()) {
                            telegramService.sendMessage(chatId, messageText)
                        } else null
                        """{"delivered": ${sendResult?.ok ?: false}, "recipientCount": ${if (chatId.isNotBlank()) 1 else 0}, "status": "${if (sendResult?.ok == true) "DISPATCHED" else "FAILED"}"}"""
                    }
                    "product.recommend", "product.search" -> ai.orchestree.backend.sales.RealProductRecommendTool().execute(tenantId, strParams)
                    "cart.create" -> ai.orchestree.backend.sales.RealCartCreateTool().execute(tenantId, strParams)
                    "order.create" -> ai.orchestree.backend.sales.RealOrderCreateTool().execute(tenantId, strParams)
                    "invoice.generate" -> ai.orchestree.backend.sales.RealInvoiceGenerateTool().execute(tenantId, strParams)
                    "discount.apply", "payment_discount_apply" -> ai.orchestree.backend.sales.RealDiscountApplyTool().execute(tenantId, strParams)
                    "refund.process", "payment_refund_transaction" -> ai.orchestree.backend.sales.RealRefundProcessTool().execute(tenantId, strParams)
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

        logExecutionToDb(tenantId, toolName, params, finalResult)
        return finalResult
    }

    private fun logExecutionToDb(
        tenantId: String,
        toolName: String,
        params: Map<String, Any>,
        result: McpExecutionResult
    ) {
        try {
            val logId = "mcplog-${java.util.UUID.randomUUID().toString().take(8)}"
            val paramsJson = params.entries.joinToString(prefix = "{", postfix = "}") {
                "\"${it.key}\": \"${it.value.toString().replace("\"", "\\\"")}\""
            }
            ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                conn.prepareStatement("""
                    INSERT INTO mcp_tool_execution_logs (
                        id, tenant_id, tool_id, tool_name, caller_agent_id, task_id, workflow_execution_id,
                        input_params_json, output_result_json, execution_time_ms, is_success, error_message,
                        risk_level, human_approved_by, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """).use { ps ->
                    ps.setString(1, logId)
                    ps.setString(2, tenantId)
                    ps.setString(3, toolName)
                    ps.setString(4, toolName)
                    ps.setString(5, "agent-orchestrator")
                    ps.setString(6, "task-auto")
                    ps.setString(7, "wf-auto")
                    ps.setString(8, paramsJson)
                    ps.setString(9, result.output.take(500))
                    ps.setLong(10, result.durationMs)
                    ps.setBoolean(11, result.success)
                    ps.setString(12, result.errorMessage)
                    ps.setString(13, result.riskLevel.name)
                    ps.setString(14, if (result.isBlockedByGovernance) "BLOCKED" else "SYSTEM_APPROVED")
                    ps.setLong(15, System.currentTimeMillis())
                    ps.executeUpdate()
                }

                conn.prepareStatement("UPDATE mcp_tools SET total_invocations = total_invocations + 1 WHERE name = ? OR tool_code = ?").use { ps ->
                    ps.setString(1, toolName)
                    ps.setString(2, toolName)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            logger.warn("Log MCP tool execution notice: ${e.message}")
        }
    }
}
