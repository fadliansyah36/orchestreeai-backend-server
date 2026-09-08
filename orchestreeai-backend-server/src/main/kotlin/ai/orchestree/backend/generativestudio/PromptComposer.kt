package ai.orchestree.backend.generativestudio

import kotlinx.serialization.Serializable

@Serializable
data class ComposedCreativePrompt(
    val mainPrompt: String,
    val negativePrompt: String,
    val styleGuidelines: List<String>,
    val targetAspect: String
)

object PromptComposer {

    fun composeGroundedPrompt(
        productName: String,
        targetAudience: String,
        visualTheme: String,
        aspectRatio: String = "1:1"
    ): ComposedCreativePrompt {
        val main = "Professional commercial studio product photography of $productName. " +
                "Setting: Clean, elegant modern aesthetic tailored for $targetAudience. " +
                "Style: $visualTheme, high dynamic range, soft natural lighting, hyperrealistic, 8k resolution."

        val negative = "blurry, low quality, distorted text, watermark, cropped, oversaturated, amateur"

        val styles = listOf(
            "Studio Softbox Lighting",
            "Minimalist Depth of Field",
            "Neutral Warm Palette",
            "Compliant with Brand Guidelines"
        )

        return ComposedCreativePrompt(
            mainPrompt = main,
            negativePrompt = negative,
            styleGuidelines = styles,
            targetAspect = aspectRatio
        )
    }
}
