package ai.orchestree.backend

import ai.orchestree.backend.database.repositories.orchestration.PendingApprovalRepository
import ai.orchestree.backend.database.repositories.orchestration.WorkflowExecutionRepository
import ai.orchestree.backend.mcptools.McpToolExecutor
import ai.orchestree.backend.mcptools.McpToolRegistry
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.NodeExecutionResult
import ai.orchestree.backend.orchestration.NodeExecutionStatus
import ai.orchestree.backend.orchestration.NodeResult
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.orchestration.WorkflowContext
import ai.orchestree.backend.orchestration.WorkflowExecution
import ai.orchestree.backend.orchestration.WorkflowNode
import ai.orchestree.backend.orchestration.WorkflowNodeType
import ai.orchestree.backend.orchestration.approval.ApprovalDecision
import ai.orchestree.backend.orchestration.approval.HumanInTheLoopGate
import ai.orchestree.backend.orchestration.approval.PendingApproval
import ai.orchestree.backend.orchestration.nodes.HumanApprovalWorkflowNode
import ai.orchestree.backend.payments.PaymentGatewayClient
import ai.orchestree.backend.payments.PaymentTransactionRequest
import ai.orchestree.backend.resilience.executeWithRetry
import ai.orchestree.backend.security.AuditLogger
import ai.orchestree.backend.webhooks.PaymentNotification
import ai.orchestree.backend.webhooks.PaymentWebhookHandler
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger

class HumanInTheLoopAndResilienceTest {

    private lateinit var pendingApprovalRepo: PendingApprovalRepository
    private lateinit var executionRepo: WorkflowExecutionRepository
    private lateinit var engine: OrchestrationEngine
    private lateinit var gate: HumanInTheLoopGate
    private lateinit var auditLog: AuditLogger

    @BeforeEach
    fun setUp() {
        pendingApprovalRepo = PendingApprovalRepository()
        pendingApprovalRepo.clearInMemory()
        executionRepo = WorkflowExecutionRepository()
        auditLog = AuditLogger()
        engine = OrchestrationEngine(
            modelRouter = ModelRouter(),
            mcpExecutor = McpToolExecutor(McpToolRegistry.defaultRegistry()),
            workflowExecutionRepo = executionRepo,
            pendingApprovalRepo = pendingApprovalRepo
        )
        gate = engine.humanGate
    }

    // =========================================================================
    // LANGKAH 1 — HUMAN APPROVAL SEBAGAI PRIMITIVE FORMAL
    // =========================================================================

    @Test
    fun `test role determination logic based on reason`() {
        // Diskon / Refund / Financial -> FINANCE_ADMIN
        assertEquals("FINANCE_ADMIN", gate.determineApproverRole("Permintaan diskon khusus 35% untuk klien enterprise"))
        assertEquals("FINANCE_ADMIN", gate.determineApproverRole("Customer refund approval order #TX-901"))
        assertEquals("FINANCE_ADMIN", gate.determineApproverRole("Persetujuan biaya operasional luar kota"))

        // Security / Pentest / Risiko Enterprise -> SUPER_ADMIN
        assertEquals("SUPER_ADMIN", gate.determineApproverRole("Aksi enterprise berisiko tinggi drop database schema"))
        assertEquals("SUPER_ADMIN", gate.determineApproverRole("Eksekusi security vulnerability patch"))
        assertEquals("SUPER_ADMIN", gate.determineApproverRole("High risk production deploy authorization"))

        // Department Manager / HR -> DEPT_MANAGER
        assertEquals("DEPT_MANAGER", gate.determineApproverRole("Persetujuan cuti staff oleh pimpinan"))
        assertEquals("DEPT_MANAGER", gate.determineApproverRole("Evaluasi HR manager untuk promosi"))

        // Default -> TENANT_ADMIN
        assertEquals("TENANT_ADMIN", gate.determineApproverRole("Pembaruan profil umum perusahaan"))
    }

