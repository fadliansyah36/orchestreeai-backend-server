package ai.orchestree.backend.enterprise

import ai.orchestree.backend.api.*
import ai.orchestree.backend.intelligence.AiCognitiveLoop
import ai.orchestree.backend.intelligence.AiResearchAgent
import ai.orchestree.backend.intelligence.CrossSystemCorrelator
import ai.orchestree.backend.scheduler.jobs.ProactiveDailyReportJob
import ai.orchestree.backend.plugins.configureSerialization
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Verification Test for Fase 2B.2 (PRD Addendum 2 Bagian 61 - 66)
 * Verifies:
 * - AI Cognitive Loop 10 Tahap (Bagian 61)
 * - CrossSystemCorrelator (Bagian 61.2)
 * - Company Activity Stream & Audit Log reuse (Bagian 62)
 * - Company Context Fabric 8 Dimensi (Bagian 63)
 * - AI Research Agent 6 Prioritas Sumber (Bagian 64)
 * - Automatic Reporting Engine via ProactiveDailyReportJob (Bagian 65)
 * - Management Conversational Query with Multi-Turn Drill-Down, Role Personalization & ABAC (Bagian 66)
 */
class EnterpriseCognitiveLoopAndWorkforceVerificationTest {

    private val tenantId = "tenant-2b2-test"

    @BeforeEach
    fun setUp() {
        FeatureCapabilityService.setTenantTier(tenantId, TierLevel.ENTERPRISE)
        // Reset permissions for tenant
        AiDataPermissionService.grantPolicy(
            AiDataPermissionPolicy(
                id = "pol-chief-exec",
                tenantId = tenantId,
                agentId = "agent-chief-of-staff",
                connectionId = "conn-enterprise",
                accessLevel = "ANALYZE",
                allowedTablesOrTypes = listOf("*", "EX03", "General")
            )
        )
    }

    // =========================================================================
    // LANGKAH 1: AI Cognitive Loop 10 Tahap & CrossSystemCorrelator (Bagian 61)
    // =========================================================================

    @Test
    fun testAiCognitiveLoopAllTenStagesExecuted() = runBlocking {
        val loop = AiCognitiveLoop()
        val result = loop.process(
            tenantId = tenantId,
            input = "Audit anomali tekanan hidrolik dan risiko proyek",
            entityReference = "EX03",
            agentId = "agent-chief-of-staff",
            callerRole = "EXECUTIVE"
        )

        assertNotNull(result)
        assertEquals(tenantId, result.tenantId)
        assertEquals("EX03", result.entityReference)
        assertTrue(result.confidenceScore > 0.0)
        assertTrue(result.finalOutput.isNotBlank())

        // Verify all 10 stages are present and executed in order
        assertEquals(10, result.stages.size, "Cognitive Loop must execute exactly 10 sequential stages")
        val stageNames = result.stages.map { it.stageName }
        val expectedStages = listOf(
            "PERCEPTUAL_INTAKE",
            "CROSS_SYSTEM_CORRELATION",
            "INTENT_DECOMPOSITION",
            "CONTEXT_ASSEMBLY",
            "KNOWLEDGE_RETRIEVAL",
            "DECISION_AND_RISK_EVALUATION",
            "ACTION_ORCHESTRATION",
            "OUTPUT_VALIDATION",
            "CONFIDENCE_SCORING",
            "LEARNING_AND_CONSOLIDATION"
        )
        assertEquals(expectedStages, stageNames)

        for (i in 1..10) {
            assertEquals(i, result.stages[i - 1].stageNumber)
            assertNotNull(result.stages[i - 1].summary)
        }
    }

    @Test
    fun testCrossSystemCorrelatorMultiSystemDetection() = runBlocking {
        // Publish activities from 2 distinct systems for entity EX03
        CompanyActivityStreamService.publishActivity(
            tenantId = tenantId,
            systemType = "SAP_ERP",
            summary = "PO #1049 suku cadang Excavator EX03 tertunda",
            entityReference = "EX03"
        )
        CompanyActivityStreamService.publishActivity(
            tenantId = tenantId,
            systemType = "CMMS",
            summary = "Sensor hidrolik Excavator EX03 bertekanan rendah 180 bar",
            entityReference = "EX03"
        )

        val correlator = CrossSystemCorrelator()
        val correlation = correlator.correlateSignals(tenantId, "EX03", timeWindowHours = 48)

        assertTrue(correlation.isCorrelated, "Correlation must be detected with 2 distinct systems")
        assertTrue(correlation.distinctSystemsCount >= 2)
        assertTrue(correlation.distinctSystems.contains("SAP_ERP") || correlation.distinctSystems.contains("CMMS"))
        assertEquals("HIGH", correlation.impactLevel)
        assertTrue(correlation.riskScore > 0.7)
    }

    // =========================================================================
    // LANGKAH 2: Activity Stream & Context Fabric 8 Dimensions (Bagian 62-63)
    // =========================================================================

