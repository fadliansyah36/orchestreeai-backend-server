package ai.orchestree.backend.database.repositories.workforce

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class ProactiveCollaborationScope(
    val id: String = UUID.randomUUID().toString(),
    val staffId: String,
    val scopeType: String, // "department_scoped" or "executive_full_summary"
    val collaboratingAiJobTitleIds: List<String>? = null,
    val collaboratingAgentNames: List<String> = emptyList(),
    val departmentCategoryCode: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class DepartmentAiCollaborationMapping(
    val departmentCategoryCode: String,
    val aiJobCodes: List<String>,
    val aiJobNames: List<String>
)

class ProactiveCollaborationScopeRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val staffProfileRepo: StaffProfileRepository = StaffProfileRepository()
) {
    private val logger = LoggerFactory.getLogger(ProactiveCollaborationScopeRepository::class.java)
    private val memoryScopes = ConcurrentHashMap<String, ProactiveCollaborationScope>()

    // Official Department Category to AI Collaboration Mapping (14 categories from Fase 91.A / V113)
    private val departmentMappings = mapOf(
        "sales" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "sales",
            aiJobCodes = listOf("sales", "marketing", "crm_customer_success"),
            aiJobNames = listOf("AI Sales Agent", "AI Marketing Agent", "AI CRM & Customer Success")
        ),
        "marketing" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "marketing",
            aiJobCodes = listOf("marketing", "sales", "company_intelligence"),
            aiJobNames = listOf("AI Marketing Agent", "AI Sales Agent", "AI Market Intelligence")
        ),
        "customer_service" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "customer_service",
            aiJobCodes = listOf("customer_service", "crm_customer_success"),
            aiJobNames = listOf("AI Customer Service Agent", "AI CRM Agent")
        ),
        "hr" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "hr",
            aiJobCodes = listOf("hr_recruitment"),
            aiJobNames = listOf("AI HR & Talent Acquisition Agent")
        ),
        "finance" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "finance",
            aiJobCodes = listOf("finance"),
            aiJobNames = listOf("AI Finance & Accounting Agent")
        ),
        "operations" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "operations",
            aiJobCodes = listOf("operations", "procurement"),
            aiJobNames = listOf("AI Operations Agent", "AI Procurement Agent")
        ),
        "procurement" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "procurement",
            aiJobCodes = listOf("procurement", "operations", "finance"),
            aiJobNames = listOf("AI Procurement Agent", "AI Operations Agent", "AI Finance Agent")
        ),
        "project" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "project",
            aiJobCodes = listOf("project", "task_workflow"),
            aiJobNames = listOf("AI Project Management Agent", "AI Task Workflow Agent")
        ),
        "it_engineering" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "it_engineering",
            aiJobCodes = listOf("knowledge_document", "task_workflow"),
            aiJobNames = listOf("AI IT & Tech Knowledge Agent", "AI Task Workflow Agent")
        ),
        "legal" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "legal",
            aiJobCodes = listOf("knowledge_document"),
            aiJobNames = listOf("AI Legal & Compliance Knowledge Agent")
        ),
        "research" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "research",
            aiJobCodes = listOf("research", "company_intelligence"),
            aiJobNames = listOf("AI Research Agent", "AI Market Intelligence Agent")
        ),
        "hse" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "hse",
            aiJobCodes = listOf("operations"),
            aiJobNames = listOf("AI HSE Operations Agent")
        ),
        "quality" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "quality",
            aiJobCodes = listOf("operations", "knowledge_document"),
            aiJobNames = listOf("AI Quality Assurance Agent", "AI SOP Knowledge Agent")
        ),
        "executive" to DepartmentAiCollaborationMapping(
            departmentCategoryCode = "executive",
            aiJobCodes = listOf("chief_of_staff", "reporting", "company_intelligence"),
            aiJobNames = listOf("AI Chief of Staff", "AI Executive Reporting Agent", "AI Company Intelligence")
        )
    )

    companion object {
        val defaultInstance by lazy { ProactiveCollaborationScopeRepository() }
    }

    fun getMappingForDepartment(deptCode: String): DepartmentAiCollaborationMapping? {
        val normalized = deptCode.lowercase().replace("dept-", "").replace(" ", "_")
        return departmentMappings[normalized] ?: departmentMappings.entries.firstOrNull {
            normalized.contains(it.key) || it.key.contains(normalized)
        }?.value
    }

    /**
     * LANGKAH 2: Logic Penentuan Scope Otomatis Saat Pendaftaran
     * ATURAN TEGAS:
     * - HANYA owner/direksi mendapat executive_full_summary (Chief of Staff cross-department synthesis).
     * - Staff human departemen HANYA berkolaborasi dengan AI Agent dalam scope departemennya.
     */
    suspend fun determineProactiveScope(
        staffId: String,
        explicitJobLevel: String? = null,
        explicitDepartment: String? = null
    ): ProactiveCollaborationScope {
        // Cek profile staff
        val staffProfile = staffProfileRepo.findById(staffId)
        val jobLevelCandidate = explicitJobLevel?.lowercase()
            ?: staffProfile?.jobTitle?.lowercase()
            ?: ""

        val isExecutiveTier = jobLevelCandidate.contains("owner") ||
                jobLevelCandidate.contains("direksi") ||
                jobLevelCandidate.contains("director") ||
                jobLevelCandidate.contains("ceo") ||
                jobLevelCandidate.contains("komisaris") ||
                jobLevelCandidate.contains("c-level") ||
                jobLevelCandidate.contains("executive")

        if (isExecutiveTier) {
            val scope = ProactiveCollaborationScope(
                staffId = staffId,
                scopeType = "executive_full_summary",
                collaboratingAiJobTitleIds = null,
                collaboratingAgentNames = listOf("AI Chief of Staff (Ringkasan Lintas Seluruh Departemen)"),
                departmentCategoryCode = "executive"
            )
            saveScope(scope)
            return scope
        } else {
            val deptCandidate = explicitDepartment
                ?: staffProfile?.departmentId
                ?: "sales"

            val mapping = getMappingForDepartment(deptCandidate)
            require(mapping != null && mapping.aiJobCodes.isNotEmpty()) {
                "Departemen staff ini belum memiliki mapping AI Agent kolaborasi - " +
                        "hubungi Admin untuk konfigurasi department_ai_collaboration_mapping"
            }

            val scope = ProactiveCollaborationScope(
                staffId = staffId,
                scopeType = "department_scoped",
                collaboratingAiJobTitleIds = mapping.aiJobCodes,
                collaboratingAgentNames = mapping.aiJobNames,
                departmentCategoryCode = mapping.departmentCategoryCode
            )
            saveScope(scope)
            return scope
        }
    }

    fun saveScope(scope: ProactiveCollaborationScope) {
        memoryScopes[scope.staffId] = scope
        logger.info("Saved proactive collaboration scope for staff=${scope.staffId}: type=${scope.scopeType}, agents=${scope.collaboratingAiJobTitleIds}")
    }

    suspend fun getScopeForStaff(staffId: String): ProactiveCollaborationScope {
        return memoryScopes[staffId] ?: determineProactiveScope(staffId)
    }

    suspend fun get(staffId: String): ProactiveCollaborationScope = getScopeForStaff(staffId)

    fun getAllScopes(): List<ProactiveCollaborationScope> {
        return memoryScopes.values.toList()
    }
}
