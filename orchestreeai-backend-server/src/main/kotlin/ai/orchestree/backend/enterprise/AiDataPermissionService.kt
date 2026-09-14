package ai.orchestree.backend.enterprise

import ai.orchestree.backend.billing.DatabaseManager
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class AiDataPermissionPolicy(
    val id: String,
    val tenantId: String,
    val agentId: String,
    val connectionId: String,
    val accessLevel: String = "READ_ONLY", // 'READ_ONLY', 'ANALYZE', 'RECOMMEND', 'EXECUTE'
    val allowedTablesOrTypes: List<String> = listOf("*"),
    val allowedFields: List<String> = listOf("*"),
    val conditionRulesJson: String = "{}",
    val grantedByUserId: String = "system",
    val grantedAt: Instant = Instant.now()
)

data class PermissionDecision(
    val allowed: Boolean,
    val decision: String, // "ALLOWED" or "DENIED_NO_POLICY" or "DENIED_INSUFFICIENT_ACCESS_LEVEL" or "DENIED_SCOPE"
    val reason: String,
    val policyId: String? = null
)

data class AiDataAccessRequest(
    val id: String,
    val tenantId: String,
    val agentId: String,
    val connectionId: String,
    val requestedAccessLevel: String = "READ_ONLY",
    val requestedScope: List<String> = listOf("*"),
    val businessReason: String,
    var status: String = "PENDING", // 'PENDING', 'APPROVED', 'REJECTED'
    var reviewedByUserId: String? = null,
    var reviewedAt: Instant? = null,
    var rejectionReason: String = "",
    val createdAt: Instant = Instant.now()
)

class AiDataPermissionException(
    val decision: String,
    val reason: String,
    message: String = "[$decision] $reason"
) : RuntimeException(message)

/**
 * Attribute-Based Access Control (ABAC) for AI Data Permissions
 * Implements PRD Addendum 2 Bagian 59: Permission-First Architecture.
 *
 * CRITICAL RULE: DEFAULT-DENY TOTAL.
 * AI Agents possess ZERO access to external systems or ingested enterprise records
 * until an Admin/Owner explicitly configures a policy row in ai_data_permission_policies.
 */
object AiDataPermissionService {
    private val logger = LoggerFactory.getLogger(AiDataPermissionService::class.java)

    // In-memory policy cache: tenantId -> map of "agentId:connectionId" -> AiDataPermissionPolicy
    private val policiesStore = ConcurrentHashMap<String, ConcurrentHashMap<String, AiDataPermissionPolicy>>()
    private val accessRequestsStore = ConcurrentHashMap<String, AiDataAccessRequest>()

