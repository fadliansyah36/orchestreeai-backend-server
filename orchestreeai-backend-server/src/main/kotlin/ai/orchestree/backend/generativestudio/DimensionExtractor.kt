package ai.orchestree.backend.generativestudio

import kotlinx.serialization.Serializable

@Serializable
data class CanvasDimensionSpec(
    val channel: String, // INSTAGRAM_FEED, INSTAGRAM_STORY, FACEBOOK_BANNER, TIKTOK_VIDEO, SHOPEE_PRODUCT
    val width: Int,
    val height: Int,
    val aspectRatio: String,
    val dpi: Int = 72
)

object DimensionExtractor {

    fun getStandardDimensions(channel: String): CanvasDimensionSpec {
        return when (channel.uppercase()) {
            "INSTAGRAM_STORY", "TIKTOK_VIDEO" -> CanvasDimensionSpec(
                channel = channel.uppercase(),
                width = 1080,
                height = 1920,
                aspectRatio = "9:16"
            )
            "FACEBOOK_BANNER" -> CanvasDimensionSpec(
                channel = channel.uppercase(),
                width = 1200,
                height = 628,
                aspectRatio = "1.91:1"
            )
            "SHOPEE_PRODUCT", "TOKOPEDIA_PRODUCT" -> CanvasDimensionSpec(
                channel = channel.uppercase(),
                width = 800,
                height = 800,
                aspectRatio = "1:1"
            )
            else -> CanvasDimensionSpec(
                channel = "INSTAGRAM_FEED",
                width = 1080,
                height = 1080,
                aspectRatio = "1:1"
            )
        }
    }
}
