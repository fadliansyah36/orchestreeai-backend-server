package ai.orchestree.backend.api

import ai.orchestree.backend.memory.CompanyBrainEmbeddingPipeline
import ai.orchestree.backend.memory.ConversationRollingMemoryEngine
import ai.orchestree.backend.memory.HybridSearchEngine
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.security.PromptInjectionGuard
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
data class ChatApiRequest(
    val message: String,
    val tenantId: String = "tenant-default",
    val conversationId: String? = null,
    val agentId: String? = null,
    val systemPrompt: String? = null
)

@Serializable
data class ChatApiResponse(
    val reply: String,
    val modelUsed: String,
    val latencyMs: Long,
    val conversationId: String,
    val references: List<String> = emptyList()
)

@Serializable
data class DocumentUploadRequest(
    val title: String,
    val content: String,
    val tenantId: String
)

private val modelRouter = ModelRouter()
private val orchestrationEngine = ai.orchestree.backend.orchestration.OrchestrationEngine(modelRouter = modelRouter)
private val rollingMemory = ConversationRollingMemoryEngine()
private val hybridSearch = HybridSearchEngine()
private val embeddingPipeline = CompanyBrainEmbeddingPipeline()
private val promptGuard = PromptInjectionGuard()
private val intentClassifier = ai.orchestree.backend.intelligence.IntentClassifier(modelRouter)
private val outputValidator = ai.orchestree.backend.intelligence.OutputValidator()

private suspend fun io.ktor.server.routing.RoutingContext.handleChatMessage(req: ChatApiRequest) {
    val convId = req.conversationId ?: "conv-${java.util.UUID.randomUUID().toString().take(8)}"

    // 1. Guard against prompt injection
    val (isSafe, blockReason) = promptGuard.inspect(req.message)
    if (!isSafe) {
        call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to (blockReason ?: "Security policy violation"))
        )
        return
    }

    // 2. POLA A: Dispatch through OrchestrationEngine with full governance & audit trail
    val wfResult = orchestrationEngine.runWorkflow(
        tenantId = req.tenantId,
        workflowDefId = "wf-chat-inbound",
        prompt = req.message,
        contextParams = mapOf(
            "conversationId" to convId,
            "systemPrompt" to (req.systemPrompt ?: "Anda adalah Asisten AI Bisnis OrchestreeAI yang profesional, analitis, dan solutif.")
        )
    )

    val reply = wfResult.finalOutput ?: "Respons berhasil diproses."
    val modelUsed = wfResult.nodeRuns.find { it.nodeId == "chat-n3-synthesize" }?.output?.take(30) ?: "nvidia-nim"
    val refs = emptyList<String>()

    rollingMemory.appendMessage(convId, "User", req.message)
    rollingMemory.appendMessage(convId, "Assistant", reply)

    call.respond(
        HttpStatusCode.OK,
        ChatApiResponse(
            reply = reply,
            modelUsed = modelUsed,
            latencyMs = wfResult.durationMs,
            conversationId = convId,
            references = refs
        )
    )
}

fun Route.chatRoutes() {
    route("/chat") {
        /**
         * LANGKAH 4.2: Client Android memanggil endpoint backend (POST /api/v1/chat)
         * yang SECARA INTERNAL memanggil ModelRouter — client TIDAK PERNAH memegang API key LLM.
         */
        post {
            val req = call.receive<ChatApiRequest>()
            handleChatMessage(req)
        }

        post("/messages") {
            val req = call.receive<ChatApiRequest>()
            handleChatMessage(req)
        }


        get("/history/{conversationId}") {
            val convId = call.parameters["conversationId"] ?: ""
            val history = rollingMemory.getRollingHistory(convId)
            call.respond(HttpStatusCode.OK, history.map { mapOf("role" to it.first, "content" to it.second) })
        }
    }

    route("/agents") {
        post("/{agentId}/chat") {
            val agentId = call.parameters["agentId"] ?: "general-agent"
            val req = call.receive<ChatApiRequest>()
            val convId = req.conversationId ?: "agent-$agentId-${java.util.UUID.randomUUID().toString().take(6)}"

            val agentPersona = when (agentId) {
                "cmo_maya" -> "Anda adalah Maya, Chief Marketing Officer AI. Fokus pada brand awareness, customer acquisition, dan strategi kampanye."
                "cfo_fauzan" -> "Anda adalah Fauzan, Chief Financial Officer AI. Fokus pada cash flow, budgeting, ROI, dan efisiensi biaya."
                "coo_budi" -> "Anda adalah Budi, Chief Operating Officer AI. Fokus pada SLA, logistik, efisiensi operasional, dan kepuasan pelanggan."
                "cro_dewi" -> "Anda adalah Dewi, Chief Revenue Officer AI. Fokus pada sales pipeline, conversion rate, dan ekspansi pasar."
                else -> "Anda adalah AI Autonomous Workforce Agent untuk OrchestreeAI."
            }

            val fullPrompt = "$agentPersona\nUser: ${req.message}\nAssistant:"
            val startTime = System.currentTimeMillis()
            val tenant = req.tenantId.ifBlank { "tenant-default" }
            val result = modelRouter.execute(
                ModelRouteRequest(
                    taskCategory = "REASONING",
                    prompt = fullPrompt,
                    tenantId = tenant
                )
            )

            if (result.isSuccess) {
                val response = result.getOrThrow()
                call.respond(
                    HttpStatusCode.OK,
                    ChatApiResponse(
                        reply = response.text,
                        modelUsed = response.modelUsed,
                        latencyMs = System.currentTimeMillis() - startTime,
                        conversationId = convId
                    )
                )
            } else {
                val ex = result.exceptionOrNull()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "failed",
                        "error" to (ex?.message ?: "Gagal memproses respons agent"),
                        "details" to if (ex is ai.orchestree.backend.modelrouter.AllProvidersInChainFailedException) ex.providerErrors else emptyMap<String, String>()
                    )
                )
            }
        }
    }

    route("/company-brain") {
        post("/documents") {
            val uploadReq = call.receive<DocumentUploadRequest>()
            val docId = "doc-${java.util.UUID.randomUUID().toString().take(8)}"

            val chunkCount = embeddingPipeline.processDocumentAsync(
                documentId = docId,
                tenantId = uploadReq.tenantId,
                rawText = uploadReq.content
            )

            call.respond(
                HttpStatusCode.Created,
                mapOf(
                    "documentId" to docId,
                    "title" to uploadReq.title,
                    "chunksIndexed" to chunkCount,
                    "status" to "INDEXED"
                )
            )
        }

        get("/search") {
            val query = call.request.queryParameters["query"] ?: ""
            val tenantId = call.request.queryParameters["tenantId"] ?: "tenant-default"
            val results = hybridSearch.search(tenantId, query, topK = 5)
            call.respond(HttpStatusCode.OK, results)
        }
    }
}
