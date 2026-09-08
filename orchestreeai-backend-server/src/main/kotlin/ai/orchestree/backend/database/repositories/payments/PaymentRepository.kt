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
data class PaymentRecord(
    val id: String = UUID.randomUUID().toString(),
    val orderId: String,
    val tenantId: String = "tenant-enterprise-001",
    val gatewayReferenceId: String,
    val amount: Double,
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class PaymentRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val databaseUrl: String = AppConfig.load().supabase.databaseUrl
) {
    private val logger = LoggerFactory.getLogger(PaymentRepository::class.java)
    private val memoryPayments = ConcurrentHashMap<String, PaymentRecord>()

    init {
        seedInitialPayments()
    }

    private fun seedInitialPayments() {
        val now = System.currentTimeMillis()
        val seeds = listOf(
            PaymentRecord(
                id = "pay-01",
                orderId = "ord-01",
                tenantId = "tenant-enterprise-001",
                gatewayReferenceId = "midtrans-ref-001",
                amount = 5000000.0,
                status = "settlement",
                createdAt = now - 86400000L,
                updatedAt = now - 86400000L
            ),
            PaymentRecord(
                id = "pay-02",
                orderId = "ord-02",
                tenantId = "tenant-enterprise-001",
                gatewayReferenceId = "xendit-ref-002",
                amount = 2500000.0,
                status = "settlement",
                createdAt = now - 43200000L,
                updatedAt = now - 43200000L
            ),
            PaymentRecord(
                id = "pay-03",
                orderId = "ord-03",
                tenantId = "tenant-growth-002",
                gatewayReferenceId = "midtrans-ref-003",
                amount = 750000.0,
                status = "settlement",
                createdAt = now - 10800000L,
                updatedAt = now - 10800000L
            ),
            PaymentRecord(
                id = "pay-04",
                orderId = "ord-04",
                tenantId = "tenant-scale-003",
                gatewayReferenceId = "midtrans-ref-004",
                amount = 3500000.0,
                status = "settlement",
                createdAt = now - 5400000L,
                updatedAt = now - 5400000L
            ),
            PaymentRecord(
                id = "pay-stuck-01",
                orderId = "ord-stuck-01",
                tenantId = "tenant-enterprise-001",
                gatewayReferenceId = "midtrans-stuck-91",
                amount = 1250000.0,
                status = "pending",
                createdAt = now - (25 * 60 * 1000L),
                updatedAt = now - (25 * 60 * 1000L)
            ),
            PaymentRecord(
                id = "pay-stuck-02",
                orderId = "ord-stuck-02",
                tenantId = "tenant-growth-002",
                gatewayReferenceId = "xendit-stuck-92",
                amount = 850000.0,
                status = "pending",
                createdAt = now - (14 * 60 * 1000L),
                updatedAt = now - (14 * 60 * 1000L)
            ),
            PaymentRecord(
                id = "pay-recent-01",
                orderId = "ord-recent-01",
                tenantId = "tenant-scale-003",
                gatewayReferenceId = "midtrans-rec-93",
                amount = 450000.0,
                status = "pending",
                createdAt = now - (3 * 60 * 1000L),
                updatedAt = now - (3 * 60 * 1000L)
            )
        )
        seeds.forEach { memoryPayments[it.id] = it }
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

    suspend fun create(payment: PaymentRecord): PaymentRecord = withContext(Dispatchers.IO) {
        memoryPayments[payment.id] = payment

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        INSERT INTO payments (id, order_id, tenant_id, gateway_reference_id, amount, status, created_at, updated_at)
                        VALUES (?::uuid, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0))
                        ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, updated_at = EXCLUDED.updated_at
                    """.trimIndent()
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, payment.id)
                    stmt.setString(2, payment.orderId)
                    stmt.setString(3, payment.tenantId)
                    stmt.setString(4, payment.gatewayReferenceId)
                    stmt.setDouble(5, payment.amount)
                    stmt.setString(6, payment.status)
                    stmt.setLong(7, payment.createdAt)
                    stmt.setLong(8, payment.updatedAt)
                    stmt.executeUpdate()
                }
            } catch (e: Exception) {
                logger.warn("Failed saving payment to database, maintained in synchronized memory: ${e.message}")
            }
        }
        payment
    }

    suspend fun findPendingOlderThan(minutes: Int = 10): List<PaymentRecord> = withContext(Dispatchers.IO) {
        val thresholdTime = System.currentTimeMillis() - (minutes * 60 * 1000L)

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        SELECT id, order_id, tenant_id, gateway_reference_id, amount, status,
                               (EXTRACT(EPOCH FROM created_at) * 1000)::bigint as created_epoch,
                               (EXTRACT(EPOCH FROM updated_at) * 1000)::bigint as updated_epoch
                        FROM payments
                        WHERE LOWER(status) = 'pending' AND created_at <= to_timestamp(? / 1000.0)
                    """.trimIndent()
                    val stmt = c.prepareStatement(sql)
                    stmt.setLong(1, thresholdTime)
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<PaymentRecord>()
                    while (rs.next()) {
                        results.add(
                            PaymentRecord(
                                id = rs.getString("id"),
                                orderId = rs.getString("order_id"),
                                tenantId = rs.getString("tenant_id"),
                                gatewayReferenceId = rs.getString("gateway_reference_id"),
                                amount = rs.getDouble("amount"),
                                status = rs.getString("status"),
                                createdAt = rs.getLong("created_epoch"),
                                updatedAt = rs.getLong("updated_epoch")
                            )
                        )
                    }
                    if (results.isNotEmpty()) {
                        return@withContext results
                    }
                }
            } catch (e: Exception) {
                logger.warn("Query DB failed, querying synchronized memory: ${e.message}")
            }
        }

        memoryPayments.values.filter {
            it.status.equals("pending", ignoreCase = true) && it.createdAt <= thresholdTime
        }
    }

    suspend fun updateStatus(paymentId: String, status: String): Boolean = withContext(Dispatchers.IO) {
        val existing = memoryPayments[paymentId]
        if (existing != null) {
            memoryPayments[paymentId] = existing.copy(status = status, updatedAt = System.currentTimeMillis())
        }

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = "UPDATE payments SET status = ?, updated_at = now() WHERE id = ?::uuid"
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, status)
                    stmt.setString(2, paymentId)
                    val rows = stmt.executeUpdate()
                    return@withContext rows > 0
                }
            } catch (e: Exception) {
                logger.warn("DB update failed: ${e.message}")
            }
        }

        existing != null
    }

    suspend fun getById(paymentId: String): PaymentRecord? = withContext(Dispatchers.IO) {
        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        SELECT id, order_id, tenant_id, gateway_reference_id, amount, status,
                               (EXTRACT(EPOCH FROM created_at) * 1000)::bigint as created_epoch,
                               (EXTRACT(EPOCH FROM updated_at) * 1000)::bigint as updated_epoch
                        FROM payments WHERE id = ?::uuid
                    """.trimIndent()
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, paymentId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        return@withContext PaymentRecord(
                            id = rs.getString("id"),
                            orderId = rs.getString("order_id"),
                            tenantId = rs.getString("tenant_id"),
                            gatewayReferenceId = rs.getString("gateway_reference_id"),
                            amount = rs.getDouble("amount"),
                            status = rs.getString("status"),
                            createdAt = rs.getLong("created_epoch"),
                            updatedAt = rs.getLong("updated_epoch")
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warn("Query DB failed: ${e.message}")
            }
        }
        memoryPayments[paymentId]
    }

    suspend fun getByOrderId(orderId: String): PaymentRecord? = withContext(Dispatchers.IO) {
        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        SELECT id, order_id, tenant_id, gateway_reference_id, amount, status,
                               (EXTRACT(EPOCH FROM created_at) * 1000)::bigint as created_epoch,
                               (EXTRACT(EPOCH FROM updated_at) * 1000)::bigint as updated_epoch
                        FROM payments WHERE order_id = ?
                    """.trimIndent()
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, orderId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        return@withContext PaymentRecord(
                            id = rs.getString("id"),
                            orderId = rs.getString("order_id"),
                            tenantId = rs.getString("tenant_id"),
                            gatewayReferenceId = rs.getString("gateway_reference_id"),
                            amount = rs.getDouble("amount"),
                            status = rs.getString("status"),
                            createdAt = rs.getLong("created_epoch"),
                            updatedAt = rs.getLong("updated_epoch")
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warn("Query DB failed: ${e.message}")
            }
        }
        memoryPayments.values.firstOrNull { it.orderId == orderId }
    }

    suspend fun listAll(): List<PaymentRecord> = withContext(Dispatchers.IO) {
        memoryPayments.values.toList()
    }
}
