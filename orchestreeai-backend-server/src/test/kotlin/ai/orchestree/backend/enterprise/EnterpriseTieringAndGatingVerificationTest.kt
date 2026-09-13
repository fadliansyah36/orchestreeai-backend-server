package ai.orchestree.backend.enterprise

import ai.orchestree.backend.mcptools.McpExecutionResult
import ai.orchestree.backend.mcptools.McpRiskLevel
import ai.orchestree.backend.mcptools.McpToolExecutor
import ai.orchestree.backend.mcptools.McpToolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Enterprise AI Workforce Foundation Test (PRD Addendum 2 Bagian 57, 58, 59)
 * Verifies:
 * - Feature Capability Tiering & Gating (Bagian 57)
 * - Downgrade Risk Handling (Bagian 57.4)
 * - Third-Party Integration Fabric (Bagian 58)
 * - Permission-First Architecture / ABAC Default-Deny (Bagian 59)
 */
class EnterpriseTieringAndGatingVerificationTest {

    @BeforeEach
    fun setUp() {
        FeatureCapabilityService.setTenantTier("tenant-starter-test", TierLevel.STARTER)
        FeatureCapabilityService.setTenantTier("tenant-growth-test", TierLevel.GROWTH)
        FeatureCapabilityService.setTenantTier("tenant-enterprise-test", TierLevel.ENTERPRISE)
    }

    // ==========================================
    // LANGKAH 1: Feature Tiering & Gating (Bagian 57)
    // ==========================================

    @Test
    fun testAllTierCapabilitiesAllowedForStarterAndGrowth() {
        // All-tier capabilities should be enabled for STARTER and GROWTH
        val allTierCaps = listOf(
            "ai_execution_layer",
            "ai_action_orchestration",
            "ai_monitoring_loop",
            "continuous_learning_core",
            "ai_finance_intelligence",
            "ai_knowledge_operational_fusion",
            "ai_event_engine"
        )

        for (cap in allTierCaps) {
            assertTrue(
                FeatureCapabilityService.isCapabilityEnabled("tenant-starter-test", cap),
                "Starter tenant should have access to all-tier capability: $cap"
            )
            assertTrue(
                FeatureCapabilityService.isCapabilityEnabled("tenant-growth-test", cap),
                "Growth tenant should have access to all-tier capability: $cap"
            )
            assertDoesNotThrow {
                FeatureCapabilityService.enforceCapabilityGate("tenant-starter-test", cap)
            }
        }
    }

    @Test
    fun testEnterpriseOnlyCapabilitiesBlockedForStarterAndGrowth() {
        val enterpriseCaps = listOf(
            "integration_fabric",
            "company_context_fabric",
            "cross_system_intelligence",
            "specialist_agents_heavy_industry",
            "ai_chief_of_staff",
            "enterprise_command_center",
            "enterprise_reporting"
        )

        for (cap in enterpriseCaps) {
            assertFalse(
                FeatureCapabilityService.isCapabilityEnabled("tenant-starter-test", cap),
                "Starter tenant must NOT have enterprise capability: $cap"
            )
            assertFalse(
                FeatureCapabilityService.isCapabilityEnabled("tenant-growth-test", cap),
                "Growth tenant must NOT have enterprise capability: $cap"
            )
            assertThrows(CapabilityNotAvailableException::class.java) {
                FeatureCapabilityService.enforceCapabilityGate("tenant-growth-test", cap)
            }
        }
    }

    @Test
    fun testEnterpriseCapabilitiesAllowedForEnterpriseTier() {
        assertTrue(
            FeatureCapabilityService.isCapabilityEnabled("tenant-enterprise-test", "integration_fabric"),
            "Enterprise tenant must have integration_fabric"
        )
        assertTrue(
            FeatureCapabilityService.isCapabilityEnabled("tenant-enterprise-test", "ai_chief_of_staff"),
            "Enterprise tenant must have ai_chief_of_staff"
        )
        assertDoesNotThrow {
            FeatureCapabilityService.enforceCapabilityGate("tenant-enterprise-test", "integration_fabric")
            FeatureCapabilityService.enforceCapabilityGate("tenant-enterprise-test", "ai_chief_of_staff")
        }
    }

