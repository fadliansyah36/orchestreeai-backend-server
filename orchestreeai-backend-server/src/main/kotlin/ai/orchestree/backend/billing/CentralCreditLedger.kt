package ai.orchestree.backend.billing

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.analytics.AnalyticsRepository
import ai.orchestree.backend.orchestration.NodeResult
import ai.orchestree.backend.orchestration.WorkflowNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class CreditReservation(
    val id: String = "res-" + UUID.randomUUID().toString().take(12),
    val tenantId: String,
    val estimatedCredits: Double,
    val referenceType: String,
    val referenceId: String? = null,
    var status: String = "RESERVED", // "RESERVED", "CONSUMED", "RELEASED"
    val reservedAt: Long = System.currentTimeMillis()
)

@Serializable
data class CreditLedgerEntry(
    val id: String = "cld-" + UUID.randomUUID().toString().take(12),
    val tenantId: String,
    val amount: Double,
    val entryType: String, // "RESERVATION", "CONSUMPTION", "RELEASE", "TOPUP"
    val referenceType: String, // "selection_node", "omnichannel_chat", "generative_studio", etc.
    val referenceId: String? = null,
    val description: String,
    val balanceAfter: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TenantCreditSummary(
    val tenant_id: String,
    val balance_credits: Double,
    val reserved_credits: Double = 0.0,
    val available_credits: Double,
    val available_balance: Double = available_credits,
    val currency: String = "CREDITS",
    val total_consumed_this_month: Double,
    val status: String = "ACTIVE"
)

/**
 * Suggested Credit Matrix (PRD Master & Commercial / Billing Dokumen Sumber)
 * - Simple Chat / Prompt: 1.0 - 3.0 credits
 * - Ingestion & Clean: 2.0 - 5.0 credits
 * - Data Understanding / Schema Detection: 10.0 credits
 * - Scoring & Ranking / Data Analysis: 10.0 - 50.0 credits
 * - Deep Research: 50.0 - 300.0 credits
 * - Analytics & Chart Selection: 15.0 - 30.0 credits
 * - Strategic Recommendations & Grounded Insights: 10.0 - 20.0 credits
 */
class CreditEstimator {
    fun estimate(node: WorkflowNode): Double {
        val nodeId = node.id.lowercase()
        return when {
            nodeId.contains("ingest") || nodeId.contains("read-data") || nodeId.contains("clean") -> 2.5
            nodeId.contains("understand") || nodeId.contains("schema") -> 10.0
            nodeId.contains("validate") || nodeId.contains("quality") -> 5.0
            nodeId.contains("select") || nodeId.contains("calibrate") -> 5.0
            nodeId.contains("score") -> 25.0 // Data Analysis Matrix: 10-50
            nodeId.contains("rank") -> 10.0
            nodeId.contains("analyze") -> 20.0 // Statistical Distribution & Anomaly Analysis
            nodeId.contains("visualize") -> 10.0 // AI Chart Recommendation
            nodeId.contains("recommend") || nodeId.contains("insight") -> 15.0 // Strategic Insight Composer
            nodeId.contains("result") || nodeId.contains("deliver") -> 2.5
            nodeId.contains("deep-research") -> 100.0 // Deep Research Matrix: 50-300
            else -> 10.0
        }
    }

    fun estimateWorkflowTotal(nodes: List<WorkflowNode>): Double {
        return nodes.sumOf { estimate(it) }
    }
}

/**
 * Calculates actual credit costs dynamically based on LLM usage or operation compute.
 */
class CostIntelligenceEngine {
    fun calculateActualCost(
        llmUsage: Map<String, Any?>?,
        defaultCost: Double = 5.0
    ): Double {
        if (llmUsage == null) return defaultCost

        val promptTokens = (llmUsage["prompt_tokens"] as? Number)?.toDouble()
            ?: (llmUsage["promptTokens"] as? Number)?.toDouble() ?: 0.0
        val completionTokens = (llmUsage["completion_tokens"] as? Number)?.toDouble()
            ?: (llmUsage["completionTokens"] as? Number)?.toDouble() ?: 0.0
        val totalTokens = promptTokens + completionTokens

        return if (totalTokens > 0) {
            // Formula: 1 credit per 1000 total tokens + compute factor, minimum 1.0 credit
            val cost = (promptTokens * 0.0008) + (completionTokens * 0.002)
            Math.round(cost.coerceAtLeast(1.0) * 100.0) / 100.0
        } else {
            defaultCost
        }
    }
}

/**
 * Central Credit Ledger Service (Fase 113, Commercial & Billing System)
 *
 * Pola Eksekusi:
 * Reserve -> Execute -> Consume Actual -> Release Selisih (PERSIS Part IV Dokumen Sumber).
 *
 * SATU credit ledger & wallet untuk SELURUH fitur:
 * - Omnichannel Chat / Sales
 * - Generative Studio
 * - Universal AI Selection & Ranking
 */
class CentralCreditLedgerService(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val analyticsRepo: AnalyticsRepository = AnalyticsRepository.instance
) {
    private val logger = LoggerFactory.getLogger(CentralCreditLedgerService::class.java)

    val creditEstimator = CreditEstimator()
    val costIntelligenceEngine = CostIntelligenceEngine()

    private val activeReservations = ConcurrentHashMap<String, CreditReservation>()
    private val ledgerEntries = CopyOnWriteArrayList<CreditLedgerEntry>()
    private val localWalletBalances = ConcurrentHashMap<String, Double>()

    companion object {
        @Volatile
        private var defaultInstance: CentralCreditLedgerService? = null

        fun getInstance(): CentralCreditLedgerService {
            return defaultInstance ?: synchronized(this) {
                defaultInstance ?: CentralCreditLedgerService().also { defaultInstance = it }
            }
        }
    }

    private fun getDbConnection() = try {
        DatabaseManager.getConnection()
    } catch (e: Exception) {
        null
    }

    /**
     * Mengambil saldo kredit terkini tenant dari Central Credit Ledger.
     */
    fun getBalance(tenantId: String): Double {
        localWalletBalances[tenantId]?.let { return it }

        // Query directly from database connection if available
        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("SELECT (subscription_balance + topup_balance + bonus_balance) AS total_balance FROM ai_credit_wallets WHERE tenant_id = ?").use { ps ->
                        ps.setString(1, tenantId)
                        val rs = ps.executeQuery()
                        if (rs.next()) {
                            val bal = rs.getDouble("total_balance")
                            localWalletBalances[tenantId] = bal
                            return bal
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to read balance from ai_credit_wallets via JDBC: ${e.message}")
            }
        }

        // Fallback to shared analytics repository wallet
        val summary = runCatching {
            val usageList = kotlinx.coroutines.runBlocking { analyticsRepo.getUsageCredit() }
            usageList.firstOrNull { it.id == tenantId }?.balance
        }.getOrNull()

        val initial = summary ?: 12500.0 // Default active tenant balance
        localWalletBalances[tenantId] = initial
        return initial
    }

    /**
     * Membaca ringkasan kredit tenant (total saldo, terpakai, dan reservasi aktif).
     */
    suspend fun getTenantCreditSummary(tenantId: String): TenantCreditSummary = withContext(Dispatchers.IO) {
        val balance = getBalance(tenantId)
        val reserved = activeReservations.values
            .filter { it.tenantId == tenantId && it.status == "RESERVED" }
            .sumOf { it.estimatedCredits }

        val usageList = analyticsRepo.getUsageCredit()
        val totalUsage = usageList.firstOrNull { it.id == tenantId }?.total_usage_this_month ?: 0.0

        TenantCreditSummary(
            tenant_id = tenantId,
            balance_credits = balance,
            reserved_credits = reserved,
            available_credits = (balance - reserved).coerceAtLeast(0.0),
            currency = "CREDITS",
            total_consumed_this_month = totalUsage,
            status = "ACTIVE"
        )
    }

    /**
     * 1. RESERVE: Mengunci estimasi kredit sebelum node dieksekusi.
     */
    fun reserve(
        tenantId: String,
        estimatedCredits: Double,
        referenceType: String,
        referenceId: String? = null
    ): CreditReservation {
        val currentBalance = getBalance(tenantId)
        val currentlyReserved = activeReservations.values
            .filter { it.tenantId == tenantId && it.status == "RESERVED" }
            .sumOf { it.estimatedCredits }
        val available = currentBalance - currentlyReserved

        if (available < estimatedCredits) {
            throw InsufficientCreditException(
                "Saldo kredit tenant $tenantId tidak mencukupi. Tersedia: $available, Dibutuhkan reservasi: $estimatedCredits"
            )
        }

        val reservation = CreditReservation(
            tenantId = tenantId,
            estimatedCredits = estimatedCredits,
            referenceType = referenceType,
            referenceId = referenceId,
            status = "RESERVED"
        )
        activeReservations[reservation.id] = reservation

        val entry = CreditLedgerEntry(
            tenantId = tenantId,
            amount = estimatedCredits,
            entryType = "RESERVATION",
            referenceType = referenceType,
            referenceId = referenceId,
            description = "Reservasi kredit untuk $referenceType ($referenceId)",
            balanceAfter = available - estimatedCredits
        )
        ledgerEntries.add(entry)
        logger.info("[CREDIT_LEDGER] Reserved $estimatedCredits credits for tenant $tenantId (Reservation: ${reservation.id})")
        return reservation
    }

    /**
     * 2. CONSUME ACTUAL: Memotong kredit aktual yang terpakai ke wallet dan ledger global.
     */
    fun consume(
        reservationId: String,
        actualCredits: Double
    ): CreditLedgerEntry {
        val reservation = activeReservations[reservationId]
            ?: error("Reservation ID $reservationId tidak ditemukan atau sudah ditutup.")

        reservation.status = "CONSUMED"
        val tenantId = reservation.tenantId

        // Persist debit to PostgreSQL ai_credit_wallets & ai_credit_ledger
        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.autoCommit = false
                    try {
                        var remaining = actualCredits
                        val psSelect = c.prepareStatement("SELECT subscription_balance, bonus_balance, topup_balance, used_balance FROM ai_credit_wallets WHERE tenant_id = ? FOR UPDATE")
                        psSelect.setString(1, tenantId)
                        val rs = psSelect.executeQuery()
                        var subBal = 0.0
                        var bonusBal = 0.0
                        var topBal = 0.0
                        var usedBal = 0.0
                        if (rs.next()) {
                            subBal = rs.getDouble("subscription_balance")
                            bonusBal = rs.getDouble("bonus_balance")
                            topBal = rs.getDouble("topup_balance")
                            usedBal = rs.getDouble("used_balance")
                        }
                        rs.close()
                        psSelect.close()

                        val subDeduct = minOf(subBal, remaining)
                        remaining -= subDeduct
                        val bonusDeduct = minOf(bonusBal, remaining)
                        remaining -= bonusDeduct
                        val topDeduct = minOf(topBal, remaining)
                        remaining -= topDeduct

                        val newSub = subBal - subDeduct
                        val newBonus = bonusBal - bonusDeduct
                        val newTop = topBal - topDeduct
                        val newUsed = usedBal + actualCredits
                        val newBalance = newSub + newBonus + newTop

                        c.prepareStatement("UPDATE ai_credit_wallets SET subscription_balance = ?, bonus_balance = ?, topup_balance = ?, used_balance = ?, updated_at = now() WHERE tenant_id = ?").use { psUpdate ->
                            psUpdate.setDouble(1, newSub)
                            psUpdate.setDouble(2, newBonus)
                            psUpdate.setDouble(3, newTop)
                            psUpdate.setDouble(4, newUsed)
                            psUpdate.setString(5, tenantId)
                            psUpdate.executeUpdate()
                        }

                        c.prepareStatement(
                            "INSERT INTO ai_credit_ledger (id, tenant_id, idempotency_key, ledger_type, amount, balance_after, reference_type, reason, created_at) " +
                            "VALUES (?::uuid, ?, ?, 'CREDIT_CONSUMED', ?, ?, ?, ?, now())"
                        ).use { psLedger ->
                            psLedger.setString(1, UUID.randomUUID().toString())
                            psLedger.setString(2, tenantId)
                            psLedger.setString(3, "consume_" + UUID.randomUUID().toString())
                            psLedger.setDouble(4, -actualCredits)
                            psLedger.setDouble(5, newBalance)
                            psLedger.setString(6, reservation.referenceType)
                            psLedger.setString(7, "Konsumsi kredit aktual untuk ${reservation.referenceType}")
                            psLedger.executeUpdate()
                        }
                        c.commit()
                    } catch (ex: Exception) {
                        c.rollback()
                        throw ex
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not update PostgreSQL ai_credit_wallets & ai_credit_ledger: ${e.message}")
            }
        }

        // Also record to shared AnalyticsRepository
        analyticsRepo.recordCreditUsage(
            tenantId = tenantId,
            channelAccountId = reservation.referenceType,
            creditDeducted = actualCredits
        )

        val balanceBefore = localWalletBalances[tenantId] ?: getBalance(tenantId)
        val balanceAfter = (balanceBefore - actualCredits).coerceAtLeast(0.0)
        localWalletBalances[tenantId] = balanceAfter
        val ledgerEntry = CreditLedgerEntry(
            tenantId = tenantId,
            amount = actualCredits,
            entryType = "CONSUMPTION",
            referenceType = reservation.referenceType,
            referenceId = reservation.referenceId,
            description = "Konsumsi aktual kredit untuk ${reservation.referenceType} (${reservation.referenceId})",
            balanceAfter = balanceAfter
        )
        ledgerEntries.add(ledgerEntry)
        logger.info("[CREDIT_LEDGER] Consumed $actualCredits credits for tenant $tenantId. New Balance: $balanceAfter")
        return ledgerEntry
    }

    /**
     * 3. RELEASE SELISIH / ROLLBACK: Mengembalikan sisa reservasi ke tenant wallet.
     */
    fun release(
        reservationId: String,
        refundCredits: Double
    ) {
        val reservation = activeReservations[reservationId] ?: return
        reservation.status = "RELEASED"
        activeReservations.remove(reservationId)

        if (refundCredits > 0.0) {
            val balanceAfter = getBalance(reservation.tenantId)
            val ledgerEntry = CreditLedgerEntry(
                tenantId = reservation.tenantId,
                amount = refundCredits,
                entryType = "RELEASE",
                referenceType = reservation.referenceType,
                referenceId = reservation.referenceId,
                description = "Pelepasan sisa reservasi kredit (${reservation.id})",
                balanceAfter = balanceAfter
            )
            ledgerEntries.add(ledgerEntry)
            logger.info("[CREDIT_LEDGER] Released $refundCredits unused reserved credits back to tenant ${reservation.tenantId}")
        }
    }

    fun getAllLedgerEntries(tenantId: String): List<CreditLedgerEntry> {
        return ledgerEntries.filter { it.tenantId == tenantId }
    }

    fun getAllLedgerEntries(): List<CreditLedgerEntry> {
        return ledgerEntries.toList()
    }

    fun getSelectionCreditsConsumed(tenantId: String? = null): Double {
        return ledgerEntries
            .filter { entry ->
                entry.entryType == "CONSUMPTION" &&
                entry.referenceType.contains("selection", ignoreCase = true) &&
                (tenantId == null || entry.tenantId == tenantId)
            }
            .sumOf { it.amount }
    }

    fun getLedgerHistory(tenantId: String): List<CreditLedgerEntry> {
        return getAllLedgerEntries(tenantId)
    }

    fun getActiveReservationCount(): Int {
        return activeReservations.size
    }

    /**
     * LANGKAH 4: Eksekusi Selection Node dengan Pola Kredit Lengkap:
     * Reserve -> Execute -> Consume Actual -> Release Selisih
     */
    suspend fun executeSelectionNodeWithCredit(
        node: WorkflowNode,
        tenantId: String,
        context: MutableMap<String, Any> = mutableMapOf()
    ): NodeResult {
        val estimatedCost = creditEstimator.estimate(node)
        val reservation = reserve(tenantId, estimatedCost, referenceType = "selection_node", referenceId = node.id)
        try {
            val executionResult = node.execute(context)
            val result = executionResult.toNodeResult()
            
            val llmUsage = (context["llmUsage"] as? Map<String, Any?>) ?: executionResult.data
            val actualCost = costIntelligenceEngine.calculateActualCost(
                llmUsage = llmUsage,
                defaultCost = (estimatedCost * 0.8).coerceAtLeast(1.0)
            )

            consume(reservation.id, actualCost)
            release(reservation.id, (estimatedCost - actualCost).coerceAtLeast(0.0))
            return result
        } catch (e: Exception) {
            release(reservation.id, estimatedCost) // Rollback penuh jika gagal
            throw e
        }
    }
}

object CentralCreditLedger {
    fun deductCredit(tenantId: String, amount: Double, featureName: String, metadata: Map<String, Any> = emptyMap()) {
        val service = CentralCreditLedgerService.getInstance()
        val reservation = service.reserve(
            tenantId = tenantId,
            estimatedCredits = amount,
            referenceType = featureName,
            referenceId = metadata["referenceId"]?.toString()
        )
        service.consume(reservation.id, amount)
    }
}

