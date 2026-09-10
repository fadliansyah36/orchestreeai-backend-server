package ai.orchestree.backend.database.repositories.payments

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class PaymentReconciliationQueueItem(
    val id: String = UUID.randomUUID().toString(),
    val paymentId: String,
    val orderId: String,
    val tenantId: String,
    val detectedIssue: String,
    val gatewayReportedStatus: String? = null,
    val localStatus: String? = null,
    val resolutionStatus: String = "pending_review", // 'pending_review', 'resolved_confirmed', 'resolved_rejected'
    val resolvedBySuperAdminId: String? = null,
    val resolvedBy: String? = null,
    val resolutionReason: String? = null,
    val resolvedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val orderAmount: Double? = null,
    val gatewayAmount: Double? = null
)

class PaymentReconciliationQueueRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val databaseUrl: String = AppConfig.load().supabase.databaseUrl
) {
    private val logger = LoggerFactory.getLogger(PaymentReconciliationQueueRepository::class.java)
    private val memoryQueue = ConcurrentHashMap<String, PaymentReconciliationQueueItem>()

    init {
        seedInitialQueue()
    }

    private fun seedInitialQueue() {
        val now = System.currentTimeMillis()
        val item1 = PaymentReconciliationQueueItem(
            id = "prq-rev-01",
            paymentId = "pay-stuck-01",
            orderId = "ord-stuck-01",
            tenantId = "tenant-sample-001",
            detectedIssue = "amount_mismatch",
            gatewayReportedStatus = "settlement",
            localStatus = "pending_payment",
            resolutionStatus = "pending_review",
            orderAmount = 1250000.0,
            gatewayAmount = 1100000.0,
            createdAt = now - (25 * 60 * 1000L)
        )
        val item2 = PaymentReconciliationQueueItem(
            id = "prq-rev-02",
            paymentId = "pay-stuck-02",
            orderId = "ord-stuck-02",
            tenantId = "tenant-growth-002",
            detectedIssue = "webhook_not_received",
            gatewayReportedStatus = "settlement",
            localStatus = "pending_payment",
            resolutionStatus = "pending_review",
            orderAmount = 850000.0,
            gatewayAmount = 850000.0,
            createdAt = now - (14 * 60 * 1000L)
        )
        memoryQueue[item1.id] = item1
        memoryQueue[item2.id] = item2
    }

    private fun getDbConnection(): Connection? {
        return try {
            if (databaseUrl.isNotBlank() && databaseUrl.startsWith("postgres")) {
                val jdbcUrl = if (databaseUrl.startsWith("postgresql://")) {
                    "jdbc:" + databaseUrl
                } else {
                    databaseUrl
                }
                DriverManager.getConnection(jdbcUrl)
            } else null
        } catch (e: Throwable) {
            logger.debug("Database direct connection not available: ${e.message}")
            null
        }
    }

    suspend fun create(
        paymentId: String,
        orderId: String,
        tenantId: String,
        detectedIssue: String,
        gatewayReportedStatus: String?,
        localStatus: String?,
        orderAmount: Double? = null,
        gatewayAmount: Double? = null
    ): PaymentReconciliationQueueItem = withContext(Dispatchers.IO) {
        val item = PaymentReconciliationQueueItem(
            id = UUID.randomUUID().toString(),
            paymentId = paymentId,
            orderId = orderId,
            tenantId = tenantId,
            detectedIssue = detectedIssue,
            gatewayReportedStatus = gatewayReportedStatus,
            localStatus = localStatus,
            resolutionStatus = "pending_review",
            createdAt = System.currentTimeMillis(),
            orderAmount = orderAmount,
            gatewayAmount = gatewayAmount
        )
        memoryQueue[item.id] = item

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        INSERT INTO payment_reconciliation_queue (
                            id, payment_id, order_id, tenant_id, detected_issue,
                            gateway_reported_status, local_status, resolution_status, created_at
                        ) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0))
                    """.trimIndent()
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, item.id)
                    stmt.setString(2, item.paymentId)
                    stmt.setString(3, item.orderId)
                    stmt.setString(4, item.tenantId)
                    stmt.setString(5, item.detectedIssue)
                    stmt.setString(6, item.gatewayReportedStatus)
                    stmt.setString(7, item.localStatus)
                    stmt.setString(8, item.resolutionStatus)
                    stmt.setLong(9, item.createdAt)
                    stmt.executeUpdate()
                }
            } catch (e: Exception) {
                logger.warn("DB insert reconciliation queue failed, preserved in synchronized memory: ${e.message}")
            }
        }
        item
    }

    suspend fun markResolved(
        paymentId: String,
        resolutionStatus: String = "resolved_confirmed",
        resolvedBy: String = "system_auto_reconciliation",
        resolvedBySuperAdminId: String? = null,
        resolutionReason: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        var updated = false
        val now = System.currentTimeMillis()

        for ((id, item) in memoryQueue) {
            if (item.paymentId == paymentId) {
                memoryQueue[id] = item.copy(
                    resolutionStatus = resolutionStatus,
                    resolvedBy = resolvedBy,
                    resolvedBySuperAdminId = resolvedBySuperAdminId,
                    resolutionReason = resolutionReason,
                    resolvedAt = now
                )
                updated = true
            }
        }

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        UPDATE payment_reconciliation_queue
                        SET resolution_status = ?, resolved_by_super_admin_id = ?::uuid, resolved_at = now()
                        WHERE payment_id = ?::uuid
                    """.trimIndent()
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, resolutionStatus)
                    stmt.setString(2, resolvedBySuperAdminId ?: UUID.randomUUID().toString())
                    stmt.setString(3, paymentId)
                    val rows = stmt.executeUpdate()
                    if (rows > 0) updated = true
                }
            } catch (e: Exception) {
                logger.warn("DB markResolved failed: ${e.message}")
            }
        }
        updated
    }

    suspend fun markResolvedById(
        id: String,
        resolutionStatus: String = "resolved_confirmed",
        resolvedBy: String = "superadmin@orchestree.ai",
        resolvedBySuperAdminId: String? = null,
        resolutionReason: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        var updated = false
        val now = System.currentTimeMillis()

        val existing = memoryQueue[id]
        if (existing != null) {
            memoryQueue[id] = existing.copy(
                resolutionStatus = resolutionStatus,
                resolvedBy = resolvedBy,
                resolvedBySuperAdminId = resolvedBySuperAdminId,
                resolutionReason = resolutionReason,
                resolvedAt = now
            )
            updated = true
        }

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        UPDATE payment_reconciliation_queue
                        SET resolution_status = ?, resolved_by_super_admin_id = ?::uuid, resolved_at = now()
                        WHERE id = ?::uuid
                    """.trimIndent()
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, resolutionStatus)
                    stmt.setString(2, resolvedBySuperAdminId ?: UUID.randomUUID().toString())
                    stmt.setString(3, id)
                    val rows = stmt.executeUpdate()
                    if (rows > 0) updated = true
                }
            } catch (e: Exception) {
                logger.warn("DB markResolvedById failed: ${e.message}")
            }
        }
        updated
    }

    suspend fun getById(id: String): PaymentReconciliationQueueItem? = withContext(Dispatchers.IO) {
        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        SELECT id, payment_id, order_id, tenant_id, detected_issue,
                               gateway_reported_status, local_status, resolution_status,
                               resolved_by_super_admin_id,
                               (EXTRACT(EPOCH FROM created_at) * 1000)::bigint as created_epoch,
                               (EXTRACT(EPOCH FROM resolved_at) * 1000)::bigint as resolved_epoch
                        FROM payment_reconciliation_queue
                        WHERE id = ?::uuid
                        LIMIT 1
                    """.trimIndent()
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, id)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        return@withContext PaymentReconciliationQueueItem(
                            id = rs.getString("id"),
                            paymentId = rs.getString("payment_id"),
                            orderId = rs.getString("order_id"),
                            tenantId = rs.getString("tenant_id"),
                            detectedIssue = rs.getString("detected_issue"),
                            gatewayReportedStatus = rs.getString("gateway_reported_status"),
                            localStatus = rs.getString("local_status"),
                            resolutionStatus = rs.getString("resolution_status"),
                            resolvedBySuperAdminId = rs.getString("resolved_by_super_admin_id"),
                            createdAt = rs.getLong("created_epoch"),
                            resolvedAt = rs.getLong("resolved_epoch").takeIf { !rs.wasNull() }
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warn("Query DB failed: ${e.message}")
            }
        }

        memoryQueue[id]
    }


    suspend fun getByPaymentId(paymentId: String): PaymentReconciliationQueueItem? = withContext(Dispatchers.IO) {
        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        SELECT id, payment_id, order_id, tenant_id, detected_issue,
                               gateway_reported_status, local_status, resolution_status,
                               (EXTRACT(EPOCH FROM created_at) * 1000)::bigint as created_epoch,
                               (EXTRACT(EPOCH FROM resolved_at) * 1000)::bigint as resolved_epoch
                        FROM payment_reconciliation_queue
                        WHERE payment_id = ?::uuid
                        ORDER BY created_at DESC LIMIT 1
                    """.trimIndent()
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, paymentId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        return@withContext PaymentReconciliationQueueItem(
                            id = rs.getString("id"),
                            paymentId = rs.getString("payment_id"),
                            orderId = rs.getString("order_id"),
                            tenantId = rs.getString("tenant_id"),
                            detectedIssue = rs.getString("detected_issue"),
                            gatewayReportedStatus = rs.getString("gateway_reported_status"),
                            localStatus = rs.getString("local_status"),
                            resolutionStatus = rs.getString("resolution_status"),
                            createdAt = rs.getLong("created_epoch"),
                            resolvedAt = rs.getLong("resolved_epoch").takeIf { !rs.wasNull() }
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warn("Query DB failed: ${e.message}")
            }
        }

        memoryQueue.values.filter { it.paymentId == paymentId }.maxByOrNull { it.createdAt }
    }

    suspend fun listPendingReview(): List<PaymentReconciliationQueueItem> = withContext(Dispatchers.IO) {
        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        SELECT id, payment_id, order_id, tenant_id, detected_issue,
                               gateway_reported_status, local_status, resolution_status,
                               (EXTRACT(EPOCH FROM created_at) * 1000)::bigint as created_epoch,
                               (EXTRACT(EPOCH FROM resolved_at) * 1000)::bigint as resolved_epoch
                        FROM payment_reconciliation_queue
                        WHERE resolution_status = 'pending_review'
                        ORDER BY created_at DESC
                    """.trimIndent()
                    val rs = c.createStatement().executeQuery(sql)
                    val list = mutableListOf<PaymentReconciliationQueueItem>()
                    while (rs.next()) {
                        list.add(
                            PaymentReconciliationQueueItem(
                                id = rs.getString("id"),
                                paymentId = rs.getString("payment_id"),
                                orderId = rs.getString("order_id"),
                                tenantId = rs.getString("tenant_id"),
                                detectedIssue = rs.getString("detected_issue"),
                                gatewayReportedStatus = rs.getString("gateway_reported_status"),
                                localStatus = rs.getString("local_status"),
                                resolutionStatus = rs.getString("resolution_status"),
                                createdAt = rs.getLong("created_epoch"),
                                resolvedAt = rs.getLong("resolved_epoch").takeIf { !rs.wasNull() }
                            )
                        )
                    }
                    if (list.isNotEmpty()) return@withContext list
                }
            } catch (e: Exception) {
                logger.warn("Query DB failed: ${e.message}")
            }
        }

        memoryQueue.values.filter { it.resolutionStatus == "pending_review" }.sortedByDescending { it.createdAt }
    }

    suspend fun listAll(): List<PaymentReconciliationQueueItem> = withContext(Dispatchers.IO) {
        memoryQueue.values.sortedByDescending { it.createdAt }
    }
}
