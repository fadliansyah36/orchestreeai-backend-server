package ai.orchestree.backend

import ai.orchestree.backend.database.repositories.orchestration.WorkflowExecutionRepository
import ai.orchestree.backend.orchestration.NodeExecutionResult
import ai.orchestree.backend.orchestration.NodeExecutionStatus
import ai.orchestree.backend.orchestration.NodeStatus
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.orchestration.WorkflowExecution
import ai.orchestree.backend.orchestration.WorkflowIsolationValidator
import ai.orchestree.backend.orchestration.WorkflowNodeType
import ai.orchestree.backend.orchestration.definitions.WorkflowDefinitions
import ai.orchestree.backend.orchestration.nodes.GenericStepWorkflowNode
import ai.orchestree.backend.orchestration.nodes.HumanApprovalWorkflowNode
import ai.orchestree.backend.orchestration.parseJsonToMap
import ai.orchestree.backend.orchestration.toJson
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Fase106WorkflowCheckpointRecoveryTest {

    @Test
    fun testFiveNodeWorkflowCheckpointingAndExecution() = runBlocking {
        val repo = WorkflowExecutionRepository()
        val engine = OrchestrationEngine(workflowExecutionRepo = repo)

        val result = engine.runWorkflow(
            tenantId = "tenant-test-exec-106",
            workflowDefId = "wf-chief-of-staff-briefing",
            prompt = "Generate Q3 briefing"
        )

        assertEquals("COMPLETED", result.status)
        assertEquals(5, result.nodeRuns.size)

        val nodeIds = result.nodeRuns.map { it.nodeId }
        assertEquals(
            listOf(
                "n1-aggregate-metrics",
                "n2-anomaly-detection",
                "n3-strategic-correlation",
                "n4-synthesize-briefing",
                "n5-distribute-channels"
            ),
            nodeIds
        )

        val saved = repo.getById(result.executionId)
        assertNotNull(saved)
        assertEquals("completed", saved.executionStatus)
        assertEquals("n5-distribute-channels", saved.lastCompletedNodeId)
        assertNotNull(saved.currentStateSnapshot)
    }

    @Test
    fun testServerCrashAfterNodeThreeAndRestartRecovery() = runBlocking {
        val repo = WorkflowExecutionRepository()
        val executedNodes = mutableListOf<String>()

        val n1 = GenericStepWorkflowNode("n1-aggregate-metrics", WorkflowNodeType.TOOL_CALL, nextNodeId = "n2-anomaly-detection") { ctx ->
            executedNodes.add("n1-aggregate-metrics")
            ctx["kpi_revenue"] = 500000
            NodeExecutionResult(NodeExecutionStatus.SUCCESS, "Aggregated KPIs")
        }
        val n2 = GenericStepWorkflowNode("n2-anomaly-detection", WorkflowNodeType.PLAN, nextNodeId = "n3-strategic-correlation") { ctx ->
            executedNodes.add("n2-anomaly-detection")
            ctx["anomaly"] = "Payment latency spike"
            NodeExecutionResult(NodeExecutionStatus.SUCCESS, "Anomalies detected")
        }
        val n3 = GenericStepWorkflowNode("n3-strategic-correlation", WorkflowNodeType.PLAN, nextNodeId = "n4-synthesize-briefing") { ctx ->
            executedNodes.add("n3-strategic-correlation")
            ctx["impact"] = "High priority operational alert"
            NodeExecutionResult(NodeExecutionStatus.SUCCESS, "Strategic correlation done")
        }
        val n4 = GenericStepWorkflowNode("n4-synthesize-briefing", WorkflowNodeType.LLM_GENERATE, nextNodeId = "n5-distribute-channels") { ctx ->
            executedNodes.add("n4-synthesize-briefing")
            ctx["briefing"] = "Executive Alert: Payment latency resolved."
            NodeExecutionResult(NodeExecutionStatus.SUCCESS, "Briefing synthesized")
        }
        val n5 = GenericStepWorkflowNode("n5-distribute-channels", WorkflowNodeType.DELIVER, nextNodeId = null) { ctx ->
            executedNodes.add("n5-distribute-channels")
            ctx["delivered"] = true
            NodeExecutionResult(NodeExecutionStatus.SUCCESS, "Delivered to Slack & Email")
        }

        val engine1 = OrchestrationEngine(workflowExecutionRepo = repo)
        engine1.registerNodes(listOf(n1, n2, n3, n4, n5))

        // Create execution
        val executionId = "exec-crash-test-001"
        val initialContext = mutableMapOf<String, Any>(
            "tenantId" to "tenant-crash-test",
            "prompt" to "Daily briefing",
            "executionId" to executionId
        )
        val execution = WorkflowExecution(
            id = executionId,
            tenantId = "tenant-crash-test",
            workflowDefId = "wf-chief-of-staff-briefing",
            startNodeId = "n1-aggregate-metrics",
            executionStatus = "running",
            currentStateSnapshot = initialContext.toJson()
        )
        execution.context = initialContext
        repo.createExecution(execution)

        // STEP 1: Execute Node 1
        n1.execute(execution.context)
        repo.saveCheckpoint(execution.id, execution.context.toJson(), "n1-aggregate-metrics", "running")

        // STEP 2: Execute Node 2
        n2.execute(execution.context)
        repo.saveCheckpoint(execution.id, execution.context.toJson(), "n2-anomaly-detection", "running")

        // STEP 3: Execute Node 3
        n3.execute(execution.context)
        repo.saveCheckpoint(execution.id, execution.context.toJson(), "n3-strategic-correlation", "running")

        // VERIFY: Exactly 3 nodes have executed before crash
        assertEquals(listOf("n1-aggregate-metrics", "n2-anomaly-detection", "n3-strategic-correlation"), executedNodes)

        // SIMULATE SERVER CRASH (kill -9):
        // Server process dies immediately after Node 3 checkpoint is persisted to disk/DB.
        val snapshotBeforeCrash = repo.getById(executionId)
        assertNotNull(snapshotBeforeCrash)
        assertEquals("n3-strategic-correlation", snapshotBeforeCrash.lastCompletedNodeId)
        assertEquals("running", snapshotBeforeCrash.executionStatus)

        // Simulate time elapsed to trigger recovery scan (older than 5 minutes)
        snapshotBeforeCrash.lastUpdatedAt = System.currentTimeMillis() - (6 * 60 * 1000)

        // STEP 4: SERVER RESTARTS / NYALAKAN ULANG SERVER
        // New server instance starts, recovers interrupted workflows
        val engine2 = OrchestrationEngine(workflowExecutionRepo = repo)
        engine2.registerNodes(listOf(n1, n2, n3, n4, n5))

        // Trigger startup recovery job
        val recoveryResults = recoverInterruptedWorkflows(
            workflowExecutionRepo = repo,
            orchestrationEngine = engine2,
            olderThanMinutes = 5
        )

        assertEquals(1, recoveryResults.size)
        val recoveredResult = recoveryResults.first()

        // VERIFICATION:
        // 1. Recovered workflow completed successfully
        assertEquals("COMPLETED", recoveredResult.status)

        // 2. Nodes executed in the resumed session were ONLY Node 4 and Node 5!
        // Node 1, 2, and 3 were NOT re-executed!
        val resumedNodeRuns = recoveredResult.nodeRuns.map { it.nodeId }
        assertEquals(listOf("n4-synthesize-briefing", "n5-distribute-channels"), resumedNodeRuns)

        // 3. In total, all 5 nodes executed exactly once
        assertEquals(
            listOf(
                "n1-aggregate-metrics",
                "n2-anomaly-detection",
                "n3-strategic-correlation",
                "n4-synthesize-briefing",
                "n5-distribute-channels"
            ),
            executedNodes
        )

        // 4. State was restored correctly from checkpoint snapshot
        val finalRecord = repo.getById(executionId)
        assertNotNull(finalRecord)
        assertEquals("completed", finalRecord.executionStatus)
        assertEquals("n5-distribute-channels", finalRecord.lastCompletedNodeId)

        val restoredMap = parseJsonToMap(finalRecord.currentStateSnapshot!!)
        assertEquals(500000, (restoredMap["kpi_revenue"] as Number).toInt())
        assertEquals("Payment latency spike", restoredMap["anomaly"])
        assertEquals("High priority operational alert", restoredMap["impact"])
        assertEquals("Executive Alert: Payment latency resolved.", restoredMap["briefing"])
        assertEquals(true, restoredMap["delivered"])
    }

    @Test
    fun testHumanApprovalGatePausesWorkflowSafely() = runBlocking {
        val repo = WorkflowExecutionRepository()
        val engine = OrchestrationEngine(workflowExecutionRepo = repo)

        val executionId = "exec-human-approval-test"
        val context = mutableMapOf<String, Any>(
            "tenantId" to "tenant-enterprise-001",
            "prompt" to "Correlate sensitive financial telemetry",
            "executionId" to executionId
        )

        val execution = WorkflowExecution(
            id = executionId,
            tenantId = "tenant-enterprise-001",
            workflowDefId = "wf-enterprise-cross-system-correlation",
            startNodeId = "n1-ingest",
            executionStatus = "running",
            currentStateSnapshot = context.toJson()
        )
        execution.context = context
        repo.createExecution(execution)

        val result = engine.run(execution)

        // Node 4 is HUMAN_APPROVAL ("n4-human-gate")
        assertEquals("PAUSED_FOR_APPROVAL", result.status)
        val saved = repo.getById(executionId)
        assertNotNull(saved)
        assertEquals("paused_for_approval", saved.executionStatus)
        assertEquals("n4-human-gate", saved.lastCompletedNodeId)
    }

    @Test
    fun testWorkflowIsolationAccessControlMatrix() {
        val validator = WorkflowIsolationValidator()
        val enterpriseWf = WorkflowDefinitions.getById("wf-enterprise-cross-system-correlation")!!
        val standardWf = WorkflowDefinitions.getById("wf-task-auto-execute")!!

        // Matrix condition test
        // 1. Enterprise workflow accessed by ENTERPRISE tier -> ALLOWED
        assertTrue(validator.validateTenantAccess("t-1", enterpriseWf, "ENTERPRISE"))

        // 2. Enterprise workflow accessed by GROWTH tier -> BLOCKED
        assertFalse(validator.validateTenantAccess("t-2", enterpriseWf, "GROWTH"))

        // 3. Enterprise workflow accessed by STARTER / FREE_TIER -> BLOCKED
        assertFalse(validator.validateTenantAccess("t-3", enterpriseWf, "FREE_TIER"))

        // 4. Standard workflow accessed by GROWTH tier -> ALLOWED
        assertTrue(validator.validateTenantAccess("t-4", standardWf, "GROWTH"))

        // 5. Standard workflow accessed by ENTERPRISE tier -> ALLOWED
        assertTrue(validator.validateTenantAccess("t-5", standardWf, "ENTERPRISE"))
    }
}
