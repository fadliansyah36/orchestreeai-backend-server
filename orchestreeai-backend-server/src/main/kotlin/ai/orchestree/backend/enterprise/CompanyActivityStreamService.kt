package ai.orchestree.backend.enterprise

import ai.orchestree.backend.api.EnterpriseActivityStreamItem
import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.security.AuditLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Company Activity Stream Service (PRD Addendum 2 Bagian 62, 78.1)
 * REUSE event bus/audit log existing sebagai sumber feed, JANGAN buat sistem event kedua.
 */
object CompanyActivityStreamService {
    private val logger = LoggerFactory.getLogger(CompanyActivityStreamService::class.java)
    private val supabase = SupabaseClientProvider.fromEnv()
    val inMemoryStream = ConcurrentHashMap<String, CopyOnWriteArrayList<EnterpriseActivityStreamItem>>()

    /**
     * Synchronous in-memory access for quick context resolution.
     */
    fun getRecentActivitiesSync(tenantId: String, limit: Int = 20): List<EnterpriseActivityStreamItem> {
        val inMem = inMemoryStream[tenantId] ?: inMemoryStream["tenant-default"] ?: emptyList()
        return inMem.take(limit)
    }

    /**
     * Publish an activity event into company_activity_stream AND reuse AuditLogger.
     */
    suspend fun publishActivity(
        tenantId: String,
        systemType: String,
        summary: String,
        entityReference: String,
        activityType: String = "RECORD_SYNC",
        metadata: Map<String, String> = emptyMap(),
        auditLogger: AuditLogger? = null
    ): EnterpriseActivityStreamItem = withContext(Dispatchers.IO) {
        val streamId = "act-${UUID.randomUUID().toString().take(12)}"
        val now = System.currentTimeMillis()

        // 1. Reuse existing AuditLogger (PRD Bagian 62.1: Reuse event bus / audit log)
        auditLogger?.log(
            tenantId = tenantId,
            actor = systemType,
            action = "ACTIVITY_STREAM_$activityType",
            details = "[$entityReference] $summary",
            status = "SUCCESS"
        )

        val item = EnterpriseActivityStreamItem(
            id = streamId,
            sourceSystem = systemType,
            summaryText = summary,
            occurredAt = now
        )

        // 2. Cache in memory
        inMemoryStream.getOrPut(tenantId) { CopyOnWriteArrayList() }.add(0, item)

        // 3. Persist to Postgres database
        try {
            DatabaseManager.getConnection()?.use { conn ->
                conn.prepareStatement("""
                    INSERT INTO company_activity_stream (
                        id, tenant_id, connection_id, system_type, entity_reference, 
                        activity_type, summary, data_mode, event_timestamp, metadata_json, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'NEAR_REALTIME', NOW(), ?::jsonb, NOW())
                    ON CONFLICT (id) DO NOTHING
                """.trimIndent()).use { ps ->
                    ps.setString(1, streamId)
                    ps.setString(2, tenantId)
                    ps.setString(3, "conn-$systemType".lowercase())
                    ps.setString(4, systemType)
                    ps.setString(5, entityReference)
                    ps.setString(6, activityType)
                    ps.setString(7, summary)
                    val metaJson = buildJsonObject {
                        metadata.forEach { (k, v) -> put(k, v) }
                    }.toString()
                    ps.setString(8, metaJson)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            logger.debug("[ACTIVITY_STREAM] Database insert fallback to memory: ${e.message}")
        }

        // 4. Also backup to Supabase query table if configured
        try {
            if (supabase.isConfigured()) {
                val payload = buildJsonObject {
                    put("id", streamId)
                    put("tenant_id", tenantId)
                    put("connection_id", "conn-$systemType".lowercase())
                    put("system_type", systemType)
                    put("entity_reference", entityReference)
                    put("activity_type", activityType)
                    put("summary", summary)
                }.toString()
                supabase.insertRecord("company_activity_stream", tenantId, payload)
            }
        } catch (_: Exception) {}

        item
    }

    /**
     * Retrieve activity stream for a tenant, combining database records and audit log events.
     */
    suspend fun getStream(
        tenantId: String,
        limit: Int = 30,
        auditLogger: AuditLogger? = null
    ): List<EnterpriseActivityStreamItem> = getStreamPaginated(tenantId, limit, 0, auditLogger).second

    /**
     * Retrieve activity stream for a tenant with pagination (LIMIT/OFFSET on SQL level).
     */
    suspend fun getStreamPaginated(
        tenantId: String,
        limit: Int = 20,
        offset: Int = 0,
        auditLogger: AuditLogger? = null
    ): Pair<Long, List<EnterpriseActivityStreamItem>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<EnterpriseActivityStreamItem>()
        var count = 0L

        // 1. Query from company_activity_stream database table using SQL count and LIMIT/OFFSET
        try {
            DatabaseManager.getConnection()?.use { conn ->
                conn.prepareStatement("""
                    SELECT count(*)
                    FROM company_activity_stream
                    WHERE tenant_id = ? OR tenant_id = 'tenant-default'
                """.trimIndent()).use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) count = rs.getLong(1)
                    }
                }

                if (count > 0L) {
                    conn.prepareStatement("""
                        SELECT id, system_type, summary, EXTRACT(EPOCH FROM event_timestamp) * 1000 AS occurred_at
                        FROM company_activity_stream
                        WHERE tenant_id = ? OR tenant_id = 'tenant-default'
                        ORDER BY event_timestamp DESC LIMIT ? OFFSET ?
                    """.trimIndent()).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setInt(2, limit)
                        ps.setInt(3, offset)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                results.add(
                                    EnterpriseActivityStreamItem(
                                        id = rs.getString("id"),
                                        sourceSystem = rs.getString("system_type") ?: "ERP",
                                        summaryText = rs.getString("summary") ?: "",
                                        occurredAt = rs.getLong("occurred_at").takeIf { it > 0 } ?: System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                    return@withContext Pair(count, results)
                }
            }
        } catch (e: Exception) {
            logger.debug("[ACTIVITY_STREAM] DB fetch error: ${e.message}")
        }

        // 2. Combine with in-memory stream if DB had no records
        val inMem = inMemoryStream[tenantId] ?: emptyList<EnterpriseActivityStreamItem>()
        for (m in inMem) {
            if (results.none { it.id == m.id }) {
                results.add(m)
            }
        }

        // 3. Reuse AuditLogger feed if stream is still empty
        if (results.isEmpty() && auditLogger != null) {
            val auditItems = auditLogger.inMemoryLogs
                .filter { it.tenantId == tenantId || it.tenantId == "system" }
                .map { entry ->
                    EnterpriseActivityStreamItem(
                        id = entry.id,
                        sourceSystem = entry.actor,
                        summaryText = "${entry.action}: ${entry.details}",
                        occurredAt = entry.timestamp
                    )
                }
            results.addAll(auditItems)
        }

        // 4. Default seed if tenant has no activity yet and offset == 0
        if (results.isEmpty() && offset == 0) {
            val now = System.currentTimeMillis()
            results.add(
                EnterpriseActivityStreamItem(
                    id = "act-seed-01",
                    sourceSystem = "SAP_ERP",
                    summaryText = "Purchase Order PO-2026-0891 synced ($12,450.00)",
                    occurredAt = now - 120_000
                )
            )
            results.add(
                EnterpriseActivityStreamItem(
                    id = "act-seed-02",
                    sourceSystem = "SALESFORCE_CRM",
                    summaryText = "Lead 'Budi Santoso' status changed to QUALIFIED",
                    occurredAt = now - 600_000
                )
            )
            results.add(
                EnterpriseActivityStreamItem(
                    id = "act-seed-03",
                    sourceSystem = "ZOHO_INVENTORY",
                    summaryText = "Low stock alert: SKU-WIDGET-99 below reorder point",
                    occurredAt = now - 1_800_000
                )
            )
            return@withContext Pair(results.size.toLong(), results.take(limit))
        }

        val total = results.size.toLong()
        val paged = results.drop(offset).take(limit)
        Pair(total, paged)
    }
}
