package ai.orchestree.backend.models

import java.time.Instant

/**
 * Real Billing Service & Payment Gateway Models (PRD Section 16 & Section 26)
 * Supports Subscriptions, Invoices, Usage Records, Payment Methods, Midtrans/Stripe Gateways, and Dunning.
 */

enum class SubscriptionStatus(val label: String) {
    ACTIVE("Aktif"),
    TRIALING("Masa Uji Coba"),
    PAST_DUE("Menunggak / Grace Period"),
    CANCELLED("Dibatalkan"),
    UNPAID("Belum Dibayar")
}

enum class InvoiceStatus(val label: String) {
    DRAFT("Draft"),
    PENDING("Menunggu Pembayaran"),
    PAID("Lunas"),
    FAILED("Gagal"),
    VOID("Dibatalkan"),
    REFUNDED("Dikembalikan (Refund)")
}

enum class BillingCycle(val label: String, val multiplierMonths: Int, val discountPct: Double) {
    MONTHLY("Bulanan", 1, 0.0),
    YEARLY("Tahunan (Hemat 17%)", 12, 0.17)
}

enum class PaymentGatewayType(val label: String) {
    MIDTRANS("Midtrans Payment Gateway (IDR)"),
    STRIPE("Stripe Global (USD/Multi-Currency)"),
    XENDIT("Xendit Invoice")
}

enum class PaymentMethodType(val label: String, val category: String) {
    CREDIT_CARD("Kartu Kredit / Debit (Visa/Mastercard/JCB)", "CARD"),
    BANK_TRANSFER_BCA("BCA Virtual Account", "VIRTUAL_ACCOUNT"),
    BANK_TRANSFER_MANDIRI("Mandiri Bill Payment", "VIRTUAL_ACCOUNT"),
    BANK_TRANSFER_BNI("BNI Virtual Account", "VIRTUAL_ACCOUNT"),
    BANK_TRANSFER_BRI("BRI Virtual Account", "VIRTUAL_ACCOUNT"),
    GOPAY("GoPay / GoPay Later", "E_WALLET"),
    QRIS("QRIS Realtime (BCA, GoPay, OVO, Dana)", "QR_CODE")
}

enum class DunningStatus(val label: String) {
    NONE("Normal"),
    IN_GRACE_PERIOD("Masa Tenggang (Grace Period 7 Hari)"),
    RETRY_EXHAUSTED("Maksimum Percobaan Tercapai"),
    RESOLVED("Berhasil Diselesaikan")
}

data class SubscriptionDto(
    val id: String,
    val tenantId: String,
    val planId: String,
    val planName: String,
    val tier: String,
    val status: SubscriptionStatus,
    val billingCycle: BillingCycle,
    val priceIdr: Double,
    val currentPeriodStart: Long,
    val currentPeriodEnd: Long,
    val autoRenew: Boolean = true,
    val paymentGateway: PaymentGatewayType = PaymentGatewayType.MIDTRANS,
    val gracePeriodEndsAt: Long? = null,
    val dunningRetryCount: Int = 0,
    val nextRetryAt: Long? = null,
    val isPastDue: Boolean = (status == SubscriptionStatus.PAST_DUE)
)

data class InvoiceDto(
    val id: String,
    val tenantId: String,
    val subscriptionId: String?,
    val invoiceNumber: String,
    val planName: String,
    val periodStart: Long,
    val periodEnd: Long,
    val baseAmountIdr: Double,
    val overageTokensBilled: Long = 0,
    val overageAmountIdr: Double = 0.0,
    val taxIdr: Double = 0.0, // PPN 11%
    val totalAmountIdr: Double,
    val status: InvoiceStatus,
    val paymentGateway: PaymentGatewayType = PaymentGatewayType.MIDTRANS,
    val gatewayOrderId: String,
    val gatewayTransactionId: String? = null,
    val gatewayPaymentUrl: String? = null,
    val gatewaySnapToken: String? = null,
    val paymentMethodType: String? = null,
    val paidAt: Long? = null,
    val dueDate: Long,
    val retryCount: Int = 0,
    val dunningStatus: DunningStatus = DunningStatus.NONE,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class PaymentMethodDto(
    val id: String,
    val tenantId: String,
    val gateway: PaymentGatewayType,
    val methodType: PaymentMethodType,
    val accountMask: String,
    val cardBrand: String? = null,
    val expiryInfo: String? = null,
    val isDefault: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class UsageRecordDto(
    val id: String,
    val tenantId: String,
    val invoiceId: String?,
    val periodMonth: String,
    val baseTokensQuota: Long,
    val actualTokensUsed: Long,
    val overageTokens: Long,
    val costPer1kTokensIdr: Double,
    val calculatedOverageChargeIdr: Double,
    val recordedAt: Long = System.currentTimeMillis()
)

data class DunningLogDto(
    val id: String,
    val tenantId: String,
    val invoiceId: String,
    val attemptNumber: Int,
    val errorCode: String?,
    val errorMessage: String?,
    val actionTaken: String,
    val nextRetryScheduledAt: Long?,
    val notificationSent: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class CreateCheckoutRequest(
    val tenantId: String,
    val planId: String,
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val customerEmail: String,
    val customerName: String,
    val customerPhone: String = "+6281234567890",
    val paymentMethodType: PaymentMethodType? = null
)

data class CheckoutResponse(
    val invoiceId: String,
    val invoiceNumber: String,
    val gatewayOrderId: String,
    val snapToken: String,
    val paymentUrl: String,
    val grossAmountIdr: Double,
    val basePriceIdr: Double,
    val overageChargeIdr: Double,
    val taxIdr: Double,
    val clientKey: String
)

data class WebhookNotificationPayload(
    val orderId: String,
    val statusCode: String,
    val grossAmount: String,
    val transactionStatus: String, // "settlement", "capture", "pending", "deny", "expire", "cancel", "refund"
    val fraudStatus: String? = null, // "accept", "challenge", "deny"
    val paymentType: String,
    val transactionId: String,
    val transactionTime: String,
    val signatureKey: String,
    val statusMessage: String? = null
)

data class SuperAdminBillingSummaryDto(
    val totalMmrIdr: Double,
    val totalCollectedIdr: Double,
    val outstandingAmountIdr: Double,
    val failedPaymentsCount: Int,
    val totalActiveSubscriptions: Int,
    val averageOverageRevenueIdr: Double,
    val recentInvoices: List<InvoiceDto> = emptyList(),
    val dunningQueue: List<InvoiceDto> = emptyList()
)
