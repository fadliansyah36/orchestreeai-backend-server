package ai.orchestree.backend.api

import ai.orchestree.backend.billing.enforceEntitlementGate
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class BrandAssetItem(
    val id: String,
    val tenantId: String,
    val title: String,
    val assetType: String,
    val fileUrl: String,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class BrandAssetCreateRequest(
    val title: String,
    val assetType: String,
    val fileUrl: String,
    val isLocked: Boolean = false
)

@Serializable
data class GenerateImageRequest(
    val prompt: String? = null,
    val productName: String? = null,
    val targetAudience: String? = null,
    val visualTheme: String? = null,
    val aspectRatio: String? = "1:1",
    val tenantId: String? = null
)

@Serializable
data class ComposePromptRequest(
    val productName: String,
    val targetAudience: String,
    val visualTheme: String,
    val aspectRatio: String = "1:1"
)

@Serializable
data class CampaignCreativeRequest(
    val topic: String,
    val platform: String = "INSTAGRAM",
    val tenantId: String? = null
)

@Serializable
data class BrandLogoUploadRequest(
    val fileName: String,
    val fileBase64: String,
    val tenantId: String? = null
)

private val logger = org.slf4j.LoggerFactory.getLogger("ai.orchestree.backend.api.GenerativeStudioRoutes")

fun Route.generativeStudioRoutes(
    brandAssetService: ai.orchestree.backend.generativestudio.BrandAssetService = ai.orchestree.backend.generativestudio.BrandAssetService.defaultInstance,
    generativeStudioService: ai.orchestree.backend.generativestudio.GenerativeStudioService = ai.orchestree.backend.generativestudio.GenerativeStudioService(),
    orchestrationEngine: ai.orchestree.backend.orchestration.OrchestrationEngine = ai.orchestree.backend.orchestration.OrchestrationEngine()
) {
    route("/studio") {
        post("/generate-image") {
            if (!call.enforceEntitlementGate("generative_studio")) return@post
            val req = try {
                call.receive<GenerateImageRequest>()
            } catch (e: Exception) {
                GenerateImageRequest()
            }
            val tenantId = req.tenantId ?: call.request.queryParameters["tenantId"] ?: "tenant-default"

            // 1. Compose Grounded Creative Prompt if prompt not explicitly supplied
            val finalPrompt = if (!req.prompt.isNullOrBlank()) {
                req.prompt
            } else {
                val composed = ai.orchestree.backend.generativestudio.PromptComposer.composeGroundedPrompt(
                    productName = req.productName ?: "Exclusive Commercial Product",
                    targetAudience = req.targetAudience ?: "General Consumers",
                    visualTheme = req.visualTheme ?: "Studio Lighting, Minimalist",
                    aspectRatio = req.aspectRatio ?: "1:1"
                )
                composed.mainPrompt
            }

            // 2. Dispatch via OrchestrationEngine (wf-marketing-campaign DAG) safely
            var executionId: String? = null
            try {
                val executionResult = orchestrationEngine.runWorkflow(
                    tenantId = tenantId,
                    workflowDefId = "wf-marketing-campaign",
                    prompt = finalPrompt,
                    contextParams = mapOf(
                        "productName" to (req.productName ?: "Product"),
                        "targetAudience" to (req.targetAudience ?: "General"),
                        "visualTheme" to (req.visualTheme ?: "Studio Lighting"),
                        "aspectRatio" to (req.aspectRatio ?: "1:1")
                    )
                )
                executionId = executionResult.executionId
            } catch (e: Exception) {
                logger.warn("Workflow dispatch for marketing-campaign encountered non-blocking warning: ${e.message}")
            }

            // 3. Execute Image/Design Generation through ModelRouter fallback chain (GPT-Image-2 -> OpenRouter -> NVIDIA NIM)
            try {
                val imageUrl = generativeStudioService.generateImage(finalPrompt)

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "completed",
                        "imageUrl" to imageUrl,
                        "prompt" to finalPrompt,
                        "workflowExecutionId" to (executionId ?: "direct-gen")
                    )
                )
            } catch (e: Exception) {
                logger.error("Studio generate-image failed: ${e.message}", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "failed",
                        "error" to (e.message ?: "Image generation failed across all providers in chain"),
                        "prompt" to finalPrompt
                    )
                )
            }
        }

        post("/compose-prompt") {
            if (!call.enforceEntitlementGate("generative_studio")) return@post
            val req = call.receive<ComposePromptRequest>()
            val composed = ai.orchestree.backend.generativestudio.PromptComposer.composeGroundedPrompt(
                productName = req.productName,
                targetAudience = req.targetAudience,
                visualTheme = req.visualTheme,
                aspectRatio = req.aspectRatio
            )
            call.respond(HttpStatusCode.OK, composed)
        }

        post("/campaign-creative") {
            if (!call.enforceEntitlementGate("generative_studio")) return@post
            val req = call.receive<CampaignCreativeRequest>()
            val tenantId = req.tenantId ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val plan = generativeStudioService.generateCampaignCreative(tenantId, req.topic, req.platform)
            call.respond(HttpStatusCode.OK, plan)
        }

        get("/templates") {
            call.respond(HttpStatusCode.OK, emptyList<String>())
        }

        // 100% Deterministic Logo Upload (Fase 118 / Langkah 2.1)
        post("/brand-assets/upload-logo") {
            val req = call.receive<BrandLogoUploadRequest>()
            val tenantId = req.tenantId ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val fileBytes = try {
                java.util.Base64.getDecoder().decode(req.fileBase64)
            } catch (e: Exception) {
                req.fileBase64.toByteArray(Charsets.UTF_8)
            }
            val result = brandAssetService.uploadBrandLogo(
                tenantId = tenantId,
                fileBytes = fileBytes,
                fileName = req.fileName
            )
            call.respond(HttpStatusCode.Created, result)
        }

        post("/brand-assets/logo") {
            val req = call.receive<BrandLogoUploadRequest>()
            val tenantId = req.tenantId ?: call.request.queryParameters["tenantId"] ?: "tenant-default"
            val fileBytes = try {
                java.util.Base64.getDecoder().decode(req.fileBase64)
            } catch (e: Exception) {
                req.fileBase64.toByteArray(Charsets.UTF_8)
            }
            val result = brandAssetService.uploadBrandLogo(
                tenantId = tenantId,
                fileBytes = fileBytes,
                fileName = req.fileName
            )
            call.respond(HttpStatusCode.Created, result)
        }

        get("/assets") {
            val tenantId = call.request.queryParameters["tenantId"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.OK,
                listOf(
                    BrandAssetItem(
                        id = "asset-01",
                        tenantId = tenantId,
                        title = "Corporate Logo Vector Dark",
                        assetType = "LOGO",
                        fileUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe",
                        isLocked = true
                    ),
                    BrandAssetItem(
                        id = "asset-02",
                        tenantId = tenantId,
                        title = "Brand Typography & Color Palette",
                        assetType = "STYLE_GUIDE",
                        fileUrl = "https://images.unsplash.com/photo-1542744094-3a31f272c490",
                        isLocked = false
                    )
                )
            )
        }

        post("/assets") {
            val req = call.receive<BrandAssetCreateRequest>()
            val tenantId = call.request.queryParameters["tenantId"] ?: "tenant-default"
            call.respond(
                HttpStatusCode.Created,
                BrandAssetItem(
                    id = "asset-${java.util.UUID.randomUUID().toString().take(8)}",
                    tenantId = tenantId,
                    title = req.title,
                    assetType = req.assetType,
                    fileUrl = req.fileUrl,
                    isLocked = req.isLocked
                )
            )
        }
    }
}
