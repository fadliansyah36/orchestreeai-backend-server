package ai.orchestree.backend.api

import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRepository
import ai.orchestree.backend.memory.CandidateInteraction
import ai.orchestree.backend.memory.HybridMemorySearchEngine
import ai.orchestree.backend.memory.MemoryConsolidator
import ai.orchestree.backend.memory.MemoryDecayEngine
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
data class SearchMemoryRequest(
    val tenantId: String,
    val query: String,
    val enableReranking: Boolean = true
)

@Serializable
data class DecayMemoryRequest(
    val tenantId: String? = null
)

fun Route.memoryRoutes(
    memoryRepo: MemoryDocumentRepository = MemoryDocumentRepository(),
    consolidator: MemoryConsolidator = MemoryConsolidator(memoryRepo),
    decayEngine: MemoryDecayEngine = MemoryDecayEngine(memoryRepo),
    hybridSearchEngine: HybridMemorySearchEngine = HybridMemorySearchEngine(memoryRepo)
) {
    route("/memory") {
        // LANGKAH 1: Audit & Filter Kualitas Memory Consolidator (PRD Master Bagian 17.2)
        post("/consolidate/evaluate") {
            val interaction = call.receive<CandidateInteraction>()
            val decision = consolidator.evaluateInteraction(interaction)
            call.respond(HttpStatusCode.OK, decision)
        }

        post("/consolidate/batch") {
            val interactions = call.receive<List<CandidateInteraction>>()
            val decisions = consolidator.consolidateBatch(interactions)
            call.respond(HttpStatusCode.OK, decisions)
        }

        // LANGKAH 2: Decay Otomatis Memori Lama (Episodic 90 hari, Competitive 365 hari)
        post("/decay") {
            val req = try { call.receive<DecayMemoryRequest>() } catch (e: Exception) { DecayMemoryRequest() }
            val summary = decayEngine.applyMemoryDecay(req.tenantId)
            call.respond(HttpStatusCode.OK, summary)
        }

        // LANGKAH 3: Tuning Hybrid Search (Top-20 -> Re-Rank -> Top-5)
        post("/search") {
            val req = call.receive<SearchMemoryRequest>()
            val result = hybridSearchEngine.search(req.tenantId, req.query, req.enableReranking)
            call.respond(HttpStatusCode.OK, result)
        }

        post("/search/ab-compare") {
            val req = call.receive<SearchMemoryRequest>()
            val comparison = hybridSearchEngine.runABComparison(req.tenantId, req.query)
            call.respond(HttpStatusCode.OK, comparison)
        }

        // Daftar dokumen memori aktif / arsip
        get("/documents") {
            val tenantId = call.parameters["tenantId"] ?: "tenant-default"
            val includeArchived = call.parameters["includeArchived"]?.toBoolean() ?: false
            val docs = memoryRepo.listByTenant(tenantId, includeArchived)
            call.respond(HttpStatusCode.OK, docs)
        }
    }
}
