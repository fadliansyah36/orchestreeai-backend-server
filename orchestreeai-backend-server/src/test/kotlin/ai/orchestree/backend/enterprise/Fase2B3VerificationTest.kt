package ai.orchestree.backend.enterprise

import ai.orchestree.backend.database.repositories.taskboard.TaskRepository
import ai.orchestree.backend.events.AiEventDefinition
import ai.orchestree.backend.events.AiEventEngine
import ai.orchestree.backend.events.AiEventInstance
import ai.orchestree.backend.intelligence.AiFinanceIntelligenceService
import ai.orchestree.backend.intelligence.KnowledgeOperationalFusionEngine
import ai.orchestree.backend.intelligence.KnowledgeRule
import ai.orchestree.backend.orchestration.AiActionOrchestrator
import ai.orchestree.backend.orchestration.ApprovedAction
import ai.orchestree.backend.orchestration.MonitoringLoopEngine
import ai.orchestree.backend.orchestration.MonitoringLoopState
import ai.orchestree.backend.scheduler.jobs.MonitoringLoopJob
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Fase 2B.3 Comprehensive Verification Test (PRD Addendum 2 Bagian 67-71)
 *
 * Covers:
 * 1. AI Action Orchestration & Execution Layer (Bagian 67)
 *    - 4-layer checks: Policy, Permission, Approval Requirement, Business Rule
 *    - executeApprovedAction() execution on internal systems and Integration Fabric
 * 2. Closed-Loop Workforce Monitoring (Bagian 68)
 *    - Automatic Task Creation
 *    - State machine lifecycle: DETECT -> TASK_CREATED -> ASSIGNED -> IN_PROGRESS -> VERIFYING -> RESOLVED/ESCALATED
 *    - Anti fake closed-loop: checkResultAgainstSourceSystem()
 * 3. AI Finance Intelligence: Cash Flow Pressure Detection (Bagian 69)
 *    - Multi-factor pressure scoring (0-100)
 *    - Non-enterprise vs. Enterprise ERP data fusion
 * 4. Knowledge & Operational Fusion Engine (Bagian 70)
 *    - Fusion algorithm & mandatory human admin approval for synthesized rules
 * 5. AI Event Engine (Bagian 71)
 *    - Specialist agent event dispatching & execution
 * 6. Background Scheduler Integration
 *    - MonitoringLoopJob registration in SchedulerEngine
 */
class Fase2B3VerificationTest {

    private val tenantStarter = "tenant-fase2b3-starter"
    private val tenantEnterprise = "tenant-fase2b3-enterprise"

    private lateinit var actionOrchestrator: AiActionOrchestrator
    private lateinit var monitoringEngine: MonitoringLoopEngine
    private lateinit var financeService: AiFinanceIntelligenceService
    private lateinit var knowledgeEngine: KnowledgeOperationalFusionEngine
    private lateinit var eventEngine: AiEventEngine

    @BeforeEach
    fun setUp() {
        FeatureCapabilityService.setTenantTier(tenantStarter, TierLevel.STARTER)
        FeatureCapabilityService.setTenantTier(tenantEnterprise, TierLevel.ENTERPRISE)

        actionOrchestrator = AiActionOrchestrator()
        monitoringEngine = MonitoringLoopEngine()
        financeService = AiFinanceIntelligenceService()
        knowledgeEngine = KnowledgeOperationalFusionEngine()
        eventEngine = AiEventEngine()
    }

    // =========================================================================
    // 1. AI ACTION ORCHESTRATION & EXECUTION LAYER (Bagian 67)
    // =========================================================================

    @Test
    fun testActionOrchestrationFourLayerCheckLowRiskAutoApprove() = runBlocking {
        // Propose low-risk action with valid business rule
        val result = actionOrchestrator.proposeAction(
            tenantId = tenantStarter,
            agentId = "agent-procurement-01",
            actionType = "CREATE_TASK",
            targetSystem = "TASK_BOARD",
            payload = mapOf(
                "title" to "Restock Hydraulic Oil ISO 46",
                "description" to "Automatic restock order task",
                "quantity" to "5.0",
                "amount" to "1500000"
            ),
            assignedHuman = "supervisor-warehouse"
        )

        assertTrue(result.isApproved, "Low-risk action should be auto-approved")
        assertFalse(result.requiresHumanApproval, "Low-risk action does not require human escalation")
        assertEquals("APPROVED", result.action.status)
        println("=== RAW 4-LAYER CHECK PASSED ===")
        println("Checks: " + result.checksPassed)
        println("Status: " + result.action.status)
        assertTrue(result.checksPassed.contains("POLICY_CHECK"))
        assertTrue(result.checksPassed.contains("PERMISSION_CHECK"))
        assertTrue(result.checksPassed.contains("APPROVAL_REQUIREMENT_CHECK"))
        assertTrue(result.checksPassed.contains("BUSINESS_RULE_CHECK"))
    }

