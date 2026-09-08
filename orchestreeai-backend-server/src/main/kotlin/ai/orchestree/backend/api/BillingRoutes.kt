package ai.orchestree.backend.api

import ai.orchestree.backend.billing.*
import ai.orchestree.backend.database.repositories.workforce.AgentModel
import ai.orchestree.backend.database.repositories.workforce.User
import ai.orchestree.backend.database.repositories.workforce.AgentRepository
import ai.orchestree.backend.database.repositories.workforce.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.UUID

/**
 * Commercial & Billing System Routes (Bagian C: Full Billing API Surface)
 *
 * MENEGAKKAN:
 * ONE TENANT -> ONE SUBSCRIPTION -> ONE AI CREDIT BALANCE -> ONE CENTRAL CREDIT LEDGER -> ALL AI WORKFORCE -> ONE BILLING SOURCE OF TRUTH.
 *
 * Seluruh endpoint interaksi real-time langsung ke Supabase PostgreSQL.
 */
fun Route.billingRoutes(
    creditLedgerService: CentralCreditLedgerService = CentralCreditLedgerService.getInstance(),
    commercialCreditEngine: CommercialCreditEngine = CommercialCreditEngine(),
    repoManager: CreditRepositoryManager = CreditRepositoryManager(),
    entitlementEngine: EntitlementEngine = EntitlementEngine.defaultInstance,
    userRepo: UserRepository = UserRepository.defaultInstance,
    aiAgentRepo: AgentRepository = AgentRepository.defaultInstance
) {
    val logger = LoggerFactory.getLogger("BillingRoutes")

    // Helper to enforce auth on protected billing endpoints
    suspend fun ApplicationCall.enforceAuthAndGetTenant(): String? {
        val principal = this.principal<JWTPrincipal>()
        val authHeader = this.request.headers["Authorization"]
        if (principal == null && (authHeader == null || !authHeader.startsWith("Bearer "))) {
            this.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Unauthorized: Authentication token is required for this billing resource.")
            )
            return null
        }
        val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
            ?: this.request.headers["X-Tenant-Id"]
            ?: this.request.queryParameters["tenant_id"]
            ?: "tenant-enterprise-001"
        return tenantId
    }

    // =========================================================================
    // 1 & 2: COMMERCIAL PLANS (PUBLIC, TANPA AUTH)
    // =========================================================================
    route("/plans") {
        get {
            try {
                val plans = repoManager.getAllActiveCommercialPlans()
                call.respond(HttpStatusCode.OK, plans)
            } catch (e: Exception) {
                logger.error("Failed to retrieve commercial plans: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to retrieve plans")))
            }
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing plan id"))
            try {
                val plan = repoManager.getCommercialPlan(id)
                call.respond(HttpStatusCode.OK, plan)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Plan not found: $id"))
            }
        }

        // Public Entitlements Matrix (NO AUTH) for Homepage / Pricing comparison table
        get("/entitlements-matrix") {
            try {
                val matrix = repoManager.getPlanFeatureEntitlementsMatrix()
                call.respond(HttpStatusCode.OK, matrix)
            } catch (e: Exception) {
                logger.error("Failed to retrieve entitlements matrix: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to retrieve matrix")))
            }
        }
    }

    // =========================================================================
    // 3: TENANT ENTITLEMENTS (AUTH)
    // =========================================================================
    route("/tenant") {
        get("/entitlements") {
            val tenantId = call.enforceAuthAndGetTenant() ?: return@get
            try {
                val entitlements = entitlementEngine.getTenantEntitlements(tenantId)
                call.respond(HttpStatusCode.OK, entitlements)
            } catch (e: Exception) {
                logger.error("Failed to retrieve tenant entitlements for $tenantId: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to get entitlements")))
            }
        }
    }

    // =========================================================================
    // 4 - 21: BILLING ENDPOINTS UNDER /billing
    // =========================================================================
    route("/billing") {
        // Alias for plans
        get("/plans") {
            try {
                val plans = repoManager.getAllActiveCommercialPlans()
                call.respond(HttpStatusCode.OK, plans)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to retrieve plans")))
            }
        }

        get("/plans/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing plan id"))
            try {
                val plan = repoManager.getCommercialPlan(id)
                call.respond(HttpStatusCode.OK, plan)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Plan not found: $id"))
            }
        }

        // Formula Calculation
        post("/calculate-cost") {
            try {
                val context = call.receive<CreditCostContext>()
                val result = commercialCreditEngine.calculateCreditCost(context)
                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                logger.error("Failed to calculate credit cost: ${e.message}", e)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            }
        }

        // =====================================================================
        // SUBSCRIPTION MANAGEMENT (AUTH)
        // =====================================================================
        route("/subscription") {
            // GET /api/v1/billing/subscription
            get {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@get
                try {
                    val sub = repoManager.getTenantSubscription(tenantId)
                    if (sub == null) {
                        call.respond(HttpStatusCode.OK, SubscriptionPlanResponse(status = "none", tenantId = tenantId))
                        return@get
                    }
                    val plan = repoManager.getCommercialPlan(sub.planId)
                    call.respond(
                        HttpStatusCode.OK,
                        SubscriptionPlanResponse(
                            status = "active",
                            tenantId = tenantId,
                            subscription = sub,
                            plan = plan
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Failed to retrieve subscription for $tenantId: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Internal error")))
                }
            }

            // POST /api/v1/billing/subscription (Mulai subscription baru)
            post {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@post
                try {
                    val req = call.receive<SubscriptionStartRequest>()
                    val sub = commercialCreditEngine.startSubscription(tenantId, req.planId, req.billingInterval)
                    val plan = repoManager.getCommercialPlan(sub.planId)
                    call.respond(
                        HttpStatusCode.Created,
                        SubscriptionPlanResponse(
                            status = "created",
                            tenantId = tenantId,
                            subscription = sub,
                            plan = plan
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Failed to start subscription for $tenantId: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to start subscription")))
                }
            }

            // POST /api/v1/billing/subscription/upgrade (Prorated calculation)
            post("/upgrade") {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@post
                try {
                    val req = call.receive<SubscriptionUpgradeRequest>()
                    val result = commercialCreditEngine.upgradeSubscription(tenantId, req.targetPlanId)
                    call.respond(HttpStatusCode.OK, result)
                } catch (e: Exception) {
                    logger.error("Failed to upgrade subscription for $tenantId: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to upgrade subscription")))
                }
            }

            // POST /api/v1/billing/subscription/downgrade (Cek overage dulu - Langkah 1)
            post("/downgrade") {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@post
                try {
                    val req = call.receive<SubscriptionDowngradeRequest>()
                    when (val result = commercialCreditEngine.downgradeSubscription(tenantId, req.targetPlanId)) {
                        is DowngradeResult.Blocked -> {
                            call.respond(
                                HttpStatusCode.Conflict,
                                DowngradeBlockedResponse(
                                    status = "blocked",
                                    message = "Your current usage exceeds the target plan limit",
                                    overages = result.overages
                                )
                            )
                        }
                        is DowngradeResult.Success -> {
                            val plan = repoManager.getCommercialPlan(result.subscription.planId)
                            call.respond(
                                HttpStatusCode.OK,
                                SubscriptionPlanResponse(
                                    status = "downgraded",
                                    tenantId = tenantId,
                                    subscription = result.subscription,
                                    plan = plan
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to downgrade subscription for $tenantId: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to downgrade subscription")))
                }
            }

            // POST /api/v1/billing/subscription/cancel
            post("/cancel") {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@post
                try {
                    val sub = commercialCreditEngine.cancelSubscription(tenantId)
                    call.respond(HttpStatusCode.OK, CancelSubscriptionResponse(status = "cancelled", subscription = sub))
                } catch (e: Exception) {
                    logger.error("Failed to cancel subscription for $tenantId: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to cancel subscription")))
                }
            }
        }

        // =====================================================================
        // CREDITS ENDPOINTS (AUTH)
        // =====================================================================
        // GET /api/v1/billing/credits: format response PERSIS: {available, reserved, used, total}
        get("/credits") {
            val tenantId = call.enforceAuthAndGetTenant() ?: return@get
            try {
                val wallet = repoManager.getWallet(tenantId)
                val available = wallet.availableBalance
                val reserved = wallet.reservedBalance
                val used = wallet.usedBalance
                val total = wallet.subscriptionBalance + wallet.topupBalance + wallet.bonusBalance

                call.respond(
                    HttpStatusCode.OK,
                    CreditsDisplayResponse(
                        available = available,
                        reserved = reserved,
                        used = used,
                        total = total
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to get credits for $tenantId: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to get credits")))
            }
        }

        // Summary endpoint (backwards compatible)
        get("/credits/summary") {
            val tenantId = call.enforceAuthAndGetTenant() ?: return@get
            try {
                val summary = creditLedgerService.getTenantCreditSummary(tenantId)
                call.respond(HttpStatusCode.OK, summary)
            } catch (e: Exception) {
                logger.error("Failed to retrieve tenant credits summary for $tenantId: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to retrieve billing credits")))
            }
        }

        // GET /api/v1/billing/credits/ledger & /api/v1/billing/ledger: paginated, filterable
        val handleLedgerQuery: suspend (ApplicationCall) -> Unit = { c ->
            val tenantId = c.enforceAuthAndGetTenant()
            if (tenantId != null) {
                val limit = c.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = c.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val ledgerType = c.request.queryParameters["type"] ?: c.request.queryParameters["ledger_type"]

                try {
                    val (total, entries) = repoManager.getLedgerEntriesPaginated(tenantId, limit, offset, ledgerType)
                    c.respond(
                        HttpStatusCode.OK,
                        LedgerPageResponse(
                            entries = entries,
                            total = total,
                            limit = limit,
                            offset = offset
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Failed to retrieve ledger for $tenantId: ${e.message}", e)
                    c.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to retrieve ledger")))
                }
            }
        }

        get("/credits/ledger") { handleLedgerQuery(call) }
        get("/ledger") { handleLedgerQuery(call) }

        // POST /api/v1/billing/credits/topup
        post("/credits/topup") {
            val tenantId = call.enforceAuthAndGetTenant() ?: return@post
            try {
                val req = call.receive<CreditTopupRequest>()
                val wallet = commercialCreditEngine.topupCredits(
                    tenantId = tenantId,
                    amount = req.amount,
                    amountPaid = req.amountPaid,
                    currency = req.currency,
                    reference = req.reference
                )
                call.respond(
                    HttpStatusCode.OK,
                    TopupSuccessResponse(
                        status = "success",
                        tenantId = tenantId,
                        creditsAdded = req.amount,
                        newBalance = wallet.availableBalance,
                        reference = req.reference ?: ""
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to topup credits for $tenantId: ${e.message}", e)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to topup credits")))
            }
        }

        // =====================================================================
        // SEATS MANAGEMENT (AUTH)
        // =====================================================================
        route("/seats") {
            // GET /api/v1/billing/seats
            get {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@get
                try {
                    val seats = userRepo.findByTenant(tenantId)
                    val sub = repoManager.getTenantSubscription(tenantId)
                    val plan = sub?.let { repoManager.getCommercialPlan(it.planId) }
                    val limit = plan?.humanSeatLimit ?: 999999
                    call.respond(
                        HttpStatusCode.OK,
                        SeatsListResponse(
                            seats = seats,
                            totalActive = seats.size,
                            seatLimit = limit
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Failed to get seats for $tenantId: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to get seats")))
                }
            }

            // POST /api/v1/billing/seats
            post {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@post
                try {
                    val req = call.receive<SeatCreateRequest>()
                    val currentCount = userRepo.countActiveByTenant(tenantId)
                    val sub = repoManager.getTenantSubscription(tenantId)
                    val plan = sub?.let { repoManager.getCommercialPlan(it.planId) }
                    val limit = plan?.humanSeatLimit ?: 999999

                    if (currentCount >= limit) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf(
                                "error" to "SEAT_LIMIT_EXCEEDED",
                                "message" to "Limit kursi staff ($limit) telah tercapai. Upgrade paket langganan untuk menambah staff."
                            )
                        )
                        return@post
                    }

                    val newUser = User(
                        id = "usr-" + UUID.randomUUID().toString().take(8),
                        tenantId = tenantId,
                        departmentId = req.departmentId,
                        teamId = "team-${req.departmentId}",
                        name = req.name,
                        email = req.email,
                        role = req.role
                    )
                    userRepo.save(newUser)
                    call.respond(HttpStatusCode.Created, mapOf("status" to "created", "seat_id" to newUser.id))
                } catch (e: Exception) {
                    logger.error("Failed to create seat for $tenantId: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to create seat")))
                }
            }

            // DELETE /api/v1/billing/seats/{id}
            delete("/{id}") {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@delete
                val seatId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing seat id"))
                try {
                    val deleted = userRepo.delete(seatId)
                    if (deleted) {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "deleted", "seat_id" to seatId))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Seat not found: $seatId"))
                    }
                } catch (e: Exception) {
                    logger.error("Failed to delete seat $seatId: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to delete seat")))
                }
            }
        }

        // =====================================================================
        // AI AGENTS MANAGEMENT (AUTH)
        // =====================================================================
        route("/agents") {
            // GET /api/v1/billing/agents
            get {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@get
                try {
                    val agents = aiAgentRepo.findByTenant(tenantId)
                    val sub = repoManager.getTenantSubscription(tenantId)
                    val plan = sub?.let { repoManager.getCommercialPlan(it.planId) }
                    val limit = plan?.aiAgentLimit ?: 999999
                    call.respond(
                        HttpStatusCode.OK,
                        AgentsListResponse(
                            agents = agents,
                            totalActive = agents.size,
                            agentLimit = limit
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Failed to get agents for $tenantId: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to get agents")))
                }
            }

            // POST /api/v1/billing/agents
            post {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@post
                try {
                    val req = call.receive<BillingAgentCreateRequest>()
                    val currentCount = aiAgentRepo.countActiveByTenant(tenantId)
                    val sub = repoManager.getTenantSubscription(tenantId)
                    val plan = sub?.let { repoManager.getCommercialPlan(it.planId) }
                    val limit = plan?.aiAgentLimit ?: 999999

                    if (currentCount >= limit) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf(
                                "error" to "AGENT_LIMIT_EXCEEDED",
                                "message" to "Limit AI Agent ($limit) telah tercapai. Upgrade paket langganan untuk deploy agent baru."
                            )
                        )
                        return@post
                    }

                    val newAgent = AgentModel(
                        id = "agt-" + UUID.randomUUID().toString().take(8),
                        tenantId = tenantId,
                        name = req.name,
                        role = req.role,
                        personaCode = req.personaCode,
                        departmentId = req.departmentId ?: "dept-ai-ops",
                        status = "ACTIVE"
                    )
                    aiAgentRepo.save(newAgent)
                    call.respond(HttpStatusCode.Created, mapOf("status" to "created", "agent_id" to newAgent.id))
                } catch (e: Exception) {
                    logger.error("Failed to create agent for $tenantId: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to create agent")))
                }
            }

            // DELETE /api/v1/billing/agents/{id}
            delete("/{id}") {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@delete
                val agentId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing agent id"))
                try {
                    val deleted = aiAgentRepo.delete(agentId)
                    if (deleted) {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "deleted", "agent_id" to agentId))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Agent not found: $agentId"))
                    }
                } catch (e: Exception) {
                    logger.error("Failed to delete agent $agentId: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to delete agent")))
                }
            }
        }

        // =====================================================================
        // INVOICES (AUTH)
        // =====================================================================
        route("/invoices") {
            get {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@get
                try {
                    val invoices = repoManager.getInvoices(tenantId)
                    call.respond(HttpStatusCode.OK, invoices)
                } catch (e: Exception) {
                    logger.error("Failed to get invoices for $tenantId: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to get invoices")))
                }
            }

            get("/{id}") {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@get
                val invoiceId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing invoice id"))
                try {
                    val invoice = repoManager.getInvoiceById(invoiceId)
                    if (invoice != null && invoice.tenantId == tenantId) {
                        call.respond(HttpStatusCode.OK, invoice)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invoice not found: $invoiceId"))
                    }
                } catch (e: Exception) {
                    logger.error("Failed to get invoice $invoiceId: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to get invoice")))
                }
            }

            post("/{id}/fail-dunning") {
                val tenantId = call.enforceAuthAndGetTenant() ?: return@post
                val invoiceId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing invoice id"))
                val body = try { call.receive<Map<String, String>>() } catch (_: Exception) { emptyMap() }
                val reason = body["reason"] ?: "Payment rejected by bank"
                val result = DunningEngine.handleInvoiceFailure(
                    tenantId = tenantId,
                    invoiceId = invoiceId,
                    amount = body["amount"]?.toDoubleOrNull() ?: 500000.0,
                    reason = reason
                )
                call.respond(HttpStatusCode.OK, result)
            }
        }

        // =====================================================================
        // PAYMENT & MIDTRANS WEBHOOK
        // =====================================================================
        // POST /api/v1/billing/payment (AUTH, redirect ke Midtrans)
        post("/payment") {
            val tenantId = call.enforceAuthAndGetTenant() ?: return@post
            try {
                val req = call.receive<PaymentCreateRequest>()
                val amount = req.amount ?: 500000.0
                val planName = req.planId ?: "Professional Subscription"
                val invoice = repoManager.createInvoice(
                    tenantId = tenantId,
                    planName = planName,
                    amountIdr = amount
                )
                call.respond(
                    HttpStatusCode.OK,
                    PaymentInitiateResponse(
                        status = "pending",
                        invoiceId = invoice.id,
                        paymentUrl = invoice.paymentUrl,
                        snapToken = invoice.snapToken,
                        amount = invoice.totalAmountIdr,
                        currency = "IDR"
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to create payment for $tenantId: ${e.message}", e)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to create payment")))
            }
        }

        // POST /api/v1/billing/payment/webhook (PUBLIC, Signature Verified)
        val handleWebhook: suspend (ApplicationCall) -> Unit = { c ->
            try {
                val payload = c.receive<MidtransWebhookPayload>()
                commercialCreditEngine.handleSubscriptionPaymentWebhook(payload)
                c.respond(HttpStatusCode.OK, mapOf("status" to "processed", "order_id" to payload.orderId))
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid webhook payload or signature: ${e.message}")
                c.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid signature")))
            } catch (e: Exception) {
                logger.error("Error processing payment webhook: ${e.message}", e)
                c.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Internal error")))
            }
        }

        post("/payment/webhook") { handleWebhook(call) }
        post("/webhook/midtrans") { handleWebhook(call) }

        // =====================================================================
        // SMART UPGRADE RECOMMENDATION (Section 3)
        // =====================================================================
        // GET /api/v1/billing/upgrade-recommendation
        get("/upgrade-recommendation") {
            val tenantId = call.enforceAuthAndGetTenant() ?: return@get
            try {
                val wallet = repoManager.getWallet(tenantId)
                val sub = repoManager.getTenantSubscription(tenantId)
                val currentPlan = sub?.let { repoManager.getCommercialPlan(it.planId) }
                val seatsCount = userRepo.countActiveByTenant(tenantId)
                val agentsCount = aiAgentRepo.countActiveByTenant(tenantId)

                val seatLimit = (currentPlan?.humanSeatLimit ?: 10).coerceAtLeast(1)
                val agentLimit = (currentPlan?.aiAgentLimit ?: 10).coerceAtLeast(1)
                val totalCredits = (wallet.subscriptionBalance + wallet.topupBalance + wallet.bonusBalance).coerceAtLeast(1.0)
                val usedCredits = wallet.usedBalance
                val availableCredits = wallet.availableBalance

                val creditUtil = ((usedCredits / totalCredits) * 100.0).coerceIn(0.0, 100.0)
                val seatUtil = ((seatsCount.toDouble() / seatLimit) * 100.0).coerceIn(0.0, 100.0)
                val agentUtil = ((agentsCount.toDouble() / agentLimit) * 100.0).coerceIn(0.0, 100.0)

                val maxUtil = maxOf(creditUtil, seatUtil, agentUtil)

                val (suggestedCode, suggestedName) = when (currentPlan?.planCode?.lowercase()) {
                    "starter" -> "professional" to "Professional"
                    "professional" -> "enterprise" to "Enterprise"
                    "enterprise" -> "custom" to "Custom Enterprise"
                    else -> "professional" to "Professional"
                }

                val level = when {
                    availableCredits <= 0.0 || maxUtil >= 100.0 -> "LIMIT_REACHED"
                    maxUtil >= 80.0 -> "STRONG_RECOMMENDATION"
                    maxUtil >= 60.0 -> "SOFT_RECOMMENDATION"
                    else -> "NO_UPGRADE"
                }

                val title = when (level) {
                    "LIMIT_REACHED" -> "Batas Kuota Tercapai (Limit Reached)"
                    "STRONG_RECOMMENDATION" -> "Rekomendasi Peningkatan Paket"
                    "SOFT_RECOMMENDATION" -> "Kapasitas Operasional Mendekati Batas"
                    else -> "Utilisasi Normal"
                }

                val message = when (level) {
                    "LIMIT_REACHED" -> "Kuota Anda telah habis (Utilisasi: ${String.format(Locale.US, "%.1f", maxUtil)}%). Segera upgrade ke paket $suggestedName agar seluruh AI Workforce dan staf tetap beroperasi tanpa hambatan."
                    "STRONG_RECOMMENDATION" -> "Utilisasi kapasitas Anda telah mencapai ${String.format(Locale.US, "%.1f", maxUtil)}%. Kami menyarankan peningkatan ke paket $suggestedName untuk menghindari pembatasan operasional."
                    "SOFT_RECOMMENDATION" -> "Utilisasi Anda mencapai ${String.format(Locale.US, "%.1f", maxUtil)}%. Pantau beban kerja workforce Anda secara berkala."
                    else -> "Seluruh kapasitas kredit, kursi tim, dan agen AI berada dalam kondisi prima."
                }

                val metrics = mapOf(
                    "credit_utilization" to creditUtil,
                    "seat_utilization" to seatUtil,
                    "agent_utilization" to agentUtil,
                    "available_credits" to availableCredits,
                    "total_credits" to totalCredits
                )

                call.respond(
                    HttpStatusCode.OK,
                    UpgradeRecommendationResponse(
                        level = level,
                        title = title,
                        message = message,
                        suggestedPlanCode = suggestedCode,
                        suggestedPlanName = suggestedName,
                        utilizationMetrics = metrics
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to compute upgrade recommendation for $tenantId: ${e.message}", e)
                call.respond(
                    HttpStatusCode.OK,
                    UpgradeRecommendationResponse(
                        level = "NO_UPGRADE",
                        title = "Utilisasi Normal",
                        message = "Kapasitas kredit dan AI Workforce beroperasi normal.",
                        suggestedPlanCode = "professional",
                        suggestedPlanName = "Professional"
                    )
                )
            }
        }
    }
}
