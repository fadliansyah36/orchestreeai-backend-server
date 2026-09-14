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

    suspend fun execute(tenantId: String, competitorUrl: String? = null): String {
        logger.info("Executing competitor intelligence crawl for tenant $tenantId (override url: $competitorUrl)")

        val targetsToCrawl = mutableListOf<ai.orchestree.backend.competitor.CompetitorTarget>()
        if (!competitorUrl.isNullOrBlank()) {
            val compName = competitorUrl.substringAfter("://").substringBefore("/")
            targetsToCrawl.add(
                ai.orchestree.backend.competitor.CompetitorTarget(
                    id = "tgt-${UUID.randomUUID().toString().take(8)}",
                    tenantId = tenantId,
                    name = compName,
                    url = competitorUrl
                )
            )
        } else {
            // Fetch registered targets from Supabase
            val queryResult = supabase.queryTable("competitor_targets", tenantId)
            if (queryResult.isSuccess) {
                try {
                    val elements = kotlinx.serialization.json.Json.parseToJsonElement(queryResult.getOrDefault("[]"))
                    if (elements is kotlinx.serialization.json.JsonArray) {
                        elements.forEach { elem ->
                            val obj = elem as? kotlinx.serialization.json.JsonObject ?: return@forEach
                            val id = obj["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: return@forEach
                            val name = obj["name"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: ""
                            val url = obj["url"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } ?: ""
                            val isActive = obj["is_active"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null } != "false"
                            if (url.isNotBlank() && isActive) {
                                targetsToCrawl.add(
                                    ai.orchestree.backend.competitor.CompetitorTarget(
                                        id = id,
                                        tenantId = tenantId,
                                        name = name.ifBlank { url.substringAfter("://").substringBefore("/") },
                                        url = url
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse competitor targets: ${e.message}")
                }
            }
        }

        if (targetsToCrawl.isEmpty()) {
            val defaultUrl = "https://example.com"
            targetsToCrawl.add(
                ai.orchestree.backend.competitor.CompetitorTarget(
                    id = "tgt-default",
                    tenantId = tenantId,
                    name = "General Competitor",
                    url = defaultUrl
                )
            )
        }

        val summaries = mutableListOf<String>()
        for (target in targetsToCrawl) {
            // FASE 110: 1. Buat / perbarui self-initiated task di Kanban Board
            val execId = "exec-crawl-${UUID.randomUUID().toString().take(8)}"
            val task = orchestrationEngine.createOrUpdateAiSelfTask(
                agentId = "agent-radar-competitor",
                taskTitle = "Crawl & Audit Radar Intelijen Kompetitor: ${target.name}",
                statusLine = "Memulai crawling sinyal harga dan promo kompetitor di ${target.url}",
                monitoringTarget = target.url,
                checklists = listOf(
                    "Fetch sinyal harga dan diskon kompetitor",
                    "Ekstrak diff pergeseran strategi pemasaran",
                    "Sintesis rekomendasi tindakan taktis",
                    "Publish insight ke Supabase Realtime"
                ),
                workflowExecutionId = execId,
                tenantId = tenantId
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
            summaries.add(summaryText)
            val insightId = insight.id
            logger.info("Successfully completed crawl and persisted competitor insight $insightId for tenant $tenantId on ${target.url}")

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
        }

        return summaries.joinToString("; ")
    }
}

