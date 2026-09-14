package ai.orchestree.backend.enterprise

import ai.orchestree.backend.collaboration.MultiAgentCollaborationRequest
import ai.orchestree.backend.collaboration.SpecialistAgentCollaborationService
import ai.orchestree.backend.collaboration.SpecialistPersonas
import ai.orchestree.backend.intelligence.ChiefOfStaffService
import ai.orchestree.backend.intelligence.DataAvailabilityState
import ai.orchestree.backend.intelligence.OutputValidator
import ai.orchestree.backend.learning.ActionOutcomeRecord
import ai.orchestree.backend.learning.ContinuousLearningCore
import ai.orchestree.backend.security.AuditLogger
import ai.orchestree.backend.security.ExplainabilityTrace
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Fase 2B.4 Comprehensive Verification Test (PRD Addendum 2 Bagian 72-76, 78)
 * 
 * Verifies:
 * 1. Specialist AI Agents & Multi-Agent Collaboration (Bagian 72)
 * 2. AI Chief of Staff Executive Briefing with explicit Missing Domain disclosures (Bagian 73)
 * 3. Continuous Learning Core 8-stage closed loop & Anti-Reinforcement Guardrail (Bagian 74)
 * 4. Audit Trail & Role-Based Explainability (Bagian 75)
 * 5. AI Never Invent Data (Bagian 76)
 * 6. Final RBAC/ABAC Enterprise Capabilities (Bagian 78)
 */
class Fase2B4VerificationTest {

    private val tenantTest = "tenant-fase2b4-test"
    private lateinit var collaborationService: SpecialistAgentCollaborationService
    private lateinit var chiefOfStaffService: ChiefOfStaffService
    private lateinit var auditLogger: AuditLogger
    private lateinit var outputValidator: OutputValidator

    @BeforeEach
    fun setUp() {
        collaborationService = SpecialistAgentCollaborationService()
        chiefOfStaffService = ChiefOfStaffService()
        auditLogger = AuditLogger()
        outputValidator = OutputValidator()
    }

    // =========================================================================
    // 1. SPECIALIST AI AGENTS & MULTI-AGENT COLLABORATION (Bagian 72)
    // =========================================================================
    @Test
    fun testProjectHealthScoreCalculation() = runBlocking {
        val report = collaborationService.evaluateProjectHealth(
            tenantId = tenantTest,
            projectId = "proj-mining-expansion",
            projectName = "Mining Expansion Pit 4",
            scheduleScore = 80.0,
            budgetScore = 90.0,
            riskScore = 85.0,
            workforceScore = 85.0,
            blockersCount = 1
        )

        assertNotNull(report)
        assertEquals("ON_TRACK", report.healthStatus)
        assertEquals(85.0, report.compositeHealthScore)
        assertEquals(1, report.blockersCount)
    }

    @Test
    fun testMultiAgentCollaborationConsensus() = runBlocking {
        val request = MultiAgentCollaborationRequest(
            tenantId = tenantTest,
            title = "Penanganan Kebocoran Hidrolik Crusher 02",
            initiatingAgentId = "agent-sentinel-maint",
            initiatingPersona = SpecialistPersonas.MAINTENANCE_SPECIALIST,
            targetEntityReference = "CRUSHER-02",
            eventDetails = "Tekanan oli turun drastis di bawah 30 psi",
            requestedPersonas = listOf(
                SpecialistPersonas.MAINTENANCE_SPECIALIST,
                SpecialistPersonas.PROCUREMENT_SPECIALIST,
                SpecialistPersonas.HSE_COMPLIANCE_SPECIALIST,
                SpecialistPersonas.OPERATIONS_LEAD
            )
        )

        val session = collaborationService.initiateCollaboration(request)
        assertNotNull(session)
        assertEquals("CONSENSUS_REACHED", session.status)
        assertEquals(4, session.participatingAgents.size)
        assertTrue(session.consensusSummary.contains("Konsensus"))
        assertTrue(session.actionPlan.any { it.contains("LOTO") || it.contains("Safety") })
    }

