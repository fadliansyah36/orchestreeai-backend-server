package ai.orchestree.backend

import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.api.InMemoryConversationStore
import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.selection.SelectionRepository
import ai.orchestree.backend.database.repositories.selection.SelectionRequestRecord
import ai.orchestree.backend.database.repositories.selection.SelectionResultRecord
import ai.orchestree.backend.database.repositories.taskboard.TaskRepository
import ai.orchestree.backend.database.repositories.workforce.TenantDomainRepository
import ai.orchestree.backend.util.PagedResponse
import ai.orchestree.backend.util.PaginationDefaults
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PaginationVerificationTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    private fun generateToken(tenantId: String, userId: String = "user-admin", role: String = "TENANT_ADMIN"): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("sub", userId)
            .withClaim("user_id", userId)
            .withClaim("tenant_id", tenantId)
            .withClaim("role", role)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600 * 1000))
            .sign(algorithm)
    }

    @Test
    fun testPaginationDefaults() {
        assertEquals(20, PaginationDefaults.DEFAULT_LIMIT)
        assertEquals(100, PaginationDefaults.MAX_LIMIT)
    }

    @Test
    fun testPagedResponseStructure() {
        val sampleList = listOf("item1", "item2", "item3")
        val response = PagedResponse(
            items = sampleList,
            total = 10L,
            limit = 20,
            offset = 0
        )
        assertEquals(3, response.items.size)
        assertEquals(10L, response.total)
        assertEquals(20, response.limit)
        assertEquals(0, response.offset)
    }

    @Test
    fun testTaskRepositoryPagination() = runBlocking {
        val supabase = SupabaseClientProvider.fromEnv()
        val taskRepo = TaskRepository(supabase)
        val (total, tasks) = taskRepo.getTasksForUserPaginated("usr-test", "default", 10, 0)
        assertTrue(total >= 0L)
        assertNotNull(tasks)

        val (logTotal, logs) = taskRepo.getActivityLogsForTaskPaginated("tsk-test", 10, 0)
        assertTrue(logTotal >= 0L)
        assertNotNull(logs)
    }

    @Test
    fun testTenantDomainRepositoryPagination() {
        val (reportsTotal, reports) = TenantDomainRepository.getDailyReportsPaginated("tenant-test", 10, 0)
        assertTrue(reportsTotal >= 0L)
        assertNotNull(reports)

        val (goalsTotal, goals) = TenantDomainRepository.getGoalsPaginated("tenant-test", 10, 0)
        assertTrue(goalsTotal >= 0L)
        assertNotNull(goals)

        val (reviewsTotal, reviews) = TenantDomainRepository.getReviewsPaginated("tenant-test", 10, 0)
        assertTrue(reviewsTotal >= 0L)
        assertNotNull(reviews)

        val (predTotal, preds) = TenantDomainRepository.getPredictionsPaginated("tenant-test", 10, 0)
        assertTrue(predTotal >= 0L)
        assertNotNull(preds)

        val (anomTotal, anoms) = TenantDomainRepository.getSecurityAnomaliesPaginated("tenant-test", 10, 0)
        assertTrue(anomTotal >= 0L)
        assertNotNull(anoms)

        val (dsrTotal, dsrs) = TenantDomainRepository.getDataSubjectRequestsPaginated("tenant-test", 10, 0)
        assertTrue(dsrTotal >= 0L)
        assertNotNull(dsrs)

        val (attTotal, atts) = TenantDomainRepository.getAttendanceAnomaliesPaginated("tenant-test", 10, 0)
        assertTrue(attTotal >= 0L)
        assertNotNull(atts)
    }

    @Test
    fun testSelectionPaginationWithOver25Items() = runBlocking {
        val supabase = SupabaseClientProvider.fromEnv()
        val selectionRepo = SelectionRepository(supabase)
        val testTenantId = "tenant-pag-test-${UUID.randomUUID().toString().take(8)}"

        // 1. Generate 30 selection requests (> 25 items)
        for (i in 1..30) {
            selectionRepo.createSelectionRequest(
                SelectionRequestRecord(
                    id = "sel-req-$testTenantId-$i",
                    tenant_id = testTenantId,
                    prompt_text = "Selection prompt $i",
                    status = "completed"
                )
            )
        }

        // 2. Call page 1 (limit 10, offset 0) and page 2 (limit 10, offset 10)
        val (totalRequests, page1Requests) = selectionRepo.listSelectionRequestsPaginated(testTenantId, limit = 10, offset = 0)
        val (_, page2Requests) = selectionRepo.listSelectionRequestsPaginated(testTenantId, limit = 10, offset = 10)

        assertEquals(30L, totalRequests, "Total requests should be 30")
        assertEquals(10, page1Requests.size, "Page 1 size should be 10")
        assertEquals(10, page2Requests.size, "Page 2 size should be 10")

        // 3. Prove NO overlap between page 1 and page 2
        val page1ReqIds = page1Requests.map { it.id }.toSet()
        val page2ReqIds = page2Requests.map { it.id }.toSet()
        val overlapReqs = page1ReqIds.intersect(page2ReqIds)
        assertTrue(overlapReqs.isEmpty(), "Page 1 and Page 2 requests must have zero overlap, found: $overlapReqs")

        // 4. Generate 30 selection results (> 25 items)
        val sampleReqId = "sel-req-$testTenantId-results"
        val sampleResults = (1..30).map { i ->
            SelectionResultRecord(
                id = "sel-res-$testTenantId-$i",
                selection_request_id = sampleReqId,
                total_score = 90.0 - i,
                rank_position = i,
                priority_level = if (i <= 5) "high" else "medium",
                recommendation_classification = "hire"
            )
        }
        selectionRepo.insertResults(sampleResults, testTenantId)

        // 5. Call results page 1 and page 2
        val (totalResults, page1Results) = selectionRepo.getSelectionResultsPaginated(sampleReqId, testTenantId, limit = 10, offset = 0)
        val (_, page2Results) = selectionRepo.getSelectionResultsPaginated(sampleReqId, testTenantId, limit = 10, offset = 10)

        assertEquals(30L, totalResults, "Total results should be 30")
        assertEquals(10, page1Results.size, "Page 1 size should be 10")
        assertEquals(10, page2Results.size, "Page 2 size should be 10")

        val page1ResIds = page1Results.map { it.id }.toSet()
        val page2ResIds = page2Results.map { it.id }.toSet()
        val overlapResults = page1ResIds.intersect(page2ResIds)
        assertTrue(overlapResults.isEmpty(), "Page 1 and Page 2 results must have zero overlap, found: $overlapResults")
    }

    @Test
    fun testChatHistoryPaginationAndDescendingSortWithOver25Items() = testApplication {
        application {
            module()
        }

        val convId = "conv-pag-test-${UUID.randomUUID().toString().take(8)}"
        val tenantId = "tenant-test"

        // Insert 30 messages (> 25 items) chronologically
        for (i in 1..30) {
            InMemoryConversationStore.recordExchange(
                convId = convId,
                tenantId = tenantId,
                senderId = "sender-$i",
                senderName = "Customer $i",
                userMessage = "Customer inquiry message #$i",
                aiReply = "AI agent reply #$i",
                intent = "INQUIRY"
            )
        }

        // 1. Fetch Page 1: limit 10, offset 0
        val resp1 = client.get("/api/v1/chat/history/$convId?limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer ${generateToken(tenantId)}")
        }
        assertEquals(HttpStatusCode.OK, resp1.status)
        val body1 = Json.parseToJsonElement(resp1.bodyAsText()).jsonObject
        val total1 = body1["total"]?.jsonPrimitive?.long ?: 0L
        val limit1 = body1["limit"]?.jsonPrimitive?.int ?: 0
        val offset1 = body1["offset"]?.jsonPrimitive?.int ?: -1
        val items1 = body1["items"]?.jsonArray ?: error("Missing items in response")

        // In InMemoryConversationStore.recordExchange, each exchange produces 2 messages (user + assistant), so 30 exchanges = 60 messages
        assertTrue(total1 >= 30L, "Total should be at least 30 messages, was $total1")
        assertEquals(10, limit1)
        assertEquals(0, offset1)
        assertEquals(10, items1.size)

        // 2. Fetch Page 2: limit 10, offset 10
        val resp2 = client.get("/api/v1/chat/history/$convId?limit=10&offset=10") {
            header(HttpHeaders.Authorization, "Bearer ${generateToken(tenantId)}")
        }
        assertEquals(HttpStatusCode.OK, resp2.status)
        val body2 = Json.parseToJsonElement(resp2.bodyAsText()).jsonObject
        val offset2 = body2["offset"]?.jsonPrimitive?.int ?: -1
        val items2 = body2["items"]?.jsonArray ?: error("Missing items in response")

        assertEquals(10, offset2)
        assertEquals(10, items2.size)

        // 3. Verify ZERO overlap between page 1 and page 2
        val page1Texts = items1.map { it.jsonObject["content"]?.jsonPrimitive?.content ?: "" }.toSet()
        val page2Texts = items2.map { it.jsonObject["content"]?.jsonPrimitive?.content ?: "" }.toSet()
        val overlapChat = page1Texts.intersect(page2Texts)
        assertTrue(overlapChat.isEmpty(), "Chat history page 1 and 2 must have zero overlap, found: $overlapChat")

        // 4. Verify DESCENDING sort: the first message in Page 1 (offset 0) must be from the newest exchange (#30)
        val newestContent = items1[0].jsonObject["content"]?.jsonPrimitive?.content ?: ""
        assertTrue(
            newestContent.contains("#30"),
            "First message in page 1 must be from the latest exchange (#30) for DESC sort, found: $newestContent"
        )
    }

    @Test
    fun testInboxConversationsPaginationAndFiltersWithOver25Items() = testApplication {
        application {
            module()
        }

        val testTenantId = "tenant-inbox-${UUID.randomUUID().toString().take(8)}"

        // Insert 30 conversations (> 25 items): 20 WHATSAPP, 10 TELEGRAM
        for (i in 1..20) {
            InMemoryConversationStore.recordExchange(
                convId = "conv-wa-$testTenantId-$i",
                tenantId = testTenantId,
                senderId = "wa-user-$i",
                senderName = "WA User $i",
                userMessage = "Halo WA $i",
                aiReply = "Balasan WA $i"
            )
        }
        for (i in 1..10) {
            InMemoryConversationStore.recordExchange(
                convId = "conv-tg-$testTenantId-$i",
                tenantId = testTenantId,
                senderId = "tg-user-$i",
                senderName = "TG User $i",
                userMessage = "Halo TG $i",
                aiReply = "Balasan TG $i"
            )
        }

        // 1. Fetch Page 1: limit 10, offset 0
        val resp1 = client.get("/api/v1/tenants/$testTenantId/inbox/conversations?limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer ${generateToken(testTenantId)}")
        }
        assertEquals(HttpStatusCode.OK, resp1.status)
        val body1 = Json.parseToJsonElement(resp1.bodyAsText()).jsonObject
        val total1 = body1["total"]?.jsonPrimitive?.long ?: 0L
        val limit1 = body1["limit"]?.jsonPrimitive?.int ?: 0
        val offset1 = body1["offset"]?.jsonPrimitive?.int ?: -1
        val items1 = body1["items"]?.jsonArray ?: error("Missing items")

        assertEquals(30L, total1, "Total conversations should be 30")
        assertEquals(10, limit1)
        assertEquals(0, offset1)
        assertEquals(10, items1.size)

        // 2. Fetch Page 2: limit 10, offset 10
        val resp2 = client.get("/api/v1/tenants/$testTenantId/inbox/conversations?limit=10&offset=10") {
            header(HttpHeaders.Authorization, "Bearer ${generateToken(testTenantId)}")
        }
        assertEquals(HttpStatusCode.OK, resp2.status)
        val body2 = Json.parseToJsonElement(resp2.bodyAsText()).jsonObject
        val offset2 = body2["offset"]?.jsonPrimitive?.int ?: -1
        val items2 = body2["items"]?.jsonArray ?: error("Missing items")

        assertEquals(10, offset2)
        assertEquals(10, items2.size)

        // 3. Verify ZERO overlap between page 1 and page 2
        val page1Ids = items1.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: "" }.toSet()
        val page2Ids = items2.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: "" }.toSet()
        val overlap = page1Ids.intersect(page2Ids)
        assertTrue(overlap.isEmpty(), "Inbox page 1 and page 2 must have zero overlap, found: $overlap")

        // 4. Verify Filter ?channel=WHATSAPP along with pagination
        val respFilter = client.get("/api/v1/tenants/$testTenantId/inbox/conversations?channel=WHATSAPP&limit=15&offset=0") {
            header(HttpHeaders.Authorization, "Bearer ${generateToken(testTenantId)}")
        }
        assertEquals(HttpStatusCode.OK, respFilter.status)
        val bodyFilter = Json.parseToJsonElement(respFilter.bodyAsText()).jsonObject
        val filterTotal = bodyFilter["total"]?.jsonPrimitive?.long ?: 0L
        val filterItems = bodyFilter["items"]?.jsonArray ?: error("Missing filter items")

        assertEquals(20L, filterTotal, "Filtered total for WHATSAPP should be 20")
        assertEquals(15, filterItems.size, "Should return 15 items for limit 15")
        filterItems.forEach { item ->
            val ch = item.jsonObject["channel"]?.jsonPrimitive?.content
            assertEquals("WHATSAPP", ch, "Filtered items must all have WHATSAPP channel")
        }
    }

    @Test
    fun testActivityStreamPaginationWithOver25Items() = testApplication {
        application {
            module()
        }

        val testTenantId = "tenant-stream-${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()

        // Generate 30 stream items with ascending timestamp so item 30 is newest
        val streamItems = (1..30).map { i ->
            ai.orchestree.backend.enterprise.EnterpriseActivityStreamItem(
                id = "act-$testTenantId-$i",
                sourceSystem = "SAP_ERP",
                summaryText = "Enterprise stream event #$i",
                occurredAt = now - (31 - i) * 1000L
            )
        }
        ai.orchestree.backend.enterprise.CompanyActivityStreamService.inMemoryStream[testTenantId] =
            java.util.concurrent.CopyOnWriteArrayList(streamItems.reversed()) // Newest first

        // 1. Fetch Page 1: limit 10, offset 0
        val resp1 = client.get("/api/v1/tenants/$testTenantId/activity-stream?limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer ${generateToken(testTenantId)}")
        }
        assertEquals(HttpStatusCode.OK, resp1.status)
        val body1 = Json.parseToJsonElement(resp1.bodyAsText()).jsonObject
        val total1 = body1["total"]?.jsonPrimitive?.long ?: 0L
        val limit1 = body1["limit"]?.jsonPrimitive?.int ?: 0
        val offset1 = body1["offset"]?.jsonPrimitive?.int ?: -1
        val items1 = body1["items"]?.jsonArray ?: error("Missing items")

        assertTrue(total1 >= 30L, "Total activity stream should be >= 30, got $total1")
        assertEquals(10, limit1)
        assertEquals(0, offset1)
        assertEquals(10, items1.size)

        // 2. Fetch Page 2: limit 10, offset 10
        val resp2 = client.get("/api/v1/tenants/$testTenantId/activity-stream?limit=10&offset=10") {
            header(HttpHeaders.Authorization, "Bearer ${generateToken(testTenantId)}")
        }
        assertEquals(HttpStatusCode.OK, resp2.status)
        val body2 = Json.parseToJsonElement(resp2.bodyAsText()).jsonObject
        val offset2 = body2["offset"]?.jsonPrimitive?.int ?: -1
        val items2 = body2["items"]?.jsonArray ?: error("Missing items")

        assertEquals(10, offset2)
        assertEquals(10, items2.size)

        // 3. Verify ZERO overlap between page 1 and page 2
        val page1Ids = items1.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: "" }.toSet()
        val page2Ids = items2.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: "" }.toSet()
        val overlap = page1Ids.intersect(page2Ids)
        assertTrue(overlap.isEmpty(), "Activity stream page 1 and page 2 must have zero overlap, found: $overlap")

        // 4. Verify descending sort (newest first): first item is #30
        val firstSummary = items1[0].jsonObject["summaryText"]?.jsonPrimitive?.content ?: ""
        assertTrue(firstSummary.contains("#30"), "First item in page 1 must be event #30, found: $firstSummary")
    }

    @Test
    fun testPresenceLogsPaginationWithOver25Items() = testApplication {
        application {
            module()
        }

        val testUserId = "user-pres-${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()

        // Generate 30 presence logs
        for (i in 1..30) {
            ai.orchestree.backend.security.PresenceService.defaultInstance.presenceCheckLogRepo.insertLog(
                ai.orchestree.backend.database.repositories.presence.PresenceCheckLog(
                    id = "log-$testUserId-$i",
                    userId = testUserId,
                    checkType = "PERIODIC",
                    methodUsed = "FACE_PASSIVE",
                    verificationResult = "VERIFIED",
                    deviceId = "device-$i",
                    ipAddress = "192.168.1.$i",
                    locationApprox = "Jakarta HQ",
                    checkedAt = now - (31 - i) * 1000L
                )
            )
        }

        // 1. Fetch Page 1: limit 10, offset 0
        val resp1 = client.get("/api/v1/presence/logs?userId=$testUserId&limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer ${generateToken("tenant-default", testUserId)}")
        }
        assertEquals(HttpStatusCode.OK, resp1.status)
        val body1 = Json.parseToJsonElement(resp1.bodyAsText()).jsonObject
        val total1 = body1["total"]?.jsonPrimitive?.long ?: 0L
        val limit1 = body1["limit"]?.jsonPrimitive?.int ?: 0
        val offset1 = body1["offset"]?.jsonPrimitive?.int ?: -1
        val items1 = body1["items"]?.jsonArray ?: error("Missing items")

        assertTrue(total1 >= 30L, "Total presence logs should be >= 30, got $total1")
        assertEquals(10, limit1)
        assertEquals(0, offset1)
        assertEquals(10, items1.size)

        // 2. Fetch Page 2: limit 10, offset 10
        val resp2 = client.get("/api/v1/presence/logs?userId=$testUserId&limit=10&offset=10") {
            header(HttpHeaders.Authorization, "Bearer ${generateToken("tenant-default", testUserId)}")
        }
        assertEquals(HttpStatusCode.OK, resp2.status)
        val body2 = Json.parseToJsonElement(resp2.bodyAsText()).jsonObject
        val offset2 = body2["offset"]?.jsonPrimitive?.int ?: -1
        val items2 = body2["items"]?.jsonArray ?: error("Missing items")

        assertEquals(10, offset2)
        assertEquals(10, items2.size)

        // 3. Verify ZERO overlap between page 1 and page 2
        val page1Ids = items1.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: "" }.toSet()
        val page2Ids = items2.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: "" }.toSet()
        val overlap = page1Ids.intersect(page2Ids)
        assertTrue(overlap.isEmpty(), "Presence logs page 1 and page 2 must have zero overlap, found: $overlap")

        // 4. Verify descending sort (newest first): first item is log #30
        val firstId = items1[0].jsonObject["id"]?.jsonPrimitive?.content ?: ""
        assertTrue(firstId.endsWith("-30"), "First item in page 1 must be log #30, found: $firstId")
    }

    @Test
    fun testAiEventsPaginationWithOver25Items() = testApplication {
        application {
            module()
        }

        val testTenantId = "tenant-events-${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()

        for (i in 1..30) {
            ai.orchestree.backend.events.AiEventEngine.defaultInstance.eventInstancesStore["evt-$testTenantId-$i"] =
                ai.orchestree.backend.events.AiEventInstance(
                    id = "evt-$testTenantId-$i",
                    tenantId = testTenantId,
                    eventCode = "STOCK_LOW",
                    entityReference = "SKU-$i",
                    sourceSystem = "ERP",
                    status = "NEW",
                    severity = "HIGH",
                    triggeredAt = now - (31 - i) * 1000L
                )
        }

        val resp1 = client.get("/api/v1/tenants/$testTenantId/events?limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer ${generateToken(testTenantId)}")
        }
        assertEquals(HttpStatusCode.OK, resp1.status)
        val body1 = Json.parseToJsonElement(resp1.bodyAsText()).jsonObject
        val total1 = body1["total"]?.jsonPrimitive?.long ?: 0L
        val items1 = body1["items"]?.jsonArray ?: error("Missing items")

        assertTrue(total1 >= 30L)
        assertEquals(10, items1.size)

        val resp2 = client.get("/api/v1/tenants/$testTenantId/events?limit=10&offset=10") {
            header(HttpHeaders.Authorization, "Bearer ${generateToken(testTenantId)}")
        }
        assertEquals(HttpStatusCode.OK, resp2.status)
        val body2 = Json.parseToJsonElement(resp2.bodyAsText()).jsonObject
        val items2 = body2["items"]?.jsonArray ?: error("Missing items")

        assertEquals(10, items2.size)

        val page1Ids = items1.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: "" }.toSet()
        val page2Ids = items2.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: "" }.toSet()
        val overlap = page1Ids.intersect(page2Ids)
        assertTrue(overlap.isEmpty(), "Events page 1 and page 2 must have zero overlap")
    }

    @Test
    fun testMemoryDocumentsPaginationWithOver25Items() = testApplication {
        application {
            module()
        }

        val testTenantId = "tenant-mem-${UUID.randomUUID().toString().take(8)}"
        val memoryRepo = ai.orchestree.backend.database.repositories.memory.MemoryDocumentRepository()

        for (i in 1..30) {
            memoryRepo.inMemoryStore.computeIfAbsent(testTenantId) { java.util.concurrent.CopyOnWriteArrayList() }
                .add(
                    ai.orchestree.backend.database.repositories.memory.MemoryDocumentRecord(
                        id = "mem-doc-$testTenantId-$i",
                        tenantId = testTenantId,
                        sourceType = "episodic",
                        title = "Document Title $i",
                        content = "Document content $i"
                    )
                )
        }

        val (total, page1) = memoryRepo.listByTenantPaginated(testTenantId, limit = 10, offset = 0)
        val (_, page2) = memoryRepo.listByTenantPaginated(testTenantId, limit = 10, offset = 10)

        assertEquals(30L, total)
        assertEquals(10, page1.size)
        assertEquals(10, page2.size)

        val page1Ids = page1.map { it.id }.toSet()
        val page2Ids = page2.map { it.id }.toSet()
        assertTrue(page1Ids.intersect(page2Ids).isEmpty())
    }
}


