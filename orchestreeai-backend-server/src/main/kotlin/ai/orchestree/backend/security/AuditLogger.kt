package ai.orchestree.backend.security

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.sql.Timestamp
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val inMemoryLogs = CopyOnWriteArrayList<AuditEntry>()

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


