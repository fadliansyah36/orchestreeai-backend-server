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
            } catch (e: Exception) {
                logger.debug("Supabase insert memory notice: ${e.message}")
            }
        }
        Result.success(doc)
    }

    suspend fun getById(id: String): MemoryDocumentRecord? = withContext(Dispatchers.IO) {
        for (list in inMemoryStore.values) {
            val doc = list.find { it.id == id }
            if (doc != null) return@withContext doc
        }
        null
    }

    suspend fun listByTenant(tenantId: String, includeArchived: Boolean = false): List<MemoryDocumentRecord> = withContext(Dispatchers.IO) {
        val list = inMemoryStore[tenantId]?.toList() ?: emptyList()
        if (includeArchived) list else list.filter { !it.isArchived }
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
        }
    }
}
