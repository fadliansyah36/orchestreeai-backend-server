package ai.orchestree.backend.events

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class AiEventDefinition(
    val id: String,
    val eventCode: String,
    val responsiblePersonaType: String,
    val severityDefault: String = "HIGH",
    val description: String,
    val isMultiAgentCandidate: Boolean = false
)

@Serializable
data class AiEventInstance(
    val id: String = "evt-${UUID.randomUUID().toString().take(8)}",
    val tenantId: String,
    val eventCode: String,
    val entityReference: String,
    val sourceSystem: String = "INTERNAL",
    val payloadJson: String = "{}",
    var status: String = "NEW", // 'NEW', 'DISPATCHED', 'RESOLVED', 'ESCALATED'
    val severity: String = "HIGH",
    val isMultiAgentCollaborative: Boolean = false,
    val triggeredAt: Long = System.currentTimeMillis(),
    var handledAt: Long? = null
)

@Serializable
data class AiEventDispatchLog(
    val id: String = "disp-${UUID.randomUUID().toString().take(8)}",
    val tenantId: String,
    val eventInstanceId: String,
    val dispatchedToAgentId: String,
    val targetPersona: String,
    val dispatchedAt: Long = System.currentTimeMillis(),
    val responseSummary: String = "",
    val executionSuccess: Boolean = true,
    val isCollaborativeDispatch: Boolean = false
)

@Serializable
data class EventDispatchResult(
    val eventInstance: AiEventInstance,
    val dispatchLogs: List<AiEventDispatchLog>,
    val responsiblePersona: String,
    val summary: String
)

/**
 * AI Event Engine (PRD Addendum 2 Bagian 71)
 * Active for ALL tiers (ALL_TIER).
 * - Schema V33: ai_event_definitions, ai_event_instances, ai_event_dispatch_log (Bagian 71.1)
 * - Specialist Agent Dispatch Algorithm (Bagian 71.2)
 */
class AiEventEngine {
    private val logger = LoggerFactory.getLogger(AiEventEngine::class.java)

    companion object {
        val defaultInstance by lazy { AiEventEngine() }
    }

    val definitionsStore = ConcurrentHashMap<String, AiEventDefinition>()
    val eventInstancesStore = ConcurrentHashMap<String, AiEventInstance>()
    val dispatchLogsStore = CopyOnWriteArrayList<AiEventDispatchLog>()

    init {
        seedDefinitions()
    }

    private fun seedDefinitions() {
        val defs = listOf(
            AiEventDefinition(
                id = "def-01",
                eventCode = "EQUIPMENT_OVERHEAT",
                responsiblePersonaType = "MAINTENANCE_SPECIALIST",
                severityDefault = "CRITICAL",
                description = "Suhu mesin/radiator melebihi batas aman operasional",
                isMultiAgentCandidate = true
            ),
            AiEventDefinition(
                id = "def-02",
                eventCode = "INVENTORY_CRITICAL_STOCKOUT",
                responsiblePersonaType = "LOGISTICS_OPERATOR",
                severityDefault = "HIGH",
                description = "Stok material atau suku cadang kritis menyentuh batas minimum keselamatan",
                isMultiAgentCandidate = false
            ),
            AiEventDefinition(
                id = "def-03",
                eventCode = "CASH_FLOW_PRESSURE_SPIKE",
                responsiblePersonaType = "FINANCE_CONTROLLER",
                severityDefault = "HIGH",
                description = "Lonjakan rasio piutang tertunggak atau penyusutan runway kas drastis",
                isMultiAgentCandidate = true
            ),
            AiEventDefinition(
                id = "def-04",
                eventCode = "SLA_BREACH_RISK",
                responsiblePersonaType = "OPERATIONS_LEAD",
                severityDefault = "MEDIUM",
                description = "Progres milestone operasional tertinggal dari batas waktu SLA",
                isMultiAgentCandidate = false
            )
        )
        defs.forEach { definitionsStore[it.eventCode] = it }
    }

    /**
     * Publishes a new AI Event and triggers the Specialist Agent Dispatch Algorithm (Bagian 71.2).
     */
    suspend fun publishAndDispatch(event: AiEventInstance): EventDispatchResult = withContext(Dispatchers.IO) {
        eventInstancesStore[event.id] = event
        persistEventInstance(event)

        val def = definitionsStore[event.eventCode] ?: AiEventDefinition(
            id = "def-gen-${event.eventCode}",
            eventCode = event.eventCode,
            responsiblePersonaType = "CHIEF_OF_STAFF_AGENT",
            severityDefault = event.severity,
            description = "General Enterprise Event ${event.eventCode}",
            isMultiAgentCandidate = false
        )

        val targetAgentId = "agent-${def.responsiblePersonaType.lowercase().replace('_', '-')}-01"
        val dispatchLogs = mutableListOf<AiEventDispatchLog>()

        // 1. Primary Dispatch to Specialist Agent
        val primaryLog = AiEventDispatchLog(
            tenantId = event.tenantId,
            eventInstanceId = event.id,
            dispatchedToAgentId = targetAgentId,
            targetPersona = def.responsiblePersonaType,
            responseSummary = "Event ${event.eventCode} dispatched to ${def.responsiblePersonaType} ($targetAgentId) for entity ${event.entityReference}",
            executionSuccess = true,
            isCollaborativeDispatch = false
        )
        dispatchLogs.add(primaryLog)
        dispatchLogsStore.add(primaryLog)
        persistDispatchLog(primaryLog)

        // 2. Multi-Agent Collaborative Dispatch if candidate
        if (def.isMultiAgentCandidate || event.isMultiAgentCollaborative) {
            val collabAgentId = "agent-chief-of-staff-01"
            val collabLog = AiEventDispatchLog(
                tenantId = event.tenantId,
                eventInstanceId = event.id,
                dispatchedToAgentId = collabAgentId,
                targetPersona = "CHIEF_OF_STAFF_AGENT",
                responseSummary = "Collaborative escalation: Chief of Staff notified for cross-domain alignment on ${event.eventCode}",
                executionSuccess = true,
                isCollaborativeDispatch = true
            )
            dispatchLogs.add(collabLog)
            dispatchLogsStore.add(collabLog)
            persistDispatchLog(collabLog)
        }

        // Update event state
        event.status = "DISPATCHED"
        event.handledAt = System.currentTimeMillis()
        persistEventInstance(event)

        logger.info("[AI_EVENT_ENGINE] Event ${event.id} (${event.eventCode}) dispatched to ${def.responsiblePersonaType} (Total dispatches: ${dispatchLogs.size})")

        EventDispatchResult(
            eventInstance = event,
            dispatchLogs = dispatchLogs,
            responsiblePersona = def.responsiblePersonaType,
            summary = "Dispatched event ${event.id} to ${def.responsiblePersonaType} with ${dispatchLogs.size} agent assignments"
        )
    }

