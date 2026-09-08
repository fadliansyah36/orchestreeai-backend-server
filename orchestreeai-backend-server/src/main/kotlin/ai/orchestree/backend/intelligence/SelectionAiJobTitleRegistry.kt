package ai.orchestree.backend.intelligence

import ai.orchestree.backend.database.repositories.workforce.ProactiveCollaborationScopeRepository
import kotlinx.serialization.Serializable

/**
 * PRD Master Bagian 81.2, 91.H & Fase 110 Bagian B (V20 & V113):
 * Standarisasi 15 Master AI Job Titles resmi untuk penugasan otomatis
 * berdasarkan domain_category hasil Data Understanding (Bagian C / H).
 */
@Serializable
data class MasterAiJobTitle(
    val id: String,
    val jobCode: String,
    val jobName: String,
    val iconKey: String,
    val relevantDepartmentCategory: String,
    val description: String
)

object SelectionAiJobTitleRegistry {

    val OFFICIAL_15_JOB_TITLES: List<MasterAiJobTitle> = listOf(
        MasterAiJobTitle(
            id = "job-chief-of-staff",
            jobCode = "CHIEF_OF_STAFF",
            jobName = "AI Chief of Staff (5 Bintang)",
            iconKey = "stars",
            relevantDepartmentCategory = "executive",
            description = "Mengoordinasikan seluruh Staff AI Agent, mensintesis Executive Briefing, riset & R&D lintas departemen"
        ),
        MasterAiJobTitle(
            id = "job-intelligence",
            jobCode = "COMPANY_INTELLIGENCE",
            jobName = "AI Company Intelligence",
            iconKey = "troubleshoot",
            relevantDepartmentCategory = "strategy",
            description = "Memantau kompetitor, tren pasar (World Monitor), sinyal prospek (Vibe Prospecting), korelasi lintas sistem"
        ),
        MasterAiJobTitle(
            id = "job-operations",
            jobCode = "OPERATIONS",
            jobName = "AI Operations",
            iconKey = "precision_manufacturing",
            relevantDepartmentCategory = "operations",
            description = "Memantau produksi, equipment, fleet, kualitas, downtime, efisiensi operasional harian"
        ),
        MasterAiJobTitle(
            id = "job-sales",
            jobCode = "SALES",
            jobName = "AI Sales",
            iconKey = "point_of_sale",
            relevantDepartmentCategory = "sales",
            description = "Discovery, rekomendasi produk, upsell/cross-sell, objection handling, closing"
        ),
        MasterAiJobTitle(
            id = "job-customer-service",
            jobCode = "CUSTOMER_SERVICE",
            jobName = "AI Customer Service",
            iconKey = "support_agent",
            relevantDepartmentCategory = "customer_service",
            description = "Menjawab FAQ, menangani komplain, refund, return, warranty, eskalasi ke manusia"
        ),
        MasterAiJobTitle(
            id = "job-hr",
            jobCode = "HR_RECRUITMENT",
            jobName = "AI HR & Recruitment",
            iconKey = "badge",
            relevantDepartmentCategory = "hr",
            description = "Analisis kebutuhan tenaga kerja, monitoring kehadiran, rekrutmen, employee data dari HRIS"
        ),
        MasterAiJobTitle(
            id = "job-finance",
            jobCode = "FINANCE",
            jobName = "AI Finance",
            iconKey = "account_balance",
            relevantDepartmentCategory = "finance",
            description = "Cash flow analysis, AR/AP, budget variance, revenue forecasting"
        ),
        MasterAiJobTitle(
            id = "job-marketing",
            jobCode = "MARKETING",
            jobName = "AI Marketing",
            iconKey = "campaign",
            relevantDepartmentCategory = "marketing",
            description = "Membuat & menjalankan campaign, content generation, brand voice, atribusi revenue"
        ),
        MasterAiJobTitle(
            id = "job-knowledge",
            jobCode = "KNOWLEDGE_DOCUMENT",
            jobName = "AI Knowledge & Document",
            iconKey = "menu_book",
            relevantDepartmentCategory = "legal",
            description = "Mengelola Company Brain, ekstraksi dokumen (SOP/manual/kontrak), akurasi pengetahuan perusahaan"
        ),
        MasterAiJobTitle(
            id = "job-task-workflow",
            jobCode = "TASK_WORKFLOW",
            jobName = "AI Task & Workflow",
            iconKey = "account_tree",
            relevantDepartmentCategory = "operations",
            description = "Membuat task otomatis, memonitor closed-loop, mengelola papan kerja Human+AI"
        ),
        MasterAiJobTitle(
            id = "job-crm-success",
            jobCode = "CRM_CUSTOMER_SUCCESS",
            jobName = "AI CRM & Customer Success",
            iconKey = "loyalty",
            relevantDepartmentCategory = "sales",
            description = "Mengelola relasi pelanggan jangka panjang, retensi, kesehatan akun, reaktivasi"
        ),
        MasterAiJobTitle(
            id = "job-procurement",
            jobCode = "PROCUREMENT",
            jobName = "AI Procurement",
            iconKey = "shopping_cart_checkout",
            relevantDepartmentCategory = "procurement",
            description = "Memantau PO, RFQ, supplier, delivery, stok kritikal"
        ),
        MasterAiJobTitle(
            id = "job-project",
            jobCode = "PROJECT",
            jobName = "AI Project",
            iconKey = "engineering",
            relevantDepartmentCategory = "project",
            description = "Project Health Score, jadwal, biaya, manpower, deteksi risiko keterlambatan"
        ),
        MasterAiJobTitle(
            id = "job-research",
            jobCode = "RESEARCH",
            jobName = "AI Research",
            iconKey = "biotech",
            relevantDepartmentCategory = "research",
            description = "Riset mendalam sesuai Knowledge Priority Hierarchy, menemukan pola & mengusulkan Knowledge Rule baru"
        ),
        MasterAiJobTitle(
            id = "job-reporting",
            jobCode = "REPORTING",
            jobName = "AI Reporting",
            iconKey = "analytics",
            relevantDepartmentCategory = "executive",
            description = "Menyusun Daily/Weekly/Monthly/Management Report otomatis, proactive reporting, visualisasi data"
        )
    )

