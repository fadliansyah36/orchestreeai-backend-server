package ai.orchestree.backend.webhooks

import org.slf4j.LoggerFactory

class WhatsAppWebhookHandler(
    private val validator: WebhookSignatureValidator = WebhookSignatureValidator()
) {
    private val logger = LoggerFactory.getLogger(WhatsAppWebhookHandler::class.java)

    fun handlePayload(payload: String, signatureHeader: String?, appSecret: String): Boolean {
        val isValid = validator.verifyMetaHmacSha256(
            payload = payload,
            appSecret = appSecret,
            hubSignatureHeader = signatureHeader
        )

        if (!isValid) {
            logger.warn("WhatsApp/Meta webhook rejected: invalid X-Hub-Signature-256")
            return false
        }

        logger.info("WhatsApp/Meta webhook signature verified successfully")
        return true
    }
}
