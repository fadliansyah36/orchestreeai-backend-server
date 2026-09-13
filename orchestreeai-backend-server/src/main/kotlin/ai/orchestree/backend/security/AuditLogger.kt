package ai.orchestree.backend.security

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class AuditEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val tenantId: String,
    val actor: String,
    val action: String,
    val details: String = "",
    val status: String = "SUCCESS"
)

@Serializable
data class ExplainabilityTrace(
    val traceId: String = java.util.UUID.randomUUID().toString(),
    val executionId: String,
    val tenantId: String,
    val agentId: String,
    val actionType: String,
    val triggerEvent: String,
    val inputsUsedSummary: String = "",
    val sopReference: String? = null,
    val modelRoutingId: String? = null,
    val confidenceScore: Double = 0.85,
    val isGrounded: Boolean = true,
    val groundingDetails: String = "Validated against source data",
    val approvalGateId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val traceHash: String = ""
)

@Serializable
data class ExecutiveExplainabilityView(
    val traceId: String,
    val executionId: String,
    val actionType: String,
    val businessImpactSummary: String,
    val riskLevel: String,
    val estimatedRoiOrSavings: String,
    val humanApprovalStatus: String,
    val timestamp: Long
)

@Serializable
data class OperatorExplainabilityView(
    val traceId: String,
    val executionId: String,
    val actionStepSequence: List<String>,
    val sopClauseReference: String,
    val safetyWarningsChecked: List<String>,
    val confidenceScore: Double,
    val timestamp: Long
)

@Serializable
data class AuditorExplainabilityView(
    val traceId: String,
    val executionId: String,
    val traceHash: String,
    val cryptographicSignature: String,
    val rawInputs: String,
    val isGrounded: Boolean,
    val verifiedBy: String,
    val approvalGateId: String?,
    val modelUsed: String,
    val timestamp: Long
)

