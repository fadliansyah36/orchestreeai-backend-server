package ai.orchestree.backend.modelrouter

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.database.repositories.modelrouter.LlmProviderModelRepository
import ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository
import ai.orchestree.backend.modelrouter.CostOptimization.ConcurrentRequestCoalescer
import ai.orchestree.backend.modelrouter.CostOptimization.ModelTieringEngine
import ai.orchestree.backend.modelrouter.CostOptimization.PromptCacheManager
import ai.orchestree.backend.modelrouter.providers.GeminiLlmClient
import ai.orchestree.backend.modelrouter.providers.GptImage2Client
import ai.orchestree.backend.modelrouter.providers.OpenAiCompatibleLlmClient
import ai.orchestree.backend.resilience.executeWithRetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class AllProvidersInChainFailedException(
    val taskCategory: String,
    val candidateProviders: List<String>,
    val providerErrors: Map<String, String> = emptyMap(),
    cause: Throwable? = null
) : RuntimeException(
    if (providerErrors.isNotEmpty())
        "All LLM providers in chain failed for task '$taskCategory'. Candidates: $candidateProviders. Errors: $providerErrors"
    else
        "All LLM providers in chain failed for task '$taskCategory'. Candidates: $candidateProviders",
    cause
)

class ImageGenerationFailedException(
    message: String = "All image generation providers in chain failed (Tier 1: GPT-Image-2, Tier 2: OpenAI DALL-E 3, Tier 3: Stability AI SDXL)",
    cause: Throwable? = null
) : RuntimeException(message, cause)

