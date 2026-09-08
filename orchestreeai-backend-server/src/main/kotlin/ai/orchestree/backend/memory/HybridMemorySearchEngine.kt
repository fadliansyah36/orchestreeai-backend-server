package ai.orchestree.backend.memory

import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRecord
import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRepository
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

@Serializable
data class RankedMemoryDocument(
    val document: MemoryDocumentRecord,
    val vectorSimilarity: Double,
    val freshnessScore: Double,
    val weightedFreshness: Double,
    val specificityScore: Double,
    val compositeScore: Double,
    val rank: Int
)

@Serializable
data class SearchRetrievalResult(
    val query: String,
    val totalCandidatesEvaluated: Int,
    val isReranked: Boolean,
    val topResults: List<RankedMemoryDocument>,
    val executionTimeMs: Long
)

@Serializable
data class ABComparisonResult(
    val query: String,
    val pureVectorTop5: List<RankedMemoryDocument>,
    val rerankedTop5: List<RankedMemoryDocument>,
    val removedOutdatedIds: List<String>,
    val relevanceShiftSummary: String
)

/**
 * Hybrid Memory Search Engine with Re-Ranking (PRD Master Bagian 17.2 & PRD Fase 66 Langkah 4.2).
 *
 * Mengambil Top-20 kandidat vector similarity, kemudian melakukan Re-Ranking menggunakan
 * skor gabungan: (relevansi + freshness * relevance_weight + specificity),
 * dan mengembalikan HANYA Top-5 paling relevan dan segar untuk diinjeksikan ke konteks LLM.
 */
class HybridMemorySearchEngine(
    private val memoryRepo: MemoryDocumentRepository = MemoryDocumentRepository()
) {
    private val logger = LoggerFactory.getLogger(HybridMemorySearchEngine::class.java)

    companion object {
        const val CANDIDATE_LIMIT = 20
        const val FINAL_TOP_K = 5
    }

    /**
     * Melakukan pencarian memori dengan opsi Re-Ranking (Top-20 -> Re-rank -> Top-5).
     */
    suspend fun search(
        tenantId: String,
        query: String,
        enableReranking: Boolean = true,
        referenceTime: Instant = Instant.now()
    ): SearchRetrievalResult {
        val startTime = System.currentTimeMillis()
        val activeDocs = memoryRepo.getAllActive(tenantId)

        if (activeDocs.isEmpty()) {
            return SearchRetrievalResult(
                query = query,
                totalCandidatesEvaluated = 0,
                isReranked = enableReranking,
                topResults = emptyList(),
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // 1. Tahap 1: Hitung Vector / Lexical Similarity dan ambil Top-20 kandidat
        val candidatePool = activeDocs.map { doc ->
            val similarity = computeSimilarity(query, doc)
            val ageInDays = Duration.between(Instant.ofEpochMilli(doc.createdAt), referenceTime).toDays().coerceAtLeast(0L)
            val freshness = 1.0 / (1.0 + (ageInDays.toDouble() / 30.0))
            val weightedFreshness = freshness * doc.relevanceWeight
            val specificity = doc.specificityScore

            // Skor gabungan Re-ranking (Fase 66 Langkah 4.2)
            val compositeScore = (similarity * 0.50) + (weightedFreshness * 0.30) + (specificity * 0.20)

            RankedMemoryDocument(
                document = doc,
                vectorSimilarity = similarity,
                freshnessScore = freshness,
                weightedFreshness = weightedFreshness,
                specificityScore = specificity,
                compositeScore = compositeScore,
                rank = 0
            )
        }
            .sortedByDescending { it.vectorSimilarity }
            .take(CANDIDATE_LIMIT)

        // 2. Tahap 2: Re-Ranking (atau tetap Vector murni jika dimatikan)
        val finalResults = if (enableReranking) {
            candidatePool
                .sortedByDescending { it.compositeScore }
                .take(FINAL_TOP_K)
                .mapIndexed { index, item -> item.copy(rank = index + 1) }
        } else {
            candidatePool
                .take(FINAL_TOP_K)
                .mapIndexed { index, item -> item.copy(rank = index + 1) }
        }

        val duration = System.currentTimeMillis() - startTime
        logger.info("[HYBRID_SEARCH] Query: '$query' | Kandidat: ${candidatePool.size} | Top-K: ${finalResults.size} | Re-ranked: $enableReranking | Waktu: ${duration}ms")

        return SearchRetrievalResult(
            query = query,
            totalCandidatesEvaluated = candidatePool.size,
            isReranked = enableReranking,
            topResults = finalResults,
            executionTimeMs = duration
        )
    }

    /**
     * Pengujian A/B: Membandingkan retrieval Top-5 murni vector similarity vs Top-5 hasil Re-Ranking.
     */
    suspend fun runABComparison(
        tenantId: String,
        query: String,
        referenceTime: Instant = Instant.now()
    ): ABComparisonResult {
        val pureVector = search(tenantId, query, enableReranking = false, referenceTime = referenceTime)
        val reranked = search(tenantId, query, enableReranking = true, referenceTime = referenceTime)

        val pureIds = pureVector.topResults.map { it.document.id }
        val rerankedIds = reranked.topResults.map { it.document.id }

        // Dokumen yang berada di Top-5 Vector murni tetapi terdepak oleh Re-Ranking (misal karena usang / relevance_weight rendah)
        val displacedDocs = pureVector.topResults.filter { it.document.id !in rerankedIds }
        val displacedIds = displacedDocs.map { it.document.id }

        val summary = if (displacedDocs.isNotEmpty()) {
            val names = displacedDocs.joinToString(", ") { "${it.document.title} (Relevance: ${it.document.relevanceWeight}, Usia: ${it.freshnessScore})" }
            "Re-Ranking berhasil mendepak dokumen usang/berbobot rendah dari Top-5: $names"
        } else {
            "Peringkat disesuaikan secara dinamis berdasarkan kesegaran dan bobot relevansi."
        }

        return ABComparisonResult(
            query = query,
            pureVectorTop5 = pureVector.topResults,
            rerankedTop5 = reranked.topResults,
            removedOutdatedIds = displacedIds,
            relevanceShiftSummary = summary
        )
    }

    /**
     * Menghitung kemiripan teks/vektor antara query dan dokumen.
     */
    private fun computeSimilarity(query: String, doc: MemoryDocumentRecord): Double {
        val queryTokens = query.lowercase().split("\\s+".toRegex()).filter { it.length > 2 }.toSet()
        if (queryTokens.isEmpty()) return 0.1

        val titleTokens = doc.title.lowercase().split("\\s+".toRegex()).filter { it.length > 2 }.toSet()
        val contentTokens = doc.content.lowercase().split("\\s+".toRegex()).filter { it.length > 2 }.toSet()
        val allDocTokens = titleTokens + contentTokens

        if (allDocTokens.isEmpty()) return 0.0

        val overlap = queryTokens.intersect(allDocTokens).size
        val titleBonus = if (queryTokens.intersect(titleTokens).isNotEmpty()) 0.25 else 0.0

        val baseSimilarity = (overlap.toDouble() / queryTokens.size.toDouble()).coerceIn(0.0, 1.0)
        return (baseSimilarity * 0.75 + titleBonus).coerceIn(0.05, 0.99)
    }
}
