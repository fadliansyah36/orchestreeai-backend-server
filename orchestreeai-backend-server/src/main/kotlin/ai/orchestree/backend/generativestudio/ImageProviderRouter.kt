package ai.orchestree.backend.generativestudio

class ImageProviderRouter {
    /**
     * Fallback Priority:
     * Tier 1: GPT-Image-2 (Apimart)
     * Tier 2: OpenAI DALL-E 3
     * Tier 3: Stability AI SDXL
     */
    fun getFallbackChain(): List<String> = listOf("gpt-image-2", "dall-e-3", "sdxl-turbo")

    fun routeProvider(modelPreference: String?): String {
        return when (modelPreference?.lowercase()) {
            "dalle", "dall-e", "dall-e-3", "openai" -> "dall-e-3"
            "sdxl", "stability", "stability-ai" -> "sdxl-turbo"
            else -> "gpt-image-2"
        }
    }
}
