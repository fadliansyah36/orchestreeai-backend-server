package ai.orchestree.backend.database.repositories.payments

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.analytics.OrderRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.ConcurrentHashMap

class OrderRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val databaseUrl: String = AppConfig.load().supabase.databaseUrl
) {
    private val logger = LoggerFactory.getLogger(OrderRepository::class.java)
    private val memoryOrders = ConcurrentHashMap<String, OrderRecord>()

    init {
        seedInitialOrders()
    }

    private fun seedInitialOrders() {
        val now = System.currentTimeMillis()
        // 1. Paid Orders
        val paidOrders = listOf(
            OrderRecord("ord-01", "tenant-sample-001", "cust-01", "ORD-2026-001", 5000000.0, "paid", now - 86400000L),
            OrderRecord("ord-02", "tenant-sample-001", "cust-01", "ORD-2026-002", 2500000.0, "paid", now - 43200000L),
            OrderRecord("ord-03", "tenant-growth-002", "cust-03", "ORD-2026-003", 750000.0, "paid", now - 10800000L),
            OrderRecord("ord-04", "tenant-scale-003", "cust-04", "ORD-2026-004", 3500000.0, "paid", now - 5400000L)
        )
        // 2. Pending Orders (stuck > 10 min vs normal < 10 min)
        val pendingOrders = listOf(
            OrderRecord("ord-stuck-01", "tenant-sample-001", "cust-01", "ORD-2026-091", 1250000.0, "pending_payment", now - (25 * 60 * 1000L)),
            OrderRecord("ord-stuck-02", "tenant-growth-002", "cust-03", "ORD-2026-092", 850000.0, "pending_payment", now - (14 * 60 * 1000L)),
            OrderRecord("ord-recent-01", "tenant-scale-003", "cust-04", "ORD-2026-093", 450000.0, "pending_payment", now - (3 * 60 * 1000L))
        )
        paidOrders.forEach { memoryOrders[it.id] = it }
        pendingOrders.forEach { memoryOrders[it.id] = it }
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
        orderId: String,
        tenantId: String,
        customerId: String = "cust-001",
        orderNumber: String? = null,
        amount: Double = 0.0,
        status: String = "pending"
    ): OrderRecord = withContext(Dispatchers.IO) {
        val record = OrderRecord(
            id = orderId,
            tenantId = tenantId,
            customerId = customerId,
            orderNumber = orderNumber ?: "ORD-${orderId.takeLast(6)}",
            totalAmount = amount,
            status = status,
            createdAt = System.currentTimeMillis()
        )
        memoryOrders[orderId] = record

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = """
                        INSERT INTO orders (id, tenant_id, customer_id, order_number, total_amount, status, order_date)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, total_amount = EXCLUDED.total_amount
                    """.trimIndent()
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, record.id)
                    stmt.setString(2, record.tenantId)
                    stmt.setString(3, record.customerId)
                    stmt.setString(4, record.orderNumber)
                    stmt.setDouble(5, record.totalAmount)
                    stmt.setString(6, record.status)
                    stmt.setLong(7, record.createdAt)
                    stmt.executeUpdate()
                }
            } catch (e: Exception) {
                logger.warn("DB insert order failed, kept in synchronized memory: ${e.message}")
            }
        }
        record
    }

    suspend fun updateStatus(orderId: String, status: String): Boolean = withContext(Dispatchers.IO) {
        val existing = memoryOrders[orderId]
        if (existing != null) {
            memoryOrders[orderId] = existing.copy(status = status)
        }

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = "UPDATE orders SET status = ? WHERE id = ?"
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, status)
                    stmt.setString(2, orderId)
                    val rows = stmt.executeUpdate()
                    return@withContext rows > 0
                }
            } catch (e: Exception) {
                logger.warn("DB update order failed: ${e.message}")
            }
        }

        existing != null
    }

    suspend fun getById(orderId: String): OrderRecord? = withContext(Dispatchers.IO) {
        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = "SELECT id, tenant_id, customer_id, order_number, total_amount, status, order_date FROM orders WHERE id = ?"
                    val stmt = c.prepareStatement(sql)
                    stmt.setString(1, orderId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        return@withContext OrderRecord(
                            id = rs.getString("id"),
                            tenantId = rs.getString("tenant_id"),
                            customerId = rs.getString("customer_id"),
                            orderNumber = rs.getString("order_number"),
                            totalAmount = rs.getDouble("total_amount"),
                            status = rs.getString("status"),
                            createdAt = rs.getLong("order_date")
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warn("Query DB failed: ${e.message}")
            }
        }
        memoryOrders[orderId]
    }

    suspend fun listOrders(status: String? = null): List<OrderRecord> = withContext(Dispatchers.IO) {
        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val sql = if (status != null) {
                        "SELECT id, tenant_id, customer_id, order_number, total_amount, status, order_date FROM orders WHERE LOWER(status) = ? ORDER BY order_date DESC"
                    } else {
                        "SELECT id, tenant_id, customer_id, order_number, total_amount, status, order_date FROM orders ORDER BY order_date DESC"
                    }
                    val stmt = c.prepareStatement(sql)
                    if (status != null) {
                        stmt.setString(1, status.lowercase())
                    }
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<OrderRecord>()
                    while (rs.next()) {
                        list.add(
                            OrderRecord(
                                id = rs.getString("id"),
                                tenantId = rs.getString("tenant_id"),
                                customerId = rs.getString("customer_id"),
                                orderNumber = rs.getString("order_number"),
                                totalAmount = rs.getDouble("total_amount"),
                                status = rs.getString("status"),
                                createdAt = rs.getLong("order_date")
                            )
                        )
                    }
                    if (list.isNotEmpty()) return@withContext list
                }
            } catch (e: Exception) {
                logger.warn("Query DB failed: ${e.message}")
            }
        }

        val all = memoryOrders.values.sortedByDescending { it.createdAt }
        if (status != null) {
            val target = status.lowercase()
            all.filter {
                val s = it.status.lowercase()
                if (target == "paid") s == "paid"
                else if (target == "pending_payment" || target == "pending") s == "pending" || s == "pending_payment"
                else s == target
            }
        } else {
            all
        }
    }

    suspend fun listAll(): List<OrderRecord> = withContext(Dispatchers.IO) {
        listOrders(null)
    }
}

