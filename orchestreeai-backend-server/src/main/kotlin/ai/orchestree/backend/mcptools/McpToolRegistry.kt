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
            registry.register(
                McpToolDefinition(
                    name = "product.recommend",
                    description = "Recommends catalog products tailored to customer profile and category",
                    inputSchema = """{"type": "object", "properties": {"category": {"type": "string"}}}""",
                    riskLevel = McpRiskLevel.LOW
                )
            )
            registry.register(
                McpToolDefinition(
                    name = "product.search",
                    description = "Searches official products and inventory status by keyword or SKU",
                    inputSchema = """{"type": "object", "properties": {"query": {"type": "string"}}}""",
                    riskLevel = McpRiskLevel.LOW
                )
            )
            registry.register(
                McpToolDefinition(
                    name = "cart.create",
                    description = "Creates shopping cart with items for customer checkout",
                    inputSchema = """{"type": "object", "properties": {"customer_id": {"type": "string"}, "items": {"type": "string"}}}""",
                    riskLevel = McpRiskLevel.MEDIUM
                )
            )
            registry.register(
                McpToolDefinition(
                    name = "order.create",
                    description = "Converts shopping cart into formal order with shipping and payment details",
                    inputSchema = """{"type": "object", "properties": {"cart_id": {"type": "string"}, "customer_name": {"type": "string"}}}""",
                    riskLevel = McpRiskLevel.HIGH
                )
            )
            registry.register(
                McpToolDefinition(
                    name = "invoice.generate",
                    description = "Retrieves and formats official digital invoice for verified order",
                    inputSchema = """{"type": "object", "properties": {"order_number": {"type": "string"}}}""",
                    riskLevel = McpRiskLevel.LOW
                )
            )
            registry.register(
                McpToolDefinition(
                    name = "discount.apply",
                    description = "Evaluates and applies promotional discount within governance thresholds",
                    inputSchema = """{"type": "object", "properties": {"requested_discount_pct": {"type": "number"}, "order_or_cart_id": {"type": "string"}}}""",
                    riskLevel = McpRiskLevel.HIGH
                )
            )
            registry.register(
                McpToolDefinition(
                    name = "refund.process",
                    description = "Submits refund request for human manager approval",
                    inputSchema = """{"type": "object", "properties": {"refund_amount": {"type": "number"}, "order_id": {"type": "string"}}}""",
                    riskLevel = McpRiskLevel.CRITICAL
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
