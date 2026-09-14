package ai.orchestree.backend

import ai.orchestree.backend.api.AdminDomainStores
import ai.orchestree.backend.api.AdminMcpToolItem
import ai.orchestree.backend.billing.CentralCreditLedgerService
import ai.orchestree.backend.billing.CreditReservation
import ai.orchestree.backend.channels.adapters.InstagramAdapter
import ai.orchestree.backend.channels.adapters.TikTokAdapter
import ai.orchestree.backend.channels.adapters.WhatsAppAdapter
import ai.orchestree.backend.generativestudio.ContentPlanningLayer
import ai.orchestree.backend.generativestudio.MetadataStripper
import ai.orchestree.backend.mcptools.McpGovernanceEngine
import ai.orchestree.backend.mcptools.McpRiskLevel
import ai.orchestree.backend.mcptools.McpToolDefinition
import ai.orchestree.backend.mcptools.McpToolExecutor
import ai.orchestree.backend.orchestration.NodeExecutionStatus
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.orchestration.WorkflowContext
import ai.orchestree.backend.orchestration.nodes.GenericStepWorkflowNode
import ai.orchestree.backend.security.GooglePlayIntegrityClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class Phase2To5EndToEndStabilizationTest {

    @BeforeEach
    fun setup() {
        System.setProperty("APPLICATION_ENV", "test")
    }

    // =========================================================================
    // 1. MCP Tool Executor & Kill-Switch Governance
    // =========================================================================
    @Test
    fun testMcpKillSwitchGovernanceEnforcement() = runBlocking {
        val governance = McpGovernanceEngine()
        val tool = McpToolDefinition(
            name = "payment_refund_transaction",
            description = "Processes refunds",
            inputSchema = "{}",
            riskLevel = McpRiskLevel.CRITICAL
        )

        // Verify that critical tool is blocked for regular staff
        val (allowedStaff, reasonStaff) = governance.evaluateGovernance(tool, "tenant-test", "STAFF_HUMAN", emptyMap())
        assertFalse(allowedStaff)
        assertTrue(reasonStaff?.contains("requires TENANT_ADMIN") == true)

        // Verify that critical tool is permitted for Super Admin when active
        val (allowedAdmin, _) = governance.evaluateGovernance(tool, "tenant-test", "SUPER_ADMIN", emptyMap())
        assertTrue(allowedAdmin)

        // Register in AdminDomainStores as kill-switched and verify immediate blocking
        val storeTool = AdminMcpToolItem(
            id = "payment_refund_transaction",
            name = "payment_refund_transaction",
            description = "Refund tool",
            riskLevel = "CRITICAL",
            status = "DISABLED_BY_KILLSWITCH"
        )
        AdminDomainStores.mcpTools.removeIf { it.id == storeTool.id || it.name == storeTool.name }
        AdminDomainStores.mcpTools.add(storeTool)

        val executor = McpToolExecutor(governance = governance)
        val result = executor.executeTool(
            toolName = "payment_refund_transaction",
            params = mapOf("trxId" to "trx-123", "amount" to 50000.0),
            tenantId = "tenant-test",
            callerRole = "SUPER_ADMIN"
        )
        assertFalse(result.success)
        assertTrue(result.isBlockedByGovernance)
        assertTrue(result.errorMessage?.contains("disabled by emergency kill-switch") == true)

        // Clean up
        AdminDomainStores.mcpTools.removeIf { it.id == storeTool.id }
    }

    // =========================================================================
    // 2. Google Play Integrity Verification (Dev tokens & JWT parsing)
    // =========================================================================
    @Test
    fun testGooglePlayIntegrityTokenVerification() {
        val client = GooglePlayIntegrityClient()

        // 1. Dev token check
        val devResponse = client.decodeIntegrityToken("pit_test_token_sample_1234567890")
        assertEquals("PLAY_RECOGNIZED", devResponse.appIntegrity.appRecognitionVerdict)
        assertTrue(devResponse.deviceIntegrity.deviceRecognitionVerdict.contains("MEETS_DEVICE_INTEGRITY"))

        // 2. JWT token decoding
        val jwtHeader = java.util.Base64.getUrlEncoder().encodeToString("""{"alg":"none"}""".toByteArray())
        val jwtPayload = java.util.Base64.getUrlEncoder().encodeToString(
            """{"appIntegrity":{"appRecognitionVerdict":"PLAY_RECOGNIZED"},"deviceIntegrity":{"deviceRecognitionVerdict":["MEETS_DEVICE_INTEGRITY"]}}""".toByteArray()
        )
        val simulatedJwt = "$jwtHeader.$jwtPayload."

        val jwtResponse = client.decodeIntegrityToken(simulatedJwt)
        assertEquals("PLAY_RECOGNIZED", jwtResponse.appIntegrity.appRecognitionVerdict)
        assertTrue(jwtResponse.deviceIntegrity.deviceRecognitionVerdict.contains("MEETS_DEVICE_INTEGRITY"))

        // 3. Invalid blank token
        assertThrows(IllegalArgumentException::class.java) {
            client.decodeIntegrityToken("")
        }
    }

    // =========================================================================
    // 3. OrchestrationEngine Distribution Node (Sandbox & Dispatch)
    // =========================================================================
    @Test
    fun testOrchestrationDistributionNode() = runBlocking {
        val engine = OrchestrationEngine()

        // Test with sandbox mode enabled
        val sandboxCtx = WorkflowContext()
        sandboxCtx["is_sandbox_replay"] = true
        sandboxCtx["finalOutput"] = "Operational performance report"

        val sandboxNode = engine.getNode("n5-distribute-channels")
        assertNotNull(sandboxNode)
        val sandboxRes = sandboxNode!!.execute(sandboxCtx)
        assertEquals(NodeExecutionStatus.SUCCESS, sandboxRes.status)
        assertTrue(sandboxRes.output?.contains("[SANDBOX_REPLAY]") == true)

        // Test normal delivery flow
        val liveCtx = WorkflowContext()
        liveCtx["is_sandbox_replay"] = false
        liveCtx["tenant_id"] = "tenant-unit-test"
        liveCtx["finalOutput"] = "Q3 Revenue Target reached. Operations nominal."

        val liveRes = sandboxNode.execute(liveCtx)
        assertEquals(NodeExecutionStatus.SUCCESS, liveRes.status)
        assertTrue(liveRes.output?.contains("Delivered to Slack #exec-leadership") == true)
    }

    // =========================================================================
    // 4. ContentPlanningLayer & ModelRouter Integration
    // =========================================================================
    @Test
    fun testContentPlanningLayerPlanGeneration() = runBlocking {
        val planningLayer = ContentPlanningLayer()
        val plan = planningLayer.generateContentPlan(
            topic = "Automated Inventory Forecasting",
            platform = "LinkedIn",
            tone = "executive"
        )

        assertNotNull(plan)
        assertTrue(plan.title.isNotBlank())
        assertTrue(plan.caption.isNotBlank())
        assertTrue(plan.visualPrompt.isNotBlank())
        assertEquals("LinkedIn", plan.targetPlatform)
        assertTrue(plan.hashtags.isNotEmpty())
    }

    // =========================================================================
    // 5. MetadataStripper (EXIF, PNG chunks, SVG sanitize)
    // =========================================================================
    @Test
    fun testMetadataStripperImageSanitization() {
        // Test SVG cleaning
        val rawSvg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                <metadata><author>John Doe</author><gps>lat: -6.2, lon: 106.8</gps></metadata>
                <script>alert('malicious')</script>
                <circle cx="50" cy="50" r="40" fill="red"/>
            </svg>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val cleanedSvgBytes = MetadataStripper.stripMetadata(rawSvg, "image/svg+xml")
        val cleanedSvg = String(cleanedSvgBytes, Charsets.UTF_8)

        assertFalse(cleanedSvg.contains("<metadata>"))
        assertFalse(cleanedSvg.contains("<script>"))
        assertTrue(cleanedSvg.contains("<circle"))

        // Test JPEG stripping logic
        val jpegStream = ByteArrayOutputStream()
        jpegStream.write(0xFF); jpegStream.write(0xD8) // SOI
        jpegStream.write(0xFF); jpegStream.write(0xE1) // APP1 (EXIF)
        jpegStream.write(0x00); jpegStream.write(0x08) // Length 8
        jpegStream.write("Exif\u0000\u0000".toByteArray())
        jpegStream.write(0xFF); jpegStream.write(0xDB) // DQT
        jpegStream.write(0x00); jpegStream.write(0x04)
        jpegStream.write(byteArrayOf(0x01, 0x02))
        jpegStream.write(0xFF); jpegStream.write(0xDA) // SOS
        jpegStream.write(byteArrayOf(0x10, 0x20, 0x30))
        jpegStream.write(0xFF); jpegStream.write(0xD9) // EOI

        val rawJpeg = jpegStream.toByteArray()
        val cleanedJpeg = MetadataStripper.stripMetadata(rawJpeg, "image/jpeg")
        assertNotNull(cleanedJpeg)
        assertTrue(cleanedJpeg.isNotEmpty())
    }

    // =========================================================================
    // 6. Channel Adapters (WhatsApp, Instagram, TikTok)
    // =========================================================================
    @Test
    fun testWhatsAppAdapterSignatureAndNormalization() {
        val adapter = WhatsAppAdapter(appSecret = "test_whatsapp_secret")

        val payload = mapOf(
            "tenant_id" to "tenant-abc",
            "from" to "+6281234567890",
            "body" to "Hello Orchestree AI support"
        )
        val inbound = adapter.normalize(payload)
        assertEquals("tenant-abc", inbound.tenantId)
        assertEquals("+6281234567890", inbound.senderId)
        assertEquals("Hello Orchestree AI support", inbound.text)
        assertEquals("WHATSAPP", inbound.channelType)

        // Webhook signature verification
        val rawBody = """{"entry":[]}"""
        val hmac = javax.crypto.Mac.getInstance("HmacSHA256")
        hmac.init(javax.crypto.spec.SecretKeySpec("test_whatsapp_secret".toByteArray(), "HmacSHA256"))
        val hash = hmac.doFinal(rawBody.toByteArray()).joinToString("") { "%02x".format(it) }

        assertTrue(adapter.verifyWebhookSignature(rawBody, "sha256=$hash", "test_whatsapp_secret"))
        assertFalse(adapter.verifyWebhookSignature(rawBody, "sha256=invalidhash", "test_whatsapp_secret"))
    }

    @Test
    fun testInstagramAdapterNormalizationAndSignature() {
        val adapter = InstagramAdapter(appSecret = "test_ig_secret")

        val payload = mapOf(
            "tenant_id" to "tenant-xyz",
            "ig_user_id" to "ig_customer_999",
            "message" to "Inquiry about enterprise tier"
        )
        val inbound = adapter.normalize(payload)
        assertEquals("tenant-xyz", inbound.tenantId)
        assertEquals("ig_customer_999", inbound.senderId)
        assertEquals("Inquiry about enterprise tier", inbound.text)
        assertEquals("INSTAGRAM", inbound.channelType)
    }

    @Test
    fun testTikTokAdapterSignatureAndOrderParser() {
        val secret = "tiktok_secret_key"
        val payload = """{"order_id":"TT-1002","shop_id":"shop-01","order_status":"PAID","total_amount":350000.0,"buyer_uid":"user_tt_88"}"""

        val hmac = javax.crypto.Mac.getInstance("HmacSHA256")
        hmac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val expectedHash = hmac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }

        assertTrue(TikTokAdapter.verifyTikTokSignature(payload, secret, expectedHash))
        assertFalse(TikTokAdapter.verifyTikTokSignature(payload, secret, "wrong_hash"))

        val order = TikTokAdapter.parseTikTokOrder(payload)
        assertEquals("TT-1002", order.orderId)
        assertEquals("shop-01", order.shopId)
        assertEquals(350000.0, order.totalAmount)
        assertEquals("user_tt_88", order.buyerUid)
    }

    // =========================================================================
    // 7. Central Credit Ledger Reservation & Consumption Lifecycle
    // =========================================================================
    @Test
    fun testCreditLedgerReservationAndRelease() {
        val ledger = CentralCreditLedgerService.getInstance()
        val tenantId = "tenant-e2e-credit-test"

        // Check balance calculation
        val balance = ledger.getBalance(tenantId)
        assertTrue(balance >= 0.0)

        // Reserve credits
        val reservation = ledger.reserve(
            tenantId = tenantId,
            estimatedCredits = 10.0,
            referenceType = "e2e_integration_test",
            referenceId = "ref-101"
        )
        assertNotNull(reservation)
        assertEquals(10.0, reservation.estimatedCredits)
        assertEquals("RESERVED", reservation.status)

        // Release reservation
        ledger.release(reservation.id, 10.0)
    }
}

