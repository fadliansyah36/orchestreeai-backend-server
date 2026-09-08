package ai.orchestree.backend.security

import org.slf4j.LoggerFactory
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

class AuditLogger {
    private val logger = LoggerFactory.getLogger(AuditLogger::class.java)
    val inMemoryLogs = CopyOnWriteArrayList<AuditEntry>()

    fun log(tenantId: String, actor: String, action: String, details: String = "", status: String = "SUCCESS") {
        logger.info("[AUDIT] tenant=$tenantId actor=$actor action=$action details=$details status=$status")
        inMemoryLogs.add(AuditEntry(tenantId = tenantId, actor = actor, action = action, details = details, status = status))
    }

    fun record(action: String, pendingApprovalId: String, approvedBy: String, tenantId: String = "tenant-default") {
        logger.info("[AUDIT] action=$action pendingApprovalId=$pendingApprovalId approvedBy=$approvedBy tenant=$tenantId")
        inMemoryLogs.add(AuditEntry(tenantId = tenantId, actor = approvedBy, action = action, details = "PendingApproval: $pendingApprovalId", status = "SUCCESS"))
    }

    fun record(action: String, entityId: String) {
        logger.info("[AUDIT] action=$action entityId=$entityId")
        inMemoryLogs.add(AuditEntry(tenantId = "system", actor = "system", action = action, details = "Entity: $entityId", status = "SUCCESS"))
    }
}