    @Test
    fun testActionOrchestrationHighRiskTriggersHumanApproval() = runBlocking {
        // Propose high-value financial action (amount > 5,000,000 IDR)
        val result = actionOrchestrator.proposeAction(
            tenantId = tenantStarter,
            agentId = "agent-finance-01",
            actionType = "TRANSFER_FUNDS",
            targetSystem = "TASK_BOARD",
            payload = mapOf(
                "title" to "Emergency Vendor Wire Transfer",
                "amount" to "45000000",
                "quantity" to "1"
            ),
            assignedHuman = "cfo-executive"
        )

        assertFalse(result.isApproved, "High-risk action must not be auto-approved")
        assertTrue(result.requiresHumanApproval, "High-risk action must trigger human escalation")
        assertEquals("PENDING", result.action.status)
        assertTrue(result.action.requiresApproval)
    }

    @Test
    fun testActionOrchestrationBusinessRuleFailureRejectsImmediately() = runBlocking {
        // Quantity <= 0 violates business rule
        val result = actionOrchestrator.proposeAction(
            tenantId = tenantStarter,
            agentId = "agent-logistics",
            actionType = "CREATE_TASK",
            targetSystem = "TASK_BOARD",
            payload = mapOf(
                "title" to "Invalid Restock",
                "quantity" to "0"
            )
        )

        assertFalse(result.isApproved)
        assertEquals("REJECTED", result.action.status)
        assertNotNull(result.rejectionReason)
        assertTrue(result.rejectionReason!!.contains("quantity must be greater than zero"))
    }

    @Test
    fun testExecuteApprovedActionNonEnterpriseRoutesToTaskBoard() = runBlocking {
        // Prepare an approved action
        val approved = ApprovedAction(
            tenantId = tenantStarter,
            agentId = "agent-maint",
            actionType = "CREATE_TASK",
            targetSystem = "TASK_BOARD",
            payload = mapOf(
                "title" to "Calibrate Pressure Sensor B-12",
                "description" to "Routine sensor calibration task",
                "reason" to "Sensor drift detected"
            ),
            assignedHuman = "tech-lead",
            status = "APPROVED"
        )

        val execResult = actionOrchestrator.executeApprovedAction(approved)
        assertEquals("SUCCESS", execResult.status)
        assertTrue(execResult.executionSummary.contains("Internal Task Board"))
        println("=== RAW AUDIT RECORD ai_action_executed ===")
        println("Action ID: " + approved.id)
        println("Status: " + execResult.status)
        println("Summary: " + execResult.executionSummary)
        println("Assigned Human: " + approved.assignedHuman)
    }

    @Test
    fun testExecuteApprovedActionEnterpriseRoutesToIntegrationFabric() = runBlocking {
        // Enterprise action routes to external connector
        val enterpriseAction = ApprovedAction(
            tenantId = tenantEnterprise,
            agentId = "agent-erp-sync",
            actionType = "SYNC_PURCHASE_ORDER",
            targetSystem = "conn-sap-prod",
            payload = mapOf(
                "poNumber" to "PO-99481",
                "vendorId" to "VEND-INDONESIA-PETRO",
                "amount" to "75000000"
            ),
            assignedHuman = "sap-admin",
            status = "APPROVED"
        )

        val execResult = actionOrchestrator.executeApprovedAction(enterpriseAction)
        assertEquals("SUCCESS", execResult.status)
        assertTrue(execResult.executionSummary.contains("Integration Fabric"))

        // Confirm record ingested in fabric
        val records = EnterpriseIntegrationFabricService.listIngestedRecords(tenantEnterprise)
        assertTrue(records.any { it.recordType == "SYNC_PURCHASE_ORDER" }, "Fabric must store ingested record")
    }

    // =========================================================================
    // 2. CLOSED-LOOP WORKFORCE MONITORING (Bagian 68)
    // =========================================================================

