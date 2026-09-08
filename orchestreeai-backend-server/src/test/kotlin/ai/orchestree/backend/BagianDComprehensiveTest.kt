package ai.orchestree.backend

import ai.orchestree.backend.api.ChatApiRequest
import ai.orchestree.backend.api.ChatApiResponse
import ai.orchestree.backend.api.chatRoutes
import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.RedisConfig
import ai.orchestree.backend.database.RedisService
import ai.orchestree.backend.memory.CompanyBrainEmbeddingPipeline
import ai.orchestree.backend.memory.HybridSearchEngine
import ai.orchestree.backend.memory.MemoryDocument
import ai.orchestree.backend.memory.MemoryService
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.plugins.configureSerialization
import ai.orchestree.backend.scheduler.SchedulerEngine
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BagianDComprehensiveTest {

    @Test
    fun testRedisServiceRevocationAndRateLimiting() {
        val redisService = RedisService(RedisConfig(url = ""))
        val jti = "jwt-sample-token-id-12345"

        assertFalse(redisService.isTokenRevoked(jti))

        // Revoke token
        assertTrue(redisService.revokeToken(jti, ttlSeconds = 60))
        assertTrue(redisService.isTokenRevoked(jti))

        // Test Semantic Cache
        redisService.setSemanticCache("prompt_hash_1", "Cached LLM Response", ttlSeconds = 60)
        assertEquals("Cached LLM Response", redisService.getSemanticCache("prompt_hash_1"))

        // Test Rate Limiter
        val rateKey = "tenant-101"
        assertTrue(redisService.checkAndIncrementRateLimit(rateKey, maxRequests = 2, windowSeconds = 60))
        assertTrue(redisService.checkAndIncrementRateLimit(rateKey, maxRequests = 2, windowSeconds = 60))
        assertFalse(redisService.checkAndIncrementRateLimit(rateKey, maxRequests = 2, windowSeconds = 60)) // 3rd request fails
    }

    @Test
    fun testHybridSearchReciprocalRankFusion() = runBlocking {
        val memoryService = MemoryService()
        val tenantId = "tenant-test-rrf"

        memoryService.upsertDocument(
            MemoryDocument(
                id = "doc-1",
                tenantId = tenantId,
                sourceType = "COMPANY_BRAIN",
                content = "SOP Layanan Pelanggan: Kompensasi keterlambatan adalah voucher 50rb"
            )
        )
        memoryService.upsertDocument(
            MemoryDocument(
                id = "doc-2",
                tenantId = tenantId,
                sourceType = "COMPANY_BRAIN",
                content = "Kebijakan Cuti Karyawan dan Hari Libur Nasional"
            )
        )

        val hybridSearch = HybridSearchEngine(supabase = null, memoryService = memoryService)
        val results = hybridSearch.search(tenantId, "kompensasi voucher keterlambatan", topK = 1)

        assertEquals(1, results.size)
        assertEquals("doc-1", results[0].id)
        assertTrue(results[0].content.contains("voucher 50rb"))
    }

    @Test
    fun testCompanyBrainAsyncEmbeddingPipeline() = runBlocking {
        val memoryService = MemoryService()
        val pipeline = CompanyBrainEmbeddingPipeline(supabase = null, memoryService = memoryService)

        val docText = "OrchestreeAI adalah platform Autonomous AI Workforce Enterprise multi-tenant pertama di Indonesia."
        val chunkCount = pipeline.processDocumentAsync("doc-sop-001", "tenant-123", docText, chunkSize = 10, chunkOverlap = 2)

        assertTrue(chunkCount > 0)
        val indexed = memoryService.queryByTenant("tenant-123")
        assertEquals(chunkCount, indexed.size)
    }

    @Test
    fun testDirectServerSideChatApiEndpoint() = testApplication {
        application {
            configureSerialization()
            routing {
                chatRoutes()
            }
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.post("/chat") {
            contentType(ContentType.Application.Json)
            setBody(
                ChatApiRequest(
                    message = "Halo, bagaimana cara meningkatkan efisiensi operasional?",
                    tenantId = "tenant-test-chat"
                )
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val chatResp = response.body<ChatApiResponse>()
        assertNotNull(chatResp.reply)
        assertTrue(chatResp.reply.isNotBlank())
        assertNotNull(chatResp.modelUsed)
        assertTrue(chatResp.latencyMs >= 0)
    }

    @Test
    fun testAgentPersonaChatEndpoint() = testApplication {
        application {
            configureSerialization()
            routing {
                chatRoutes()
            }
        }

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.post("/agents/cmo_maya/chat") {
            contentType(ContentType.Application.Json)
            setBody(
                ChatApiRequest(
                    message = "Buatkan ide promosi diskon 20%",
                    tenantId = "tenant-test-cmo"
                )
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val chatResp = response.body<ChatApiResponse>()
        assertNotNull(chatResp.reply)
        assertTrue(chatResp.reply.isNotBlank())
    }

    @Test
    fun testOrchestrationEngineWorkflowExecution() = runBlocking {
        val engine = OrchestrationEngine()
        val result = engine.runWorkflow(
            tenantId = "tenant-orch-test",
            workflowDefId = "wf-customer-support",
            prompt = "Pelanggan meminta refund transaksi TRX-9912 karena barang rusak"
        )

        assertNotNull(result.executionId)
        assertEquals("COMPLETED", result.status)
        assertTrue(result.nodeRuns.isNotEmpty())
    }

    @Test
    fun testSchedulerEngineAutonomousExecution() = runBlocking {
        val scheduler = SchedulerEngine(ModelRouter())

        // Trigger manual execution of Proactive Briefing
        val log = scheduler.triggerJobManually("PROACTIVE_BRIEFING", "tenant-sched-001")
        assertEquals("PROACTIVE_BRIEFING", log.jobName)
        assertEquals("SUCCESS", log.status)
        assertNotNull(log.resultSummary)
        assertTrue(log.resultSummary.isNotBlank())

        val logs = scheduler.getLogs()
        assertTrue(logs.isNotEmpty())
    }
}
