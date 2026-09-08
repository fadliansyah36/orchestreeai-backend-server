package ai.orchestree.backend.scheduler.jobs

import ai.orchestree.backend.memory.DecayExecutionSummary
import ai.orchestree.backend.memory.MemoryDecayEngine
import org.slf4j.LoggerFactory

/**
 * Job berkala untuk mengeksekusi decay otomatis memori lama (PRD Master Bagian 17.2 & 16.3).
 * Mengurangi bobot relevansi memori episodic (>90 hari) dan competitive (>365 hari),
 * serta mengarsipkan dokumen yang bobot relevansinya turun di bawah 0.05.
 */
class MemoryDecayJob(
    private val decayEngine: MemoryDecayEngine = MemoryDecayEngine()
) {
    private val logger = LoggerFactory.getLogger(MemoryDecayJob::class.java)

    suspend fun execute(tenantId: String? = null): String {
        logger.info("[JOB:MEMORY_DECAY] Memulai job scheduled decay memori...")
        val summary: DecayExecutionSummary = decayEngine.applyMemoryDecay(tenantId)
        val result = "Memori diproses: ${summary.totalProcessed}, Bobot didecay: ${summary.totalDecayed}, Diarsipkan: ${summary.totalArchived}"
        logger.info("[JOB:MEMORY_DECAY] Selesai - $result")
        return result
    }
}
