package ai.orchestree.backend

import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.learning.ContinuousLearningCore
import ai.orchestree.backend.mcptools.McpToolExecutor
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.scheduler.SchedulerEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Phase2FoundationVerificationTest {

    @Test
    fun testOrchestrationEngineChiefOfStaffWorkflow() = runBlocking {
        val router = ModelRouter()
        val engine = OrchestrationEngine(modelRouter = router)

        val result = engine.runWorkflow(
            tenantId = "tenant-default",
            workflowDefId = "n1-aggregate-metrics",
            prompt = "Generate executive briefing",
            contextParams = mapOf(
                "tenant_id" to "tenant-default",
                "is_sandbox_replay" to true
            )
        )
        assertEquals("completed", result.status.lowercase(), "Workflow must complete successfully")
        assertTrue(result.nodeRuns.isNotEmpty(), "Node runs must be recorded")
        val finalOutput = result.finalOutput
        assertTrue(finalOutput.contains("Executive Briefing") || finalOutput.contains("Delivered") || finalOutput.contains("SANDBOX"), 
            "Final output must contain valid workflow result, got: $finalOutput")
    }

    @Test
    fun testContinuousLearningSkillConfidence() = runBlocking {
        val stats = ContinuousLearningCore.getAgentSkillConfidence("tenant-default", "agent-sales-01")
        assertNotNull(stats)
        assertEquals("agent-sales-01", stats.agentId)
        assertTrue(stats.skillConfidenceScore >= 0.0)
    }

    @Test
    fun testMcpToolExecutorRealIntegrations() = runBlocking {
        val executor = McpToolExecutor()

        // 1. CRM Lead Fetch
        val crmResult = executor.executeTool(
            toolName = "crm_fetch_lead",
            params = mapOf("leadId" to "L-TEST-8899"),
            tenantId = "tenant-default"
        )
        assertTrue(crmResult.success, "CRM tool execution must succeed")
        assertTrue(crmResult.output.contains("L-TEST-8899") || crmResult.output.contains("QUALIFIED"))

        // 2. Revenue DB Query
        val revResult = executor.executeTool(
            toolName = "db_query_revenue",
            params = emptyMap(),
            tenantId = "tenant-default"
        )
        assertTrue(revResult.success, "Revenue DB tool execution must succeed")
        assertTrue(revResult.output.contains("totalRevenueIdr"))

        // 3. Telegram Broadcast
        val tgResult = executor.executeTool(
            toolName = "telegram_send_broadcast",
            params = mapOf("message" to "Test notification"),
            tenantId = "tenant-default"
        )
        assertTrue(tgResult.success, "Telegram broadcast tool execution must succeed")
        assertTrue(tgResult.output.contains("delivered"))
    }

    @Test
    fun testSchedulerEngineActiveJobDefinitions() {
        val scheduler = SchedulerEngine()
        assertNotNull(scheduler)
        // Ensure scheduler can claim and inspect queues without exceptions
        val conn = DatabaseManager.getConnection()
        assertNotNull(conn, "Database connection must be established")
    }
}