    // =========================================================================
    // 2. AI CHIEF OF STAFF & NEVER INVENT DATA (Bagian 73 & 76)
    // =========================================================================
    @Test
    fun testChiefOfStaffExplicitMissingDomainDisclosure() = runBlocking {
        // Generate briefing for a fresh tenant where specialist agents are missing
        val briefing = chiefOfStaffService.generateExecutiveBriefing(tenantTest)

        assertNotNull(briefing)
        println("=== RAW CHIEF OF STAFF EXECUTIVE BRIEFING ===")
        println("Headline: " + briefing.headline)
        println("Data State: " + briefing.dataAvailabilityState)
        println("Missing Domains: " + briefing.missingDomains)
        println("Limitation Notice: " + briefing.dataLimitationNotice)
        println("Executive Summary: " + briefing.executiveSummary)
        println("AI Workforce Summary: " + briefing.aiWorkforceSummary)
        println("Human Workforce Summary: " + briefing.humanWorkforceSummary)

        // Mandatory PRD rule: Missing domains must be explicitly disclosed and dataState marked PARTIAL
        assertTrue(briefing.missingDomains.isNotEmpty(), "Must identify missing specialist domains")
        assertEquals("PARTIAL", briefing.dataAvailabilityState)
        assertNotNull(briefing.dataLimitationNotice)
        assertTrue(briefing.dataLimitationNotice!!.contains("belum tersedia"))
        assertTrue(briefing.executiveSummary.contains("DATA LIMITATION NOTICE") || briefing.dataLimitationNotice != null)
        
        // Authority boundary check: Human workforce summary must ONLY be aggregate
        assertFalse(briefing.humanWorkforceSummary.contains("Staff individual #"))
        assertTrue(briefing.humanWorkforceSummary.contains("agregat"))
    }

    // =========================================================================
    // 3. CONTINUOUS LEARNING CORE & ANTI-REINFORCEMENT GUARDRAIL (Bagian 74)
    // =========================================================================
    @Test
    fun testAntiReinforcementGuardrailOnSafetyViolation() = runBlocking {
        // Even if an operation succeeded in task completion, if a safety violation occurred,
        // it MUST NEVER be reinforced!
        val unsafeOutcome = ActionOutcomeRecord(
            tenantId = tenantTest,
            agentId = "agent-speed-loader",
            agentName = "Speed Loader Bot",
            nodeId = "DISPATCH_LOAD",
            executionId = "exec-safety-breach-99",
            workflowId = "fleet-dispatch",
            scenarioContext = "Bypassed safety gate to deliver payload 5 mins faster",
            actionType = "OVERRIDE_SAFETY_SPEED",
            predictedImpact = "Save 5 minutes transport cycle",
            actualOutcome = "Delivered fast but safety sensor tripped emergency brake",
            outcomeSource = "INTEGRATION_FABRIC_RESULT",
            isSuccess = true, // technically reached destination
            safetyViolation = true // CRITICAL VIOLATION
        )

        val result = ContinuousLearningCore.recordOutcome(unsafeOutcome)
        assertEquals("LEARN_FROM_REJECTION", result.classification, "Safety violation must NEVER be reinforced!")
        assertTrue(result.confidenceDelta < 0, "Confidence must be heavily penalized")

        val stats = ContinuousLearningCore.getAgentSkillConfidence(tenantTest, "agent-speed-loader")
        assertTrue(stats.rejectionCount >= 1)
    }

