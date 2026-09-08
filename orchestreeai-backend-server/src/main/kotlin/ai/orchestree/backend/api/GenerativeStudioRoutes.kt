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
data class BrandLogoUploadRequest(
    val fileName: String,
    val fileBase64: String,
    val tenantId: String? = null
)

fun Route.generativeStudioRoutes(
    brandAssetService: ai.orchestree.backend.generativestudio.BrandAssetService = ai.orchestree.backend.generativestudio.BrandAssetService.defaultInstance
) {
    route("/studio") {
        post("/generate-image") {
            if (!call.enforceEntitlementGate("generative_studio")) return@post
            call.respond(HttpStatusCode.OK, mapOf("status" to "queued", "jobId" to "img-job-init"))
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
