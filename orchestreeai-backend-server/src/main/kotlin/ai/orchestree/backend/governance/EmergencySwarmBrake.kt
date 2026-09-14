package ai.orchestree.backend.governance

import ai.orchestree.backend.security.AuditLogger
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Serializable
data class SwarmFreezeDetail(
    val scope: String, // 'PLATFORM' or 'TENANT'
    val tenantId: String? = null,
    val frozenBy: String,
    val reason: String,
    val frozenAt: Long = System.currentTimeMillis()
)

@Serializable
data class SwarmBrakeStatusResponse(
    val isPlatformFrozen: Boolean,
    val isTenantFrozen: Boolean,
    val isBlocked: Boolean,
    val activeFreezes: List<SwarmFreezeDetail>
)

class SwarmFrozenException(message: String) : RuntimeException(message)

/**
 * Emergency Swarm Brake (PRD Addendum 2 / Governance Kill-Switch)
 * Provides platform-wide and tenant-level emergency brakes to freeze all AI agent executions,
 * workflows, and autonomous action dispatches immediately upon anomaly or security breach.
 */
object EmergencySwarmBrake {
    private val logger = LoggerFactory.getLogger(EmergencySwarmBrake::class.java)
    private val auditLogger = AuditLogger()

    private val platformFrozen = AtomicBoolean(false)
    private var platformFreezeDetail: SwarmFreezeDetail? = null
    private val tenantFreezes = ConcurrentHashMap<String, SwarmFreezeDetail>()

    /**
     * Freezes entire platform swarm execution immediately.
     */
    fun freezePlatform(operatorId: String, reason: String): SwarmFreezeDetail {
        platformFrozen.set(true)
        val detail = SwarmFreezeDetail(
            scope = "PLATFORM",
            tenantId = null,
            frozenBy = operatorId,
            reason = reason
        )
        platformFreezeDetail = detail
        logger.error("[EMERGENCY_SWARM_BRAKE] PLATFORM-WIDE SWARM FROZEN by $operatorId. Reason: $reason")
        auditLogger.log(
            tenantId = "system",
            actor = operatorId,
            action = "swarm_emergency_brake_activated",
            details = "scope=PLATFORM reason=$reason"
        )
        return detail
    }

    /**
     * Resumes platform-wide swarm execution.
     */
    fun resumePlatform(operatorId: String): Boolean {
        val wasFrozen = platformFrozen.getAndSet(false)
        platformFreezeDetail = null
        logger.info("[EMERGENCY_SWARM_BRAKE] PLATFORM-WIDE SWARM RESUMED by $operatorId")
        auditLogger.log(
            tenantId = "system",
            actor = operatorId,
            action = "swarm_emergency_brake_resumed",
            details = "scope=PLATFORM"
        )
        return wasFrozen
    }

    /**
     * Freezes swarm execution for a specific tenant.
     */
    fun freezeTenant(tenantId: String, operatorId: String, reason: String): SwarmFreezeDetail {
        val detail = SwarmFreezeDetail(
            scope = "TENANT",
            tenantId = tenantId,
            frozenBy = operatorId,
            reason = reason
        )
        tenantFreezes[tenantId] = detail
        logger.warn("[EMERGENCY_SWARM_BRAKE] Tenant '$tenantId' swarm frozen by $operatorId. Reason: $reason")
        auditLogger.log(
            tenantId = tenantId,
            actor = operatorId,
            action = "tenant_swarm_brake_activated",
            details = "tenant=$tenantId reason=$reason"
        )
        return detail
    }

    /**
     * Resumes swarm execution for a specific tenant.
     */
    fun resumeTenant(tenantId: String, operatorId: String): Boolean {
        val removed = tenantFreezes.remove(tenantId)
        if (removed != null) {
            logger.info("[EMERGENCY_SWARM_BRAKE] Tenant '$tenantId' swarm resumed by $operatorId")
            auditLogger.log(
                tenantId = tenantId,
                actor = operatorId,
                action = "tenant_swarm_brake_resumed",
                details = "tenant=$tenantId"
            )
            return true
        }
        return false
    }

    /**
     * Checks whether the swarm is frozen for the given tenant (or platform-wide).
     */
    fun isSwarmFrozen(tenantId: String): Boolean {
        if (platformFrozen.get()) return true
        return tenantFreezes.containsKey(tenantId)
    }

    /**
     * Asserts that swarm execution is active. Throws SwarmFrozenException if brake is engaged.
     */
    fun assertSwarmActive(tenantId: String) {
        if (platformFrozen.get()) {
            val detail = platformFreezeDetail
            throw SwarmFrozenException(
                "EMERGENCY SWARM BRAKE ACTIVE: Platform-wide AI swarm execution is frozen by ${detail?.frozenBy ?: "admin"}. Reason: ${detail?.reason ?: "Emergency stop"}"
            )
        }
        val tenantDetail = tenantFreezes[tenantId]
        if (tenantDetail != null) {
            throw SwarmFrozenException(
                "EMERGENCY SWARM BRAKE ACTIVE: Swarm execution for tenant '$tenantId' is frozen by ${tenantDetail.frozenBy}. Reason: ${tenantDetail.reason}"
            )
        }
    }

    /**
     * Gets status of swarm brakes for tenant and platform.
     */
    fun getStatus(tenantId: String): SwarmBrakeStatusResponse {
        val pFrozen = platformFrozen.get()
        val tFrozen = tenantFreezes.containsKey(tenantId)
        val active = mutableListOf<SwarmFreezeDetail>()
        platformFreezeDetail?.let { active.add(it) }
        tenantFreezes[tenantId]?.let { active.add(it) }

        return SwarmBrakeStatusResponse(
            isPlatformFrozen = pFrozen,
            isTenantFrozen = tFrozen,
            isBlocked = pFrozen || tFrozen,
            activeFreezes = active
        )
    }
}