    fun grantPolicy(policy: AiDataPermissionPolicy) {
        val tenantMap = policiesStore.computeIfAbsent(policy.tenantId) { ConcurrentHashMap() }
        val key = "${policy.agentId}:${policy.connectionId}"
        tenantMap[key] = policy
        logger.info("[ABAC] Granted AI Data Permission Policy '${policy.id}' to agent '${policy.agentId}' for connection '${policy.connectionId}' in tenant '${policy.tenantId}' (level: ${policy.accessLevel})")

        // Persist to DB
        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = """
                    INSERT INTO ai_data_permission_policies (
                        id, tenant_id, agent_id, connection_id, access_level, allowed_tables_or_types, allowed_fields, condition_rules, granted_by_user_id, granted_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    ON CONFLICT (tenant_id, agent_id, connection_id) DO UPDATE SET
                        access_level = EXCLUDED.access_level,
                        allowed_tables_or_types = EXCLUDED.allowed_tables_or_types,
                        allowed_fields = EXCLUDED.allowed_fields,
                        condition_rules = EXCLUDED.condition_rules,
                        updated_at = CURRENT_TIMESTAMP
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, policy.id)
                    ps.setString(2, policy.tenantId)
                    ps.setString(3, policy.agentId)
                    ps.setString(4, policy.connectionId)
                    ps.setString(5, policy.accessLevel)
                    ps.setString(6, "[${policy.allowedTablesOrTypes.joinToString(",") { "\"$it\"" }}]")
                    ps.setString(7, "[${policy.allowedFields.joinToString(",") { "\"$it\"" }}]")
                    ps.setString(8, policy.conditionRulesJson)
                    ps.setString(9, policy.grantedByUserId)
                    ps.executeUpdate()
                }
            }
        } catch (_: Exception) {}
    }

    fun revokePolicy(tenantId: String, agentId: String, connectionId: String): Boolean {
        val tenantMap = policiesStore[tenantId]
        val removed = tenantMap?.remove("$agentId:$connectionId") != null
        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = "DELETE FROM ai_data_permission_policies WHERE tenant_id = ? AND agent_id = ? AND connection_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, agentId)
                    ps.setString(3, connectionId)
                    ps.executeUpdate()
                }
            }
        } catch (_: Exception) {}
        return removed
    }

    /**
     * PRD Addendum 2 Bagian 59.3: checkAiDataPermission()
     * Default-Deny: returns DENIED_NO_POLICY if no active policy exists.
     */
    fun checkAiDataPermission(
        tenantId: String,
        agentId: String,
        connectionId: String,
        recordType: String = "*",
        requiredAccessLevel: String = "READ_ONLY"
    ): PermissionDecision {
        val key = "$agentId:$connectionId"
        var policy = policiesStore[tenantId]?.get(key)

        // Check wildcard agent policies e.g. "*:connectionId"
        if (policy == null) {
            policy = policiesStore[tenantId]?.get("*:$connectionId")
        }

        // DB lookup if cache missed
        if (policy == null) {
            try {
                DatabaseManager.getConnection()?.use { conn ->
                    val sql = """
                        SELECT id, tenant_id, agent_id, connection_id, access_level, allowed_tables_or_types, allowed_fields, condition_rules, granted_by_user_id
                        FROM ai_data_permission_policies 
                        WHERE tenant_id = ? AND (agent_id = ? OR agent_id = '*') AND (connection_id = ? OR connection_id = '*')
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setString(2, agentId)
                        ps.setString(3, connectionId)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                val loaded = AiDataPermissionPolicy(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id"),
                                    agentId = rs.getString("agent_id"),
                                    connectionId = rs.getString("connection_id"),
                                    accessLevel = rs.getString("access_level") ?: "READ_ONLY",
                                    grantedByUserId = rs.getString("granted_by_user_id") ?: "system"
                                )
                                grantPolicy(loaded)
                                policy = loaded
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // PRD Bagian 59.3 Rule 1: DEFAULT-DENY TOTAL
        if (policy == null) {
            val decision = "DENIED_NO_POLICY"
            val reason = "AI Agent '$agentId' has no granted ABAC policy for connection '$connectionId' and record '$recordType' in tenant '$tenantId'"
            logger.warn("[ABAC_DEFAULT_DENY] $reason")
            recordAccessAttempt(tenantId, agentId, connectionId, requiredAccessLevel, recordType, decision, reason, null)
            return PermissionDecision(allowed = false, decision = decision, reason = reason)
        }

        // PRD Bagian 59.3 Rule 2: Check Scope (tables / record types)
        val isScopeAllowed = policy!!.allowedTablesOrTypes.contains("*") || policy!!.allowedTablesOrTypes.any { it.equals(recordType, ignoreCase = true) }
        if (!isScopeAllowed) {
            val decision = "DENIED_SCOPE"
            val reason = "Record type '$recordType' is not permitted in policy '${policy!!.id}' for AI Agent '$agentId'"
            logger.warn("[ABAC_DENIED] $reason")
            recordAccessAttempt(tenantId, agentId, connectionId, requiredAccessLevel, recordType, decision, reason, policy!!.id)
            return PermissionDecision(allowed = false, decision = decision, reason = reason, policyId = policy!!.id)
        }

        // PRD Bagian 59.3 Rule 3: Check Access Level hierarchy: EXECUTE > RECOMMEND > ANALYZE > READ_ONLY
        val policyRank = accessLevelRank(policy!!.accessLevel)
        val requiredRank = accessLevelRank(requiredAccessLevel)
        if (policyRank < requiredRank) {
            val decision = "DENIED_INSUFFICIENT_ACCESS_LEVEL"
            val reason = "Required access level '$requiredAccessLevel' exceeds policy limit '${policy!!.accessLevel}' for AI Agent '$agentId'"
            logger.warn("[ABAC_DENIED] $reason")
            recordAccessAttempt(tenantId, agentId, connectionId, requiredAccessLevel, recordType, decision, reason, policy!!.id)
            return PermissionDecision(allowed = false, decision = decision, reason = reason, policyId = policy!!.id)
        }

        // Allowed!
        val decision = "ALLOWED"
        val reason = "Access granted per policy '${policy!!.id}'"
        recordAccessAttempt(tenantId, agentId, connectionId, requiredAccessLevel, recordType, decision, reason, policy!!.id)
        return PermissionDecision(allowed = true, decision = decision, reason = reason, policyId = policy!!.id)
    }