    @Test
    fun testCompanyActivityStreamAndAuditLoggerReuse() = runBlocking {
        val auditLogger = ai.orchestree.backend.security.AuditLogger()
        val initialAuditCount = auditLogger.inMemoryLogs.size

        val activity = CompanyActivityStreamService.publishActivity(
            tenantId = tenantId,
            systemType = "WMS",
            summary = "Penerimaan sparepart seal kit hidrolik EX03",
            entityReference = "EX03",
            activityType = "INVENTORY_RECEIPT",
            auditLogger = auditLogger
        )

        assertNotNull(activity.id)
        assertEquals("WMS", activity.sourceSystem)

        // Verify AuditLogger was reused (no second event system)
        assertEquals(initialAuditCount + 1, auditLogger.inMemoryLogs.size)
        val lastAudit = auditLogger.inMemoryLogs.last()
        assertEquals("WMS", lastAudit.actor)
        assertTrue(lastAudit.action.contains("ACTIVITY_STREAM"))

        // Fetch stream
        val stream = CompanyActivityStreamService.getStream(tenantId, auditLogger = auditLogger)
        assertTrue(stream.isNotEmpty())
        assertTrue(stream.any { it.summaryText.contains("EX03") })
    }

    @Test
    fun testCompanyContextFabricEightDimensions() = runBlocking {
        // Record specific context in Semantic Memory
        CompanyContextFabricService.recordContextDimension(
            tenantId = tenantId,
            entityId = "EX03",
            dimension = "financial",
            content = "Anggaran perawatan darurat EX03 dialokasikan Rp18.500.000."
        )

        val fabric = CompanyContextFabricService.resolveContext(tenantId, "EX03")
        assertEquals("EX03", fabric.entityId)
        assertEquals(tenantId, fabric.tenantId)

        // Verify all 8 dimensions are populated
        assertTrue(fabric.currentContext.isNotBlank(), "currentContext must be resolved")
        assertTrue(fabric.historicalContext.isNotBlank(), "historicalContext must be resolved")
        assertTrue(fabric.businessContext.isNotBlank(), "businessContext must be resolved")
        assertTrue(fabric.operationalContext.isNotBlank(), "operationalContext must be resolved")
        assertTrue(fabric.humanContext.isNotBlank(), "humanContext must be resolved")
        assertTrue(fabric.assetContext.isNotBlank(), "assetContext must be resolved")
        assertTrue(fabric.financialContext.isNotBlank(), "financialContext must be resolved")
        assertTrue(fabric.projectContext.isNotBlank(), "projectContext must be resolved")
    }

    // =========================================================================
    // LANGKAH 3: AI Research Agent 6-Level Priority Sources (Bagian 64)
    // =========================================================================

    @Test
    fun testAiResearchAgentPriorityWaterfallHierarchy() = runBlocking {
        val researchAgent = AiResearchAgent()
        val result = researchAgent.executeResearch(
            tenantId = tenantId,
            query = "Analisis batas toleransi keausan pompa hidrolik Excavator EX03",
            entityId = "EX03"
        )

        assertNotNull(result.researchId)
        assertTrue(result.decomposedQueries.size >= 2, "Query decomposition must generate sub-queries")
        assertTrue(result.synthesis.isNotBlank(), "Research synthesis must be produced")
        assertTrue(result.confidenceScore > 0.0)

        // Verify citations include the priority hierarchy levels (1 through 6)
        val levels = result.sourcesUsed.map { it.priorityLevel }.distinct()
        assertTrue(levels.contains(2), "Priority 2 (Company Brain/SOP) must be cited")
        assertTrue(levels.contains(3), "Priority 3 (Historical Outcomes) must be cited")
        assertTrue(levels.contains(4), "Priority 4 (Industry Benchmarks) must be cited")
        assertTrue(levels.contains(5), "Priority 5 (Public Signals with Privacy Guardrail) must be cited")
        assertTrue(levels.contains(6), "Priority 6 (General LLM Knowledge) must be cited")
    }

    // =========================================================================
    // LANGKAH 4: Automatic Reporting Engine via ProactiveDailyReportJob (Bagian 65)
    // =========================================================================

    @Test
    fun testAutomaticReportingEnginePersistenceAndRetrieval() = runBlocking {
        val job = ProactiveDailyReportJob()
        val report = ProactiveDailyReportJob.getLatestReport(tenantId, instance = job)

        assertNotNull(report.reportId)
        assertEquals("DAILY_EXECUTIVE", report.reportType)
        assertTrue(report.summary.isNotBlank())
        assertTrue(report.generatedAt > 0)
    }

    // =========================================================================
    // LANGKAH 5: Management Conversational Query Engine (Bagian 66)
    // Multi-Turn Drill-Down, Role Personalization, and ABAC Verification
    // =========================================================================

