package ai.orchestree.backend.modelrouter.providers

import ai.orchestree.backend.modelrouter.LlmClient
import ai.orchestree.backend.modelrouter.LlmRequest
import ai.orchestree.backend.modelrouter.LlmResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

class AnthropicLlmClient(
    private val apiKeyProvider: () -> String,
    private val defaultModel: String = "claude-3-5-sonnet-20241022",
    private val httpClient: HttpClient = HttpClient(CIO)
) : LlmClient {

    override val providerId: String = "anthropic"
    override val providerName: String = "Anthropic Claude"

    private val logger = LoggerFactory.getLogger(AnthropicLlmClient::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(request: LlmRequest): Result<LlmResponse> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val apiKey = apiKeyProvider().trim()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Anthropic API key is missing"))
            }

            val targetModel = request.model ?: defaultModel
            val endpoint = "https://api.anthropic.com/v1/messages"

            val messages = buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", request.prompt)
                })
            }

            val requestBody = buildJsonObject {
                put("model", targetModel)
                put("max_tokens", request.maxTokens)
                put("temperature", request.temperature)
                request.systemInstruction?.let { put("system", it) }
                put("messages", messages)
            }

            val httpResponse = httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                setBody(requestBody.toString())
            }

            val rawBody = httpResponse.bodyAsText()
            val durationMs = System.currentTimeMillis() - startTime

            if (!httpResponse.status.isSuccess()) {
                logger.error("Anthropic API Error: HTTP ${httpResponse.status.value} - $rawBody")
                return@withContext Result.failure(RuntimeException("Anthropic error (${httpResponse.status.value}): $rawBody"))
            }

            val responseJson = json.parseToJsonElement(rawBody).jsonObject
            val contentArr = responseJson["content"]?.jsonArray
            val textContent = contentArr?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

            val usage = responseJson["usage"]?.jsonObject
            val promptTokens = usage?.get("input_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val completionTokens = usage?.get("output_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val totalTokens = promptTokens + completionTokens

            Result.success(
                LlmResponse(
                    text = textContent,
                    totalTokens = totalTokens,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    modelUsed = targetModel,
                    providerUsed = providerId,
                    durationMs = durationMs,
                    costUsd = (promptTokens * 0.000003) + (completionTokens * 0.000015)
                )
            )
        } catch (e: Exception) {
            logger.error("Anthropic execution exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