    @Test
    fun `test approval resumption after simulated 2 days preserves exact context snapshot`() = runBlocking {
        val executionId = "exec-long-delay-001"
        val tenantId = "tenant-enterprise-testing"

        // Context state yang kaya dan lengkap
        val initialContext: MutableMap<String, Any> = mutableMapOf(
            "orderId" to "ORD-2026-9988",
            "customerId" to "CUST-8831",
            "customerTier" to "PLATINUM",
            "baseAmount" to 150000000L,
            "requestedDiscountPct" to 40,
            "netAmount" to 90000000L,
            "riskScore" to 0.85,
            "requesterRole" to "STAFF_HUMAN",
            "requiresDualControl" to true,
            "sessionMetadata" to mapOf("ip" to "182.253.40.12", "device" to "Android 15 Client")
        )

        val execution = WorkflowExecution(
            id = executionId,
            tenantId = tenantId,
            workflowDefId = "discount-approval-flow",
            currentNodeId = "node-discount-gate"
        ).apply {
            this.context = initialContext
        }

        // 1. Interrupt execution via centralized HumanInTheLoopGate
        val pauseResult = gate.interrupt(execution, "Persetujuan diskon enterprise 40% bernilai besar")
        assertTrue(pauseResult is NodeResult.Paused)
        val pendingId = (pauseResult as NodeResult.Paused).pendingApprovalId

        // Verify PendingApproval record in repository
        val stored = pendingApprovalRepo.get(pendingId)
        assertNotNull(stored)
        assertEquals("PENDING", stored!!.status)
        assertEquals("FINANCE_ADMIN", stored.approverRoleRequired)
        assertEquals(executionId, stored.executionId)
        assertEquals("node-discount-gate", stored.nodeId)

        // Verifikasi fullContextSnapshot menyimpan seluruh atribut lengkap
        val snapshotObj = stored.fullContextSnapshot.jsonObject
        assertEquals("ORD-2026-9988", snapshotObj["orderId"]?.jsonPrimitive?.content)
        assertEquals("PLATINUM", snapshotObj["customerTier"]?.jsonPrimitive?.content)
        assertEquals("90000000", snapshotObj["netAmount"]?.jsonPrimitive?.content)

        // 2. SIMULASI WAKTU BERLALU 2 HARI (48 JAM)
        val twoDaysAgo = Instant.now().minus(48, ChronoUnit.HOURS)
        pendingApprovalRepo.save(stored.copy(requestedAt = twoDaysAgo.toString()))

        // Daftarkan downstream node agar workflow bisa lanjut setelah approval
        val nextNode = object : WorkflowNode {
            override val id: String = "node-deliver-discount"
            override val type: WorkflowNodeType = WorkflowNodeType.DELIVER
            override suspend fun execute(context: MutableMap<String, Any>): NodeExecutionResult {
                // Verifikasi bahwa data dari 2 hari lalu masih ada persis
                val orderId = context["orderId"]
                val netAmount = context["netAmount"]
                val decision = context["approval_decision"]
                val approvedBy = context["approved_by"]
                val out = "Discount applied for $orderId, final amount $netAmount, approved by $approvedBy ($decision)"
                context["finalOutput"] = out
                return NodeExecutionResult(
                    status = NodeExecutionStatus.SUCCESS,
                    output = out
                )
            }
            override fun next(context: Map<String, Any>): String? = null
        }
        engine.registerNode(nextNode)

        // Daftarkan gate node dengan nextNodeId
        val gateNode = HumanApprovalWorkflowNode("node-discount-gate", nextNodeId = "node-deliver-discount")
        engine.registerNode(gateNode)

        // 3. RESUME SETELAH 2 HARI DENGAN APPROVAL
        val resumeResult = gate.resume(
            pendingApprovalId = pendingId,
            decision = ApprovalDecision.APPROVED,
            approvedBy = "cfo@digitalnusantara.id"
        )

        // Verifikasi hasil resume
        assertEquals("COMPLETED", resumeResult.status)
        assertTrue(resumeResult.finalOutput.contains("Discount applied for ORD-2026-9988"))
        assertTrue(resumeResult.finalOutput.contains("cfo@digitalnusantara.id"))

        // Verifikasi status approval di repository terupdate
        val updatedApproval = pendingApprovalRepo.get(pendingId)
        assertNotNull(updatedApproval)
        assertEquals("APPROVED", updatedApproval!!.status)
        assertEquals("cfo@digitalnusantara.id", updatedApproval.approvedBy)
    }

    @Test
    fun `test rejection after long delay aborts workflow safely`() = runBlocking {
        val executionId = "exec-rejected-flow"
        val initialContext: MutableMap<String, Any> = mutableMapOf(
            "refundAmount" to 75000000L,
            "refundReason" to "Barang cacat produksi batch 12"
        )

        val execution = WorkflowExecution(
            id = executionId,
            tenantId = "tenant-002",
            workflowDefId = "refund-flow",
            currentNodeId = "refund-approval-node"
        ).apply { this.context = initialContext }

        val pauseResult = gate.interrupt(execution, "Refund dana besar pelanggan")
        val pendingId = (pauseResult as NodeResult.Paused).pendingApprovalId

        // Resume dengan REJECTED
        val result = gate.resume(
            pendingApprovalId = pendingId,
            decision = ApprovalDecision.REJECTED,
            approvedBy = "compliance-officer@orchestree.ai"
        )

        assertEquals("ABORTED", result.status)
        assertTrue(result.finalOutput.contains("Ditolak oleh compliance-officer@orchestree.ai"))

        val status = pendingApprovalRepo.get(pendingId)?.status
        assertEquals("REJECTED", status)
    }