    @Test
    fun testManagementQueryMultiTurnDrillDownContextRetention() = testApplication {
        application {
            configureSerialization()
            routing { enterpriseRoutes() }
        }

        val testSessionId = "sess-drilldown-${System.currentTimeMillis()}"

        // Turn 1: Inquire about entity EX03
        val turn1Response = client.post("/tenants/$tenantId/management-query") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    ManagementQueryRequest.serializer(),
                    ManagementQueryRequest(
                        question = "Bagaimana status operasional dan kondisi Excavator EX03 saat ini?",
                        entityFocus = "EX03",
                        sessionId = testSessionId,
                        role = "EXECUTIVE",
                        agentId = "agent-chief-of-staff"
                    )
                )
            )
        }

        assertEquals(HttpStatusCode.OK, turn1Response.status)
        val turn1Body = Json.decodeFromString<ManagementQueryResponse>(turn1Response.bodyAsText())
        assertEquals(testSessionId, turn1Body.sessionId)
        assertEquals(1, turn1Body.turnCount)
        assertFalse(turn1Body.accessRestricted)
        assertTrue(turn1Body.answer.isNotBlank())
        println("=== RAW RESPONSE TURN 1 ===")
        println(turn1Response.bodyAsText())

        // Turn 2: Drill-down follow-up WITHOUT repeating the full entity name "Excavator EX03"
        val turn2Response = client.post("/tenants/$tenantId/management-query") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    ManagementQueryRequest.serializer(),
                    ManagementQueryRequest(
                        question = "Berapa estimasi biaya perbaikannya dan apa rekomendasi mitigasinya?",
                        entityFocus = "EX03",
                        sessionId = testSessionId, // Context continuity
                        role = "EXECUTIVE",
                        agentId = "agent-chief-of-staff"
                    )
                )
            )
        }

        assertEquals(HttpStatusCode.OK, turn2Response.status)
        val turn2Body = Json.decodeFromString<ManagementQueryResponse>(turn2Response.bodyAsText())
        assertEquals(testSessionId, turn2Body.sessionId)
        assertEquals(2, turn2Body.turnCount, "Turn count must increment to 2, confirming multi-turn state retention")
        assertFalse(turn2Body.accessRestricted)
        assertTrue(turn2Body.answer.isNotBlank())
        println("=== RAW RESPONSE TURN 2 (DRILL-DOWN WITH CONTEXT RETENTION) ===")
        println(turn2Response.bodyAsText())
    }

    @Test
    fun testManagementQueryRolePersonalization() = testApplication {
        application {
            configureSerialization()
            routing { enterpriseRoutes() }
        }

        // Test Executive Role
        val execResp = client.post("/tenants/$tenantId/management-query") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    ManagementQueryRequest.serializer(),
                    ManagementQueryRequest(
                        question = "Ringkas utilisasi aset proyek Sukamaju",
                        entityFocus = "General",
                        role = "EXECUTIVE"
                    )
                )
            )
        }
        val execBody = Json.decodeFromString<ManagementQueryResponse>(execResp.bodyAsText())
        assertEquals("EXECUTIVE", execBody.rolePersonalization)
        assertTrue(execBody.answer.contains("[EXECUTIVE Perspective]"))

        // Test Operational Staff Role
        val staffResp = client.post("/tenants/$tenantId/management-query") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    ManagementQueryRequest.serializer(),
                    ManagementQueryRequest(
                        question = "Panduan inspeksi hidrolik harian",
                        entityFocus = "General",
                        role = "OPERATIONAL_STAFF"
                    )
                )
            )
        }
        val staffBody = Json.decodeFromString<ManagementQueryResponse>(staffResp.bodyAsText())
        assertEquals("OPERATIONAL_STAFF", staffBody.rolePersonalization)
        assertTrue(staffBody.answer.contains("[OPERATIONAL_STAFF Perspective]"))
    }

    @Test
    fun testManagementQueryAbacDefaultDenyEnforcement() = testApplication {
        application {
            configureSerialization()
            routing { enterpriseRoutes() }
        }

        // Attempt query with an unauthorized agent persona on restricted entity
        val blockedResponse = client.post("/tenants/$tenantId/management-query") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    ManagementQueryRequest.serializer(),
                    ManagementQueryRequest(
                        question = "Tampilkan data rahasia finansial",
                        entityFocus = "RESTRICTED_PAYROLL",
                        role = "STAFF",
                        agentId = "agent-unauthorized-guest" // Denied by ABAC
                    )
                )
            )
        }

        assertEquals(HttpStatusCode.Forbidden, blockedResponse.status)
        val blockedBody = Json.decodeFromString<ManagementQueryResponse>(blockedResponse.bodyAsText())
        assertTrue(blockedBody.accessRestricted, "accessRestricted must be true on ABAC denial")
        assertEquals("RESTRICTED", blockedBody.dataAvailability)
        assertTrue(blockedBody.answer.contains("AKSES DIBATASI (ABAC Default-Deny)"))
        println("=== RAW RESPONSE ABAC BLOCKED ===")
        println(blockedResponse.bodyAsText())
    }
}
