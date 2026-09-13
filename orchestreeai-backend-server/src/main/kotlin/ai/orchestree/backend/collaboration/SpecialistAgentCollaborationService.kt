package ai.orchestree.backend.collaboration

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object SpecialistPersonas {
    const val MAINTENANCE_SPECIALIST = "MAINTENANCE_SPECIALIST"
    const val FLEET_LOGISTICS_SPECIALIST = "FLEET_LOGISTICS_SPECIALIST"
    const val HSE_COMPLIANCE_SPECIALIST = "HSE_COMPLIANCE_SPECIALIST"
    const val PROJECT_CONTROL_SPECIALIST = "PROJECT_CONTROL_SPECIALIST"
    const val ENGINEERING_SPECIALIST = "ENGINEERING_SPECIALIST"
    const val PROCUREMENT_SPECIALIST = "PROCUREMENT_SPECIALIST"
    const val FINANCE_CONTROLLER = "FINANCE_CONTROLLER"
    const val OPERATIONS_LEAD = "OPERATIONS_LEAD"
}

@Serializable
data class ProjectHealthReport(
    val id: String = "phs-" + UUID.randomUUID().toString().take(8),
    val tenantId: String,
    val projectId: String,
    val projectName: String,
    val scheduleScore: Double,
    val budgetScore: Double,
    val riskScore: Double,
    val workforceScore: Double,
    val compositeHealthScore: Double,
    val healthStatus: String, // EXCELLENT, ON_TRACK, NEEDS_ATTENTION, CRITICAL
    val blockersCount: Int,
    val summaryDetails: String = "",
    val evaluatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class MultiAgentCollaborationRequest(
    val tenantId: String,
    val title: String,
    val initiatingAgentId: String,
    val initiatingPersona: String,
    val targetEntityReference: String,
    val eventDetails: String,
    val requestedPersonas: List<String> = emptyList()
)

@Serializable
data class AgentContribution(
    val persona: String,
    val agentName: String,
    val recommendation: String,
    val urgency: String = "MEDIUM"
)

@Serializable
data class MultiAgentCollaborationSession(
    val id: String = "collab-" + UUID.randomUUID().toString().take(8),
    val tenantId: String,
    val title: String,
    val initiatingAgentId: String,
    val initiatingPersona: String,
    val targetEntityReference: String,
    val participatingAgents: List<AgentContribution> = emptyList(),
    val status: String = "CONSENSUS_REACHED", // INITIATED, IN_DISCUSSION, CONSENSUS_REACHED, EXECUTING, COMPLETED
    val consensusSummary: String,
    val actionPlan: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class SpecialistAgentCollaborationService {
    private val logger = LoggerFactory.getLogger(SpecialistAgentCollaborationService::class.java)
    private val inMemoryHealthReports = ConcurrentHashMap<String, ProjectHealthReport>()
    private val inMemorySessions = ConcurrentHashMap<String, CopyOnWriteArrayList<MultiAgentCollaborationSession>>()

    /**
     * PRD Bagian 72.3: Project Health Score Engine
     * Evaluates schedule, budget, risk, and workforce to produce an explainable composite score.
     */
    suspend fun evaluateProjectHealth(
        tenantId: String,
        projectId: String,
        projectName: String,
        scheduleScore: Double = 88.0,
        budgetScore: Double = 92.0,
        riskScore: Double = 85.0,
        workforceScore: Double = 90.0,
        blockersCount: Int = 0
    ): ProjectHealthReport = withContext(Dispatchers.IO) {
        val composite = ((scheduleScore * 0.30) + (budgetScore * 0.30) + (riskScore * 0.20) + (workforceScore * 0.20))
        val roundedComposite = (composite * 10).toInt() / 10.0

        val status = when {
            roundedComposite >= 90.0 -> "EXCELLENT"
            roundedComposite >= 75.0 -> "ON_TRACK"
            roundedComposite >= 60.0 -> "NEEDS_ATTENTION"
            else -> "CRITICAL"
        }

        val report = ProjectHealthReport(
            tenantId = tenantId,
            projectId = projectId,
            projectName = projectName,
            scheduleScore = scheduleScore,
            budgetScore = budgetScore,
            riskScore = riskScore,
            workforceScore = workforceScore,
            compositeHealthScore = roundedComposite,
            healthStatus = status,
            blockersCount = blockersCount,
            summaryDetails = "Proyek '$projectName' berstatus $status dengan skor komposit $roundedComposite%. Blocker aktif: $blockersCount."
        )

        inMemoryHealthReports["$tenantId:$projectId"] = report

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        INSERT INTO project_health_scores
                        (id, tenant_id, project_id, project_name, schedule_score, budget_score, 
                         risk_score, workforce_score, composite_health_score, health_status, blockers_count, evaluated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()).use { ps ->
                        ps.setString(1, report.id)
                        ps.setString(2, report.tenantId)
                        ps.setString(3, report.projectId)
                        ps.setString(4, report.projectName)
                        ps.setDouble(5, report.scheduleScore)
                        ps.setDouble(6, report.budgetScore)
                        ps.setDouble(7, report.riskScore)
                        ps.setDouble(8, report.workforceScore)
                        ps.setDouble(9, report.compositeHealthScore)
                        ps.setString(10, report.healthStatus)
                        ps.setInt(11, report.blockersCount)
                        ps.setLong(12, report.evaluatedAt)
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not persist project health score: ${e.message}")
            }
        }

        report
    }

    suspend fun getProjectHealth(tenantId: String, projectId: String): ProjectHealthReport? = withContext(Dispatchers.IO) {
        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        SELECT id, tenant_id, project_id, project_name, schedule_score, budget_score, 
                               risk_score, workforce_score, composite_health_score, health_status, blockers_count, evaluated_at
                        FROM project_health_scores
                        WHERE tenant_id = ? AND project_id = ?
                        ORDER BY evaluated_at DESC LIMIT 1
                    """.trimIndent()).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setString(2, projectId)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                return@withContext ProjectHealthReport(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id"),
                                    projectId = rs.getString("project_id"),
                                    projectName = rs.getString("project_name"),
                                    scheduleScore = rs.getDouble("schedule_score"),
                                    budgetScore = rs.getDouble("budget_score"),
                                    riskScore = rs.getDouble("risk_score"),
                                    workforceScore = rs.getDouble("workforce_score"),
                                    compositeHealthScore = rs.getDouble("composite_health_score"),
                                    healthStatus = rs.getString("health_status"),
                                    blockersCount = rs.getInt("blockers_count"),
                                    evaluatedAt = rs.getLong("evaluated_at")
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not query project_health_scores: ${e.message}")
            }
        }
        inMemoryHealthReports["$tenantId:$projectId"]
    }

    /**
     * PRD Bagian 72.4: Multi-Agent Collaboration Workflow
     * Coordinates multiple Specialist Agents to resolve complex industrial / operational issues.
     */
    suspend fun initiateCollaboration(request: MultiAgentCollaborationRequest): MultiAgentCollaborationSession = withContext(Dispatchers.IO) {
        val contributions = mutableListOf<AgentContribution>()

        val personasToEngage = if (request.requestedPersonas.isNotEmpty()) {
            request.requestedPersonas
        } else {
            listOf(
                request.initiatingPersona,
                SpecialistPersonas.MAINTENANCE_SPECIALIST,
                SpecialistPersonas.PROCUREMENT_SPECIALIST,
                SpecialistPersonas.OPERATIONS_LEAD
            ).distinct()
        }

        for (persona in personasToEngage) {
            when (persona) {
                SpecialistPersonas.MAINTENANCE_SPECIALIST -> contributions.add(
                    AgentContribution(
                        persona = persona,
                        agentName = "Maintenance Specialist Agent",
                        recommendation = "Isolasi peralatan ${request.targetEntityReference}, periksa segel mekanik dan vibrasi, lalu lakukan penggantian komponen aus sesuai SOP Maintenance.",
                        urgency = "HIGH"
                    )
                )
                SpecialistPersonas.PROCUREMENT_SPECIALIST -> contributions.add(
                    AgentContribution(
                        persona = persona,
                        agentName = "Procurement Specialist Agent",
                        recommendation = "Cek ketersediaan spare parts di warehouse. Buat PR/PO cepat untuk spare parts kritis terkait ${request.targetEntityReference}.",
                        urgency = "MEDIUM"
                    )
                )
                SpecialistPersonas.OPERATIONS_LEAD -> contributions.add(
                    AgentContribution(
                        persona = persona,
                        agentName = "Operations Lead Agent",
                        recommendation = "Alihkan aliran/beban kerja operasional ke jalur standby untuk meminimalkan dampak downtime pada target ${request.targetEntityReference}.",
                        urgency = "HIGH"
                    )
                )
                SpecialistPersonas.HSE_COMPLIANCE_SPECIALIST -> contributions.add(
                    AgentContribution(
                        persona = persona,
                        agentName = "HSE Compliance Agent",
                        recommendation = "Pastikan izin kerja (Work Permit) dan Lock-Out Tag-Out (LOTO) telah terverifikasi sebelum tim teknisi mengakses ${request.targetEntityReference}.",
                        urgency = "CRITICAL"
                    )
                )
                SpecialistPersonas.FLEET_LOGISTICS_SPECIALIST -> contributions.add(
                    AgentContribution(
                        persona = persona,
                        agentName = "Fleet Logistics Specialist Agent",
                        recommendation = "Jadwalkan unit armada pengganti dan koordinasikan rute logistik agar pengiriman material tidak terhambat.",
                        urgency = "MEDIUM"
                    )
                )
                else -> contributions.add(
                    AgentContribution(
                        persona = persona,
                        agentName = "$persona Agent",
                        recommendation = "Mendukung penanganan masalah ${request.targetEntityReference} dengan pengawasan standar operasional.",
                        urgency = "LOW"
                    )
                )
            }
        }

        val actionPlan = listOf(
            "1. LOTO & Safety Check: Verifikasi izin kerja dan isolasi energi pada ${request.targetEntityReference}.",
            "2. Load Rerouting: Alihkan beban operasi ke unit cadangan.",
            "3. Parts Expediting: Konfirmasi ketersediaan suku cadang dan terbitkan purchase order jika stok menipis.",
            "4. Component Overhaul: Pelaksanaan servis teknis dan kalibrasi pasca-perbaikan."
        )

        val consensusSummary = "Konsensus Tercapai antara ${contributions.size} Specialist Agents: Penanganan komprehensif untuk ${request.targetEntityReference} mencakup pengalihan beban operasional, verifikasi keselamatan kerja, dan percepatan penyediaan suku cadang teknis."

        val session = MultiAgentCollaborationSession(
            tenantId = request.tenantId,
            title = request.title,
            initiatingAgentId = request.initiatingAgentId,
            initiatingPersona = request.initiatingPersona,
            targetEntityReference = request.targetEntityReference,
            participatingAgents = contributions,
            status = "CONSENSUS_REACHED",
            consensusSummary = consensusSummary,
            actionPlan = actionPlan
        )

        val list = inMemorySessions.computeIfAbsent(request.tenantId) { CopyOnWriteArrayList() }
        list.add(0, session)

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        INSERT INTO multi_agent_collaborations
                        (id, tenant_id, title, initiating_agent_id, initiating_persona, target_entity_reference,
                         status, consensus_summary, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()).use { ps ->
                        ps.setString(1, session.id)
                        ps.setString(2, session.tenantId)
                        ps.setString(3, session.title)
                        ps.setString(4, session.initiatingAgentId)
                        ps.setString(5, session.initiatingPersona)
                        ps.setString(6, session.targetEntityReference)
                        ps.setString(7, session.status)
                        ps.setString(8, session.consensusSummary)
                        ps.setLong(9, session.createdAt)
                        ps.setLong(10, session.updatedAt)
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not insert multi_agent_collaboration: ${e.message}")
            }
        }

        session
    }

    suspend fun getCollaborations(tenantId: String): List<MultiAgentCollaborationSession> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MultiAgentCollaborationSession>()
        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        SELECT id, tenant_id, title, initiating_agent_id, initiating_persona, target_entity_reference,
                               status, consensus_summary, created_at, updated_at
                        FROM multi_agent_collaborations
                        WHERE tenant_id = ?
                        ORDER BY created_at DESC
                    """.trimIndent()).use { ps ->
                        ps.setString(1, tenantId)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                result.add(
                                    MultiAgentCollaborationSession(
                                        id = rs.getString("id"),
                                        tenantId = rs.getString("tenant_id"),
                                        title = rs.getString("title"),
                                        initiatingAgentId = rs.getString("initiating_agent_id"),
                                        initiatingPersona = rs.getString("initiating_persona"),
                                        targetEntityReference = rs.getString("target_entity_reference"),
                                        status = rs.getString("status"),
                                        consensusSummary = rs.getString("consensus_summary"),
                                        createdAt = rs.getLong("created_at"),
                                        updatedAt = rs.getLong("updated_at")
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not query multi_agent_collaborations: ${e.message}")
            }
        }

        if (result.isEmpty()) {
            return@withContext inMemorySessions[tenantId] ?: emptyList()
        }
        result
    }
}
