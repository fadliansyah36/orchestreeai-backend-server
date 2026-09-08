package ai.orchestree.backend.generativestudio

import kotlinx.serialization.Serializable

@Serializable
data class ContentPlan(
    val title: String,
    val caption: String,
    val visualPrompt: String,
    val targetPlatform: String,
    val hashtags: List<String> = emptyList()
)

class ContentPlanningLayer {
    fun planContent(topic: String, platform: String): ContentPlan {
        return ContentPlan(
            title = topic,
            caption = "Content caption for $topic",
            visualPrompt = "Professional visual for $topic",
            targetPlatform = platform,
            hashtags = listOf("#$topic", "#OrchestreeAI")
        )
    }
}
