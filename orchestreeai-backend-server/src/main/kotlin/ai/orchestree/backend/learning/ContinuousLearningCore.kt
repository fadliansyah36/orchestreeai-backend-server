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
    val verifiedBy: String? = null
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

    fun classifyOutcome(outcomeSource: String, isSuccess: Boolean, details: String = ""): OutcomeClassificationResult {
        return when {
            outcomeSource == "PAYMENT_WEBHOOK" && isSuccess -> {
                OutcomeClassificationResult(classification = "REINFORCE", confidenceDelta = 2.5)
            }
            outcomeSource == "HUMAN_EXPLICIT_REJECT" -> {
                OutcomeClassificationResult(classification = "LEARN_FROM_REJECTION", confidenceDelta = -5.0)
            }
            outcomeSource == "MONITORING_LOOP_RESULT" && !isSuccess -> {
                OutcomeClassificationResult(classification = "CORRECT", confidenceDelta = -3.5)
            }
            outcomeSource == "SOP_PRECHECK_BLOCKED" -> {
                OutcomeClassificationResult(classification = "LEARN_FROM_REJECTION", confidenceDelta = -4.0)
            }
            isSuccess -> {
                OutcomeClassificationResult(classification = "REINFORCE", confidenceDelta = 1.0)
            }
            else -> {
                OutcomeClassificationResult(classification = "CORRECT", confidenceDelta = -2.0)
            }
        }
    }

    suspend fun onNodeOutcomeAvailable(request: NodeOutcomeRequest) = withContext(Dispatchers.IO) {
        val classification = classifyOutcome(request.outcomeSource, request.isSuccess, request.actualOutcome)
        val now = System.currentTimeMillis()
        val conn = DatabaseManager.getConnection()

        if (conn != null) {
            try {
                conn.use { c ->
                    // 1. Insert into agent_decision_outcomes
                    c.prepareStatement("""
                        INSERT INTO agent_decision_outcomes 
                        (id, tenant_id, agent_id, agent_name, node_id, execution_id, workflow_id, scenario_context, 
                         action_type, predicted_impact, actual_outcome, outcome_source, outcome_classification, 
                         verified_by, confidence_delta, metric_impact_json, created_at, confidence)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()).use { ps ->
                        ps.setString(1, "out-" + UUID.randomUUID().toString().take(8))
                        ps.setString(2, request.tenantId)
                        ps.setString(3, request.agentId)
                        ps.setString(4, request.agentId)
                        ps.setString(5, request.nodeId)
                        ps.setString(6, request.executionId)
                        ps.setString(7, request.workflowId)
                        ps.setString(8, request.scenarioContext)
                        ps.setString(9, request.actionType)
                        ps.setString(10, request.predictedImpact)
                        ps.setString(11, request.actualOutcome)
                        ps.setString(12, request.outcomeSource)
                        ps.setString(13, classification.classification)
                        ps.setString(14, request.verifiedBy ?: "SYSTEM_AUTOMATED")
                        ps.setDouble(15, classification.confidenceDelta)
                        ps.setString(16, "{}")
                        ps.setLong(17, now)
                        ps.setDouble(18, 0.85)
                        ps.executeUpdate()
                    }

                    // 2. Update or insert agent_skill_confidence
                    var currentScore = 80.0
                    var sampleSize = 0
                    var reinforceCount = 0
                    var correctCount = 0
                    var rejectionCount = 0

                    c.prepareStatement("""
                        SELECT current_confidence_score, sample_size, reinforce_count, correct_count, rejection_count
                        FROM agent_skill_confidence 
                        WHERE tenant_id = ? AND agent_id = ? AND skill_id = ?
                    """.trimIndent()).use { psSel ->
                        psSel.setString(1, request.tenantId)
                        psSel.setString(2, request.agentId)
                        psSel.setString(3, request.actionType)
                        psSel.executeQuery().use { rs ->
                            if (rs.next()) {
                                currentScore = rs.getDouble("current_confidence_score")
                                sampleSize = rs.getInt("sample_size")
                                reinforceCount = rs.getInt("reinforce_count")
                                correctCount = rs.getInt("correct_count")
                                rejectionCount = rs.getInt("rejection_count")
                            }
                        }
                    }

                    sampleSize += 1
                    when (classification.classification) {
                        "REINFORCE" -> reinforceCount += 1
                        "CORRECT" -> correctCount += 1
                        "LEARN_FROM_REJECTION" -> rejectionCount += 1
                    }

                    // Calculate new score with floor 10.0 and ceiling 100.0
                    currentScore = (currentScore + classification.confidenceDelta).coerceIn(10.0, 100.0)

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
                        psUp.setString(1, "conf-${request.agentId}-${request.actionType}")
                        psUp.setString(2, request.tenantId)
                        psUp.setString(3, request.agentId)
                        psUp.setString(4, request.actionType)
                        psUp.setString(5, request.actionType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                        psUp.setDouble(6, currentScore)
                        psUp.setInt(7, sampleSize)
                        psUp.setInt(8, reinforceCount)
                        psUp.setInt(9, correctCount)
                        psUp.setInt(10, rejectionCount)
                        psUp.setLong(11, now)
                        psUp.executeUpdate()
                    }

                    // 3. Check sample size threshold for Distillation (>= 5 failures -> generate lesson learned)
                    if (rejectionCount >= 5) {
                        c.prepareStatement("""
                            INSERT INTO agent_lessons_learned 
                            (id, tenant_id, agent_id, skill_id, status, sample_count, root_cause_analysis, 
                             corrective_guidance, created_at)
                            VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?)
                            ON CONFLICT (id) DO NOTHING
                        """.trimIndent()).use { psLes ->
                            psLes.setString(1, "les-${request.agentId}-${request.actionType}")
                            psLes.setString(2, request.tenantId)
                            psLes.setString(3, request.agentId)
                            psLes.setString(4, request.actionType)
                            psLes.setInt(5, rejectionCount)
                            psLes.setString(6, "Pola deviasi berulang terdeteksi pada $rejectionCount kasus: ${request.actualOutcome}")
                            psLes.setString(7, "Batas autonomous threshold diperketat untuk aksi ${request.actionType}. Seluruh request diskon atau deviasi wajib eskalasi.")
                            psLes.setLong(8, now)
                            psLes.executeUpdate()
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not record continuous learning outcome in DB: ${e.message}")
            }
        }
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

