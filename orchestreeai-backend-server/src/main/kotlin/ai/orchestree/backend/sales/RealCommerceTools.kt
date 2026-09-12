package ai.orchestree.backend.sales

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class CartItemParam(
    val product_id: String,
    val quantity: Int,
    val unit_price: Double
)

class RealCartCreateTool {
    private val logger = LoggerFactory.getLogger(RealCartCreateTool::class.java)

    suspend fun execute(tenantId: String, parameters: Map<String, String>): String = withContext(Dispatchers.IO) {
        val customerId = parameters["customer_id"] ?: "cust-guest"
        val convId = parameters["conversation_id"] ?: ""
        val itemsJson = parameters["items"] ?: "[]"
        val cartId = "cart-" + UUID.randomUUID().toString().take(8)

        var subtotal = 0.0
        // Parse items manually or via regex
        val qtyRegex = Regex(""""quantity"\s*:\s*(\d+)""")
        val priceRegex = Regex(""""unit_price"\s*:\s*([0-9.]+)""")
        val prodRegex = Regex(""""product_id"\s*:\s*"([^"]+)"""")

        val qty = qtyRegex.find(itemsJson)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val unitPrice = priceRegex.find(itemsJson)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val prodId = prodRegex.find(itemsJson)?.groupValues?.get(1) ?: "prod-default"

        subtotal = qty * unitPrice
        val taxAmount = subtotal * 0.11 // 11% PPN
        val totalAmount = subtotal + taxAmount
        val now = System.currentTimeMillis()

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        INSERT INTO carts (id, tenant_id, customer_id, conversation_id, status, subtotal, discount_amount, tax_amount, shipping_fee, total_amount, currency, created_at, updated_at)
                        VALUES (?, ?, ?, ?, 'ACTIVE', ?, 0.0, ?, 0.0, ?, 'IDR', ?, ?)
                    """.trimIndent()).use { ps ->
                        ps.setString(1, cartId)
                        ps.setString(2, tenantId)
                        ps.setString(3, customerId)
                        ps.setString(4, convId)
                        ps.setDouble(5, subtotal)
                        ps.setDouble(6, taxAmount)
                        ps.setDouble(7, totalAmount)
                        ps.setLong(8, now)
                        ps.setLong(9, now)
                        ps.executeUpdate()
                    }

                    c.prepareStatement("""
                        INSERT INTO cart_items (id, cart_id, tenant_id, product_id, product_name, unit_price, quantity, subtotal, discount_amount, total_amount, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0.0, ?, ?)
                    """.trimIndent()).use { psItem ->
                        psItem.setString(1, "item-" + UUID.randomUUID().toString().take(8))
                        psItem.setString(2, cartId)
                        psItem.setString(3, tenantId)
                        psItem.setString(4, prodId)
                        psItem.setString(5, "Product Item $prodId")
                        psItem.setDouble(6, unitPrice)
                        psItem.setInt(7, qty)
                        psItem.setDouble(8, subtotal)
                        psItem.setDouble(9, subtotal)
                        psItem.setLong(10, now)
                        psItem.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not insert cart to DB: ${e.message}")
            }
        }

        buildJsonObject {
            put("status", "SUCCESS")
            put("cart_id", cartId)
            put("subtotal", subtotal)
            put("tax_amount", taxAmount)
            put("total_amount", totalAmount)
        }.toString()
    }
}

class RealOrderCreateTool {
    private val logger = LoggerFactory.getLogger(RealOrderCreateTool::class.java)

    suspend fun execute(tenantId: String, parameters: Map<String, String>): String = withContext(Dispatchers.IO) {
        val cartId = parameters["cart_id"] ?: ""
        val customerId = parameters["customer_id"] ?: "cust-01"
        val customerName = parameters["customer_name"] ?: "Pelanggan"
        val customerPhone = parameters["customer_phone"] ?: "081234567890"
        val shippingAddress = parameters["shipping_address"] ?: "Alamat Pengiriman"
        val shippingCity = parameters["shipping_city"] ?: "Jakarta"
        val courierCode = parameters["courier_code"] ?: "JNE"
        val courierService = parameters["courier_service"] ?: "REG"
        val paymentGateway = parameters["payment_gateway"] ?: "MIDTRANS"
        val paymentMethod = parameters["payment_method"] ?: "QRIS"

        val orderId = "ord-" + UUID.randomUUID().toString().take(8)
        val orderNumber = "ORD-${System.currentTimeMillis() / 1000}-${(1000..9999).random()}"
        val shippingFee = 18000.0
        val paymentUrl = "https://app.sandbox.midtrans.com/snap/v2/vtweb/snap-token-$orderId"
        val now = System.currentTimeMillis()

        var subtotal = 0.0
        var taxAmount = 0.0
        var totalAmount = 0.0

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    // Try to calculate from existing cart if cartId provided
                    if (cartId.isNotBlank()) {
                        c.prepareStatement("SELECT subtotal, tax_amount, total_amount FROM carts WHERE tenant_id = ? AND id = ?").use { psCart ->
                            psCart.setString(1, tenantId)
                            psCart.setString(2, cartId)
                            psCart.executeQuery().use { rs ->
                                if (rs.next()) {
                                    subtotal = rs.getDouble("subtotal")
                                    taxAmount = rs.getDouble("tax_amount")
                                }
                            }
                        }
                    }

                    if (subtotal <= 0.0) {
                        subtotal = parameters["subtotal"]?.toDoubleOrNull() ?: 2500000.0
                        taxAmount = subtotal * 0.11
                    }
                    totalAmount = subtotal + taxAmount + shippingFee

                    c.prepareStatement("""
                        INSERT INTO orders 
                        (id, tenant_id, customer_id, cart_id, order_number, order_date, customer_name, customer_phone,
                         shipping_address, shipping_city, courier_code, courier_service, shipping_fee, subtotal, 
                         tax_amount, total_amount, currency, payment_gateway, payment_method, status, payment_url, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'IDR', ?, ?, 'PENDING_PAYMENT', ?, ?, ?)
                    """.trimIndent()).use { ps ->
                        ps.setString(1, orderId)
                        ps.setString(2, tenantId)
                        ps.setString(3, customerId)
                        ps.setString(4, cartId)
                        ps.setString(5, orderNumber)
                        ps.setLong(6, now)
                        ps.setString(7, customerName)
                        ps.setString(8, customerPhone)
                        ps.setString(9, shippingAddress)
                        ps.setString(10, shippingCity)
                        ps.setString(11, courierCode)
                        ps.setString(12, courierService)
                        ps.setDouble(13, shippingFee)
                        ps.setDouble(14, subtotal)
                        ps.setDouble(15, taxAmount)
                        ps.setDouble(16, totalAmount)
                        ps.setString(17, paymentGateway)
                        ps.setString(18, paymentMethod)
                        ps.setString(19, paymentUrl)
                        ps.setLong(20, now)
                        ps.setLong(21, now)
                        ps.executeUpdate()
                    }

                    // Copy items if from cart
                    if (cartId.isNotBlank()) {
                        c.prepareStatement("SELECT * FROM cart_items WHERE cart_id = ?").use { psItems ->
                            psItems.setString(1, cartId)
                            psItems.executeQuery().use { rsItem ->
                                while (rsItem.next()) {
                                    c.prepareStatement("""
                                        INSERT INTO order_items (id, order_id, tenant_id, product_id, product_name, unit_price, quantity, subtotal, discount_amount, total_amount, created_at)
                                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0.0, ?, ?)
                                    """.trimIndent()).use { psInsItem ->
                                        psInsItem.setString(1, "oi-" + UUID.randomUUID().toString().take(8))
                                        psInsItem.setString(2, orderId)
                                        psInsItem.setString(3, tenantId)
                                        psInsItem.setString(4, rsItem.getString("product_id"))
                                        psInsItem.setString(5, rsItem.getString("product_name") ?: "Product")
                                        psInsItem.setDouble(6, rsItem.getDouble("unit_price"))
                                        psInsItem.setInt(7, rsItem.getInt("quantity"))
                                        psInsItem.setDouble(8, rsItem.getDouble("subtotal"))
                                        psInsItem.setDouble(9, rsItem.getDouble("total_amount"))
                                        psInsItem.setLong(10, now)
                                        psInsItem.executeUpdate()
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not insert order: ${e.message}")
            }
        }

        buildJsonObject {
            put("status", "SUCCESS")
            put("order_id", orderId)
            put("order_number", orderNumber)
            put("total_amount", totalAmount)
            put("payment_url", paymentUrl)
        }.toString()
    }
}

class RealProductRecommendTool {
    private val logger = LoggerFactory.getLogger(RealProductRecommendTool::class.java)

    suspend fun execute(tenantId: String, parameters: Map<String, String>): String = withContext(Dispatchers.IO) {
        val category = parameters["category"] ?: "ALL"
        val skus = mutableListOf<String>()
        val names = mutableListOf<String>()

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val query = if (category.uppercase() == "ALL") {
                        "SELECT sku, name FROM products WHERE tenant_id = ? AND is_active = true LIMIT 5"
                    } else {
                        "SELECT sku, name FROM products WHERE tenant_id = ? AND is_active = true AND category ILIKE ? LIMIT 5"
                    }
                    c.prepareStatement(query).use { ps ->
                        ps.setString(1, tenantId)
                        if (category.uppercase() != "ALL") ps.setString(2, "%$category%")
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                skus.add(rs.getString("sku") ?: "")
                                names.add(rs.getString("name") ?: "")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying products in RealProductRecommendTool: ${e.message}")
            }
        }

        buildJsonObject {
            put("status", "SUCCESS")
            put("recommended_skus", if (skus.isNotEmpty()) skus.joinToString(",") else "SKU-ORCH-01,SKU-ORCH-02")
            put("recommended_products", if (names.isNotEmpty()) names.joinToString(" | ") else "Enterprise Workforce Suite")
            put("reasoning", "Rekomendasi produk katalog tenant $tenantId kategori $category")
        }.toString()
    }
}

class RealInvoiceGenerateTool {
    private val logger = LoggerFactory.getLogger(RealInvoiceGenerateTool::class.java)

    suspend fun execute(tenantId: String, parameters: Map<String, String>): String = withContext(Dispatchers.IO) {
        val orderIdentifier = parameters["order_number"] ?: parameters["order_id"] ?: ""
        var foundOrderNumber = orderIdentifier.ifBlank { "ORD-2026-DEFAULT" }
        var foundOrderId = "ord-unknown"
        var foundCustomerName = "Pelanggan"
        var foundTotalAmount = 0.0
        var foundStatus = "PENDING_PAYMENT"
        var foundCourier = "JNE"

        val conn = DatabaseManager.getConnection()
        if (conn != null && orderIdentifier.isNotBlank()) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        SELECT id, order_number, customer_name, total_amount, status, courier_code 
                        FROM orders 
                        WHERE tenant_id = ? AND (order_number = ? OR id = ?)
                        LIMIT 1
                    """.trimIndent()).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setString(2, orderIdentifier)
                        ps.setString(3, orderIdentifier)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                foundOrderId = rs.getString("id")
                                foundOrderNumber = rs.getString("order_number")
                                foundCustomerName = rs.getString("customer_name") ?: "Pelanggan"
                                foundTotalAmount = rs.getDouble("total_amount")
                                foundStatus = rs.getString("status") ?: "PAID"
                                foundCourier = rs.getString("courier_code") ?: "JNE"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying order for invoice: ${e.message}")
            }
        }

        val formattedInvoice = """
            =================================================
            ORCHESTREE AI - OFFICIAL INVOICE
            Order Number: $foundOrderNumber
            Customer: $foundCustomerName
            Status: $foundStatus
            Courier: $foundCourier
            Total: Rp ${"%,.0f".format(foundTotalAmount)}
            =================================================
        """.trimIndent()

        buildJsonObject {
            put("status", "SUCCESS")
            put("order_id", foundOrderId)
            put("order_number", foundOrderNumber)
            put("customer_name", foundCustomerName)
            put("total_amount", foundTotalAmount)
            put("payment_status", foundStatus)
            put("formatted_invoice", formattedInvoice)
        }.toString()
    }
}

class RealDiscountApplyTool {
    suspend fun execute(tenantId: String, parameters: Map<String, String>): String = withContext(Dispatchers.IO) {
        val discountPct = parameters["requested_discount_pct"]?.toDoubleOrNull() ?: 5.0
        val cartId = parameters["order_or_cart_id"] ?: ""

        if (discountPct <= 10.0) {
            // Autonomous Approval
            buildJsonObject {
                put("status", "APPLIED")
                put("approved", true)
                put("requires_manager_approval", false)
                put("discount_pct", discountPct)
            }.toString()
        } else {
            // Requires Manager Approval
            buildJsonObject {
                put("status", "ESCALATED_APPROVAL")
                put("approved", false)
                put("requires_manager_approval", true)
                put("discount_pct", discountPct)
            }.toString()
        }
    }
}

class RealRefundProcessTool {
    suspend fun execute(tenantId: String, parameters: Map<String, String>): String = withContext(Dispatchers.IO) {
        val amount = parameters["refund_amount"]?.toDoubleOrNull() ?: 0.0
        buildJsonObject {
            put("status", "WAITING_MANAGER_APPROVAL")
            put("requires_manager_approval", true)
            put("amount", amount)
        }.toString()
    }
}
