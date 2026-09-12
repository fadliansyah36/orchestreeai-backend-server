package ai.orchestree.backend.plugins

import ai.orchestree.backend.api.adminRoutes
import ai.orchestree.backend.api.adminPublicSecurityRoutes
import ai.orchestree.backend.api.attendanceRoutes
import ai.orchestree.backend.api.authRoutes
import ai.orchestree.backend.api.billingRoutes
import ai.orchestree.backend.api.chatRoutes
import ai.orchestree.backend.api.enterpriseRoutes
import ai.orchestree.backend.api.generativeStudioRoutes
import ai.orchestree.backend.api.memoryRoutes
import ai.orchestree.backend.api.omnichannelSalesRoutes
import ai.orchestree.backend.api.orchestrationRoutes
import ai.orchestree.backend.api.presenceRoutes
import ai.orchestree.backend.api.selectionRoutes
import ai.orchestree.backend.api.tenantRoutes
import ai.orchestree.backend.api.masterDataPublicRoutes
import ai.orchestree.backend.api.masterDataTenantRoutes
import ai.orchestree.backend.prospect.prospectPublicRoutes
import ai.orchestree.backend.prospect.prospectAdminRoutes
import ai.orchestree.backend.webhooks.PaymentNotification
import ai.orchestree.backend.webhooks.PaymentWebhookHandler
import ai.orchestree.backend.webhooks.TelegramWebhookHandler
import ai.orchestree.backend.webhooks.WhatsAppWebhookHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class HealthStatusResponse(
    val status: String,
    val service: String,
    val timestamp: Long
)

fun Application.configureRouting() {
    val paymentHandler = PaymentWebhookHandler()
    val whatsappHandler = WhatsAppWebhookHandler()
    val telegramHandler = TelegramWebhookHandler()
    val json = Json { ignoreUnknownKeys = true }

    routing {
        // Public Root and Health check
        get("/") {
            call.respondText("OrchestreeAI Enterprise Autonomous AI Workforce Server - Running")
        }

        get("/health") {
            call.respond(
                HealthStatusResponse(
                    status = "healthy",
                    service = "orchestreeai-backend-server",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        get("/api/health") {
            call.respond(
                HealthStatusResponse(
                    status = "healthy",
                    service = "orchestreeai-backend-server",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // Direct Authentication Routes (/auth/login, /auth/refresh, etc.)
        authRoutes()
        adminPublicSecurityRoutes()

        route("/api/v1") {
            // Public Authentication Routes
            authRoutes()
            presenceRoutes()
            billingRoutes()

            // Public Platform Assets (for Splash, Header, & App Branding)
            get("/platform-assets/icon-logo") {
                val url = ai.orchestree.backend.generativestudio.BrandAssetService.defaultInstance.getPlatformIconLogoUrl()
                call.respond(HttpStatusCode.OK, mapOf("platformIconLogoUrl" to (url ?: "")))
            }

            // Public Master Data Endpoints (Fase 91 & 120)
            masterDataPublicRoutes()
            masterDataTenantRoutes()

            // Public Prospect Registration System (Fase 127)
            prospectPublicRoutes()

            // Public Webhook Endpoints with signature verification
            route("/payments") {
                post("/webhook/{gateway}") {
                    val gateway = call.parameters["gateway"] ?: "midtrans"
                    val body = call.receiveText()
                    val serverKey = ai.orchestree.backend.config.EnvLoader.get("PAYMENT_GATEWAY_SERVER_KEY")

                    val payloadObj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                    val orderId = payloadObj?.get("order_id")?.jsonPrimitive?.content ?: ""
                    val statusCode = payloadObj?.get("status_code")?.jsonPrimitive?.content ?: ""
                    val grossAmount = payloadObj?.get("gross_amount")?.jsonPrimitive?.content ?: ""
                    val signatureKey = payloadObj?.get("signature_key")?.jsonPrimitive?.content
                        ?: call.request.header("X-Signature")
                    val transactionStatus = payloadObj?.get("transaction_status")?.jsonPrimitive?.content ?: "settlement"

                    val notif = PaymentNotification(
                        orderId = orderId,
                        statusCode = statusCode,
                        grossAmount = grossAmount,
                        transactionStatus = transactionStatus,
                        signatureKey = signatureKey
                    )

                    val verified = paymentHandler.handlePaymentNotification(notif, serverKey)
                    if (!verified) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid payment webhook signature"))
                    } else {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "processed", "gateway" to gateway))
                    }
                }
            }

            route("/shipments") {
                post("/webhook/{courier}") {
                    val courier = call.parameters["courier"] ?: "jne"
                    val body = call.receiveText()
                    call.respond(HttpStatusCode.OK, mapOf("status" to "processed", "courier" to courier))
                }
            }

            route("/webhooks") {
                post("/payment/{gateway}") {
                    val gateway = call.parameters["gateway"] ?: "midtrans"
                    val body = call.receiveText()
                    val serverKey = ai.orchestree.backend.config.EnvLoader.get("PAYMENT_GATEWAY_SERVER_KEY")

                    val payloadObj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                    val orderId = payloadObj?.get("order_id")?.jsonPrimitive?.content ?: ""
                    val statusCode = payloadObj?.get("status_code")?.jsonPrimitive?.content ?: ""
                    val grossAmount = payloadObj?.get("gross_amount")?.jsonPrimitive?.content ?: ""
                    val signatureKey = payloadObj?.get("signature_key")?.jsonPrimitive?.content
                        ?: call.request.header("X-Signature")
                    val transactionStatus = payloadObj?.get("transaction_status")?.jsonPrimitive?.content ?: "settlement"

                    val notif = PaymentNotification(
                        orderId = orderId,
                        statusCode = statusCode,
                        grossAmount = grossAmount,
                        transactionStatus = transactionStatus,
                        signatureKey = signatureKey
                    )

                    val verified = paymentHandler.handlePaymentNotification(notif, serverKey)
                    if (!verified) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid payment webhook signature"))
                    } else {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "processed", "gateway" to gateway))
                    }
                }

                post("/whatsapp") {
                    val sig = call.request.header("X-Hub-Signature-256")
                    val body = call.receiveText()
                    val appSecret = ai.orchestree.backend.config.EnvLoader.get("META_APP_SECRET", "meta_webhook_secret")
                    val verified = whatsappHandler.handlePayload(body, sig, appSecret)
                    if (!verified) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid WhatsApp/Meta webhook signature"))
                    } else {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "received"))
                    }
                }

                post("/telegram") {
                    val secretToken = call.request.header("X-Telegram-Bot-Api-Secret-Token")
                    val expectedToken = ai.orchestree.backend.config.EnvLoader.get("TELEGRAM_WEBHOOK_SECRET", ai.orchestree.backend.config.EnvLoader.get("TELEGRAM_OFFICIAL_BOT_TOKEN"))
                    val verified = telegramHandler.handleWebhook(secretToken, expectedToken)
                    if (!verified) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid Telegram webhook secret token"))
                    } else {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
                    }
                }
            }

            // Public Security & Admin Authentication Routes (Fase 124)
            adminPublicSecurityRoutes()

            // Protected Routes: Require valid Supabase JWT
            authenticate("supabase-auth") {
                tenantRoutes()
                omnichannelSalesRoutes()
                enterpriseRoutes()
                orchestrationRoutes()
                chatRoutes()
                generativeStudioRoutes()
                adminRoutes()
                attendanceRoutes()
                memoryRoutes()
                selectionRoutes()
                prospectAdminRoutes()
            }
        }
    }
}
