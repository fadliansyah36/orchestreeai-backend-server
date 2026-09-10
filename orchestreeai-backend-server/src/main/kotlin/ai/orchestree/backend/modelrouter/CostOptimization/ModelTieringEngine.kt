package ai.orchestree.backend.modelrouter.CostOptimization

import ai.orchestree.backend.database.repositories.modelrouter.LlmProviderModelRepository
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
    private val providerRepo: ProviderRegistryRepository = ProviderRegistryRepository.instance,
    private val modelRepo: LlmProviderModelRepository = LlmProviderModelRepository.instance
) {
    fun mapTaskToComplexityTier(taskCategory: String, promptLength: Int): String {
        val cat = taskCategory.uppercase()
        return when {
            cat.contains("FRONTIER") || cat.contains("DEEP_REASONING") || cat.contains("COMPLEX_ANALYSIS") || cat.contains("UNIVERSAL_PROMPT_COMPOSER") -> "frontier"
            cat.contains("REASONING") || cat.contains("CODE") || cat.contains("AUTONOMOUS") -> "complex"
            cat.contains("FAST") || cat.contains("CLASSIF") || cat.contains("TRIAGE") || promptLength < 200 -> "simple"
            else -> "moderate"
        }
    }

    fun selectOptimalModel(
        taskCategory: String,
        sensitivityTier: String,
        promptLength: Int
    ): Pair<String, String> { // providerId to modelIdentifier
        if (taskCategory == "IMAGE_GEN") {
            val imgProvider = providerRepo.getActiveImageProvider()
            return imgProvider.providerCode.lowercase() to imgProvider.defaultModel
        }

        val providers = providerRepo.getLlmProvidersOrderedByFallbackPriority()
        val cat = taskCategory.uppercase()
        val complexityTier = mapTaskToComplexityTier(taskCategory, promptLength)

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
            val code = specialized.providerCode.lowercase()
            val candidateModel = modelRepo.getActiveModelsForTier(code, complexityTier)
                .firstOrNull()?.modelIdentifier
                ?: specialized.defaultModel.ifBlank { null }
                ?: modelRepo.getActiveModelsForProvider(code).firstOrNull()?.modelIdentifier
                ?: ""
            return code to candidateModel
        }

        // 2. Fallback to top priority provider from database
        val topProvider = providers.firstOrNull()
        val topCode = topProvider?.providerCode?.lowercase() ?: "nvidia_nim"
        val topModel = modelRepo.getActiveModelsForTier(topCode, complexityTier)
            .firstOrNull()?.modelIdentifier
            ?: topProvider?.defaultModel?.ifBlank { null }
            ?: modelRepo.getActiveModelsForProvider(topCode).firstOrNull()?.modelIdentifier
            ?: modelRepo.getAllActive().firstOrNull()?.modelIdentifier
            ?: ""
        return topCode to topModel
    }
}