    @Test
    fun testTenantCapabilityOverrideAllowsGrowthTrial() {
        val tenantId = "tenant-growth-override-test"
        FeatureCapabilityService.setTenantTier(tenantId, TierLevel.GROWTH)

        // Baseline: blocked
        assertFalse(FeatureCapabilityService.isCapabilityEnabled(tenantId, "integration_fabric"))

        // Add override for trial
        FeatureCapabilityService.setTenantOverride(
            tenantId = tenantId,
            capabilityKey = "integration_fabric",
            enabled = true,
            reason = "30-day Enterprise Trial"
        )

        assertTrue(
            FeatureCapabilityService.isCapabilityEnabled(tenantId, "integration_fabric"),
            "Override must allow capability even on Growth tier"
        )
        assertDoesNotThrow {
            FeatureCapabilityService.enforceCapabilityGate(tenantId, "integration_fabric")
        }

        // Revoke override
        FeatureCapabilityService.setTenantOverride(
            tenantId = tenantId,
            capabilityKey = "integration_fabric",
            enabled = false,
            reason = "Trial expired"
        )

        assertFalse(
            FeatureCapabilityService.isCapabilityEnabled(tenantId, "integration_fabric"),
            "Revoked override must block capability"
        )
    }

    @Test
    fun testTenantDowngradeIsolationBagian57_4() {
        val tenantId = "tenant-downgrade-demo"
        FeatureCapabilityService.setTenantTier(tenantId, TierLevel.ENTERPRISE)

        // Register connection and verify healthy
        val conn = EnterpriseIntegrationFabricService.createConnection(
            tenantId = tenantId,
            systemName = "SAP ERP Production",
            systemType = "erp",
            connectorKind = "API_KEY",
            plainCredentialsJson = """{"apiKey": "sap-secret-token-123"}""",
            endpointUrl = "https://sap.enterprise.internal/api"
        )
        assertEquals("HEALTHY", conn.status)
        assertTrue(FeatureCapabilityService.isChiefOfStaffEventAcceptanceAllowed(tenantId))

        // Trigger downgrade to GROWTH
        val report = FeatureCapabilityService.handleTenantDowngrade(tenantId, "ENTERPRISE", "GROWTH")
        assertEquals(TierLevel.GROWTH, FeatureCapabilityService.getTenantTier(tenantId))
        assertTrue(report.chiefOfStaffReadOnly, "Chief of Staff must be set to read-only")
        assertFalse(FeatureCapabilityService.isChiefOfStaffEventAcceptanceAllowed(tenantId), "Chief of staff events must be rejected")

        // Integration Fabric gate must now fail
        assertThrows(CapabilityNotAvailableException::class.java) {
            FeatureCapabilityService.enforceCapabilityGate(tenantId, "integration_fabric")
        }
    }

    // ==========================================
    // LANGKAH 2: Third-Party Integration Fabric (Bagian 58)
    // ==========================================

    @Test
    fun testIntegrationFabricConnectionCreationGated() {
        // Growth tier cannot create connection
        assertThrows(CapabilityNotAvailableException::class.java) {
            EnterpriseIntegrationFabricService.createConnection(
                tenantId = "tenant-growth-test",
                systemName = "Oracle ERP",
                systemType = "erp",
                connectorKind = "OAUTH2",
                plainCredentialsJson = """{"clientId": "oracle-id"}""",
                endpointUrl = "https://oracle.internal"
            )
        }

        // Enterprise tier can create connection
        val conn = EnterpriseIntegrationFabricService.createConnection(
            tenantId = "tenant-enterprise-test",
            systemName = "Oracle ERP",
            systemType = "erp",
            connectorKind = "OAUTH2",
            plainCredentialsJson = """{"clientId": "oracle-id", "secret": "s3cr3t"}""",
            endpointUrl = "https://oracle.internal"
        )
        assertNotNull(conn.id)
        assertEquals("oracle erp", conn.systemName.lowercase())
        assertTrue(conn.credentialRef.isNotBlank(), "Credentials must be encrypted")
        assertFalse(conn.credentialRef.contains("s3cr3t"), "Plaintext secret must never be stored in credentialRef")
    }

