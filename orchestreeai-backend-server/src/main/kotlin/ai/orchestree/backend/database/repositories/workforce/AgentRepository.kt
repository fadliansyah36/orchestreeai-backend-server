package ai.orchestree.backend.database.repositories.workforce

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.serialization.Serializable

@Serializable
data class AgentModel(
    val id: String,
    val tenantId: String,
    val name: String,
    val role: String,
    val personaCode: String,
    val departmentId: String? = null,
    val status: String = "ACTIVE",
    val skills: List<String> = emptyList()
)

class AgentRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val agentsStore = java.util.concurrent.ConcurrentHashMap<String, AgentModel>()

    init {
        agentsStore["agent-radar-competitor"] = AgentModel(
            id = "agent-radar-competitor",
            tenantId = "tenant-sample-001",
            name = "Radar Intelijen Kompetitor",
            role = "Market Intelligence Specialist",
            personaCode = "RADAR",
            departmentId = "dept-salesmarketing"
        )
        agentsStore["agent-chief-of-staff"] = AgentModel(
            id = "agent-chief-of-staff",
            tenantId = "tenant-sample-001",
            name = "Chief of Staff AI",
            role = "Executive Operations Coordinator",
            personaCode = "COS",
            departmentId = "dept-executive"
        )
        agentsStore["agent-orchestrator"] = AgentModel(
            id = "agent-orchestrator",
            tenantId = "tenant-sample-001",
            name = "Autonomous Orchestrator",
            role = "Core Workflow Engine",
            personaCode = "ORCH",
            departmentId = "dept-ai-ops"
        )
    }

    suspend fun getAgents(tenantId: String): Result<String> {
        return supabase.queryTable("agents", tenantId)
    }

    suspend fun get(agentId: String): AgentModel? {
        return agentsStore[agentId] ?: AgentModel(
            id = agentId,
            tenantId = "tenant-sample-001",
            name = "AI Agent $agentId",
            role = "Autonomous Worker",
            personaCode = "AGENT",
            departmentId = "dept-ai-ops"
        )
    }

    fun countActiveByTenant(tenantId: String): Int {
        return agentsStore.values.count { it.tenantId == tenantId && it.status.uppercase() == "ACTIVE" }
    }

    fun findByTenant(tenantId: String): List<AgentModel> {
        return agentsStore.values.filter { it.tenantId == tenantId && it.status.uppercase() == "ACTIVE" }
    }

    fun save(agent: AgentModel) {
        agentsStore[agent.id] = agent
    }

    fun delete(agentId: String): Boolean {
        val existing = agentsStore[agentId]
        if (existing != null) {
            agentsStore[agentId] = existing.copy(status = "DEACTIVATED")
            return true
        }
        return false
    }

    companion object {
        val defaultInstance: AgentRepository by lazy { AgentRepository() }
    }
}
