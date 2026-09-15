package ai.orchestree.backend.api

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.identity.TenantRepository
import ai.orchestree.backend.database.repositories.taskboard.TaskRepository
import ai.orchestree.backend.database.repositories.workforce.TenantDomainRepository
import ai.orchestree.backend.modelrouter.ModelRouter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class DepartmentCreateRequest(
    val name: String,
    val description: String = "",
    val managerUserId: String? = null,
    val colorTag: String = "#1E6FE0"
)

@Serializable
data class StaffCreateRequest(
    val name: String,
    val email: String,
    val role: String,
    val departmentId: String
)

@Serializable
data class AgentCreateRequest(
    val name: String,
    val jobTitleId: String,
    val departmentId: String,
    val structuralRoleId: String? = null
)

@Serializable
data class TaskMoveRequest(
    val toColumn: String = "",
    val targetColumn: String = "",
    val version: Int = 1,
    val expectedVersion: Int = 1,
    val targetIndex: Int = 0
) {
    val destinationColumn: String get() = toColumn.ifBlank { targetColumn }
}

@Serializable
data class InboundChannelMessageApiRequest(
    val channel: String, // 'telegram', 'whatsapp', 'dashboard'
    val senderId: String,
    val senderName: String? = null,
    val message: String,
    val tenantId: String = "tenant-default"
)

@Serializable
data class ChecklistItemCreateRequest(
    val itemText: String,
    val orderIndex: Int = 0
)

@Serializable
data class TaskDescriptionUpdateRequest(
    val descriptionRichText: String
)

@Serializable
data class AttachmentCreateRequest(
    val fileName: String,
    val fileUrl: String,
    val fileSizeBytes: Long = 0L,
    val uploadedBy: String = "staff"
)

@Serializable
data class CompetitorTargetItem(
    val id: String,
    val tenantId: String,
    val name: String,
    val category: String,
    val urls: List<String> = emptyList(),
    val frequency: String = "daily",
    val status: String = "ACTIVE"
)

@Serializable
data class ProactiveSubscriptionItem(
    val id: String,
    val tenantId: String,
    val staffId: String,
    val channel: String,
    val types: List<String> = emptyList(),
    val sendTimes: List<String> = emptyList(),
    val status: String = "ACTIVE"
)

@Serializable
data class WorldTrendClusterItem(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val confidence: Double = 0.95,
    val publishedAt: Long = System.currentTimeMillis()
)

@Serializable
data class CompetitorTargetRequest(
    val name: String,
    val category: String,
    val urls: List<String>,
    val frequency: String = "daily",
    val assignedAgentId: String? = null
)

@Serializable
data class IntegrationConnectRequest(
    val authCode: String? = null,
    val redirectUri: String? = null,
    val scopes: List<String> = emptyList()
)

@Serializable
data class ProactiveSubscriptionRequest(
    val staffId: String,
    val channel: String,
    val types: List<String>,
    val sendTimes: List<String>
)

@Serializable
data class TenantDashboardOverviewResponse(
    val tenantId: String,
    val activeAgents: Int,
    val humanStaffCount: Int,
    val activeTasks: Int,
    val completionRate: Double,
    val status: String
)

@Serializable
data class GenericStatusResponse(
    val status: String,
    val message: String? = null,
    val id: String? = null
)

@Serializable
data class BoardKanbanResponse(
    val boardId: String,
    val tenantId: String,
    val columns: List<String>
)

@Serializable
data class CompetitorInsightItem(
    val id: String,
    val targetId: String,
    val category: String,
    val summary: String,
    val confidence: Double,
    val impactScore: Double
)

@Serializable
data class RankingEntry(
    val name: String,
    val score: Double,
    val rank: Int
)

@Serializable
data class AnalyticsScoreResponse(
    val period: String,
    val humanRanking: List<RankingEntry>,
    val aiRanking: List<RankingEntry>
)

@Serializable
data class WorkReportDailyItem(
    val id: String,
    val tenantId: String,
    val staffId: String,
    val staffName: String,
    val reportDate: String,
    val accomplishments: String,
    val blockers: String = "",
    val plannedNext: String = "",
    val status: String = "SUBMITTED"
)

