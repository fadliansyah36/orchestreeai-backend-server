package ai.orchestree.backend.scheduler.jobs

import ai.orchestree.backend.database.repositories.payments.OrderRepository
import ai.orchestree.backend.database.repositories.payments.PaymentRecord
import ai.orchestree.backend.database.repositories.payments.PaymentReconciliationQueueRepository
import ai.orchestree.backend.database.repositories.payments.PaymentRepository
import ai.orchestree.backend.payments.PaymentGatewayClient
import ai.orchestree.backend.security.AuditLogger
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

data class PaymentReconciliationResult(
    val checkedCount: Int,
    val autoReconciledCount: Int,
    val pendingReviewCount: Int,
    val details: List<String> = emptyList()
)

class PaymentReconciliationJob(
    val paymentRepo: PaymentRepository = PaymentRepository(),
    val orderRepo: OrderRepository = OrderRepository(),
    val paymentReconciliationQueueRepo: PaymentReconciliationQueueRepository = PaymentReconciliationQueueRepository(),
    val paymentGatewayClient: PaymentGatewayClient = PaymentGatewayClient(),
    val auditLog: AuditLogger = AuditLogger(),
    private val superAdminNotifier: (String) -> Unit = {}
) {
    private val logger = LoggerFactory.getLogger(PaymentReconciliationJob::class.java)
    val notifiedMessages = CopyOnWriteArrayList<String>()

    fun notifySuperAdmin(message: String) {
        logger.warn("[ALERT:SUPER_ADMIN] $message")
        notifiedMessages.add(message)
        superAdminNotifier(message)
    }

    /**
     * Verifies whether gateway gross amount strictly matches local payment amount.
     */
    suspend fun gatewayAmountMatchesLocal(payment: PaymentRecord): Boolean {
        val details = paymentGatewayClient.getTransactionDetails(payment.gatewayReferenceId, payment.orderId)
        if (details == null) {
            logger.warn("Could not query transaction details from gateway for reference: ${payment.gatewayReferenceId}")
            return false
        }
        val diff = kotlin.math.abs(details.grossAmount - payment.amount)
        val isMatch = diff < 0.01
        if (!isMatch) {
            logger.warn("Amount mismatch detected for Order ${payment.orderId}: Gateway reports ${details.grossAmount}, local records ${payment.amount}")
        }
        return isMatch
    }

    /**
     * Periodic anomaly detection job:
     * - Finds pending payments older than threshold minutes.
     * - Queries Payment Gateway API directly (Midtrans/Xendit) to discover ground truth.
     * - Detects if gateway is already 'settlement' but local status is still 'pending'.
     * - Inserts into payment_reconciliation_queue.
     * - Sends alert to Super Admin.
     * - Automatically resolves if amount matches exactly.
     * - Leaves suspicious / mismatched amounts in queue for manual Super Admin review.
     */
    suspend fun runPaymentReconciliationCheck(): PaymentReconciliationResult = runPaymentReconciliationCheck(stuckMinutesThreshold = 10)

    suspend fun runPaymentReconciliationCheck(stuckMinutesThreshold: Int): PaymentReconciliationResult {
        // Cari SEMUA payments dengan status='pending' yang SUDAH LEBIH
        // dari 10 menit sejak dibuat (indikasi webhook mungkin gagal)
        val stuckPayments = paymentRepo.findPendingOlderThan(minutes = stuckMinutesThreshold)
        val resultDetails = mutableListOf<String>()
        var reconciledCount = 0
        var pendingReviewCount = 0

        logger.info("[RECONCILIATION] Found ${stuckPayments.size} pending payments older than $stuckMinutesThreshold minutes")

        for (payment in stuckPayments) {
            // CEK LANGSUNG ke API Payment Gateway (Midtrans/Xendit) -
            // BUKAN asumsi, TANYA LANGSUNG status sebenarnya
            val gatewayDetails = paymentGatewayClient.getTransactionDetails(payment.gatewayReferenceId, payment.orderId)
            val gatewayStatus = gatewayDetails?.transactionStatus
                ?: paymentGatewayClient.checkTransactionStatus(payment.gatewayReferenceId, payment.orderId)

            if (gatewayStatus.equals("settlement", ignoreCase = true) && !payment.status.equals("settlement", ignoreCase = true)) {
                // TEMUAN: gateway bilang SUDAH BAYAR, tapi lokal masih
                // pending -> INI KASUS YANG DIKHAWATIRKAN USER
                val amountMatches = if (gatewayDetails != null) {
                    kotlin.math.abs(gatewayDetails.grossAmount - payment.amount) < 0.01
                } else {
                    gatewayAmountMatchesLocal(payment)
                }
                val detectedIssue = if (amountMatches) {
                    "gateway_reports_paid_but_local_pending"
                } else {
                    "amount_mismatch"
                }

                paymentReconciliationQueueRepo.create(
                    paymentId = payment.id,
                    orderId = payment.orderId,
                    tenantId = payment.tenantId,
                    detectedIssue = detectedIssue,
                    gatewayReportedStatus = gatewayStatus,
                    localStatus = payment.status,
                    orderAmount = payment.amount,
                    gatewayAmount = gatewayDetails?.grossAmount
                )

                notifySuperAdmin("Ditemukan transaksi terbayar tapi belum terkonfirmasi lokal: Order ${payment.orderId}")

                // OTOMATIS PERBAIKI JIKA AMAN (amount cocok persis) -
                // TIDAK PERLU tunggu manual jika data konsisten:
                if (amountMatches) {
                    paymentRepo.updateStatus(payment.id, "settlement")
                    orderRepo.updateStatus(payment.orderId, "paid")
                    paymentReconciliationQueueRepo.markResolved(
                        paymentId = payment.id,
                        resolutionStatus = "resolved_confirmed",
                        resolvedBy = "system_auto_reconciliation"
                    )
                    auditLog.record("payment_auto_reconciled", payment.id)
                    reconciledCount++
                    resultDetails.add("Order ${payment.orderId} AUTO_RECONCILED to settlement/paid")
                } else {
                    // JIKA amount TIDAK cocok atau ada kejanggalan lain,
                    // BIARKAN di queue untuk REVIEW MANUAL Super Admin
                    // (Bagian D) - JANGAN auto-approve kasus mencurigakan.
                    pendingReviewCount++
                    resultDetails.add("Order ${payment.orderId} FLAGGED for Super Admin manual review (amount mismatch)")
                }
            }
        }

        return PaymentReconciliationResult(
            checkedCount = stuckPayments.size,
            autoReconciledCount = reconciledCount,
            pendingReviewCount = pendingReviewCount,
            details = resultDetails
        )
    }

    suspend fun execute(): String {
        val result = runPaymentReconciliationCheck(stuckMinutesThreshold = 10)
        return "Checked ${result.checkedCount} stuck payments, auto-reconciled ${result.autoReconciledCount}, flagged ${result.pendingReviewCount} for review"
    }
}
