package ai.orchestree.backend.scheduler

import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.database.repositories.scheduler.SchedulerJobQueueRecord
import ai.orchestree.backend.database.repositories.scheduler.SchedulerJobQueueRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.util.UUID

/**
 * LANGKAH 3 VERIFIKASI:
 * Test ekstensif untuk membuktikan mekanisme Database-Level Locking (SELECT ... FOR UPDATE SKIP LOCKED)
 * pada SchedulerEngine dan SchedulerJobQueueRepository bekerja sempurna dalam mencegah race condition multi-pod.
 */
class SchedulerLockingComprehensiveTest {

    private val queueRepo = SchedulerJobQueueRepository()
    private val schedulerEngine = SchedulerEngine(schedulerJobQueueRepo = queueRepo)

    @BeforeEach
    fun setupTable() {
        runBlocking {
            queueRepo.ensureTableExists()
        }
    }

    private fun getLiveConnection(): Connection? = DatabaseManager.getConnection()

    @Test
    fun test1_DatabaseLevelLocking_ForUpdateSkipLocked_PreventsRaceCondition() = runBlocking {
        println("\n==========================================================================")
        println("=== TEST 1: Database-Level Locking (FOR UPDATE SKIP LOCKED) Multi-Pod Race ===")
        println("==========================================================================")

        val conn = getLiveConnection()
        assertNotNull(conn, "Koneksi Supabase PostgreSQL live wajib tersedia!")

        // Enqueue 3 scheduled jobs yang siap dieksekusi (scheduled_at <= now)
        val now = System.currentTimeMillis()
        val job1Id = "job-lock-1-" + UUID.randomUUID().toString().take(6)
        val job2Id = "job-lock-2-" + UUID.randomUUID().toString().take(6)
        val job3Id = "job-lock-3-" + UUID.randomUUID().toString().take(6)

        queueRepo.enqueue(
            SchedulerJobQueueRecord(
                id = job1Id,
                tenantId = "tenant-test-lock-1",
                jobType = "TRIAL_EXPIRY",
                status = "pending",
                scheduledAt = now - 5000
            )
        )
        queueRepo.enqueue(
            SchedulerJobQueueRecord(
                id = job2Id,
                tenantId = "tenant-test-lock-2",
                jobType = "HEALTH_CHECK",
                status = "pending",
                scheduledAt = now - 3000
            )
        )
        queueRepo.enqueue(
            SchedulerJobQueueRecord(
                id = job3Id,
                tenantId = "tenant-test-lock-3",
                jobType = "CREDIT_EXPIRATION",
                status = "pending",
                scheduledAt = now - 1000
            )
        )

        println("Enqueued 3 pending jobs: $job1Id, $job2Id, $job3Id")

        // Simulasi 5 Pod saling berlomba secara paralel memanggil claimNextJob()
        // Dengan 3 job tersedia dan 5 pod berlomba, persis 3 pod harus mendapatkan tepat 1 job masing-masing,
        // dan 2 pod lainnya harus mendapatkan null (karena semua job sudah terkunci dan di-skip!).
        val podNames = listOf("pod-worker-alpha", "pod-worker-beta", "pod-worker-gamma", "pod-worker-delta", "pod-worker-epsilon")

        val claimedJobs = coroutineScope {
            podNames.map { pod ->
                async {
                    val claimed = schedulerEngine.claimNextSchedulerJob(pod)
                    println("Pod '$pod' claim result: ${claimed?.id ?: "NONE (skipped / locked)"}")
                    pod to claimed
                }
            }.awaitAll()
        }

        val nonNullClaims = claimedJobs.mapNotNull { it.second }
        println("Total claimed jobs across 5 concurrent pods: ${nonNullClaims.size}")

        // Verifikasi tidak ada job yang diklaim dua kali (ID unik 100%)
        val uniqueClaimedIds = nonNullClaims.map { it.id }.toSet()
        assertEquals(nonNullClaims.size, uniqueClaimedIds.size, "Race condition terdeteksi! Terdapat job yang diklaim ganda oleh lebih dari 1 pod!")
        assertEquals(3, uniqueClaimedIds.size, "Tepat 3 job harus diklaim oleh 3 pod berbeda")

        // Verifikasi bahwa 2 pod sisanya tidak mendapatkan apa-apa
        val nullClaims = claimedJobs.filter { it.second == null }
        assertEquals(2, nullClaims.size, "Dua pod lainnya wajib menerima null karena seluruh job telah terkunci (SKIP LOCKED)")

        // Verifikasi langsung ke tabel database PostgreSQL via RAW SQL
        val claimedInDb = mutableListOf<String>()
        conn!!.prepareStatement("SELECT id, status, claimed_by FROM scheduler_job_queue WHERE id IN (?, ?, ?)").use { ps ->
            ps.setString(1, job1Id)
            ps.setString(2, job2Id)
            ps.setString(3, job3Id)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val id = rs.getString("id")
                    val status = rs.getString("status")
                    val claimedBy = rs.getString("claimed_by")
                    claimedInDb.add(id)
                    println("[RAW SQL PROOF]: Job $id -> status: $status, claimed_by: $claimedBy")
                    assertEquals("claimed", status)
                    assertNotNull(claimedBy)
                }
            }
        }
        assertEquals(3, claimedInDb.size)

        // Bersihkan data test
        conn.prepareStatement("DELETE FROM scheduler_job_queue WHERE id IN (?, ?, ?)").use { ps ->
            ps.setString(1, job1Id)
            ps.setString(2, job2Id)
            ps.setString(3, job3Id)
            ps.executeUpdate()
        }
        conn.close()

        println(">>> TEST 1: FOR UPDATE SKIP LOCKED Race Condition Prevention VERIFIED PASSED! <<<")
    }

    @Test
    fun test2_ProcessNextClaimedJob_ExecutionAndStatusUpdate() = runBlocking {
        println("\n==========================================================================")
        println("=== TEST 2: Process Next Claimed Job End-to-End Workflow ===")
        println("==========================================================================")

        val jobId = "job-exec-" + UUID.randomUUID().toString().take(6)
        queueRepo.enqueue(
            SchedulerJobQueueRecord(
                id = jobId,
                tenantId = "tenant-exec-test",
                jobType = "HEALTH_CHECK",
                status = "pending",
                scheduledAt = System.currentTimeMillis() - 2000
            )
        )

        val runLog = schedulerEngine.processNextClaimedJob("pod-executor-1")
        assertNotNull(runLog, "Job harus berhasil diklaim dan dieksekusi")
        assertEquals("HEALTH_CHECK", runLog!!.jobName)
        assertEquals("SUCCESS", runLog.status)

        // Cek status akhir di database
        val updatedRecord = queueRepo.getJobById(jobId)
        assertNotNull(updatedRecord)
        assertEquals("completed", updatedRecord!!.status)
        assertEquals("pod-executor-1", updatedRecord.claimedBy)
        assertNotNull(updatedRecord.completedAt)
        println("[RAW SQL PROOF]: Job $jobId completed successfully by ${updatedRecord.claimedBy} with result: ${updatedRecord.executionResult}")

        // Bersihkan
        val conn = getLiveConnection()
        conn?.prepareStatement("DELETE FROM scheduler_job_queue WHERE id = ?")?.use { ps ->
            ps.setString(1, jobId)
            ps.executeUpdate()
        }
        conn?.close()

        println(">>> TEST 2: Process Claimed Job End-to-End VERIFIED PASSED! <<<")
    }
}
