package ai.orchestree.backend.billing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Entitlement Engine (Bagian C - Langkah 2)
 *
 * MENEGAKKAN:
 * "suspend fun enforceEntitlement(tenantId: String, featureKey: String): Boolean"
 * WAJIB dipasang sebagai gate di SETIAP endpoint fitur berbayar.
 * Mengkonsolidasikan capability gating dan entitlement checks.
 */
class EntitlementEngine(
    private val repoManager: CreditRepositoryManager = CreditRepositoryManager()
) {
    private val logger = LoggerFactory.getLogger(EntitlementEngine::class.java)

    companion object {
        val defaultInstance: EntitlementEngine by lazy { EntitlementEngine() }
    }

    /**
     * LANGKAH 2 — ENTITLEMENT ENGINE: MIDDLEWARE PENGECEKAN DI SETIAP FITUR
     * suspend fun enforceEntitlement(tenantId: String, featureKey: String): Boolean {
     *     val subscription = tenantSubscriptionRepo.get(tenantId)
     *     if (subscription.status !in listOf("active","trial")) return false  // grace period tetap aktif
     *     val entitlement = planFeatureEntitlementRepo.get(subscription.planId, featureKey)
     *     return entitlement.entitlementValue != "not_included"
     * }
     */
    suspend fun enforceEntitlement(tenantId: String, featureKey: String): Boolean = withContext(Dispatchers.IO) {
        val subscription = repoManager.getTenantSubscription(tenantId) ?: return@withContext false

        // Founder exclusive has full access to all capabilities
        if (subscription.isFounderExclusive) {
            return@withContext true
        }

        // Active, trial, and grace period are allowed (grace period tetap aktif)
        if (subscription.status.lowercase() !in listOf("active", "trial", "grace_period")) {
            return@withContext false
        }

        // Check custom tenant override first
        val customOverride = subscription.customEntitlementOverride
        if (!customOverride.isNullOrBlank() && customOverride.contains(featureKey)) {
            if (customOverride.contains("\"$featureKey\": \"not_included\"") ||
                customOverride.contains("\"$featureKey\":\"not_included\"")
            ) {
                return@withContext false
            }
            if (customOverride.contains("\"$featureKey\": \"included\"") ||
                customOverride.contains("\"$featureKey\":\"included\"")
            ) {
                return@withContext true
            }
        }

        val entitlement = repoManager.getPlanFeatureEntitlement(subscription.planId, featureKey)
        if (entitlement != null) {
            return@withContext entitlement.entitlementValue != "not_included"
        }

        // Default fallback based on plan tier
        val plan = repoManager.getCommercialPlan(subscription.planId)
        when (plan.planCode.lowercase()) {
            "starter", "community" -> {
                featureKey in listOf("universal_selection", "omnichannel_chat", "basic_analytics")
            }
            else -> true
        }
    }

    suspend fun getTenantEntitlements(tenantId: String): TenantEntitlementsResponse = withContext(Dispatchers.IO) {
        val subscription = repoManager.getTenantSubscription(tenantId)
        val plan = subscription?.let { repoManager.getCommercialPlan(it.planId) }
        val baseEntitlements = plan?.let { repoManager.getAllFeatureEntitlementsForPlan(it.id) } ?: emptyMap()

        val activeEntitlements = baseEntitlements.toMutableMap()
        val customOverride = subscription?.customEntitlementOverride
        if (!customOverride.isNullOrBlank()) {
            val regex = Regex("\"([a-zA-Z0-9_]+)\"\\s*:\\s*\"([a-zA-Z0-9_]+)\"")
            for (match in regex.findAll(customOverride)) {
                val key = match.groupValues[1]
                val value = match.groupValues[2]
                activeEntitlements[key] = value
            }
        }

        TenantEntitlementsResponse(
            tenantId = tenantId,
            planId = plan?.id ?: "unknown",
            planCode = plan?.planCode ?: "starter",
            planName = plan?.planName ?: "Starter Plan",
            status = subscription?.status ?: "active",
            isFounderExclusive = subscription?.isFounderExclusive ?: false,
            entitlements = activeEntitlements
        )
    }
}

/**
 * Gate middleware to consolidate capability gating and entitlement checks.
 * Responds with HTTP 403 Forbidden if not entitled.
 */
suspend fun ApplicationCall.enforceEntitlementGate(
    featureKey: String,
    entitlementEngine: EntitlementEngine = EntitlementEngine.defaultInstance
): Boolean {
    val principal = this.principal<JWTPrincipal>()
    val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
        ?: this.request.headers["X-Tenant-Id"]
        ?: this.request.queryParameters["tenant_id"]
        ?: "tenant-enterprise-001"

    val isAllowed = entitlementEngine.enforceEntitlement(tenantId, featureKey)
    if (!isAllowed) {
        this.respond(
            HttpStatusCode.Forbidden,
            mapOf(
                "error" to "EntitlementRequired",
                "feature" to featureKey,
                "message" to "Fitur '$featureKey' tidak termasuk dalam paket langganan aktif tenant Anda. Silakan upgrade paket langganan."
            )
        )
        return false
    }
    return true
}