    private fun accessLevelRank(level: String): Int {
        return when (level.uppercase()) {
            "READ_ONLY" -> 1
            "ANALYZE" -> 2
            "RECOMMEND" -> 3
            "EXECUTE" -> 4
            else -> 1
        }
    }

    fun enforceAiDataPermission(
        tenantId: String,
        agentId: String,
        connectionId: String,
        recordType: String = "*",
        requiredAccessLevel: String = "READ_ONLY"
    ) {
        val decision = checkAiDataPermission(tenantId, agentId, connectionId, recordType, requiredAccessLevel)
        if (!decision.allowed) {
            throw AiDataPermissionException(decision.decision, decision.reason)
        }
    }

    private fun recordAccessAttempt(
        tenantId: String,
        agentId: String,
        connectionId: String,
        actionType: String,
        requestedResource: String,
        decision: String,
        reason: String,
        policyId: String?
    ) {
        val reqId = "req-${UUID.randomUUID().toString().take(12)}"
        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = """
                    INSERT INTO ai_data_access_requests (
                        id, tenant_id, agent_id, connection_id, requested_access_level, requested_scope, business_reason, status, rejection_reason, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, CURRENT_TIMESTAMP)
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, reqId)
                    ps.setString(2, tenantId)
                    ps.setString(3, agentId)
                    ps.setString(4, connectionId)
                    ps.setString(5, actionType)
                    ps.setString(6, "[\"$requestedResource\"]")
                    ps.setString(7, reason)
                    ps.setString(8, if (decision == "ALLOWED") "APPROVED" else "REJECTED")
                    ps.setString(9, if (decision == "ALLOWED") "" else reason)
                    ps.executeUpdate()
                }
            }
        } catch (_: Exception) {}
    }

    fun listPolicies(tenantId: String): List<AiDataPermissionPolicy> {
        val tenantMap = policiesStore[tenantId]
        return tenantMap?.values?.toList() ?: emptyList()
    }

    /**
     * PRD Addendum 2 Bagian 78.2: RBAC/ABAC Enterprise Workforce Capability Matrix
     */
    enum class EnterpriseCapability {
        VIEW_ACTIVITY_STREAM,
        QUERY_CONTEXT_FABRIC,
        EXECUTE_MANAGEMENT_QUERY,
        VIEW_DAILY_REPORTS,
        MANAGE_KNOWLEDGE_RULES,
        DISPATCH_AI_EVENTS,
        ACCESS_CHIEF_OF_STAFF_BRIEFINGS,
        CREATE_RESEARCH_DIRECTIVES,
        VIEW_AGENT_SKILL_CONFIDENCE,
        VIEW_DATA_QUALITY_ISSUES,
        EVALUATE_PROJECT_HEALTH,
        INITIATE_MULTI_AGENT_COLLABORATION,
        VIEW_ROLE_BASED_EXPLAINABILITY
    }

    fun hasCapability(role: String, capability: EnterpriseCapability): Boolean {
        val r = role.uppercase()
        return when (capability) {
            EnterpriseCapability.VIEW_ACTIVITY_STREAM -> true
            EnterpriseCapability.QUERY_CONTEXT_FABRIC -> true
            EnterpriseCapability.EXECUTE_MANAGEMENT_QUERY -> true
            EnterpriseCapability.VIEW_DAILY_REPORTS -> true
            EnterpriseCapability.MANAGE_KNOWLEDGE_RULES -> r in listOf("TENANT_ADMIN", "ADMIN", "OWNER", "EXECUTIVE")
            EnterpriseCapability.DISPATCH_AI_EVENTS -> true
            EnterpriseCapability.ACCESS_CHIEF_OF_STAFF_BRIEFINGS -> r in listOf("TENANT_ADMIN", "ADMIN", "OWNER", "EXECUTIVE", "MANAGER")
            EnterpriseCapability.CREATE_RESEARCH_DIRECTIVES -> r in listOf("TENANT_ADMIN", "ADMIN", "OWNER", "EXECUTIVE", "MANAGER")
            EnterpriseCapability.VIEW_AGENT_SKILL_CONFIDENCE -> true
            EnterpriseCapability.VIEW_DATA_QUALITY_ISSUES -> true
            EnterpriseCapability.EVALUATE_PROJECT_HEALTH -> true
            EnterpriseCapability.INITIATE_MULTI_AGENT_COLLABORATION -> true
            EnterpriseCapability.VIEW_ROLE_BASED_EXPLAINABILITY -> true
        }
    }

}
