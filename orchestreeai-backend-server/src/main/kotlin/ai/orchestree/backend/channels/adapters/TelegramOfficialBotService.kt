package ai.orchestree.backend.channels.adapters

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Serializable
data class TelegramSendResult(
    val ok: Boolean,
    val messageId: Long? = null,
    val description: String? = null
)

class TelegramOfficialBotService(
    private val botToken: String = System.getenv("TELEGRAM_BOT_TOKEN") ?: ""
) {
    private val logger = LoggerFactory.getLogger(TelegramOfficialBotService::class.java)
    private val client = HttpClient.newHttpClient()

    suspend fun sendMessage(chatId: String, text: String): TelegramSendResult = withContext(Dispatchers.IO) {
        if (botToken.isBlank()) {
            logger.warn("Telegram bot token is not configured. Delivery bypassed for chatId: $chatId")
            return@withContext TelegramSendResult(ok = false, messageId = null)
        }

        try {
            val url = "https://api.telegram.org/bot$botToken/sendMessage"
            val payload = """{"chat_id":"$chatId","text":"${text.replace("\"", "\\\"")}","parse_mode":"Markdown"}"""
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val isOk = response.statusCode() == 200 && response.body().contains("\"ok\":true")
            TelegramSendResult(ok = isOk, description = response.body())
        } catch (e: Exception) {
            logger.error("Failed to send Telegram message: ${e.message}", e)
            TelegramSendResult(ok = false, description = e.message)
        }
    }
}
