package ai.orchestree.backend.generativestudio

import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

@Serializable
data class ContentPlan(
    val title: String,
    val caption: String,
    val visualPrompt: String,
    val targetPlatform: String,
    val hashtags: List<String> = emptyList()
)

class ContentPlanningLayer(
    private val modelRouter: ModelRouter = ModelRouter()
) {
    private val logger = LoggerFactory.getLogger(ContentPlanningLayer::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateContentPlan(
        topic: String,
        platform: String,
        tone: String = "professional",
        tenantId: String = "tenant-default"
    ): ContentPlan {
        val prompt = """
            Generate an engaging, platform-tailored marketing content plan for target platform '$platform'.
            Topic / Product / Theme: "$topic"
            Tone of Voice: $tone

            You must return a valid JSON object with the following keys:
            {
              "title": "A catchy headline or title",
              "caption": "A well-structured caption with hook, value proposition, and call to action",
              "visualPrompt": "A highly detailed visual prompt for generative AI image creation (e.g. subject, lighting, composition, style)",
              "hashtags": ["#tag1", "#tag2", "#tag3"]
            }
        """.trimIndent()

        try {
            val response = modelRouter.execute(
                ModelRouteRequest(
                    taskCategory = "CREATIVE_WRITING",
                    prompt = prompt,
                    systemInstruction = "You are an elite creative director and copywriter specializing in omnichannel social campaigns and AI imagery.",
                    tenantId = tenantId,
                    temperature = 0.7,
                    responseFormatJson = true
                )
            ).getOrThrow()

            val text = response.text.trim()
            val jsonMatch = Regex("""\{[\s\S]*\}""").find(text)?.value ?: text
            val jsonElement = json.parseToJsonElement(jsonMatch).jsonObject

            val title = jsonElement["title"]?.jsonPrimitive?.content ?: topic
            val caption = jsonElement["caption"]?.jsonPrimitive?.content ?: "Explore $topic with Orchestree AI."
            val visualPrompt = jsonElement["visualPrompt"]?.jsonPrimitive?.content
                ?: "High resolution cinematic photography of $topic, professional studio lighting, 8k"
            val hashtags = try {
                jsonElement["hashtags"]?.toString()
                    ?.let { Regex(""""([^"]+)"""").findAll(it).map { m -> m.groupValues[1] }.toList() }
                    ?: listOf("#${topic.filter { it.isLetterOrDigit() }}", "#OrchestreeAI")
            } catch (_: Exception) {
                listOf("#${topic.filter { it.isLetterOrDigit() }}", "#OrchestreeAI")
            }

            logger.info("Successfully generated dynamic ContentPlan for topic '$topic' via ModelRouter")
            return ContentPlan(
                title = title,
                caption = caption,
                visualPrompt = visualPrompt,
                targetPlatform = platform,
                hashtags = hashtags
            )
        } catch (e: Exception) {
            logger.warn("ModelRouter content generation failed or fell back, using smart fallback: ${e.message}")
            val cleanTag = topic.filter { it.isLetterOrDigit() }
            return ContentPlan(
                title = topic.replaceFirstChar { it.uppercase() },
                caption = "Elevate your strategy with $topic. Designed for agile teams, delivering real business outcomes. Learn more with Orchestree AI.",
                visualPrompt = "Clean isometric 3D render showcasing $topic, modern corporate technology aesthetic, dramatic studio lighting, octane render 8k",
                targetPlatform = platform,
                hashtags = listOf("#$cleanTag", "#OrchestreeAI", "#Enterprise", "#Innovation")
            )
        }
    }

    fun planContent(topic: String, platform: String): ContentPlan {
        return runBlocking {
            generateContentPlan(topic, platform)
        }
    }
}
