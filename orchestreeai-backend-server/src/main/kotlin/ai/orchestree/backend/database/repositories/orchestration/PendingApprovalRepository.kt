package ai.orchestree.backend.database.repositories.orchestration

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.orchestration.approval.PendingApproval
import ai.orchestree.backend.orchestration.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class PendingApprovalRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(PendingApprovalRepository::class.java)
    private val inMemoryStore = ConcurrentHashMap<String, PendingApproval>()

    suspend fun create(
        executionId: String,
        nodeId: String,
        fullContextSnapshot: JsonElement,
        requestedAt: Instant = Instant.now(),
        expiresAt: Instant? = null,
        approverRoleRequired: String = "TENANT_ADMIN",
        reason: String = "",
        tenantId: String = "tenant-default"
    ): PendingApproval = withContext(Dispatchers.IO) {
        val pending = PendingApproval(
            executionId = executionId,
            nodeId = nodeId,
            fullContextSnapshot = fullContextSnapshot,
            requestedAt = requestedAt,
            expiresAt = expiresAt,
            approverRoleRequired = approverRoleRequired,
            reason = reason,
            tenantId = tenantId
        )

        inMemoryStore[pending.id] = pending

        if (supabase.isConfigured()) {
            try {
                val payload = mapOf(
                    "id" to pending.id,
                    "tenant_id" to pending.tenantId,
                    "execution_id" to pending.executionId,
                    "node_id" to pending.nodeId,
                    "full_context_snapshot" to fullContextSnapshot.toString(),
                    "requested_at" to pending.requestedAt,
                    "expires_at" to (pending.expiresAt ?: ""),
                    "approver_role_required" to pending.approverRoleRequired,
                    "reason" to pending.reason,
                    "status" to pending.status
                )
                supabase.insertRecord("pending_approvals", pending.tenantId, payload.toJson())
            } catch (e: Exception) {
                logger.warn("Supabase insert pending_approvals warning (in-memory preserved): ${e.message}")
            }
        }

        pending
    }

    suspend fun get(pendingApprovalId: String): PendingApproval? = withContext(Dispatchers.IO) {
        val mem = inMemoryStore[pendingApprovalId]
        if (mem != null) return@withContext mem

        if (supabase.isConfigured()) {
            try {
                val res = supabase.queryTableGlobal(
                    tableName = "pending_approvals",
                    extraParams = mapOf("id" to "eq.$pendingApprovalId")
                )
                if (res.isSuccess) {
                    val arr = Json.parseToJsonElement(res.getOrThrow()).jsonArray
                    val obj = arr.firstOrNull()?.jsonObject
                    if (obj != null) {
                        val parsed = PendingApproval(
                            id = obj["id"]?.jsonPrimitive?.content ?: pendingApprovalId,
                            executionId = obj["execution_id"]?.jsonPrimitive?.content ?: "",
                            nodeId = obj["node_id"]?.jsonPrimitive?.content ?: "",
                            fullContextSnapshot = Json.parseToJsonElement(
                                obj["full_context_snapshot"]?.jsonPrimitive?.content ?: "{}"
                            ),
                            requestedAt = obj["requested_at"]?.jsonPrimitive?.content ?: Instant.now().toString(),
                            expiresAt = obj["expires_at"]?.jsonPrimitive?.content,
                            approverRoleRequired = obj["approver_role_required"]?.jsonPrimitive?.content ?: "TENANT_ADMIN",
                            reason = obj["reason"]?.jsonPrimitive?.content ?: "",
                            tenantId = obj["tenant_id"]?.jsonPrimitive?.content ?: "tenant-default",
                            status = obj["status"]?.jsonPrimitive?.content ?: "PENDING",
                            approvedBy = obj["approved_by"]?.jsonPrimitive?.content,
                            decisionAt = obj["decision_at"]?.jsonPrimitive?.content
                        )
                        inMemoryStore[parsed.id] = parsed
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                logger.warn("Query pending_approvals from Supabase: ${e.message}")
            }
        }

        null
    }

    suspend fun updateStatus(
        pendingApprovalId: String,
        status: String,
        approvedBy: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = inMemoryStore[pendingApprovalId]
        val decisionAt = Instant.now().toString()
        if (existing != null) {
            existing.status = status
            existing.approvedBy = approvedBy
            existing.decisionAt = decisionAt
        }

        if (supabase.isConfigured()) {
            val tenantId = existing?.tenantId ?: "tenant-default"
            try {
                val payload = mutableMapOf<String, Any>(
                    "status" to status,
                    "decision_at" to decisionAt
                )
                if (approvedBy != null) {
                    payload["approved_by"] = approvedBy
                }
                supabase.updateRecord(
                    tableName = "pending_approvals",
                    tenantId = tenantId,
                    filter = "id=eq.$pendingApprovalId",
                    jsonPayload = payload.toJson()
                )
            } catch (e: Exception) {
                logger.warn("Update pending_approvals in Supabase: ${e.message}")
            }
        }

        true
    }

    fun save(pending: PendingApproval) {
        inMemoryStore[pending.id] = pending
    }

    fun listPending(tenantId: String = "tenant-default"): List<PendingApproval> {
        return inMemoryStore.values.filter {
            (it.tenantId == tenantId || tenantId == "tenant-default") && it.status == "PENDING"
        }
    }

    fun listAll(): List<PendingApproval> = inMemoryStore.values.toList()

    fun clearInMemory() {
        inMemoryStore.clear()
    }
}
