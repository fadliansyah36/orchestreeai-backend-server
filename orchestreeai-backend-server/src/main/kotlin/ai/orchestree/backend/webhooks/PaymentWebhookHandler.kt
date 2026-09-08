package ai.orchestree.backend.webhooks

import ai.orchestree.backend.resilience.executeWithRetry
import org.slf4j.LoggerFactory

data class PaymentNotification(
    val orderId: String,
    val statusCode: String,
    val grossAmount: String,
    val transactionStatus: String,
    val signatureKey: String?
)

class PaymentWebhookHandler(
    private val validator: WebhookSignatureValidator = WebhookSignatureValidator()
) {
    private val logger = LoggerFactory.getLogger(PaymentWebhookHandler::class.java)

    fun handlePaymentNotification(notif: PaymentNotification, serverKey: String): Boolean {
        val isValid = validator.verifyMidtransSha512(
            orderId = notif.orderId,
            statusCode = notif.statusCode,
            grossAmount = notif.grossAmount,
            serverKey = serverKey,
            signatureKey = notif.signatureKey
        )

        if (!isValid) {
            logger.warn("Invalid signature for payment notification orderId: ${notif.orderId}")
            return false
        }

        logger.info("Payment webhook verified for orderId: ${notif.orderId}, status: ${notif.transactionStatus}")
        when (notif.transactionStatus) {
            "capture", "settlement" -> {
                logger.info("Fulfilling order ${notif.orderId}: Subscription / Credits Activated")
            }
            "cancel", "expire", "deny" -> {
                logger.warn("Payment failed/expired for order ${notif.orderId}")
            }
        }
        return true
    }

    /**
     * Executes verified payment fulfillment with unified executeWithRetry resilience.
     */
    suspend fun processAndFulfillWithRetry(
        notif: PaymentNotification,
        serverKey: String,
        onSuccessFulfillment: suspend (orderId: String) -> Unit = {}
    ): Boolean {
        val verified = handlePaymentNotification(notif, serverKey)
        if (!verified) return false

        if (notif.transactionStatus == "capture" || notif.transactionStatus == "settlement") {
            executeWithRetry(
                maxAttempts = 3,
                initialDelayMs = 1000L,
                backoffMultiplier = 2.0
            ) {
                logger.info("Triggering resilient fulfillment callback for order ${notif.orderId}")
                onSuccessFulfillment(notif.orderId)
            }
        }
        return true
    }
}