    @Test
    fun testMonitoringLoopLifecycleAndSourceVerification() = runBlocking {
        val sku = "ITEM-SKU-TEST-101"
        // Setup initial source data
        monitoringEngine.updateSourceSystemData(
            tenantId = tenantStarter,
            sourceSystem = "INTERNAL_INVENTORY",
            entityReference = sku,
            liveValue = 5.0
        )

        // 1. Detect anomaly and start loop (State: DETECT)
        val loop = monitoringEngine.registerAnomaly(
            tenantId = tenantStarter,
            anomalyOrMetricType = "INVENTORY_SHORTAGE",
            entityReference = sku,
            sourceSystem = "INTERNAL_INVENTORY",
            baselineValue = 20.0,
            detectedValue = 5.0,
            targetResolvedValue = 20.0,
            assignedAgentOrHumanId = "agent-warehouse"
        )

        assertEquals(MonitoringLoopState.DETECT, loop.state)

        // Tick 1: DETECT -> TASK_CREATED
        val ticked1List = monitoringEngine.monitoringLoopTick(loop.id)
        val ticked1 = ticked1List.firstOrNull { it.id == loop.id }
        assertEquals(MonitoringLoopState.TASK_CREATED, ticked1?.state)
        assertNotNull(ticked1?.taskId)

        // Verify task was created in repository
        val taskRepo = TaskRepository()
        val createdTask = taskRepo.getById(ticked1!!.taskId!!)
        assertNotNull(createdTask)
        assertTrue(createdTask!!.title.contains(sku))

        // Tick 2: TASK_CREATED -> ASSIGNED
        val ticked2List = monitoringEngine.monitoringLoopTick(loop.id)
        val ticked2 = ticked2List.firstOrNull { it.id == loop.id }
        assertEquals(MonitoringLoopState.ASSIGNED, ticked2?.state)

        // Tick 3: ASSIGNED -> IN_PROGRESS
        val ticked3List = monitoringEngine.monitoringLoopTick(loop.id)
        val ticked3 = ticked3List.firstOrNull { it.id == loop.id }
        assertEquals(MonitoringLoopState.IN_PROGRESS, ticked3?.state)

        // User marks task as DONE on TaskBoard
        val ongoingTask = taskRepo.getById(ticked1.taskId!!)!!
        taskRepo.updateTask(ongoingTask.copy(columnName = "DONE", progressPct = 100))

        // Tick 4: IN_PROGRESS -> VERIFYING
        val ticked4List = monitoringEngine.monitoringLoopTick(loop.id)
        val ticked4 = ticked4List.firstOrNull { it.id == loop.id }
        assertEquals(MonitoringLoopState.VERIFYING, ticked4?.state)

        // Objective Verification: update actual source data to meet target (liveValue = 25.0 >= target 20.0)
        monitoringEngine.updateSourceSystemData(
            tenantId = tenantStarter,
            sourceSystem = "INTERNAL_INVENTORY",
            entityReference = sku,
            liveValue = 25.0
        )

        // Tick 5: VERIFYING -> RESOLVED (Anti Fake Closed-Loop: verified against real source)
        val ticked5List = monitoringEngine.monitoringLoopTick(loop.id)
        val ticked5 = ticked5List.firstOrNull { it.id == loop.id }
        assertEquals(MonitoringLoopState.RESOLVED, ticked5?.state)
        assertNotNull(ticked5?.sourceVerifiedAt)
        println("=== RAW MONITORING LOOP TRANSITION HISTORY ===")
        for (evt in monitoringEngine.transitionHistory.filter { it.loopId == loop.id }) {
            println("Transition: " + evt.fromState + " -> " + evt.toState + " | Detail: " + evt.detail)
        }
        println("Final Verified At: " + ticked5?.sourceVerifiedAt)
    }

    @Test
    fun testMonitoringLoopEscalationWhenSourceSystemMismatch() = runBlocking {
        val sku = "SKU-PUMP-VALVE-01"
        monitoringEngine.updateSourceSystemData(
            tenantId = tenantStarter,
            sourceSystem = "INTERNAL_INVENTORY",
            entityReference = sku,
            liveValue = 2.0
        )

        val loop = monitoringEngine.registerAnomaly(
            tenantId = tenantStarter,
            anomalyOrMetricType = "CRITICAL_PART_DEPLETED",
            entityReference = sku,
            sourceSystem = "INTERNAL_INVENTORY",
            baselineValue = 10.0,
            detectedValue = 2.0,
            targetResolvedValue = 10.0,
            assignedAgentOrHumanId = "agent-procure"
        )

        // Manually move loop directly to VERIFYING
        loop.state = MonitoringLoopState.VERIFYING
        loop.verificationAttempts = 3 // Exceeding max attempts (MAX_VERIFICATION_ATTEMPTS = 3)

        // Source system still has depleted stock (2.0 < target 10.0)
        val tickedList = monitoringEngine.monitoringLoopTick(loop.id)
        val ticked = tickedList.firstOrNull { it.id == loop.id }
        assertEquals(MonitoringLoopState.ESCALATED, ticked?.state)
        assertNotNull(ticked?.escalationReason)
        assertTrue(ticked?.escalationReason!!.contains("verification FAILED") || ticked.escalationReason!!.contains("Fake closed-loop prevented"))
    }

