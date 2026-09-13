package ai.orchestree.backend.database.repositories.memory

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class MemoryDocumentRecord(
    val id: String = "mem-${UUID.randomUUID().toString().take(8)}",
    val tenantId: String,
    val sourceType: String = "episodic", // "episodic", "competitive", "semantic", "company_context"
    val title: String,
    val content: String,
    val tags: String = "",
    val sourceReference: String = "System / Workflow Engine",
    var relevanceWeight: Double = 1.0,
    var isArchived: Boolean = false,
    val importanceScore: Double = 0.5,
    val noveltyScore: Double = 0.5,
    val specificityScore: Double = 0.5,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    var archivedAt: Long? = null,
    val embedding: List<Double>? = null
)

/**
 * Repository for Memory Documents (PRD Master Bagian 17.2, Fase 66 Langkah 4.2).
 * Supports full lifecycle: consolidation filtering, automated decay, archiving, and hybrid search re-ranking.
 */
class MemoryDocumentRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(MemoryDocumentRepository::class.java)
    private val inMemoryStore = ConcurrentHashMap<String, CopyOnWriteArrayList<MemoryDocumentRecord>>()

    private fun Map<String, Any?>.toSafeJson(): String {
        val entriesStr = entries.joinToString(",") { (k, v) ->
            val vStr = when (v) {
                null -> "null"
                is Number, is Boolean -> "$v"
                else -> "\"${v.toString().replace("\"", "\\\"")}\""
            }
            "\"$k\":$vStr"
        }
        return "{$entriesStr}"
    }

    suspend fun getAllActive(tenantId: String? = null): List<MemoryDocumentRecord> = withContext(Dispatchers.IO) {
        if (tenantId != null) {
            inMemoryStore[tenantId]?.filter { !it.isArchived } ?: emptyList()
        } else {
            inMemoryStore.values.flatten().filter { !it.isArchived }
        }
    }

    suspend fun updateRelevanceWeight(id: String, weight: Double): Boolean = withContext(Dispatchers.IO) {
        for (list in inMemoryStore.values) {
            val doc = list.find { it.id == id }
            if (doc != null) {
                doc.relevanceWeight = weight.coerceIn(0.0, 1.0)
                if (supabase.isConfigured()) {
                    try {
                        val payload = mapOf("relevance_weight" to doc.relevanceWeight)
                        supabase.updateRecord("memory_documents", doc.tenantId, id, payload.toSafeJson())
                    } catch (e: Exception) {
                        logger.debug("Supabase update relevance notice: ${e.message}")
                    }
                }
                return@withContext true
            }
        }
        false
    }

    suspend fun archive(id: String): Boolean = withContext(Dispatchers.IO) {
        for (list in inMemoryStore.values) {
            val doc = list.find { it.id == id }
            if (doc != null) {
                doc.isArchived = true
                doc.archivedAt = System.currentTimeMillis()
                doc.relevanceWeight = 0.0
                if (supabase.isConfigured()) {
                    try {
                        val payload = mapOf(
                            "is_archived" to true,
                            "relevance_weight" to 0.0,
                            "archived_at" to doc.archivedAt
                        )
                        supabase.updateRecord("memory_documents", doc.tenantId, id, payload.toSafeJson())
                    } catch (e: Exception) {
                        logger.debug("Supabase archive memory notice: ${e.message}")
                    }
                }
                logger.info("[MEMORY] Document ${doc.id} (${doc.title}) archived due to decay")
                return@withContext true
            }
        }
        false
    }

    suspend fun upsert(doc: MemoryDocumentRecord): Result<MemoryDocumentRecord> = withContext(Dispatchers.IO) {
        val list = inMemoryStore.computeIfAbsent(doc.tenantId) { CopyOnWriteArrayList() }
        list.removeIf { it.id == doc.id }
        list.add(doc)

        try {
            ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                conn.prepareStatement("""
                    INSERT INTO memory_documents (
                        id, tenant_id, type, source_type, title, content, tags, source_reference,
                        relevance_weight, is_archived, importance_score, novelty_score, specificity_score
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        title = EXCLUDED.title,
                        content = EXCLUDED.content,
                        tags = EXCLUDED.tags,
                        relevance_weight = EXCLUDED.relevance_weight,
                        is_archived = EXCLUDED.is_archived,
                        importance_score = EXCLUDED.importance_score
                """).use { ps ->
                    ps.setString(1, doc.id)
                    ps.setString(2, doc.tenantId)
                    ps.setString(3, doc.sourceType)
                    ps.setString(4, doc.sourceType)
                    ps.setString(5, doc.title)
                    ps.setString(6, doc.content)
                    ps.setString(7, doc.tags)
                    ps.setString(8, doc.sourceReference)
                    ps.setDouble(9, doc.relevanceWeight)
                    ps.setBoolean(10, doc.isArchived)
                    ps.setDouble(11, doc.importanceScore)
                    ps.setDouble(12, doc.noveltyScore)
                    ps.setDouble(13, doc.specificityScore)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            logger.warn("PostgreSQL insert memory notice: ${e.message}")
            if (supabase.isConfigured()) {
                try {
                    val payload = mapOf(
                        "id" to doc.id,
                        "tenant_id" to doc.tenantId,
                        "type" to doc.sourceType,
                        "source_type" to doc.sourceType,
                        "title" to doc.title,
                        "content" to doc.content,
                        "tags" to doc.tags,
                        "source_reference" to doc.sourceReference,
                        "relevance_weight" to doc.relevanceWeight,
                        "is_archived" to doc.isArchived,
                        "importance_score" to doc.importanceScore,
                        "novelty_score" to doc.noveltyScore,
                        "specificity_score" to doc.specificityScore,
                        "metadata_json" to doc.metadata.toSafeJson(),
                        "created_at" to doc.createdAt
                    )
                    supabase.insertRecord("memory_documents", doc.tenantId, payload.toSafeJson())
                } catch (se: Exception) {
                    logger.debug("Supabase insert memory notice: ${se.message}")
                }
            }
        }
        Result.success(doc)
    }

    suspend fun getById(id: String): MemoryDocumentRecord? = withContext(Dispatchers.IO) {
        for (list in inMemoryStore.values) {
            val doc = list.find { it.id == id }
            if (doc != null) return@withContext doc
        }
        try {
            ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                conn.prepareStatement("SELECT id, tenant_id, source_type, title, content, tags, source_reference, relevance_weight, is_archived, importance_score, novelty_score, specificity_score FROM memory_documents WHERE id = ?").use { ps ->
                    ps.setString(1, id)
                    val rs = ps.executeQuery()
                    if (rs.next()) {
                        return@withContext MemoryDocumentRecord(
                            id = rs.getString("id"),
                            tenantId = rs.getString("tenant_id"),
                            sourceType = rs.getString("source_type") ?: "episodic",
                            title = rs.getString("title") ?: "",
                            content = rs.getString("content") ?: "",
                            tags = rs.getString("tags") ?: "",
                            sourceReference = rs.getString("source_reference") ?: "",
                            relevanceWeight = rs.getDouble("relevance_weight"),
                            isArchived = rs.getBoolean("is_archived"),
                            importanceScore = rs.getDouble("importance_score"),
                            noveltyScore = rs.getDouble("novelty_score"),
                            specificityScore = rs.getDouble("specificity_score")
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        null
    }

    suspend fun listByTenant(tenantId: String, includeArchived: Boolean = false): List<MemoryDocumentRecord> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MemoryDocumentRecord>()
        try {
            ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                val sql = if (includeArchived) {
                    "SELECT id, tenant_id, source_type, title, content, tags, source_reference, relevance_weight, is_archived, importance_score, novelty_score, specificity_score FROM memory_documents WHERE tenant_id = ? OR tenant_id = 'tenant-default' ORDER BY id"
                } else {
                    "SELECT id, tenant_id, source_type, title, content, tags, source_reference, relevance_weight, is_archived, importance_score, novelty_score, specificity_score FROM memory_documents WHERE (tenant_id = ? OR tenant_id = 'tenant-default') AND is_archived = false ORDER BY id"
                }
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    val rs = ps.executeQuery()
                    while (rs.next()) {
                        results.add(
                            MemoryDocumentRecord(
                                id = rs.getString("id"),
                                tenantId = rs.getString("tenant_id"),
                                sourceType = rs.getString("source_type") ?: "episodic",
                                title = rs.getString("title") ?: "",
                                content = rs.getString("content") ?: "",
                                tags = rs.getString("tags") ?: "",
                                sourceReference = rs.getString("source_reference") ?: "",
                                relevanceWeight = rs.getDouble("relevance_weight"),
                                isArchived = rs.getBoolean("is_archived"),
                                importanceScore = rs.getDouble("importance_score"),
                                noveltyScore = rs.getDouble("novelty_score"),
                                specificityScore = rs.getDouble("specificity_score")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Query DB memory_documents notice: ${e.message}")
        }

        if (results.isNotEmpty()) {
            return@withContext results
        }

        val inMem = inMemoryStore[tenantId]?.toList() ?: emptyList()
        val filteredInMem = if (includeArchived) inMem else inMem.filter { !it.isArchived }
        if (filteredInMem.isNotEmpty()) return@withContext filteredInMem

        seedSampleDataIfEmpty(tenantId)
        val refreshed = inMemoryStore[tenantId]?.toList() ?: emptyList()
        if (includeArchived) refreshed else refreshed.filter { !it.isArchived }
    }

    fun clear() {
        inMemoryStore.clear()
    }

    fun seedSampleDataIfEmpty(tenantId: String) {
        val existing = inMemoryStore[tenantId]
        if (existing.isNullOrEmpty()) {
            val list = CopyOnWriteArrayList<MemoryDocumentRecord>()
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            // 1. Fresh episodic memory (10 days old)
            list.add(
                MemoryDocumentRecord(
                    id = "mem-fresh-episodic",
                    tenantId = tenantId,
                    sourceType = "episodic",
                    title = "Diskusi Strategi Diskon Q3",
                    content = "Klien PT Maju Bersama meminta diskon volume 15% untuk perpanjangan kontrak tier enterprise.",
                    relevanceWeight = 1.0,
                    importanceScore = 0.85,
                    noveltyScore = 0.80,
                    specificityScore = 0.90,
                    createdAt = now - (10 * dayMs)
                )
            )

            // 2. Old episodic memory (100 days old -> should decay to 0 and archive)
            list.add(
                MemoryDocumentRecord(
                    id = "mem-old-episodic",
                    tenantId = tenantId,
                    sourceType = "episodic",
                    title = "Jadwal Pertemuan Internal Mei",
                    content = "Pertemuan koordinasi sync rutin mingguan tim marketing membahas revisi poster.",
                    relevanceWeight = 1.0,
                    importanceScore = 0.40,
                    noveltyScore = 0.30,
                    specificityScore = 0.40,
                    createdAt = now - (100 * dayMs)
                )
            )

            // 3. Competitive memory (180 days old -> age 180 / 365 => decayedWeight ~0.50)
            list.add(
                MemoryDocumentRecord(
                    id = "mem-mid-competitive",
                    tenantId = tenantId,
                    sourceType = "competitive",
                    title = "Analisis Fitur Kompetitor X",
                    content = "Kompetitor X meluncurkan modul AI lead generation dengan pricing Rp 5.000.000 per bulan.",
                    relevanceWeight = 1.0,
                    importanceScore = 0.90,
                    noveltyScore = 0.75,
                    specificityScore = 0.85,
                    createdAt = now - (180 * dayMs)
                )
            )

            // 4. Semantic / Company Context memory (Permanent, never decays)
            list.add(
                MemoryDocumentRecord(
                    id = "mem-permanent-semantic",
                    tenantId = tenantId,
                    sourceType = "company_context",
                    title = "Kebijakan Privasi dan NDA Perusahaan",
                    content = "Seluruh data pelanggan wajib disimpan di enkripsi AES-256 dan dilarang dibagikan ke pihak ketiga tanpa persetujuan legal.",
                    relevanceWeight = 1.0,
                    importanceScore = 1.0,
                    noveltyScore = 0.95,
                    specificityScore = 0.95,
                    createdAt = now - (400 * dayMs)
                )
            )

            inMemoryStore[tenantId] = list

            try {
                ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                    conn.prepareStatement("""
                        INSERT INTO memory_documents (
                            id, tenant_id, type, source_type, title, content, tags, source_reference,
                            relevance_weight, is_archived, importance_score, novelty_score, specificity_score
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO NOTHING
                    """).use { ps ->
                        list.forEach { item ->
                            ps.setString(1, item.id)
                            ps.setString(2, item.tenantId)
                            ps.setString(3, item.sourceType)
                            ps.setString(4, item.sourceType)
                            ps.setString(5, item.title)
                            ps.setString(6, item.content)
                            ps.setString(7, item.tags)
                            ps.setString(8, item.sourceReference)
                            ps.setDouble(9, item.relevanceWeight)
                            ps.setBoolean(10, item.isArchived)
                            ps.setDouble(11, item.importanceScore)
                            ps.setDouble(12, item.noveltyScore)
                            ps.setDouble(13, item.specificityScore)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Seed DB memory_documents notice: ${e.message}")
            }
        }
    }
}
