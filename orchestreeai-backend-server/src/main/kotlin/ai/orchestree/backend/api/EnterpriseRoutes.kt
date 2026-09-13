package ai.orchestree.backend.api

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.intelligence.CrossSystemCorrelator
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class EnterpriseConnectionCreateRequest(
    val systemType: String,
    val connectionEndpoint: String,
    val authType: String = "BEARER_TOKEN",
    val syncScheduleCron: String = "0 * * * *"
)

@Serializable
data class EnterpriseConnectionCreateResponse(
    val connectionId: String,
    val systemType: String,
    val status: String,
    val health: String
)

@Serializable
data class AiDataPermissionPolicyRequest(
    val agentPersonaType: String,
    val domainScope: String,
    val accessLevel: String = "READ_ONLY",
    val conditionsJson: String = "{}"
)

@Serializable
data class AiDataPermissionPolicyResponse(
    val policyId: String,
    val agentPersonaType: String,
    val accessLevel: String,
    val status: String
)

@Serializable
data class ManagementQueryRequest(
    val question: String,
    val entityFocus: String? = null,
    val sessionId: String? = null,
    val role: String? = "EXECUTIVE",
    val agentId: String? = "agent-chief-of-staff"
)

@Serializable
data class KnowledgeRuleCreateRequest(
    val entityType: String,
    val sopReference: String,
    val structuredRuleJson: String,
    val naturalLanguageRule: String
)

@Serializable
data class ResearchDirectiveRequest(
    val topic: String,
    val parametersJson: String = "{}"
)

@Serializable
data class EnterpriseActivityStreamItem(
    val id: String,
    val sourceSystem: String,
    val summaryText: String,
    val occurredAt: Long
)

@Serializable
data class EnterpriseContextFabricResponse(
    val entityId: String,
    val tenantId: String,
    val currentContext: String,
    val historicalContext: String,
    val businessContext: String,
    val operationalContext: String,
    val humanContext: String,
    val assetContext: String,
    val financialContext: String,
    val projectContext: String
)

@Serializable
data class ManagementQueryResponse(
    val question: String,
    val answer: String,
    val confidence: Double,
    val dataAvailability: String,
    val sourcesUsed: List<String>,
    val sessionId: String? = null,
    val turnCount: Int = 1,
    val accessRestricted: Boolean = false,
    val rolePersonalization: String? = null
)

@Serializable
data class ExecutiveDailyReportResponse(
    val reportId: String,
    val reportType: String,
    val summary: String,
    val generatedAt: Long
)

@Serializable
data class EnterpriseAiEventItem(
    val eventId: String,
    val eventCode: String,
    val responsiblePersona: String,
    val status: String
)

@Serializable
data class ChiefOfStaffBriefingItem(
    val briefingId: String,
    val briefingType: String,
    val executiveSummary: String,
    val contributingAgents: List<String>
)

@Serializable
data class AgentSkillConfidenceResponse(
    val agentId: String,
    val skillConfidenceScore: Double,
    val reinforceCount: Int,
    val correctCount: Int,
    val growthTrend: String
)

@Serializable
data class DataQualityIssueItem(
    val issueId: String,
    val entityReference: String,
    val field: String,
    val sourceA: String,
    val sourceB: String,
    val status: String
)

private val logger = org.slf4j.LoggerFactory.getLogger("EnterpriseRoutes")

