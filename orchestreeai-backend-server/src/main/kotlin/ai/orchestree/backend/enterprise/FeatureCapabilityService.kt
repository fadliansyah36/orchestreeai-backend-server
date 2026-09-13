package ai.orchestree.backend.enterprise

import ai.orchestree.backend.billing.DatabaseManager
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * PRD Addendum 2 Bagian 57.3:
 * Exception thrown when a tenant attempts to use a capability above their plan tier without an active override.
 */
class CapabilityNotAvailableException(
    val tenantId: String,
    val capabilityKey: String,
    val requiredTier: String,
    message: String = "Feature capability '$capabilityKey' is not available for tenant '$tenantId'. Requires tier: $requiredTier"
) : RuntimeException(message)

enum class TierLevel(val level: Int, val planCode: String, val tierName: String) {
    STARTER(1, "starter", "STARTER"),
    GROWTH(2, "growth", "GROWTH"),
    ENTERPRISE(3, "enterprise", "ENTERPRISE"),
    CUSTOM(4, "custom", "CUSTOM");

    companion object {
        fun fromString(str: String): TierLevel {
            return when (str.trim().uppercase()) {
                "STARTER", "PLAN-STARTER" -> STARTER
                "GROWTH", "PLAN-GROWTH" -> GROWTH
                "ENTERPRISE", "PLAN-ENTERPRISE" -> ENTERPRISE
                "CUSTOM", "PLAN-CUSTOM" -> CUSTOM
                else -> STARTER
            }
        }

        fun fromLevel(level: Int): TierLevel {
            return entries.firstOrNull { it.level == level } ?: STARTER
        }
    }
}

data class FeatureCapabilityDef(
    val capabilityKey: String,
    val capabilityName: String,
    val category: String, // 'ALL_TIER' or 'ENTERPRISE_ONLY'
    val minTierLevel: Int,
    val description: String = "",
    val isSystemCore: Boolean = true
)

data class TenantOverride(
    val tenantId: String,
    val capabilityKey: String,
    val enabledOverride: Boolean,
    val reason: String = "",
    val expiresAt: Instant? = null
)

data class DowngradeReport(
    val tenantId: String,
    val previousTier: String,
    val newTier: String,
    val suspendedConnectionsCount: Int,
    val suspendedJobsCount: Int,
    val chiefOfStaffReadOnly: Boolean,
    val message: String
)

/**
 * Feature Capability & Tiering Engine
 * Implements PRD Addendum 2 Bagian 57: Feature Tiering & Gating Architecture.
 */
object FeatureCapabilityService {
    private val logger = LoggerFactory.getLogger(FeatureCapabilityService::class.java)

