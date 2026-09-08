package ai.orchestree.backend.generativestudio

import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter

class ContentGuardrailEngine {
    fun checkCompliance(content: String): Pair<Boolean, String?> {
        val sensitiveWords = listOf("guaranteed 100% profit", "get rich quick", "scam")
        for (w in sensitiveWords) {
            if (content.contains(w, ignoreCase = true)) {
                return false to "Content violates advertising compliance: contains restricted term '$w'"
            }
        }
        return true to null
    }
}

class GenerativeStudioService(
    private val modelRouter: ModelRouter = ModelRouter(),
    private val guardrailEngine: ContentGuardrailEngine = ContentGuardrailEngine()
) {
    suspend fun generateCampaignCreative(
        tenantId: String,
        topic: String,
        platform: String
    ): ContentPlan {
        val prompt = "Create a high-converting $platform marketing caption and image generation prompt for product/topic: $topic"
        val res = modelRouter.execute(
            ModelRouteRequest(
                taskCategory = "REASONING",
                prompt = prompt,
                tenantId = tenantId
            )
        )
        val text = if (res.isSuccess) res.getOrThrow().text else "Exciting offer on $topic! Get yours today."

        return ContentPlan(
            title = topic,
            caption = text,
            visualPrompt = "Professional, cinematic high-resolution product photography of $topic in modern studio lighting",
            targetPlatform = platform,
            hashtags = listOf("#$topic", "#Promo", "#OrchestreeAI")
        )
    }

    suspend fun generateImage(prompt: String): String {
        val res = modelRouter.gptImage2Client.complete(
            ai.orchestree.backend.modelrouter.LlmRequest(prompt = prompt, model = "gpt-image-2")
        )
        return if (res.isSuccess) res.getOrThrow().text else "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe"
    }
}
