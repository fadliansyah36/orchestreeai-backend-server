package ai.orchestree.backend.governance

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class DataQualityIssue(
    val id: String = "dqi-" + UUID.randomUUID().toString().take(8),
    val tenantId: String,
    val entityType: String, // INVENTORY, FLEET, ASSET_MAINTENANCE, PO, INVOICE
    val entityReference: String,
    val issueType: String, // INCONSISTENT_INVENTORY_RECORD, DISCREPANCY_ERP_PHYSICAL, MISSING_TELEMETRY, STALE_DATA
    val severity: String = "MEDIUM", // LOW, MEDIUM, HIGH, CRITICAL
    val description: String,
    val detectedByAgent: String,
    val sourceSystemsInvolved: List<String> = emptyList(),
    val conflictingValues: Map<String, String> = emptyMap(),
    val status: String = "OPEN", // OPEN, RESOLVED
    val resolvedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ConflictDetectionRequest(
    val tenantId: String,
    val entityType: String,
    val entityReference: String,
    val detectedByAgent: String,
    val sourceA: String,
    val valueA: String,
    val sourceB: String,
    val valueB: String,
    val thresholdTolerancePercent: Double = 0.0
)

class DataQualityGovernanceService {
    private val logger = LoggerFactory.getLogger(DataQualityGovernanceService::class.java)
    private val inMemoryIssues = ConcurrentHashMap<String, CopyOnWriteArrayList<DataQualityIssue>>()

    suspend fun detectAndRecordConflict(request: ConflictDetectionRequest): DataQualityIssue? = withContext(Dispatchers.IO) {
        val valADouble = request.valueA.toDoubleOrNull()
        val valBDouble = request.valueB.toDoubleOrNull()

        val isConflict = if (valADouble != null && valBDouble != null) {
            val diff = Math.abs(valADouble - valBDouble)
            val base = Math.max(valADouble, valBDouble).coerceAtLeast(1.0)
            (diff / base) * 100.0 > request.thresholdTolerancePercent
        } else {
            request.valueA.trim().lowercase() != request.valueB.trim().lowercase()
        }

        if (!isConflict) {
            return@withContext null
        }

        val issue = DataQualityIssue(
            tenantId = request.tenantId,
            entityType = request.entityType,
            entityReference = request.entityReference,
            issueType = "DISCREPANCY_${request.sourceA.uppercase()}_${request.sourceB.uppercase()}",
            severity = if (request.entityType in listOf("INVOICE", "PAYMENT", "SAFETY_CRITICAL")) "HIGH" else "MEDIUM",
            description = "Discrepancy detected between ${request.sourceA} (${request.valueA}) and ${request.sourceB} (${request.valueB}) for ${request.entityReference}",
            detectedByAgent = request.detectedByAgent,
            sourceSystemsInvolved = listOf(request.sourceA, request.sourceB),
            conflictingValues = mapOf(request.sourceA to request.valueA, request.sourceB to request.valueB),
            status = "OPEN"
        )

        // Cache in memory
        val list = inMemoryIssues.computeIfAbsent(request.tenantId) { CopyOnWriteArrayList() }
        list.add(0, issue)

        // Persist to DB
        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        INSERT INTO data_quality_issues
                        (id, tenant_id, entity_type, entity_reference, issue_type, severity, description, 
                         detected_by_agent, source_systems_involved, conflicting_values_json, status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
                    """.trimIndent()).use { ps ->
                        ps.setString(1, issue.id)
                        ps.setString(2, issue.tenantId)
                        ps.setString(3, issue.entityType)
                        ps.setString(4, issue.entityReference)
                        ps.setString(5, issue.issueType)
                        ps.setString(6, issue.severity)
                        ps.setString(7, issue.description)
                        ps.setString(8, issue.detectedByAgent)
                        ps.setString(9, "[\"${request.sourceA}\", \"${request.sourceB}\"]")
                        ps.setString(10, "{\"${request.sourceA}\": \"${request.valueA}\", \"${request.sourceB}\": \"${request.valueB}\"}")
                        ps.setString(11, issue.status)
                        ps.setLong(12, issue.createdAt)
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not insert data_quality_issue to DB: ${e.message}")
            }
        }

        issue
    }

    suspend fun getIssues(tenantId: String, status: String? = null): List<DataQualityIssue> = withContext(Dispatchers.IO) {
        val result = mutableListOf<DataQualityIssue>()
        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val query = if (status != null) {
                        "SELECT id, tenant_id, entity_type, entity_reference, issue_type, severity, description, detected_by_agent, status, resolved_at, created_at FROM data_quality_issues WHERE tenant_id = ? AND status = ? ORDER BY created_at DESC"
                    } else {
                        "SELECT id, tenant_id, entity_type, entity_reference, issue_type, severity, description, detected_by_agent, status, resolved_at, created_at FROM data_quality_issues WHERE tenant_id = ? ORDER BY created_at DESC"
                    }
                    c.prepareStatement(query).use { ps ->
                        ps.setString(1, tenantId)
                        if (status != null) ps.setString(2, status)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                result.add(
                                    DataQualityIssue(
                                        id = rs.getString("id"),
                                        tenantId = rs.getString("tenant_id"),
                                        entityType = rs.getString("entity_type"),
                                        entityReference = rs.getString("entity_reference"),
                                        issueType = rs.getString("issue_type"),
                                        severity = rs.getString("severity"),
                                        description = rs.getString("description"),
                                        detectedByAgent = rs.getString("detected_by_agent"),
                                        status = rs.getString("status"),
                                        resolvedAt = rs.getLong("resolved_at").takeIf { !rs.wasNull() },
                                        createdAt = rs.getLong("created_at")
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying data_quality_issues from DB: ${e.message}")
            }
        }

        if (result.isEmpty()) {
            val cached = inMemoryIssues[tenantId] ?: emptyList<DataQualityIssue>()
            return@withContext if (status != null) cached.filter { it.status == status } else cached
        }
        result
    }

    suspend fun resolveIssue(tenantId: String, issueId: String, resolvedBy: String): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        var updated = false

        // Update in-memory
        inMemoryIssues[tenantId]?.let { list ->
            val idx = list.indexOfFirst { it.id == issueId }
            if (idx != -1) {
                val cur = list[idx]
                list[idx] = cur.copy(status = "RESOLVED", resolvedAt = now)
                updated = true
            }
        }

        // Update DB
        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("UPDATE data_quality_issues SET status = 'RESOLVED', resolved_at = ? WHERE tenant_id = ? AND id = ?").use { ps ->
                        ps.setLong(1, now)
                        ps.setString(2, tenantId)
                        ps.setString(3, issueId)
                        updated = ps.executeUpdate() > 0 || updated
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error resolving data_quality_issue in DB: ${e.message}")
            }
        }
        updated
    }
}
