package ai.orchestree.backend.orchestration.definitions

import ai.orchestree.backend.orchestration.WorkflowNodeType
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowNodeDef(
    val nodeId: String,
    val name: String,
    val type: WorkflowNodeType,
    val orderIndex: Int,
    val configJson: String = "{}"
)

@Serializable
data class WorkflowDefinition(
    val id: String,
    val name: String,
    val domain: String,
    val description: String,
    val requiresEnterpriseTier: Boolean = false,
    val nodes: List<WorkflowNodeDef> = emptyList()
)

object WorkflowDefinitions {
    val ALL_WORKFLOWS: List<WorkflowDefinition> = listOf(
        WorkflowDefinition(
            id = "wf-task-auto-execute",
            name = "Task Autonomous Execution",
            domain = "taskboard",
            description = "Decomposes a user task, plans subtasks, executes via MCP tools, and delivers output",
            nodes = listOf(
                WorkflowNodeDef("n1-classify", "Classify Intent & Modality", WorkflowNodeType.CLASSIFY, 0),
                WorkflowNodeDef("n2-plan", "Plan Execution DAG", WorkflowNodeType.PLAN, 1),
                WorkflowNodeDef("n3-tools", "Execute MCP Tools", WorkflowNodeType.TOOL_CALL, 2),
                WorkflowNodeDef("n4-synthesize", "Synthesize Output via LLM", WorkflowNodeType.LLM_GENERATE, 3),
                WorkflowNodeDef("n5-deliver", "Deliver Final Result", WorkflowNodeType.DELIVER, 4)
            )
        ),
        WorkflowDefinition(
            id = "wf-competitor-audit",
            name = "Competitor Intelligence & Crawl",
            domain = "salesmarketing",
            description = "Crawls competitor web signals, extracts pricing/features diffs, and synthesizes intelligence report",
            nodes = listOf(
                WorkflowNodeDef("n1-crawl", "Fetch Competitor Signals", WorkflowNodeType.TOOL_CALL, 0),
                WorkflowNodeDef("n2-extract-diff", "Diff Feature & Pricing", WorkflowNodeType.LLM_GENERATE, 1),
                WorkflowNodeDef("n3-deliver", "Publish Strategy Brief", WorkflowNodeType.DELIVER, 2)
            )
        ),
        WorkflowDefinition(
            id = "wf-marketing-campaign",
            name = "Creative Studio & Campaign Generation",
            domain = "generativestudio",
            description = "Plans omnichannel creative assets, generates visual creatives, applies guardrails, and renders output",
            nodes = listOf(
                WorkflowNodeDef("n1-plan", "Content Planning & Copywriting", WorkflowNodeType.PLAN, 0),
                WorkflowNodeDef("n2-image-gen", "Generate Visual Assets", WorkflowNodeType.TOOL_CALL, 1),
                WorkflowNodeDef("n3-guardrail", "Compliance & Brand Guardrail", WorkflowNodeType.LLM_GENERATE, 2),
                WorkflowNodeDef("n4-deliver", "Schedule / Publish", WorkflowNodeType.DELIVER, 3)
            )
        ),
        WorkflowDefinition(
            id = "wf-enterprise-cross-system-correlation",
            name = "Enterprise Cross-System Correlation",
            domain = "enterprise",
            description = "Correlates telemetry from workforce, sales, CRM, and financial data for executive decision support",
            requiresEnterpriseTier = true,
            nodes = listOf(
                WorkflowNodeDef("n1-ingest", "Ingest Telemetry Signals", WorkflowNodeType.TOOL_CALL, 0),
                WorkflowNodeDef("n2-correlate", "Correlate Cross-Domain Matrix", WorkflowNodeType.PLAN, 1),
                WorkflowNodeDef("n3-risk-eval", "Calculate Enterprise Risk Score", WorkflowNodeType.LLM_GENERATE, 2),
                WorkflowNodeDef("n4-human-gate", "Dual-Control Governance Approval", WorkflowNodeType.HUMAN_APPROVAL, 3),
                WorkflowNodeDef("n5-deliver", "Distribute Executive Briefing", WorkflowNodeType.DELIVER, 4)
            )
        ),
        WorkflowDefinition(
            id = "wf-chief-of-staff-briefing",
            name = "AI Chief of Staff Briefing",
            domain = "executive",
            description = "Aggregates enterprise telemetry, detects anomalies, correlates strategy, synthesizes briefing, and delivers to channels",
            nodes = listOf(
                WorkflowNodeDef("n1-aggregate-metrics", "Aggregate Enterprise Telemetry & KPIs", WorkflowNodeType.TOOL_CALL, 0),
                WorkflowNodeDef("n2-anomaly-detection", "Detect Anomalies & Outliers", WorkflowNodeType.PLAN, 1),
                WorkflowNodeDef("n3-strategic-correlation", "Correlate Strategic Initiatives", WorkflowNodeType.PLAN, 2),
                WorkflowNodeDef("n4-synthesize-briefing", "Synthesize Executive Briefing via LLM", WorkflowNodeType.LLM_GENERATE, 3),
                WorkflowNodeDef("n5-distribute-channels", "Deliver to Executive Channels", WorkflowNodeType.DELIVER, 4)
            )
        ),
        WorkflowDefinition(
            id = "wf-proactive-briefing",
            name = "Proactive Daily Executive Briefing",
            domain = "executive",
            description = "Proactive daily telemetry aggregation, anomaly detection, strategic correlation, and multi-channel executive briefing",
            nodes = listOf(
                WorkflowNodeDef("n1-aggregate-metrics", "Aggregate Enterprise Telemetry & KPIs", WorkflowNodeType.TOOL_CALL, 0),
                WorkflowNodeDef("n2-anomaly-detection", "Detect Anomalies & Outliers", WorkflowNodeType.PLAN, 1),
                WorkflowNodeDef("n3-strategic-correlation", "Correlate Strategic Initiatives", WorkflowNodeType.PLAN, 2),
                WorkflowNodeDef("n4-synthesize-briefing", "Synthesize Executive Briefing via LLM", WorkflowNodeType.LLM_GENERATE, 3),
                WorkflowNodeDef("n5-distribute-channels", "Deliver to Executive Channels", WorkflowNodeType.DELIVER, 4)
            )
        ),
        WorkflowDefinition(
            id = "wf-world-monitor-scan",
            name = "World Monitor Macro Trend Scan",
            domain = "intelligence",
            description = "Scans macro-economic indicators, supply chain shifts, and regional market trends",
            requiresEnterpriseTier = true,
            nodes = listOf(
                WorkflowNodeDef("n1-scan-signals", "Ingest Global Signals", WorkflowNodeType.TOOL_CALL, 0),
                WorkflowNodeDef("n2-cluster-trends", "Cluster Emerging Trends", WorkflowNodeType.PLAN, 1),
                WorkflowNodeDef("n3-synthesize-radar", "Synthesize Macro Radar", WorkflowNodeType.LLM_GENERATE, 2),
                WorkflowNodeDef("n4-deliver", "Publish World Monitor Intel", WorkflowNodeType.DELIVER, 3)
            )
        ),
        WorkflowDefinition(
            id = "universal_ai_selection",
            name = "Universal AI Selection & Ranking Workflow",
            domain = "selection",
            description = "10-stage universal AI selection, scoring, ranking, analysis, and delivery workflow",
            nodes = listOf(
                WorkflowNodeDef("n1-read-data", "READ_DATA", WorkflowNodeType.TOOL_CALL, 0),
                WorkflowNodeDef("n2-understand", "UNDERSTAND", WorkflowNodeType.LLM_GENERATE, 1),
                WorkflowNodeDef("n3-validate", "VALIDATE", WorkflowNodeType.PLAN, 2),
                WorkflowNodeDef("n4-select", "SELECT", WorkflowNodeType.PLAN, 3),
                WorkflowNodeDef("n5-score", "SCORE", WorkflowNodeType.LLM_GENERATE, 4),
                WorkflowNodeDef("n6-rank", "RANK", WorkflowNodeType.PLAN, 5),
                WorkflowNodeDef("n7-analyze", "ANALYZE", WorkflowNodeType.LLM_GENERATE, 6),
                WorkflowNodeDef("n8-visualize", "VISUALIZE", WorkflowNodeType.PLAN, 7),
                WorkflowNodeDef("n9-recommend", "RECOMMEND", WorkflowNodeType.LLM_GENERATE, 8),
                WorkflowNodeDef("n10-result", "RESULT", WorkflowNodeType.DELIVER, 9)
            )
        ),
        WorkflowDefinition(
            id = "wf-chat-inbound",
            name = "Inbound Conversational AI",
            domain = "chat",
            description = "Multi-turn conversational chat with intent classification, RAG retrieval, LLM synthesis, and grounding validation",
            nodes = listOf(
                WorkflowNodeDef("chat-n1-classify", "Classify User Intent", WorkflowNodeType.CLASSIFY, 0),
                WorkflowNodeDef("chat-n2-rag", "Retrieve Company Brain RAG", WorkflowNodeType.TOOL_CALL, 1),
                WorkflowNodeDef("chat-n3-synthesize", "Synthesize Grounded Response", WorkflowNodeType.LLM_GENERATE, 2),
                WorkflowNodeDef("chat-n4-validate", "Validate Output Grounding", WorkflowNodeType.PLAN, 3),
                WorkflowNodeDef("chat-n5-deliver", "Deliver Final Reply", WorkflowNodeType.DELIVER, 4)
            )
        ),
        WorkflowDefinition(
            id = "wf-omnichannel-sales-persona",
            name = "Omnichannel Sales & Consultative AI Employee Workflow",
            domain = "salesmarketing",
            description = "Empathetic Cekat.ai-style customer engagement, consultative qualification, catalog grounding, and commerce actions",
            nodes = listOf(
                WorkflowNodeDef("sales-n1-identity", "Resolve Customer Identity", WorkflowNodeType.TOOL_CALL, 0),
                WorkflowNodeDef("sales-n2-classify", "Classify Sales Intent & Persona", WorkflowNodeType.CLASSIFY, 1),
                WorkflowNodeDef("sales-n3-grounding", "Catalog Grounding & Stock Check", WorkflowNodeType.TOOL_CALL, 2),
                WorkflowNodeDef("sales-n4-persona-llm", "Synthesize Empathetic Persona Reply via ModelRouter", WorkflowNodeType.LLM_GENERATE, 3),
                WorkflowNodeDef("sales-n5-deliver", "Deliver to Omnichannel Customer", WorkflowNodeType.DELIVER, 4)
            )
        )
    )

    fun getById(id: String): WorkflowDefinition? = ALL_WORKFLOWS.find { it.id == id }
}
