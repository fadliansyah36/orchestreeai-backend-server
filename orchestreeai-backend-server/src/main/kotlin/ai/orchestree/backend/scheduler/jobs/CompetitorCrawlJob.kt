package ai.orchestree.backend.scheduler.jobs

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.NodeExecutionResult
import ai.orchestree.backend.orchestration.NodeExecutionStatus
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.orchestration.WorkflowExecution
import ai.orchestree.backend.resilience.executeWithRetry
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID

class CompetitorCrawlJob(
    private val modelRouter: ModelRouter = ModelRouter(),
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val orchestrationEngine: OrchestrationEngine = OrchestrationEngine()
) {
    private val logger = LoggerFactory.getLogger(CompetitorCrawlJob::class.java)

    suspend fun execute(tenantId: String, competitorUrl: String = "https://example.com"): String {
        logger.info("Executing competitor intelligence crawl for tenant $tenantId on $competitorUrl")

        // FASE 110: 1. Buat / perbarui self-initiated task di Kanban Board
        val execId = "exec-crawl-${UUID.randomUUID().toString().take(8)}"
        val task = orchestrationEngine.createOrUpdateAiSelfTask(
            agentId = "agent-radar-competitor",
            taskTitle = "Crawl & Audit Radar Intelijen Kompetitor: ${competitorUrl.substringAfter("://").substringBefore("/")}",
            statusLine = "Memulai crawling sinyal harga dan promo kompetitor di $competitorUrl",
            monitoringTarget = competitorUrl,
            checklists = listOf(
                "Fetch sinyal harga dan diskon kompetitor",
                "Ekstrak diff pergeseran strategi pemasaran",
                "Sintesis rekomendasi tindakan taktis",
                "Publish insight ke Supabase Realtime"
            ),
            workflowExecutionId = execId,
            tenantId = tenantId
        )

        val compName = competitorUrl.substringAfter("://").substringBefore("/")
        val target = ai.orchestree.backend.competitor.CompetitorTarget(
            id = "tgt-${UUID.randomUUID().toString().take(8)}",
            tenantId = tenantId,
            name = compName,
            url = competitorUrl
        )

        val insight = executeWithRetry(
            maxAttempts = 3,
            initialDelayMs = 500L,
            backoffMultiplier = 2.0
        ) {
            ai.orchestree.backend.competitor.CompetitorIntelligenceEngine.analyzeCompetitorTarget(
                target = target,
                modelRouter = modelRouter,
                supabase = supabase
            )
        }

        val summaryText = "${insight.summary} (Importance: ${insight.importance}, Action: ${insight.recommendedAction})"
        val insightId = insight.id
        logger.info("Successfully completed crawl and persisted competitor insight $insightId for tenant $tenantId")

        // FASE 110: 2. Selesaikan workflow node agar task otomatis pindah dari IN_PROGRESS ke DONE
        val execution = WorkflowExecution(
            id = execId,
            tenantId = tenantId,
            workflowDefId = "wf-competitor-audit"
        ).apply {
            context["taskId"] = task.id
            context["finalOutput"] = summaryText
        }

        orchestrationEngine.onWorkflowNodeCompleted(
            execution = execution,
            nodeId = "n3-deliver",
            nodeResult = NodeExecutionResult(
                status = NodeExecutionStatus.SUCCESS,
                output = "Crawling & sintesis selesai. Insight $insightId diterbitkan."
            )
        )

        return summaryText
    }
}

