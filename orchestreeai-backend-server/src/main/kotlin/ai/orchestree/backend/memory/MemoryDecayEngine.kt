package ai.orchestree.backend.memory

import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRepository
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

@Serializable
data class DecayedDocumentDetail(
    val id: String,
    val title: String,
    val sourceType: String,
    val ageInDays: Long,
    val previousWeight: Double,
    val newWeight: Double,
    val isArchived: Boolean
)

@Serializable
data class DecayExecutionSummary(
    val totalProcessed: Int,
    val totalDecayed: Int,
    val totalArchived: Int,
    val executedAt: Long = System.currentTimeMillis(),
    val details: List<DecayedDocumentDetail> = emptyList()
)

/**
 * Memory Decay Engine (PRD Master Bagian 17.2 & 16.3).
 *
 * Menurunkan relevance_weight memori lama secara bertahap:
 * - Episodic: 90 hari linear decay. Jika usia > 90 hari, bobot menjadi 0.0.
 * - Competitive: 12 bulan (365 hari) rolling decay.
 * - Semantic / Company Context: Permanen (tidak pernah decay, bobot tetap 1.0).
 *
 * Jika relevance_weight <= 0.05, dokumen diarsipkan (archive, bukan hapus).
 */
class MemoryDecayEngine(
    private val memoryDocumentRepo: MemoryDocumentRepository = MemoryDocumentRepository()
) {
    private val logger = LoggerFactory.getLogger(MemoryDecayEngine::class.java)

    suspend fun applyMemoryDecay(
        tenantId: String? = null,
        referenceTime: Instant = Instant.now()
    ): DecayExecutionSummary {
        val activeDocs = memoryDocumentRepo.getAllActive(tenantId)
        val details = mutableListOf<DecayedDocumentDetail>()
        var decayedCount = 0
        var archivedCount = 0

        logger.info("[MEMORY_DECAY] Memulai proses decay untuk ${activeDocs.size} dokumen memori aktif (tenant: $tenantId)...")

        for (doc in activeDocs) {
            val createdAtInstant = Instant.ofEpochMilli(doc.createdAt)
            val ageInDays = Duration.between(createdAtInstant, referenceTime).toDays().coerceAtLeast(0L)
            val oldWeight = doc.relevanceWeight

            val decayedWeight = when (doc.sourceType.lowercase()) {
                "episodic" -> if (ageInDays > 90) 0.0 else (1.0 - (ageInDays.toDouble() / 90.0)).coerceIn(0.0, 1.0)
                "competitive" -> if (ageInDays > 365) 0.0 else (1.0 - (ageInDays.toDouble() / 365.0)).coerceIn(0.0, 1.0)
                else -> 1.0 // company_context / semantic / core policies permanen, tidak decay
            }

            memoryDocumentRepo.updateRelevanceWeight(doc.id, decayedWeight)
            decayedCount++

            var isArchived = false
            if (decayedWeight <= 0.05) {
                memoryDocumentRepo.archive(doc.id) // Arsip, bukan hapus
                archivedCount++
                isArchived = true
                logger.info("[MEMORY_DECAY] Dokumen ${doc.id} (${doc.title}) mencapai bobot ${"%.2f".format(decayedWeight)} <= 0.05 -> DIARSIPKAN.")
            }

            details.add(
                DecayedDocumentDetail(
                    id = doc.id,
                    title = doc.title,
                    sourceType = doc.sourceType,
                    ageInDays = ageInDays,
                    previousWeight = oldWeight,
                    newWeight = decayedWeight,
                    isArchived = isArchived
                )
            )
        }

        logger.info("[MEMORY_DECAY] Selesai: $decayedCount dokumen diperbarui, $archivedCount dokumen diarsipkan.")

        return DecayExecutionSummary(
            totalProcessed = activeDocs.size,
            totalDecayed = decayedCount,
            totalArchived = archivedCount,
            details = details
        )
    }
}
