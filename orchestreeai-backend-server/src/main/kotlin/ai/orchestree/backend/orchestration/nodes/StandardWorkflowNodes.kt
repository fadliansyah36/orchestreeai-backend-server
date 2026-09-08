package ai.orchestree.backend.orchestration.nodes

import ai.orchestree.backend.intelligence.IntentClassifier
import ai.orchestree.backend.mcptools.McpToolExecutor
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.NodeExecutionResult
import ai.orchestree.backend.orchestration.NodeExecutionStatus
import ai.orchestree.backend.orchestration.WorkflowExecution
import ai.orchestree.backend.orchestration.WorkflowNode
import ai.orchestree.backend.orchestration.WorkflowNodeType
import ai.orchestree.backend.orchestration.approval.HumanInTheLoopGate
import org.slf4j.LoggerFactory

class ClassifyWorkflowNode(
    override val id: String = "classify-node",
    private val intentClassifier: IntentClassifier = IntentClassifier(),
    var nextNodeId: String? = null
) : WorkflowNode {
    override val type: WorkflowNodeType = WorkflowNodeType.CLASSIFY

    override suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult {
        val prompt = context["prompt"]?.toString() ?: ""
        val classified = intentClassifier.classify(prompt)
        context["intent"] = classified.intentCode
        context["intentConfidence"] = classified.confidence
        return NodeExecutionResult(
            status = NodeExecutionStatus.SUCCESS,
            output = "Classified intent as: ${classified.intentCode} (confidence: ${classified.confidence})"
        )
    }

    override fun next(context: Map<String, Any>): String? = nextNodeId
}

class PlanWorkflowNode(
    override val id: String = "plan-node",
    private val modelRouter: ModelRouter = ModelRouter(),
    var nextNodeId: String? = null
) : WorkflowNode {
    override val type: WorkflowNodeType = WorkflowNodeType.PLAN

    override suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult {
        val prompt = context["prompt"]?.toString() ?: ""
        val intent = context["intent"]?.toString() ?: "GENERAL_INQUIRY"

        val planRes = modelRouter.execute(
            ModelRouteRequest(
                taskCategory = "REASONING",
                prompt = "Create a structured execution plan for intent '$intent' based on prompt: $prompt",
                tenantId = context["tenantId"]?.toString() ?: "tenant-default"
            )
        )

        val planText = if (planRes.isSuccess) planRes.getOrThrow().text else "Standard 3-step execution plan"
        context["executionPlan"] = planText
        return NodeExecutionResult(
            status = NodeExecutionStatus.SUCCESS,
            output = "Plan generated: $planText"
        )
    }

    override fun next(context: Map<String, Any>): String? = nextNodeId
}

class ToolCallWorkflowNode(
    override val id: String = "tool-call-node",
    private val mcpExecutor: McpToolExecutor,
    var nextNodeId: String? = null
) : WorkflowNode {
    override val type: WorkflowNodeType = WorkflowNodeType.TOOL_CALL

    override suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult {
        val toolName = context["toolName"]?.toString() ?: "crm_fetch_lead"
        val params = (context["toolParams"] as? Map<String, Any>) ?: emptyMap()

        return try {
            val tenantId = context["tenantId"]?.toString() ?: "tenant-default"
            val callerRole = context["callerRole"]?.toString() ?: "STAFF_HUMAN"
            val toolRes = mcpExecutor.executeTool(toolName, params, tenantId, callerRole)
            if (toolRes.success) {
                context["toolResult"] = toolRes.output
                NodeExecutionResult(
                    status = NodeExecutionStatus.SUCCESS,
                    output = "Tool '$toolName' executed successfully: ${toolRes.output}"
                )
            } else {
                NodeExecutionResult(
                    status = NodeExecutionStatus.FAILED,
                    errorMessage = toolRes.errorMessage ?: "Tool execution failed"
                )
            }
        } catch (e: Exception) {
            NodeExecutionResult(
                status = NodeExecutionStatus.FAILED,
                errorMessage = "Tool call failed: ${e.message}"
            )
        }
    }

    override fun next(context: Map<String, Any>): String? = nextNodeId
}

