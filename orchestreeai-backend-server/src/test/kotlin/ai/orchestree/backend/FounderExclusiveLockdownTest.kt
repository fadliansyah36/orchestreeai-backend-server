package ai.orchestree.backend

import ai.orchestree.backend.billing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FounderExclusiveLockdownTest {

    private val dbManager = DatabaseManager
    private val repoManager = CreditRepositoryManager()
    private val commercialEngine = CommercialCreditEngine()
    private val entitlementEngine = EntitlementEngine.defaultInstance

    @Test
    fun verifyFounderExclusiveTenantsAndLockdown() = runBlocking {
        val founderTenants = listOf("tenant-orchestreeai", "tenant-trexio-adventure")

        for (tenantId in founderTenants) {
            // 1. Subscription is active, founder exclusive, and never expires
            val sub = repoManager.getTenantSubscription(tenantId)
            assertNotNull(sub, "Subscription must exist for $tenantId")
            assertTrue(sub.isFounderExclusive, "Subscription must have isFounderExclusive=true")
            assertEquals("active", sub.status.lowercase())
            assertEquals(null, sub.currentPeriodEnd, "Founder subscription must never expire")

            // 2. Entitlement bypasses all feature checks
            val testFeatures = listOf("universal_selection", "omnichannel_chat", "company_brain_rag", "custom_mcp_tools")
            for (feat in testFeatures) {
                assertTrue(entitlementEngine.enforceEntitlement(tenantId, feat), "Feature $feat must be granted to founder")
            }

            // 3. Wallet is unlimited
            val wallet = repoManager.getWallet(tenantId)
            assertTrue(wallet.isUnlimited, "AiCreditWallet must be unlimited for $tenantId")

            // 4. Metering is bypassed
            var executed = false
            val context = CreditCostContext(activityType = "ai_agent_task")
            val res = commercialEngine.executeWithCreditLifecycle(tenantId, context) {
                executed = true
                TaskExecutionResult(referenceId = "test-ref")
            }
            assertTrue(executed)
            assertEquals("test-ref", res.referenceId)

            // 5. Lockdown against cancellation, upgrade, downgrade, start new
            assertFailsWith<IllegalStateException> { commercialEngine.cancelSubscription(tenantId) }
            val starterPlan = repoManager.getAllActiveCommercialPlans().first { it.planCode == "starter" }
            assertFailsWith<IllegalStateException> { commercialEngine.upgradeSubscription(tenantId, starterPlan.id) }
            assertFailsWith<IllegalStateException> { commercialEngine.downgradeSubscription(tenantId, starterPlan.id) }
            assertFailsWith<IllegalStateException> { commercialEngine.startSubscription(tenantId, "starter") }

            // 6. Dunning exemption
            val dunning = DunningEngine.handleInvoiceFailure(tenantId, "INV-001", 1000.0, "Test")
            assertEquals("RESOLVED", dunning.status)
        }

        // 7. Commercial plans endpoint hides founder_exclusive
        val publicPlans = repoManager.getAllActiveCommercialPlans()
        assertTrue(publicPlans.none { it.planCode.equals("founder_exclusive", ignoreCase = true) })
    }
}
