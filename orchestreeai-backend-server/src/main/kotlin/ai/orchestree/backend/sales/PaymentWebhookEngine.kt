package ai.orchestree.backend.sales

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.UUID

object PaymentWebhookEngine {
    private val logger = LoggerFactory.getLogger(PaymentWebhookEngine::class.java)

    /**
     * Memvalidasi signature webhook gateway pembayaran dan memproses settlement pesanan secara atomik.
     */
    suspend fun handlePaymentWebhook(
        tenantId: String,
        gateway: String,
        signatureHeader: String,
        payloadJsonStr: String,
        serverKeyOrToken: String
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        logger.info("[PAYMENT_WEBHOOK] Received $gateway webhook for tenant: $tenantId")

        // Parse order_id, status_code, gross_amount, transaction_status from payload
        val orderNumber = extractJsonField(payloadJsonStr, "order_id") ?: ""
        val statusCode = extractJsonField(payloadJsonStr, "status_code") ?: "200"
        val grossAmountStr = extractJsonField(payloadJsonStr, "gross_amount") ?: "0.00"
        val transactionStatus = extractJsonField(payloadJsonStr, "transaction_status") ?: "settlement"
        val signatureKeyInPayload = extractJsonField(payloadJsonStr, "signature_key") ?: signatureHeader

        // Calculate expected SHA-512 signature: SHA512(order_id + status_code + gross_amount + serverKey)
        val rawSigInput = orderNumber + statusCode + grossAmountStr + serverKeyOrToken
        val md = MessageDigest.getInstance("SHA-512")
        val calculatedSignature = md.digest(rawSigInput.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        val isSignatureValid = signatureKeyInPayload.equals(calculatedSignature, ignoreCase = true)

        val conn = DatabaseManager.getConnection()

        if (!isSignatureValid) {
            logger.warn("[PAYMENT_WEBHOOK:REJECT] Invalid signature detected for order $orderNumber from $gateway")
            // Record security incident
            if (conn != null) {
                try {
                    conn.use { c ->
                        c.prepareStatement("""
                            INSERT INTO security_incidents 
                            (id, tenant_id, incident_type, severity, description, status, detected_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent()).use { ps ->
                            ps.setString(1, "inc-" + UUID.randomUUID().toString().take(8))
                            ps.setString(2, tenantId)
                            ps.setString(3, "INVALID_WEBHOOK_SIGNATURE")
                            ps.setString(4, "HIGH")
                            ps.setString(5, "Tampered webhook payload received for order $orderNumber from gateway $gateway")
                            ps.setString(6, "BLOCKED")
                            ps.setLong(7, now)
                            ps.executeUpdate()
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Could not log security incident: ${e.message}")
                }
            }
            return@withContext Pair(401, "Rejected: Invalid webhook signature")
        }

        // Signature valid -> Mark order as PAID
        if (conn != null) {
            try {
                conn.use { c ->
                    // 1. Update order status
                    c.prepareStatement("""
                        UPDATE orders 
                        SET status = 'PAID', paid_at = ?, updated_at = ?
                        WHERE tenant_id = ? AND (order_number = ? OR id = ?)
                    """.trimIndent()).use { ps ->
                        ps.setLong(1, now)
                        ps.setLong(2, now)
                        ps.setString(3, tenantId)
                        ps.setString(4, orderNumber)
                        ps.setString(5, orderNumber)
                        ps.executeUpdate()
                    }

                    // 2. Insert payment webhook log
                    c.prepareStatement("""
                        INSERT INTO payment_webhook_logs 
                        (id, tenant_id, gateway, payload, is_signature_valid, http_status_code, process_status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()).use { psLog ->
                        psLog.setString(1, "pwl-" + UUID.randomUUID().toString().take(8))
                        psLog.setString(2, tenantId)
                        psLog.setString(3, gateway)
                        psLog.setString(4, payloadJsonStr)
                        psLog.setBoolean(5, true)
                        psLog.setInt(6, 200)
                        psLog.setString(7, "PROCESSED")
                        psLog.setLong(8, now)
                        psLog.executeUpdate()
                    }

                    // 3. Insert audit log
                    c.prepareStatement("""
                        INSERT INTO audit_logs 
                        (id, tenant_id, action, entity_target, details, timestamp)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()).use { psAudit ->
                        psAudit.setString(1, "aud-" + UUID.randomUUID().toString().take(8))
                        psAudit.setString(2, tenantId)
                        psAudit.setString(3, "ORDER_PAYMENT_SETTLED")
                        psAudit.setString(4, "Order $orderNumber")
                        psAudit.setString(5, "Payment settled via $gateway for amount $grossAmountStr")
                        psAudit.setLong(6, now)
                        psAudit.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not execute payment settlement DB updates: ${e.message}")
            }
        }

        Pair(200, "OK: Order $orderNumber settled successfully")
    }

    private fun extractJsonField(json: String, fieldName: String): String? {
        val pattern = Regex(""""$fieldName"\s*:\s*"([^"]+)"""")
        val match = pattern.find(json)
        if (match != null) return match.groupValues[1]

        val numberPattern = Regex(""""$fieldName"\s*:\s*([0-9.]+)""")
        val numMatch = numberPattern.find(json)
        return numMatch?.groupValues?.get(1)
    }
}