class LlmGenerateWorkflowNode(
    override val id: String = "llm-generate-node",
    private val modelRouter: ModelRouter = ModelRouter(),
    var nextNodeId: String? = null
) : WorkflowNode {
    override val type: WorkflowNodeType = WorkflowNodeType.LLM_GENERATE

    override suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult {
        val prompt = context["prompt"]?.toString() ?: ""
        val contextInfo = context["toolResult"]?.toString() ?: context["executionPlan"]?.toString() ?: ""

        val res = modelRouter.execute(
            ModelRouteRequest(
                taskCategory = "GENERAL",
                prompt = "Synthesize final response given prompt: $prompt and background info: $contextInfo",
                tenantId = context["tenantId"]?.toString() ?: "tenant-default"
            )
        )

        return if (res.isSuccess) {
            val responseText = res.getOrThrow().text
            context["finalOutput"] = responseText
            NodeExecutionResult(status = NodeExecutionStatus.SUCCESS, output = responseText)
        } else {
            NodeExecutionResult(
                status = NodeExecutionStatus.FAILED,
                errorMessage = res.exceptionOrNull()?.message ?: "LLM generation failed"
            )
        }
    }

    override fun next(context: Map<String, Any>): String? = nextNodeId
}

class HumanApprovalWorkflowNode(
    override val id: String = "human-approval-node",
    var nextNodeId: String? = null,
    private val humanGateProvider: (() -> HumanInTheLoopGate)? = null,
    val approverRole: String? = null,
    val defaultReason: String = "Workflow requires human authorization before proceeding"
) : WorkflowNode {
    override val type: WorkflowNodeType = WorkflowNodeType.HUMAN_APPROVAL

    override suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult {
        context["approvalRequired"] = true
        val reason = context["approvalReason"]?.toString() ?: defaultReason

        val gate = try {
            humanGateProvider?.invoke() ?: if (HumanInTheLoopGate.isInitialized()) HumanInTheLoopGate.getInstance() else null
        } catch (e: Exception) {
            null
        }

        return if (gate != null) {
            val execId = context["_executionId"]?.toString()
                ?: context["executionId"]?.toString()
                ?: "exec-${java.util.UUID.randomUUID().toString().take(8)}"
            val tenantId = context["tenantId"]?.toString() ?: "tenant-default"
            val workflowDefId = context["workflowDefId"]?.toString() ?: "workflow-default"

            val execution = WorkflowExecution(
                id = execId,
                tenantId = tenantId,
                workflowDefId = workflowDefId,
                currentNodeId = id
            ).apply {
                this.context = context
            }

            val paused = gate.interrupt(execution, reason)
            paused.toNodeExecutionResult()
        } else {
            NodeExecutionResult(
                status = NodeExecutionStatus.NEEDS_HUMAN,
                output = reason,
                data = mapOf("reason" to reason)
            )
        }
    }

    override fun next(context: Map<String, Any>): String? = nextNodeId
}

class DeliverWorkflowNode(
    override val id: String = "deliver-node",
    var nextNodeId: String? = null
) : WorkflowNode {
    override val type: WorkflowNodeType = WorkflowNodeType.DELIVER

    override suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult {
        val output = context["finalOutput"]?.toString() ?: "Completed successfully"
        val isSandbox = context["is_sandbox_replay"] == true || context["sandbox_mode"] == "ISOLATED_DRY_RUN"
        return if (isSandbox) {
            NodeExecutionResult(
                status = NodeExecutionStatus.SUCCESS,
                output = "[SANDBOX_REPLAY] External delivery suppressed (no actual WhatsApp/Telegram/Slack/Payment calls). Dry-run payload: $output"
            )
        } else {
            NodeExecutionResult(
                status = NodeExecutionStatus.SUCCESS,
                output = "Delivered payload: $output"
            )
        }
    }

    override fun next(context: Map<String, Any>): String? = nextNodeId
}

class GenericStepWorkflowNode(
    override val id: String,
    override val type: WorkflowNodeType = WorkflowNodeType.PLAN,
    var nextNodeId: String? = null,
    private val action: suspend (MutableMap<String, Any>) -> NodeExecutionResult = {
        NodeExecutionResult(NodeExecutionStatus.SUCCESS, "Node $id executed successfully")
    }
) : WorkflowNode {
    override suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult {
        return action(context)
    }

    override fun next(context: Map<String, Any>): String? = nextNodeId
}