    // Matriks Kapabilitas Resmi Bagian 57.2
    val CAPABILITY_MATRIX: Map<String, FeatureCapabilityDef> = mapOf(
        // All-Tier Capabilities (min_tier_level = 1)
        "ai_execution_layer" to FeatureCapabilityDef(
            capabilityKey = "ai_execution_layer",
            capabilityName = "AI Execution Layer",
            category = "ALL_TIER",
            minTierLevel = 1,
            description = "Eksekusi alur kerja dan tindakan AI dengan verifikasi persetujuan manusia."
        ),
        "ai_action_orchestration" to FeatureCapabilityDef(
            capabilityKey = "ai_action_orchestration",
            capabilityName = "AI Action Orchestration",
            category = "ALL_TIER",
            minTierLevel = 1,
            description = "Orkestrasi alur kerja multi-langkah dan integrasi internal OrchestreeAI."
        ),
        "ai_monitoring_loop" to FeatureCapabilityDef(
            capabilityKey = "ai_monitoring_loop",
            capabilityName = "Workforce Monitoring Loop",
            category = "ALL_TIER",
            minTierLevel = 1,
            description = "Closed-Loop Workforce Monitoring dari deteksi hingga verifikasi penyelesaian objektif."
        ),
        "continuous_learning_core" to FeatureCapabilityDef(
            capabilityKey = "continuous_learning_core",
            capabilityName = "Continuous Learning Core",
            category = "ALL_TIER",
            minTierLevel = 1,
            description = "Closed-loop Continuous Learning Core untuk peningkatan skill AI berbasis hasil terverifikasi."
        ),
        "ai_finance_intelligence" to FeatureCapabilityDef(
            capabilityKey = "ai_finance_intelligence",
            capabilityName = "AI Finance Intelligence",
            category = "ALL_TIER",
            minTierLevel = 1,
            description = "Analitik tekanan arus kas, umur piutang/utang (AR/AP), dan peramalan keuangan data internal."
        ),
        "ai_knowledge_operational_fusion" to FeatureCapabilityDef(
            capabilityKey = "ai_knowledge_operational_fusion",
            capabilityName = "AI Knowledge & Operational Fusion",
            category = "ALL_TIER",
            minTierLevel = 1,
            description = "Penyatuan dinamis SOP Company Brain dengan metrik operasional internal."
        ),
        "ai_event_engine" to FeatureCapabilityDef(
            capabilityKey = "ai_event_engine",
            capabilityName = "AI Event Engine",
            category = "ALL_TIER",
            minTierLevel = 1,
            description = "Event-driven AI dispatcher untuk aktivasi proaktif agen spesialis sesuai kejadian."
        ),

        // Enterprise-Only Capabilities (min_tier_level = 3)
        "integration_fabric" to FeatureCapabilityDef(
            capabilityKey = "integration_fabric",
            capabilityName = "Third-Party Integration Fabric",
            category = "ENTERPRISE_ONLY",
            minTierLevel = 3,
            description = "Third-Party Integration Fabric penuh ke SAP, Oracle, MS Dynamics, Odoo, CMMS, Fleet, & HRIS eksternal."
        ),
        "company_context_fabric" to FeatureCapabilityDef(
            capabilityKey = "company_context_fabric",
            capabilityName = "Company Context Fabric",
            category = "ENTERPRISE_ONLY",
            minTierLevel = 3,
            description = "Penyatuan 8 dimensi konteks perusahaan secara holistik dari sistem pihak ketiga."
        ),
        "cross_system_intelligence" to FeatureCapabilityDef(
            capabilityKey = "cross_system_intelligence",
            capabilityName = "Cross-System Intelligence",
            category = "ENTERPRISE_ONLY",
            minTierLevel = 3,
            description = "Korelasi sinyal anomali dan deteksi risiko lintas sistem enterprise pihak ketiga."
        ),
        "specialist_agents_heavy_industry" to FeatureCapabilityDef(
            capabilityKey = "specialist_agents_heavy_industry",
            capabilityName = "Specialist Agents for Heavy Industry",
            category = "ENTERPRISE_ONLY",
            minTierLevel = 3,
            description = "Agen AI Spesialis industri berat (Maintenance, Fleet, Project, HSE, Engineering)."
        ),
        "ai_chief_of_staff" to FeatureCapabilityDef(
            capabilityKey = "ai_chief_of_staff",
            capabilityName = "AI Chief of Staff (5-Star)",
            category = "ENTERPRISE_ONLY",
            minTierLevel = 3,
            description = "AI Chief of Staff (5-Bintang) untuk koordinasi eksekutif dan sintesis briefing lintas departemen."
        ),
        "enterprise_command_center" to FeatureCapabilityDef(
            capabilityKey = "enterprise_command_center",
            capabilityName = "Enterprise Command Center",
            category = "ENTERPRISE_ONLY",
            minTierLevel = 3,
            description = "Enterprise Command Center Dashboard dengan visualisasi Health Score lintas entitas."
        ),
        "enterprise_reporting" to FeatureCapabilityDef(
            capabilityKey = "enterprise_reporting",
            capabilityName = "Enterprise Reporting",
            category = "ENTERPRISE_ONLY",
            minTierLevel = 3,
            description = "Automatic Reporting lintas sistem pihak ketiga dan pelaporan proaktif seketika."
        )
    )

    // In-memory caches for high-speed gating
    private val tenantTierCache = ConcurrentHashMap<String, TierLevel>()
    private val tenantOverridesCache = ConcurrentHashMap<String, ConcurrentHashMap<String, TenantOverride>>()
    private val chiefOfStaffSuspendedTenants = ConcurrentHashMap.newKeySet<String>()

    init {
        // Preload default system tenants
        tenantTierCache["tenant-default"] = TierLevel.ENTERPRISE
        tenantTierCache["system"] = TierLevel.CUSTOM
    }

    fun setTenantTier(tenantId: String, tier: TierLevel) {
        tenantTierCache[tenantId] = tier
        logger.info("[FEATURE_GATING] Tenant '$tenantId' tier set to ${tier.name} (level: ${tier.level})")
    }

