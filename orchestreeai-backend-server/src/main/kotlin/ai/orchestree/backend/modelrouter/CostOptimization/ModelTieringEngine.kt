package ai.orchestree.backend.modelrouter.CostOptimization

import ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class ConcurrentRequestCoalescer<T> {
    private val inFlight = ConcurrentHashMap<String, Deferred<T>>()
    private val mutex = Mutex()

    suspend fun executeOrCoalesce(key: String, block: suspend () -> T): T = coroutineScope {
        val existing = inFlight[key]
        if (existing != null) {
            return@coroutineScope existing.await()
        }

        val deferred = mutex.withLock {
            inFlight.getOrPut(key) {
                async {
                    try {
                        block()
                    } finally {
                        inFlight.remove(key)
                    }
                }
            }
        }
        deferred.await()
    }
}

class ModelTieringEngine(
    private val providerRepo: ProviderRegistryRepository = ProviderRegistryRepository.instance
) {
    fun selectOptimalModel(
        taskCategory: String,
        sensitivityTier: String,
        promptLength: Int
    ): Pair<String, String> { // providerId to defaultModel
        if (taskCategory == "IMAGE_GEN") {
            val imgProvider = providerRepo.getActiveImageProvider()
            return imgProvider.providerCode.lowercase() to imgProvider.defaultModel
        }

        val providers = providerRepo.getLlmProvidersOrderedByFallbackPriority()
        val cat = taskCategory.uppercase()

        // 1. Check specialization in active database-registered providers
        val specialized = providers.firstOrNull { p ->
            val spec = p.taskSpecialization.uppercase()
            when {
                cat.contains("FAST") || cat.contains("CLASSIF") || promptLength < 200 ->
                    spec.contains("FAST") || spec.contains("LOW_LATENCY") || spec.contains("CLASSIFICATION")
                cat.contains("REASONING") || cat.contains("CODE") || sensitivityTier == "RESTRICTED" ->
                    spec.contains("REASONING") || spec.contains("CODE") || spec.contains("COMPLEX")
                cat.contains("CREATIVE") || cat.contains("MARKETING") || cat.contains("DESIGN") ->
                    spec.contains("CREATIVE") || spec.contains("COMPOSITION") || spec.contains("REASONING")
                else -> false
            }
        }

        if (specialized != null) {
            val defaultModel = when (specialized.providerCode.uppercase()) {
                "GROQ" -> "llama-3.3-70b-versatile"
                "DEEPSEEK" -> "deepseek-chat"
                "OPENROUTER" -> "anthropic/claude-3.5-sonnet"
                "ANTHROPIC" -> "claude-3-5-sonnet-20241022"
                else -> "default-model"
            }
            return specialized.providerCode.lowercase() to defaultModel
        }

        // 2. Fallback to top priority provider from database
        val topProvider = providers.firstOrNull()
        val topCode = topProvider?.providerCode?.lowercase() ?: "openrouter"
        val topModel = when (topCode) {
            "openrouter" -> "anthropic/claude-3.5-sonnet"
            "groq" -> "llama-3.3-70b-versatile"
            "deepseek" -> "deepseek-chat"
            "anthropic" -> "claude-3-5-sonnet-20241022"
            else -> "default-model"
        }
        return topCode to topModel
    }
}
