package ai.orchestree.backend.modelrouter.providers

import ai.orchestree.backend.modelrouter.LlmClient
import ai.orchestree.backend.modelrouter.LlmRequest
import ai.orchestree.backend.modelrouter.LlmResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

class GeminiLlmClient(
    private val apiKeyProvider: () -> String,
    private val defaultModel: String = "gemini-3.5-flash-lite",
    private val httpClient: HttpClient = HttpClient(CIO)
) : LlmClient {

    override val providerId: String = "gemini"
    override val providerName: String = "Google Gemini"

    private val logger = LoggerFactory.getLogger(GeminiLlmClient::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(request: LlmRequest): Result<LlmResponse> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val apiKey = apiKeyProvider().trim()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is missing"))
            }

            val targetModel = request.model ?: defaultModel
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$apiKey"

            val partsArray = buildJsonArray {
                add(buildJsonObject {
                    put("text", request.prompt)
                })
            }

            val contentsArray = buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", partsArray)
                })
            }

            val requestBody = buildJsonObject {
                put("contents", contentsArray)
                request.systemInstruction?.let { sys ->
                    put("systemInstruction", buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", sys) })
                        })
                    })
                }
                put("generationConfig", buildJsonObject {
                    put("temperature", request.temperature)
                    put("maxOutputTokens", request.maxTokens)
                    if (request.responseFormatJson) {
                        put("responseMimeType", "application/json")
                    }
                })
            }

            val response = httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            val rawBody = response.bodyAsText()
            val durationMs = System.currentTimeMillis() - startTime

            if (!response.status.isSuccess()) {
                logger.error("Gemini API error (status ${response.status}): $rawBody")
                return@withContext Result.failure(IllegalStateException("Gemini API error (${response.status}): $rawBody"))
            }

            val rootJson = json.parseToJsonElement(rawBody).jsonObject
            val candidates = rootJson["candidates"]?.jsonArray
            val firstCandidate = candidates?.firstOrNull()?.jsonObject
            val parts = firstCandidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
            val text = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

            val usageMetadata = rootJson["usageMetadata"]?.jsonObject
            val promptTokens = usageMetadata?.get("promptTokenCount")?.jsonPrimitive?.intOrNull ?: 0
            val completionTokens = usageMetadata?.get("candidatesTokenCount")?.jsonPrimitive?.intOrNull ?: 0
            val totalTokens = usageMetadata?.get("totalTokenCount")?.jsonPrimitive?.intOrNull ?: (promptTokens + completionTokens)

            // Calculate estimated cost
            val costUsd = (promptTokens * 0.000000075) + (completionTokens * 0.0000003)

            Result.success(
                LlmResponse(
                    text = text,
                    totalTokens = totalTokens,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    modelUsed = targetModel,
                    providerUsed = providerId,
                    durationMs = durationMs,
                    costUsd = costUsd
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to execute Gemini completion", e)
            Result.failure(e)
        }
    }
}
