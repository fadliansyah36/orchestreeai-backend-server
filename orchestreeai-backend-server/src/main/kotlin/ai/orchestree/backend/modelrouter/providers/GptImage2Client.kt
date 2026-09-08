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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

class GptImage2Client(
    private val apiKeyProvider: () -> String,
    private val apiUrlProvider: () -> String = { "https://api.apimart.ai/v1/images/generations" },
    private val httpClient: HttpClient = HttpClient(CIO)
) : LlmClient {

    override val providerId: String = "gpt_image_2"
    override val providerName: String = "GPT-Image-2 (Apimart AI)"

    private val logger = LoggerFactory.getLogger(GptImage2Client::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(request: LlmRequest): Result<LlmResponse> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val apiKey = apiKeyProvider().trim()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Apimart API key is missing for GPT-Image-2"))
            }

            val endpoint = apiUrlProvider()
            val requestBody = buildJsonObject {
                put("prompt", request.prompt)
                put("model", request.model ?: "gpt-image-2")
                put("n", 1)
                put("size", "1024x1024")
                put("response_format", "url")
            }

            val httpResponse = httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(requestBody.toString())
            }

            val rawBody = httpResponse.bodyAsText()
            val durationMs = System.currentTimeMillis() - startTime

            if (!httpResponse.status.isSuccess()) {
                logger.error("GPT-Image-2 API error: HTTP ${httpResponse.status.value} - $rawBody")
                return@withContext Result.failure(RuntimeException("GPT-Image-2 error (${httpResponse.status.value}): $rawBody"))
            }

            val responseJson = json.parseToJsonElement(rawBody).jsonObject
            val dataArr = responseJson["data"]?.jsonArray
            val imageUrl = dataArr?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""

            Result.success(
                LlmResponse(
                    text = imageUrl,
                    totalTokens = 100,
                    modelUsed = "gpt-image-2",
                    providerUsed = providerId,
                    durationMs = durationMs,
                    costUsd = 0.04
                )
            )
        } catch (e: Exception) {
            logger.error("GPT-Image-2 generation exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
