package ai.orchestree.backend.memory

import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRecord
import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class CandidateInteraction(
    val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val sender: String = "user",
    val content: String,
    val context: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ConsolidationDecision(
    val interactionId: String,
    val isConsolidated: Boolean,
    val importanceScore: Double,
    val noveltyScore: Double,
    val combinedScore: Double,
    val rejectionReason: String? = null,
    val savedDocumentId: String? = null
)

/**
 * Memory Consolidator (PRD Master Bagian 17.2).
 *
 * Menerapkan threshold kebaruan & kepentingan SEBELUM menulis memory permanen.
 * Menyaring interaksi remeh / obrolan ringan sehari-hari agar tidak membebani database
 * dan tidak mengotori context retrieval LLM.
 */
class MemoryConsolidator(
    private val memoryRepo: MemoryDocumentRepository = MemoryDocumentRepository(),
    private val threshold: Double = 0.60
) {
    private val logger = LoggerFactory.getLogger(MemoryConsolidator::class.java)

    companion object {
        // Frasa dan kata kunci remeh / non-substantif yang tidak layak masuk memori permanen
        private val TRIVIAL_PHRASES = setOf(
            "halo", "hai", "hello", "hi", "selamat pagi", "selamat siang", "selamat sore", "selamat malam",
            "terima kasih", "makasih", "thanks", "thank you", "thx",
            "oke", "ok", "sip", "siap", "siap bos", "noted", "baik", "ya", "iya", "yup", "tes", "test", "testing",
            "apa kabar", "gimana kabar", "lagi apa", "wkwk", "haha", "hehe", "lol", "mantap", "keren"
        )

        // Indikator kepentingan tinggi: keputusan bisnis, SLA, komitmen, angka, diskon, bug, eskalasi
        private val HIGH_IMPORTANCE_INDICATORS = setOf(
            "kontrak", "diskon", "harga", "deal", "perjanjian", "sla", "deadline", "komitmen",
            "anggaran", "budget", "kebijakan", "policy", "nda", "target", "kpi", "revisi", "keputusan",
            "client", "klien", "urgent", "eskalasi", "migrasi", "database", "arsip", "terminasi", "klausul"
        )
    }

    /**
     * Mengevaluasi satu interaksi kandidat dan menentukan apakah layak ditulis ke memori permanen.
     */
    suspend fun evaluateInteraction(interaction: CandidateInteraction): ConsolidationDecision = withContext(Dispatchers.Default) {
        val cleanText = interaction.content.trim().lowercase()

        // 1. Cek langsung apakah interaksi remeh murni
        val isExplicitlyTrivial = isTrivial(cleanText)
        val wordCount = cleanText.split("\\s+".toRegex()).size

        // 2. Hitung Importance Score (0.0 - 1.0)
        val importanceScore = calculateImportance(cleanText, isExplicitlyTrivial, wordCount)

        // 3. Hitung Novelty Score (0.0 - 1.0) terhadap memori aktif yang sudah ada
        val existingDocs = memoryRepo.getAllActive(interaction.tenantId)
        val noveltyScore = calculateNovelty(cleanText, existingDocs)

        // 4. Skor gabungan terbobot (PRD: Importance 60%, Novelty 40%)
        val combinedScore = (importanceScore * 0.60) + (noveltyScore * 0.40)

        // 5. Gate Threshold
        val shouldConsolidate = combinedScore >= threshold && !isExplicitlyTrivial

        if (shouldConsolidate) {
            val title = if (interaction.content.length > 50) interaction.content.take(47) + "..." else interaction.content
            val doc = MemoryDocumentRecord(
                tenantId = interaction.tenantId,
                sourceType = "episodic",
                title = title,
                content = interaction.content,
                relevanceWeight = 1.0,
                importanceScore = importanceScore,
                noveltyScore = noveltyScore,
                specificityScore = (wordCount.toDouble() / 50.0).coerceIn(0.3, 0.95),
                sourceReference = "Consolidator / ${interaction.sender}",
                createdAt = interaction.timestamp
            )
            memoryRepo.upsert(doc)
            logger.info("[CONSOLIDATOR] Interaksi konsolidasi DITERIMA (Skor: ${"%.2f".format(combinedScore)} >= $threshold): ${doc.id}")

            ConsolidationDecision(
                interactionId = interaction.id,
                isConsolidated = true,
                importanceScore = importanceScore,
                noveltyScore = noveltyScore,
                combinedScore = combinedScore,
                savedDocumentId = doc.id
            )
        } else {
            val reason = if (isExplicitlyTrivial) {
                "Interaksi terdeteksi obrolan remeh / filler tanpa nilai substantif (Skor: ${"%.2f".format(combinedScore)} < $threshold)"
            } else {
                "Skor konsolidasi ${"%.2f".format(combinedScore)} di bawah ambang batas minimal $threshold"
            }
            logger.debug("[CONSOLIDATOR] Interaksi DITOLAK: $reason")

            ConsolidationDecision(
                interactionId = interaction.id,
                isConsolidated = false,
                importanceScore = importanceScore,
                noveltyScore = noveltyScore,
                combinedScore = combinedScore,
                rejectionReason = reason
            )
        }
    }

    /**
     * Memproses batch interaksi kandidat.
     */
    suspend fun consolidateBatch(interactions: List<CandidateInteraction>): List<ConsolidationDecision> {
        return interactions.map { evaluateInteraction(it) }
    }

    /**
     * Backward-compatible consolidation trigger.
     */
    fun consolidate(tenantId: String): Int {
        return 0
    }

    private fun isTrivial(text: String): Boolean {
        // Teks kosong atau sangat pendek
        if (text.length < 4) return true

        // Persis sama dengan salah satu frasa remeh
        if (TRIVIAL_PHRASES.contains(text)) return true

        // Frasa remeh dengan tanda baca (misal "halo!!", "oke.")
        val stripped = text.replace("[^a-zA-Z0-9 ]".toRegex(), "").trim()
        if (TRIVIAL_PHRASES.contains(stripped)) return true

        // Hanya terdiri dari kata-kata remeh
        val words = stripped.split("\\s+".toRegex())
        if (words.isNotEmpty() && words.all { TRIVIAL_PHRASES.contains(it) }) return true

        return false
    }

    private fun calculateImportance(cleanText: String, isExplicitlyTrivial: Boolean, wordCount: Int): Double {
        if (isExplicitlyTrivial) return 0.15

        var score = 0.40

        // Bonus jika terdapat indikator substantif bisnis
        val foundIndicators = HIGH_IMPORTANCE_INDICATORS.count { cleanText.contains(it) }
        score += (foundIndicators * 0.15)

        // Bonus jika terdapat angka, persentase, atau uang
        if (Regex("(\\d+%|rp\\s*\\d+|\\$\\d+|\\b\\d{4,}\\b)").containsMatchIn(cleanText)) {
            score += 0.20
        }

        // Penalty jika teks sangat singkat tanpa indikator penting
        if (wordCount < 5 && foundIndicators == 0) {
            score -= 0.25
        }

        return score.coerceIn(0.05, 0.98)
    }

    private fun calculateNovelty(cleanText: String, existingDocs: List<MemoryDocumentRecord>): Double {
        if (existingDocs.isEmpty()) return 0.90

        val words = cleanText.split("\\s+".toRegex()).filter { it.length > 3 }.toSet()
        if (words.isEmpty()) return 0.20

        var maxOverlapRatio = 0.0
        for (doc in existingDocs) {
            val docWords = doc.content.lowercase().split("\\s+".toRegex()).filter { it.length > 3 }.toSet()
            if (docWords.isNotEmpty()) {
                val intersection = words.intersect(docWords).size
                val ratio = intersection.toDouble() / words.size.toDouble()
                if (ratio > maxOverlapRatio) {
                    maxOverlapRatio = ratio
                }
            }
        }

        // Jika overlap sangat tinggi dengan dokumen yang sudah ada, nilai novelty rendah
        val novelty = 1.0 - (maxOverlapRatio * 0.85)
        return novelty.coerceIn(0.10, 0.95)
    }
}
