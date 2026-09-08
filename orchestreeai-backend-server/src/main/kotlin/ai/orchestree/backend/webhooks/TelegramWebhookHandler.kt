package ai.orchestree.backend.webhooks

import org.slf4j.LoggerFactory

class TelegramWebhookHandler(
    private val validator: WebhookSignatureValidator = WebhookSignatureValidator()
) {
    private val logger = LoggerFactory.getLogger(TelegramWebhookHandler::class.java)

    fun handleWebhook(secretTokenHeader: String?, expectedSecretToken: String): Boolean {
        val isValid = validator.verifyTelegramSecretToken(secretTokenHeader, expectedSecretToken)
        if (!isValid) {
            logger.warn("Telegram webhook rejected: invalid X-Telegram-Bot-Api-Secret-Token")
            return false
        }

        logger.info("Telegram webhook secret token verified successfully")
        return true
    }
}
