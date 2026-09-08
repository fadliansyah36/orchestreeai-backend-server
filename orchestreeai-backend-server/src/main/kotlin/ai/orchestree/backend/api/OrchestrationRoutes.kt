package ai.orchestree.backend.api

import ai.orchestree.backend.database.repositories.intelligence.AgentDecisionOutcomeRepository
import ai.orchestree.backend.database.repositories.intelligence.ConfidenceCalibrationRepository
import ai.orchestree.backend.intelligence.ConfidenceEngine
import ai.orchestree.backend.mcptools.McpToolExecutor
import ai.orchestree.backend.mcptools.McpToolRegistry
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.observability.WorkflowTracingManager
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.orchestration.WorkflowExecutionResult
import ai.orchestree.backend.orchestration.approval.ApprovalDecision
import ai.orchestree.backend.orchestration.approval.PendingApproval
import ai.orchestree.backend.scheduler.jobs.ConfidenceCalibrationJob
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class WorkflowDispatchRequest(
    val workflowDefId: String,
    val prompt: String,
    val tenantId: String = "tenant-default",
    val contextParams: Map<String, String> = emptyMap()
)

@Serializable
data class ApprovalDecisionApiRequest(
    val decision: String, // APPROVED or REJECTED
    val approvedBy: String? = null
)

@Serializable
data class McpExecuteApiRequest(
    val toolName: String,
    val params: Map<String, String>,
    val tenantId: String = "tenant-default",
    val callerRole: String = "STAFF_HUMAN"
)

private val orchestrationEngine = OrchestrationEngine(
    modelRouter = ModelRouter(),
    mcpExecutor = McpToolExecutor(McpToolRegistry.defaultRegistry())
)
private val mcpExecutor = McpToolExecutor(McpToolRegistry.defaultRegistry())
private val executionHistory = ConcurrentHashMap<String, WorkflowExecutionResult>()

fun Route.orchestrationRoutes() {
    route("/orchestration") {
        post("/dispatch") {
            val req = call.receive<WorkflowDispatchRequest>()
            val result = orchestrationEngine.runWorkflow(
                tenantId = req.tenantId,
                workflowDefId = req.workflowDefId,
                prompt = req.prompt,
                contextParams = req.contextParams
            )
            executionHistory[result.executionId] = result
            call.respond(HttpStatusCode.OK, result)
        }

        get("/status/{executionId}") {
            val executionId = call.parameters["executionId"] ?: "unknown"
            val result = executionHistory[executionId]
            if (result != null) {
                call.respond(HttpStatusCode.OK, result)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Execution $executionId not found")
                )
            }
        }

        get("/approvals") {
            val tenantId = call.request.queryParameters["tenantId"] ?: "tenant-default"
            val pendingList = orchestrationEngine.humanGate.pendingApprovalRepo.listPending(tenantId)
            call.respond(HttpStatusCode.OK, pendingList)
        }

        post("/approvals/{id}/decision") {
            val approvalId = call.parameters["id"] ?: ""
            val req = call.receive<ApprovalDecisionApiRequest>()
            val decision = if (req.decision.equals("APPROVED", ignoreCase = true)) {
                ApprovalDecision.APPROVED
            } else {
                ApprovalDecision.REJECTED
            }
            try {
                val approver = req.approvedBy
                    ?: call.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()
                    ?: call.principal<JWTPrincipal>()?.payload?.subject
                    ?: "system_user"
                val result = orchestrationEngine.humanGate.resume(
                    pendingApprovalId = approvalId,
                    decision = decision,
                    approvedBy = approver
                )
                executionHistory[result.executionId] = result
                call.respond(HttpStatusCode.OK, result)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "Approval not found")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Internal error")))
            }
        }

        // LANGKAH 1.2: OpenTelemetry Tracing API
        get("/traces") {
            val traces = WorkflowTracingManager.getAllExecutionSummaries()
            call.respond(HttpStatusCode.OK, traces)
        }

        get("/traces/{executionId}") {
            val executionId = call.parameters["executionId"] ?: ""
            val spans = WorkflowTracingManager.getSpansForExecution(executionId)
            if (spans.isNotEmpty()) {
                call.respond(HttpStatusCode.OK, mapOf(
                    "executionId" to executionId,
                    "traceId" to (spans.firstOrNull()?.traceId ?: ""),
                    "nodeCount" to spans.size,
                    "totalDurationMs" to spans.sumOf { it.durationMs },
                    "spans" to spans
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "No trace spans found for execution $executionId"))
            }
        }

        get("/traces/by-trace/{traceId}") {
            val traceId = call.parameters["traceId"] ?: ""
            val spans = WorkflowTracingManager.getSpansForTrace(traceId)
            call.respond(HttpStatusCode.OK, spans)
        }
    }

    route("/intelligence/confidence") {
        val outcomeRepo = AgentDecisionOutcomeRepository()
        val calibRepo = ConfidenceCalibrationRepository()
        val calibJob = ConfidenceCalibrationJob(outcomeRepo, calibRepo)
        val confidenceEngine = ConfidenceEngine(calibRepo)

        get("/calibration") {
            val tenantId = call.request.queryParameters["tenantId"] ?: "tenant-default"
            var calibrations = calibRepo.getLatestCalibration(tenantId)
            if (calibrations.isEmpty()) {
                calibrations = calibJob.calibrateConfidenceScores(tenantId)
            }
            call.respond(HttpStatusCode.OK, calibrations)
        }

        post("/calibrate") {
            val tenantId = call.request.queryParameters["tenantId"] ?: "tenant-default"
            val result = calibJob.calibrateConfidenceScores(tenantId)
            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "CALIBRATED",
                "tenantId" to tenantId,
                "bucketsCalibrated" to result.size,
                "calibrations" to result
            ))
        }

        get("/audit") {
            val tenantId = call.request.queryParameters["tenantId"] ?: "tenant-default"
            calibJob.calibrateConfidenceScores(tenantId)
            val report = confidenceEngine.generateAuditReport(tenantId)
            call.respond(HttpStatusCode.OK, report)
        }
    }

    route("/mcp") {
        get("/tools") {
            val tools = McpToolRegistry.defaultRegistry().listAll()
            call.respond(HttpStatusCode.OK, tools)
        }

        post("/execute") {
            val req = call.receive<McpExecuteApiRequest>()
            @Suppress("UNCHECKED_CAST")
            val rawParams = req.params as Map<String, Any>
            val result = mcpExecutor.executeTool(
                toolName = req.toolName,
                params = rawParams,
                tenantId = req.tenantId,
                callerRole = req.callerRole
            )
            call.respond(HttpStatusCode.OK, result)
        }
    }
}
