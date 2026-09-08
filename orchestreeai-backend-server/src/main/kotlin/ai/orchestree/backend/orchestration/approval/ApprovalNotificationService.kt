package ai.orchestree.backend.orchestration.approval

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.orchestration.toJson
import org.slf4j.LoggerFactory

class ApprovalNotificationService(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(ApprovalNotificationService::class.java)

    suspend fun notifyApprover(pending: PendingApproval) {
        logger.info("[NOTIFICATION] Pending approval requested: id=${pending.id}, execution=${pending.executionId}, node=${pending.nodeId}, roleRequired=${pending.approverRoleRequired}, reason=${pending.reason}")

        if (supabase.isConfigured()) {
            try {
                val payload = mapOf(
                    "tenant_id" to pending.tenantId,
                    "title" to "Persetujuan Diperlukan: ${pending.reason.ifBlank { pending.nodeId }}",
                    "content" to "Workflow execution ${pending.executionId} membutuhkan otorisasi role ${pending.approverRoleRequired}",
                    "category" to "TASK",
                    "priority" to "HIGH",
                    "target_role" to pending.approverRoleRequired,
                    "action_url" to "/approvals/${pending.id}"
                )
                supabase.insertRecord("app_notifications", pending.tenantId, payload.toJson())
            } catch (e: Exception) {
                logger.warn("Failed sending notification to Supabase: ${e.message}")
            }
        }
    }
}
