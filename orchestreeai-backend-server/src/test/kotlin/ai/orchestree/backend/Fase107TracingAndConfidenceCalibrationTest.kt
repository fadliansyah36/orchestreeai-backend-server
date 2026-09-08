package ai.orchestree.backend

import ai.orchestree.backend.database.repositories.intelligence.AgentDecisionOutcomeRecord
import ai.orchestree.backend.database.repositories.intelligence.AgentDecisionOutcomeRepository
import ai.orchestree.backend.database.repositories.intelligence.ConfidenceCalibrationRecord
import ai.orchestree.backend.database.repositories.intelligence.ConfidenceCalibrationRepository
import ai.orchestree.backend.database.repositories.orchestration.PendingApprovalRepository
import ai.orchestree.backend.database.repositories.orchestration.WorkflowExecutionRepository
import ai.orchestree.backend.intelligence.ConfidenceEngine
import ai.orchestree.backend.mcptools.McpToolExecutor
import ai.orchestree.backend.mcptools.McpToolRegistry
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.observability.WorkflowTracingManager
import ai.orchestree.backend.orchestration.NodeExecutionResult
import ai.orchestree.backend.orchestration.NodeExecutionStatus
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.orchestration.WorkflowExecution
import ai.orchestree.backend.orchestration.WorkflowNode
import ai.orchestree.backend.orchestration.WorkflowNodeType
import ai.orchestree.backend.scheduler.jobs.ConfidenceCalibrationJob
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class Fase107TracingAndConfidenceCalibrationTest {

    private lateinit var engine: OrchestrationEngine
    private lateinit var outcomeRepo: AgentDecisionOutcomeRepository
    private lateinit var calibrationRepo: ConfidenceCalibrationRepository
    private lateinit var calibrationJob: ConfidenceCalibrationJob
    private lateinit var confidenceEngine: ConfidenceEngine

    @BeforeEach
    fun setUp() {
        engine = OrchestrationEngine(
            modelRouter = ModelRouter(),
            mcpExecutor = McpToolExecutor(McpToolRegistry.defaultRegistry()),
            workflowExecutionRepo = WorkflowExecutionRepository(),
            pendingApprovalRepo = PendingApprovalRepository()
        )
        outcomeRepo = AgentDecisionOutcomeRepository()
        outcomeRepo.clear()

        calibrationRepo = ConfidenceCalibrationRepository()
        calibrationRepo.clear()

        calibrationJob = ConfidenceCalibrationJob(outcomeRepo, calibrationRepo)
        confidenceEngine = ConfidenceEngine(calibrationRepo)
    }

    @Test
    fun testWorkflowExecutionProducesOpenTelemetrySpansPerNode() = runBlocking {
        // Setup a 3-node workflow
        val node1 = object : WorkflowNode {
            override val id = "n1-classify"
            override val type = WorkflowNodeType.CLASSIFY
            override suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult {
                context["intent"] = "SUPPORT_BILLING"
                return NodeExecutionResult(status = NodeExecutionStatus.SUCCESS, output = "Billing question")
            }
            override fun next(context: Map<String, Any>) = "n2-plan"
        }

        val node2 = object : WorkflowNode {
            override val id = "n2-plan"
            override val type = WorkflowNodeType.PLAN
            override suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult {
                context["actionPlan"] = "FETCH_INVOICE"
                return NodeExecutionResult(status = NodeExecutionStatus.SUCCESS, output = "Fetch invoice")
            }
            override fun next(context: Map<String, Any>) = "n3-deliver"
        }

        val node3 = object : WorkflowNode {
            override val id = "n3-deliver"
            override val type = WorkflowNodeType.DELIVER
            override suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult {
                return NodeExecutionResult(status = NodeExecutionStatus.SUCCESS, output = "Invoice delivered")
            }
            override fun next(context: Map<String, Any>) = null
        }

        engine.registerNode(node1)
        engine.registerNode(node2)
        engine.registerNode(node3)

        val executionId = "exec-trace-${UUID.randomUUID().toString().take(8)}"
        val execution = WorkflowExecution(
            id = executionId,
            tenantId = "tenant-enterprise-1",
            workflowDefId = "wf-customer-billing",
            startNodeId = "n1-classify"
        ).apply {
            context["userId"] = "user-99"
        }

        val res = engine.run(execution)
        assertEquals("COMPLETED", res.status)
        assertEquals(3, res.nodeRuns.size)

        // Verify OpenTelemetry Spans were recorded
        val spans = WorkflowTracingManager.getSpansForExecution(executionId)
        assertEquals(3, spans.size, "Must have exactly 1 OpenTelemetry span per node execution")

        // Spans must share same traceId
        val traceId = spans.first().traceId
        assertTrue(traceId.isNotBlank(), "Trace ID must not be blank")
        assertTrue(spans.all { it.traceId == traceId }, "All spans in workflow execution must share identical trace_id")

        // Verify node 1 span
        val s1 = spans.first { it.nodeId == "n1-classify" }
        assertEquals("CLASSIFY", s1.nodeType)
        assertEquals("SUCCESS", s1.status)
        assertEquals("tenant-enterprise-1", s1.tenantId)
        assertTrue(s1.durationMs >= 0)

        // Verify node 2 span
        val s2 = spans.first { it.nodeId == "n2-plan" }
        assertEquals("PLAN", s2.nodeType)
        assertEquals("SUCCESS", s2.status)

        // Verify node 3 span
        val s3 = spans.first { it.nodeId == "n3-deliver" }
        assertEquals("DELIVER", s3.nodeType)
        assertEquals("SUCCESS", s3.status)

        // Verify execution summary can be retrieved
        val summary = WorkflowTracingManager.getAllExecutionSummaries().find { it.executionId == executionId }
        assertNotNull(summary, "Execution summary must be recorded")
        assertEquals(3, summary!!.nodeCount)
        assertEquals(traceId, summary.traceId)
    }

    @Test
    fun testConfidenceCalibrationJobBucketsAndDetectsMiscalibration() = runBlocking {
        val tenantId = "tenant-calib-test"

        // Simulate historical decisions for 80% confidence bucket:
        // Claimed 80% (80.0 to 89.0), but only 2 out of 5 were reinforced (actual accuracy = 40.0%)
        // Deviation = abs(80 - 40) = 40% > 15% -> MUST be flagged as UNCALIBRATED / MISCALIBRATED!
        outcomeRepo.recordOutcome(AgentDecisionOutcomeRecord(id = "d1", tenantId = tenantId, confidence = 82.0, outcomeClassification = "REINFORCE"))
        outcomeRepo.recordOutcome(AgentDecisionOutcomeRecord(id = "d2", tenantId = tenantId, confidence = 85.0, outcomeClassification = "REINFORCE"))
        outcomeRepo.recordOutcome(AgentDecisionOutcomeRecord(id = "d3", tenantId = tenantId, confidence = 80.0, outcomeClassification = "CORRECT"))
        outcomeRepo.recordOutcome(AgentDecisionOutcomeRecord(id = "d4", tenantId = tenantId, confidence = 88.0, outcomeClassification = "CORRECT"))
        outcomeRepo.recordOutcome(AgentDecisionOutcomeRecord(id = "d5", tenantId = tenantId, confidence = 84.0, outcomeClassification = "CORRECT"))

        // Simulate historical decisions for 90% confidence bucket:
        // Claimed 90%, 9 out of 10 reinforced (actual accuracy = 90.0%)
        // Deviation = 0% <= 15% -> MUST be CALIBRATED!
        repeat(9) { idx ->
            outcomeRepo.recordOutcome(AgentDecisionOutcomeRecord(id = "d-good-$idx", tenantId = tenantId, confidence = 92.0, outcomeClassification = "REINFORCE"))
        }
        outcomeRepo.recordOutcome(AgentDecisionOutcomeRecord(id = "d-good-err", tenantId = tenantId, confidence = 95.0, outcomeClassification = "CORRECT"))

        // Execute weekly calibration job
        val calibrations = calibrationJob.calibrateConfidenceScores(tenantId)
        assertTrue(calibrations.isNotEmpty())

        val bucket80 = calibrations.find { it.confidenceBucket == 80 }
        assertNotNull(bucket80, "Bucket 80% must exist")
        assertEquals(5, bucket80!!.sampleSize)
        assertEquals(40.0, bucket80.actualAccuracy, 0.1)
        assertEquals(40.0, bucket80.deviation, 0.1)
        assertFalse(bucket80.isCalibrated, "Bucket 80% with 40% deviation MUST be marked as NOT calibrated")

        val bucket90 = calibrations.find { it.confidenceBucket == 90 }
        assertNotNull(bucket90, "Bucket 90% must exist")
        assertEquals(10, bucket90!!.sampleSize)
        assertEquals(90.0, bucket90.actualAccuracy, 0.1)
        assertEquals(0.0, bucket90.deviation, 0.1)
        assertTrue(bucket90.isCalibrated, "Bucket 90% with 0% deviation MUST be marked as calibrated")
    }

    @Test
    fun testConfidenceEngineCalculatesOutputConfidenceAndAuditReport() = runBlocking {
        val tenantId = "tenant-audit-test"

        // Inject calibration record where 80% has actual 60% accuracy (20% deviation -> uncalibrated)
        calibrationRepo.record(
            tenantId = tenantId,
            bucket = 80,
            actualAccuracy = 60.0,
            sampleSize = 25
        )

        // Inject calibrated record for 90% (88% actual accuracy -> 2% deviation -> calibrated)
        calibrationRepo.record(
            tenantId = tenantId,
            bucket = 90,
            actualAccuracy = 88.0,
            sampleSize = 30
        )

        val auditReport = confidenceEngine.generateAuditReport(tenantId)
        assertEquals(tenantId, auditReport.tenantId)
        assertEquals(1, auditReport.miscalibratedBucketsCount)
        assertEquals(55, auditReport.totalSamplesAudited)
        assertTrue(auditReport.auditSummary.contains("Klaim 80%"), "Audit summary must include miscalibrated bucket details")

        // Test calculateOutputConfidence
        val calibScore = confidenceEngine.calculateOutputConfidence(
            llmResponseLength = 100,
            modelUsed = "gemini-2.0-flash",
            durationMs = 2000L,
            claimedConfidencePct = 82.0,
            tenantId = tenantId
        )
        // Since bucket 80 is uncalibrated with actual 60%, calibratedConfidencePct should reflect empirical accuracy (60.0%)
        assertEquals(60.0, calibScore.calibratedConfidencePct, 0.1, "Confidence score must be empirically adjusted to real ground-truth accuracy")
        assertFalse(calibScore.isCalibrated, "Score in bucket 80 should indicate uncalibrated state")
        assertNotNull(calibScore.calibrationWarning, "Score should contain calibration warning")
    }
}