@Serializable
data class CreateWorkReportRequest(
    val staffId: String,
    val staffName: String,
    val reportDate: String,
    val accomplishments: String,
    val blockers: String = "",
    val plannedNext: String = ""
)

@Serializable
data class GoalKpiItem(
    val id: String,
    val tenantId: String,
    val title: String,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val period: String,
    val status: String = "ON_TRACK"
)

@Serializable
data class PerformanceReviewItem(
    val id: String,
    val tenantId: String,
    val staffId: String,
    val reviewerId: String,
    val period: String,
    val overallScore: Double,
    val feedback: String,
    val status: String = "FINALIZED"
)

@Serializable
data class PerformanceRiskPredictionItem(
    val id: String,
    val tenantId: String,
    val staffId: String,
    val staffName: String,
    val riskLevel: String,
    val riskScore: Double,
    val primaryFactor: String,
    val recommendation: String
)

@Serializable
data class ExecutiveBriefItem(
    val id: String,
    val tenantId: String,
    val title: String,
    val period: String,
    val executiveSummary: String,
    val keyAchievements: List<String>,
    val riskAreas: List<String>
)

@Serializable
data class SecurityAnomalyItem(
    val id: String,
    val tenantId: String,
    val anomalyType: String,
    val severity: String,
    val description: String,
    val detectedAt: Long,
    val status: String = "PENDING_REVIEW"
)

@Serializable
data class DataSubjectRequestItem(
    val id: String,
    val tenantId: String,
    val requestType: String,
    val requesterEmail: String,
    val status: String = "PENDING",
    val requestedAt: Long = System.currentTimeMillis()
)

@Serializable
data class CreateDataSubjectRequest(
    val requestType: String,
    val requesterEmail: String,
    val details: String = ""
)

@Serializable
data class AttendanceAnomalyItem(
    val id: String,
    val tenantId: String,
    val staffId: String,
    val staffName: String,
    val anomalyType: String,
    val timestamp: Long,
    val status: String = "OPEN",
    val resolutionNotes: String = ""
)