    /**
     * Lists events for a tenant.
     */
    fun listEvents(tenantId: String): List<AiEventInstance> = listEventsPaginated(tenantId, 50, 0).second

    /**
     * Lists events for a tenant with pagination (LIMIT/OFFSET on SQL level).
     */
    fun listEventsPaginated(tenantId: String, limit: Int = 20, offset: Int = 0): Pair<Long, List<AiEventInstance>> {
        var count = 0L
        val dbList = mutableListOf<AiEventInstance>()

        try {
            DatabaseManager.getConnection()?.use { conn ->
                conn.prepareStatement("""
                    SELECT count(*) FROM ai_event_instances 
                    WHERE tenant_id = ? OR tenant_id = 'tenant-default'
                """.trimIndent()).use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) count = rs.getLong(1)
                    }
                }

                if (count > 0L) {
                    conn.prepareStatement("""
                        SELECT id, tenant_id, event_code, entity_reference, source_system,
                               payload_json, status, severity, is_multi_agent_collaborative,
                               triggered_at, handled_at
                        FROM ai_event_instances
                        WHERE tenant_id = ? OR tenant_id = 'tenant-default'
                        ORDER BY triggered_at DESC
                        LIMIT ? OFFSET ?
                    """.trimIndent()).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setInt(2, limit)
                        ps.setInt(3, offset)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                dbList.add(
                                    AiEventInstance(
                                        id = rs.getString("id"),
                                        tenantId = rs.getString("tenant_id"),
                                        eventCode = rs.getString("event_code"),
                                        entityReference = rs.getString("entity_reference"),
                                        sourceSystem = rs.getString("source_system") ?: "INTERNAL",
                                        payloadJson = rs.getString("payload_json") ?: "{}",
                                        status = rs.getString("status") ?: "NEW",
                                        severity = rs.getString("severity") ?: "HIGH",
                                        isMultiAgentCollaborative = rs.getBoolean("is_multi_agent_collaborative"),
                                        triggeredAt = rs.getLong("triggered_at"),
                                        handledAt = rs.getLong("handled_at").takeIf { it > 0L }
                                    )
                                )
                            }
                        }
                    }
                    return Pair(count, dbList)
                }
            }
        } catch (e: Exception) {
            logger.debug("Failed querying ai_event_instances from DB: ${e.message}")
        }

        val allInMem = eventInstancesStore.values.filter {
            it.tenantId == tenantId || it.tenantId == "tenant-default"
        }.sortedByDescending { it.triggeredAt }

        val total = allInMem.size.toLong()
        val paged = allInMem.drop(offset).take(limit)
        return Pair(total, paged)
    }

    private fun persistEventInstance(event: AiEventInstance) {
        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = """
                    INSERT INTO ai_event_instances (
                        id, tenant_id, event_code, entity_reference, source_system,
                        payload_json, status, severity, is_multi_agent_collaborative,
                        triggered_at, handled_at
                    ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        status = EXCLUDED.status,
                        handled_at = EXCLUDED.handled_at
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, event.id)
                    ps.setString(2, event.tenantId)
                    ps.setString(3, event.eventCode)
                    ps.setString(4, event.entityReference)
                    ps.setString(5, event.sourceSystem)
                    ps.setString(6, event.payloadJson)
                    ps.setString(7, event.status)
                    ps.setString(8, event.severity)
                    ps.setBoolean(9, event.isMultiAgentCollaborative)
                    ps.setLong(10, event.triggeredAt)
                    ps.setLong(11, event.handledAt ?: 0L)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            logger.debug("Could not persist to ai_event_instances table (using memory store): ${e.message}")
        }
    }

    private fun persistDispatchLog(log: AiEventDispatchLog) {
        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = """
                    INSERT INTO ai_event_dispatch_log (
                        id, tenant_id, event_instance_id, dispatched_to_agent_id,
                        target_persona, dispatched_at, response_summary, execution_success,
                        is_collaborative_dispatch
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, log.id)
                    ps.setString(2, log.tenantId)
                    ps.setString(3, log.eventInstanceId)
                    ps.setString(4, log.dispatchedToAgentId)
                    ps.setString(5, log.targetPersona)
                    ps.setLong(6, log.dispatchedAt)
                    ps.setString(7, log.responseSummary)
                    ps.setBoolean(8, log.executionSuccess)
                    ps.setBoolean(9, log.isCollaborativeDispatch)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            logger.debug("Could not persist to ai_event_dispatch_log table (using memory store): ${e.message}")
        }
    }
}
