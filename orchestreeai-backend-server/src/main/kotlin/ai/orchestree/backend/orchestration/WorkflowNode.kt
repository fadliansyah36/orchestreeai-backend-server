package ai.orchestree.backend.orchestration

import kotlinx.serialization.Serializable

enum class WorkflowNodeType {
    CLASSIFY,
    PLAN,
    TOOL_CALL,
    LLM_GENERATE,
    HUMAN_APPROVAL,
    DELIVER,
    CONCURRENT
}

enum class NodeExecutionStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    NEEDS_HUMAN,
    SKIPPED
}

typealias NodeStatus = NodeExecutionStatus

@Serializable
data class WorkflowNodeRun(
    val nodeId: String,
    val nodeType: String,
    val status: String,
    val input: String? = null,
    val output: String? = null,
    val errorMessage: String? = null,
    val durationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class WorkflowExecutionResult(
    val executionId: String,
    val workflowDefId: String,
    val status: String,
    val nodeRuns: List<WorkflowNodeRun> = emptyList(),
    val finalOutput: String = "",
    val durationMs: Long = 0
)

@Serializable
data class WorkflowReplayResult(
    val replayExecutionId: String,
    val originalExecutionId: String,
    val workflowDefId: String,
    val status: String,
    val isDeterministicMatch: Boolean,
    val originalOutput: String,
    val replayOutput: String,
    val nodeRuns: List<WorkflowNodeRun> = emptyList(),
    val sandboxDetails: Map<String, String> = emptyMap(),
    val durationMs: Long = 0
)

sealed class NodeResult {
    data class Success(val output: String? = null, val data: Map<String, Any> = emptyMap()) : NodeResult()
    data class Paused(val pendingApprovalId: String, val reason: String? = null) : NodeResult()
    data class Failed(val errorMessage: String) : NodeResult()

    fun toNodeExecutionResult(): NodeExecutionResult = when (this) {
        is Success -> NodeExecutionResult(NodeExecutionStatus.SUCCESS, output = output, data = data)
        is Paused -> NodeExecutionResult(
            status = NodeExecutionStatus.NEEDS_HUMAN,
            output = reason ?: "Paused awaiting approval $pendingApprovalId",
            data = mapOf("pendingApprovalId" to pendingApprovalId)
        )
        is Failed -> NodeExecutionResult(NodeExecutionStatus.FAILED, errorMessage = errorMessage)
    }
}

data class NodeExecutionResult(
    val status: NodeExecutionStatus,
    val output: String? = null,
    val errorMessage: String? = null,
    val data: Map<String, Any> = emptyMap()
) {
    fun toNodeResult(): NodeResult = when (status) {
        NodeExecutionStatus.SUCCESS -> NodeResult.Success(output, data)
        NodeExecutionStatus.NEEDS_HUMAN -> {
            val pendingId = (data["pendingApprovalId"] as? String) ?: output ?: "pending"
            NodeResult.Paused(pendingId, output)
        }
        else -> NodeResult.Failed(errorMessage ?: "Execution failed")
    }
}

@Serializable
data class WorkflowExecution(
    val id: String,
    val tenantId: String,
    val workflowDefId: String,
    var startNodeId: String? = null,
    var currentNodeId: String? = null,
    var lastCompletedNodeId: String? = null,
    var executionStatus: String = "running",
    var currentStateSnapshot: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    var lastUpdatedAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null,
    var resultSummary: String? = null
) {
    @kotlinx.serialization.Transient
    var context: MutableMap<String, Any> = mutableMapOf()
}

class WorkflowContext(
    val data: MutableMap<String, Any> = mutableMapOf()
) : MutableMap<String, Any> by data {
    val tenantId: String
        get() = (data["tenantId"] ?: data["tenant_id"])?.toString() ?: "tenant-default"

    val executionId: String
        get() = (data["executionId"] ?: data["_executionId"])?.toString() ?: ""

    fun toJson(): String = (data as Map<String, Any>).toJson()

    companion object {
        fun fromJson(jsonStr: String): WorkflowContext = WorkflowContext(parseJsonToMap(jsonStr))
        fun fromJson(jsonElement: kotlinx.serialization.json.JsonElement): WorkflowContext =
            fromJson(jsonElement.toString())
    }
}

interface WorkflowNode {
    val id: String
    val type: WorkflowNodeType
    suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult
    fun next(context: Map<String, Any>): String? = null
}

fun Map<String, Any>.toJson(): String {
    val sb = StringBuilder("{")
    var first = true
    for ((key, value) in this) {
        if (!first) sb.append(",")
        first = false
        sb.append("\"").append(escapeJson(key)).append("\":")
        sb.append(valueToJson(value))
    }
    sb.append("}")
    return sb.toString()
}

private fun valueToJson(value: Any?): String {
    return when (value) {
        null -> "null"
        is Number, is Boolean -> value.toString()
        is String -> "\"${escapeJson(value)}\""
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            (value as Map<String, Any>).toJson()
        }
        is Iterable<*> -> {
            val items = value.map { valueToJson(it) }
            "[${items.joinToString(",")}]"
        }
        else -> "\"${escapeJson(value.toString())}\""
    }
}

private fun escapeJson(str: String): String {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\b")
        .replace("\u000c", "\\f")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

fun parseJsonToMap(jsonStr: String): MutableMap<String, Any> {
    if (jsonStr.isBlank() || jsonStr == "{}") return mutableMapOf()
    return try {
        val element = kotlinx.serialization.json.Json.parseToJsonElement(jsonStr)
        if (element is kotlinx.serialization.json.JsonObject) {
            jsonElementToMap(element)
        } else {
            mutableMapOf()
        }
    } catch (e: Exception) {
        mutableMapOf()
    }
}

private fun jsonElementToMap(obj: kotlinx.serialization.json.JsonObject): MutableMap<String, Any> {
    val map = mutableMapOf<String, Any>()
    for ((k, v) in obj) {
        map[k] = when (v) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                if (v.isString) v.content
                else {
                    val raw = v.content
                    raw.toBooleanStrictOrNull() ?: raw.toLongOrNull() ?: raw.toDoubleOrNull() ?: raw
                }
            }
            is kotlinx.serialization.json.JsonObject -> jsonElementToMap(v)
            is kotlinx.serialization.json.JsonArray -> v.map { elem ->
                if (elem is kotlinx.serialization.json.JsonPrimitive) {
                    if (elem.isString) elem.content
                    else {
                        val raw = elem.content
                        raw.toBooleanStrictOrNull() ?: raw.toLongOrNull() ?: raw.toDoubleOrNull() ?: raw
                    }
                } else elem.toString()
            }
        }
    }
    return map
}