    @Test
    fun testCircuitBreakerTripsAfterConsecutiveFailures() {
        val tenantId = "tenant-enterprise-test"
        val conn = EnterpriseIntegrationFabricService.createConnection(
            tenantId = tenantId,
            systemName = "CMMS Fleet Engine",
            systemType = "cmms",
            connectorKind = "BEARER",
            plainCredentialsJson = """{"token": "cmms-tok"}""",
            endpointUrl = "https://cmms.enterprise.internal"
        )

        assertTrue(EnterpriseIntegrationFabricService.checkConnectionHealthAndRateLimit(conn.id))

        // Record 4 failures -> breaker not yet tripped
        for (i in 1..4) {
            EnterpriseIntegrationFabricService.recordConnectionFailure(conn.id, "HTTP 504 Gateway Timeout attempt $i")
            assertFalse(conn.circuitBreakerTripped)
        }

        // 5th failure -> trips breaker
        EnterpriseIntegrationFabricService.recordConnectionFailure(conn.id, "HTTP 504 Gateway Timeout attempt 5")
        assertTrue(conn.circuitBreakerTripped, "Breaker must trip after 5 failures")
        assertEquals("ERROR", conn.status)

        // Blocked by circuit breaker
        assertFalse(EnterpriseIntegrationFabricService.checkConnectionHealthAndRateLimit(conn.id))

        // Recover on successful ping
        EnterpriseIntegrationFabricService.recordConnectionSuccess(conn.id)
        assertFalse(conn.circuitBreakerTripped)
        assertEquals("HEALTHY", conn.status)
        assertTrue(EnterpriseIntegrationFabricService.checkConnectionHealthAndRateLimit(conn.id))
    }

    // ==========================================
    // LANGKAH 3: Permission-First ABAC (Bagian 59)
    // ==========================================

    @Test
    fun testAiAgentDefaultDenyTotalWithoutPolicy() {
        val tenantId = "tenant-enterprise-test"
        val agentId = "agent-maintenance-ai"
        val connectionId = "conn-sap-heavy-industry"

        // PRD Bagian 59.3 Rule: Default-Deny Total
        val decision = AiDataPermissionService.checkAiDataPermission(
            tenantId = tenantId,
            agentId = agentId,
            connectionId = connectionId,
            recordType = "EQUIPMENT",
            requiredAccessLevel = "READ_ONLY"
        )

        assertFalse(decision.allowed, "AI Agent must be denied access without policy")
        assertEquals("DENIED_NO_POLICY", decision.decision)
        assertTrue(decision.reason.contains("has no granted ABAC policy"))
    }

