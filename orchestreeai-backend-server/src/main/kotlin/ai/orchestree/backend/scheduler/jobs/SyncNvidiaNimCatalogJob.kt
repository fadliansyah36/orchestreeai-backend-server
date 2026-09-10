package ai.orchestree.backend.scheduler.jobs

import ai.orchestree.backend.database.repositories.modelrouter.LlmProviderModelRepository
import org.slf4j.LoggerFactory

/**
 * SyncNvidiaNimCatalogJob
 * Scheduled job to dynamically discover and synchronize the latest NVIDIA NIM model catalog.
 * Runs periodically to ensure models are up-to-date without hardcoding model names.
 */
class SyncNvidiaNimCatalogJob(
    private val modelRepo: LlmProviderModelRepository = LlmProviderModelRepository.instance
) {
    private val logger = LoggerFactory.getLogger(SyncNvidiaNimCatalogJob::class.java)

    suspend fun execute(apiKeyOverride: String? = null): Int {
        logger.info("[SCHEDULER] Executing dynamic NVIDIA NIM model catalog discovery job...")
        val syncedCount = modelRepo.syncNvidiaNimModelCatalog(apiKeyOverride)
        logger.info("[SCHEDULER] NVIDIA NIM catalog discovery job completed. Synced $syncedCount model(s).")
        return syncedCount
    }
}