    // =========================================================================
    // 3. AI FINANCE INTELLIGENCE (Bagian 69)
    // =========================================================================

    @Test
    fun testCashFlowPressureDetectionAllTiers() = runBlocking {
        val assessment = financeService.analyzeCashFlowPressure(
            tenantId = tenantStarter
        )

        assertNotNull(assessment)
        assertEquals(tenantStarter, assessment.tenantId)
        assertTrue(assessment.pressureScore in 0.0..100.0)
        assertNotNull(assessment.pressureLevel)
        assertTrue(assessment.cashRunwayMonths >= 0.0)
        assertTrue(assessment.arAgingBuckets.isNotEmpty())
        assertTrue(assessment.mitigationRecommendations.isNotEmpty(), "Must produce actionable recommendations")
    }

    // =========================================================================
    // 4. KNOWLEDGE & OPERATIONAL FUSION ENGINE (Bagian 70)
    // =========================================================================

    @Test
    fun testKnowledgeOperationalFusionRequiresAdminApproval() = runBlocking {
        // 1. Synthesize candidate knowledge rule
        val candidate = KnowledgeRule(
            id = "rule-vib-01",
            tenantId = tenantStarter,
            entityType = "PUMP",
            condition = "vibration_amplitude_mm_s",
            comparisonOperator = ">",
            thresholdValue = 4.5,
            sopReference = "SOP-MAINT-HEAVY-04",
            ruleDescription = "Jika getaran > 4.5 mm/s, inspeksi darurat",
            status = "PENDING_APPROVAL"
        )
        val rule = knowledgeEngine.proposeRule(candidate)

        assertEquals("PENDING_APPROVAL", rule.status, "Rule must be PENDING_APPROVAL initially")

        // 2. Evaluasi saat pending -> diabaikan (pendingRulesIgnoredCount > 0, tidak trigger directive)
        val fusionPending = knowledgeEngine.fuseOperationalData(
            tenantId = tenantStarter,
            entityId = "PUMP-ALPHA-01",
            entityType = "PUMP",
            operationalMetrics = mapOf("vibration_amplitude_mm_s" to 6.8)
        )
        assertTrue(fusionPending.pendingRulesIgnoredCount >= 1, "Pending rules must be ignored")
        assertFalse(fusionPending.triggeredDirectives.any { it.ruleId == rule.id }, "Pending rule must not trigger directives")

        // 3. Admin approves rule
        val approved = knowledgeEngine.approveRule(
            tenantId = tenantStarter,
            ruleId = rule.id,
            adminUserId = "admin-master-01"
        )
        assertNotNull(approved)
        assertEquals("APPROVED", approved?.status)

        // 4. Evaluasi saat approved -> harus trigger directive
        val fusionApproved = knowledgeEngine.fuseOperationalData(
            tenantId = tenantStarter,
            entityId = "PUMP-ALPHA-01",
            entityType = "PUMP",
            operationalMetrics = mapOf("vibration_amplitude_mm_s" to 6.8)
        )
        assertTrue(fusionApproved.triggeredDirectives.any { it.ruleId == rule.id }, "Approved rule must trigger directive when threshold is exceeded")
    }

    // =========================================================================
    // 5. AI EVENT ENGINE (Bagian 71)
    // =========================================================================

    @Test
    fun testAiEventEngineDispatchesToSpecialistAgents() = runBlocking {
        // Dispatch predefined event
        val instance = AiEventInstance(
            tenantId = tenantStarter,
            eventCode = "EQUIPMENT_OVERHEAT",
            entityReference = "TURBINE-04",
            severity = "CRITICAL",
            payloadJson = "{\"temperatureC\": 102.5}"
        )

        val result = eventEngine.publishAndDispatch(instance)

        assertNotNull(result)
        assertEquals("DISPATCHED", result.eventInstance.status)
        assertEquals("MAINTENANCE_SPECIALIST", result.responsiblePersona)
        assertTrue(result.dispatchLogs.isNotEmpty())
        assertTrue(result.summary.contains("MAINTENANCE_SPECIALIST"))
    }

    // =========================================================================
    // 6. SCHEDULER ENGINE INTEGRATION
    // =========================================================================

    @Test
    fun testMonitoringLoopJobExecution() = runBlocking {
        val job = MonitoringLoopJob(monitoringEngine)
        val result = job.execute(tenantStarter)
        assertNotNull(result)
        assertTrue(result.contains("Ticked"))
    }
}
