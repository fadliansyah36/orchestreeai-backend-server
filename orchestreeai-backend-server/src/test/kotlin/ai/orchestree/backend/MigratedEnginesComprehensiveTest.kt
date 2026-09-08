package ai.orchestree.backend

import ai.orchestree.backend.cost.SemanticResponseCacheManager
import ai.orchestree.backend.deployment.UnifiedPilotAndGaEngine
import ai.orchestree.backend.generativestudio.CsvDocumentRenderer
import ai.orchestree.backend.generativestudio.DimensionExtractor
import ai.orchestree.backend.generativestudio.DocxDocumentRenderer
import ai.orchestree.backend.generativestudio.PdfDocumentRenderer
import ai.orchestree.backend.generativestudio.PptxDocumentRenderer
import ai.orchestree.backend.generativestudio.PromptComposer
import ai.orchestree.backend.generativestudio.XlsxDocumentRenderer
import ai.orchestree.backend.intelligence.CompetitorDiffEngine
import ai.orchestree.backend.learning.ContinuousLearningCore
import ai.orchestree.backend.sales.CourierTrackingEngine
import ai.orchestree.backend.sales.GroundingOutputValidator
import ai.orchestree.backend.sales.LeadQualificationEngine
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class MigratedEnginesComprehensiveTest {

    @Test
    fun testLeadQualificationBantScoring() {
        val (score, stage) = LeadQualificationEngine.calculateBantScore(
            budget = 60_000_000.0,
            hasDecisionMaker = true,
            explicitNeedIdentified = true,
            purchaseDays = 7,
            companyFitTier = "ENTERPRISE",
            engagementVelocity = 1.0
        )
        println("[TEST LOG] BANT Result: score=$score, stage=$stage")
        assertTrue(score >= 80)
        assertEquals("QUALIFIED_OPPORTUNITY", stage)
    }

    @Test
    fun testCourierTrackingEngine() = runBlocking {
        val tracking = CourierTrackingEngine.trackWaybill("JNE", "SOCAG001234567")
        println("[TEST LOG] Courier tracking: status=${tracking.status}, events=${tracking.history.size}")
        assertEquals("IN_TRANSIT", tracking.status)
        assertEquals("JNE", tracking.courierCode)
        assertTrue(tracking.history.isNotEmpty())
    }

    @Test
    fun testGroundingOutputValidator() = runBlocking {
        // Test valid statement
        val validCheck = GroundingOutputValidator.validateCommerceResponse(
            tenantId = "tenant-test-01",
            aiGeneratedMessage = "Produk PROD-001 tersedia dengan diskon 10%."
        )
        println("[TEST LOG] Valid Grounding Check: isValid=${validCheck.isValid}")
        assertTrue(validCheck.isValid)

        // Test discount violation (> 15%)
        val invalidDiscount = GroundingOutputValidator.validateCommerceResponse(
            tenantId = "tenant-test-01",
            aiGeneratedMessage = "Kami berikan potongan 50% spesial untuk Anda hari ini!"
        )
        println("[TEST LOG] Invalid Discount Check: isValid=${invalidDiscount.isValid}, violations=${invalidDiscount.violations}")
        assertFalse(invalidDiscount.isValid)
        assertTrue(invalidDiscount.violations.any { it.contains("UNAUTHORIZED_DISCOUNT_PROMISE") })
    }

    @Test
    fun testContinuousLearningCore() {
        val reinforce = ContinuousLearningCore.classifyOutcome(
            outcomeSource = "PAYMENT_WEBHOOK",
            isSuccess = true
        )
        println("[TEST LOG] Continuous Learning: classification=${reinforce.classification}, delta=${reinforce.confidenceDelta}")
        assertEquals("REINFORCE", reinforce.classification)
        assertTrue(reinforce.confidenceDelta > 0)

        val reject = ContinuousLearningCore.classifyOutcome(
            outcomeSource = "HUMAN_EXPLICIT_REJECT",
            isSuccess = false
        )
        assertEquals("LEARN_FROM_REJECTION", reject.classification)
        assertTrue(reject.confidenceDelta < 0)
    }

    @Test
    fun testCompetitorDiffEngine() {
        val diffs = CompetitorDiffEngine.computePriceDeltas(
            sku = "SKU-COFFEE-01",
            internalPrice = 120000.0,
            competitorPrices = mapOf(
                "Competitor A" to 100000.0,
                "Competitor B" to 140000.0
            )
        )
        println("[TEST LOG] Competitor Diffs: count=${diffs.size}")
        assertEquals(2, diffs.size)
        val compA = diffs.first { it.competitorName == "Competitor A" }
        assertEquals(20000.0, compA.differenceAmount, 0.01)
        assertTrue(compA.recommendation.contains("lebih mahal"))
    }

    @Test
    fun testDocumentRenderers() {
        // PDF Renderer
        val pdfBytes = PdfDocumentRenderer.renderDocument(
            title = "Laporan Penjualan Q3",
            author = "Orchestree AI Chief of Staff",
            contentParagraphs = listOf("Total revenue meningkat 34% YoY.", "Gross margin terjaga di level 42%.")
        )
        val pdfHeader = String(pdfBytes.take(8).toByteArray(), StandardCharsets.ISO_8859_1)
        println("[TEST LOG] PDF Bytes: size=${pdfBytes.size}, header=$pdfHeader")
        assertTrue(pdfHeader.startsWith("%PDF-1."))

        // DOCX Renderer (ZIP file starting with PK\x03\x04)
        val docxBytes = DocxDocumentRenderer.renderDocument(
            title = "SOP Layanan Pelanggan",
            paragraphs = listOf("1. Sambut pelanggan dalam 30 detik.", "2. Identifikasi kebutuhan spesifik.")
        )
        println("[TEST LOG] DOCX Bytes: size=${docxBytes.size}")
        assertTrue(docxBytes.size > 100)
        assertEquals(0x50.toByte(), docxBytes[0]) // 'P'
        assertEquals(0x4B.toByte(), docxBytes[1]) // 'K'

        // XLSX Renderer
        val xlsxBytes = XlsxDocumentRenderer.renderSpreadsheet(
            sheetName = "Revenue",
            headers = listOf("Month", "Revenue", "Target"),
            rows = listOf(listOf("Januari", "100000000", "90000000"), listOf("Februari", "120000000", "110000000"))
        )
        println("[TEST LOG] XLSX Bytes: size=${xlsxBytes.size}")
        assertTrue(xlsxBytes.size > 100)
        assertEquals(0x50.toByte(), xlsxBytes[0]) // 'P'
        assertEquals(0x4B.toByte(), xlsxBytes[1]) // 'K'

        // PPTX Renderer
        val pptxBytes = PptxDocumentRenderer.renderPresentation(
            title = "Pitch Deck Eksekutif",
            slides = listOf("Slide 1" to listOf("Poin A", "Poin B"))
        )
        println("[TEST LOG] PPTX Bytes: size=${pptxBytes.size}")
        assertTrue(pptxBytes.size > 100)

        // CSV Renderer (RFC 4180 compliant escaping)
        val csvBytes = CsvDocumentRenderer.renderCsv(
            headers = listOf("Nama, Lengkap", "Catatan"),
            rows = listOf(listOf("Budi, \"Santoso\"", "Line 1\nLine 2"))
        )
        val csvStr = String(csvBytes, StandardCharsets.UTF_8)
        println("[TEST LOG] CSV Output: $csvStr")
        assertTrue(csvStr.contains("\"Nama, Lengkap\""))
        assertTrue(csvStr.contains("\"Budi, \"\"Santoso\"\"\""))
    }

    @Test
    fun testGenerativeStudioSpecs() {
        val dim = DimensionExtractor.getStandardDimensions("INSTAGRAM_STORY")
        println("[TEST LOG] Instagram Story: ${dim.width}x${dim.height} (${dim.aspectRatio})")
        assertEquals(1080, dim.width)
        assertEquals(1920, dim.height)
        assertEquals("9:16", dim.aspectRatio)

        val prompt = PromptComposer.composeGroundedPrompt(
            productName = "Sepatu Lari Trail",
            targetAudience = "Pelari Marathon",
            visualTheme = "Outdoor Sunrise Adventure"
        )
        println("[TEST LOG] Composed Prompt: ${prompt.mainPrompt}")
        assertTrue(prompt.mainPrompt.contains("Sepatu Lari Trail"))
        assertTrue(prompt.negativePrompt.contains("blurry"))
    }

    @Test
    fun testCanaryHealthGate() {
        // Healthy canary: 10% -> 25%
        val healthy = UnifiedPilotAndGaEngine.evaluateCanaryHealth(
            currentTrafficPct = 10,
            sampleRequestsCount = 1000,
            failedRequestsCount = 2, // 0.2% error rate
            p99LatencyMs = 250
        )
        println("[TEST LOG] Canary Healthy: nextPct=${healthy.recommendedNextPercentage}, rollback=${healthy.rollbackTriggered}")
        assertTrue(healthy.allowProgression)
        assertEquals(25, healthy.recommendedNextPercentage)
        assertFalse(healthy.rollbackTriggered)

        // Degraded canary: error rate 5% -> Rollback triggered
        val degraded = UnifiedPilotAndGaEngine.evaluateCanaryHealth(
            currentTrafficPct = 25,
            sampleRequestsCount = 1000,
            failedRequestsCount = 50, // 5.0% error rate
            p99LatencyMs = 4500
        )
        println("[TEST LOG] Canary Degraded: rollback=${degraded.rollbackTriggered}")
        assertFalse(degraded.allowProgression)
        assertTrue(degraded.rollbackTriggered)
        assertEquals(0, degraded.recommendedNextPercentage)
    }

    @Test
    fun testSemanticCacheHash() {
        val h1 = SemanticResponseCacheManager.hashPrompt("Berapa harga paket premium?")
        val h2 = SemanticResponseCacheManager.hashPrompt("   berapa harga paket premium?   ")
        println("[TEST LOG] Semantic Cache Hash 1: $h1")
        println("[TEST LOG] Semantic Cache Hash 2: $h2")
        assertEquals(h1, h2)
    }
}
