package ai.orchestree.backend.memory

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class SearchMatch(
    val id: String,
    val tenantId: String,
    val content: String,
    val similarityScore: Double,
    val metadata: Map<String, String> = emptyMap()
)

class HybridSearchEngine(
    private val supabase: SupabaseClientProvider? = null,
    private val memoryService: MemoryService = MemoryService()
) {
    private val logger = LoggerFactory.getLogger(HybridSearchEngine::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * LANGKAH 3.1: Hybrid Search PERSIS PRD Bagian 17.2
     * kNN pgvector (HNSW Index) + Full-Text PostgreSQL + Reciprocal Rank Fusion (RRF)
     */
    suspend fun search(
        tenantId: String,
        query: String,
        topK: Int = 5,
        queryEmbedding: List<Double>? = null
    ): List<MemoryDocument> {
        // 1. If Supabase is configured, call match_documents RPC with vector + text
        if (supabase != null && supabase.isConfigured()) {
            try {
                val embeddingArrayStr = queryEmbedding?.joinToString(prefix = "[", postfix = "]", separator = ",") ?: "[]"
                val rpcPayload = """
                    {
                        "query_text": "${query.replace("\"", "\\\"")}",
                        "query_embedding": $embeddingArrayStr,
                        "filter_tenant_id": "$tenantId",
                        "match_count": ${topK * 2}
                    }
                """.trimIndent()

                val rpcResult = supabase.callRpc("hybrid_match_documents", rpcPayload, tenantId)
                if (rpcResult.isSuccess) {
                    val rawJson = rpcResult.getOrThrow()
                    val matches = try {
                        json.decodeFromString<List<SearchMatch>>(rawJson)
                    } catch (e: Exception) {
                        emptyList()
                    }

                    if (matches.isNotEmpty()) {
                        logger.info("HybridSearch (pgvector + FTS) returned ${matches.size} items for tenant $tenantId")
                        return matches.take(topK).map { match ->
                            MemoryDocument(
                                id = match.id,
                                tenantId = match.tenantId,
                                sourceType = "COMPANY_BRAIN",
                                content = match.content,
                                metadata = match.metadata
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Supabase hybrid search RPC failed (${e.message}), falling back to in-memory RRF engine")
            }
        }

        // 2. In-Memory RRF Hybrid Search (Full-Text Token + Lexical Ranks)
        return performReciprocalRankFusion(tenantId, query, topK)
    }

    /**
     * Reciprocal Rank Fusion (RRF) implementation: score(d) = sum(1.0 / (k + rank_i(d)))
     */
    private fun performReciprocalRankFusion(
        tenantId: String,
        query: String,
        topK: Int,
        rrfK: Int = 60
    ): List<MemoryDocument> {
        val docs = memoryService.queryByTenant(tenantId)
        if (docs.isEmpty()) return emptyList()

        val queryTokens = query.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }

        // Rank 1: Exact Substring / Token Count Matching
        val textRanked = docs.map { doc ->
            val docText = doc.content.lowercase()
            val score = queryTokens.count { docText.contains(it) }
            doc to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }

        // Rank 2: Length & Recency Weighted Ranking
        val recencyRanked = docs.sortedByDescending { it.timestamp }

        // Combine using RRF
        val rrfScores = mutableMapOf<String, Double>()
        val docMap = docs.associateBy { it.id }

        textRanked.forEachIndexed { index, doc ->
            val rank = index + 1
            rrfScores[doc.id] = rrfScores.getOrDefault(doc.id, 0.0) + (1.0 / (rrfK + rank))
        }

        recencyRanked.forEachIndexed { index, doc ->
            val rank = index + 1
            rrfScores[doc.id] = rrfScores.getOrDefault(doc.id, 0.0) + (0.5 / (rrfK + rank))
        }

        return rrfScores.entries
            .sortedByDescending { it.value }
            .take(topK)
            .mapNotNull { docMap[it.key] }
    }
}