    fun getTenantTier(tenantId: String): TierLevel {
        // 1. Check in-memory cache
        val cached = tenantTierCache[tenantId]
        if (cached != null) return cached

        // 2. Check Database tenants / subscriptions if available
        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = """
                    SELECT COALESCE(sp.tier, t.subscription_tier, 'STARTER') as plan_tier,
                           COALESCE(sp.tier_level, 1) as plan_level
                    FROM tenants t
                    LEFT JOIN subscription_plans sp ON sp.id = t.subscription_plan_id OR sp.tier = UPPER(t.subscription_tier)
                    WHERE t.id = ?
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val tierStr = rs.getString("plan_tier") ?: "STARTER"
                            val tier = TierLevel.fromString(tierStr)
                            tenantTierCache[tenantId] = tier
                            return tier
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.debug("[FEATURE_GATING] DB lookup failed for tenant $tenantId: ${e.message}")
        }

        // Default to STARTER if not found
        val fallback = TierLevel.STARTER
        tenantTierCache[tenantId] = fallback
        return fallback
    }

    fun setTenantOverride(tenantId: String, capabilityKey: String, enabled: Boolean, reason: String = "", expiresAt: Instant? = null) {
        val map = tenantOverridesCache.computeIfAbsent(tenantId) { ConcurrentHashMap() }
        map[capabilityKey] = TenantOverride(tenantId, capabilityKey, enabled, reason, expiresAt)
        logger.info("[FEATURE_GATING] Override set for tenant '$tenantId', cap '$capabilityKey' -> $enabled ($reason)")

        // Sync to DB
        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = """
                    INSERT INTO tenant_capability_overrides (id, tenant_id, capability_key, enabled_override, reason, expires_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (tenant_id, capability_key) DO UPDATE SET
                        enabled_override = EXCLUDED.enabled_override,
                        reason = EXCLUDED.reason,
                        expires_at = EXCLUDED.expires_at,
                        updated_at = CURRENT_TIMESTAMP
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, "ovr-${java.util.UUID.randomUUID().toString().take(12)}")
                    ps.setString(2, tenantId)
                    ps.setString(3, capabilityKey)
                    ps.setBoolean(4, enabled)
                    ps.setString(5, reason)
                    if (expiresAt != null) {
                        ps.setTimestamp(6, java.sql.Timestamp.from(expiresAt))
                    } else {
                        ps.setNull(6, java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                    }
                    ps.executeUpdate()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * PRD Addendum 2 Bagian 57.3: isCapabilityEnabled()
     * Evaluates if capability is enabled based on Tenant Overrides -> Plan Tier Level Matrix.
     */
    fun isCapabilityEnabled(tenantId: String, capabilityKey: String): Boolean {
        // Step 1: Check tenant capability overrides (priority over plan)
        val overrides = tenantOverridesCache[tenantId]
        val override = overrides?.get(capabilityKey)
        if (override != null) {
            val now = Instant.now()
            if (override.expiresAt == null || override.expiresAt.isAfter(now)) {
                return override.enabledOverride
            }
        }

        // DB check for override if cache missed
        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = """
                    SELECT enabled_override, expires_at 
                    FROM tenant_capability_overrides 
                    WHERE tenant_id = ? AND capability_key = ?
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, capabilityKey)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val enabled = rs.getBoolean("enabled_override")
                            val exp = rs.getTimestamp("expires_at")
                            if (exp == null || exp.toInstant().isAfter(Instant.now())) {
                                val map = tenantOverridesCache.computeIfAbsent(tenantId) { ConcurrentHashMap() }
                                map[capabilityKey] = TenantOverride(tenantId, capabilityKey, enabled, "", exp?.toInstant())
                                return enabled
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Step 2: Evaluate Plan Tier Level against Capability Matrix
        val capDef = CAPABILITY_MATRIX[capabilityKey]
        val minTierLevel = capDef?.minTierLevel ?: 1
        val tenantTier = getTenantTier(tenantId)

        return tenantTier.level >= minTierLevel
    }

    /**
     * PRD Addendum 2 Bagian 57.3: enforceCapabilityGate()
     * Throws CapabilityNotAvailableException if capability is not enabled.
     */
    fun enforceCapabilityGate(tenantId: String, capabilityKey: String) {
        if (!isCapabilityEnabled(tenantId, capabilityKey)) {
            val capDef = CAPABILITY_MATRIX[capabilityKey]
            val requiredTier = when (capDef?.minTierLevel ?: 3) {
                1 -> "STARTER"
                2 -> "GROWTH"
                3 -> "ENTERPRISE"
                4 -> "CUSTOM"
                else -> "ENTERPRISE"
            }
            logger.warn("[FEATURE_GATE_DENIED] Tenant '$tenantId' denied access to capability '$capabilityKey'. Required: $requiredTier")
            throw CapabilityNotAvailableException(
                tenantId = tenantId,
                capabilityKey = capabilityKey,
                requiredTier = requiredTier,
                message = "Feature capability '$capabilityKey' is not available for tenant '$tenantId'. Requires tier: $requiredTier"
            )
        }
    }

    /**
     * PRD Addendum 2 Bagian 57.4: Penanganan Risiko Downgrade
     * 1. Auto-suspend Third-Party Integration Fabric (set status = 'SUSPENDED', do NOT delete rows).
     * 2. Auto-pause data sync jobs (status = 'SUSPENDED').
     * 3. AI Chief of Staff stops receiving new events, past briefings remain read-only.
     */
    fun handleTenantDowngrade(tenantId: String, previousTier: String, newTier: String): DowngradeReport {
        val newTierLevel = TierLevel.fromString(newTier)
        setTenantTier(tenantId, newTierLevel)

        var suspendedConnections = 0
        var suspendedJobs = 0

        // If downgraded below ENTERPRISE (level < 3), apply downgrade protections
        val isDowngradeBelowEnterprise = newTierLevel.level < TierLevel.ENTERPRISE.level

        if (isDowngradeBelowEnterprise) {
            logger.warn("[DOWNGRADE_RISK] Tenant '$tenantId' downgraded from $previousTier to $newTier. Executing Bagian 57.4 downgrade isolation.")
            
            // Mark Chief of Staff as read-only (stops accepting new events)
            chiefOfStaffSuspendedTenants.add(tenantId)

            try {
                DatabaseManager.getConnection()?.use { conn ->
                    // 1. Auto-suspend enterprise system connections (PRD: JANGAN HAPUS!)
                    val updateConnSql = """
                        UPDATE enterprise_system_connections 
                        SET status = 'SUSPENDED', 
                            error_reason = 'Tenant subscription downgraded to $newTier. Integration Fabric auto-suspended per PRD Bagian 57.4.',
                            updated_at = CURRENT_TIMESTAMP
                        WHERE tenant_id = ? AND status != 'SUSPENDED'
                    """.trimIndent()
                    conn.prepareStatement(updateConnSql).use { ps ->
                        ps.setString(1, tenantId)
                        suspendedConnections = ps.executeUpdate()
                    }

                    // 2. Auto-pause sync jobs
                    val updateJobsSql = """
                        UPDATE enterprise_data_sync_jobs
                        SET last_run_status = 'SUSPENDED',
                            error_message = 'Sync job auto-suspended due to tier downgrade per PRD Bagian 57.4.'
                        WHERE tenant_id = ? AND last_run_status != 'SUSPENDED'
                    """.trimIndent()
                    conn.prepareStatement(updateJobsSql).use { ps ->
                        ps.setString(1, tenantId)
                        suspendedJobs = ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.error("[DOWNGRADE_RISK] Error during database downgrade suspension for tenant $tenantId: ${e.message}")
            }
        }

        return DowngradeReport(
            tenantId = tenantId,
            previousTier = previousTier,
            newTier = newTier,
            suspendedConnectionsCount = suspendedConnections,
            suspendedJobsCount = suspendedJobs,
            chiefOfStaffReadOnly = isDowngradeBelowEnterprise,
            message = if (isDowngradeBelowEnterprise) {
                "Tenant downgraded to $newTier. $suspendedConnections Integration Fabric connections suspended (preserved for re-activation). Chief of Staff read-only mode activated."
            } else {
                "Tenant tier adjusted to $newTier."
            }
        )
    }

    fun isChiefOfStaffEventAcceptanceAllowed(tenantId: String): Boolean {
        if (chiefOfStaffSuspendedTenants.contains(tenantId)) return false
        return isCapabilityEnabled(tenantId, "ai_chief_of_staff")
    }
}