    @Test
    fun testAiAgentGrantedPolicyAllowsAccessAndEnforcesScopeAndLevel() {
        val tenantId = "tenant-enterprise-test"
        val agentId = "agent-finance-ai"
        val connectionId = "conn-oracle-financials"

        // Grant READ_ONLY policy for "INVOICE" records only
        AiDataPermissionService.grantPolicy(
            AiDataPermissionPolicy(
                id = "pol-fin-01",
                tenantId = tenantId,
                agentId = agentId,
                connectionId = connectionId,
                accessLevel = "READ_ONLY",
                allowedTablesOrTypes = listOf("INVOICE")
            )
        )

        // 1. Permitted scope & level
        val allowedDecision = AiDataPermissionService.checkAiDataPermission(
            tenantId = tenantId,
            agentId = agentId,
            connectionId = connectionId,
            recordType = "INVOICE",
            requiredAccessLevel = "READ_ONLY"
        )
        assertTrue(allowedDecision.allowed)
        assertEquals("ALLOWED", allowedDecision.decision)

        // 2. Denied scope (EQUIPMENT is not allowed in policy)
        val deniedScopeDecision = AiDataPermissionService.checkAiDataPermission(
            tenantId = tenantId,
            agentId = agentId,
            connectionId = connectionId,
            recordType = "EQUIPMENT",
            requiredAccessLevel = "READ_ONLY"
        )
        assertFalse(deniedScopeDecision.allowed)
        assertEquals("DENIED_SCOPE", deniedScopeDecision.decision)

        // 3. Denied level (EXECUTE exceeds READ_ONLY policy)
        val deniedLevelDecision = AiDataPermissionService.checkAiDataPermission(
            tenantId = tenantId,
            agentId = agentId,
            connectionId = connectionId,
            recordType = "INVOICE",
            requiredAccessLevel = "EXECUTE"
        )
        assertFalse(deniedLevelDecision.allowed)
        assertEquals("DENIED_INSUFFICIENT_ACCESS_LEVEL", deniedLevelDecision.decision)
    }

    @Test
    fun testMcpToolExecutorGatingAndAbacEnforcement() = runBlocking {
        val executor = McpToolExecutor()
        val tenantId = "tenant-enterprise-test"

        // Test 1: Enterprise tool on Growth tenant -> blocked by capability tier gate
        assertThrows(CapabilityNotAvailableException::class.java) {
            runBlocking {
                executor.executeTool(
                    toolName = "enterprise_fetch_record",
                    params = mapOf("connectionId" to "conn-1", "recordType" to "PO"),
                    tenantId = "tenant-growth-test",
                    callerRole = "STAFF_HUMAN"
                )
            }
        }

        // Test 2: Enterprise tool on Enterprise tenant called by AI Agent WITHOUT policy -> Blocked by ABAC DENIED_NO_POLICY
        val aiResultNoPolicy = executor.executeTool(
            toolName = "enterprise_fetch_record",
            params = mapOf("agentId" to "agent-chief-ai", "connectionId" to "conn-sap", "recordType" to "PO"),
            tenantId = tenantId,
            callerRole = "AI_AGENT"
        )
        assertFalse(aiResultNoPolicy.success, "AI Agent without policy must fail")
        assertTrue(aiResultNoPolicy.isBlockedByGovernance, "Must be blocked by governance")
        assertTrue(aiResultNoPolicy.errorMessage?.contains("DENIED_NO_POLICY") == true, "Must contain DENIED_NO_POLICY, got: ${aiResultNoPolicy.errorMessage}")

        // Test 3: Grant policy to AI Agent -> Execution succeeds
        AiDataPermissionService.grantPolicy(
            AiDataPermissionPolicy(
                id = "pol-chief-01",
                tenantId = tenantId,
                agentId = "agent-chief-ai",
                connectionId = "conn-sap",
                accessLevel = "READ_ONLY",
                allowedTablesOrTypes = listOf("PO", "*")
            )
        )

        val aiResultWithPolicy = executor.executeTool(
            toolName = "enterprise_fetch_record",
            params = mapOf("agentId" to "agent-chief-ai", "connectionId" to "conn-sap", "recordType" to "PO"),
            tenantId = tenantId,
            callerRole = "AI_AGENT"
        )
        assertTrue(aiResultWithPolicy.success, "AI Agent with policy must succeed")
        assertFalse(aiResultWithPolicy.isBlockedByGovernance)
        assertTrue(aiResultWithPolicy.output.contains("SYNCHRONIZED") || aiResultWithPolicy.output.contains("found"))
    }
}
