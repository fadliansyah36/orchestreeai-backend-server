package ai.orchestree.backend.billing

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CommercialPlan(
    val id: String,
    val planCode: String,
    val planName: String,
    val billingInterval: String = "monthly",
    val price: Double?,
    val currency: String = "IDR",
    val creditAllocation: Double?,
    val humanSeatLimit: Int?,
    val aiAgentLimit: Int?,
    val isPriceVisible: Boolean = true,
    val isActive: Boolean = true,
    val sortOrder: Int? = null
)

@Serializable
data class PlanFeatureEntitlement(
    val id: String,
    val planId: String,
    val featureKey: String,
    val entitlementValue: String
)

@Serializable
data class TenantSubscription(
    val id: String,
    val tenantId: String,
    val planId: String,
    val status: String, // 'trial','active','past_due','grace_period','suspended','cancelled'
    val currentPeriodStart: Long?,
    val currentPeriodEnd: Long?,
    val isFounderExclusive: Boolean = false,
    val customEntitlementOverride: String? = null
)

@Serializable
data class AiCreditWallet(
    val id: String,
    val tenantId: String,
    val subscriptionBalance: Double = 0.0,
    val topupBalance: Double = 0.0,
    val bonusBalance: Double = 0.0,
    val reservedBalance: Double = 0.0,
    val usedBalance: Double = 0.0,
    val expiredBalance: Double = 0.0,
    val isUnlimited: Boolean = false
) {
    val availableBalance: Double
        get() = (subscriptionBalance + topupBalance + bonusBalance - reservedBalance).coerceAtLeast(0.0)
}

@Serializable
data class CreditMeteringRule(
    val id: String = UUID.randomUUID().toString(),
    val activityType: String,
    val baseWorkUnitMin: Double,
    val baseWorkUnitMax: Double
)

@Serializable
data class CreditCostFactor(
    val id: String = UUID.randomUUID().toString(),
    val factorType: String, // 'complexity_factor','model_cost_factor','tool_factor','execution_factor'
    val factorKey: String,
    val factorValue: Double
)

@Serializable
data class CreditCostContext(
    val activityType: String,
    val complexityLevel: String = "simple", // 'simple'/'medium'/'complex'
    val modelUsed: String = "standard",     // 'openrouter'/'groq'/'deepseek'/'claude'/'kimi'/'standard'
    val toolsInvoked: Int = 0,
    val executionType: String = "single_step" // 'single_step'/'multi_step'/'autonomous'
)

@Serializable
data class CreditCostResult(
    val estimatedCost: Double,
    val breakdown: Map<String, Double>
)

open class InsufficientCreditException(
    val available: Double = 0.0,
    val required: Double = 0.0,
    message: String = "Saldo kredit tidak mencukupi. Tersedia: $available, Dibutuhkan: $required"
) : RuntimeException(message) {
    constructor(message: String) : this(0.0, 0.0, message)
}

data class TaskExecutionResult(
    val referenceId: String? = null,
    val llmUsageDetail: Map<String, Any?>? = null,
    val data: Any? = null
)

typealias TaskResult = TaskExecutionResult

@Serializable
data class MidtransWebhookPayload(
    @kotlinx.serialization.SerialName("transaction_id")
    val transactionId: String? = null,
    @kotlinx.serialization.SerialName("order_id")
    val orderId: String,
    @kotlinx.serialization.SerialName("gross_amount")
    val grossAmount: String,
    @kotlinx.serialization.SerialName("payment_type")
    val paymentType: String? = "bank_transfer",
    @kotlinx.serialization.SerialName("transaction_time")
    val transactionTime: String? = null,
    @kotlinx.serialization.SerialName("transaction_status")
    val transactionStatus: String, // 'settlement', 'capture', 'pending', 'deny', 'expire', 'cancel'
    @kotlinx.serialization.SerialName("fraud_status")
    val fraudStatus: String? = "accept",
    @kotlinx.serialization.SerialName("status_code")
    val statusCode: String = "200",
    @kotlinx.serialization.SerialName("signature_key")
    val signatureKey: String,
    @kotlinx.serialization.SerialName("custom_field1")
    val customField1: String? = null,
    @kotlinx.serialization.SerialName("custom_field2")
    val customField2: String? = null
)

