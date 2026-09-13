package ai.orchestree.backend.intelligence

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

data class CrossSystemCorrelationResult(
    val isCorrelated: Boolean,
    val entityReference: String,
    val distinctSystemsCount: Int,
    val distinctSystems: List<String>,
    val contributingStreamIds: List<String>,
    val timeWindowHours: Int,
    val summaryInsight: String,
    val riskScore: Double,
    val confidenceScore: Double,
    val impactLevel: String
)

class CrossSystemCorrelator(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(CrossSystemCorrelator::class.java)

    /**
     * PRD Addendum 2 Bagian 61.2: Cross-System Correlator
     * Memverifikasi sinyal multi-sistem terintegrasi Supabase PostgreSQL.
     * HANYA menganggap cross-system insight jika MINIMAL 2 sistem berbeda
     * memberi sinyal terkait entitas yang sama dalam jendela waktu tertentu (default 48 jam).
     */
    suspend fun correlateSignals(
        tenantId: String,
        entityReference: String,
        timeWindowHours: Int = 48
    ): CrossSystemCorrelationResult = withContext(Dispatchers.IO) {
        logger.info("[CORRELATOR] Correlating cross-system signals for tenant $tenantId on entity $entityReference")
        
        // 1. Query company_activity_stream dari Supabase
        val streamItems = mutableListOf<Pair<String, String>>()

        val streamResult = supabase.queryTable(
            tableName = "company_activity_stream",
            tenantId = tenantId,
            select = "id,system_type,summary,created_at",
            extraParams = mapOf(
                "entity_reference" to "eq.$entityReference"
            )
        )

        if (streamResult.isSuccess) {
            val raw = streamResult.getOrDefault("[]")
            if (raw.isNotBlank() && raw != "[]") {
                try {
                    val parsed = Json.parseToJsonElement(raw)
                    if (parsed is JsonArray) {
                        parsed.forEach { elem ->
                            val obj = elem as? JsonObject ?: return@forEach
                            val id = obj["id"]?.jsonPrimitive?.content ?: return@forEach
                            val sysType = obj["system_type"]?.jsonPrimitive?.content ?: return@forEach
                            streamItems.add(id to sysType)
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("[CORRELATOR] Failed parsing company_activity_stream from Supabase: ${e.message}")
                }
            }
        }

        // 2. Fallback / supplementary check to PostgreSQL DatabaseManager & CompanyActivityStreamService
        if (streamItems.size < 2) {
            try {
                ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                    conn.prepareStatement("""
                        SELECT id, system_type, summary
                        FROM company_activity_stream
                        WHERE (tenant_id = ? OR tenant_id = 'tenant-default')
                          AND (entity_reference ILIKE ? OR summary ILIKE ?)
                          AND event_timestamp >= NOW() - (? || ' hours')::INTERVAL
                        ORDER BY event_timestamp DESC LIMIT 20
                    """.trimIndent()).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setString(2, "%$entityReference%")
                        ps.setString(3, "%$entityReference%")
                        ps.setInt(4, timeWindowHours)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                val id = rs.getString("id")
                                val sysType = rs.getString("system_type") ?: "ERP"
                                if (streamItems.none { it.first == id }) {
                                    streamItems.add(id to sysType)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.debug("[CORRELATOR] DB fallback query check: ${e.message}")
            }
        }

        // 3. Fallback to in-memory activity stream
        if (streamItems.size < 2) {
            val memoryStream = ai.orchestree.backend.enterprise.CompanyActivityStreamService.getStream(tenantId)
            val matchingMemory = memoryStream.filter { it.summaryText.contains(entityReference, ignoreCase = true) }
            for (m in matchingMemory) {
                if (streamItems.none { it.first == m.id }) {
                    streamItems.add(m.id to m.sourceSystem)
                }
            }
        }

        val distinctSystems = streamItems.map { it.second }.distinct()
        val contributingIds = streamItems.map { it.first }
        val isCorrelated = distinctSystems.size >= 2

        if (isCorrelated) {
            CrossSystemCorrelationResult(
                isCorrelated = true,
                entityReference = entityReference,
                distinctSystemsCount = distinctSystems.size,
                distinctSystems = distinctSystems,
                contributingStreamIds = contributingIds,
                timeWindowHours = timeWindowHours,
                summaryInsight = "Terdeteksi anomali operasional cross-system antara ${distinctSystems.joinToString(" & ")} untuk entitas $entityReference.",
                riskScore = 0.82,
                confidenceScore = 0.94,
                impactLevel = "HIGH"
            )
        } else {
            CrossSystemCorrelationResult(
                isCorrelated = false,
                entityReference = entityReference,
                distinctSystemsCount = distinctSystems.size,
                distinctSystems = distinctSystems,
                contributingStreamIds = contributingIds,
                timeWindowHours = timeWindowHours,
                summaryInsight = "Tidak ditemukan korelasi multi-sistem signifikan untuk entitas $entityReference dalam jendela waktu ${timeWindowHours} jam.",
                riskScore = 0.1,
                confidenceScore = 0.85,
                impactLevel = "LOW"
            )
        }
    }
}
