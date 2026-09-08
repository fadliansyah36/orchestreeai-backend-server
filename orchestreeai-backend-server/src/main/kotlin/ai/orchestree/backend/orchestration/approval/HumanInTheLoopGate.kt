package ai.orchestree.backend.orchestration.approval

import ai.orchestree.backend.database.repositories.orchestration.PendingApprovalRepository
import ai.orchestree.backend.orchestration.NodeResult
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.orchestration.WorkflowContext
import ai.orchestree.backend.orchestration.WorkflowExecution
import ai.orchestree.backend.orchestration.WorkflowExecutionResult
import ai.orchestree.backend.orchestration.toJson
import ai.orchestree.backend.security.AuditLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

enum class ApprovalDecision {
    APPROVED,
    REJECTED
}

@Serializable
data class PendingApproval(
    val id: String = "appr-${UUID.randomUUID().toString().take(8)}",
    val executionId: String,
    val nodeId: String,
    val fullContextSnapshot: JsonElement, // state LENGKAP, bukan ringkasan
    val requestedAt: String = Instant.now().toString(),
    val expiresAt: String? = null,
    val approverRoleRequired: String,
    val reason: String = "",
    val tenantId: String = "tenant-default",
    var status: String = "PENDING",
    var approvedBy: String? = null,
    var decisionAt: String? = null
) {
    constructor(
        executionId: String,
        nodeId: String,
        fullContextSnapshot: JsonElement,
        requestedAt: Instant,
        expiresAt: Instant?,
        approverRoleRequired: String,
        reason: String = "",
        tenantId: String = "tenant-default",
        status: String = "PENDING",
        approvedBy: String? = null,
        decisionAt: String? = null,
        id: String = "appr-${UUID.randomUUID().toString().take(8)}"
    ) : this(
        id = id,
        executionId = executionId,
        nodeId = nodeId,
        fullContextSnapshot = fullContextSnapshot,
        requestedAt = requestedAt.toString(),
        expiresAt = expiresAt?.toString(),
        approverRoleRequired = approverRoleRequired,
        reason = reason,
        tenantId = tenantId,
        status = status,
        approvedBy = approvedBy,
        decisionAt = decisionAt
    )

    val requestedInstant: Instant
        get() = try { Instant.parse(requestedAt) } catch (e: Exception) { Instant.now() }

    val expiresInstant: Instant?
        get() = expiresAt?.let { try { Instant.parse(it) } catch (e: Exception) { null } }
}

class HumanInTheLoopGate(
    val pendingApprovalRepo: PendingApprovalRepository = PendingApprovalRepository(),
    val notificationService: ApprovalNotificationService = ApprovalNotificationService(),
    val auditLog: AuditLogger = AuditLogger(),
    private val orchestrationEngineProvider: () -> OrchestrationEngine
) {
    private val logger = LoggerFactory.getLogger(HumanInTheLoopGate::class.java)

    companion object {
        @Volatile
        private var defaultInstance: HumanInTheLoopGate? = null

        fun init(gate: HumanInTheLoopGate): HumanInTheLoopGate {
            defaultInstance = gate
            return gate
        }

        fun getInstance(): HumanInTheLoopGate {
            return defaultInstance ?: error("HumanInTheLoopGate has not been initialized.")
        }

        fun isInitialized(): Boolean = defaultInstance != null
    }

    fun determineApproverRole(reason: String): String {
        val lower = reason.lowercase()
        return when {
            "diskon" in lower || "discount" in lower || "refund" in lower || "financial" in lower || "biaya" in lower -> "FINANCE_ADMIN"
            "security" in lower || "pentest" in lower || "risiko" in lower || "risk" in lower || "enterprise" in lower -> "SUPER_ADMIN"
            "manager" in lower || "pimpinan" in lower || "hr" in lower -> "DEPT_MANAGER"
            else -> "TENANT_ADMIN"
        }
    }

    suspend fun interrupt(
        execution: WorkflowExecution,
        reason: String,
        expiresAt: Instant? = null
    ): NodeResult {
        val targetNodeId = execution.currentNodeId
            ?: execution.lastCompletedNodeId
            ?: "approval-node"

        val contextJson = Json.parseToJsonElement(execution.context.toJson())
        val role = determineApproverRole(reason)

        val pending = pendingApprovalRepo.create(
            executionId = execution.id,
            nodeId = targetNodeId,
            fullContextSnapshot = contextJson, // WAJIB LENGKAP
            requestedAt = Instant.now(),
            expiresAt = expiresAt,
            approverRoleRequired = role,
            reason = reason,
            tenantId = execution.tenantId
        )

        try {
            orchestrationEngineProvider().workflowExecutionRepo.saveCheckpoint(
                executionId = execution.id,
                currentStateSnapshot = execution.context.toJson(),
                lastCompletedNodeId = targetNodeId,
                status = "paused_for_approval"
            )
        } catch (_: Exception) {
            // Best effort checkpoint
        }

        notificationService.notifyApprover(pending)
        logger.info("[HITL] Workflow execution ${execution.id} paused at node $targetNodeId. PendingApproval: ${pending.id} ($role)")
        return NodeResult.Paused(pending.id, reason)
    }

    suspend fun resume(
        pendingApprovalId: String,
        decision: ApprovalDecision,
        approvedBy: String
    ): WorkflowExecutionResult {
        val pending = pendingApprovalRepo.get(pendingApprovalId)
            ?: throw IllegalArgumentException("Pending approval not found: $pendingApprovalId")

        // REKONSTRUKSI CONTEXT PERSIS SEPERTI SAAT DIJEDA, TIDAK PEDULI
        // BERAPA LAMA WAKTU BERLALU (3 detik atau 3 hari, HASIL SAMA)
        val restoredContext = WorkflowContext.fromJson(pending.fullContextSnapshot)
        restoredContext["approval_decision"] = decision.name
        restoredContext["approved_by"] = approvedBy
        restoredContext["approved_at"] = Instant.now().toString()

        val orchestrationEngine = orchestrationEngineProvider()
        return if (decision == ApprovalDecision.APPROVED) {
            pendingApprovalRepo.updateStatus(pendingApprovalId, "APPROVED", approvedBy)
            auditLog.record("human_approval_granted", pendingApprovalId, approvedBy, pending.tenantId)
            orchestrationEngine.continueFrom(pending.executionId, restoredContext)
        } else {
            pendingApprovalRepo.updateStatus(pendingApprovalId, "REJECTED", approvedBy)
            auditLog.record("human_approval_rejected", pendingApprovalId, approvedBy, pending.tenantId)
            orchestrationEngine.abort(pending.executionId, reason = "Ditolak oleh $approvedBy")
        }
    }
}
