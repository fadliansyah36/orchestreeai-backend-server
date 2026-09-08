package ai.orchestree.backend.mcptools

class McpToolRegistry {
    private val tools = mutableMapOf<String, McpToolDefinition>()

    companion object {
        fun defaultRegistry(): McpToolRegistry {
            val registry = McpToolRegistry()
            registry.register(
                McpToolDefinition(
                    name = "crm_fetch_lead",
                    description = "Fetches CRM lead record by ID or email",
                    inputSchema = """{"type": "object", "properties": {"leadId": {"type": "string"}}}""",
                    riskLevel = McpRiskLevel.LOW
                )
            )
            registry.register(
                McpToolDefinition(
                    name = "db_query_revenue",
                    description = "Queries aggregated revenue metrics",
                    inputSchema = """{"type": "object", "properties": {"period": {"type": "string"}}}""",
                    riskLevel = McpRiskLevel.LOW
                )
            )
            registry.register(
                McpToolDefinition(
                    name = "payment_refund_transaction",
                    description = "Processes transaction refund via payment gateway",
                    inputSchema = """{"type": "object", "properties": {"trxId": {"type": "string"}, "amount": {"type": "number"}}}""",
                    riskLevel = McpRiskLevel.CRITICAL
                )
            )
            registry.register(
                McpToolDefinition(
                    name = "telegram_send_broadcast",
                    description = "Sends broadcast notification to registered Telegram channels",
                    inputSchema = """{"type": "object", "properties": {"message": {"type": "string"}}}""",
                    riskLevel = McpRiskLevel.MEDIUM
                )
            )
            return registry
        }
    }

    fun register(tool: McpToolDefinition) {
        tools[tool.name] = tool
    }

    fun get(name: String): McpToolDefinition? = tools[name]

    fun listAll(): List<McpToolDefinition> = tools.values.toList()
}
