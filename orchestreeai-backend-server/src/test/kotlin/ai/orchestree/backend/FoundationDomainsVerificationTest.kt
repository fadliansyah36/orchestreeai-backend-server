package ai.orchestree.backend

import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRecord
import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRepository
import ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository
import ai.orchestree.backend.database.repositories.orchestration.WorkflowExecutionRepository
import ai.orchestree.backend.enterprise.EnterpriseIntegrationFabricService
import ai.orchestree.backend.enterprise.FeatureCapabilityService
import ai.orchestree.backend.enterprise.TierLevel
import ai.orchestree.backend.intelligence.ConfidenceEngine
import ai.orchestree.backend.mcptools.*
import ai.orchestree.backend.memory.HybridMemorySearchEngine
import ai.orchestree.backend.orchestration.WorkflowExecution
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class FoundationDomainsVerificationTest {

    @Test
    fun testCoreInfrastructureAndWorkflowExecutionPersistence() = runBlocking {
        val repo = WorkflowExecutionRepository()
        val testId = "exec-test-${UUID.randomUUID().toString().take(8)}"
        val execution = WorkflowExecution(
            id = testId,
            tenantId = "tenant-default",
            workflowDefId = "wf-task-auto-execute",
            executionStatus = "running",
            currentStateSnapshot = """{"step": 1, "status": "IN_PROGRESS"}""",
            startedAt = System.currentTimeMillis()
        )
        execution.context["testKey"] = "testVal"

        // Test createExecution
        val createResult = repo.createExecution(execution)
        assertTrue(createResult.isSuccess, "createExecution should succeed")

        // Test saveCheckpoint
        val checkpointResult = repo.saveCheckpoint(
            executionId = testId,
            currentStateSnapshot = """{"step": 2, "status": "CHECKPOINT_SAVED"}""",
            lastCompletedNodeId = "node-1-classify",
            status = "running"
        )
        assertTrue(checkpointResult.isSuccess, "saveCheckpoint should succeed")

        // Test getById
        val loaded = repo.getById(testId)
        assertNotNull(loaded, "Execution should be retrievable")
        assertEquals(testId, loaded?.id)
        assertEquals("node-1-classify", loaded?.lastCompletedNodeId)
    }

    @Test
    fun testModelRouterFallbackOrderingFromDatabase() {
        val providerRepo = ProviderRegistryRepository()
        val providers = providerRepo.getAllActive()
        assertFalse(providers.isEmpty(), "Providers table should have registered models")

        val sortedByFallback = providerRepo.getLlmProvidersOrderedByFallbackPriority()
        assertFalse(sortedByFallback.isEmpty(), "Fallback priority list must not be empty")

        // Check fallback ordering respects policies
        val first = sortedByFallback.first()
        assertNotEquals("GEMINI", first.providerCode.uppercase(), "Gemini should not be primary when external providers exist")
    }

    @Test
    fun testSemanticMemoryAndHybridSearch() = runBlocking {
        val memRepo = MemoryDocumentRepository()
        val docId = "doc-test-${UUID.randomUUID().toString().take(8)}"
        val doc = MemoryDocumentRecord(
            id = docId,
            tenantId = "tenant-default",
            title = "Test Foundation Architecture Spec",
            content = "This document validates semantic vector search and hybrid retrieval across memory documents.",
            tags = "ARCHITECTURE",
            embedding = listOf(0.12, 0.45, 0.88, 0.23, 0.05)
        )

        val saveRes = memRepo.upsert(doc)
        assertTrue(saveRes.isSuccess, "Document should save successfully")

        val searchEngine = HybridMemorySearchEngine(memRepo)
        val searchResult = searchEngine.search("tenant-default", "Foundation Architecture", enableReranking = true)
        assertNotNull(searchResult)
        assertTrue(searchResult.topResults.isNotEmpty(), "Hybrid search should find matching documents")
    }

    @Test
    fun testMcpToolRegistryAndExecutor() = runBlocking {
        val registry = McpToolRegistry.defaultRegistry()
        val customTool = McpToolDefinition(
            name = "test_custom_evaluator",
            description = "Evaluates test inputs",
            inputSchema = """{"type": "object", "properties": {"code": {"type": "string"}}}""",
            riskLevel = McpRiskLevel.LOW
        )
        registry.register(customTool)
        assertNotNull(registry.get("test_custom_evaluator"))

        val executor = McpToolExecutor(registry = registry)
        val result = executor.executeTool(
            toolName = "test_custom_evaluator",
            params = mapOf("code" to "verify_all"),
            tenantId = "tenant-default",
            callerRole = "ADMIN"
        )
        assertTrue(result.success, "Tool execution should succeed")
    }

    @Test
    fun testEnterpriseIntegrationFabricCircuitBreakerAndHealth() {
        val fabric = EnterpriseIntegrationFabricService
        FeatureCapabilityService.setTenantTier("tenant-default", TierLevel.ENTERPRISE)
        val conn = fabric.createConnection(
            tenantId = "tenant-default",
            systemName = "SAP ERP Test",
            systemType = "sap",
            connectorKind = "ODATA",
            plainCredentialsJson = """{"apiKey": "test-key"}""",
            endpointUrl = "https://sap.test.internal/odata"
        )
        assertNotNull(conn)

        // Initial health should be valid
        val isHealthy = fabric.checkConnectionHealthAndRateLimit(conn.id)
        assertTrue(isHealthy, "New connection should be healthy by default")

        // Ingest a test record
        val record = fabric.ingestRecord(
            connectionId = conn.id,
            tenantId = "tenant-default",
            recordType = "PO",
            externalRecordId = "PO-9999",
            entityReference = "Order #9999",
            payloadJson = """{"amount": 1500000, "currency": "IDR"}"""
        )
        assertNotNull(record)
        assertEquals("PO-9999", record.externalRecordId)

        // List ingested records
        val records = fabric.listIngestedRecords("tenant-default", "PO")
        assertTrue(records.any { it.externalRecordId == "PO-9999" })
    }

    @Test
    fun testIntelligenceLayerConfidenceCalculation() = runBlocking {
        val confidenceEngine = ConfidenceEngine()
        val score = confidenceEngine.calculateOutputConfidence(
            llmResponseLength = 350,
            modelUsed = "openrouter/anthropic/claude-3-sonnet",
            durationMs = 420L,
            claimedConfidencePct = 90.0,
            tenantId = "tenant-default"
        )
        assertNotNull(score)
        assertTrue(score.rawConfidencePct in 0.0..100.0, "Confidence percentage must be bounded between 0 and 100")
        assertTrue(score.calibratedConfidencePct in 0.0..100.0)
    }
}
