package ai.orchestree.backend.database.repositories.orchestration

import ai.orchestree.backend.config.EnvLoader
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
import java.sql.DriverManager
import java.util.Properties
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
                  "tenantId": "tenant-sample-001",
                  "prompt": "Analyze market trends and prepare executive summary",
                  "briefing": "Executive Briefing: Q3 Revenue +18%",
                  "finalOutput": "Market summary complete. WhatsApp dispatch confirmed.",
                  "executionId": "$sampleId"
                }
            """.trimIndent()
            val originalExecution = WorkflowExecution(
                id = sampleId,
                tenantId = "tenant-sample-001",
                workflowDefId = "executive-briefing-workflow",
                executionStatus = "completed",
                currentStateSnapshot = originalSnapshot,
                resultSummary = "Market summary complete. WhatsApp dispatch confirmed."
            )
            originalExecution.context = parseJsonToMap(originalSnapshot)
            inMemoryStore[sampleId] = originalExecution
        }
    }

    private fun getJdbcConnection(): java.sql.Connection? {
        return try {
            ai.orchestree.backend.billing.DatabaseManager.getConnection()
        } catch (e: Exception) {
            logger.warn("Could not establish JDBC connection via DatabaseManager: ${e.message}")
            null
        }
    }

    suspend fun createExecution(execution: WorkflowExecution): Result<WorkflowExecution> = withContext(Dispatchers.IO) {
        inMemoryStore[execution.id] = execution

        val totalSteps = when (execution.workflowDefId) {
            "wf-chat-inbound" -> 5
            "universal_ai_selection" -> 10
            "wf-task-auto-execute" -> 5
            "wf-marketing-campaign" -> 4
            "wf-chief-of-staff-briefing" -> 5
            "wf-world-monitor-scan" -> 4
            "wf-enterprise-cross-system-correlation" -> 5
            "wf-competitor-audit" -> 3
            else -> 5
        }
        val inputPayload = execution.context.toJson().takeIf { it.isNotBlank() && it != "{}" }
            ?: execution.currentStateSnapshot?.takeIf { it.isNotBlank() && it != "{}" }
            ?: run {
                val fallbackMap = mapOf(
                    "prompt" to (execution.context["prompt"]?.toString() ?: ""),
                    "tenantId" to execution.tenantId,
                    "executionId" to execution.id,
                    "workflowDefId" to execution.workflowDefId
                )
                fallbackMap.toJson()
            }

        var dbSuccess = false
        var lastException: Exception? = null

        // 1. Try Direct JDBC (PostgreSQL / Supabase direct or pooler)
        try {
            getJdbcConnection()?.use { conn ->
                val sql = """
                    INSERT INTO workflow_executions (
                        id, tenant_id, workflow_def_id, trigger_source, input_payload,
                        status, execution_status, current_step_index, total_steps,
                        started_at, current_state_snapshot, last_completed_node_id, last_updated_at
                    ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?::jsonb, ?, now())
                    ON CONFLICT (id) DO UPDATE SET
                        status = EXCLUDED.status,
                        execution_status = EXCLUDED.execution_status,
                        current_step_index = EXCLUDED.current_step_index,
                        total_steps = EXCLUDED.total_steps,
                        current_state_snapshot = EXCLUDED.current_state_snapshot,
                        last_completed_node_id = EXCLUDED.last_completed_node_id,
                        last_updated_at = now()
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, execution.id)
                    stmt.setString(2, execution.tenantId)
                    stmt.setString(3, execution.workflowDefId)
                    stmt.setString(4, "OrchestrationEngine")
                    stmt.setString(5, inputPayload)
                    stmt.setString(6, execution.executionStatus.uppercase())
                    stmt.setString(7, execution.executionStatus.lowercase())
                    stmt.setInt(8, 0)
                    stmt.setInt(9, totalSteps)
                    stmt.setLong(10, execution.startedAt)
                    stmt.setString(11, inputPayload)
                    stmt.setString(12, execution.lastCompletedNodeId ?: "")
                    stmt.executeUpdate()
                }
                logger.info("Successfully inserted workflow_execution ${execution.id} to PostgreSQL database via JDBC")
                dbSuccess = true
            }
        } catch (e: Exception) {
            logger.error("JDBC insert error on workflow_executions (${execution.id}): ${e.message}", e)
            lastException = e
        }

        // 2. Try Supabase REST Client if JDBC was not configured or failed
        if (!dbSuccess && supabase.isConfigured()) {
            try {
                val payload = mapOf(
                    "id" to execution.id,
                    "tenant_id" to execution.tenantId,
                    "workflow_def_id" to execution.workflowDefId,
                    "trigger_source" to "OrchestrationEngine",
                    "input_payload" to inputPayload,
                    "status" to execution.executionStatus.uppercase(),
                    "execution_status" to execution.executionStatus.lowercase(),
                    "current_step_index" to 0,
                    "total_steps" to totalSteps,
                    "started_at" to execution.startedAt,
                    "last_completed_node_id" to (execution.lastCompletedNodeId ?: "")
                )
                val res = supabase.insertRecord("workflow_executions", execution.tenantId, payload.toJson())
                if (res.isSuccess) {
                    logger.info("Successfully inserted workflow_execution ${execution.id} via Supabase REST")
                    dbSuccess = true
                } else {
                    val ex = res.exceptionOrNull() ?: RuntimeException("Supabase insert returned failure")
                    logger.error("Supabase REST insert failed on workflow_executions (${execution.id}): ${ex.message}", ex)
                    lastException = ex as? Exception ?: RuntimeException(ex.message, ex)
                }
            } catch (e: Exception) {
                logger.error("Exception in Supabase REST insert for workflow_executions (${execution.id}): ${e.message}", e)
                lastException = e
            }
        }

        if (!dbSuccess) {
            val ex = lastException ?: IllegalStateException("Failed to persist workflow execution ${execution.id}: No database connection available")
            logger.warn("Database connection unavailable, workflow execution ${execution.id} stored in in-memory fallback: ${ex.message}")
        }

        Result.success(execution)
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

        var dbSuccess = false
        var lastException: Exception? = null

        // 1. Try Direct JDBC
        try {
            getJdbcConnection()?.use { conn ->
                val sql = """
                    UPDATE workflow_executions
                    SET current_state_snapshot = ?::jsonb,
                        last_completed_node_id = ?,
                        execution_status = ?,
                        status = ?,
                        last_updated_at = now()
                    WHERE id = ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, currentStateSnapshot.ifBlank { "{}" })
                    stmt.setString(2, lastCompletedNodeId)
                    stmt.setString(3, status.lowercase())
                    stmt.setString(4, status.uppercase())
                    stmt.setString(5, executionId)
                    stmt.executeUpdate()
                }
                dbSuccess = true
            }
        } catch (e: Exception) {
            logger.error("JDBC error saving checkpoint for $executionId: ${e.message}", e)
            lastException = e
        }

        // 2. Try Supabase REST
        if (!dbSuccess && supabase.isConfigured()) {
            val tenantId = existing?.tenantId ?: "tenant-default"
            try {
                val payload = mapOf(
                    "last_completed_node_id" to lastCompletedNodeId,
                    "execution_status" to status.lowercase(),
                    "status" to status.uppercase()
                )
                supabase.updateRecord(
                    tableName = "workflow_executions",
                    tenantId = tenantId,
                    filter = "id=eq.$executionId",
                    jsonPayload = payload.toJson()
                )
                dbSuccess = true
            } catch (e: Exception) {
                logger.error("Failed saving checkpoint to Supabase for $executionId: ${e.message}", e)
                lastException = e
            }
        }

        if (!dbSuccess && lastException != null) {
            logger.warn("Database connection unavailable, checkpoint for execution $executionId kept in in-memory fallback: ${lastException.message}")
        }

        Result.success(true)
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

        var dbSuccess = false
        var lastException: Exception? = null

        // 1. Try Direct JDBC
        try {
            getJdbcConnection()?.use { conn ->
                val isFinished = status.equals("completed", ignoreCase = true) || status.equals("failed", ignoreCase = true)
                val sql = if (isFinished) {
                    """
                        UPDATE workflow_executions
                        SET execution_status = ?,
                            status = ?,
                            completed_at = ?,
                            last_updated_at = now()
                        WHERE id = ?
                    """.trimIndent()
                } else {
                    """
                        UPDATE workflow_executions
                        SET execution_status = ?,
                            status = ?,
                            last_updated_at = now()
                        WHERE id = ?
                    """.trimIndent()
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, status.lowercase())
                    stmt.setString(2, status.uppercase())
                    if (isFinished) {
                        stmt.setLong(3, System.currentTimeMillis())
                        stmt.setString(4, executionId)
                    } else {
                        stmt.setString(3, executionId)
                    }
                    stmt.executeUpdate()
                }
                dbSuccess = true
            }
        } catch (e: Exception) {
            logger.error("JDBC error updating status for $executionId: ${e.message}", e)
            lastException = e
        }

        // 2. Try Supabase REST
        if (!dbSuccess && supabase.isConfigured()) {
            val tenantId = existing?.tenantId ?: "tenant-default"
            try {
                val payload = mutableMapOf<String, Any>(
                    "execution_status" to status.lowercase(),
                    "status" to status.uppercase()
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
                dbSuccess = true
            } catch (e: Exception) {
                logger.error("Failed updating status to Supabase for $executionId: ${e.message}", e)
                lastException = e
            }
        }

        if (!dbSuccess && lastException != null) {
            logger.warn("Database connection unavailable, status for execution $executionId kept in in-memory fallback: ${lastException.message}")
        }

        Result.success(true)
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

    suspend fun getById(executionId: String): WorkflowExecution? = withContext(Dispatchers.IO) {
        val mem = inMemoryStore[executionId]
        if (mem != null) return@withContext mem

        try {
            getJdbcConnection()?.use { conn ->
                val sql = "SELECT id, tenant_id, workflow_def_id, execution_status, last_completed_node_id, current_state_snapshot, started_at, EXTRACT(EPOCH FROM last_updated_at)*1000 AS last_updated_ms FROM workflow_executions WHERE id = ?"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, executionId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        val snapshot = rs.getString("current_state_snapshot")
                        val startedAt = rs.getLong("started_at")
                        val lastUpdated = rs.getLong("last_updated_ms")
                        val exec = WorkflowExecution(
                            id = rs.getString("id"),
                            tenantId = rs.getString("tenant_id"),
                            workflowDefId = rs.getString("workflow_def_id"),
                            lastCompletedNodeId = rs.getString("last_completed_node_id"),
                            executionStatus = rs.getString("execution_status") ?: "running",
                            currentStateSnapshot = snapshot,
                            startedAt = startedAt,
                            lastUpdatedAt = if (lastUpdated > 0) lastUpdated else startedAt
                        )
                        if (snapshot != null) exec.context = parseJsonToMap(snapshot)
                        inMemoryStore[executionId] = exec
                        return@withContext exec
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Error fetching workflow_execution $executionId from JDBC: ${e.message}")
        }
        null
    }

    fun registerExecutionInMemory(execution: WorkflowExecution) {
        inMemoryStore[execution.id] = execution
    }

    fun getAllInMemory(): List<WorkflowExecution> = inMemoryStore.values.toList()

    suspend fun listRecentExecutions(limit: Int = 50, tenantId: String? = null): List<WorkflowExecution> = withContext(Dispatchers.IO) {
        val memoryList = inMemoryStore.values.filter { tenantId == null || it.tenantId == tenantId }

        // 1. Try JDBC
        try {
            getJdbcConnection()?.use { conn ->
                val sql = if (tenantId != null) {
                    "SELECT id, tenant_id, workflow_def_id, execution_status, last_completed_node_id, current_state_snapshot, started_at, EXTRACT(EPOCH FROM last_updated_at)*1000 AS last_updated_ms FROM workflow_executions WHERE tenant_id = ? ORDER BY started_at DESC LIMIT ?"
                } else {
                    "SELECT id, tenant_id, workflow_def_id, execution_status, last_completed_node_id, current_state_snapshot, started_at, EXTRACT(EPOCH FROM last_updated_at)*1000 AS last_updated_ms FROM workflow_executions ORDER BY started_at DESC LIMIT ?"
                }
                conn.prepareStatement(sql).use { stmt ->
                    if (tenantId != null) {
                        stmt.setString(1, tenantId)
                        stmt.setInt(2, limit)
                    } else {
                        stmt.setInt(1, limit)
                    }
                    val rs = stmt.executeQuery()
                    val dbList = mutableListOf<WorkflowExecution>()
                    while (rs.next()) {
                        val id = rs.getString("id")
                        val tId = rs.getString("tenant_id")
                        val wDefId = rs.getString("workflow_def_id")
                        val execStatus = rs.getString("execution_status") ?: "running"
                        val lastCompleted = rs.getString("last_completed_node_id")
                        val snapshot = rs.getString("current_state_snapshot")
                        val startedAt = rs.getLong("started_at")
                        val lastUpdated = rs.getLong("last_updated_ms")
                        val context = if (snapshot != null) parseJsonToMap(snapshot) else mutableMapOf()
                        val exec = WorkflowExecution(
                            id = id,
                            tenantId = tId,
                            workflowDefId = wDefId,
                            lastCompletedNodeId = lastCompleted,
                            executionStatus = execStatus,
                            currentStateSnapshot = snapshot,
                            startedAt = startedAt,
                            lastUpdatedAt = if (lastUpdated > 0) lastUpdated else startedAt
                        )
                        exec.context = context
                        dbList.add(exec)
                    }
                    if (dbList.isNotEmpty()) {
                        val combined = (memoryList + dbList).distinctBy { it.id }.sortedByDescending { it.lastUpdatedAt }
                        return@withContext combined.take(limit)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("JDBC error listing workflow_executions: ${e.message}")
        }
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
