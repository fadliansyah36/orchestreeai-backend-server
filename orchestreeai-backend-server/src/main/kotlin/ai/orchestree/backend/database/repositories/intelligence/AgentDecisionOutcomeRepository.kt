package ai.orchestree.backend.database.repositories.intelligence

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.orchestration.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class AgentDecisionOutcomeRecord(
    val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val agentId: String = "agent-system",
    val agentName: String = "Orchestree AI Specialist",
    val nodeId: String = "node-decision",
    val executionId: String = "exec-default",
    val workflowId: String = "workflow-default",
    val scenarioContext: String = "Production decision execution",
    val actionType: String = "DECISION_ACTION",
    val predictedImpact: String = "High positive business impact",
    val actualOutcome: String = "REINFORCE",
    val outcomeSource: String = "HUMAN_EXPLICIT_APPROVAL",
    val outcomeClassification: String = "REINFORCE", // 'REINFORCE', 'CORRECT', 'LEARN_FROM_REJECTION'
    val verifiedBy: String = "SYSTEM_VALIDATOR",
    val confidence: Double = 85.0, // 0.0 - 100.0
    val confidenceDelta: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Direct outcome accessor matching prompt requirements
    val outcome: String get() = outcomeClassification
}

/**
 * Repository for storing and querying AI Agent Decision Outcomes (Closed-Loop Learning).
 * Ground truth feedback utilized for empirical confidence score calibration.
 */
class AgentDecisionOutcomeRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(AgentDecisionOutcomeRepository::class.java)
    private val inMemoryStore = ConcurrentHashMap<String, CopyOnWriteArrayList<AgentDecisionOutcomeRecord>>()

    suspend fun recordOutcome(record: AgentDecisionOutcomeRecord): Result<AgentDecisionOutcomeRecord> = withContext(Dispatchers.IO) {
        inMemoryStore.computeIfAbsent(record.tenantId) { CopyOnWriteArrayList() }.add(record)

        if (supabase.isConfigured()) {
            try {
                val payload = mapOf(
                    "id" to record.id,
                    "tenant_id" to record.tenantId,
                    "agent_id" to record.agentId,
                    "agent_name" to record.agentName,
                    "node_id" to record.nodeId,
                    "execution_id" to record.executionId,
                    "workflow_id" to record.workflowId,
                    "scenario_context" to record.scenarioContext,
                    "action_type" to record.actionType,
                    "predicted_impact" to record.predictedImpact,
                    "actual_outcome" to record.actualOutcome,
                    "outcome_source" to record.outcomeSource,
                    "outcome_classification" to record.outcomeClassification,
                    "verified_by" to record.verifiedBy,
                    "confidence" to record.confidence,
                    "confidence_delta" to record.confidenceDelta,
                    "created_at" to record.createdAt
                )
                supabase.insertRecord("agent_decision_outcomes", record.tenantId, payload.toJson())
            } catch (e: Exception) {
                logger.debug("Supabase insert outcome notice: ${e.message}")
            }
        }
        Result.success(record)
    }

    suspend fun getWithConfidenceLastMonth(tenantId: String): List<AgentDecisionOutcomeRecord> = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val records = inMemoryStore[tenantId]?.filter { it.createdAt >= thirtyDaysAgo } ?: emptyList()
        records
    }

    suspend fun listByTenant(tenantId: String): List<AgentDecisionOutcomeRecord> = withContext(Dispatchers.IO) {
        inMemoryStore[tenantId]?.toList() ?: emptyList()
    }

    fun seedSampleDataIfEmpty(tenantId: String) {
        val existing = inMemoryStore[tenantId]
        if (existing.isNullOrEmpty()) {
            val list = CopyOnWriteArrayList<AgentDecisionOutcomeRecord>()
            // Seed realistic distribution across confidence buckets:
            // 90s: claimed ~90-95%, actual accuracy ~88% (calibrated)
            repeat(15) { i ->
                list.add(
                    AgentDecisionOutcomeRecord(
                        id = "out-90-$i",
                        tenantId = tenantId,
                        confidence = 92.0,
                        outcomeClassification = if (i < 13) "REINFORCE" else "CORRECT"
                    )
                )
            }
            // 80s: claimed ~80-85%, actual accuracy ~65% (uncalibrated! overconfident deviation > 15%)
            repeat(20) { i ->
                list.add(
                    AgentDecisionOutcomeRecord(
                        id = "out-80-$i",
                        tenantId = tenantId,
                        confidence = 85.0,
                        outcomeClassification = if (i < 13) "REINFORCE" else "CORRECT" // 13/20 = 65% vs 85% claimed
                    )
                )
            }
            // 70s: claimed ~70%, actual accuracy ~72% (calibrated)
            repeat(18) { i ->
                list.add(
                    AgentDecisionOutcomeRecord(
                        id = "out-70-$i",
                        tenantId = tenantId,
                        confidence = 72.0,
                        outcomeClassification = if (i < 13) "REINFORCE" else "LEARN_FROM_REJECTION"
                    )
                )
            }
            // 50s: claimed ~50%, actual accuracy ~50%
            repeat(10) { i ->
                list.add(
                    AgentDecisionOutcomeRecord(
                        id = "out-50-$i",
                        tenantId = tenantId,
                        confidence = 54.0,
                        outcomeClassification = if (i < 5) "REINFORCE" else "CORRECT"
                    )
                )
            }
            inMemoryStore[tenantId] = list
        }
    }

    fun clear() {
        inMemoryStore.clear()
    }
}
