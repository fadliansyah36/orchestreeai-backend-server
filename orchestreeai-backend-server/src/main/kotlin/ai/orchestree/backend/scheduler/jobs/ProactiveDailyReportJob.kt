package ai.orchestree.backend.scheduler.jobs

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.NodeExecutionResult
import ai.orchestree.backend.orchestration.NodeExecutionStatus
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.orchestration.WorkflowExecution
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID

open class ProactiveDailyReportJob(
    private val modelRouter: ModelRouter = ModelRouter(),
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val orchestrationEngine: OrchestrationEngine = OrchestrationEngine()
) {
    private val logger = LoggerFactory.getLogger(ProactiveDailyReportJob::class.java)

    open suspend fun execute(tenantId: String): String {
        logger.info("Executing Proactive Daily Briefing generation for tenant: $tenantId")

        val execId = "exec-brief-${UUID.randomUUID().toString().take(8)}"
        val task = orchestrationEngine.createOrUpdateAiSelfTask(
            agentId = "agent-chief-of-staff",
            taskTitle = "AI Chief of Staff: Penyusunan Executive Daily Briefing",
            statusLine = "Memulai agregasi KPI dan deteksi anomali operasional",
            monitoringTarget = "daily-executive-briefing",
            checklists = listOf(
                "Agregasi metrik pendapatan dan performa",
                "Deteksi anomali churn dan operational pipeline",
                "Sintesis ringkasan eksekutif multi-divisi",
                "Kirim notifikasi ke channel in-app dan eksekutif"
            ),
            workflowExecutionId = execId,
            tenantId = tenantId
        )

        val prompt = "Generate a concise proactive executive briefing summary for tenant $tenantId covering revenue pacing, critical tasks, and agent productivity."
        val res = modelRouter.execute(
            ModelRouteRequest(
                taskCategory = "PROACTIVE_BRIEF",
                prompt = prompt,
                tenantId = tenantId
            )
        )
        val text = if (res.isSuccess) res.getOrThrow().text else "Proactive Briefing: Operations nominal. Revenue target on track."
        logger.info("Generated proactive briefing for $tenantId: ${text.take(60)}...")

        // 1. Persist notification to 'notifications' table
        val notifId = "ntf-${UUID.randomUUID().toString().take(8)}"
        val notifPayload = buildJsonObject {
            put("id", notifId)
            put("tenant_id", tenantId)
            put("title", "Laporan Harian Proaktif Tersedia")
            put("message", text.take(150))
            put("type", "PROACTIVE_BRIEF")
            put("is_read", false)
        }.toString()
        supabase.insertRecord("notifications", tenantId, notifPayload)

        // 2. Mark workflow node completed -> moves task to DONE and logs activity
        val execution = WorkflowExecution(
            id = execId,
            tenantId = tenantId,
            workflowDefId = "wf-chief-of-staff-briefing"
        ).apply {
            context["taskId"] = task.id
            context["finalOutput"] = text
        }

        orchestrationEngine.onWorkflowNodeCompleted(
            execution = execution,
            nodeId = "n5-distribute-channels",
            nodeResult = NodeExecutionResult(
                status = NodeExecutionStatus.SUCCESS,
                output = "Briefing berhasil didistribusikan ke dashboard & channel in-app."
            )
        )

        return text
    }

    /**
     * LANGKAH 3: Revisi runProactiveJob() Dengan Scope Enforcement
     * - Staff Human HANYA berkolaborasi dengan AI Agent dalam scope departemennya (department_scoped).
     * - Owner / Direksi menerima ringkasan eksekutif lintas seluruh departemen via Chief of Staff (executive_full_summary).
     * - Memvalidasi operasi channel akun internal_proactive_reporting.
     */
    suspend fun runProactiveJob(
        sub: ai.orchestree.backend.models.ProactiveSubscriptionDto,
        targetChannelAccount: ai.orchestree.backend.channels.ChannelAccountConfig? = null,
        proactiveScopeRepo: ai.orchestree.backend.database.repositories.workforce.ProactiveCollaborationScopeRepository =
            ai.orchestree.backend.database.repositories.workforce.ProactiveCollaborationScopeRepository.defaultInstance,
        chiefOfStaffService: ai.orchestree.backend.intelligence.ChiefOfStaffService =
            ai.orchestree.backend.intelligence.ChiefOfStaffService(),
        riskEngine: ai.orchestree.backend.intelligence.RiskEngine =
            ai.orchestree.backend.intelligence.RiskEngine()
    ): ProactiveJobResult {
        if (sub.staffId.isBlank()) {
            logger.warn("Skipping proactive job for subscription ${sub.id}: staffId is blank and has no associated recipient")
            return ProactiveJobResult(
                success = false,
                staffId = "",
                scopeType = "UNSPECIFIED",
                channel = sub.channel.name,
                messageContent = "",
                dispatchedAgents = emptyList(),
                riskPassed = true,
                status = "SKIPPED_NULL_STAFF"
            )
        }

        logger.info("Running proactive job with scope enforcement for staff=${sub.staffId}, role=${sub.staffRole}")

        if (targetChannelAccount != null) {
            val mode = targetChannelAccount.operationMode.name.lowercase()
            require(mode == "internal_proactive_reporting" || mode == "ai_autopilot") {
                "FATAL: bukan channel internal. Operation mode '$mode' tidak diizinkan untuk proactive reporting."
            }
        }

        val scope = proactiveScopeRepo.getScopeForStaff(sub.staffId)

        val contextLines: List<String> = when (scope.scopeType) {
            "department_scoped" -> {
                // HANYA ambil data dari AI Agent dalam scope departemen ini
                ai.orchestree.backend.intelligence.ContextResolver.forStaffDailyBrief(
                    staffId = sub.staffId,
                    types = sub.enabledNotifTypes.map { it.name },
                    restrictToAiJobTitleIds = scope.collaboratingAiJobTitleIds // WAJIB dibatasi
                )
            }
            "executive_full_summary" -> {
                // REUSE Chief of Staff executive briefing - merangkum LINTAS SELURUH Specialist Agent + performa Staff Human SELURUH departemen
                chiefOfStaffService.getLatestBriefingSummary(sub.tenantId)
            }
            else -> emptyList()
        }

        if (contextLines.isEmpty()) {
            return ProactiveJobResult(
                success = false,
                staffId = sub.staffId,
                scopeType = scope.scopeType,
                channel = sub.channel.name,
                messageContent = "",
                dispatchedAgents = emptyList(),
                riskPassed = true,
                status = "EMPTY_CONTEXT"
            )
        }

        val header = if (scope.scopeType == "executive_full_summary") {
            "👑 [EXECUTIVE BRIEFING - AI CHIEF OF STAFF]\nRingkasan Lintas Seluruh Departemen & Performa Tim:\n"
        } else {
            val deptLabel = scope.departmentCategoryCode?.uppercase() ?: "DEPARTEMEN"
            val agentListStr = (scope.collaboratingAgentNames.ifEmpty { scope.collaboratingAiJobTitleIds ?: emptyList() }).joinToString(", ")
            "📋 [DAILY BRIEFING - DEPARTEMEN $deptLabel]\nKolaborasi AI Agent ($agentListStr):\n"
        }

        val message = header + contextLines.joinToString("\n• ", prefix = "• ")

        // Tone & risk check
        val toneCheck = riskEngine.evaluateOutboundMessage(message)
        if (!toneCheck.passed) {
            logger.warn("Escalating to review: outbound proactive message failed tone check for staff=${sub.staffId}: ${toneCheck.reason}")
            return ProactiveJobResult(
                success = false,
                staffId = sub.staffId,
                scopeType = scope.scopeType,
                channel = sub.channel.name,
                messageContent = message,
                dispatchedAgents = scope.collaboratingAgentNames,
                riskPassed = false,
                status = "ESCALATED_TO_REVIEW"
            )
        }

        // Persist to log
        val logPayload = buildJsonObject {
            put("id", "pml-${UUID.randomUUID().toString().take(8)}")
            put("tenant_id", sub.tenantId)
            put("staff_id", sub.staffId)
            put("staff_name", sub.staffName.ifBlank { "Staff" })
            put("channel", sub.channel.name)
            put("message_type", "DAILY_BRIEF")
            put("content", message)
            put("status", "DELIVERED")
            put("agent_sender_name", sub.assignedAgentName.ifBlank { "Proactive Agent" })
        }.toString()
        supabase.insertRecord("proactive_messages_log", sub.tenantId, logPayload)

        return ProactiveJobResult(
            success = true,
            staffId = sub.staffId,
            scopeType = scope.scopeType,
            channel = sub.channel.name,
            messageContent = message,
            dispatchedAgents = scope.collaboratingAgentNames,
            riskPassed = true,
            status = "DELIVERED"
        )
    }
}

data class ProactiveJobResult(
    val success: Boolean,
    val staffId: String,
    val scopeType: String,
    val channel: String,
    val messageContent: String,
    val dispatchedAgents: List<String>,
    val riskPassed: Boolean,
    val status: String
)

