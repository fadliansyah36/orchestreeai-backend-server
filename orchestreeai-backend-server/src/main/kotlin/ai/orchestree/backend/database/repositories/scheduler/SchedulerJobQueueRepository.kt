package ai.orchestree.backend.database.repositories.scheduler

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.util.UUID

data class SchedulerJobQueueRecord(
    val id: String,
    val tenantId: String?,
    val jobType: String,
    val payload: String = "{}",
    val status: String, // 'pending', 'claimed', 'completed', 'failed'
    val scheduledAt: Long,
    val claimedAt: Long? = null,
    val claimedBy: String? = null,
    val completedAt: Long? = null,
    val executionResult: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

class SchedulerJobQueueRepository {
    private val logger = LoggerFactory.getLogger(SchedulerJobQueueRepository::class.java)

    /**
     * Pastikan tabel scheduler_job_queue ada di PostgreSQL.
     */
    suspend fun ensureTableExists() = withContext(Dispatchers.IO) {
        val conn = DatabaseManager.getConnection() ?: return@withContext
        conn.use { c ->
            c.createStatement().use { st ->
                st.execute("""
                    CREATE TABLE IF NOT EXISTS scheduler_job_queue (
                        id VARCHAR(64) PRIMARY KEY,
                        tenant_id VARCHAR(64),
                        job_type VARCHAR(128) NOT NULL,
                        payload TEXT DEFAULT '{}',
                        status VARCHAR(32) NOT NULL DEFAULT 'pending',
                        scheduled_at BIGINT NOT NULL,
                        claimed_at BIGINT,
                        claimed_by VARCHAR(128),
                        completed_at BIGINT,
                        execution_result TEXT,
                        retry_count INT NOT NULL DEFAULT 0,
                        created_at BIGINT NOT NULL
                    );
                    CREATE INDEX IF NOT EXISTS idx_scheduler_job_queue_status_sched 
                    ON scheduler_job_queue (status, scheduled_at);
                """.trimIndent())
            }
        }
    }

    suspend fun enqueue(record: SchedulerJobQueueRecord): Boolean = withContext(Dispatchers.IO) {
        val conn = DatabaseManager.getConnection() ?: return@withContext false
        conn.use { c ->
            c.prepareStatement("""
                INSERT INTO scheduler_job_queue 
                (id, tenant_id, job_type, payload, status, scheduled_at, retry_count, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET 
                    status = EXCLUDED.status, 
                    scheduled_at = EXCLUDED.scheduled_at
            """.trimIndent()).use { ps ->
                ps.setString(1, record.id)
                ps.setString(2, record.tenantId)
                ps.setString(3, record.jobType)
                ps.setString(4, record.payload)
                ps.setString(5, record.status)
                ps.setLong(6, record.scheduledAt)
                ps.setInt(7, record.retryCount)
                ps.setLong(8, record.createdAt)
                ps.executeUpdate() > 0
            }
        }
    }

    /**
     * LANGKAH 3.2: DATABASE-LEVEL LOCKING (SELECT ... FOR UPDATE SKIP LOCKED)
     * Mengambil job berikutnya secara atomik dan eksklusif sehingga HANYA satu pod yang dapat memprosesnya.
     */
    suspend fun claimNextJob(podIdentifier: String, currentTimeMs: Long = System.currentTimeMillis()): SchedulerJobQueueRecord? = withContext(Dispatchers.IO) {
        val conn = DatabaseManager.getConnection() ?: return@withContext null
        conn.autoCommit = false
        try {
            var claimedRecord: SchedulerJobQueueRecord? = null

            // 1. SELECT FOR UPDATE SKIP LOCKED
            val selectSql = """
                SELECT id, tenant_id, job_type, payload, status, scheduled_at, claimed_at, claimed_by, completed_at, execution_result, retry_count, created_at
                FROM scheduler_job_queue
                WHERE status = 'pending' AND scheduled_at <= ?
                ORDER BY scheduled_at ASC
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            """.trimIndent()

            conn.prepareStatement(selectSql).use { ps ->
                ps.setLong(1, currentTimeMs)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        claimedRecord = mapResultSet(rs)
                    }
                }
            }

            // 2. Jika ditemukan, tandai status sebagai 'claimed' oleh podIdentifier ini
            claimedRecord?.let { job ->
                val updateSql = """
                    UPDATE scheduler_job_queue
                    SET status = 'claimed',
                        claimed_at = ?,
                        claimed_by = ?
                    WHERE id = ?
                """.trimIndent()

                conn.prepareStatement(updateSql).use { psUpdate ->
                    psUpdate.setLong(1, currentTimeMs)
                    psUpdate.setString(2, podIdentifier)
                    psUpdate.setString(3, job.id)
                    psUpdate.executeUpdate()
                }

                claimedRecord = job.copy(
                    status = "claimed",
                    claimedAt = currentTimeMs,
                    claimedBy = podIdentifier
                )
            }

            conn.commit()
            claimedRecord
        } catch (e: Exception) {
            try {
                conn.rollback()
            } catch (_: Exception) {}
            logger.error("[JOB_QUEUE] Error claiming next job: ${e.message}", e)
            null
        } finally {
            try {
                conn.close()
            } catch (_: Exception) {}
        }
    }

    suspend fun markCompleted(id: String, result: String): Boolean = withContext(Dispatchers.IO) {
        val conn = DatabaseManager.getConnection() ?: return@withContext false
        conn.use { c ->
            c.prepareStatement("""
                UPDATE scheduler_job_queue
                SET status = 'completed',
                    completed_at = ?,
                    execution_result = ?
                WHERE id = ?
            """.trimIndent()).use { ps ->
                ps.setLong(1, System.currentTimeMillis())
                ps.setString(2, result)
                ps.setString(3, id)
                ps.executeUpdate() > 0
            }
        }
    }

    suspend fun markFailed(id: String, reason: String, requeue: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val conn = DatabaseManager.getConnection() ?: return@withContext false
        conn.use { c ->
            val nextStatus = if (requeue) "pending" else "failed"
            c.prepareStatement("""
                UPDATE scheduler_job_queue
                SET status = ?,
                    execution_result = ?,
                    retry_count = retry_count + 1
                WHERE id = ?
            """.trimIndent()).use { ps ->
                ps.setString(1, nextStatus)
                ps.setString(2, reason)
                ps.setString(3, id)
                ps.executeUpdate() > 0
            }
        }
    }

    suspend fun getJobById(id: String): SchedulerJobQueueRecord? = withContext(Dispatchers.IO) {
        val conn = DatabaseManager.getConnection() ?: return@withContext null
        conn.use { c ->
            c.prepareStatement("SELECT * FROM scheduler_job_queue WHERE id = ?").use { ps ->
                ps.setString(1, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapResultSet(rs) else null
                }
            }
        }
    }

    private fun mapResultSet(rs: ResultSet): SchedulerJobQueueRecord {
        return SchedulerJobQueueRecord(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            jobType = rs.getString("job_type"),
            payload = rs.getString("payload") ?: "{}",
            status = rs.getString("status"),
            scheduledAt = rs.getLong("scheduled_at"),
            claimedAt = rs.getLong("claimed_at").takeIf { !rs.wasNull() },
            claimedBy = rs.getString("claimed_by"),
            completedAt = rs.getLong("completed_at").takeIf { !rs.wasNull() },
            executionResult = rs.getString("execution_result"),
            retryCount = rs.getInt("retry_count"),
            createdAt = rs.getLong("created_at")
        )
    }
}
