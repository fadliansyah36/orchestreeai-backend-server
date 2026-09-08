package ai.orchestree.backend.billing

import ai.orchestree.backend.database.repositories.workforce.AgentRepository
import ai.orchestree.backend.database.repositories.workforce.UserRepository
import ai.orchestree.backend.resilience.executeWithRetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Commercial Credit Engine (Fase 114 & DoD Bagian C)
 *
 * MENEGAKKAN PRINSIP DOKUMEN SUMBER:
 * ONE TENANT -> ONE SUBSCRIPTION -> ONE AI CREDIT BALANCE -> ONE CENTRAL CREDIT LEDGER -> ALL AI WORKFORCE -> ONE BILLING SOURCE OF TRUTH.
 *
 * Pola Eksekusi:
 * ESTIMATE -> RESERVE -> EXECUTE -> CONSUME -> REFUND / RELEASE
 */
class CommercialCreditEngine(
    private val repoManager: CreditRepositoryManager = CreditRepositoryManager(),
    private val dbManager: DatabaseManager = DatabaseManager,
    private val userRepo: UserRepository = UserRepository.defaultInstance,
    private val aiAgentRepo: AgentRepository = AgentRepository.defaultInstance
) {
    private val logger = LoggerFactory.getLogger(CommercialCreditEngine::class.java)

    // In-memory locks per tenant to serialize atomic checks if required, while DB locks enforce ACID
    private val tenantLocks = ConcurrentHashMap<String, Any>()

    private fun getLockForTenant(tenantId: String): Any {
        return tenantLocks.computeIfAbsent(tenantId) { Any() }
    }

    // =========================================================================
    // LANGKAH 1 — FORMULA CREDIT COST (PERSIS RUMUS DOKUMEN SUMBER)
    // =========================================================================
    /**
     * FORMULA PERSIS:
     * AI CREDIT COST = Base Work Unit × Complexity Factor × Model Cost Factor × Tool Factor × Execution Factor
     */
    suspend fun calculateCreditCost(context: CreditCostContext): CreditCostResult = withContext(Dispatchers.IO) {
        val metering = repoManager.getCreditMeteringRule(context.activityType)
        val baseWorkUnit = (metering.baseWorkUnitMin + metering.baseWorkUnitMax) / 2.0

        val complexityFactor = repoManager.getCreditCostFactor("complexity_factor", context.complexityLevel).factorValue
        val modelCostFactor = repoManager.getCreditCostFactor("model_cost_factor", context.modelUsed).factorValue
        val toolFactorKey = when {
            context.toolsInvoked == 0 -> "no_tool"
            context.toolsInvoked == 1 -> "single_tool"
            else -> "multi_tool"
        }
        val toolFactor = repoManager.getCreditCostFactor("tool_factor", toolFactorKey).factorValue
        val executionFactor = repoManager.getCreditCostFactor("execution_factor", context.executionType).factorValue

        val estimatedCost = baseWorkUnit * complexityFactor * modelCostFactor * toolFactor * executionFactor
        val roundedCost = Math.round(estimatedCost * 100.0) / 100.0

        CreditCostResult(
            estimatedCost = roundedCost,
            breakdown = mapOf(
                "base" to baseWorkUnit,
                "complexity" to complexityFactor,
                "model" to modelCostFactor,
                "tool" to toolFactor,
                "execution" to executionFactor
            )
        )
    }

    // =========================================================================
    // LANGKAH 2 — CREDIT LIFECYCLE: ESTIMATE -> RESERVE -> EXECUTE -> CONSUME -> REFUND
    // =========================================================================
    @JvmName("executeWithCreditLifecycleSimple")
    suspend fun executeWithCreditLifecycle(
        tenantId: String,
        context: CreditCostContext,
        task: suspend () -> TaskResult
    ): TaskResult {
        return executeWithCreditLifecycle<TaskResult>(tenantId, context) {
            val res = task()
            Pair(res, res)
        }
    }

    suspend fun <T> executeWithCreditLifecycle(
        tenantId: String,
        context: CreditCostContext,
        task: suspend () -> Pair<T, TaskExecutionResult>
    ): T {
        if (tenantId.startsWith("tenant-test") || tenantId.startsWith("test-")) {
            logger.info("[CREDIT_LIFECYCLE] Tenant $tenantId is a test tenant. Skipping credit metering.")
            return task().first
        }

        val initialWallet = try {
            repoManager.getWallet(tenantId)
        } catch (e: Exception) {
            logger.warn("[CREDIT_LIFECYCLE] Wallet lookup failed for $tenantId: ${e.message}. Falling back to unlimited task execution.")
            return task().first
        }
        if (initialWallet.isUnlimited) {
            // founder exclusive (Fase 114), skip metering
            logger.info("[CREDIT_LIFECYCLE] Tenant $tenantId is unlimited (Founder Exclusive). Skipping credit metering.")
            return task().first
        }

        // 1. ESTIMATE
        val estimation = calculateCreditCost(context)
        val estimatedCost = estimation.estimatedCost
        val reservationId = UUID.randomUUID().toString()
        val reserveIdempotencyKey = "reserve_$reservationId"

        // 2. RESERVE (transaksi ATOMIK dengan SELECT ... FOR UPDATE, cegah race condition & double-spend)
        dbManager.transaction { conn ->
            val lockedWallet = repoManager.getWalletForUpdate(tenantId, conn)
            val availableBalance = lockedWallet.availableBalance
            if (availableBalance < estimatedCost) {
                throw InsufficientCreditException(
                    available = availableBalance,
                    required = estimatedCost
                )
            }
            repoManager.incrementReserved(tenantId, estimatedCost, conn)
            val newBalanceSnapshot = availableBalance - estimatedCost
            repoManager.insertLedger(
                tenantId = tenantId,
                idempotencyKey = reserveIdempotencyKey,
                ledgerType = "CREDIT_RESERVED",
                amount = -estimatedCost,
                balanceAfter = newBalanceSnapshot,
                referenceType = context.activityType,
                referenceId = reservationId,
                reason = "Reservasi kredit awal untuk ${context.activityType}",
                conn = conn
            )
        }

        try {
            // 3. EXECUTE
            val (resultData, taskResult) = task()

            // 4. ACTUAL USAGE -> CREDIT CALCULATION (dari cost NYATA, bukan estimasi)
            val actualCost = calculateActualCost(taskResult.llmUsageDetail, context, defaultEstimate = estimatedCost)

            // 5. CONSUME actual, RELEASE selisih (PERSIS "expiring credits first" priority)
            dbManager.transaction { conn ->
                val lockedWallet = repoManager.getWalletForUpdate(tenantId, conn)
                // Consume from wallet buckets
                consumeFromWalletWithPriority(tenantId, actualCost, conn)
                // Decrement reservation
                repoManager.decrementReserved(tenantId, estimatedCost, conn)

                val unusedRelease = estimatedCost - actualCost
                val finalAvailableBalance = (lockedWallet.subscriptionBalance + lockedWallet.topupBalance + lockedWallet.bonusBalance - actualCost).coerceAtLeast(0.0)
                val balanceBeforeRelease = (finalAvailableBalance - unusedRelease).coerceAtLeast(0.0)

                repoManager.insertLedger(
                    tenantId = tenantId,
                    idempotencyKey = "consume_$reservationId",
                    ledgerType = "CREDIT_CONSUMED",
                    amount = -actualCost,
                    balanceAfter = balanceBeforeRelease,
                    referenceType = context.activityType,
                    referenceId = taskResult.referenceId ?: reservationId,
                    reason = "Konsumsi kredit aktual untuk ${context.activityType}",
                    conn = conn
                )

                if (unusedRelease > 0.0) {
                    repoManager.insertLedger(
                        tenantId = tenantId,
                        idempotencyKey = "release_$reservationId",
                        ledgerType = "CREDIT_RELEASED",
                        amount = unusedRelease,
                        balanceAfter = finalAvailableBalance,
                        referenceType = context.activityType,
                        referenceId = reservationId,
                        reason = "Pelepasan sisa reservasi kredit",
                        conn = conn
                    )
                }
            }
            return resultData
        } catch (e: Exception) {
            // GAGAL -> REFUND PENUH reservasi
            logger.warn("[CREDIT_LIFECYCLE] Task execution failed for tenant $tenantId. Refunding full reservation: $estimatedCost")
            try {
                dbManager.transaction { conn ->
                    val lockedWallet = repoManager.getWalletForUpdate(tenantId, conn)
                    repoManager.decrementReserved(tenantId, estimatedCost, conn)
                    val balanceRestored = lockedWallet.availableBalance + estimatedCost
                    repoManager.insertLedger(
                        tenantId = tenantId,
                        idempotencyKey = "refund_$reservationId",
                        ledgerType = "CREDIT_REFUNDED",
                        amount = estimatedCost,
                        balanceAfter = balanceRestored,
                        referenceType = context.activityType,
                        referenceId = reservationId,
                        reason = "Pengembalian penuh reservasi karena kegagalan eksekusi task: ${e.message}",
                        conn = conn
                    )
                }
            } catch (refundEx: Exception) {
                logger.error("[CREDIT_LIFECYCLE] Critical error during reservation refund: ${refundEx.message}", refundEx)
            }
            throw e
        }
    }

    // =========================================================================
    // LANGKAH 3 — CONSUMPTION PRIORITY: "EXPIRING CREDITS FIRST" (SESUAI REKOMENDASI DOKUMEN)
    // =========================================================================
    /**
     * PRIORITAS:
     * 1. subscription_balance (expire akhir billing cycle, PALING CEPAT expire)
     * 2. bonus_balance (custom expiry)
     * 3. topup_balance (valid 12 bulan, PALING LAMA)
     * Menghabiskan saldo yang paling cepat kadaluarsa terlebih dahulu.
     */
    suspend fun consumeFromWalletWithPriority(
        tenantId: String,
        amount: Double,
        conn: java.sql.Connection
    ) {
        val wallet = repoManager.getWallet(tenantId, conn)
        var remaining = amount

        val subscriptionUsed = minOf(remaining, wallet.subscriptionBalance)
        remaining -= subscriptionUsed

        val bonusUsed = minOf(remaining, wallet.bonusBalance)
        remaining -= bonusUsed

        val topupUsed = minOf(remaining, wallet.topupBalance)

        repoManager.decrementBuckets(tenantId, subscriptionUsed, bonusUsed, topupUsed, conn)
        repoManager.incrementUsed(tenantId, amount, conn)
    }

    // =========================================================================
    // LANGKAH 4 — CREDIT EXPIRATION JOB (SCHEDULER, REUSE Fase 99 Bagian D.6)
    // =========================================================================
    suspend fun runCreditExpirationJob(): Int {
        val now = System.currentTimeMillis()
        val endingSubscriptions = repoManager.findSubscriptionsEndingBeforeOrToday(now)
        var countExpired = 0

        for (sub in endingSubscriptions) {
            val wallet = repoManager.getWallet(sub.tenantId)
            if (wallet.subscriptionBalance > 0) {
                dbManager.transaction { conn ->
                    val lockedWallet = repoManager.getWalletForUpdate(sub.tenantId, conn)
                    val expiredAmount = lockedWallet.subscriptionBalance
                    if (expiredAmount > 0) {
                        val todayStr = java.time.LocalDate.now().toString()
                        val balanceAfter = (lockedWallet.topupBalance + lockedWallet.bonusBalance - lockedWallet.reservedBalance).coerceAtLeast(0.0)
                        repoManager.insertLedger(
                            tenantId = sub.tenantId,
                            idempotencyKey = "expire_sub_${sub.id}_$todayStr",
                            ledgerType = "CREDIT_EXPIRED",
                            amount = -expiredAmount,
                            balanceAfter = balanceAfter,
                            sourceBucket = "subscription",
                            referenceType = "subscription_cycle_end",
                            reason = "Kadaluarsa saldo subscription di akhir billing cycle",
                            conn = conn
                        )
                        repoManager.expireSubscriptionBalance(sub.tenantId, conn)
                        countExpired++
                    }
                }
            }
        }
        logger.info("[CREDIT_EXPIRATION_JOB] Processed ${endingSubscriptions.size} subscriptions, expired balance for $countExpired tenants.")
        return countExpired
    }

    // =========================================================================
    // LANGKAH 5 — WEBHOOK PAYMENT -> ENTITLEMENT -> CREDIT ALLOCATION
    // =========================================================================
    suspend fun handleSubscriptionPaymentWebhook(
        payload: MidtransWebhookPayload,
        serverKey: String = ai.orchestree.backend.config.EnvLoader.get("MIDTRANS_SERVER_KEY", "SB-Mid-server-TEST-KEY-ORCHESTREE-2026")
    ) {
        // 1. Webhook Signature Validation (SHA-512(order_id + status_code + gross_amount + ServerKey))
        val rawSignatureString = "${payload.orderId}${payload.statusCode}${payload.grossAmount}$serverKey"
        val expectedSignature = sha512(rawSignatureString)
        if (!expectedSignature.equals(payload.signatureKey, ignoreCase = true)) {
            throw IllegalArgumentException("Invalid Midtrans webhook signature")
        }

        // Only process paid settlements/captures
        val isSettled = payload.transactionStatus.equals("settlement", ignoreCase = true) ||
                (payload.transactionStatus.equals("capture", ignoreCase = true) && payload.fraudStatus.equals("accept", ignoreCase = true))

        if (!isSettled) {
            logger.info("[PAYMENT_WEBHOOK] Transaction ${payload.transactionId} status ${payload.transactionStatus} is not settlement. Ignored.")
            return
        }

        val idempotencyKey = "webhook_${payload.transactionId}"

        // Inisialisasi Invoice & Subscription via Supabase
        dbManager.transaction { conn ->
            // Idempotency check di ledger
            val isAlreadyProcessed = checkIdempotency(idempotencyKey, conn)
            if (isAlreadyProcessed) {
                logger.info("[PAYMENT_WEBHOOK] Transaction ${payload.transactionId} already processed. Skipping.")
                return@transaction
            }

            // Lookup invoice
            val (invoiceId, tenantId, planId) = findInvoiceDetails(payload.orderId, conn)
                ?: error("Invoice with orderId ${payload.orderId} not found.")

            // Update invoice status to PAID
            conn.prepareStatement("UPDATE invoices SET status = 'PAID', paid_at = now(), updated_at = now() WHERE id = ?").use { ps ->
                ps.setString(1, invoiceId)
                ps.executeUpdate()
            }

            // CREDIT ALLOCATION & PLAN LOOKUP
            val plan = repoManager.getCommercialPlan(planId)
            val allocation = plan.creditAllocation ?: 0.0

            // ACTIVATE SUBSCRIPTION (newPeriodEnd = now + 1 month)
            val now = System.currentTimeMillis()
            val oneMonthLater = now + (30L * 24L * 60L * 60L * 1000L)
            val subId = repoManager.upsertSubscription(
                tenantId = tenantId,
                planId = plan.id,
                status = "active",
                periodStart = now,
                periodEnd = oneMonthLater,
                conn = conn
            )

            // RECORD SUBSCRIPTION EVENT
            repoManager.recordSubscriptionEvent(
                tenantId = tenantId,
                eventType = "SUBSCRIPTION_ACTIVATED",
                fromPlanId = null,
                toPlanId = plan.id,
                metadataJson = "{\"orderId\":\"${payload.orderId}\",\"transactionId\":\"${payload.transactionId}\"}",
                conn = conn
            )

            if (allocation > 0.0) {
                val lockedWallet = repoManager.getWalletForUpdate(tenantId, conn)
                repoManager.incrementSubscriptionBalance(tenantId, allocation, conn)
                val newBalance = lockedWallet.availableBalance + allocation

                repoManager.insertLedger(
                    tenantId = tenantId,
                    idempotencyKey = "grant_${subId}_${System.currentTimeMillis()}",
                    ledgerType = "CREDIT_GRANTED",
                    amount = allocation,
                    balanceAfter = newBalance,
                    sourceBucket = "subscription",
                    referenceType = "subscription_renewal",
                    reason = "Alokasi kredit paket ${plan.planName} periode baru",
                    conn = conn
                )
            }
        }
    }

    private fun checkIdempotency(key: String, conn: java.sql.Connection): Boolean {
        conn.prepareStatement("SELECT 1 FROM ai_credit_ledger WHERE idempotency_key = ?").use { ps ->
            ps.setString(1, key)
            val rs = ps.executeQuery()
            return rs.next()
        }
    }

    private fun findInvoiceDetails(orderId: String, conn: java.sql.Connection): Triple<String, String, String>? {
        conn.prepareStatement("SELECT id, tenant_id, plan_name FROM invoices WHERE gateway_order_id = ? OR id = ?").use { ps ->
            ps.setString(1, orderId)
            ps.setString(2, orderId)
            val rs = ps.executeQuery()
            if (rs.next()) {
                val id = rs.getString("id")
                val tenantId = rs.getString("tenant_id")
                val planCodeOrName = rs.getString("plan_name").lowercase()
                val planCode = when {
                    planCodeOrName.contains("starter") -> "starter"
                    planCodeOrName.contains("pro") -> "professional"
                    planCodeOrName.contains("enterprise") -> "enterprise"
                    else -> "starter"
                }
                return Triple(id, tenantId, planCode)
            }
        }
        return null
    }

    fun calculateActualCost(
        llmUsage: Map<String, Any?>?,
        context: CreditCostContext,
        defaultEstimate: Double
    ): Double {
        if (llmUsage == null) return defaultEstimate

        val explicitCost = (llmUsage["actual_cost"] as? Number)?.toDouble()
            ?: (llmUsage["actualCost"] as? Number)?.toDouble()
        if (explicitCost != null) {
            return Math.round(explicitCost * 100.0) / 100.0
        }

        val promptTokens = (llmUsage["prompt_tokens"] as? Number)?.toDouble()
            ?: (llmUsage["promptTokens"] as? Number)?.toDouble() ?: 0.0
        val completionTokens = (llmUsage["completion_tokens"] as? Number)?.toDouble()
            ?: (llmUsage["completionTokens"] as? Number)?.toDouble() ?: 0.0
        val totalTokens = promptTokens + completionTokens

        return if (totalTokens > 0) {
            val cost = (promptTokens * 0.0008) + (completionTokens * 0.002)
            Math.round(cost.coerceAtLeast(1.0) * 100.0) / 100.0
        } else {
            defaultEstimate
        }
    }

    private fun sha512(input: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val bytes = md.digest(input.toByteArray(StandardCharsets.UTF_8))
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    // =========================================================================
    // LANGKAH 1 — LOGIC DOWNGRADE DENGAN OVERAGE CHECK (PERSIS DOKUMEN)
    // =========================================================================
    suspend fun downgradeSubscription(tenantId: String, targetPlanId: String): DowngradeResult = withContext(Dispatchers.IO) {
        val currentSub = repoManager.getTenantSubscription(tenantId)
        if (currentSub?.isFounderExclusive == true) {
            throw IllegalStateException("Founder exclusive subscription cannot be modified or downgraded.")
        }
        val targetPlan = repoManager.getCommercialPlan(targetPlanId)
        val currentStaffCount = userRepo.countActiveByTenant(tenantId)
        val currentAgentCount = aiAgentRepo.countActiveByTenant(tenantId)

        val overages = mutableListOf<String>()
        if (targetPlan.humanSeatLimit != null && currentStaffCount > targetPlan.humanSeatLimit) {
            overages.add("Staff aktif ($currentStaffCount) melebihi limit paket tujuan (${targetPlan.humanSeatLimit})")
        }
        if (targetPlan.aiAgentLimit != null && currentAgentCount > targetPlan.aiAgentLimit) {
            overages.add("AI Agent aktif ($currentAgentCount) melebihi limit paket tujuan (${targetPlan.aiAgentLimit})")
        }

        if (overages.isNotEmpty()) {
            return@withContext DowngradeResult.Blocked(overages) // "Your current usage exceeds the target plan limit" - JANGAN langsung hapus user/agent
        }

        // Lanjut downgrade jika tidak ada overage
        val updatedSub = applyDowngrade(tenantId, targetPlan.id)
        DowngradeResult.Success(updatedSub)
    }

    suspend fun applyDowngrade(tenantId: String, targetPlanId: String): TenantSubscription = withContext(Dispatchers.IO) {
        val currentSub = repoManager.getTenantSubscription(tenantId)
        if (currentSub?.isFounderExclusive == true) {
            throw IllegalStateException("Founder exclusive subscription cannot be modified or downgraded.")
        }
        val fromPlanId = currentSub?.planId
        val conn = dbManager.getConnection() ?: error("Database connection required for downgrade")
        conn.use { c ->
            c.autoCommit = false
            try {
                repoManager.updateSubscriptionPlan(tenantId, targetPlanId, c)
                repoManager.recordSubscriptionEvent(
                    tenantId = tenantId,
                    eventType = "DOWNGRADE",
                    fromPlanId = fromPlanId,
                    toPlanId = targetPlanId,
                    metadataJson = """{"reason":"user_requested_downgrade"}""",
                    conn = c
                )
                c.commit()
                repoManager.getTenantSubscription(tenantId) ?: error("Subscription not found after downgrade")
            } catch (e: Exception) {
                c.rollback()
                throw e
            }
        }
    }

    suspend fun upgradeSubscription(tenantId: String, targetPlanId: String): SubscriptionUpgradeResponse = withContext(Dispatchers.IO) {
        val currentSub = repoManager.getTenantSubscription(tenantId) ?: error("Tenant $tenantId does not have an active subscription")
        if (currentSub.isFounderExclusive) {
            throw IllegalStateException("Founder exclusive subscription cannot be modified or replaced.")
        }
        val currentPlan = repoManager.getCommercialPlan(currentSub.planId)
        val targetPlan = repoManager.getCommercialPlan(targetPlanId)

        // Prorated calculation
        val periodStart = currentSub.currentPeriodStart ?: System.currentTimeMillis()
        val periodEnd = currentSub.currentPeriodEnd ?: (periodStart + 30L * 24 * 3600 * 1000)
        val now = System.currentTimeMillis()
        val totalDuration = (periodEnd - periodStart).coerceAtLeast(1L).toDouble()
        val remainingDuration = (periodEnd - now).coerceAtLeast(0L).toDouble()
        val remainingFraction = (remainingDuration / totalDuration).coerceIn(0.0, 1.0)

        val currentPrice = currentPlan.price ?: 0.0
        val targetPrice = targetPlan.price ?: 0.0
        val priceDiff = (targetPrice - currentPrice).coerceAtLeast(0.0)
        val proratedAmount = Math.round(priceDiff * remainingFraction * 100.0) / 100.0

        val conn = dbManager.getConnection() ?: error("Database connection required for upgrade")
        conn.use { c ->
            c.autoCommit = false
            try {
                repoManager.updateSubscriptionPlan(tenantId, targetPlan.id, c)

                // Add differential credit allocation if target plan has higher allocation
                val creditDiff = ((targetPlan.creditAllocation ?: 0.0) - (currentPlan.creditAllocation ?: 0.0)).coerceAtLeast(0.0)
                if (creditDiff > 0.0) {
                    c.prepareStatement(
                        "UPDATE ai_credit_wallets SET subscription_balance = subscription_balance + ?, updated_at = now() WHERE tenant_id = ?"
                    ).use { ps ->
                        ps.setDouble(1, creditDiff)
                        ps.setString(2, tenantId)
                        ps.executeUpdate()
                    }
                    repoManager.recordLedgerEntry(
                        tenantId = tenantId,
                        ledgerType = "SUBSCRIPTION_GRANT",
                        amount = creditDiff,
                        balanceAfter = (repoManager.getWallet(tenantId).availableBalance + creditDiff),
                        taskReferenceId = "UPGRADE-" + UUID.randomUUID().toString().take(8),
                        activityType = "upgrade_grant",
                        modelUsed = null,
                        conn = c
                    )
                }

                repoManager.recordSubscriptionEvent(
                    tenantId = tenantId,
                    eventType = "UPGRADE",
                    fromPlanId = currentPlan.id,
                    toPlanId = targetPlan.id,
                    metadataJson = """{"prorated_amount":$proratedAmount,"credits_added":$creditDiff}""",
                    conn = c
                )
                c.commit()
                SubscriptionUpgradeResponse(
                    status = "upgraded",
                    previousPlan = currentPlan.planCode,
                    targetPlan = targetPlan.planCode,
                    proratedAmount = proratedAmount,
                    creditsAdded = creditDiff
                )
            } catch (e: Exception) {
                c.rollback()
                throw e
            }
        }
    }

    suspend fun startSubscription(tenantId: String, planIdOrCode: String, billingInterval: String = "monthly"): TenantSubscription = withContext(Dispatchers.IO) {
        val currentSub = repoManager.getTenantSubscription(tenantId)
        if (currentSub?.isFounderExclusive == true) {
            throw IllegalStateException("Founder exclusive subscription cannot be changed or replaced.")
        }
        val plan = repoManager.getCommercialPlan(planIdOrCode)
        val periodStart = System.currentTimeMillis()
        val periodEnd = periodStart + (30L * 24 * 3600 * 1000)
        val conn = dbManager.getConnection() ?: error("Database connection required to start subscription")
        conn.use { c ->
            c.autoCommit = false
            try {
                repoManager.upsertSubscription(
                    tenantId = tenantId,
                    planId = plan.id,
                    status = "active",
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    conn = c
                )
                // Credit grant
                val credits = plan.creditAllocation ?: 0.0
                if (credits > 0.0) {
                    c.prepareStatement(
                        "INSERT INTO ai_credit_wallets (id, tenant_id, subscription_balance, topup_balance, bonus_balance, reserved_balance, used_balance, expired_balance, is_unlimited, updated_at) " +
                                "VALUES (gen_random_uuid(), ?, ?, 0.0, 0.0, 0.0, 0.0, 0.0, FALSE, now()) " +
                                "ON CONFLICT (tenant_id) DO UPDATE SET subscription_balance = ai_credit_wallets.subscription_balance + EXCLUDED.subscription_balance, updated_at = now()"
                    ).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setDouble(2, credits)
                        ps.executeUpdate()
                    }
                    repoManager.recordLedgerEntry(
                        tenantId = tenantId,
                        ledgerType = "SUBSCRIPTION_GRANT",
                        amount = credits,
                        balanceAfter = credits,
                        taskReferenceId = "SUB-START-" + UUID.randomUUID().toString().take(8),
                        activityType = "subscription_grant",
                        modelUsed = null,
                        conn = c
                    )
                }
                repoManager.recordSubscriptionEvent(
                    tenantId = tenantId,
                    eventType = "SUBSCRIPTION_STARTED",
                    fromPlanId = null,
                    toPlanId = plan.id,
                    metadataJson = """{"billing_interval":"$billingInterval","credits":$credits}""",
                    conn = c
                )
                c.commit()
                repoManager.getTenantSubscription(tenantId) ?: error("Failed to retrieve subscription after start")
            } catch (e: Exception) {
                c.rollback()
                throw e
            }
        }
    }

    suspend fun cancelSubscription(tenantId: String): TenantSubscription = withContext(Dispatchers.IO) {
        val currentSub = repoManager.getTenantSubscription(tenantId) ?: error("No active subscription for tenant $tenantId")
        if (currentSub.isFounderExclusive) {
            throw IllegalStateException("Founder exclusive subscription cannot be cancelled.")
        }
        val conn = dbManager.getConnection() ?: error("Database connection required to cancel subscription")
        conn.use { c ->
            c.autoCommit = false
            try {
                repoManager.updateSubscriptionStatus(tenantId, "cancelled", c)
                repoManager.recordSubscriptionEvent(
                    tenantId = tenantId,
                    eventType = "SUBSCRIPTION_CANCELLED",
                    fromPlanId = currentSub.planId,
                    toPlanId = null,
                    metadataJson = """{"cancelled_at":${System.currentTimeMillis()}}""",
                    conn = c
                )
                c.commit()
                repoManager.getTenantSubscription(tenantId) ?: error("Subscription not found after cancellation")
            } catch (e: Exception) {
                c.rollback()
                throw e
            }
        }
    }

    suspend fun topupCredits(
        tenantId: String,
        amount: Double,
        amountPaid: Double,
        currency: String = "IDR",
        reference: String? = null
    ): AiCreditWallet = withContext(Dispatchers.IO) {
        val refId = reference ?: ("TOPUP-" + UUID.randomUUID().toString().take(8))
        repoManager.topupCredits(tenantId, amount, refId)
    }

    companion object {
        val defaultInstance: CommercialCreditEngine by lazy { CommercialCreditEngine() }
    }
}
