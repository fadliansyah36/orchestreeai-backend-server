package ai.orchestree.backend

import ai.orchestree.backend.api.ReviewSelectionResultRequest
import ai.orchestree.backend.billing.CentralCreditLedgerService
import ai.orchestree.backend.billing.CreditEstimator
import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.database.repositories.selection.SelectionCriterionRecord
import ai.orchestree.backend.database.repositories.selection.SelectionRepository
import ai.orchestree.backend.database.repositories.selection.SelectionResultRecord
import ai.orchestree.backend.generativestudio.DeterministicRenderer
import ai.orchestree.backend.intelligence.DataRow
import ai.orchestree.backend.intelligence.OutputValidator
import ai.orchestree.backend.intelligence.RankedResult
import ai.orchestree.backend.intelligence.ScoringResult
import ai.orchestree.backend.intelligence.WeightedCriterion
import ai.orchestree.backend.orchestration.NodeExecutionResult
import ai.orchestree.backend.orchestration.NodeExecutionStatus
import ai.orchestree.backend.orchestration.NodeResult
import ai.orchestree.backend.orchestration.WorkflowNodeType
import ai.orchestree.backend.orchestration.nodes.GenericStepWorkflowNode
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SelectionBagianGComprehensiveTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val json = Json { ignoreUnknownKeys = true }

    private fun generateAdminToken(tenantId: String = "tenant-enterprise-001"): String {
        return JWT.create()
            .withSubject("usr-admin-01")
            .withClaim("sub", "usr-admin-01")
            .withClaim("user_id", "usr-admin-01")
            .withClaim("tenant_id", tenantId)
            .withClaim("role", "TENANT_ADMIN")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600 * 1000))
            .sign(algorithm)
    }

    /**
     * LANGKAH 1: AI Insight Composer Grounding & Anti-Halusinasi
     */
    @Test
    fun testGroundedInsightGenerationAndHallucinationDetection() {
        val validator = OutputValidator()

        val sourceData = mapOf<String, Any?>(
            "Nama" to "Budi Santoso",
            "Pengalaman Kerja" to "5 tahun",
            "Nilai Tes Teknis" to 88.5,
            "total_score" to 85.0,
            "rank_position" to 1,
            "risk_score" to 12.0,
            "confidence_score" to 0.95
        )

        // 1. Grounded statement: strictly facts present in sourceData
        val groundedText = "Budi Santoso memperoleh total skor 85.0 dan berada di posisi #1 dengan nilai teknis 88.5."
        val validationGrounded = validator.validateGrounding(groundedText, sourceData)
        assertTrue(validationGrounded.isGrounded, "Statement containing real attributes should be verified as grounded")
        assertEquals(0, validationGrounded.ungroundedClaims.size)

        // 2. Hallucinated statement: claims 15 years experience and Harvard graduation not in source
        val hallucinatedText = "Budi Santoso lulusan Harvard dengan 15 tahun pengalaman memimpin Google telah disetujui."
        val validationHallucinated = validator.validateGrounding(hallucinatedText, sourceData)
        assertFalse(validationHallucinated.isGrounded, "Fabricated claims must be detected as hallucinations")
        assertTrue(validationHallucinated.ungroundedClaims.isNotEmpty())

        // 3. Fallback Sanitization / Anchoring
        val anchored = validator.sanitizeOrAnchorInsight(
            originalInsight = hallucinatedText,
            sourceData = sourceData,
            candidateName = "Budi Santoso",
            rankPosition = 1,
            totalScore = 85.0
        )
        assertTrue(anchored.contains("Budi Santoso"), "Anchored insight must cite candidate name")
        assertTrue(anchored.contains("#1"), "Anchored insight must cite true rank")
        assertTrue(anchored.contains("85.0"), "Anchored insight must cite true total score")
        assertFalse(anchored.contains("Harvard"), "Sanitized text must strip unverified hallucinated institutions")
    }

    /**
     * LANGKAH 2: Human Review, Approval Gate & Downstream Execution
     */
    @Test
    fun testHumanReviewAndDownstreamExecutionGate() = runBlocking {
        val repo = SelectionRepository()
        val tenantId = "tenant-test-review-01"

        // Mock a selection result
        val dummyResult = SelectionResultRecord(
            id = "res-test-hr-001",
            selection_request_id = "req-test-hr",
            rank_position = 1,
            total_score = 92.5,
            recommendation_classification = "selected",
            human_review_status = "pending"
        )
        repo.insertResults(listOf(dummyResult), tenantId)

        // Rule: AI recommendation="selected" CANNOT execute downstream without human approval
        assertFalse(repo.canExecuteDownstreamAction(dummyResult), "Pending status must be blocked from downstream auto-execution")

        val rejectedResult = dummyResult.copy(human_review_status = "rejected")
        assertFalse(repo.canExecuteDownstreamAction(rejectedResult), "Rejected status must be blocked from downstream auto-execution")

        val approvedResult = dummyResult.copy(human_review_status = "approved")
        assertTrue(repo.canExecuteDownstreamAction(approvedResult), "Approved status is permitted for downstream execution")

        // Test update review
        val updateRes = repo.updateResultReview(
            resultId = "res-test-hr-001",
            tenantId = tenantId,
            action = "approve",
            reviewerId = "usr-manager-hr",
            notes = "Kualifikasi sesuai standar divisi engineering."
        )
        assertTrue(updateRes.isSuccess)
        val updatedRecord = updateRes.getOrThrow()
        assertEquals("approved", updatedRecord.human_review_status)
        assertEquals("usr-manager-hr", updatedRecord.human_reviewer_id)
        assertNotNull(updatedRecord.human_reviewed_at)
        assertEquals("Kualifikasi sesuai standar divisi engineering.", updatedRecord.human_review_notes)
    }

    /**
     * LANGKAH 3: Deterministic Export Renderers (CSV, XLSX, PDF)
     */
    @Test
    fun testDeterministicExportRenderers() {
        val renderer = DeterministicRenderer()
        val reqId = "req-export-eval-101"

        val criteria = listOf(
            SelectionCriterionRecord(
                id = "crit-1",
                selection_request_id = reqId,
                criterion_name = "Keahlian Teknis",
                criterion_source = "user_prompt",
                weight_percentage = 60.0
            ),
            SelectionCriterionRecord(
                id = "crit-2",
                selection_request_id = reqId,
                criterion_name = "Pengalaman Kerja",
                criterion_source = "user_prompt",
                weight_percentage = 40.0
            )
        )

        val breakdown = JsonObject(
            mapOf(
                "Keahlian Teknis" to JsonPrimitive(54.0),
                "Pengalaman Kerja" to JsonPrimitive(36.0)
            )
        )

        val results = listOf(
            SelectionResultRecord(
                id = "res-001",
                selection_request_id = reqId,
                rank_position = 1,
                total_score = 90.0,
                recommendation_classification = "selected",
                priority_level = "high",
                risk_score = 10.0,
                confidence_score = 0.95,
                score_breakdown = breakdown,
                human_review_status = "approved",
                human_reviewer_id = "usr-lead",
                ai_insight_text = "Kandidat unggul dalam pengujian teknis dan rekam jejak arsitektur."
            ),
            SelectionResultRecord(
                id = "res-002",
                selection_request_id = reqId,
                rank_position = 2,
                total_score = 72.0,
                recommendation_classification = "review",
                priority_level = "medium",
                risk_score = 25.0,
                confidence_score = 0.80,
                score_breakdown = breakdown,
                human_review_status = "pending",
                ai_insight_text = "Memerlukan peninjauan portofolio lanjutan."
            )
        )

        // 1. CSV Rendering
        val csv = renderer.renderSelectionCsv(reqId, results, criteria)
        assertTrue(csv.contains("Rank,Result ID,Total Score,Classification"), "CSV must contain standard headers")
        assertTrue(csv.contains("res-001,90.00,selected,high"), "CSV must contain formatted candidate 1 data")
        assertTrue(csv.contains("res-002,72.00,review,medium"), "CSV must contain formatted candidate 2 data")

        // 2. XLSX Rendering (OpenXML ZIP container)
        val xlsxBytes = renderer.renderSelectionXlsx(reqId, results, criteria)
        assertTrue(xlsxBytes.isNotEmpty(), "XLSX output must not be empty")
        // Verify ZIP integrity & required parts
        val zis = ZipInputStream(ByteArrayInputStream(xlsxBytes))
        val entryNames = mutableListOf<String>()
        var entry = zis.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            entry = zis.nextEntry
        }
        assertTrue(entryNames.contains("[Content_Types].xml"), "XLSX must have Content_Types")
        assertTrue(entryNames.contains("xl/workbook.xml"), "XLSX must have workbook.xml")
        assertTrue(entryNames.contains("xl/worksheets/sheet1.xml"), "XLSX must have sheet1.xml")

        // 3. PDF Rendering (PDF-1.4 spec)
        val pdfBytes = renderer.renderSelectionPdf(reqId, results, criteria)
        assertTrue(pdfBytes.isNotEmpty(), "PDF output must not be empty")
        val pdfString = String(pdfBytes, StandardCharsets.ISO_8859_1)
        assertTrue(pdfString.startsWith("%PDF-1.4"), "PDF must start with %PDF-1.4 header")
        assertTrue(pdfString.contains("%%EOF"), "PDF must terminate with %%EOF marker")
        assertTrue(pdfString.contains("OrchestreeAI - Selection & Ranking Report"), "PDF must contain document title")
    }

    /**
     * LANGKAH 4: Unified AI Credit System (Fase 113 Central Credit Ledger)
     */
    @Test
    fun testUnifiedAiCentralCreditLedger() = runBlocking {
        val ledger = CentralCreditLedgerService.getInstance()
        val tenantId = "tenant-credit-test-${System.currentTimeMillis()}"

        // Initial balance
        val initialSummary = ledger.getTenantCreditSummary(tenantId)
        assertEquals(12500.0, initialSummary.balance_credits, 0.001)

        // Test Estimator
        val estimator = CreditEstimator()
        val dummyNode = GenericStepWorkflowNode("test-score-node", WorkflowNodeType.LLM_GENERATE) { ctx ->
            ctx["executed"] = true
            NodeExecutionResult(NodeExecutionStatus.SUCCESS, "Scoring complete", data = mapOf("tokensUsed" to 1200))
        }
        val estCost = estimator.estimate(dummyNode)
        assertEquals(25.0, estCost, 0.001)

        val context = mutableMapOf<String, Any>()
        val result = ledger.executeSelectionNodeWithCredit(dummyNode, tenantId, context)

        assertTrue(result is NodeResult.Success, "Node execution with credit ledger must succeed")
        assertTrue(context["executed"] == true)

        // Check balances post-execution
        val updatedSummary = ledger.getTenantCreditSummary(tenantId)
        assertTrue(updatedSummary.balance_credits < 12500.0, "Available balance must be deducted after actual consumption")
        assertEquals(0.0, updatedSummary.reserved_credits, 0.001, "Reserved credits must be released back to 0")
        assertEquals(0, ledger.getActiveReservationCount(), "Active reservations map must be cleared")

        // Verify ledger entries recorded
        val history = ledger.getLedgerHistory(tenantId)
        assertTrue(history.isNotEmpty(), "Ledger history must contain audit records")
        assertTrue(history.any { it.entryType == "CONSUMPTION" })
        assertTrue(history.any { it.entryType == "RELEASE" })
    }

    /**
     * End-to-End Selection HTTP Routes Integration (Review, Execute Downstream, Export)
     */
    @Test
    fun testSelectionRoutesEndToEnd() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val testRepo = SelectionRepository()
        testRepo.insertResults(listOf(
            SelectionResultRecord(
                id = "res-test-e2e-001",
                selection_request_id = "req-sample-01",
                rank_position = 1,
                total_score = 88.0,
                recommendation_classification = "selected",
                human_review_status = "pending"
            ),
            SelectionResultRecord(
                id = "res-test-e2e-002",
                selection_request_id = "req-sample-01",
                rank_position = 2,
                total_score = 65.0,
                recommendation_classification = "review",
                human_review_status = "pending"
            )
        ), "tenant-enterprise-001")

        val client = createClient {}
        val token = generateAdminToken()

        // 1. Billing Summary API
        val billingSummary = client.get("/api/v1/billing/credits/summary") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, billingSummary.status)
        assertTrue(billingSummary.bodyAsText().contains("available_balance"))

        // 2. Billing History API
        val billingHistory = client.get("/api/v1/billing/credits/ledger") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, billingHistory.status)

        // 3. Human Review Endpoint: Approve
        val reviewReq = ReviewSelectionResultRequest(
            action = "approve",
            notes = "Candidate verified by Engineering VP",
            overrideClassification = "selected"
        )
        val reviewResp = client.post("/api/v1/selection/results/res-test-e2e-001/review") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(reviewReq))
        }
        assertEquals(HttpStatusCode.OK, reviewResp.status)
        val reviewBody = reviewResp.bodyAsText()
        assertTrue(reviewBody.contains("approved"))
        assertTrue(reviewBody.contains("Candidate verified by Engineering VP"))

        // 4. Downstream Execution: When Approved -> Success
        val downstreamApproved = client.post("/api/v1/selection/results/res-test-e2e-001/execute-downstream") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, downstreamApproved.status)
        assertTrue(downstreamApproved.bodyAsText().contains("downstream_action_approved_and_executed"))

        // 5. Downstream Execution: When Rejected or Pending -> Precondition Failed
        val rejectReq = ReviewSelectionResultRequest(
            action = "reject",
            notes = "Does not meet minimum requirements"
        )
        client.post("/api/v1/selection/results/res-test-e2e-002/review") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(rejectReq))
        }
        val downstreamRejected = client.post("/api/v1/selection/results/res-test-e2e-002/execute-downstream") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.PreconditionFailed, downstreamRejected.status)
        assertTrue(downstreamRejected.bodyAsText().contains("Downstream execution prohibited"))

        // 6. Export Endpoints (CSV, XLSX, PDF)
        val exportCsv = client.get("/api/v1/selection/req-sample-01/export?format=csv") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, exportCsv.status)
        assertTrue(exportCsv.bodyAsText().contains("Rank,Result ID"))

        val exportXlsx = client.get("/api/v1/selection/req-sample-01/export?format=xlsx") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, exportXlsx.status)
        assertTrue(exportXlsx.bodyAsBytes().isNotEmpty())

        val exportPdf = client.get("/api/v1/selection/req-sample-01/export?format=pdf") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, exportPdf.status)
        val pdfContent = String(exportPdf.bodyAsBytes(), StandardCharsets.ISO_8859_1)
        assertTrue(pdfContent.startsWith("%PDF-1.4"))
    }
}