    private val jobByCode = OFFICIAL_15_JOB_TITLES.associateBy { it.jobCode.lowercase() }
    private val jobById = OFFICIAL_15_JOB_TITLES.associateBy { it.id }

    fun getByCode(code: String): MasterAiJobTitle? = jobByCode[code.lowercase()]
    fun getById(id: String): MasterAiJobTitle? = jobById[id]

    /**
     * LANGKAH 1.1: Pemetaan otomatis domain_category hasil Data Understanding (Bagian C)
     * ke 15 Master AI Job Titles yang relevan:
     * 'recruitment' -> AI HR & Recruitment
     * 'finance' -> AI Finance
     * 'tender' / 'procurement' -> AI Procurement
     * 'sales' -> AI Sales
     * 'marketing' -> AI Marketing
     * 'project' / 'mining' -> AI Project / AI Operations
     * 'research' -> AI Research
     * dst.
     * REUSE mapping department_ai_collaboration (Fase 110 Bagian B) sebagai referensi konsisten.
     */
    fun mapDomainToAiJobTitle(domainCategory: String): MasterAiJobTitle {
        val d = domainCategory.lowercase().trim().replace("-", "_").replace(" ", "_")

        // 1. Pemetaan eksplisit berdasarkan kata kunci domain spesifik
        when {
            d.contains("recruit") || d.contains("hr") || d.contains("talent") || d.contains("kandidat") || d.contains("pelamar") ->
                return getByCode("HR_RECRUITMENT") ?: OFFICIAL_15_JOB_TITLES[5]

            d.contains("finance") || d.contains("accounting") || d.contains("pajak") || d.contains("cashflow") || d.contains("budget") || d.contains("keuangan") ->
                return getByCode("FINANCE") ?: OFFICIAL_15_JOB_TITLES[6]

            d.contains("procure") || d.contains("tender") || d.contains("supplier") || d.contains("vendor") || d.contains("lelang") || d.contains("rfq") || d.contains("pengadaan") ->
                return getByCode("PROCUREMENT") ?: OFFICIAL_15_JOB_TITLES[11]

            d.contains("sales") || d.contains("penjualan") || d.contains("lead") || d.contains("prospect") || d.contains("closing") ->
                return getByCode("SALES") ?: OFFICIAL_15_JOB_TITLES[3]

            d.contains("market") || d.contains("campaign") || d.contains("pemasaran") || d.contains("iklan") ->
                return getByCode("MARKETING") ?: OFFICIAL_15_JOB_TITLES[7]

            d.contains("project") || d.contains("proyek") || d.contains("milestone") ->
                return getByCode("PROJECT") ?: OFFICIAL_15_JOB_TITLES[12]

            d.contains("mining") || d.contains("tambang") || d.contains("operat") || d.contains("fleet") || d.contains("alat_berat") || d.contains("hse") || d.contains("logistics") ->
                return getByCode("OPERATIONS") ?: OFFICIAL_15_JOB_TITLES[2]

            d.contains("research") || d.contains("riset") || d.contains("r_and_d") || d.contains("studi") ->
                return getByCode("RESEARCH") ?: OFFICIAL_15_JOB_TITLES[13]

            d.contains("customer_service") || d.contains("support") || d.contains("cs") || d.contains("komplain") ->
                return getByCode("CUSTOMER_SERVICE") ?: OFFICIAL_15_JOB_TITLES[4]

            d.contains("legal") || d.contains("knowledge") || d.contains("kontrak") || d.contains("sop") || d.contains("document") || d.contains("hukum") ->
                return getByCode("KNOWLEDGE_DOCUMENT") ?: OFFICIAL_15_JOB_TITLES[8]

            d.contains("task") || d.contains("workflow") || d.contains("tugas") ->
                return getByCode("TASK_WORKFLOW") ?: OFFICIAL_15_JOB_TITLES[9]

            d.contains("crm") || d.contains("retention") || d.contains("churn") || d.contains("loyalty") ->
                return getByCode("CRM_CUSTOMER_SUCCESS") ?: OFFICIAL_15_JOB_TITLES[10]

            d.contains("intel") || d.contains("competitor") || d.contains("strategy") ->
                return getByCode("COMPANY_INTELLIGENCE") ?: OFFICIAL_15_JOB_TITLES[1]

            d.contains("report") || d.contains("laporan") || d.contains("analytics") ->
                return getByCode("REPORTING") ?: OFFICIAL_15_JOB_TITLES[14]
        }

        // 2. REUSE referensi mapping department_ai_collaboration (Fase 110 Bagian B)
        try {
            val deptMapping = ProactiveCollaborationScopeRepository.defaultInstance.getMappingForDepartment(d)
            if (deptMapping != null && deptMapping.aiJobCodes.isNotEmpty()) {
                val primaryJobCode = deptMapping.aiJobCodes.first()
                val mappedJob = getByCode(primaryJobCode)
                if (mappedJob != null) return mappedJob
            }
        } catch (_: Throwable) {}

        // 3. Fallback jika tidak terpetakan secara spesifik: AI Chief of Staff (5 Bintang)
        return getByCode("CHIEF_OF_STAFF") ?: OFFICIAL_15_JOB_TITLES[0]
    }
}