    @Test
    fun testContinuousLearningReinforcementOnVerifiedSuccess() = runBlocking {
        val successfulOutcome = ActionOutcomeRecord(
            tenantId = tenantTest,
            agentId = "agent-reliable-maint",
            agentName = "Reliable Maint Bot",
            nodeId = "PUMP_INSPECTION",
            executionId = "exec-clean-success-01",
            workflowId = "pm-daily",
            scenarioContext = "Vibration analysis matched SOP parameters",
            actionType = "ROUTINE_INSPECTION",
            predictedImpact = "Prevent unplanned downtime",
            actualOutcome = "Target achieved without warning",
            outcomeSource = "SOURCE_SYSTEM_VERIFICATION",
            isSuccess = true,
            sopBreached = false,
            safetyViolation = false
        )

        val result = ContinuousLearningCore.recordOutcome(successfulOutcome)
        assertEquals("REINFORCE", result.classification)
        assertTrue(result.confidenceDelta > 0)
    }

    // =========================================================================
    // 4. AUDIT TRAIL & ROLE-BASED EXPLAINABILITY (Bagian 75)
    // =========================================================================
    @Test
    fun testRoleBasedExplainabilityViews() {
        val trace = ExplainabilityTrace(
            executionId = "exec-explain-772",
            tenantId = tenantTest,
            agentId = "agent-chief-of-staff",
            actionType = "SYNTHESIZE_EXECUTIVE_BRIEFING",
            triggerEvent = "SCHEDULED_EXECUTIVE_BRIEFING",
            inputsUsedSummary = "Tasks aggregated: 14 completed, Context Fabric checked",
            confidenceScore = 0.94,
            sopReference = "SOP-CORP-GOV-01"
        )
        auditLogger.recordExplainability(trace)

        // 1. Executive Role
        val execView = auditLogger.getRoleBasedExplainability(tenantTest, trace.executionId, "EXECUTIVE")
        assertNotNull(execView)
        println("=== RAW EXECUTIVE EXPLAINABILITY VIEW ===")
        println(execView)
        assertTrue(execView is ai.orchestree.backend.security.ExecutiveExplainabilityView)

        // 2. Operator Role
        val opView = auditLogger.getRoleBasedExplainability(tenantTest, trace.executionId, "OPERATOR")
        assertNotNull(opView)
        println("=== RAW OPERATOR EXPLAINABILITY VIEW ===")
        println(opView)
        assertTrue(opView is ai.orchestree.backend.security.OperatorExplainabilityView)

        // 3. Auditor Role
        val auditorView = auditLogger.getRoleBasedExplainability(tenantTest, trace.executionId, "AUDITOR")
        assertNotNull(auditorView)
        println("=== RAW AUDITOR EXPLAINABILITY VIEW ===")
        println(auditorView)
        assertTrue(auditorView is ai.orchestree.backend.security.AuditorExplainabilityView)
    }

    // =========================================================================
    // 5. RBAC/ABAC ENTERPRISE CAPABILITIES (Bagian 78)
    // =========================================================================
    @Test
    fun testEnterpriseRbacCapabilitiesEnforcement() {
        // Executive / Admin roles have full executive privileges
        assertTrue(AiDataPermissionService.hasCapability("EXECUTIVE", AiDataPermissionService.EnterpriseCapability.ACCESS_CHIEF_OF_STAFF_BRIEFINGS))
        assertTrue(AiDataPermissionService.hasCapability("TENANT_ADMIN", AiDataPermissionService.EnterpriseCapability.MANAGE_KNOWLEDGE_RULES))

        // Operational staff can view stream but cannot manage knowledge rules or access chief of staff briefings
        assertFalse(AiDataPermissionService.hasCapability("STAFF", AiDataPermissionService.EnterpriseCapability.MANAGE_KNOWLEDGE_RULES))
        assertFalse(AiDataPermissionService.hasCapability("OPERATOR", AiDataPermissionService.EnterpriseCapability.ACCESS_CHIEF_OF_STAFF_BRIEFINGS))
        assertTrue(AiDataPermissionService.hasCapability("STAFF", AiDataPermissionService.EnterpriseCapability.VIEW_ACTIVITY_STREAM))
    }
}
