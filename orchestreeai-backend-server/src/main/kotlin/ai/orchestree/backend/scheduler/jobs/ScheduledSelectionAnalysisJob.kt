package ai.orchestree.backend.scheduler.jobs

import ai.orchestree.backend.database.repositories.selection.AutoSelectionConfigRepository
import ai.orchestree.backend.database.repositories.selection.SelectionRepository
import ai.orchestree.backend.database.repositories.selection.SelectionRequestRecord
import ai.orchestree.backend.database.repositories.selection.SelectionSourceDocumentRecord
import ai.orchestree.backend.intelligence.SelectionEngine
import ai.orchestree.backend.intelligence.SelectionAiJobTitleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * LANGKAH 2.1: Scheduled Selection Analysis Job (Cron, reuse SchedulerEngine Fase 99).
 * Mengeksekusi analisis dan pemeringkatan dataset seleksi secara terjadwal
 * untuk tenant yang mengaktifkan fitur Auto-Selection.
 */
class ScheduledSelectionAnalysisJob(
    private val selectionEngine: SelectionEngine = SelectionEngine(),
    private val selectionRepo: SelectionRepository = SelectionRepository(),
    private val autoSelectionRepo: AutoSelectionConfigRepository = AutoSelectionConfigRepository.defaultInstance
) {
    private val logger = LoggerFactory.getLogger(ScheduledSelectionAnalysisJob::class.java)

    suspend fun execute(tenantId: String, payload: Map<String, Any> = emptyMap()): String = withContext(Dispatchers.IO) {
        logger.info("[SCHEDULER:SELECTION] Running scheduled selection analysis for tenant: $tenantId")

        val configs = autoSelectionRepo.getConfigsForTenant(tenantId).filter { it.isEnabled }
        if (configs.isEmpty()) {
            val msg = "No active auto-selection configs found for tenant $tenantId"
            logger.info(msg)
            return@withContext msg
        }

        var processedCount = 0
        val pendingRequests = selectionRepo.listSelectionRequests(tenantId).filter { it.status == "pending" || it.status == "scheduled" }

        for (req in pendingRequests) {
            try {
                logger.info("Executing scheduled selection request: ${req.id}")
                // Eksekusi prompt-only atau document-based jika ada
                if (req.source_type == "prompt_only") {
                    selectionEngine.processPromptOnlySelection(req.id, tenantId, req.prompt_text)
                    processedCount++
                }
            } catch (e: Exception) {
                logger.error("Failed processing scheduled selection request ${req.id}: ${e.message}", e)
            }
        }

        val summary = "Scheduled selection completed for tenant $tenantId: ${configs.size} folder configs active, $processedCount requests processed."
        logger.info(summary)
        summary
    }
}
