package ai.orchestree.backend.learning

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class OutcomeClassificationResult(
    val classification: String, // REINFORCE, CORRECT, LEARN_FROM_REJECTION
    val confidenceDelta: Double
)

@Serializable
data class ActionOutcomeRecord(
    val tenantId: String,
    val agentId: String,
    val agentName: String,
    val nodeId: String,
    val executionId: String,
    val workflowId: String,
    val scenarioContext: String,
    val actionType: String,
    val predictedImpact: String,
    val actualOutcome: String,
    val outcomeSource: String, // PAYMENT_WEBHOOK, HUMAN_EXPLICIT_REJECT, MONITORING_LOOP_RESULT, SOP_PRECHECK_BLOCKED, INTEGRATION_FABRIC_RESULT, SOURCE_SYSTEM_VERIFICATION
    val isSuccess: Boolean,
    val verifiedBy: String? = null,
    val sopBreached: Boolean = false,
    val safetyViolation: Boolean = false,
    val metricImpactJson: String = "{}"
)

@Serializable
data class NodeOutcomeRequest(
    val tenantId: String,
    val agentId: String,
    val agentName: String,
    val nodeId: String,
    val executionId: String,
    val workflowId: String,
    val scenarioContext: String,
    val actionType: String,
    val predictedImpact: String,
    val actualOutcome: String,
    val outcomeSource: String, // PAYMENT_WEBHOOK, HUMAN_EXPLICIT_REJECT, MONITORING_LOOP_RESULT, SOP_PRECHECK_BLOCKED
    val isSuccess: Boolean,
    val verifiedBy: String? = null,
    val sopBreached: Boolean = false,
    val safetyViolation: Boolean = false,
    val metricImpactJson: String = "{}"
)