class ModelRouter(
    private val config: AppConfig = AppConfig.load(),
    private val providerRepo: ProviderRegistryRepository = ProviderRegistryRepository.instance,
    private val modelRepo: LlmProviderModelRepository = LlmProviderModelRepository.instance,
    customProviders: Map<String, LlmClient>? = null
) {
    private val logger = LoggerFactory.getLogger(ModelRouter::class.java)

    private val promptCache = PromptCacheManager()
    private val coalescer = ConcurrentRequestCoalescer<Result<LlmResponse>>()
    private val tieringEngine = ModelTieringEngine(providerRepo, modelRepo)

    private val failureCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val circuitBreakers = ConcurrentHashMap<String, CircuitBreakerState>()
    private val outputValidator = ai.orchestree.backend.intelligence.OutputValidator()
    val auditLogger = ai.orchestree.backend.security.AuditLogger()

    val nvidiaNimClient = OpenAiCompatibleLlmClient(
        providerId = "nvidia_nim",
        providerName = "NVIDIA NIM",
        baseUrl = ai.orchestree.backend.config.EnvLoader.get("NVIDIA_BASE_URL", "https://integrate.api.nvidia.com/v1"),
        apiKeyProvider = {
            ai.orchestree.backend.config.EnvLoader.get("NVIDIA_API_KEY").ifBlank {
                System.getenv("NVIDIA_API_KEY") ?: ""
            }
        },
        defaultModel = ai.orchestree.backend.config.EnvLoader.get("NVIDIA_MODEL_NAME", "meta/llama-3.1-70b-instruct")
    )

    val groqClient = OpenAiCompatibleLlmClient(
        providerId = "groq",
        providerName = "Groq Cloud",
        baseUrl = ai.orchestree.backend.config.EnvLoader.get("GROQ_BASE_URL", "https://api.groq.com/openai/v1"),
        apiKeyProvider = { ai.orchestree.backend.config.EnvLoader.get("GROQ_API_KEY") },
        defaultModel = ai.orchestree.backend.config.EnvLoader.get("GROQ_MODEL_NAME", "llama-3.3-70b-versatile")
    )

    val openRouterClient = OpenAiCompatibleLlmClient(
        providerId = "openrouter",
        providerName = "OpenRouter",
        baseUrl = ai.orchestree.backend.config.EnvLoader.get("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1"),
        apiKeyProvider = { ai.orchestree.backend.config.EnvLoader.get("OPENROUTER_API_KEY") },
        defaultModel = ai.orchestree.backend.config.EnvLoader.get("OPENROUTER_MODEL_NAME", "meta-llama/llama-3.1-70b-instruct")
    )

    val geminiClient = GeminiLlmClient(
        apiKeyProvider = { ai.orchestree.backend.config.EnvLoader.get("GEMINI_API_KEY") },
        defaultModel = "gemini-3.5-flash-lite"
    )

    val gptImage2Client = GptImage2Client(
        apiKeyProvider = { 
            ai.orchestree.backend.config.EnvLoader.get("GPT_IMAGE_API_KEY").ifBlank {
                ai.orchestree.backend.config.EnvLoader.get("GPT_IMAGE_2_API_KEY").ifBlank {
                    ai.orchestree.backend.config.EnvLoader.get("APIMART_API_KEY")
                }
            }
        },
        apiUrlProvider = { 
            ai.orchestree.backend.config.EnvLoader.get("GPT_IMAGE_API_URL").ifBlank {
                ai.orchestree.backend.config.EnvLoader.get("GPT_IMAGE_2_API_URL", "https://api.apimart.ai/v1/images/generations")
            }
        }
    )

    private val providers: Map<String, LlmClient> = customProviders ?: buildMap {
        put("nvidia_nim", nvidiaNimClient)
        put("openrouter", openRouterClient)
        put("groq", groqClient)
        put("gpt_image_2", gptImage2Client)
        put("gemini", geminiClient)
        put("google_gemini", geminiClient)
    }

    suspend fun route(request: ModelRouteRequest): Result<LlmResponse> = execute(request)

    suspend fun route(request: LlmRequest): LlmResponse {
        val routeReq = ModelRouteRequest(
            prompt = request.prompt,
            systemInstruction = request.systemInstruction,
            temperature = request.temperature,
            model = request.model,
            responseFormatJson = request.responseFormatJson
        )
        val res = execute(routeReq)
        if (res.isSuccess) {
            return res.getOrThrow()
        }
        throw res.exceptionOrNull() ?: AllProvidersInChainFailedException("GENERAL", listOf("nvidia_nim", "openrouter"))
    }

    suspend fun execute(request: ModelRouteRequest): Result<LlmResponse> = withContext(Dispatchers.IO) {
        // 1. Check Prompt Cache
        val cached = promptCache.get(request.prompt, request.systemInstruction)
        if (cached != null) {
            logger.info("Prompt cache hit for task category: ${request.taskCategory}")
            return@withContext Result.success(
                LlmResponse(
                    text = cached,
                    totalTokens = 0,
                    modelUsed = "cache-hit",
                    providerUsed = "local-cache",
                    durationMs = 1
                )
            )
        }

        // 2. Coalesce concurrent requests with identical prompts
        val coalesceKey = "${request.taskCategory}:${request.prompt}"
        coalescer.executeOrCoalesce(coalesceKey) {
            executeWithRoutingAndFallback(request)
        }
    }

    private suspend fun executeWithRoutingAndFallback(request: ModelRouteRequest): Result<LlmResponse> {
        val complexityTier = tieringEngine.mapTaskToComplexityTier(request.taskCategory, request.prompt.length)

        // Providers ordered strictly by database fallback_priority (NVIDIA_NIM Tier 1, OPENROUTER Tier 2, etc.)
        val activeProviders = providerRepo.getLlmProvidersOrderedByFallbackPriority()
            .filter { it.isEnabled && it.healthStatus != "disabled" }
            .map { it.providerCode.lowercase() }
            .filter { providers.containsKey(it) }

        val attemptedProviders = mutableListOf<String>()
        val providerErrors = mutableMapOf<String, String>()

        for (providerId in activeProviders) {
            if (getCircuitBreakerState(providerId) == CircuitBreakerState.OPEN) {
                logger.warn("Circuit breaker for $providerId is OPEN. Skipping.")
                continue
            }

            attemptedProviders.add(providerId)

            // Dynamic model resolution from catalog
            val targetModel = if (!request.model.isNullOrBlank()) {
                request.model
            } else {
                val candidateModels = modelRepo.getActiveModelsForTier(providerId, complexityTier)
                val resolvedFromRepo = candidateModels.firstOrNull()?.modelIdentifier
                if (!resolvedFromRepo.isNullOrBlank()) {
                    resolvedFromRepo
                } else {
                    OpenAiCompatibleLlmClient.resolveFallbackModel(providerId)
                }
            }

            val sanitizedPrompt = outputValidator.sanitizePromptBeforeLlmCall(
                userPrompt = request.prompt,
                systemContext = request.systemInstruction ?: "",
                auditLog = auditLogger
            )

            val llmReq = LlmRequest(
                prompt = sanitizedPrompt,
                systemInstruction = request.systemInstruction,
                temperature = request.temperature,
                model = targetModel.ifBlank { null },
                responseFormatJson = request.responseFormatJson
            )

            val client = providers[providerId] ?: continue
            val res = try {
                executeWithRetry(
                    maxAttempts = 2,
                    initialDelayMs = 500L,
                    backoffMultiplier = 2.0,
                    retryableExceptions = setOf(Exception::class)
                ) {
                    val attemptRes = client.complete(llmReq)
                    if (attemptRes.isFailure) {
                        throw attemptRes.exceptionOrNull() ?: RuntimeException("Provider $providerId completion failed")
                    }
                    attemptRes
                }
            } catch (e: Exception) {
                providerErrors[providerId] = e.message ?: e.toString()
                Result.failure(e)
            }

            if (res.isSuccess) {
                recordSuccess(providerId)
                val rawText = res.getOrThrow().text
                val filteredText = outputValidator.filterLlmOutput(
                    llmResponse = rawText,
                    internalInstructionsToProtect = listOfNotNull(request.systemInstruction)
                )
                promptCache.put(request.prompt, request.systemInstruction, filteredText)
                val usedModel = res.getOrThrow().modelUsed.ifBlank { targetModel }
                val resp = res.getOrThrow().copy(
                    text = filteredText,
                    providerUsed = providerId,
                    modelUsed = usedModel
                )
                recordUsage(request.tenantId, providerId, resp.modelUsed, request.prompt, resp.text)
                return Result.success(resp)
            } else {
                recordFailure(providerId)
                logger.warn("Provider '$providerId' failed: ${res.exceptionOrNull()?.message}. Attempting next fallback in chain...")
            }
        }

        logger.error("Structured LLM Failure Log: All configured providers failed for task '${request.taskCategory}'. Attempted: $attemptedProviders. Errors: $providerErrors")
        return Result.failure(AllProvidersInChainFailedException(request.taskCategory, attemptedProviders, providerErrors))
    }

    private fun recordUsage(
        tenantId: String,
        provider: String,
        model: String,
        prompt: String,
        response: String
    ) {
        val inTok = (prompt.length / 4).coerceAtLeast(1)
        val outTok = (response.length / 4).coerceAtLeast(1)
        val record = ai.orchestree.backend.database.repositories.analytics.LlmUsageLogRecord(
            id = "llm-log-${System.currentTimeMillis()}-${java.util.UUID.randomUUID().toString().take(6)}",
            tenantId = tenantId,
            provider = provider.uppercase(),
            modelName = model,
            inputTokens = inTok,
            outputTokens = outTok,
            totalTokens = inTok + outTok,
            estimatedCostUsd = 0.001,
            timestamp = System.currentTimeMillis()
        )
        ai.orchestree.backend.database.repositories.analytics.AnalyticsRepository.defaultInstance.recordLlmUsage(record)
    }

    private fun getCircuitBreakerState(providerId: String): CircuitBreakerState {
        return circuitBreakers.getOrDefault(providerId, CircuitBreakerState.CLOSED)
    }

    private fun recordSuccess(providerId: String) {
        failureCounts.getOrPut(providerId) { AtomicInteger(0) }.set(0)
        circuitBreakers[providerId] = CircuitBreakerState.CLOSED
    }

    private fun recordFailure(providerId: String) {
        val count = failureCounts.getOrPut(providerId) { AtomicInteger(0) }.incrementAndGet()
        if (count >= 3) {
            logger.error("Tripping Circuit Breaker to OPEN for provider: $providerId (failures: $count)")
            circuitBreakers[providerId] = CircuitBreakerState.OPEN
        }
    }

    /**
     * Image Generation Fallback Chain:
     * Tier 1: GPT-Image-2 (Apimart)
     * Tier 2: OpenAI DALL-E 3
     * Tier 3: Stability AI SDXL
     * If all fail: throw ImageGenerationFailedException
     */
    suspend fun generateImage(prompt: String, tenantId: String?): Result<String> {
        val imageProviders = providerRepo.getImageProviders().filter { it.isEnabled }
        val sortedProviders = imageProviders.sortedBy { it.priority }

        val errors = mutableMapOf<String, String>()
        for (provider in sortedProviders) {
            try {
                when (provider.providerCode.uppercase()) {
                    "GPT_IMAGE_2" -> {
                        val apiKey = ai.orchestree.backend.config.EnvLoader.get("GPT_IMAGE_API_KEY").ifBlank {
                            ai.orchestree.backend.config.EnvLoader.get("GPT_IMAGE_2_API_KEY").ifBlank {
                                ai.orchestree.backend.config.EnvLoader.get("APIMART_API_KEY")
                            }
                        }
                        if (apiKey.isBlank()) {
                            errors["GPT_IMAGE_2"] = "Apimart API key not configured (GPT_IMAGE_API_KEY / GPT_IMAGE_2_API_KEY / APIMART_API_KEY missing)"
                        } else {
                            val res = gptImage2Client.complete(LlmRequest(prompt = prompt, model = provider.defaultModel))
                            if (res.isSuccess) {
                                return Result.success(res.getOrThrow().text)
                            } else {
                                errors["GPT_IMAGE_2"] = res.exceptionOrNull()?.message ?: "Failed"
                            }
                        }
                    }
                    "OPENAI_DALLE" -> {
                        val apiKey = ai.orchestree.backend.config.EnvLoader.get("OPENAI_API_KEY")
                        if (apiKey.isBlank()) {
                            errors["OPENAI_DALLE"] = "OpenAI API key not configured (OPENAI_API_KEY missing)"
                        } else {
                            val openAiImageClient = GptImage2Client(
                                apiKeyProvider = { apiKey },
                                apiUrlProvider = { "https://api.openai.com/v1/images/generations" }
                            )
                            val res = openAiImageClient.complete(LlmRequest(prompt = prompt, model = "dall-e-3"))
                            if (res.isSuccess) return Result.success(res.getOrThrow().text)
                            else errors["OPENAI_DALLE"] = res.exceptionOrNull()?.message ?: "Failed"
                        }
                    }
                    "STABILITY_AI" -> {
                        val apiKey = ai.orchestree.backend.config.EnvLoader.get("STABILITY_API_KEY")
                        if (apiKey.isBlank()) {
                            errors["STABILITY_AI"] = "Stability API key not configured (STABILITY_API_KEY missing)"
                        } else {
                            errors["STABILITY_AI"] = "Stability AI endpoint requires multipart/REST client, not configured"
                        }
                    }
                }
            } catch (e: Exception) {
                errors[provider.providerCode] = e.message ?: "Exception"
            }
        }

        val ex = ImageGenerationFailedException("All image generation providers in chain failed: $errors")
        return Result.failure(ex)
    }

    suspend fun generateImage(prompt: String): Result<String> = generateImage(prompt, null)
}
