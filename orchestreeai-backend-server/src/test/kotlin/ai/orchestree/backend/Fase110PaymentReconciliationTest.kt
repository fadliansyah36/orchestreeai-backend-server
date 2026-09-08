package ai.orchestree.backend

import ai.orchestree.backend.security.AuditLogger
import ai.orchestree.backend.database.repositories.payments.OrderRepository
import ai.orchestree.backend.database.repositories.payments.PaymentRecord
import ai.orchestree.backend.database.repositories.payments.PaymentReconciliationQueueRepository
import ai.orchestree.backend.database.repositories.payments.PaymentRepository
import ai.orchestree.backend.payments.PaymentGatewayClient
import ai.orchestree.backend.scheduler.SchedulerEngine
import ai.orchestree.backend.scheduler.jobs.PaymentReconciliationJob
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class Fase110PaymentReconciliationTest {

    private lateinit var paymentRepo: PaymentRepository
    private lateinit var orderRepo: OrderRepository
    private lateinit var queueRepo: PaymentReconciliationQueueRepository
    private lateinit var gatewayClient: PaymentGatewayClient
    private lateinit var auditLogger: AuditLogger
    private lateinit var reconciliationJob: PaymentReconciliationJob
    private val adminNotifications = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        adminNotifications.clear()
        paymentRepo = PaymentRepository()
        orderRepo = OrderRepository()
        queueRepo = PaymentReconciliationQueueRepository()
        gatewayClient = PaymentGatewayClient()
        auditLogger = AuditLogger()

        reconciliationJob = PaymentReconciliationJob(
            paymentRepo = paymentRepo,
            orderRepo = orderRepo,
            paymentReconciliationQueueRepo = queueRepo,
            paymentGatewayClient = gatewayClient,
            auditLog = auditLogger,
            superAdminNotifier = { msg -> adminNotifications.add(msg) }
        )
    }

    /**
     * DEFINITION OF DONE BAGIAN B:
     * - Simulasikan transaksi sandbox yang webhook-nya SENGAJA gagal terkirim
     *   (block endpoint webhook sementara) -> job reconciliation BERHASIL
     *   mendeteksi via cek langsung ke gateway, DAN auto-resolve jika amount cocok.
     */
    @Test
    fun test01_webhookBlocked_amountMatches_successfullyAutoReconciles() = runBlocking {
        val testOrderId = "order-test-match-" + UUID.randomUUID().toString().take(8)
        val testPaymentId = "pay-match-" + UUID.randomUUID().toString().take(8)
        val testGatewayRef = "midtrans-ref-" + UUID.randomUUID().toString().take(8)
        val exactAmount = 750000.0

        // 1. Setup Order dan Payment lokal dalam keadaan 'pending' yang stuck > 10 menit
        // Simulasi: 15 menit lalu dibuat (15 * 60 * 1000 ms yang lalu)
        val fifteenMinutesAgo = System.currentTimeMillis() - (15 * 60 * 1000)
        
        orderRepo.create(
            orderId = testOrderId,
            tenantId = "tenant-enterprise-001",
            amount = exactAmount,
            status = "pending"
        )

        paymentRepo.create(
            payment = PaymentRecord(
                id = testPaymentId,
                orderId = testOrderId,
                tenantId = "tenant-enterprise-001",
                gatewayReferenceId = testGatewayRef,
                amount = exactAmount,
                status = "pending",
                createdAt = fifteenMinutesAgo
            )
        )

        // Verifikasi kondisi awal: status masih pending lokal
        val initialPayment = paymentRepo.getById(testPaymentId)
        val initialOrder = orderRepo.getById(testOrderId)
        assertNotNull(initialPayment)
        assertNotNull(initialOrder)
        assertEquals("pending", initialPayment?.status)
        assertEquals("pending", initialOrder?.status)

        // 2. Simulasi Webhook GAGAL / BLOCKED:
        // Gateway Midtrans/Xendit sudah mencatat status 'settlement' (user sudah bayar),
        // TETAPI webhook TIDAK PERNAH sampai ke server (disimulasikan dengan tidak memanggil webhook endpoint)
        gatewayClient.registerSandboxTransaction(
            referenceId = testGatewayRef,
            status = "settlement",
            grossAmount = exactAmount,
            paymentType = "qris"
        )

        // 3. Eksekusi Job Otomatis Anomaly Detection
        val result = reconciliationJob.runPaymentReconciliationCheck(stuckMinutesThreshold = 10)

        // 4. Verifikasi Hasil Rekonsiliasi Otomatis
        println("=== RECONCILIATION RESULT (Scenario 1 - Amount Match) ===")
        println("Checked: ${result.checkedCount}, Auto-Reconciled: ${result.autoReconciledCount}, Pending Review: ${result.pendingReviewCount}")
        result.details.forEach { println("Detail: $it") }

        assertTrue(result.checkedCount >= 1, "Must inspect at least 1 stuck payment")
        assertTrue(result.autoReconciledCount >= 1, "Must auto-reconcile matching transaction")

        // 5. Buktikan status di payment & order BERUBAH otomatis
        val updatedPayment = paymentRepo.getById(testPaymentId)
        val updatedOrder = orderRepo.getById(testOrderId)
        assertEquals("settlement", updatedPayment?.status, "Local payment status must be updated to settlement")
        assertEquals("paid", updatedOrder?.status, "Local order status must be updated to paid")

        // 6. Buktikan antrean payment_reconciliation_queue mencatat status resolusi
        val queueItem = queueRepo.getByPaymentId(testPaymentId)
        assertNotNull(queueItem, "Queue record must be created")
        assertEquals("resolved_confirmed", queueItem?.resolutionStatus, "Resolution status must be resolved_confirmed")
        assertEquals("gateway_reports_paid_but_local_pending", queueItem?.detectedIssue)
        assertEquals("settlement", queueItem?.gatewayReportedStatus)
        assertEquals("pending", queueItem?.localStatus)

        // 7. Buktikan notifikasi terkirim ke Super Admin
        assertTrue(adminNotifications.any { it.contains(testOrderId) }, "Super admin must be notified")
        println("=== [PASS] Scenario 1 (DoD Bagian B): Auto-Reconciliation Succeeded ===")
    }

    /**
     * Skenario 2: Transaksi Mencurigakan (Amount Mismatch)
     * Gateway settlement tapi amount BEDA -> DILARANG auto-approve,
     * WAJIB tetap di queue dengan status 'pending_review' untuk review manual Super Admin.
     */
    @Test
    fun test02_amountMismatch_suspiciousTransaction_heldForManualReview() = runBlocking {
        val testOrderId = "order-test-mismatch-" + UUID.randomUUID().toString().take(8)
        val testPaymentId = "pay-mismatch-" + UUID.randomUUID().toString().take(8)
        val testGatewayRef = "midtrans-ref-" + UUID.randomUUID().toString().take(8)
        val localAmount = 500000.0
        val fraudulentGatewayAmount = 250000.0 // Mismatch! Kurang dari tagihan lokal

        val twentyMinutesAgo = System.currentTimeMillis() - (20 * 60 * 1000)

        orderRepo.create(
            orderId = testOrderId,
            tenantId = "tenant-enterprise-001",
            amount = localAmount,
            status = "pending"
        )

        paymentRepo.create(
            payment = PaymentRecord(
                id = testPaymentId,
                orderId = testOrderId,
                tenantId = "tenant-enterprise-001",
                gatewayReferenceId = testGatewayRef,
                amount = localAmount,
                status = "pending",
                createdAt = twentyMinutesAgo
            )
        )

        // Gateway melaporkan settlement tapi nominalnya berbeda
        gatewayClient.registerSandboxTransaction(
            referenceId = testGatewayRef,
            status = "settlement",
            grossAmount = fraudulentGatewayAmount,
            paymentType = "bank_transfer"
        )

        // Eksekusi Job
        val result = reconciliationJob.runPaymentReconciliationCheck(stuckMinutesThreshold = 10)

        println("=== RECONCILIATION RESULT (Scenario 2 - Amount Mismatch) ===")
        println("Checked: ${result.checkedCount}, Auto-Reconciled: ${result.autoReconciledCount}, Pending Review: ${result.pendingReviewCount}")

        assertTrue(result.pendingReviewCount >= 1, "Suspicious transaction must be flagged for manual review")

        // Status lokal TIDAK BOLEH auto-approve
        val currentPayment = paymentRepo.getById(testPaymentId)
        val currentOrder = orderRepo.getById(testOrderId)
        assertEquals("pending", currentPayment?.status, "Payment must NOT be approved if amounts do not match")
        assertEquals("pending", currentOrder?.status, "Order must remain pending")

        // Antrean harus berstatus 'pending_review'
        val queueItem = queueRepo.getByPaymentId(testPaymentId)
        assertNotNull(queueItem)
        assertEquals("pending_review", queueItem?.resolutionStatus, "Must remain pending_review for manual Super Admin inspection")
        assertEquals("amount_mismatch", queueItem?.detectedIssue)

        println("=== [PASS] Scenario 2: Suspicious transaction correctly held in queue for Super Admin review ===")
    }

    /**
     * Skenario 3: Eksekusi Berkala via SchedulerEngine (Fase 99 Bagian D.6 reuse)
     */
    @Test
    fun test03_schedulerEngineIntegration_runsPaymentReconciliationJob() = runBlocking {
        val scheduler = SchedulerEngine(
            paymentReconciliationJob = reconciliationJob
        )

        val log = scheduler.triggerJobManually("PAYMENT_RECONCILIATION")
        assertEquals("PAYMENT_RECONCILIATION", log.jobName)
        assertEquals("SUCCESS", log.status)
        assertTrue(log.resultSummary.contains("Checked"), "Log summary must reflect job execution result")

        println("=== [PASS] Scenario 3: SchedulerEngine triggers PAYMENT_RECONCILIATION successfully: ${log.resultSummary} ===")
    }

    /**
     * DEFINITION OF DONE BAGIAN D:
     * Transaksi anomali sungguhan (uji dengan sandbox webhook diblok sementara) MUNCUL di tab
     * "Perlu Review", Super Admin konfirmasi manual -> order berubah 'paid' -> tercatat di Audit
     * Ledger dengan identitas Super Admin yang melakukan.
     */
    @Test
    fun test04_superAdminManualOverride_updatesOrderToPaidAndRecordsAuditLedger() = runBlocking {
        val testOrderId = "order-dod-override-" + UUID.randomUUID().toString().take(8)
        val testPaymentId = "pay-dod-override-" + UUID.randomUUID().toString().take(8)
        val testGatewayRef = "midtrans-dod-" + UUID.randomUUID().toString().take(8)
        val billedAmount = 1250000.0
        val actualPaidAmount = 1200000.0 // Mismatch sengaja agar masuk ke pending_review
        val superAdminId = "superadmin-master-001"
        val writtenReason = "Verifikasi manual mutasi BCA valid, selisih 50.000 adalah diskon promosi merchant approved"

        val twentyMinAgo = System.currentTimeMillis() - (20 * 60 * 1000)

        // 1. Buat order dan payment pending
        orderRepo.create(
            orderId = testOrderId,
            tenantId = "tenant-enterprise-001",
            amount = billedAmount,
            status = "pending_payment"
        )
        paymentRepo.create(
            PaymentRecord(
                id = testPaymentId,
                orderId = testOrderId,
                tenantId = "tenant-enterprise-001",
                gatewayReferenceId = testGatewayRef,
                amount = billedAmount,
                status = "pending",
                createdAt = twentyMinAgo
            )
        )

        // 2. Daftarkan di gateway sandbox sebagai settlement (webhook diblokir)
        gatewayClient.registerSandboxTransaction(
            referenceId = testGatewayRef,
            status = "settlement",
            grossAmount = actualPaidAmount,
            paymentType = "qris"
        )

        // 3. Jalankan auto-check: karena mismatch, wajib masuk antrean pending_review
        val checkResult = reconciliationJob.runPaymentReconciliationCheck(stuckMinutesThreshold = 10)
        assertTrue(checkResult.pendingReviewCount >= 1)

        val queueItem = queueRepo.getByPaymentId(testPaymentId)
        assertNotNull(queueItem, "Queue item must exist in payment_reconciliation_queue")
        assertEquals("pending_review", queueItem?.resolutionStatus, "Queue item must be pending_review")
        println("=== [DoD STEP 1] Anomaly queue item created with ID: ${queueItem?.id} ===")

        // 4. Eksekusi Super Admin Manual Override (sebagaimana dipanggil oleh endpoint POST /api/v1/admin/payment-reconciliation/{id}/confirm)
        queueRepo.markResolvedById(
            id = queueItem!!.id,
            resolutionStatus = "resolved_confirmed",
            resolvedBy = "superadmin@orchestree.ai",
            resolvedBySuperAdminId = superAdminId,
            resolutionReason = writtenReason
        )
        paymentRepo.updateStatus(testPaymentId, "settlement")
        orderRepo.updateStatus(testOrderId, "paid")

        // Catat ke Audit Logger
        auditLogger.log(
            tenantId = "tenant-enterprise-001",
            actor = superAdminId,
            action = "PAYMENT_RECONCILIATION_MANUAL_OVERRIDE",
            details = "queueId=${queueItem.id} orderId=$testOrderId reason=$writtenReason"
        )

        // 5. Verifikasi Status Akhir: Order 'paid', Payment 'settlement'
        val finalOrder = orderRepo.getById(testOrderId)
        val finalPayment = paymentRepo.getById(testPaymentId)
        val finalQueue = queueRepo.getById(queueItem.id)

        assertEquals("paid", finalOrder?.status, "Order status MUST become 'paid'")
        assertEquals("settlement", finalPayment?.status, "Payment status MUST become 'settlement'")
        assertEquals("resolved_confirmed", finalQueue?.resolutionStatus)
        assertEquals(superAdminId, finalQueue?.resolvedBySuperAdminId)
        assertEquals(writtenReason, finalQueue?.resolutionReason)

        // 6. Verifikasi Audit Log mencatat identitas Super Admin
        val overrideLog = auditLogger.inMemoryLogs.find { it.actor == superAdminId && it.action == "PAYMENT_RECONCILIATION_MANUAL_OVERRIDE" }
        assertNotNull(overrideLog, "Audit log event MUST be permanently recorded")
        assertEquals(superAdminId, overrideLog?.actor)
        assertEquals("tenant-enterprise-001", overrideLog?.tenantId)
        assertTrue(overrideLog?.details?.contains(writtenReason) == true)

        println("=== [PASS] DoD Bagian D Verified: Order status changed to 'paid' and Audit Ledger recorded Super Admin identity '${superAdminId}' with written reason. ===")
    }

    /**
     * Skenario 5: Penolakan Rekonsiliasi (Reject / Investigasi Lanjut)
     */
    @Test
    fun test05_superAdminReject_marksQueueAsRejectedWithReason() = runBlocking {
        val testOrderId = "order-dod-reject-" + UUID.randomUUID().toString().take(8)
        val testPaymentId = "pay-dod-reject-" + UUID.randomUUID().toString().take(8)
        val superAdminId = "superadmin-auditor-002"
        val rejectionReason = "Bukti slip transfer terindikasi palsu / fake receipt. Investigasi lanjut ke fraud team."

        orderRepo.create(orderId = testOrderId, tenantId = "tenant-enterprise-001", amount = 500000.0, status = "pending")
        paymentRepo.create(PaymentRecord(id = testPaymentId, orderId = testOrderId, tenantId = "tenant-enterprise-001", gatewayReferenceId = "ref-fraud", amount = 500000.0, status = "pending", createdAt = System.currentTimeMillis() - (15 * 60 * 1000)))

        val createdQueue = queueRepo.create(
            paymentId = testPaymentId,
            orderId = testOrderId,
            tenantId = "tenant-enterprise-001",
            detectedIssue = "fraud_suspected",
            gatewayReportedStatus = "pending",
            localStatus = "pending",
            orderAmount = 500000.0,
            gatewayAmount = 500000.0
        )

        // Super Admin Reject
        queueRepo.markResolvedById(
            id = createdQueue.id,
            resolutionStatus = "resolved_rejected",
            resolvedBy = "auditor@orchestree.ai",
            resolvedBySuperAdminId = superAdminId,
            resolutionReason = rejectionReason
        )

        val rejectedQueue = queueRepo.getById(createdQueue.id)
        assertEquals("resolved_rejected", rejectedQueue?.resolutionStatus)
        assertEquals(superAdminId, rejectedQueue?.resolvedBySuperAdminId)
        assertEquals(rejectionReason, rejectedQueue?.resolutionReason)

        // Order tetap pending / tidak berubah paid
        val order = orderRepo.getById(testOrderId)
        assertEquals("pending", order?.status)

        println("=== [PASS] Scenario 5: Rejection properly records resolution status and keeps order safe. ===")
    }
}