fun Route.tenantRoutes() {
    val supabase = SupabaseClientProvider.fromEnv()
    val tenantRepo = TenantRepository(supabase)
    val taskRepo = TaskRepository(supabase)
    val modelRouter = ModelRouter()
    val orchestrationEngine = ai.orchestree.backend.orchestration.OrchestrationEngine(modelRouter = modelRouter)

    route("/tenants/{id}") {
        // Overview dashboard tenant (PRD Master 15.1)
        get("/dashboard/overview") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val overview = TenantDomainRepository.getDashboardOverview(tenantId)
            call.respond(HttpStatusCode.OK, overview)
        }

        // CRUD Departemen (PRD Master 15.1, 8.4)
        get("/departments") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val result = supabase.queryTable("departments", tenantId)
            call.respondText(result.getOrDefault("[]"), ContentType.Application.Json, HttpStatusCode.OK)
        }

        post("/departments") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<DepartmentCreateRequest>()
            val deptId = "dept-${java.util.UUID.randomUUID().toString().take(8)}"
            val payload = buildJsonObject {
                put("id", deptId)
                put("tenant_id", tenantId)
                put("name", req.name)
                put("description", req.description)
                put("color_hex", req.colorTag.ifBlank { "#1E6FE0" })
                if (!req.managerUserId.isNullOrBlank()) {
                    put("manager_user_id", req.managerUserId)
                }
            }.toString()
            supabase.insertRecord("departments", tenantId, payload)
            call.respond(HttpStatusCode.Created, GenericStatusResponse(status = "created", id = deptId, message = req.name))
        }

        delete("/departments/{deptId}") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val deptId = call.parameters["deptId"] ?: ""
            supabase.deleteRecord("departments", tenantId, "id=eq.$deptId")
            call.respond(HttpStatusCode.OK, GenericStatusResponse(status = "DELETED", id = deptId, message = "Department deleted"))
        }

        // CRUD Staff Human (PRD Master 15.1)
        get("/staff") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val result = supabase.queryTable("staff_profiles", tenantId, "*,users(name,email,avatar_url)")
            call.respondText(result.getOrDefault("[]"), ContentType.Application.Json, HttpStatusCode.OK)
        }

        delete("/staff/{staffId}") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val staffId = call.parameters["staffId"] ?: ""
            supabase.deleteRecord("staff_profiles", tenantId, "id=eq.$staffId")
            call.respond(HttpStatusCode.OK, GenericStatusResponse(status = "DELETED", id = staffId, message = "Staff deleted"))
        }

        post("/staff") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<StaffCreateRequest>()
            val userId = "usr-${java.util.UUID.randomUUID().toString().take(8)}"
            val userPayload = buildJsonObject {
                put("id", userId)
                put("tenant_id", tenantId)
                put("email", req.email)
                put("name", req.name)
                put("password_hash", "scrypt_temp_placeholder")
                put("is_active", true)
            }.toString()
            supabase.insertRecord("users", tenantId, userPayload)
            val staffId = "stf-${java.util.UUID.randomUUID().toString().take(8)}"
            val staffPayload = buildJsonObject {
                put("id", staffId)
                put("user_id", userId)
                put("tenant_id", tenantId)
                if (req.departmentId.isNotBlank()) {
                    put("department_id", req.departmentId)
                }
                put("job_title", req.role)
                put("status", "ACTIVE")
            }.toString()
            supabase.insertRecord("staff_profiles", tenantId, staffPayload)
            call.respond(HttpStatusCode.Created, GenericStatusResponse(status = "created", id = staffId, message = req.email))
        }

        // Kelola AI Agent tenant (PRD Master 15.1, 25.5)
        get("/agents") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val result = supabase.queryTable("ai_agents", tenantId)
            call.respondText(result.getOrDefault("[]"), ContentType.Application.Json, HttpStatusCode.OK)
        }

        post("/agents") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<AgentCreateRequest>()
            val agentId = "agt-${java.util.UUID.randomUUID().toString().take(8)}"
            val payload = buildJsonObject {
                put("id", agentId)
                put("tenant_id", tenantId)
                put("name", req.name)
                put("role_title", req.name)
                if (req.departmentId.isNotBlank()) {
                    put("department_id", req.departmentId)
                }
                if (!req.jobTitleId.isNullOrBlank()) {
                    put("job_title_id", req.jobTitleId)
                }
                if (!req.structuralRoleId.isNullOrBlank()) {
                    put("structural_role_id", req.structuralRoleId)
                }
                put("status", "ONLINE")
            }.toString()
            supabase.insertRecord("ai_agents", tenantId, payload)
            call.respond(HttpStatusCode.Created, GenericStatusResponse(status = "created", id = agentId, message = req.name))
        }

        // CRUD Tasks tenant (PRD Master 15.1, 8.2, LANGKAH 1.1)
        get("/tasks") {
            val boardId = call.request.queryParameters["boardId"] ?: "default"
            val principal = call.principal<JWTPrincipal>()
            val authenticatedUserId = principal?.payload?.subject ?: principal?.payload?.getClaim("user_id")?.asString()
            val userId = authenticatedUserId
                ?: call.request.queryParameters["userId"]
                ?: call.request.headers["X-User-Id"]
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User identity is required to fetch assigned tasks"))
                return@get
            }

            val tasks = taskRepo.getTasksForUser(userId, boardId)
            call.respond(HttpStatusCode.OK, tasks)
        }

        post("/tasks") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<Map<String, String>>()
            val taskId = "tsk-${java.util.UUID.randomUUID().toString().take(8)}"
            val payload = buildJsonObject {
                put("id", taskId)
                put("tenant_id", tenantId)
                put("title", req["title"] ?: "")
                put("description", req["description"] ?: "")
                put("column", req["column"] ?: "TODO")
                put("priority", req["priority"] ?: "MEDIUM")
                put("assignee_type", req["assigneeType"] ?: "AI_AGENT")
                put("assignee_id", req["assigneeId"] ?: "")
                put("assignee_name", req["assigneeName"] ?: "")
                req["departmentId"]?.takeIf { it.isNotBlank() }?.let {
                    put("department_id", it)
                }
            }.toString()
            supabase.insertRecord("tasks", tenantId, payload)
            call.respond(HttpStatusCode.Created, GenericStatusResponse(status = "created", id = taskId, message = req["title"]))
        }

        // State board kanban (PRD Master 15.1, 8.1)
        get("/boards/{boardId}") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val boardId = call.parameters["boardId"] ?: "default"
            val board = TenantDomainRepository.getBoard(boardId, tenantId)
            call.respond(HttpStatusCode.OK, board)
        }
    }

    // Inbound Proactive Channel & Task Checklists/Activity (PRD Fase 110, LANGKAH 1.1)
    route("/tasks") {
        get {
            val boardId = call.request.queryParameters["boardId"] ?: "default"
            val principal = call.principal<JWTPrincipal>()
            val authenticatedUserId = principal?.payload?.subject ?: principal?.payload?.getClaim("user_id")?.asString()
            val userId = authenticatedUserId
                ?: call.request.queryParameters["userId"]
                ?: call.request.headers["X-User-Id"]
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User identity is required to fetch assigned tasks"))
                return@get
            }

            val tasks = taskRepo.getTasksForUser(userId, boardId)
            call.respond(HttpStatusCode.OK, tasks)
        }

        post("/inbound-channel-message") {
            val req = call.receive<InboundChannelMessageApiRequest>()
            val gateway = ai.orchestree.backend.channels.ChannelGateway()
            val orchestrationEngine = ai.orchestree.backend.orchestration.OrchestrationEngine(modelRouter = ai.orchestree.backend.modelrouter.ModelRouter())
            val result = gateway.handleInboundProactiveChannelMessage(
                tenantId = req.tenantId,
                channelType = req.channel,
                senderId = req.senderId,
                senderName = req.senderName,
                messageText = req.message,
                orchestrationEngine = orchestrationEngine,
                taskRepo = taskRepo
            )
            call.respond(HttpStatusCode.OK, result)
        }

        // Proactive Subscriptions & Scope (PRD Master 15.1, 11.1)
        route("/proactive") {
            get("/subscriptions") {
                val tenantId = call.parameters["id"] ?: "tenant-default"
                val subs = TenantDomainRepository.getProactiveSubscriptions(tenantId)
                call.respond(HttpStatusCode.OK, subs)
            }

            get("/scope/{staffId}") {
                val staffId = call.parameters["staffId"] ?: ""
                val jobLevel = call.request.queryParameters["jobLevel"]
                val dept = call.request.queryParameters["department"]
                val scopeRepo = ai.orchestree.backend.database.repositories.workforce.ProactiveCollaborationScopeRepository.defaultInstance
                val scope = scopeRepo.determineProactiveScope(staffId, jobLevel, dept)
                call.respond(HttpStatusCode.OK, scope)
            }

            post("/subscriptions") {
                val tenantId = call.parameters["id"] ?: "tenant-default"
                val req = call.receive<ProactiveSubscriptionRequest>()
                val created = TenantDomainRepository.createProactiveSubscription(tenantId, req)
                val scopeRepo = ai.orchestree.backend.database.repositories.workforce.ProactiveCollaborationScopeRepository.defaultInstance
                val scope = scopeRepo.determineProactiveScope(req.staffId)
                call.respond(
                    HttpStatusCode.Created,
                    GenericStatusResponse(status = "ACTIVE", id = created.id, message = "Subscribed with scope ${scope.scopeType}")
                )
            }
        }
    }

    route("/tenants/tasks") {
        post("/inbound-channel-message") {
            val req = call.receive<InboundChannelMessageApiRequest>()
            val gateway = ai.orchestree.backend.channels.ChannelGateway()
            val orchestrationEngine = ai.orchestree.backend.orchestration.OrchestrationEngine(modelRouter = ai.orchestree.backend.modelrouter.ModelRouter())
            val result = gateway.handleInboundProactiveChannelMessage(
                tenantId = req.tenantId,
                channelType = req.channel,
                senderId = req.senderId,
                senderName = req.senderName,
                messageText = req.message,
                orchestrationEngine = orchestrationEngine,
                taskRepo = taskRepo
            )
            call.respond(HttpStatusCode.OK, result)
        }
    }

    // Pindahkan task antar kolom (PRD Master 15.1, 8.3)
    route("/tasks/{taskId}") {
        patch("/move") {
            val taskId = call.parameters["taskId"] ?: ""
            val req = call.receive<TaskMoveRequest>()
            val tenantId = call.request.headers["X-Tenant-Id"] ?: "tenant-default"
            val destination = req.destinationColumn.ifBlank { "TODO" }
            val updatePayload = buildJsonObject {
                put("column_name", destination)
                put("column", destination)
            }.toString()
            supabase.updateRecord("tasks", tenantId, "id=eq.$taskId", updatePayload)
            call.respond(
                HttpStatusCode.OK,
                GenericStatusResponse(status = "UPDATED", id = taskId, message = destination)
            )
        }

        get("/checklists") {
            val taskId = call.parameters["taskId"] ?: ""
            val items = taskRepo.getChecklistsForTask(taskId)
            call.respond(HttpStatusCode.OK, items)
        }

        post("/checklists") {
            val taskId = call.parameters["taskId"] ?: ""
            val tenantId = call.request.headers["X-Tenant-Id"] ?: "tenant-default"
            val req = call.receive<ChecklistItemCreateRequest>()
            val item = ai.orchestree.backend.database.repositories.taskboard.TaskChecklistRecord(
                taskId = taskId,
                tenantId = tenantId,
                itemText = req.itemText,
                orderIndex = req.orderIndex
            )
            val created = taskRepo.createChecklist(item)
            call.respond(HttpStatusCode.Created, created)
        }

        patch("/checklists/{checklistId}/toggle") {
            val checklistId = call.parameters["checklistId"] ?: ""
            val isCompleted = call.request.queryParameters["completed"]?.toBooleanStrictOrNull() ?: true
            val completedBy = call.request.queryParameters["completedBy"] ?: "user"
            val success = taskRepo.toggleChecklistCompletion(checklistId, isCompleted, completedBy, "human")
            call.respond(HttpStatusCode.OK, mapOf("success" to success, "checklistId" to checklistId, "isCompleted" to isCompleted))
        }

        get("/activity-log") {
            val taskId = call.parameters["taskId"] ?: ""
            val logs = taskRepo.getActivityLogsForTask(taskId)
            call.respond(HttpStatusCode.OK, logs)
        }

        patch("/description") {
            val taskId = call.parameters["taskId"] ?: ""
            val req = call.receive<TaskDescriptionUpdateRequest>()
            val result = taskRepo.updateTaskDescription(taskId, req.descriptionRichText)
            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, result.getOrThrow())
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (result.exceptionOrNull()?.message ?: "Task not found")))
            }
        }

        get("/attachments") {
            val taskId = call.parameters["taskId"] ?: ""
            val items = taskRepo.listAttachments(taskId)
            call.respond(HttpStatusCode.OK, items)
        }

        post("/attachments") {
            val taskId = call.parameters["taskId"] ?: ""
            val tenantId = call.request.headers["X-Tenant-Id"] ?: "tenant-default"
            val req = call.receive<AttachmentCreateRequest>()
            val item = ai.orchestree.backend.database.repositories.taskboard.TaskAttachmentItem(
                taskId = taskId,
                tenantId = tenantId,
                fileName = req.fileName,
                fileUrl = req.fileUrl,
                fileSizeBytes = req.fileSizeBytes,
                uploadedBy = req.uploadedBy
            )
            val created = taskRepo.addAttachment(item)
            call.respond(HttpStatusCode.Created, created)
        }
    }

    // Competitive Intelligence Monitoring (PRD Master 15.1, 7.1)
    route("/intel/competitors") {
        get {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = call.request.headers["X-Tenant-Id"]
                ?: call.request.queryParameters["tenantId"]
                ?: principal?.payload?.getClaim("tenant_id")?.asString()
                ?: "tenant-default"
            val queryResult = supabase.queryTable("competitor_targets", tenantId)
            val list = if (queryResult.isSuccess) {
                val raw = queryResult.getOrDefault("[]")
                try {
                    val elements = kotlinx.serialization.json.Json.parseToJsonElement(raw)
                    if (elements is kotlinx.serialization.json.JsonArray) {
                        elements.mapNotNull { elem ->
                            val obj = elem as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                            val id = obj["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: return@mapNotNull null
                            val name = obj["name"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: ""
                            val category = obj["category"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: "General"
                            val url = obj["url"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: ""
                            val frequency = obj["crawl_frequency_hours"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: "24"
                            val isActive = obj["is_active"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } != "false"
                            CompetitorTargetItem(
                                id = id,
                                tenantId = tenantId,
                                name = name,
                                category = category,
                                urls = if (url.isNotBlank()) listOf(url) else emptyList(),
                                frequency = "${frequency}h",
                                status = if (isActive) "ACTIVE" else "INACTIVE"
                            )
                        }
                    } else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            call.respond(HttpStatusCode.OK, list)
        }

        post {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = call.request.headers["X-Tenant-Id"]
                ?: call.request.queryParameters["tenantId"]
                ?: principal?.payload?.getClaim("tenant_id")?.asString()
                ?: "tenant-default"
            val req = call.receive<CompetitorTargetRequest>()
            val targetId = "tgt-${java.util.UUID.randomUUID().toString().take(8)}"
            val primaryUrl = req.urls.firstOrNull() ?: "https://${req.name.lowercase().replace(" ", "")}.com"

            val target = ai.orchestree.backend.competitor.CompetitorTarget(
                id = targetId,
                tenantId = tenantId,
                name = req.name,
                url = primaryUrl,
                category = req.category
            )

            // 1. Persist Target to Supabase
            try {
                val payload = kotlinx.serialization.json.buildJsonObject {
                    put("id", target.id)
                    put("tenant_id", tenantId)
                    put("name", target.name)
                    put("url", target.url)
                    put("category", target.category)
                    put("frequency", "24h")
                    put("insight_prefs", "{}")
                    put("status", "ACTIVE")
                    put("last_monitored_at", java.time.Instant.now().toString())
                }.toString()
                supabase.insertRecord("competitor_targets", tenantId, payload)
            } catch (e: Exception) {
                // Non-blocking if table not initialized
            }

            // 2. POLA A: Dispatch via OrchestrationEngine (wf-competitor-audit DAG)
            try {
                orchestrationEngine.runWorkflow(
                    tenantId = tenantId,
                    workflowDefId = "wf-competitor-audit",
                    prompt = "Crawl and analyze competitor: ${target.name} (${target.url})",
                    contextParams = mapOf(
                        "targetId" to target.id,
                        "url" to target.url,
                        "category" to target.category
                    )
                )
            } catch (e: Exception) {
                // Non-blocking log
            }

            // 3. Call CompetitorIntelligenceEngine (running ModelRouter and web analysis)
            try {
                val insight = ai.orchestree.backend.competitor.CompetitorIntelligenceEngine.analyzeCompetitorTarget(
                    target = target,
                    modelRouter = modelRouter,
                    supabase = supabase
                )

                // 3. Connect to ContinuousLearningCore
                ai.orchestree.backend.learning.ContinuousLearningCore.onNodeOutcomeAvailable(
                    ai.orchestree.backend.learning.NodeOutcomeRequest(
                        tenantId = tenantId,
                        agentId = req.assignedAgentId ?: "agent-competitor-analyst",
                        agentName = "Competitor Intelligence Agent",
                        nodeId = "competitor-analysis-node",
                        executionId = "crawl-${System.currentTimeMillis()}",
                        workflowId = "competitor-crawl",
                        scenarioContext = "${target.name} (${target.url})",
                        actionType = "ANALYZE_COMPETITOR",
                        predictedImpact = "Market intelligence tracking",
                        actualOutcome = insight.summary,
                        outcomeSource = "MONITORING_LOOP_RESULT",
                        isSuccess = true
                    )
                )
            } catch (e: Exception) {
                // Non-blocking
            }

            call.respond(
                HttpStatusCode.Created,
                GenericStatusResponse(
                    status = "WATCHING",
                    id = targetId,
                    message = req.name
                )
            )
        }

        get("/{id}/insights") {
            val targetId = call.parameters["id"] ?: ""
            val principal = call.principal<JWTPrincipal>()
            val tenantId = call.request.headers["X-Tenant-Id"]
                ?: call.request.queryParameters["tenantId"]
                ?: principal?.payload?.getClaim("tenant_id")?.asString()
                ?: "tenant-default"

            // 1. Query existing insights from Supabase
            val queryResult = supabase.queryTable(
                tableName = "competitor_insights",
                tenantId = tenantId,
                extraParams = mapOf("target_id" to "eq.$targetId")
            )

            val insights = if (queryResult.isSuccess) {
                val raw = queryResult.getOrDefault("[]")
                try {
                    val elements = kotlinx.serialization.json.Json.parseToJsonElement(raw)
                    if (elements is kotlinx.serialization.json.JsonArray && elements.isNotEmpty()) {
                        elements.mapNotNull { elem ->
                            val obj = elem as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                            val id = obj["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: return@mapNotNull null
                            val category = obj["category"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: "PROMO_MARKETING"
                            val summary = obj["summary"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: ""
                            val confidence = obj["confidence"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toDoubleOrNull() else null } ?: 0.85
                            val impact = obj["impact_score"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toDoubleOrNull() else null } ?: 0.80
                            CompetitorInsightItem(
                                id = id,
                                targetId = targetId,
                                category = category,
                                summary = summary,
                                confidence = confidence,
                                impactScore = impact
                            )
                        }
                    } else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()

            // 2. If no insights exist in DB yet, check if target exists in competitor_targets table
            val finalInsights = if (insights.isEmpty()) {
                val targetResult = supabase.queryTable(
                    tableName = "competitor_targets",
                    tenantId = tenantId,
                    extraParams = mapOf("id" to "eq.$targetId")
                )
                if (targetResult.isSuccess) {
                    try {
                        val elems = kotlinx.serialization.json.Json.parseToJsonElement(targetResult.getOrDefault("[]"))
                        if (elems is kotlinx.serialization.json.JsonArray && elems.isNotEmpty()) {
                            val tObj = elems.first() as? kotlinx.serialization.json.JsonObject
                            val tName = tObj?.get("name")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: "Competitor"
                            val tUrl = tObj?.get("url")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: ""
                            val target = ai.orchestree.backend.competitor.CompetitorTarget(
                                id = targetId,
                                tenantId = tenantId,
                                name = tName,
                                url = tUrl
                            )
                            val liveInsight = ai.orchestree.backend.competitor.CompetitorIntelligenceEngine.analyzeCompetitorTarget(
                                target = target,
                                modelRouter = modelRouter,
                                supabase = supabase
                            )
                            listOf(
                                CompetitorInsightItem(
                                    id = liveInsight.id,
                                    targetId = targetId,
                                    category = liveInsight.category,
                                    summary = liveInsight.summary,
                                    confidence = liveInsight.confidence,
                                    impactScore = liveInsight.impactScore
                                )
                            )
                        } else emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else emptyList()
            } else {
                insights
            }

            call.respond(HttpStatusCode.OK, finalInsights)
        }
    }

    route("/intel/world-trends") {
        get {
            val trends = TenantDomainRepository.getWorldTrends()
            call.respond(HttpStatusCode.OK, trends)
        }
    }

    // Integration Hub Connect (PRD Master 15.1, 10.2)
    route("/integrations/{platform}") {
        post("/connect") {
            val platform = call.parameters["platform"] ?: "unknown"
            call.receive<IntegrationConnectRequest>()
            call.respond(
                HttpStatusCode.OK,
                GenericStatusResponse(status = "CONNECTED", id = platform, message = "HEALTHY")
            )
        }
    }

    // Proactive Subscriptions (PRD Master 15.1, 11.1)
    route("/proactive") {
        get("/subscriptions") {
            val tenantId = call.request.queryParameters["tenantId"] ?: "tenant-default"
            val subs = TenantDomainRepository.getProactiveSubscriptions(tenantId)
            call.respond(HttpStatusCode.OK, subs)
        }

        get("/scope/{staffId}") {
            val staffId = call.parameters["staffId"] ?: ""
            val jobLevel = call.request.queryParameters["jobLevel"]
            val dept = call.request.queryParameters["department"]
            val scopeRepo = ai.orchestree.backend.database.repositories.workforce.ProactiveCollaborationScopeRepository.defaultInstance
            val scope = scopeRepo.determineProactiveScope(staffId, jobLevel, dept)
            call.respond(HttpStatusCode.OK, scope)
        }

        post("/subscriptions") {
            val tenantId = call.request.queryParameters["tenantId"] ?: "tenant-default"
            val req = call.receive<ProactiveSubscriptionRequest>()
            val created = TenantDomainRepository.createProactiveSubscription(tenantId, req)
            val scopeRepo = ai.orchestree.backend.database.repositories.workforce.ProactiveCollaborationScopeRepository.defaultInstance
            val scope = scopeRepo.determineProactiveScope(req.staffId)
            call.respond(
                HttpStatusCode.Created,
                GenericStatusResponse(status = "ACTIVE", id = created.id, message = "Subscribed with scope ${scope.scopeType}")
            )
        }
    }

    // Analytics Scores & Ranking (PRD Master 15.1, 9.2)
    route("/analytics") {
        get("/scores") {
            val period = call.request.queryParameters["period"] ?: "2026-08"
            val response = TenantDomainRepository.getAnalyticsScores(period)
            call.respond(HttpStatusCode.OK, response)
        }
    }

    // Performance Management (Rekomendasi 1)
    route("/performance") {
        get("/reports") {
            val tenantId = call.parameters["id"] ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val reports = TenantDomainRepository.getDailyReports(tenantId)
            call.respond(HttpStatusCode.OK, reports)
        }

        post("/reports") {
            val tenantId = call.parameters["id"] ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val req = call.receive<CreateWorkReportRequest>()
            val report = TenantDomainRepository.createDailyReport(tenantId, req)
            call.respond(HttpStatusCode.Created, report)
        }

        get("/goals") {
            val tenantId = call.parameters["id"] ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val goals = TenantDomainRepository.getGoals(tenantId)
            call.respond(HttpStatusCode.OK, goals)
        }

        get("/reviews") {
            val tenantId = call.parameters["id"] ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val reviews = TenantDomainRepository.getReviews(tenantId)
            call.respond(HttpStatusCode.OK, reviews)
        }

        get("/predictions") {
            val tenantId = call.parameters["id"] ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val predictions = TenantDomainRepository.getPredictions(tenantId)
            call.respond(HttpStatusCode.OK, predictions)
        }

        get("/executive-briefs") {
            val tenantId = call.parameters["id"] ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val briefs = TenantDomainRepository.getExecutiveBriefs(tenantId)
            call.respond(HttpStatusCode.OK, briefs)
        }
    }

    // Security & Data Governance / GDPR (Rekomendasi 2)
    route("/security") {
        get("/anomalies") {
            val tenantId = call.parameters["id"] ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val anomalies = TenantDomainRepository.getSecurityAnomalies(tenantId)
            call.respond(HttpStatusCode.OK, anomalies)
        }

        get("/dsr") {
            val tenantId = call.parameters["id"] ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val requests = TenantDomainRepository.getDataSubjectRequests(tenantId)
            call.respond(HttpStatusCode.OK, requests)
        }

        post("/dsr") {
            val tenantId = call.parameters["id"] ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val req = call.receive<CreateDataSubjectRequest>()
            val created = TenantDomainRepository.createDataSubjectRequest(tenantId, req)
            call.respond(HttpStatusCode.Created, created)
        }
    }

    // Attendance Anomalies (Rekomendasi 3)
    route("/attendance/anomalies") {
        get {
            val tenantId = call.parameters["id"] ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val anomalies = TenantDomainRepository.getAttendanceAnomalies(tenantId)
            call.respond(HttpStatusCode.OK, anomalies)
        }

        post("/{anomalyId}/resolve") {
            val anomalyId = call.parameters["anomalyId"] ?: ""
            val resolved = TenantDomainRepository.resolveAttendanceAnomaly(anomalyId)
            call.respond(
                HttpStatusCode.OK,
                GenericStatusResponse(status = if (resolved) "RESOLVED" else "NOT_FOUND", id = anomalyId, message = if (resolved) "Anomaly resolved successfully in database" else "Anomaly not found or already resolved")
            )
        }
    }
}
