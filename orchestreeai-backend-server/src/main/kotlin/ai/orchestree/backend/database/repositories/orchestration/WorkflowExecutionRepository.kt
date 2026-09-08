package ai.orchestree.backend.database.repositories.orchestration

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.orchestration.WorkflowExecution
import ai.orchestree.backend.orchestration.parseJsonToMap
import ai.orchestree.backend.orchestration.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class WorkflowExecutionRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(WorkflowExecutionRepository::class.java)
    private val inMemoryStore = ConcurrentHashMap<String, WorkflowExecution>()

    fun seedSampleExecutions() {
        if (inMemoryStore.isEmpty()) {
            val sampleId = "wf-exec-sample-001"
            val originalSnapshot = """
                {
                  "tenantId": "tenant-enterprise-001",
                  "prompt": "Analyze market trends and prepare executive summary",
                  "briefing": "Executive Briefing: Q3 Revenue +18%",
                  "finalOutput": "Market summary complete. WhatsApp dispatch confirmed.",
                  "executionId": "$sampleId"
                }
            """.trimIndent()
            val originalExecution = WorkflowExecution(
                id = sampleId,
                tenantId = "tenant-enterprise-001",
                workflowDefId = "executive-briefing-workflow",
                executionStatus = "completed",
                currentStateSnapshot = originalSnapshot,
                resultSummary = "Market summary complete. WhatsApp dispatch confirmed."
            )
            originalExecution.context = parseJsonToMap(originalSnapshot)
            inMemoryStore[sampleId] = originalExecution
        }
    }

    suspend fun createExecution(execution: WorkflowExecution): Result<WorkflowExecution> = withContext(Dispatchers.IO) {
        inMemoryStore[execution.id] = execution
        if (!supabase.isConfigured()) {
            return@withContext Result.success(execution)
        }

        try {
            val payload = mapOf(
                "id" to execution.id,
                "tenant_id" to execution.tenantId,
                "workflow_def_id" to execution.workflowDefId,
                "execution_status" to execution.executionStatus,
                "status" to execution.executionStatus.uppercase(),
                "last_completed_node_id" to (execution.lastCompletedNodeId ?: ""),
                "current_state_snapshot" to (execution.currentStateSnapshot ?: "{}"),
                "started_at" to execution.startedAt,
                "last_updated_at" to System.currentTimeMillis()
            )
            supabase.insertRecord("workflow_executions", execution.tenantId, payload.toJson())
            Result.success(execution)
        } catch (e: Exception) {
            logger.warn("Supabase insert execution warning (in-memory preserved): ${e.message}")
            Result.success(execution)
        }
    }

    suspend fun saveCheckpoint(
        executionId: String,
        currentStateSnapshot: String,
        lastCompletedNodeId: String,
        status: String = "running"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val existing = inMemoryStore[executionId]
        if (existing != null) {
            existing.currentStateSnapshot = currentStateSnapshot
            existing.lastCompletedNodeId = lastCompletedNodeId
            existing.executionStatus = status
            existing.lastUpdatedAt = System.currentTimeMillis()
        }

        if (!supabase.isConfigured()) {
            return@withContext Result.success(true)
        }

        val tenantId = existing?.tenantId ?: "tenant-default"
        try {
            val payload = mapOf(
                "current_state_snapshot" to currentStateSnapshot,
                "last_completed_node_id" to lastCompletedNodeId,
                "execution_status" to status,
                "status" to status.uppercase(),
                "last_updated_at" to System.currentTimeMillis()
            )
            supabase.updateRecord(
                tableName = "workflow_executions",
                tenantId = tenantId,
                filter = "id=eq.$executionId",
                jsonPayload = payload.toJson()
            )
            Result.success(true)
        } catch (e: Exception) {
            logger.warn("Failed saving checkpoint to Supabase for $executionId: ${e.message}")
            Result.success(true)
        }
    }

    suspend fun updateStatus(executionId: String, status: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val existing = inMemoryStore[executionId]
        if (existing != null) {
            existing.executionStatus = status
            existing.lastUpdatedAt = System.currentTimeMillis()
            if (status == "completed" || status == "failed") {
                existing.completedAt = System.currentTimeMillis()
            }
        }

        if (!supabase.isConfigured()) {
            return@withContext Result.success(true)
        }

        val tenantId = existing?.tenantId ?: "tenant-default"
        try {
            val payload = mutableMapOf<String, Any>(
                "execution_status" to status,
                "status" to status.uppercase(),
                "last_updated_at" to System.currentTimeMillis()
            )
            if (status == "completed" || status == "failed") {
                payload["completed_at"] = System.currentTimeMillis()
            }
            supabase.updateRecord(
                tableName = "workflow_executions",
                tenantId = tenantId,
                filter = "id=eq.$executionId",
                jsonPayload = payload.toJson()
            )
            Result.success(true)
        } catch (e: Exception) {
            logger.warn("Failed updating status to Supabase for $executionId: ${e.message}")
            Result.success(true)
        }
    }

    suspend fun findStaleRunning(olderThanMinutes: Long = 5): List<WorkflowExecution> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cutoff = now - (olderThanMinutes * 60 * 1000)

        // 1. Check in-memory store
        val staleFromMemory = inMemoryStore.values.filter { execution ->
            (execution.executionStatus.equals("running", ignoreCase = true)) &&
                execution.lastUpdatedAt <= cutoff
        }

        // 2. Query Supabase if configured
        if (!supabase.isConfigured()) {
            return@withContext staleFromMemory
        }

        try {
            val res = supabase.queryTableGlobal(
                tableName = "workflow_executions",
                extraParams = mapOf(
                    "execution_status" to "eq.running"
                )
            )
            if (res.isSuccess) {
                val jsonStr = res.getOrThrow()
                val parsed = parseSupabaseExecutions(jsonStr, cutoff)
                val combined = (staleFromMemory + parsed).distinctBy { it.id }
                return@withContext combined
            }
        } catch (e: Exception) {
            logger.warn("Querying stale executions from Supabase: ${e.message}")
        }

        return@withContext staleFromMemory
    }

    suspend fun getById(executionId: String): WorkflowExecution? {
        return inMemoryStore[executionId]
    }

    fun registerExecutionInMemory(execution: WorkflowExecution) {
        inMemoryStore[execution.id] = execution
    }

    fun getAllInMemory(): List<WorkflowExecution> = inMemoryStore.values.toList()

    suspend fun listRecentExecutions(limit: Int = 50, tenantId: String? = null): List<WorkflowExecution> = withContext(Dispatchers.IO) {
        val memoryList = inMemoryStore.values.filter { tenantId == null || it.tenantId == tenantId }
        if (!supabase.isConfigured()) {
            return@withContext memoryList.sortedByDescending { it.lastUpdatedAt }.take(limit)
        }

        try {
            val params = mutableMapOf<String, String>()
            if (tenantId != null) params["tenant_id"] = "eq.$tenantId"
            val res = supabase.queryTableGlobal(
                tableName = "workflow_executions",
                select = "*",
                extraParams = params
            )
            if (res.isSuccess) {
                val parsed = parseSupabaseExecutions(res.getOrThrow(), 0L)
                val combined = (memoryList + parsed).distinctBy { it.id }.sortedByDescending { it.lastUpdatedAt }
                return@withContext combined.take(limit)
            }
        } catch (e: Exception) {
            logger.warn("Querying workflow_executions from Supabase: ${e.message}")
        }
        memoryList.sortedByDescending { it.lastUpdatedAt }.take(limit)
    }

    private fun parseSupabaseExecutions(jsonStr: String, cutoff: Long): List<WorkflowExecution> {
        val list = mutableListOf<WorkflowExecution>()
        try {
            val jsonArray = Json.parseToJsonElement(jsonStr).jsonArray
            for (elem in jsonArray) {
                val obj = elem.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: continue
                val tenantId = obj["tenant_id"]?.jsonPrimitive?.content ?: "tenant-default"
                val workflowDefId = obj["workflow_def_id"]?.jsonPrimitive?.content ?: ""
                val execStatus = obj["execution_status"]?.jsonPrimitive?.content ?: "running"
                val lastCompleted = obj["last_completed_node_id"]?.jsonPrimitive?.content
                val snapshot = obj["current_state_snapshot"]?.toString()
                val lastUpdated = obj["last_updated_at"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L

                if (lastUpdated <= cutoff) {
                    val context = if (snapshot != null) parseJsonToMap(snapshot) else mutableMapOf()
                    val exec = WorkflowExecution(
                        id = id,
                        tenantId = tenantId,
                        workflowDefId = workflowDefId,
                        lastCompletedNodeId = lastCompleted,
                        executionStatus = execStatus,
                        currentStateSnapshot = snapshot,
                        lastUpdatedAt = lastUpdated
                    )
                    exec.context = context
                    list.add(exec)
                }
            }
        } catch (e: Exception) {
            logger.warn("Error parsing executions: ${e.message}")
        }
        return list
    }
}