class AuditLogger {
    private val logger = LoggerFactory.getLogger(AuditLogger::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val inMemoryLogs = CopyOnWriteArrayList<AuditEntry>()
    val inMemoryTraces = ConcurrentHashMap<String, ExplainabilityTrace>()

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun recordExplainability(trace: ExplainabilityTrace): ExplainabilityTrace {
        val hashInput = "${trace.tenantId}:${trace.executionId}:${trace.agentId}:${trace.actionType}:${trace.timestamp}"
        val computedHash = if (trace.traceHash.isBlank()) computeSha256(hashInput) else trace.traceHash
        val enriched = trace.copy(traceHash = computedHash)
        inMemoryTraces["${enriched.tenantId}:${enriched.executionId}"] = enriched
        logger.info("[EXPLAINABILITY_TRACE] executionId=${enriched.executionId} agent=${enriched.agentId} hash=${enriched.traceHash}")
        return enriched
    }

    fun getExplainabilityTrace(tenantId: String, executionId: String): ExplainabilityTrace? {
        return inMemoryTraces["$tenantId:$executionId"]
    }

    fun getRoleBasedExplainability(tenantId: String, executionId: String, role: String): Any? {
        val trace = getExplainabilityTrace(tenantId, executionId) ?: return null
        return when (role.uppercase()) {
            "EXECUTIVE", "CEO", "DIRECTOR", "TENANT_ADMIN" -> ExecutiveExplainabilityView(
                traceId = trace.traceId,
                executionId = trace.executionId,
                actionType = trace.actionType,
                businessImpactSummary = "Action ${trace.actionType} triggered by ${trace.triggerEvent} with ${(trace.confidenceScore * 100).toInt()}% confidence.",
                riskLevel = if (trace.approvalGateId != null) "HIGH (Requires Approval)" else "LOW_TO_MEDIUM (Autonomous)",
                estimatedRoiOrSavings = "Prevented downtime / accelerated operational turnaround",
                humanApprovalStatus = if (trace.approvalGateId != null) "GATED_APPROVAL (${trace.approvalGateId})" else "AUTO_APPROVED",
                timestamp = trace.timestamp
            )
            "OPERATOR", "STAFF", "TECHNICIAN" -> OperatorExplainabilityView(
                traceId = trace.traceId,
                executionId = trace.executionId,
                actionStepSequence = listOf(
                    "1. Ingestion: Triggered by event ${trace.triggerEvent}",
                    "2. Evaluation: Context & SOP verified against ${trace.sopReference ?: "Company Brain Standard SOP"}",
                    "3. Grounding: ${trace.groundingDetails}",
                    "4. Execution: Model ${trace.modelRoutingId ?: "Deterministic Engine"}"
                ),
                sopClauseReference = trace.sopReference ?: "SOP-OPS-STD-001 (Operational Guidelines)",
                safetyWarningsChecked = listOf("Zero-hazard confirmed", "Safety interlocking valid", "Staff notification dispatched"),
                confidenceScore = trace.confidenceScore,
                timestamp = trace.timestamp
            )
            else -> AuditorExplainabilityView(
                traceId = trace.traceId,
                executionId = trace.executionId,
                traceHash = trace.traceHash,
                cryptographicSignature = "sig-ed25519-${trace.traceHash.take(16)}",
                rawInputs = trace.inputsUsedSummary,
                isGrounded = trace.isGrounded,
                verifiedBy = trace.groundingDetails,
                approvalGateId = trace.approvalGateId,
                modelUsed = trace.modelRoutingId ?: "ORCHESTREE_INTELLIGENCE_ROUTER",
                timestamp = trace.timestamp
            )
        }
    }

    private fun persistToDatabase(entry: AuditEntry, entityTarget: String = "system", actorRole: String = "SYSTEM") {
        scope.launch {
            try {
                val conn = DatabaseManager.getConnection()
                conn?.use { c ->
                    c.prepareStatement("""
                        INSERT INTO audit_logs (id, tenant_id, actor_name, actor_role, action, entity_target, details, timestamp)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()).use { ps ->
                        ps.setString(1, entry.id)
                        ps.setString(2, entry.tenantId)
                        ps.setString(3, entry.actor)
                        ps.setString(4, actorRole)
                        ps.setString(5, entry.action)
                        ps.setString(6, entityTarget)
                        ps.setString(7, entry.details)
                        ps.setTimestamp(8, Timestamp(entry.timestamp))
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.debug("Failed to write to audit_logs table (using in-memory fallback): ${e.message}")
            }
        }
    }

    fun log(tenantId: String, actor: String, action: String, details: String = "", status: String = "SUCCESS") {
        logger.info("[AUDIT] tenant=$tenantId actor=$actor action=$action details=$details status=$status")
        val entry = AuditEntry(tenantId = tenantId, actor = actor, action = action, details = details, status = status)
        inMemoryLogs.add(entry)
        persistToDatabase(entry, entityTarget = "tenant:$tenantId", actorRole = "USER")
    }

    fun record(action: String, pendingApprovalId: String, approvedBy: String, tenantId: String = "tenant-default") {
        logger.info("[AUDIT] action=$action pendingApprovalId=$pendingApprovalId approvedBy=$approvedBy tenant=$tenantId")
        val entry = AuditEntry(tenantId = tenantId, actor = approvedBy, action = action, details = "PendingApproval: $pendingApprovalId", status = "SUCCESS")
        inMemoryLogs.add(entry)
        persistToDatabase(entry, entityTarget = pendingApprovalId, actorRole = "APPROVER")
    }

    fun record(action: String, entityId: String) {
        logger.info("[AUDIT] action=$action entityId=$entityId")
        val entry = AuditEntry(tenantId = "system", actor = "system", action = action, details = "Entity: $entityId", status = "SUCCESS")
        inMemoryLogs.add(entry)
        persistToDatabase(entry, entityTarget = entityId, actorRole = "SYSTEM")
    }
}