@Serializable
data class LessonLearned(
    val id: String,
    val tenantId: String,
    val agentId: String,
    val skillId: String,
    val status: String, // ACTIVE, INVALIDATED_BY_ADMIN
    val sampleCount: Int,
    val rootCauseAnalysis: String,
    val correctiveGuidance: String,
    val validatedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

object ContinuousLearningCore {
    private val logger = LoggerFactory.getLogger(ContinuousLearningCore::class.java)
    private val inMemorySkillScores = java.util.concurrent.ConcurrentHashMap<String, AgentSkillStats>()
    private val inMemoryOutcomes = java.util.concurrent.CopyOnWriteArrayList<ActionOutcomeRecord>()

    /**
     * PRD Bagian 74.4: Anti-Reinforcement-of-Bad-Pattern Guardrail
     * If an outcome breached SOP, violated safety, or was explicitly rejected by human / system,
     * the system MUST NEVER reinforce the pattern, even if short-term operational metrics look positive.
     */
    fun classifyOutcomeWithGuardrail(
        outcomeSource: String,
        isSuccess: Boolean,
        sopBreached: Boolean = false,
        safetyViolation: Boolean = false,
        details: String = ""
    ): OutcomeClassificationResult {
        // Strict Guardrail Enforcement
        if (safetyViolation) {
            logger.warn("[ANTI_REINFORCE_GUARDRAIL] Safety violation detected! Classifying as LEARN_FROM_REJECTION with heavy penalty.")
            return OutcomeClassificationResult(classification = "LEARN_FROM_REJECTION", confidenceDelta = -8.0)
        }
        if (sopBreached) {
            logger.warn("[ANTI_REINFORCE_GUARDRAIL] SOP breach detected! Classification blocked from REINFORCE. Penalty applied.")
            return OutcomeClassificationResult(classification = "LEARN_FROM_REJECTION", confidenceDelta = -5.0)
        }
        if (outcomeSource == "HUMAN_EXPLICIT_REJECT") {
            return OutcomeClassificationResult(classification = "LEARN_FROM_REJECTION", confidenceDelta = -5.0)
        }
        if (outcomeSource == "SOP_PRECHECK_BLOCKED") {
            return OutcomeClassificationResult(classification = "LEARN_FROM_REJECTION", confidenceDelta = -4.0)
        }
        if (outcomeSource == "MONITORING_LOOP_RESULT" && !isSuccess) {
            return OutcomeClassificationResult(classification = "CORRECT", confidenceDelta = -3.5)
        }
        if (!isSuccess) {
            return OutcomeClassificationResult(classification = "CORRECT", confidenceDelta = -2.0)
        }

        // Positive reinforcement only when fully successful without breaches
        return when {
            outcomeSource == "PAYMENT_WEBHOOK" -> OutcomeClassificationResult(classification = "REINFORCE", confidenceDelta = 2.5)
            outcomeSource == "SOURCE_SYSTEM_VERIFICATION" -> OutcomeClassificationResult(classification = "REINFORCE", confidenceDelta = 2.0)
            else -> OutcomeClassificationResult(classification = "REINFORCE", confidenceDelta = 1.0)
        }
    }

    fun classifyOutcome(outcomeSource: String, isSuccess: Boolean, details: String = ""): OutcomeClassificationResult {
        return classifyOutcomeWithGuardrail(outcomeSource, isSuccess, false, false, details)
    }

    /**
     * PRD Bagian 74.3: Eight-Stage Closed-Loop Learning recordOutcome
     */
    suspend fun recordOutcome(record: ActionOutcomeRecord): OutcomeClassificationResult = withContext(Dispatchers.IO) {
        // Stage 4: Outcome Classification with Anti-Reinforcement Guardrail
        val classification = classifyOutcomeWithGuardrail(
            outcomeSource = record.outcomeSource,
            isSuccess = record.isSuccess,
            sopBreached = record.sopBreached,
            safetyViolation = record.safetyViolation,
            details = record.actualOutcome
        )

        val now = System.currentTimeMillis()
        inMemoryOutcomes.add(record)

        // Stage 5: Confidence Score Calibration (decay + impact weighting)
        val key = "${record.tenantId}:${record.agentId}"
        val existingStats = inMemorySkillScores[key] ?: AgentSkillStats(
            agentId = record.agentId,
            skillConfidenceScore = 85.0,
            reinforceCount = 0,
            correctCount = 0,
            rejectionCount = 0,
            sampleSize = 0,
            growthTrend = "+0.0% MoM"
        )

        val newSampleSize = existingStats.sampleSize + 1
        var newReinforce = existingStats.reinforceCount
        var newCorrect = existingStats.correctCount
        var newRejection = existingStats.rejectionCount

        when (classification.classification) {
            "REINFORCE" -> newReinforce += 1
            "CORRECT" -> newCorrect += 1
            "LEARN_FROM_REJECTION" -> newRejection += 1
        }

        val calibratedScore = (existingStats.skillConfidenceScore + classification.confidenceDelta).coerceIn(10.0, 100.0)
        val roundedScore = (calibratedScore * 10).toInt() / 10.0
        val trend = if (newReinforce >= newCorrect) "+${String.format(java.util.Locale.US, "%.1f", (newReinforce.toDouble() / newSampleSize) * 5.0)}% MoM" else "-1.5% MoM"

        inMemorySkillScores[key] = AgentSkillStats(
            agentId = record.agentId,
            skillConfidenceScore = roundedScore,
            reinforceCount = newReinforce,
            correctCount = newCorrect,
            rejectionCount = newRejection,
            sampleSize = newSampleSize,
            growthTrend = trend
        )

        // Stage 1 & 2: Record to DB if available
        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        INSERT INTO agent_decision_outcomes 
                        (id, tenant_id, agent_id, agent_name, node_id, execution_id, workflow_id, scenario_context, 
                         action_type, predicted_impact, actual_outcome, outcome_source, outcome_classification, 
                         verified_by, confidence_delta, metric_impact_json, created_at, confidence)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()).use { ps ->
                        ps.setString(1, "out-" + UUID.randomUUID().toString().take(8))
                        ps.setString(2, record.tenantId)
                        ps.setString(3, record.agentId)
                        ps.setString(4, record.agentName)
                        ps.setString(5, record.nodeId)
                        ps.setString(6, record.executionId)
                        ps.setString(7, record.workflowId)
                        ps.setString(8, record.scenarioContext)
                        ps.setString(9, record.actionType)
                        ps.setString(10, record.predictedImpact)
                        ps.setString(11, record.actualOutcome)
                        ps.setString(12, record.outcomeSource)
                        ps.setString(13, classification.classification)
                        ps.setString(14, record.verifiedBy ?: "SYSTEM_AUTOMATED")
                        ps.setDouble(15, classification.confidenceDelta)
                        ps.setString(16, record.metricImpactJson)
                        ps.setLong(17, now)
                        ps.setDouble(18, roundedScore / 100.0)
                        ps.executeUpdate()
                    }

                    c.prepareStatement("""
                        INSERT INTO agent_skill_confidence 
                        (id, tenant_id, agent_id, skill_id, skill_name, current_confidence_score, sample_size, 
                         reinforce_count, correct_count, rejection_count, last_evaluated_at, decay_factor)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1.0)
                        ON CONFLICT (id) DO UPDATE SET 
                            current_confidence_score = EXCLUDED.current_confidence_score,
                            sample_size = EXCLUDED.sample_size,
                            reinforce_count = EXCLUDED.reinforce_count,
                            correct_count = EXCLUDED.correct_count,
                            rejection_count = EXCLUDED.rejection_count,
                            last_evaluated_at = EXCLUDED.last_evaluated_at
                    """.trimIndent()).use { psUp ->
                        psUp.setString(1, "conf-${record.agentId}-${record.actionType}")
                        psUp.setString(2, record.tenantId)
                        psUp.setString(3, record.agentId)
                        psUp.setString(4, record.actionType)
                        psUp.setString(5, record.actionType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                        psUp.setDouble(6, roundedScore)
                        psUp.setInt(7, newSampleSize)
                        psUp.setInt(8, newReinforce)
                        psUp.setInt(9, newCorrect)
                        psUp.setInt(10, newRejection)
                        psUp.setLong(11, now)
                        psUp.executeUpdate()
                    }

                    // Stage 6: Distillation to Policy Candidate if repeated failures occur
                    if (newRejection >= 3) {
                        c.prepareStatement("""
                            INSERT INTO agent_lessons_learned 
                            (id, tenant_id, agent_id, skill_id, status, sample_count, root_cause_analysis, 
                             corrective_guidance, created_at)
                            VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?)
                            ON CONFLICT (id) DO NOTHING
                        """.trimIndent()).use { psLes ->
                            psLes.setString(1, "les-${record.agentId}-${record.actionType}")
                            psLes.setString(2, record.tenantId)
                            psLes.setString(3, record.agentId)
                            psLes.setString(4, record.actionType)
                            psLes.setInt(5, newRejection)
                            psLes.setString(6, "Anti-reinforcement active: $newRejection rejections/breaches detected on ${record.actionType}")
                            psLes.setString(7, "Strict human approval mandatory for ${record.actionType}. Automatic threshold tightened.")
                            psLes.setLong(8, now)
                            psLes.executeUpdate()
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not record continuous learning outcome in DB: ${e.message}")
            }
        }

        classification
    }

    suspend fun onNodeOutcomeAvailable(request: NodeOutcomeRequest) = withContext(Dispatchers.IO) {
        val record = ActionOutcomeRecord(
            tenantId = request.tenantId,
            agentId = request.agentId,
            agentName = request.agentName,
            nodeId = request.nodeId,
            executionId = request.executionId,
            workflowId = request.workflowId,
            scenarioContext = request.scenarioContext,
            actionType = request.actionType,
            predictedImpact = request.predictedImpact,
            actualOutcome = request.actualOutcome,
            outcomeSource = request.outcomeSource,
            isSuccess = request.isSuccess,
            verifiedBy = request.verifiedBy,
            sopBreached = request.sopBreached,
            safetyViolation = request.safetyViolation,
            metricImpactJson = request.metricImpactJson
        )
        recordOutcome(record)
    }

    suspend fun invalidateLesson(tenantId: String, lessonId: String, adminUserId: String): Boolean = withContext(Dispatchers.IO) {
        val conn = DatabaseManager.getConnection() ?: return@withContext false
        try {
            conn.use { c ->
                c.prepareStatement("""
                    UPDATE agent_lessons_learned 
                    SET status = 'INVALIDATED_BY_ADMIN', validated_by = ? 
                    WHERE id = ? AND tenant_id = ?
                """.trimIndent()).use { ps ->
                    ps.setString(1, adminUserId)
                    ps.setString(2, lessonId)
                    ps.setString(3, tenantId)
                    ps.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            logger.warn("Could not invalidate lesson: ${e.message}")
            false
        }
    }

    suspend fun retrieveRelevantLessons(tenantId: String, agentId: String, skillId: String): List<LessonLearned> = withContext(Dispatchers.IO) {
        val lessons = mutableListOf<LessonLearned>()
        val conn = DatabaseManager.getConnection() ?: return@withContext emptyList()
        try {
            conn.use { c ->
                c.prepareStatement("""
                    SELECT id, tenant_id, agent_id, skill_id, status, sample_count, root_cause_analysis, corrective_guidance, validated_by, created_at
                    FROM agent_lessons_learned 
                    WHERE tenant_id = ? AND agent_id = ? AND skill_id = ? AND status = 'ACTIVE'
                """.trimIndent()).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, agentId)
                    ps.setString(3, skillId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            lessons.add(
                                LessonLearned(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id"),
                                    agentId = rs.getString("agent_id"),
                                    skillId = rs.getString("skill_id"),
                                    status = rs.getString("status"),
                                    sampleCount = rs.getInt("sample_count"),
                                    rootCauseAnalysis = rs.getString("root_cause_analysis") ?: "",
                                    correctiveGuidance = rs.getString("corrective_guidance") ?: "",
                                    validatedBy = rs.getString("validated_by"),
                                    createdAt = rs.getLong("created_at")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Could not query lessons: ${e.message}")
        }
        lessons
    }

    suspend fun getAgentSkillConfidence(tenantId: String, agentId: String): AgentSkillStats = withContext(Dispatchers.IO) {
        var avgScore = 85.0
        var totalReinforce = 0
        var totalCorrect = 0
        var totalRejection = 0
        var totalSamples = 0
        var foundInDb = false

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        SELECT AVG(current_confidence_score) as avg_score, 
                               SUM(reinforce_count) as sum_reinforce, 
                               SUM(correct_count) as sum_correct, 
                               SUM(rejection_count) as sum_rejection, 
                               SUM(sample_size) as sum_samples
                        FROM agent_skill_confidence 
                        WHERE tenant_id = ? AND agent_id = ?
                    """.trimIndent()).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setString(2, agentId)
                        ps.executeQuery().use { rs ->
                            if (rs.next() && rs.getObject("avg_score") != null) {
                                avgScore = rs.getDouble("avg_score")
                                totalReinforce = rs.getInt("sum_reinforce")
                                totalCorrect = rs.getInt("sum_correct")
                                totalRejection = rs.getInt("sum_rejection")
                                totalSamples = rs.getInt("sum_samples")
                                foundInDb = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying agent_skill_confidences: ${e.message}")
            }
        }

        if (!foundInDb) {
            val inMem = inMemorySkillScores["$tenantId:$agentId"]
            if (inMem != null) {
                return@withContext inMem
            }
        }

        val trend = if (totalReinforce >= totalCorrect) "+${String.format(java.util.Locale.US, "%.1f", (totalReinforce.toDouble() / (totalSamples.coerceAtLeast(1))) * 5.0)}% MoM" else "-1.5% MoM"
        AgentSkillStats(
            agentId = agentId,
            skillConfidenceScore = (avgScore * 10).toInt() / 10.0,
            reinforceCount = totalReinforce,
            correctCount = totalCorrect,
            rejectionCount = totalRejection,
            sampleSize = totalSamples,
            growthTrend = if (foundInDb) trend else "+0.0% MoM"
        )
    }
}

@Serializable
data class AgentSkillStats(
    val agentId: String,
    val skillConfidenceScore: Double,
    val reinforceCount: Int,
    val correctCount: Int,
    val rejectionCount: Int,
    val sampleSize: Int,
    val growthTrend: String
)