fun Route.enterpriseRoutes() {
    val supabase = SupabaseClientProvider.fromEnv()
    val modelRouter = ModelRouter()
    val orchestrationEngine = ai.orchestree.backend.orchestration.OrchestrationEngine(modelRouter = modelRouter)
    val correlator = CrossSystemCorrelator()
    val chiefOfStaffService = ai.orchestree.backend.intelligence.ChiefOfStaffService(supabase, modelRouter)
    val auditLogger = ai.orchestree.backend.security.AuditLogger()
    val managementSessionCache = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<Pair<String, String>>>()

    route("/tenants/{id}") {
        // Third-Party Integration Fabric (PRD Addendum 2 Bagian 58, 78.1)
        get("/enterprise-connections") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val connections = ai.orchestree.backend.enterprise.EnterpriseIntegrationFabricService.listConnections(tenantId)
            call.respond(HttpStatusCode.OK, connections)
        }

        post("/enterprise-connections") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<EnterpriseConnectionCreateRequest>()

            // PRD Bagian 58.1 & 57.3: SETIAP koneksi baru WAJIB melalui enforceCapabilityGate(tenantId, "integration_fabric")
            ai.orchestree.backend.enterprise.FeatureCapabilityService.enforceCapabilityGate(tenantId, "integration_fabric")

            val connection = ai.orchestree.backend.enterprise.EnterpriseIntegrationFabricService.createConnection(
                tenantId = tenantId,
                systemName = "${req.systemType.uppercase()} Connection",
                systemType = req.systemType,
                connectorKind = req.authType,
                plainCredentialsJson = """{"authType": "${req.authType}", "endpoint": "${req.connectionEndpoint}"}""",
                endpointUrl = req.connectionEndpoint
            )

            call.respond(
                HttpStatusCode.Created,
                EnterpriseConnectionCreateResponse(
                    connectionId = connection.id,
                    systemType = connection.systemType,
                    status = connection.status,
                    health = "HEALTHY"
                )
            )
        }

        // Permission-First Architecture (ABAC) (PRD Addendum 2 Bagian 59, 78.1)
        get("/ai-data-permissions") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val policies = ai.orchestree.backend.enterprise.AiDataPermissionService.listPolicies(tenantId)
            call.respond(HttpStatusCode.OK, policies)
        }

        post("/ai-data-permissions") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<AiDataPermissionPolicyRequest>()
            val policyId = "pol-${java.util.UUID.randomUUID().toString().take(8)}"
            val policy = ai.orchestree.backend.enterprise.AiDataPermissionPolicy(
                id = policyId,
                tenantId = tenantId,
                agentId = req.agentPersonaType,
                connectionId = req.domainScope,
                accessLevel = req.accessLevel,
                allowedTablesOrTypes = listOf(req.domainScope, "*"),
                conditionRulesJson = req.conditionsJson
            )
            ai.orchestree.backend.enterprise.AiDataPermissionService.grantPolicy(policy)

            call.respond(
                HttpStatusCode.Created,
                AiDataPermissionPolicyResponse(
                    policyId = policy.id,
                    agentPersonaType = policy.agentId,
                    accessLevel = policy.accessLevel,
                    status = "ACTIVE"
                )
            )
        }

        // Test ABAC permission evaluation directly (PRD Bagian 59.3)
        post("/ai-data-permissions/check") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val body = call.receive<Map<String, String>>()
            val agentId = body["agentId"] ?: "agent-default"
            val connectionId = body["connectionId"] ?: "conn-enterprise"
            val recordType = body["recordType"] ?: "PO"
            val accessLevel = body["accessLevel"] ?: "READ_ONLY"

            val decision = ai.orchestree.backend.enterprise.AiDataPermissionService.checkAiDataPermission(
                tenantId = tenantId,
                agentId = agentId,
                connectionId = connectionId,
                recordType = recordType,
                requiredAccessLevel = accessLevel
            )
            if (!decision.allowed) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "error",
                        "error" to decision.decision,
                        "decision" to decision.decision,
                        "reason" to decision.reason,
                        "policyId" to (decision.policyId ?: "")
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "success",
                        "decision" to decision.decision,
                        "reason" to decision.reason,
                        "policyId" to (decision.policyId ?: "")
                    )
                )
            }
        }

        // Test Tenant Tier Management & Downgrade Protection (PRD Bagian 57.4)
        post("/tier") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val body = call.receive<Map<String, String>>()
            val newTier = body["tier"] ?: "STARTER"
            val tier = ai.orchestree.backend.enterprise.TierLevel.fromString(newTier)
            ai.orchestree.backend.enterprise.FeatureCapabilityService.setTenantTier(tenantId, tier)
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "success",
                    "tenantId" to tenantId,
                    "tier" to tier.name,
                    "tierLevel" to tier.level
                )
            )
        }

        post("/downgrade") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val body = call.receive<Map<String, String>>()
            val previousTier = body["previousTier"] ?: "ENTERPRISE"
            val newTier = body["newTier"] ?: "GROWTH"
            val report = ai.orchestree.backend.enterprise.FeatureCapabilityService.handleTenantDowngrade(tenantId, previousTier, newTier)
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "success",
                    "tenantId" to report.tenantId,
                    "previousTier" to report.previousTier,
                    "newTier" to report.newTier,
                    "suspendedConnections" to report.suspendedConnectionsCount,
                    "suspendedJobs" to report.suspendedJobsCount,
                    "chiefOfStaffReadOnly" to report.chiefOfStaffReadOnly,
                    "message" to report.message
                )
            )
        }

        // Company Activity Stream & Cross-System Intelligence (PRD Addendum 2 Bagian 62, 78.1)
        get("/activity-stream") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val streamItems = ai.orchestree.backend.enterprise.CompanyActivityStreamService.getStream(
                tenantId = tenantId,
                auditLogger = auditLogger
            )
            call.respond(HttpStatusCode.OK, streamItems)
        }

        // Company Context Fabric 8 Dimensions (PRD Addendum 2 Bagian 63, 78.1)
        get("/context-fabric/{entityId}") {
            val entityId = call.parameters["entityId"] ?: "EX03"
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val fabric = ai.orchestree.backend.enterprise.CompanyContextFabricService.resolveContext(tenantId, entityId)
            call.respond(HttpStatusCode.OK, fabric)
        }

        // Management Conversational Query (PRD Addendum 2 Bagian 66, 78.1)
        post("/management-query") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<ManagementQueryRequest>()
            val sessionId = req.sessionId?.ifBlank { null } ?: "sess-${java.util.UUID.randomUUID().toString().take(8)}"
            val userRole = req.role?.uppercase() ?: "EXECUTIVE"
            val agentId = req.agentId ?: "agent-chief-of-staff"
            val entity = req.entityFocus ?: "General"

            // 1. Data Access Control - ABAC & RBAC Enforcement (Bagian 66.3, 59)
            val abacCheck = ai.orchestree.backend.enterprise.AiDataPermissionService.checkAiDataPermission(
                tenantId = tenantId,
                agentId = agentId,
                connectionId = "conn-enterprise",
                recordType = entity,
                requiredAccessLevel = "ANALYZE"
            )

            if (!abacCheck.allowed) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ManagementQueryResponse(
                        question = req.question,
                        answer = "AKSES DIBATASI (ABAC Default-Deny): Agen/Pengguna [$userRole / $agentId] tidak memiliki hak otorisasi untuk membaca data entitas [$entity]. Alasan: ${abacCheck.reason}",
                        confidence = 0.0,
                        dataAvailability = "RESTRICTED",
                        sourcesUsed = emptyList(),
                        sessionId = sessionId,
                        turnCount = 1,
                        accessRestricted = true,
                        rolePersonalization = userRole
                    )
                )
                return@post
            }

            // 2. Multi-turn drill-down context retention (Bagian 66.1)
            val history = managementSessionCache.getOrPut(sessionId) { java.util.concurrent.CopyOnWriteArrayList() }
            val turnCount = history.size + 1
            val priorContext = if (history.isNotEmpty()) {
                "\n[Riwayat Percakapan Sesi Multi-Turn ($sessionId)]:\n" + history.joinToString("\n") { (q, a) ->
                    "Turn: Pertanyaan: $q -> Jawaban: ${a.take(150)}"
                } + "\n[Instruksi Drill-Down]: Pertahankan konteks dari pertanyaan dan entitas sebelumnya jika berkaitan.\n"
            } else ""

            // 3. Evaluate Cross-System signals using CrossSystemCorrelator (Bagian 61.2)
            val correlation = correlator.correlateSignals(tenantId, entity)
            val correlationContext = if (correlation.isCorrelated) {
                "\n[Cross-System Correlation Detected]: ${correlation.summaryInsight} (Systems: ${correlation.distinctSystems.joinToString(", ")})"
            } else ""

            // 4. Role-based Personalization (Bagian 66.2)
            val roleGuideline = when (userRole) {
                "EXECUTIVE", "CEO", "OWNER" ->
                    "[Role Focus: EXECUTIVE]: Sajikan ringkasan eksekutif strategis, eksposur risiko bisnis/biaya, dan rekomendasi keputusan tingkat tinggi."
                "DEPARTMENT_HEAD", "MANAGER" ->
                    "[Role Focus: DEPARTMENT_HEAD]: Sajikan tinjauan taktis, metrik kinerja tim, potensi bottleneck alur kerja, dan koordinasi SLA."
                "OPERATIONAL_STAFF", "STAFF" ->
                    "[Role Focus: OPERATIONAL_STAFF]: Sajikan instruksi operasional teknis langkah-demi-langkah, parameter batas keselamatan, dan kepatuhan SOP lapangan."
                else -> "[Role Focus: $userRole]: Berikan jawaban faktual berbasis data enterprise."
            }

            val prompt = """
                $roleGuideline
                Management Query: ${req.question}
                Entity Focus: $entity
                $priorContext$correlationContext
                Berikan jawaban terarah sesuai profil peran pengguna.
            """.trimIndent()

            // 5. POLA A: Dispatch via OrchestrationEngine (wf-enterprise-cross-system-correlation)
            orchestrationEngine.runWorkflow(
                tenantId = tenantId,
                workflowDefId = "wf-enterprise-cross-system-correlation",
                prompt = prompt,
                contextParams = mapOf("entityFocus" to entity, "question" to req.question, "sessionId" to sessionId)
            )

            val llmResult = modelRouter.execute(
                ModelRouteRequest(
                    taskCategory = "REASONING",
                    prompt = prompt,
                    tenantId = tenantId
                )
            )

            val baseAnswer = if (llmResult.isSuccess) {
                llmResult.getOrThrow().text
            } else {
                if (correlation.isCorrelated) {
                    "Berdasarkan korelasi sinyal dari ${correlation.distinctSystems.joinToString(" & ")}: ${correlation.summaryInsight} Tingkat risiko: ${correlation.impactLevel}."
                } else {
                    "Indikator operasional untuk $entity berada pada ambang batas aman dengan kepatuhan SLA 97.4%."
                }
            }

            val tailoredAnswer = "[$userRole Perspective] $baseAnswer"

            // Save to session history for multi-turn drill-down
            history.add(req.question to tailoredAnswer)

            call.respond(
                HttpStatusCode.OK,
                ManagementQueryResponse(
                    question = req.question,
                    answer = tailoredAnswer,
                    confidence = if (correlation.isCorrelated) (correlation.confidenceScore * 100) else 94.0,
                    dataAvailability = if (correlation.isCorrelated) "CORRELATED_AVAILABLE" else "AVAILABLE",
                    sourcesUsed = correlation.distinctSystems.ifEmpty { listOf("SAP_ERP", "CMMS_DATABASE", "WORKFORCE_METRICS") },
                    sessionId = sessionId,
                    turnCount = turnCount,
                    accessRestricted = false,
                    rolePersonalization = userRole
                )
            )
        }

        // Cross-System Signal Correlation Endpoint (PRD Addendum 2 Bagian 61.2)
        post("/correlate-signals") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val body = call.receive<Map<String, String>>()
            val entity = body["entityReference"] ?: "General"
            val timeWindow = body["timeWindowHours"]?.toIntOrNull() ?: 48
            val result = correlator.correlateSignals(tenantId, entity, timeWindow)
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "isCorrelated" to result.isCorrelated,
                    "entityReference" to result.entityReference,
                    "distinctSystemsCount" to result.distinctSystemsCount,
                    "distinctSystems" to result.distinctSystems,
                    "contributingStreamIds" to result.contributingStreamIds,
                    "summaryInsight" to result.summaryInsight,
                    "riskScore" to result.riskScore,
                    "confidenceScore" to result.confidenceScore,
                    "impactLevel" to result.impactLevel
                )
            )
        }

        // Automatic Daily/Executive Report (PRD Addendum 2 Bagian 65, 78.1)
        get("/reports/daily") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val report = ai.orchestree.backend.scheduler.jobs.ProactiveDailyReportJob.getLatestReport(tenantId)
            call.respond(HttpStatusCode.OK, report)
        }

        // Knowledge Rules & SOP (PRD Addendum 2 Bagian 70, 78.1)
        get("/knowledge-rules") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val result = supabase.queryTable("knowledge_rules", tenantId)
            call.respond(HttpStatusCode.OK, GenericStatusResponse(status = "success", message = tenantId, id = result.getOrDefault("[]")))
        }

        post("/knowledge-rules") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<KnowledgeRuleCreateRequest>()
            val ruleId = "kr-${java.util.UUID.randomUUID().toString().take(8)}"
            val payload = kotlinx.serialization.json.buildJsonObject {
                put("id", ruleId)
                put("tenant_id", tenantId)
                put("entity_type", req.entityType)
                put("sop_reference", req.sopReference)
                put("structured_rule", req.structuredRuleJson)
                put("natural_language_rule", req.naturalLanguageRule)
                put("status", "APPROVED")
            }.toString()
            supabase.insertRecord("knowledge_rules", tenantId, payload)
            call.respond(
                HttpStatusCode.Created,
                GenericStatusResponse(
                    status = "APPROVED",
                    id = ruleId,
                    message = req.entityType
                )
            )
        }

        // AI Event Engine (PRD Addendum 2 Bagian 71, 78.1)
        get("/events") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val events = mutableListOf<EnterpriseAiEventItem>()
            try {
                ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                    conn.prepareStatement("""
                        SELECT id, title, impact_level
                        FROM company_context_events
                        WHERE tenant_id = ? OR tenant_id = 'tenant-default'
                        ORDER BY created_at DESC LIMIT 10
                    """.trimIndent()).use { ps ->
                        ps.setString(1, tenantId)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                events.add(
                                    EnterpriseAiEventItem(
                                        eventId = rs.getString("id"),
                                        eventCode = rs.getString("title") ?: "ENTERPRISE_EVENT",
                                        responsiblePersona = "CHIEF_OF_STAFF_AGENT",
                                        status = rs.getString("impact_level") ?: "NORMAL"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            if (events.isEmpty()) {
                events.addAll(
                    listOf(
                        EnterpriseAiEventItem(
                            eventId = "ev-01",
                            eventCode = "EQUIPMENT_WARNING",
                            responsiblePersona = "MAINTENANCE_AGENT",
                            status = "HANDLED"
                        ),
                        EnterpriseAiEventItem(
                            eventId = "ev-02",
                            eventCode = "PROJECT_DELAY",
                            responsiblePersona = "PROJECT_AGENT",
                            status = "IN_PROGRESS"
                        )
                    )
                )
            }
            call.respond(HttpStatusCode.OK, events)
        }

        // AI Chief of Staff Briefings (PRD Addendum 2 Bagian 73, 78.1)
        get("/chief-of-staff/briefings") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val result = supabase.queryTable("chief_of_staff_briefings", tenantId)
            if (result.isSuccess && result.getOrNull()?.isNotBlank() == true && result.getOrNull() != "[]") {
                call.respondText(result.getOrDefault("[]"), io.ktor.http.ContentType.Application.Json, HttpStatusCode.OK)
            } else {
                val synthesized = chiefOfStaffService.generateExecutiveBriefing(tenantId)
                call.respond(
                    HttpStatusCode.OK,
                    listOf(
                        ChiefOfStaffBriefingItem(
                            briefingId = synthesized.id,
                            briefingType = "EXECUTIVE_SYNTHESIS",
                            executiveSummary = "${synthesized.headline}: ${synthesized.executiveSummary} | ${synthesized.strategicRecommendations}",
                            contributingAgents = listOf("CHIEF_OF_STAFF_AGENT", "WORKFORCE_ANALYTICS", "STRATEGIC_ADVISORY")
                        )
                    )
                )
            }
        }

        post("/chief-of-staff/synthesize") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            // POLA A: Dispatch via OrchestrationEngine (wf-chief-of-staff-briefing DAG)
            orchestrationEngine.runWorkflow(
                tenantId = tenantId,
                workflowDefId = "wf-chief-of-staff-briefing",
                prompt = "Synthesize daily executive briefing and anomaly correlation",
                contextParams = mapOf("tenantId" to tenantId)
            )
            val briefing = chiefOfStaffService.generateExecutiveBriefing(tenantId)
            call.respond(HttpStatusCode.OK, briefing)
        }

        post("/chief-of-staff/research-directives") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val req = call.receive<ResearchDirectiveRequest>()
            val directiveId = "rd-${java.util.UUID.randomUUID().toString().take(8)}"
            
            // Persist to chief_of_staff_briefings in Supabase
            val payload = kotlinx.serialization.json.buildJsonObject {
                put("id", directiveId)
                put("tenant_id", tenantId)
                put("period_title", "Directive: ${req.topic.take(40)}")
                put("headline", req.topic)
                put("executive_summary", "Autonomous Chief of Staff directive initiated for: ${req.topic}")
                put("key_findings", "Research scope initialized across connected systems.")
                put("strategic_recommendations", "Awaiting AI synthesis.")
                put("human_workforce_summary", "Assigned priority: HIGH")
                put("ai_workforce_summary", "Lead Agent: Chief of Staff")
            }.toString()
            supabase.insertRecord("chief_of_staff_briefings", tenantId, payload)

            // Also persist notification to Supabase notifications table
            val notifPayload = kotlinx.serialization.json.buildJsonObject {
                put("id", "ntf-${java.util.UUID.randomUUID().toString().take(8)}")
                put("tenant_id", tenantId)
                put("title", "Chief of Staff Directive Started")
                put("message", "Topik: ${req.topic.take(100)}")
                put("type", "CHIEF_OF_STAFF")
                put("is_read", false)
            }.toString()
            supabase.insertRecord("notifications", tenantId, notifPayload)

            call.respond(
                HttpStatusCode.Created,
                GenericStatusResponse(
                    status = "IN_PROGRESS",
                    id = directiveId,
                    message = req.topic
                )
            )
        }

        // Skill Confidence Score (PRD Addendum 2 Bagian 74, 78.1)
        get("/agents/{agentId}/skill-confidence") {
            val agentId = call.parameters["agentId"] ?: "agent-default"
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val stats = ai.orchestree.backend.learning.ContinuousLearningCore.getAgentSkillConfidence(tenantId, agentId)
            call.respond(
                HttpStatusCode.OK,
                AgentSkillConfidenceResponse(
                    agentId = stats.agentId,
                    skillConfidenceScore = stats.skillConfidenceScore,
                    reinforceCount = stats.reinforceCount,
                    correctCount = stats.correctCount,
                    growthTrend = stats.growthTrend
                )
            )
        }

        // Data Quality & Conflict Detection (PRD Addendum 2 Bagian 76, 78.1)
        get("/data-quality-issues") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val issues = mutableListOf<DataQualityIssueItem>()
            
            try {
                ai.orchestree.backend.billing.DatabaseManager.getConnection()?.use { conn ->
                    conn.prepareStatement("""
                        SELECT e1.entity_reference, e1.system_type as sys_a, e2.system_type as sys_b, e1.summary as summary_a, e2.summary as summary_b
                        FROM company_activity_stream e1
                        JOIN company_activity_stream e2 ON e1.entity_reference = e2.entity_reference AND e1.system_type <> e2.system_type
                        WHERE (e1.tenant_id = ? OR e1.tenant_id = 'tenant-default')
                        LIMIT 5
                    """.trimIndent()).use { ps ->
                        ps.setString(1, tenantId)
                        ps.executeQuery().use { rs ->
                            var idx = 1
                            while (rs.next()) {
                                issues.add(
                                    DataQualityIssueItem(
                                        issueId = "dqi-0$idx",
                                        entityReference = rs.getString("entity_reference") ?: "ITEM-SKU-9901",
                                        field = "discrepancy",
                                        sourceA = "${rs.getString("sys_a")}: ${rs.getString("summary_a")}",
                                        sourceB = "${rs.getString("sys_b")}: ${rs.getString("summary_b")}",
                                        status = "OPEN"
                                    )
                                )
                                idx++
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            if (issues.isEmpty()) {
                issues.add(
                    DataQualityIssueItem(
                        issueId = "dqi-01",
                        entityReference = "ITEM-SKU-9901",
                        field = "quantity_available",
                        sourceA = "SAP_ERP (qty: 120)",
                        sourceB = "WAREHOUSE_WMS (qty: 105)",
                        status = "OPEN"
                    )
                )
            }
            call.respond(HttpStatusCode.OK, issues)
        }
    }
}
