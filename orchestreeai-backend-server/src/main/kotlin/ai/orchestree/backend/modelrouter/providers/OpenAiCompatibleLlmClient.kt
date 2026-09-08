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

class OpenAiCompatibleLlmClient(
    override val providerId: String,
    override val providerName: String,
    private val baseUrl: String,
    private val apiKeyProvider: () -> String,
    private val defaultModel: String,
    private val httpClient: HttpClient = HttpClient(CIO)
) : LlmClient {

    private val logger = LoggerFactory.getLogger(OpenAiCompatibleLlmClient::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(request: LlmRequest): Result<LlmResponse> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val apiKey = apiKeyProvider().trim()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("API Key is missing for provider: $providerName"))
            }

            val targetModel = request.model ?: defaultModel
            val endpoint = if (baseUrl.endsWith("/chat/completions")) baseUrl else "${baseUrl.trimEnd('/')}/chat/completions"

            val messages = buildJsonArray {
                request.systemInstruction?.let { sys ->
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", sys)
                    })
                }
                add(buildJsonObject {
                    put("role", "user")
                    put("content", request.prompt)
                })
            }

            val requestBody = buildJsonObject {
                put("model", targetModel)
                put("messages", messages)
                put("temperature", request.temperature)
                put("max_tokens", request.maxTokens)
                if (request.responseFormatJson) {
                    put("response_format", buildJsonObject { put("type", "json_object") })
                }
            }

            val httpResponse = httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(requestBody.toString())
            }

            val rawBody = httpResponse.bodyAsText()
            val durationMs = System.currentTimeMillis() - startTime

            if (!httpResponse.status.isSuccess()) {
                logger.error("Error from $providerName ($endpoint): HTTP ${httpResponse.status.value} - $rawBody")
                return@withContext Result.failure(RuntimeException("$providerName error (${httpResponse.status.value}): $rawBody"))
            }

            val responseJson = json.parseToJsonElement(rawBody).jsonObject
            val choices = responseJson["choices"]?.jsonArray
            val content = choices?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: ""

            val usage = responseJson["usage"]?.jsonObject
            val promptTokens = usage?.get("prompt_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val completionTokens = usage?.get("completion_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val totalTokens = usage?.get("total_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: (promptTokens + completionTokens)

            Result.success(
                LlmResponse(
                    text = content,
                    totalTokens = totalTokens,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    modelUsed = targetModel,
                    providerUsed = providerId,
                    durationMs = durationMs,
                    costUsd = calculateEstimatedCost(providerId, targetModel, promptTokens, completionTokens)
                )
            )
        } catch (e: Exception) {
            logger.error("Exception in $providerName complete: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun calculateEstimatedCost(provider: String, model: String, promptTokens: Int, completionTokens: Int): Double {
        return when {
            provider == "deepseek" -> (promptTokens * 0.00000014) + (completionTokens * 0.00000028)
            provider == "groq" -> (promptTokens * 0.00000005) + (completionTokens * 0.00000008)
            provider == "openrouter" -> (promptTokens * 0.0000005) + (completionTokens * 0.0000015)
            else -> 0.0
        }
    }
}