    // =========================================================================
    // LANGKAH 2 — RETRY & BACKOFF STANDAR TUNGGAL (executeWithRetry)
    // =========================================================================

    @Test
    fun `test executeWithRetry retries transient network errors then succeeds`() = runBlocking {
        val attempts = AtomicInteger(0)

        val result = executeWithRetry(
            maxAttempts = 3,
            initialDelayMs = 20L,
            backoffMultiplier = 2.0,
            retryableExceptions = setOf(SocketTimeoutException::class, IOException::class)
        ) {
            val count = attempts.incrementAndGet()
            if (count < 3) {
                throw SocketTimeoutException("Simulated connection timeout attempt $count")
            }
            "SUCCESS_AFTER_RETRY"
        }

        assertEquals("SUCCESS_AFTER_RETRY", result)
        assertEquals(3, attempts.get(), "Harus mencoba 3 kali sebelum berhasil")
    }

    @Test
    fun `test executeWithRetry fails fast on non-retryable exception`() = runBlocking {
        val attempts = AtomicInteger(0)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                executeWithRetry(
                    maxAttempts = 3,
                    initialDelayMs = 20L,
                    retryableExceptions = setOf(IOException::class)
                ) {
                    attempts.incrementAndGet()
                    throw IllegalArgumentException("Non-retryable invalid argument error")
                }
            }
        }

        assertEquals("Non-retryable invalid argument error", ex.message)
        assertEquals(1, attempts.get(), "Non-retryable exception dilarang di-retry, harus langsung gagal pada percobaan 1")
    }

    @Test
    fun `test executeWithRetry throws exception when all attempts exhausted`() = runBlocking {
        val attempts = AtomicInteger(0)

        val ex = assertThrows(IOException::class.java) {
            runBlocking {
                executeWithRetry(
                    maxAttempts = 3,
                    initialDelayMs = 20L,
                    backoffMultiplier = 2.0,
                    retryableExceptions = setOf(IOException::class)
                ) {
                    attempts.incrementAndGet()
                    throw IOException("Network pipe broken")
                }
            }
        }

        assertEquals("Network pipe broken", ex.message)
        assertEquals(3, attempts.get(), "Harus mencoba maksimal 3 kali sebelum melempar exception terakhir")
    }

    // =========================================================================
    // INTEGRATION WITH PAYMENT GATEWAY & WEBHOOK
    // =========================================================================

    @Test
    fun `test payment gateway client transaction creation with retry`() = runBlocking {
        val client = PaymentGatewayClient()
        val req = PaymentTransactionRequest(
            orderId = "ORD-TEST-${System.currentTimeMillis()}",
            grossAmount = 5000000L,
            customerEmail = "corp@digitalnusantara.id",
            customerName = "PT Nusantara"
        )

        val res = client.createTransaction(req)
        assertTrue(res.isSuccess)
        val data = res.getOrThrow()
        assertNotNull(data.token)
        assertNotNull(data.redirectUrl)
        assertTrue(data.redirectUrl.contains("snap"))
    }

    @Test
    fun `test payment webhook handler resilient fulfillment`() = runBlocking {
        val handler = PaymentWebhookHandler()
        val serverKey = "SB-Mid-server-TEST-KEY-ORCHESTREE-2026"
        val orderId = "ORD-VERIFIED-991"
        val grossAmount = "15000000"
        val statusCode = "200"

        // Compute valid signature
        val raw = "$orderId$statusCode$grossAmount$serverKey"
        val md = java.security.MessageDigest.getInstance("SHA-512")
        val expectedSig = md.digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        val notif = PaymentNotification(
            orderId = orderId,
            statusCode = statusCode,
            grossAmount = grossAmount,
            transactionStatus = "settlement",
            signatureKey = expectedSig
        )

        var fulfilledOrder: String? = null
        val success = handler.processAndFulfillWithRetry(notif, serverKey) { id ->
            fulfilledOrder = id
        }

        assertTrue(success)
        assertEquals(orderId, fulfilledOrder)
    }
}
