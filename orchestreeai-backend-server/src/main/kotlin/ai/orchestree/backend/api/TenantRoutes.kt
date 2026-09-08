package ai.orchestree.backend.api

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.identity.TenantRepository
import ai.orchestree.backend.database.repositories.taskboard.TaskRepository
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

    route("/tenants/{id}") {
        // Overview dashboard tenant (PRD Master 15.1)
        get("/dashboard/overview") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                TenantDashboardOverviewResponse(
                    tenantId = tenantId,
                    activeAgents = 15,
                    humanStaffCount = 42,
                    activeTasks = 128,
                    completionRate = 94.5,
                    status = "HEALTHY"
                )
            )
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
            call.respond(
                HttpStatusCode.OK,
                BoardKanbanResponse(
                    boardId = boardId,
                    tenantId = tenantId,
                    columns = listOf("BACKLOG", "TODO", "IN_PROGRESS", "REVIEW", "DONE")
                )
            )
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
            val result = gateway.handleInboundProactiveChannelMessage(
                tenantId = req.tenantId,
                channelType = req.channel,
                senderId = req.senderId,
                senderName = req.senderName,
                messageText = req.message,
                taskRepo = taskRepo
            )
            call.respond(HttpStatusCode.OK, result)
        }
    }

    route("/tenants/tasks") {
        post("/inbound-channel-message") {
            val req = call.receive<InboundChannelMessageApiRequest>()
            val gateway = ai.orchestree.backend.channels.ChannelGateway()
            val result = gateway.handleInboundProactiveChannelMessage(
                tenantId = req.tenantId,
                channelType = req.channel,
                senderId = req.senderId,
                senderName = req.senderName,
                messageText = req.message,
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
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    CompetitorTargetItem(
                        id = "tgt-comp-01",
                        tenantId = tenantId,
                        name = "Alpha Retail Tech",
                        category = "Retail Commerce",
                        urls = listOf("https://alpharetail.example.com"),
                        frequency = "daily",
                        status = "ACTIVE"
                    ),
                    CompetitorTargetItem(
                        id = "tgt-comp-02",
                        tenantId = tenantId,
                        name = "Beta Omnichannel Solution",
                        category = "Enterprise SaaS",
                        urls = listOf("https://betaomni.example.com"),
                        frequency = "daily",
                        status = "ACTIVE"
                    )
                )
            )
        }

        post {
            val req = call.receive<CompetitorTargetRequest>()
            call.respond(
                HttpStatusCode.Created,
                GenericStatusResponse(
                    status = "WATCHING",
                    id = "tgt-${java.util.UUID.randomUUID().toString().take(8)}",
                    message = req.name
                )
            )
        }

        get("/{id}/insights") {
            val targetId = call.parameters["id"] ?: ""
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    CompetitorInsightItem(
                        id = "ins-01",
                        targetId = targetId,
                        category = "PROMO_DISCOUNT",
                        summary = "Kompetitor meluncurkan promo flash sale diskon 30%",
                        confidence = 0.92,
                        impactScore = 0.85
                    )
                )
            )
        }
    }

    route("/intel/world-trends") {
        get {
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    WorldTrendClusterItem(
                        id = "trend-01",
                        title = "Adopsi AI Agent Autonomous dalam Ritel Regional",
                        summary = "Peningkatan 45% dalam adopsi otomasi CS berbasis LLM multimodal di Asia Tenggara kuartal ini.",
                        category = "TECHNOLOGY",
                        confidence = 0.94
                    ),
                    WorldTrendClusterItem(
                        id = "trend-02",
                        title = "Fluktuasi Pasokan Komponen & Tarif Logistik",
                        summary = "Biaya freight forwarder mengalami penyesuaian 8% menyusul regulasi kepabeanan lintas batas baru.",
                        category = "SUPPLY_CHAIN",
                        confidence = 0.88
                    )
                )
            )
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
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    ProactiveSubscriptionItem(
                        id = "sub-01",
                        tenantId = tenantId,
                        staffId = "staff-01",
                        channel = "WHATSAPP",
                        types = listOf("DAILY_BRIEF", "ANOMALY_ALERT"),
                        sendTimes = listOf("08:00", "17:00"),
                        status = "ACTIVE"
                    )
                )
            )
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
            val req = call.receive<ProactiveSubscriptionRequest>()
            val scopeRepo = ai.orchestree.backend.database.repositories.workforce.ProactiveCollaborationScopeRepository.defaultInstance
            val scope = scopeRepo.determineProactiveScope(req.staffId)
            call.respond(
                HttpStatusCode.Created,
                GenericStatusResponse(status = "ACTIVE", id = req.staffId, message = "Subscribed with scope ${scope.scopeType}")
            )
        }
    }

    // Analytics Scores & Ranking (PRD Master 15.1, 9.2)
    route("/analytics") {
        get("/scores") {
            val period = call.request.queryParameters["period"] ?: "2026-08"
            call.respond(
                HttpStatusCode.OK,
                AnalyticsScoreResponse(
                    period = period,
                    humanRanking = listOf(
                        RankingEntry(name = "Budi Santoso", score = 92.4, rank = 1),
                        RankingEntry(name = "Siti Rahma", score = 89.8, rank = 2)
                    ),
                    aiRanking = listOf(
                        RankingEntry(name = "CLOSER", score = 96.1, rank = 1),
                        RankingEntry(name = "SDR", score = 91.5, rank = 2)
                    )
                )
            )
        }
    }

    // Performance Management (Rekomendasi 1)
    route("/performance") {
        get("/reports") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    WorkReportDailyItem(
                        id = "wr-01",
                        tenantId = tenantId,
                        staffId = "staff-01",
                        staffName = "Budi Santoso",
                        reportDate = "2026-09-04",
                        accomplishments = "Menyelesaikan migrasi REST endpoint TenantRoutes dan integrasi BackendApiClient",
                        blockers = "None",
                        plannedNext = "Pengujian modul performa dan security"
                    )
                )
            )
        }

        post("/reports") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<CreateWorkReportRequest>()
            call.respond(
                HttpStatusCode.Created,
                WorkReportDailyItem(
                    id = "wr-${System.currentTimeMillis()}",
                    tenantId = tenantId,
                    staffId = req.staffId,
                    staffName = req.staffName,
                    reportDate = req.reportDate,
                    accomplishments = req.accomplishments,
                    blockers = req.blockers,
                    plannedNext = req.plannedNext
                )
            )
        }

        get("/goals") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    GoalKpiItem(
                        id = "kpi-01",
                        tenantId = tenantId,
                        title = "Q3 Revenue Target",
                        targetValue = 100000000.0,
                        currentValue = 84500000.0,
                        unit = "IDR",
                        period = "Q3-2026",
                        status = "ON_TRACK"
                    ),
                    GoalKpiItem(
                        id = "kpi-02",
                        tenantId = tenantId,
                        title = "Customer Satisfaction Score",
                        targetValue = 95.0,
                        currentValue = 92.5,
                        unit = "%",
                        period = "Q3-2026",
                        status = "ON_TRACK"
                    )
                )
            )
        }

        get("/reviews") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    PerformanceReviewItem(
                        id = "rev-01",
                        tenantId = tenantId,
                        staffId = "staff-01",
                        reviewerId = "mgr-01",
                        period = "Q2-2026",
                        overallScore = 91.2,
                        feedback = "Kinerja konsisten, kepemimpinan teknis sangat memuaskan.",
                        status = "FINALIZED"
                    )
                )
            )
        }

        get("/predictions") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    PerformanceRiskPredictionItem(
                        id = "pred-01",
                        tenantId = tenantId,
                        staffId = "staff-02",
                        staffName = "Siti Rahma",
                        riskLevel = "LOW",
                        riskScore = 12.5,
                        primaryFactor = "Beban kerja seimbang",
                        recommendation = "Pertahankan ritme kerja reguler"
                    )
                )
            )
        }

        get("/executive-briefs") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    ExecutiveBriefItem(
                        id = "eb-01",
                        tenantId = tenantId,
                        title = "Monthly Executive Performance Brief",
                        period = "August 2026",
                        executiveSummary = "Pencapaian KPI enterprise stabil pada 92% target, efisiensi operasional agen meningkat 18%.",
                        keyAchievements = listOf("Pencapaian penjualan produk utama 110%", "Adopsi AI workforce naik 40%"),
                        riskAreas = listOf("Kapasitas server selama flash deal")
                    )
                )
            )
        }
    }

    // Security & Data Governance / GDPR (Rekomendasi 2)
    route("/security") {
        get("/anomalies") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    SecurityAnomalyItem(
                        id = "sec-01",
                        tenantId = tenantId,
                        anomalyType = "IP_LOCATION_CHANGE",
                        severity = "MEDIUM",
                        description = "Percobaan login dari lokasi baru terdeteksi dan memerlukan verifikasi",
                        detectedAt = System.currentTimeMillis() - 7200000L
                    )
                )
            )
        }

        get("/dsr") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    DataSubjectRequestItem(
                        id = "dsr-01",
                        tenantId = tenantId,
                        requestType = "RIGHT_TO_ERASURE",
                        requesterEmail = "user@external.com",
                        status = "PENDING"
                    )
                )
            )
        }

        post("/dsr") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<CreateDataSubjectRequest>()
            call.respond(
                HttpStatusCode.Created,
                DataSubjectRequestItem(
                    id = "dsr-${System.currentTimeMillis()}",
                    tenantId = tenantId,
                    requestType = req.requestType,
                    requesterEmail = req.requesterEmail,
                    status = "PENDING"
                )
            )
        }
    }

    // Attendance Anomalies (Rekomendasi 3)
    route("/attendance/anomalies") {
        get {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    AttendanceAnomalyItem(
                        id = "anom-01",
                        tenantId = tenantId,
                        staffId = "staff-01",
                        staffName = "Budi Santoso",
                        anomalyType = "OUT_OF_GEOFENCE",
                        timestamp = System.currentTimeMillis() - 86400000L,
                        status = "RESOLVED",
                        resolutionNotes = "Tugas luar kota telah dikonfirmasi oleh Manager"
                    )
                )
            )
        }

        post("/{anomalyId}/resolve") {
            val anomalyId = call.parameters["anomalyId"] ?: ""
            call.respond(
                HttpStatusCode.OK,
                GenericStatusResponse(status = "RESOLVED", id = anomalyId, message = "Anomaly resolved successfully")
            )
        }
    }
}
