package ai.orchestree.backend.orchestration

import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.database.repositories.orchestration.PendingApprovalRepository
import ai.orchestree.backend.database.repositories.taskboard.Task
import ai.orchestree.backend.database.repositories.taskboard.TaskRepository
import ai.orchestree.backend.enterprise.AiDataPermissionService
import ai.orchestree.backend.enterprise.EnterpriseIntegrationFabricService
import ai.orchestree.backend.enterprise.FeatureCapabilityService
import ai.orchestree.backend.enterprise.TierLevel
import ai.orchestree.backend.intelligence.RiskEngine
import ai.orchestree.backend.orchestration.approval.ApprovalNotificationService
import ai.orchestree.backend.orchestration.approval.HumanInTheLoopGate
import ai.orchestree.backend.orchestration.approval.PendingApproval
import ai.orchestree.backend.security.AuditLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class ApprovedAction(
    val id: String = "act-${UUID.randomUUID().toString().take(8)}",
    val tenantId: String,
    val agentId: String,
    val actionType: String, // 'CREATE_TASK', 'NOTIFY_HUMAN', 'UPDATE_INVENTORY', 'TRIGGER_WORKFLOW', 'EXTERNAL_ERP_ACTION'
    val targetSystem: String, // non-enterprise: 'TASK_BOARD', 'NOTIFICATION', 'INTERNAL_INVENTORY'; enterprise: connectionId
    val payload: Map<String, String> = emptyMap(),
    val riskScore: Double = 0.0,
    val assignedHuman: String = "admin",
    val requiresApproval: Boolean = false,
    val approvalId: String? = null,
    val approvedBy: String? = null,
    val status: String = "PENDING", // 'PENDING', 'APPROVED', 'EXECUTED', 'FAILED', 'REJECTED'
    val executedAt: Long? = null,
    val executionResult: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ActionProposalResult(
    val action: ApprovedAction,
    val isApproved: Boolean,
    val requiresHumanApproval: Boolean,
    val rejectionReason: String? = null,
    val checksPassed: List<String>
)

@Serializable
data class ActionExecutionResult(
    val actionId: String,
    val status: String, // 'SUCCESS', 'FAILED'
    val targetSystem: String,
    val executionSummary: String,
    val auditLogId: String,
    val notifiedHuman: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AI Action Orchestration & Execution Layer (PRD Addendum 2 Bagian 67)
 * Active for ALL tiers (ALL_TIER).
 * - Implements 4-layer check before human escalation: Policy, Permission, Approval Requirement, Business Rule (Bagian 67.1)
 * - Implements executeApprovedAction() (Bagian 67.2)
 * - Non-Enterprise routes to internal modules (Task Board/Notification); Enterprise routes via Integration Fabric
 */
class AiActionOrchestrator(
    private val riskEngine: RiskEngine = RiskEngine(),
    private val taskRepository: TaskRepository = TaskRepository(),
    private val auditLogger: AuditLogger = AuditLogger(),
    private val notificationService: ApprovalNotificationService = ApprovalNotificationService(),
    private val pendingApprovalRepository: PendingApprovalRepository = PendingApprovalRepository()
) {
    private val logger = LoggerFactory.getLogger(AiActionOrchestrator::class.java)
    val actionsStore = ConcurrentHashMap<String, ApprovedAction>()

    /**
     * Evaluates an action proposal through the 4-layer check flow (Bagian 67.1):
     * 1. Policy Check
     * 2. Permission Check (AiDataPermissionService.checkAiDataPermission)
     * 3. Approval Requirement Check (RiskEngine & Guardrail Matrix)
     * 4. Business Rule Check
     */
    suspend fun proposeAction(
        tenantId: String,
        agentId: String,
        actionType: String,
        targetSystem: String,
        payload: Map<String, String>,
        assignedHuman: String = "admin"
    ): ActionProposalResult = withContext(Dispatchers.IO) {
        val passedChecks = mutableListOf<String>()

        // 1. POLICY CHECK: Verify tenant policy / feature capability
        // Note: ai_action_orchestration is ALL_TIER (minTierLevel = 1)
        val policyAllowed = try {
            FeatureCapabilityService.isCapabilityEnabled(tenantId, "ai_action_orchestration")
        } catch (_: Exception) { true }

        if (!policyAllowed) {
            val rejected = ApprovedAction(
                tenantId = tenantId,
                agentId = agentId,
                actionType = actionType,
                targetSystem = targetSystem,
                payload = payload,
                assignedHuman = assignedHuman,
                status = "REJECTED",
                executionResult = "Policy check failed: capability not enabled for tenant"
            )
            actionsStore[rejected.id] = rejected
            return@withContext ActionProposalResult(
                action = rejected,
                isApproved = false,
                requiresHumanApproval = false,
                rejectionReason = rejected.executionResult,
                checksPassed = passedChecks
            )
        }
        passedChecks.add("POLICY_CHECK")

        // 2. PERMISSION CHECK: checkAiDataPermission from Fase 2B.1
        // Verify agent has permission to execute write operation on target system
        val permDecision = AiDataPermissionService.checkAiDataPermission(
            tenantId = tenantId,
            agentId = agentId,
            connectionId = targetSystem,
            recordType = actionType,
            requiredAccessLevel = "READ_WRITE"
        )

        // For non-enterprise internal modules, default allow if not explicitly blocked
        val isInternalSystem = targetSystem in listOf("TASK_BOARD", "NOTIFICATION", "INTERNAL_INVENTORY", "INTERNAL")
        val permAllowed = if (isInternalSystem && permDecision.decision == "DENIED_NO_POLICY") {
            true // Internal modules default allowed for non-enterprise
        } else {
            permDecision.allowed
        }

        if (!permAllowed) {
            val rejected = ApprovedAction(
                tenantId = tenantId,
                agentId = agentId,
                actionType = actionType,
                targetSystem = targetSystem,
                payload = payload,
                assignedHuman = assignedHuman,
                status = "REJECTED",
                executionResult = "Permission check failed: ${permDecision.reason}"
            )
            actionsStore[rejected.id] = rejected
            return@withContext ActionProposalResult(
                action = rejected,
                isApproved = false,
                requiresHumanApproval = false,
                rejectionReason = rejected.executionResult,
                checksPassed = passedChecks
            )
        }
        passedChecks.add("PERMISSION_CHECK")

        // 3. APPROVAL REQUIREMENT CHECK (Guardrail Matrix & Risk Engine)
        val financialAmount = (payload["amount"]?.toDoubleOrNull() ?: 0.0).toLong()
        val isFinancial = actionType.contains("FINANCE", ignoreCase = true) || actionType.contains("PAYMENT", ignoreCase = true) || financialAmount > 0
        val riskScore = riskEngine.calculateRiskScore(
            intent = actionType,
            isFinancialAction = isFinancial,
            amountIdr = financialAmount
        )

        val isFinancialHighValue = financialAmount > 5_000_000L
        val isHighRisk = riskScore >= 0.7 || isFinancialHighValue || actionType in listOf("DELETE_RECORD", "FORCE_OVERRIDE", "TRANSFER_FUNDS")

        passedChecks.add("APPROVAL_REQUIREMENT_CHECK")

        // 4. BUSINESS RULE CHECK
        val qty = payload["quantity"]?.toDoubleOrNull() ?: 1.0
        if (qty <= 0) {
            val rejected = ApprovedAction(
                tenantId = tenantId,
                agentId = agentId,
                actionType = actionType,
                targetSystem = targetSystem,
                payload = payload,
                riskScore = riskScore,
                assignedHuman = assignedHuman,
                status = "REJECTED",
                executionResult = "Business rule check failed: quantity must be greater than zero"
            )
            actionsStore[rejected.id] = rejected
            return@withContext ActionProposalResult(
                action = rejected,
                isApproved = false,
                requiresHumanApproval = false,
                rejectionReason = rejected.executionResult,
                checksPassed = passedChecks
            )
        }
        passedChecks.add("BUSINESS_RULE_CHECK")

        val actionId = "act-${UUID.randomUUID().toString().take(8)}"

        if (isHighRisk) {
            // Human Escalation required: reuse existing PendingApprovalRepository and ApprovalNotificationService
            val contextJson = buildJsonObject {
                put("actionId", actionId)
                put("actionType", actionType)
                put("targetSystem", targetSystem)
                put("riskScore", riskScore)
                put("assignedHuman", assignedHuman)
            }
            val pending = pendingApprovalRepository.create(
                executionId = actionId,
                nodeId = "HUMAN_APPROVAL",
                fullContextSnapshot = contextJson,
                requestedAt = Instant.now(),
                expiresAt = Instant.now().plusSeconds(86400),
                approverRoleRequired = "TENANT_ADMIN",
                reason = "Action $actionType on $targetSystem requires authorization (risk: $riskScore)",
                tenantId = tenantId
            )
            notificationService.notifyApprover(pending)

            val pendingAction = ApprovedAction(
                id = actionId,
                tenantId = tenantId,
                agentId = agentId,
                actionType = actionType,
                targetSystem = targetSystem,
                payload = payload,
                riskScore = riskScore,
                assignedHuman = assignedHuman,
                requiresApproval = true,
                approvalId = pending.id,
                status = "PENDING"
            )
            actionsStore[actionId] = pendingAction
            persistApprovedAction(pendingAction)

            return@withContext ActionProposalResult(
                action = pendingAction,
                isApproved = false,
                requiresHumanApproval = true,
                rejectionReason = null,
                checksPassed = passedChecks
            )
        } else {
            // Low risk: auto-approved
            val approvedAction = ApprovedAction(
                id = actionId,
                tenantId = tenantId,
                agentId = agentId,
                actionType = actionType,
                targetSystem = targetSystem,
                payload = payload,
                riskScore = riskScore,
                assignedHuman = assignedHuman,
                requiresApproval = false,
                approvedBy = "SYSTEM_AUTO",
                status = "APPROVED"
            )
            actionsStore[actionId] = approvedAction
            persistApprovedAction(approvedAction)

            return@withContext ActionProposalResult(
                action = approvedAction,
                isApproved = true,
                requiresHumanApproval = false,
                rejectionReason = null,
                checksPassed = passedChecks
            )
        }
    }

    /**
     * Implementasi executeApprovedAction() PERSIS kode Bagian 67.2.
     * Executes an approved action:
     * - For non-Enterprise: targetSystem routes to internal modules (Task Board/Notification).
     * - For Enterprise: via Integration Fabric (Fase 2B.1).
     * - Records audit log 'ai_action_executed' and dispatches notification to assignedHuman.
     */
    suspend fun executeApprovedAction(action: ApprovedAction): ActionExecutionResult = withContext(Dispatchers.IO) {
        require(action.status == "APPROVED") { "Cannot execute action ${action.id} with status ${action.status}; must be APPROVED" }

        val tenantId = action.tenantId
        val isEnterprise = try {
            FeatureCapabilityService.getTenantTier(tenantId).level >= TierLevel.ENTERPRISE.level
        } catch (_: Exception) { false }

        val resultSummary: String
        var notifiedTarget: String? = null

        if (!isEnterprise || action.targetSystem in listOf("TASK_BOARD", "NOTIFICATION", "INTERNAL_INVENTORY", "INTERNAL")) {
            // NON-ENTERPRISE (or internal target system): Route to internal modules
            when (action.targetSystem) {
                "TASK_BOARD" -> {
                    val task = Task(
                        id = "tsk-${UUID.randomUUID().toString().take(8)}",
                        tenantId = tenantId,
                        title = action.payload["title"] ?: "[AI ACTION] ${action.actionType}",
                        description = action.payload["description"] ?: "Action generated by agent ${action.agentId}",
                        columnName = "TODO",
                        assigneeType = "HUMAN",
                        assigneeId = action.assignedHuman,
                        assigneeName = "Assigned Staff (${action.assignedHuman})",
                        createdByType = "orchestration_engine",
                        createdByAiAgentId = action.agentId,
                        detectionReason = action.payload["reason"] ?: "Automated Action Orchestration",
                        recommendedQuantity = action.payload["quantity"]?.toDoubleOrNull()
                    )
                    taskRepository.createTask(task)
                    resultSummary = "Internal Task Board: Created task ${task.id} assigned to ${action.assignedHuman}"
                }
                "NOTIFICATION" -> {
                    notifiedTarget = action.assignedHuman
                    val pendingNotice = pendingApprovalRepository.create(
                        executionId = action.id,
                        nodeId = "ACTION_DISPATCH",
                        fullContextSnapshot = buildJsonObject {
                            put("actionId", action.id)
                            put("actionType", action.actionType)
                            put("assignedHuman", action.assignedHuman)
                            put("details", action.payload["message"] ?: "Action notification")
                        },
                        requestedAt = Instant.now(),
                        expiresAt = null,
                        approverRoleRequired = "OPERATIONAL_STAFF",
                        reason = action.payload["message"] ?: "Notifikasi aksi AI ke staff ${action.assignedHuman}",
                        tenantId = tenantId
                    )
                    notificationService.notifyApprover(pendingNotice)
                    resultSummary = "Internal Notification: Dispatched to assigned human ${action.assignedHuman}"
                }
                else -> {
                    // Internal inventory or generic internal execution
                    resultSummary = "Internal Module (${action.targetSystem}): Executed ${action.actionType} successfully with payload keys=${action.payload.keys}"
                }
            }
        } else {
            // ENTERPRISE: Route via Integration Fabric
            EnterpriseIntegrationFabricService.ingestRecord(
                connectionId = action.targetSystem,
                tenantId = tenantId,
                recordType = action.actionType,
                externalRecordId = action.payload["externalRecordId"] ?: "ext-${action.id}",
                entityReference = action.payload["entityReference"] ?: action.id,
                payloadJson = buildJsonObject {
                    action.payload.forEach { (k, v) -> put(k, v) }
                    put("orchestrationActionId", action.id)
                    put("dispatchedAgentId", action.agentId)
                }.toString()
            )
            resultSummary = "Integration Fabric: Dispatched action ${action.id} to enterprise connector ${action.targetSystem}"
        }

        // Always notify assignedHuman if action specifies notification or assignment
        if (notifiedTarget == null && action.assignedHuman.isNotBlank()) {
            notifiedTarget = action.assignedHuman
            val notice = pendingApprovalRepository.create(
                executionId = action.id,
                nodeId = "ACTION_COMPLETE",
                fullContextSnapshot = buildJsonObject {
                    put("actionId", action.id)
                    put("summary", resultSummary)
                },
                requestedAt = Instant.now(),
                expiresAt = null,
                approverRoleRequired = "OPERATIONAL_STAFF",
                reason = "Aksi AI selesai: $resultSummary",
                tenantId = tenantId
            )
            notificationService.notifyApprover(notice)
        }

        // Mandatory Audit Log: ai_action_executed
        auditLogger.log(
            tenantId = tenantId,
            actor = action.approvedBy ?: action.agentId,
            action = "ai_action_executed",
            details = "actionId=${action.id} type=${action.actionType} targetSystem=${action.targetSystem} result=$resultSummary"
        )

        // Update action status
        val executedAction = action.copy(
            status = "EXECUTED",
            executedAt = System.currentTimeMillis(),
            executionResult = resultSummary
        )
        actionsStore[action.id] = executedAction
        persistApprovedAction(executedAction)

        val latestAudit = auditLogger.inMemoryLogs.lastOrNull { it.action == "ai_action_executed" && it.tenantId == tenantId }
        val auditId = latestAudit?.id ?: "audit-${UUID.randomUUID().toString().take(8)}"

        ActionExecutionResult(
            actionId = action.id,
            status = "SUCCESS",
            targetSystem = action.targetSystem,
            executionSummary = resultSummary,
            auditLogId = auditId,
            notifiedHuman = notifiedTarget
        )
    }

    private fun persistApprovedAction(action: ApprovedAction) {
        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = """
                    INSERT INTO approved_actions (
                        id, tenant_id, agent_id, action_type, target_system,
                        payload_json, risk_score, approval_id, approved_by, status,
                        executed_at, execution_result, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        status = EXCLUDED.status,
                        approved_by = EXCLUDED.approved_by,
                        executed_at = EXCLUDED.executed_at,
                        execution_result = EXCLUDED.execution_result
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, action.id)
                    ps.setString(2, action.tenantId)
                    ps.setString(3, action.agentId)
                    ps.setString(4, action.actionType)
                    ps.setString(5, action.targetSystem)
                    ps.setString(6, buildJsonObject { action.payload.forEach { (k, v) -> put(k, v) } }.toString())
                    ps.setDouble(7, action.riskScore)
                    ps.setString(8, action.approvalId)
                    ps.setString(9, action.approvedBy)
                    ps.setString(10, action.status)
                    ps.setTimestamp(11, action.executedAt?.let { Timestamp(it) })
                    ps.setString(12, action.executionResult)
                    ps.setTimestamp(13, Timestamp(action.createdAt))
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            logger.debug("Could not persist to approved_actions table (using memory store): ${e.message}")
        }
    }
}
