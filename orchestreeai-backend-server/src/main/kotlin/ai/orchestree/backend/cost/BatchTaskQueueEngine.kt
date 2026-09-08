package ai.orchestree.backend.cost

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class QueuedBatchItem(
    val itemId: String,
    val tenantId: String,
    val taskType: String,
    val payloadJson: String,
    val priority: Int = 1,
    val queuedAt: Long = System.currentTimeMillis()
)

object BatchTaskQueueEngine {
    private val logger = LoggerFactory.getLogger(BatchTaskQueueEngine::class.java)

    /**
     * Memasukkan pekerjaan ke batch queue dengan diskon biaya LLM batch API (50% cheaper off-peak processing).
     */
    suspend fun enqueueBatchTask(
        tenantId: String,
        taskType: String,
        payloadJson: String,
        priority: Int = 1
    ): String = withContext(Dispatchers.IO) {
        val itemId = "batch-" + UUID.randomUUID().toString().take(8)
        val conn = DatabaseManager.getConnection()

        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        INSERT INTO batch_tasks_queue 
                        (id, tenant_id, task_type, payload_json, priority, status, created_at)
                        VALUES (?, ?, ?, ?, ?, 'QUEUED', ?)
                    """.trimIndent()).use { ps ->
                        ps.setString(1, itemId)
                        ps.setString(2, tenantId)
                        ps.setString(3, taskType)
                        ps.setString(4, payloadJson)
                        ps.setInt(5, priority)
                        ps.setLong(6, System.currentTimeMillis())
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not insert into batch_tasks_queue: ${e.message}")
            }
        }

        itemId
    }
}