sealed class DowngradeResult {
    data class Blocked(val overages: List<String>) : DowngradeResult()
    data class Success(val subscription: TenantSubscription) : DowngradeResult()
}

@Serializable
data class SubscriptionPlanResponse(
    val status: String = "ok",
    val tenantId: String? = null,
    val subscription: TenantSubscription? = null,
    val plan: CommercialPlan? = null
)

@Serializable
data class CancelSubscriptionResponse(
    val status: String = "cancelled",
    val subscription: TenantSubscription
)

@Serializable
data class SubscriptionDetailsResponse(
    val tenantId: String,
    val planId: String,
    val planName: String,
    val status: String,
    val periodStart: Long? = null,
    val periodEnd: Long? = null,
    val creditAllocation: Double = 0.0,
    val seatLimit: Int = 0,
    val agentLimit: Int = 0
)

@Serializable
data class LedgerEntryItem(
    val id: String,
    val tenantId: String,
    val ledgerType: String,
    val amount: Double,
    val balanceAfter: Double,
    val referenceId: String? = null,
    val referenceType: String? = null,
    val reason: String? = null,
    val createdAt: Long = 0L
)

@Serializable
data class LedgerPageResponse(
    val entries: List<LedgerEntryItem>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

@Serializable
data class TopupSuccessResponse(
    val status: String = "success",
    val tenantId: String,
    val creditsAdded: Double,
    val newBalance: Double,
    val reference: String = ""
)

@Serializable
data class SeatsListResponse(
    val seats: List<ai.orchestree.backend.database.repositories.workforce.User>,
    val totalActive: Int,
    val seatLimit: Int
)

@Serializable
data class AgentsListResponse(
    val agents: List<ai.orchestree.backend.database.repositories.workforce.AgentModel>,
    val totalActive: Int,
    val agentLimit: Int
)

@Serializable
data class PaymentInitiateResponse(
    val status: String = "pending",
    @kotlinx.serialization.SerialName("invoice_id")
    val invoiceId: String,
    @kotlinx.serialization.SerialName("payment_url")
    val paymentUrl: String?,
    @kotlinx.serialization.SerialName("snap_token")
    val snapToken: String?,
    val amount: Double,
    val currency: String = "IDR"
)

@Serializable
data class DowngradeBlockedResponse(
    val status: String = "blocked",
    val message: String,
    val overages: List<String>
)

@Serializable
data class DowngradeSuccessResponse(
    val status: String = "downgraded",
    val subscription: TenantSubscription
)

@Serializable
data class SubscriptionUpgradeResponse(
    val status: String = "upgraded",
    @kotlinx.serialization.SerialName("previous_plan")
    val previousPlan: String,
    @kotlinx.serialization.SerialName("target_plan")
    val targetPlan: String,
    @kotlinx.serialization.SerialName("prorated_amount")
    val proratedAmount: Double,
    @kotlinx.serialization.SerialName("credits_added")
    val creditsAdded: Double
)

@Serializable
data class TenantEntitlementsResponse(
    @kotlinx.serialization.SerialName("tenant_id")
    val tenantId: String,
    @kotlinx.serialization.SerialName("plan_id")
    val planId: String,
    @kotlinx.serialization.SerialName("plan_code")
    val planCode: String,
    @kotlinx.serialization.SerialName("plan_name")
    val planName: String,
    val status: String,
    @kotlinx.serialization.SerialName("is_founder_exclusive")
    val isFounderExclusive: Boolean,
    val entitlements: Map<String, String>
)

@Serializable
data class CreditsDisplayResponse(
    val available: Double,
    val reserved: Double,
    val used: Double,
    val total: Double
)

@Serializable
data class CommercialInvoiceRecord(
    val id: String,
    val tenantId: String,
    val invoiceNumber: String,
    val planName: String? = null,
    val periodStart: Long? = null,
    val periodEnd: Long? = null,
    val totalAmountIdr: Double,
    val status: String,
    val paymentGateway: String? = null,
    val gatewayOrderId: String? = null,
    val paymentUrl: String? = null,
    val snapToken: String? = null,
    val createdAt: Long? = null
)

@Serializable
data class SubscriptionStartRequest(
    val planId: String,
    val billingInterval: String = "monthly"
)

@Serializable
data class SubscriptionUpgradeRequest(
    val targetPlanId: String
)

@Serializable
data class SubscriptionDowngradeRequest(
    val targetPlanId: String
)

@Serializable
data class CreditTopupRequest(
    val amount: Double,
    val amountPaid: Double = 0.0,
    val currency: String = "IDR",
    val reference: String? = null
)

@Serializable
data class SeatCreateRequest(
    val name: String,
    val email: String,
    val role: String = "STAFF_HUMAN",
    val departmentId: String = "general"
)

@Serializable
data class BillingAgentCreateRequest(
    val name: String,
    val role: String = "Autonomous AI Specialist",
    val personaCode: String = "AGENT",
    val departmentId: String? = null
)

@Serializable
data class PaymentCreateRequest(
    val planId: String? = null,
    val invoiceId: String? = null,
    val amount: Double? = null,
    val currency: String = "IDR"
)

// =========================================================================
// FASE 114: SUPER ADMIN COMMERCIAL & CREDIT MANAGEMENT MODELS
// =========================================================================

@Serializable
data class EntitlementMatrixRow(
    val featureKey: String,
    val values: Map<String, String> // planCode -> entitlementValue ('included','limited','basic','advanced','enterprise','custom','not_included')
)

@Serializable
data class EntitlementMatrixResponse(
    val plans: List<String>,
    val rows: List<EntitlementMatrixRow>
)

@Serializable
data class EntitlementUpdateRequest(
    val planCode: String,
    val featureKey: String,
    val value: String
)

@Serializable
data class ManualCreditAdjustmentRequest(
    val tenantId: String,
    val amount: Double,
    val ledgerType: String, // 'CREDIT_ADJUSTMENT', 'CREDIT_BONUS', 'CREDIT_REFUNDED', 'CREDIT_EXPIRED'
    val reason: String,
    val operatorId: String = "superadmin@orchestree.ai"
)

@Serializable
data class ManualCreditAdjustmentResponse(
    val status: String,
    val tenantId: String,
    val amount: Double,
    val ledgerType: String,
    val newAvailableBalance: Double,
    val operatorId: String,
    val reason: String,
    val wallet: AiCreditWallet
)

@Serializable
data class TenantCustomOverrideRequest(
    val overrideJson: String
)

@Serializable
data class TenantCustomOverrideResponse(
    val tenantId: String,
    val customEntitlementOverride: String?
)

@Serializable
data class UnitEconomicsPlanDto(
    val planCode: String,
    val planName: String,
    val activeSubscribers: Int,
    val revenueIdr: Double,
    val aiCostIdr: Double,
    val cloudCostIdr: Double,
    val thirdPartyCostIdr: Double,
    val supportCostIdr: Double,
    val grossProfitIdr: Double,
    val grossMarginPercent: Double
)

@Serializable
data class FinancialCommandCenterResponse(
    val kpis: Map<String, Double>,
    val revenueBreakdown: Map<String, Double>,
    val unitEconomics: List<UnitEconomicsPlanDto>
)

@Serializable
data class CommercialPlanUpsertRequest(
    val id: String? = null,
    val planCode: String,
    val planName: String,
    val billingInterval: String = "monthly",
    val price: Double? = null,
    val currency: String = "IDR",
    val creditAllocation: Double? = null,
    val humanSeatLimit: Int? = null,
    val aiAgentLimit: Int? = null,
    val isPriceVisible: Boolean = true,
    val isActive: Boolean = true,
    val sortOrder: Int? = null
)

@Serializable
data class UpgradeRecommendationResponse(
    val level: String, // NO_UPGRADE, SOFT_RECOMMENDATION, STRONG_RECOMMENDATION, LIMIT_REACHED
    val title: String,
    val message: String,
    val suggestedPlanCode: String,
    val suggestedPlanName: String,
    val utilizationMetrics: Map<String, Double> = emptyMap()
)

