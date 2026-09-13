package ai.orchestree.backend.intelligence

import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.orchestration.ApprovedAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class KnowledgeRule(
    val id: String = "kr-${UUID.randomUUID().toString().take(8)}",
    val tenantId: String,
    val entityType: String, // 'EQUIPMENT', 'INVENTORY', 'FINANCE', 'SAFETY', 'FLEET'
    val condition: String, // e.g. 'operating_temperature', 'stock_level', 'vibration_index'
    val comparisonOperator: String = ">=", // '>', '>=', '<', '<=', '==', '!='
    val thresholdValue: Double,
    val sopReference: String, // e.g. 'SOP-MAINT-HEAVY-04'
    val ruleDescription: String,
    val proposedByAiAgentId: String? = null,
    val proposedByPersona: String? = null,
    var status: String = "PENDING_APPROVAL", // 'PENDING_APPROVAL', 'APPROVED', 'REJECTED'
    var rejectionReason: String? = null,
    var approvedByUserId: String? = null,
    var approvedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class TriggeredSopDirective(
    val ruleId: String,
    val entityId: String,
    val metricName: String,
    val liveValue: Double,
    val thresholdValue: Double,
    val sopReference: String,
    val directiveMessage: String,
    val requiresImmediateAction: Boolean
)

@Serializable
data class OperationalFusionResult(
    val tenantId: String,
    val entityId: String,
    val entityType: String,
    val evaluatedMetrics: Map<String, Double>,
    val approvedRulesCount: Int,
    val pendingRulesIgnoredCount: Int,
    val triggeredDirectives: List<TriggeredSopDirective>,
    val synthesizedExecutiveAction: String?,
    val recommendedActions: List<ApprovedAction>,
    val fusedAt: Long = System.currentTimeMillis()
)

/**
 * AI Knowledge + Operational Data Fusion Engine (PRD Addendum 2 Bagian 70)
 * Active for ALL tiers (ALL_TIER).
 * - Implements Fusion algorithm (Bagian 70.1)
 * - Knowledge Rule MANDATORY Admin Approval before active (Bagian 70.2)
 */
class KnowledgeOperationalFusionEngine {
    private val logger = LoggerFactory.getLogger(KnowledgeOperationalFusionEngine::class.java)
    val rulesStore = ConcurrentHashMap<String, KnowledgeRule>()

    init {
        seedBaselineRules()
    }

    private fun seedBaselineRules() {
        // Seed initial approved SOP rule for heavy equipment / inventory
        val rule1 = KnowledgeRule(
            id = "kr-maint-01",
            tenantId = "tenant-default",
            entityType = "EQUIPMENT",
            condition = "operating_temperature",
            comparisonOperator = ">=",
            thresholdValue = 85.0,
            sopReference = "SOP-MAINT-EXCAVATOR-01",
            ruleDescription = "Suhu operasional radiator excavator melebihi batas 85C; jalankan siklus pendinginan & inspeksi seal hidrolik",
            status = "APPROVED",
            approvedByUserId = "admin-sys",
            approvedAt = System.currentTimeMillis() - 86400000L
        )
        val rule2 = KnowledgeRule(
            id = "kr-inv-01",
            tenantId = "tenant-default",
            entityType = "INVENTORY",
            condition = "stock_level",
            comparisonOperator = "<=",
            thresholdValue = 15.0,
            sopReference = "SOP-INV-RESTOCK-03",
            ruleDescription = "Stok suku cadang kritis di bawah ambang batas keselamatan (15 unit); segera terbitkan order pengadaan darurat",
            status = "APPROVED",
            approvedByUserId = "admin-sys",
            approvedAt = System.currentTimeMillis() - 86400000L
        )
        rulesStore[rule1.id] = rule1
        rulesStore[rule2.id] = rule2
    }

    /**
     * Creates a new Knowledge Rule.
     * MANDATORY: Newly created rules are initialized in PENDING_APPROVAL status.
     * They will NOT be active until approved by an Admin.
     */
    suspend fun proposeRule(rule: KnowledgeRule): KnowledgeRule = withContext(Dispatchers.IO) {
        val pendingRule = rule.copy(
            status = "PENDING_APPROVAL",
            approvedByUserId = null,
            approvedAt = null,
            updatedAt = System.currentTimeMillis()
        )
        rulesStore[pendingRule.id] = pendingRule
        persistRuleToDb(pendingRule)
        logger.info("[KNOWLEDGE_FUSION] Rule proposed: ${pendingRule.id} for ${pendingRule.entityType} (Status: PENDING_APPROVAL)")
        pendingRule
    }

    /**
     * Admin approves a knowledge rule, making it ACTIVE for operational data fusion.
     */
    suspend fun approveRule(tenantId: String, ruleId: String, adminUserId: String): KnowledgeRule? = withContext(Dispatchers.IO) {
        val rule = rulesStore[ruleId] ?: return@withContext null
        if (rule.tenantId != tenantId && rule.tenantId != "tenant-default") return@withContext null

        rule.status = "APPROVED"
        rule.approvedByUserId = adminUserId
        rule.approvedAt = System.currentTimeMillis()
        rule.updatedAt = System.currentTimeMillis()

        persistRuleToDb(rule)
        logger.info("[KNOWLEDGE_FUSION] Rule approved by admin $adminUserId: ${rule.id} (Status: APPROVED)")
        rule
    }

    /**
     * Admin rejects a knowledge rule.
     */
    suspend fun rejectRule(tenantId: String, ruleId: String, adminUserId: String, reason: String): KnowledgeRule? = withContext(Dispatchers.IO) {
        val rule = rulesStore[ruleId] ?: return@withContext null
        if (rule.tenantId != tenantId && rule.tenantId != "tenant-default") return@withContext null

        rule.status = "REJECTED"
        rule.rejectionReason = reason
        rule.approvedByUserId = adminUserId
        rule.updatedAt = System.currentTimeMillis()

        persistRuleToDb(rule)
        logger.info("[KNOWLEDGE_FUSION] Rule rejected by admin $adminUserId: ${rule.id}")
        rule
    }

    /**
     * Lists knowledge rules for tenant, optionally filtered by status.
     */
    fun listRules(tenantId: String, statusFilter: String? = null): List<KnowledgeRule> {
        return rulesStore.values.filter { rule ->
            (rule.tenantId == tenantId || rule.tenantId == "tenant-default") &&
            (statusFilter == null || rule.status.equals(statusFilter, ignoreCase = true))
        }
    }

    /**
     * Algoritma Fusion (Bagian 70.1):
     * Fuses real-time operational telemetry with Company Brain Knowledge Rules.
     * MANDATORY: Only APPROVED rules are evaluated. PENDING_APPROVAL rules are skipped.
     */
    suspend fun fuseOperationalData(
        tenantId: String,
        entityId: String,
        entityType: String,
        operationalMetrics: Map<String, Double>
    ): OperationalFusionResult = withContext(Dispatchers.IO) {
        val allTenantRules = listRules(tenantId)
        val approvedRules = allTenantRules.filter {
            it.status.equals("APPROVED", ignoreCase = true) || it.status.equals("ACTIVE", ignoreCase = true)
        }.filter { it.entityType.equals(entityType, ignoreCase = true) }

        val pendingCount = allTenantRules.count {
            it.status.equals("PENDING_APPROVAL", ignoreCase = true) && it.entityType.equals(entityType, ignoreCase = true)
        }

        val triggeredDirectives = mutableListOf<TriggeredSopDirective>()
        val actionProposals = mutableListOf<ApprovedAction>()

        for (rule in approvedRules) {
            val liveVal = operationalMetrics[rule.condition] ?: continue
            val isViolated = evaluateCondition(liveVal, rule.comparisonOperator, rule.thresholdValue)

            if (isViolated) {
                val directiveMsg = "[SOP DIRECTIVE: ${rule.sopReference}] ${rule.ruleDescription}. Nilai ${rule.condition} saat ini ($liveVal) melampaui batas ${rule.comparisonOperator} ${rule.thresholdValue}."
                triggeredDirectives.add(
                    TriggeredSopDirective(
                        ruleId = rule.id,
                        entityId = entityId,
                        metricName = rule.condition,
                        liveValue = liveVal,
                        thresholdValue = rule.thresholdValue,
                        sopReference = rule.sopReference,
                        directiveMessage = directiveMsg,
                        requiresImmediateAction = true
                    )
                )

                // Generate automatic action proposal
                actionProposals.add(
                    ApprovedAction(
                        id = "act-sop-${UUID.randomUUID().toString().take(6)}",
                        tenantId = tenantId,
                        agentId = "agent-sop-sentinel",
                        actionType = "CREATE_TASK",
                        targetSystem = "TASK_BOARD",
                        payload = mapOf(
                            "title" to "[SOP EXECUTION] ${rule.sopReference}: Anomali $entityId",
                            "description" to directiveMsg,
                            "reason" to "Knowledge Fusion Triggered: ${rule.sopReference}",
                            "quantity" to "1"
                        ),
                        assignedHuman = "operational-lead",
                        status = "APPROVED",
                        approvedBy = "SYSTEM_KNOWLEDGE_FUSION"
                    )
                )
            }
        }

        val executiveSummary = if (triggeredDirectives.isNotEmpty()) {
            "Terdeteksi ${triggeredDirectives.size} penyimpangan operasional terhadap SOP yang disetujui. Dokumen rujukan: ${triggeredDirectives.joinToString { it.sopReference }}."
        } else {
            "Semua metrik operasional $entityId sesuai dengan SOP dan Knowledge Rules yang disetujui."
        }

        OperationalFusionResult(
            tenantId = tenantId,
            entityId = entityId,
            entityType = entityType,
            evaluatedMetrics = operationalMetrics,
            approvedRulesCount = approvedRules.size,
            pendingRulesIgnoredCount = pendingCount,
            triggeredDirectives = triggeredDirectives,
            synthesizedExecutiveAction = executiveSummary,
            recommendedActions = actionProposals
        )
    }

    private fun evaluateCondition(value: Double, operator: String, threshold: Double): Boolean {
        return when (operator) {
            ">=" -> value >= threshold
            ">" -> value > threshold
            "<=" -> value <= threshold
            "<" -> value < threshold
            "==" -> Math.abs(value - threshold) < 0.001
            "!=" -> Math.abs(value - threshold) >= 0.001
            else -> false
        }
    }

    private fun persistRuleToDb(rule: KnowledgeRule) {
        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = """
                    INSERT INTO knowledge_rules (
                        id, tenant_id, entity_type, condition, comparison_operator,
                        threshold_value, sop_reference, rule_description, proposed_by_ai_agent_id,
                        proposed_by_persona, status, rejection_reason, approved_by_user_id,
                        approved_at, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        status = EXCLUDED.status,
                        rejection_reason = EXCLUDED.rejection_reason,
                        approved_by_user_id = EXCLUDED.approved_by_user_id,
                        approved_at = EXCLUDED.approved_at,
                        updated_at = EXCLUDED.updated_at
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, rule.id)
                    ps.setString(2, rule.tenantId)
                    ps.setString(3, rule.entityType)
                    ps.setString(4, rule.condition)
                    ps.setString(5, rule.comparisonOperator)
                    ps.setDouble(6, rule.thresholdValue)
                    ps.setString(7, rule.sopReference)
                    ps.setString(8, rule.ruleDescription)
                    ps.setString(9, rule.proposedByAiAgentId)
                    ps.setString(10, rule.proposedByPersona)
                    ps.setString(11, rule.status)
                    ps.setString(12, rule.rejectionReason)
                    ps.setString(13, rule.approvedByUserId)
                    ps.setLong(14, rule.approvedAt ?: 0L)
                    ps.setLong(15, rule.createdAt)
                    ps.setLong(16, rule.updatedAt)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            logger.debug("Could not persist to knowledge_rules table (using memory store): ${e.message}")
        }
    }
}
