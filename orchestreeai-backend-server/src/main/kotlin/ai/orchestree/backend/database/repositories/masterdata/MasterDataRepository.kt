package ai.orchestree.backend.database.repositories.masterdata

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class DepartmentCategoryRecord(
    val id: String,
    val category_code: String,
    val category_name: String,
    val description: String? = null,
    val icon_key: String? = null,
    val is_active: Boolean = true
)

@Serializable
data class IndustryCatalogRecord(
    val id: String,
    val industry_code: String,
    val industry_name: String,
    val description: String? = null,
    val is_active: Boolean = true
)

@Serializable
data class JobLevelCatalogRecord(
    val id: String,
    val level_code: String,
    val level_name: String,
    val hierarchy_order: Int,
    val description: String? = null
)

@Serializable
data class JobSubTitleCatalogRecord(
    val id: String,
    val parent_level_id: String? = null,
    val department_category_id: String? = null,
    val sub_title_name: String,
    val is_active: Boolean = true
)

@Serializable
data class AiJobTitleRecord(
    val id: String,
    val job_code: String,
    val job_name: String,
    val icon_key: String? = null,
    val star_rating: Int = 4,
    val description: String,
    val maps_to_persona_type: String,
    val is_top_coordinator: Boolean = false,
    val relevant_department_category_id: String? = null
)

@Serializable
data class AiStructuralRoleRecord(
    val id: String,
    val parent_job_title_id: String,
    val structural_code: String,
    val structural_name: String,
    val skill_summary: String,
    val maps_to_persona_type: String
)

@Serializable
data class AiAgentSkillRecord(
    val id: String,
    val skill_code: String,
    val skill_name: String,
    val skill_category: String,
    val description: String,
    val applicable_job_title_ids: List<String> = emptyList(),
    val applicable_structural_role_ids: List<String> = emptyList(),
    val required_mcp_tools: List<String> = emptyList(),
    val is_active: Boolean = true,
    val source_type: String = "built_in"
)

class MasterDataRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(MasterDataRepository::class.java)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // In-memory backing stores initialized with official Fase 91 & 92 seed definitions
    private val departmentCategories = CopyOnWriteArrayList<DepartmentCategoryRecord>()
    private val industryCatalog = CopyOnWriteArrayList<IndustryCatalogRecord>()
    private val jobLevelCatalog = CopyOnWriteArrayList<JobLevelCatalogRecord>()
    private val jobSubTitleCatalog = CopyOnWriteArrayList<JobSubTitleCatalogRecord>()
    private val aiJobTitles = CopyOnWriteArrayList<AiJobTitleRecord>()
    private val aiStructuralRoles = CopyOnWriteArrayList<AiStructuralRoleRecord>()
    private val aiAgentSkills = CopyOnWriteArrayList<AiAgentSkillRecord>()

    init {
        seedOfficialCatalog()
    }

    private fun seedOfficialCatalog() {
        // 14 Official Department Categories (Fase 91 Bagian A)
        val depts = listOf(
            DepartmentCategoryRecord("dept-exec", "executive", "Executive & Leadership", "Direksi, C-Level, dan kepemimpinan strategis perusahaan", "crown"),
            DepartmentCategoryRecord("dept-sales", "sales", "Sales & Business Development", "Penjualan, akuisisi pelanggan, dan pengembangan bisnis", "trending_up"),
            DepartmentCategoryRecord("dept-mkt", "marketing", "Marketing & Brand", "Pemasaran, branding, dan komunikasi produk", "megaphone"),
            DepartmentCategoryRecord("dept-cs", "customer_service", "Customer Service & Success", "Layanan pelanggan dan keberhasilan pelanggan", "headset"),
            DepartmentCategoryRecord("dept-hr", "hr", "Human Resources & People", "Sumber daya manusia, rekrutmen, dan pengembangan talenta", "people"),
            DepartmentCategoryRecord("dept-fin", "finance", "Finance & Accounting", "Keuangan, akuntansi, dan pengendalian anggaran", "wallet"),
            DepartmentCategoryRecord("dept-ops", "operations", "Operations & Production", "Operasional harian, produksi, dan efisiensi proses", "settings"),
            DepartmentCategoryRecord("dept-proc", "procurement", "Procurement & Supply Chain", "Pengadaan, rantai pasok, dan manajemen vendor", "truck"),
            DepartmentCategoryRecord("dept-proj", "project", "Project & Program Management", "Manajemen proyek dan program lintas tim", "flag"),
            DepartmentCategoryRecord("dept-it", "it_engineering", "IT & Engineering", "Teknologi informasi dan pengembangan sistem", "code"),
            DepartmentCategoryRecord("dept-legal", "legal", "Legal & Compliance", "Hukum, kepatuhan regulasi, dan manajemen risiko", "gavel"),
            DepartmentCategoryRecord("dept-rnd", "research", "Research & Development", "Riset, inovasi, dan pengembangan produk baru", "flask"),
            DepartmentCategoryRecord("dept-hse", "hse", "Health, Safety & Environment", "Kesehatan, keselamatan kerja, dan lingkungan", "shield"),
            DepartmentCategoryRecord("dept-qa", "quality", "Quality Assurance & Control", "Jaminan dan pengendalian kualitas", "check_circle")
        )
        departmentCategories.addAll(depts)

        // 20 Official Industries (Fase 91 Bagian D)
        val industries = listOf(
            IndustryCatalogRecord("ind-fnb", "fnb", "Food & Beverage", "Restoran, kafe, katering, dan industri makanan-minuman"),
            IndustryCatalogRecord("ind-retail", "retail", "Retail & E-Commerce", "Perdagangan eceran dan toko online"),
            IndustryCatalogRecord("ind-health", "healthcare", "Kesehatan & Rumah Sakit", "Rumah sakit, klinik, apotek, dan layanan kesehatan"),
            IndustryCatalogRecord("ind-const", "construction", "Konstruksi & Real Estate", "Kontraktor bangunan, properti, dan pengembangan lahan"),
            IndustryCatalogRecord("ind-mfg", "manufacturing", "Manufaktur & Industri", "Pabrik produksi barang dan industri berat"),
            IndustryCatalogRecord("ind-tech", "startup_tech", "Startup Teknologi & SaaS", "Perusahaan rintisan berbasis teknologi digital"),
            IndustryCatalogRecord("ind-umkm", "umkm", "UMKM (Usaha Mikro, Kecil, Menengah)", "Usaha skala kecil-menengah lintas sektor"),
            IndustryCatalogRecord("ind-prof", "professional_services", "Jasa Profesional (Konsultan, Hukum, Akuntansi)", "Penyedia jasa keahlian profesional"),
            IndustryCatalogRecord("ind-edu", "education", "Pendidikan & Pelatihan", "Sekolah, lembaga kursus, dan pelatihan"),
            IndustryCatalogRecord("ind-log", "logistics", "Logistik & Transportasi", "Pengiriman, ekspedisi, dan transportasi barang"),
            IndustryCatalogRecord("ind-mine", "mining_energy", "Pertambangan & Energi", "Ekstraksi sumber daya alam dan energi"),
            IndustryCatalogRecord("ind-agri", "agriculture", "Pertanian & Perkebunan", "Produksi pertanian, perkebunan, dan agribisnis"),
            IndustryCatalogRecord("ind-bank", "finance_banking", "Keuangan & Perbankan", "Bank, fintech, asuransi, dan lembaga keuangan"),
            IndustryCatalogRecord("ind-auto", "automotive", "Otomotif", "Dealer, bengkel, dan industri kendaraan"),
            IndustryCatalogRecord("ind-tour", "hospitality_tourism", "Perhotelan & Pariwisata", "Hotel, resor, dan biro perjalanan"),
            IndustryCatalogRecord("ind-fash", "fashion_beauty", "Fashion & Kecantikan", "Industri busana, kosmetik, dan perawatan diri"),
            IndustryCatalogRecord("ind-media", "media_entertainment", "Media & Hiburan", "Produksi konten, penyiaran, dan hiburan"),
            IndustryCatalogRecord("ind-agency", "agency_creative", "Digital Agency & Kreatif", "Agensi pemasaran digital dan kreatif"),
            IndustryCatalogRecord("ind-ngo", "nonprofit", "Organisasi Nirlaba", "Yayasan, NGO, dan organisasi sosial"),
            IndustryCatalogRecord("ind-other", "other", "Lainnya", "Kategori industri di luar daftar resmi")
        )
        industryCatalog.addAll(industries)

        // 9 Official Job Levels (Fase 91 Bagian B)
        val levels = listOf(
            JobLevelCatalogRecord("lvl-owner", "owner", "Owner / Pendiri", 1, "Pemilik perusahaan atau pendiri utama"),
            JobLevelCatalogRecord("lvl-direksi", "direksi", "Direksi / C-Level (CEO, COO, CFO, CTO, CMO, CHRO)", 2, "Jajaran eksekutif tertinggi perusahaan"),
            JobLevelCatalogRecord("lvl-vp", "vp", "Vice President / Senior Director", 3, "Kepemimpinan senior lintas divisi"),
            JobLevelCatalogRecord("lvl-gm", "gm", "General Manager", 4, "Kepala operasional unit bisnis/cabang"),
            JobLevelCatalogRecord("lvl-mgr", "manajer", "Manajer / Head of Department", 5, "Kepala departemen fungsional"),
            JobLevelCatalogRecord("lvl-spv", "supervisor", "Supervisor / Team Lead / Koordinator", 6, "Pemimpin tim operasional harian"),
            JobLevelCatalogRecord("lvl-spec", "staff_senior", "Staff Senior / Spesialis", 7, "Karyawan berpengalaman dengan keahlian spesifik"),
            JobLevelCatalogRecord("lvl-staff", "staff", "Staff / Karyawan", 8, "Karyawan operasional umum"),
            JobLevelCatalogRecord("lvl-intern", "magang", "Magang / Intern", 9, "Peserta magang/pelatihan kerja")
        )
        jobLevelCatalog.addAll(levels)

        // Job Sub Titles (Fase 91 Bagian C)
        val subTitles = listOf(
            JobSubTitleCatalogRecord("sub-1", "lvl-direksi", "dept-exec", "Chief Executive Officer (CEO)"),
            JobSubTitleCatalogRecord("sub-2", "lvl-direksi", "dept-exec", "Chief Operating Officer (COO)"),
            JobSubTitleCatalogRecord("sub-3", "lvl-direksi", "dept-exec", "Chief Financial Officer (CFO)"),
            JobSubTitleCatalogRecord("sub-4", "lvl-direksi", "dept-exec", "Chief Technology Officer (CTO)"),
            JobSubTitleCatalogRecord("sub-5", "lvl-owner", "dept-exec", "Founder & Chief Commissioner"),
            JobSubTitleCatalogRecord("sub-6", "lvl-mgr", "dept-sales", "Sales Manager"),
            JobSubTitleCatalogRecord("sub-7", "lvl-mgr", "dept-sales", "Business Development Manager"),
            JobSubTitleCatalogRecord("sub-8", "lvl-spv", "dept-sales", "Sales Supervisor"),
            JobSubTitleCatalogRecord("sub-9", "lvl-spec", "dept-sales", "Senior Account Executive"),
            JobSubTitleCatalogRecord("sub-10", "lvl-staff", "dept-sales", "Sales Representative / SDR"),
            JobSubTitleCatalogRecord("sub-11", "lvl-mgr", "dept-mkt", "Marketing Manager"),
            JobSubTitleCatalogRecord("sub-12", "lvl-spec", "dept-mkt", "Senior Content & Brand Strategist"),
            JobSubTitleCatalogRecord("sub-13", "lvl-staff", "dept-mkt", "Social Media & Campaign Specialist"),
            JobSubTitleCatalogRecord("sub-14", "lvl-mgr", "dept-cs", "Customer Service Manager"),
            JobSubTitleCatalogRecord("sub-15", "lvl-staff", "dept-cs", "Customer Care Officer"),
            JobSubTitleCatalogRecord("sub-16", "lvl-mgr", "dept-hr", "HR & Talent Acquisition Manager"),
            JobSubTitleCatalogRecord("sub-17", "lvl-staff", "dept-hr", "People Operations Specialist"),
            JobSubTitleCatalogRecord("sub-18", "lvl-mgr", "dept-fin", "Finance & Accounting Manager"),
            JobSubTitleCatalogRecord("sub-19", "lvl-spec", "dept-fin", "Tax & Cash Flow Specialist"),
            JobSubTitleCatalogRecord("sub-20", "lvl-mgr", "dept-ops", "Operations & Supply Manager"),
            JobSubTitleCatalogRecord("sub-21", "lvl-staff", "dept-ops", "Field Operations Coordinator"),
            JobSubTitleCatalogRecord("sub-22", "lvl-mgr", "dept-it", "Engineering Lead / IT Manager"),
            JobSubTitleCatalogRecord("sub-23", "lvl-spec", "dept-it", "Senior Full-Stack Engineer")
        )
        jobSubTitleCatalog.addAll(subTitles)

        // 15 AI Job Titles (Fase 91 Bagian H)
        val aiTitles = listOf(
            AiJobTitleRecord("jt-cos", "chief_of_staff", "AI Chief of Staff", "crown", 5, "Mengoordinasikan seluruh Staff AI Agent, mensintesis Executive Briefing", "CHIEF_OF_STAFF", true, "dept-exec"),
            AiJobTitleRecord("jt-intel", "company_intelligence", "AI Company Intelligence", "radar", 4, "Memantau kompetitor, tren pasar (World Monitor), sinyal prospek", "COMPANY_INTELLIGENCE_AGENT", false, "dept-exec"),
            AiJobTitleRecord("jt-ops", "operations", "AI Operations", "settings", 4, "Memantau produksi, equipment, fleet, kualitas, downtime", "OPERATIONS_AGENT", false, "dept-ops"),
            AiJobTitleRecord("jt-sales", "sales", "AI Sales", "trending_up", 4, "Discovery, rekomendasi produk, upsell/cross-sell, closing", "SALES_AGENT", false, "dept-sales"),
            AiJobTitleRecord("jt-cs", "customer_service", "AI Customer Service", "headset", 4, "Menjawab FAQ, menangani komplain, refund, return", "CUSTOMER_SERVICE_AGENT", false, "dept-cs"),
            AiJobTitleRecord("jt-hr", "hr_recruitment", "AI HR & Recruitment", "people", 4, "Analisis kebutuhan tenaga kerja, monitoring kehadiran, rekrutmen", "HR_AGENT", false, "dept-hr"),
            AiJobTitleRecord("jt-fin", "finance", "AI Finance", "wallet", 4, "Cash flow analysis, AR/AP, budget variance, forecasting", "FINANCE_AGENT", false, "dept-fin"),
            AiJobTitleRecord("jt-mkt", "marketing", "AI Marketing", "megaphone", 4, "Membuat campaign, content generation, brand voice", "MARKETING_AGENT", false, "dept-mkt"),
            AiJobTitleRecord("jt-know", "knowledge_document", "AI Knowledge & Document", "book", 4, "Mengelola Company Brain, ekstraksi dokumen (SOP/manual)", "KNOWLEDGE_AGENT", false, "dept-it"),
            AiJobTitleRecord("jt-flow", "task_workflow", "AI Task & Workflow", "kanban", 4, "Membuat task otomatis, memonitor closed-loop Human+AI", "WORKFLOW_ORCHESTRATOR_AGENT", false, "dept-proj"),
            AiJobTitleRecord("jt-crm", "crm_customer_success", "AI CRM & Customer Success", "heart_handshake", 4, "Mengelola relasi pelanggan, retensi, kesehatan akun", "CUSTOMER_SUCCESS_AGENT", false, "dept-cs"),
            AiJobTitleRecord("jt-proc", "procurement", "AI Procurement", "truck", 4, "Memantau PO, RFQ, supplier, delivery, stok kritikal", "PROCUREMENT_AGENT", false, "dept-proc"),
            AiJobTitleRecord("jt-proj", "project", "AI Project", "flag", 4, "Project Health Score, jadwal, biaya, risiko keterlambatan", "PROJECT_AGENT", false, "dept-proj"),
            AiJobTitleRecord("jt-res", "research", "AI Research", "flask", 4, "Riset mendalam, Knowledge Rule baru", "RESEARCH_AGENT", false, "dept-rnd"),
            AiJobTitleRecord("jt-rep", "reporting", "AI Reporting", "chart_bar", 4, "Daily/Weekly/Monthly/Management Report otomatis", "REPORTING_AGENT", false, "dept-exec")
        )
        aiJobTitles.addAll(aiTitles)

        // 20 AI Structural Roles (Fase 91 Bagian I)
        val structural = listOf(
            AiStructuralRoleRecord("str-sdr", "jt-sales", "sdr", "AI SDR", "Kualifikasi lead awal, penggalian kebutuhan", "SDR"),
            AiStructuralRoleRecord("str-cons", "jt-sales", "sales_consultant", "AI Sales Consultant", "Sales discovery mendalam", "SALES_CONSULTANT"),
            AiStructuralRoleRecord("str-adv", "jt-sales", "product_advisor", "AI Product Advisor", "Rekomendasi produk, upsell, cross-sell", "PRODUCT_ADVISOR"),
            AiStructuralRoleRecord("str-cls", "jt-sales", "closer", "AI Closer", "Objection handling dan closing", "CLOSER"),
            AiStructuralRoleRecord("str-flw", "jt-sales", "follow_up", "AI Follow-Up Agent", "Follow-up lead dingin, abandoned cart", "FOLLOW_UP_AGENT"),
            AiStructuralRoleRecord("str-cmp", "jt-mkt", "campaign_specialist", "AI Campaign Specialist", "Eksekusi campaign marketing", "CAMPAIGN_SPECIALIST"),
            AiStructuralRoleRecord("str-cnt", "jt-mkt", "content_creator", "AI Content Creator", "Pembuatan konten kreatif", "CONTENT_CREATOR"),
            AiStructuralRoleRecord("str-rec", "jt-cs", "receptionist", "AI Receptionist", "Menyambut dan klasifikasi kebutuhan awal", "RECEPTIONIST"),
            AiStructuralRoleRecord("str-cmp", "jt-cs", "complaint_handler", "AI Complaint Handler", "Penanganan komplain dan eskalasi", "COMPLAINT_HANDLER"),
            AiStructuralRoleRecord("str-ret", "jt-crm", "retention_specialist", "AI Retention Specialist", "Reaktivasi dan retensi customer", "RETENTION_AGENT"),
            AiStructuralRoleRecord("str-mnt", "jt-ops", "maintenance_officer", "AI Maintenance Officer", "Analisis maintenance equipment", "MAINTENANCE_AGENT"),
            AiStructuralRoleRecord("str-flt", "jt-ops", "fleet_analyst", "AI Fleet Analyst", "Analisis fleet dan telematika kendaraan", "FLEET_AGENT"),
            AiStructuralRoleRecord("str-rcr", "jt-hr", "recruitment_screener", "AI Recruitment Screener", "Penyaringan kandidat rekrutmen", "RECRUITMENT_AGENT"),
            AiStructuralRoleRecord("str-att", "jt-hr", "attendance_officer", "AI Attendance Officer", "Monitoring kehadiran staff", "ATTENDANCE_AGENT"),
            AiStructuralRoleRecord("str-csh", "jt-fin", "cashflow_analyst", "AI Cash Flow Analyst", "Analisis arus kas perusahaan", "CASHFLOW_AGENT"),
            AiStructuralRoleRecord("str-pot", "jt-proc", "po_tracker", "AI PO Tracker", "Pelacakan status purchase order", "PO_TRACKER_AGENT"),
            AiStructuralRoleRecord("str-sch", "jt-proj", "schedule_analyst", "AI Schedule Analyst", "Analisis jadwal dan risiko proyek", "SCHEDULE_AGENT"),
            AiStructuralRoleRecord("str-doc", "jt-know", "document_analyst", "AI Document Analyst", "Ekstraksi dan analisis dokumen", "DOCUMENT_ANALYST"),
            AiStructuralRoleRecord("str-ptn", "jt-res", "pattern_finder", "AI Pattern Finder", "Penemuan pola dari data historis", "PATTERN_FINDER_AGENT"),
            AiStructuralRoleRecord("str-viz", "jt-rep", "data_viz_agent", "AI Data Visualization Agent", "Visualisasi data untuk laporan", "DATA_VIZ_AGENT")
        )
        aiStructuralRoles.addAll(structural)

        // Built-in AI Agent Skills (Fase 92)
        val skills = listOf(
            AiAgentSkillRecord("sk-lead-qual", "lead_qualification", "Lead Qualification & BANT Scoring", "core", "Menganalisis profil lead, budget, authority, need, timeline", listOf("jt-sales"), listOf("str-sdr"), listOf("crm_tool")),
            AiAgentSkillRecord("sk-prod-rec", "product_recommendation", "Smart Product Recommendation Engine", "core", "Rekomendasi katalog produk berdasarkan kebutuhan prospek", listOf("jt-sales"), listOf("str-adv"), listOf("catalog_tool")),
            AiAgentSkillRecord("sk-obj-hand", "objection_handling", "High-Conversion Objection Handling", "core", "Menjawab keberatan harga, fitur, dan skeptisisme calon pembeli", listOf("jt-sales"), listOf("str-cls"), listOf("sales_script_tool")),
            AiAgentSkillRecord("sk-copywriting", "creative_copywriting", "Omnichannel Copywriting & Ad Creation", "core", "Menghasilkan copy iklan, caption media sosial, dan newsletter", listOf("jt-mkt"), listOf("str-cnt", "str-cmp"), listOf("creative_tool")),
            AiAgentSkillRecord("sk-brand-voice", "brand_voice_guard", "Brand Voice Compliance & Tone Guard", "core", "Memvalidasi konten terhadap panduan brand persona tenant", listOf("jt-mkt"), listOf("str-cnt"), listOf("brand_voice_tool")),
            AiAgentSkillRecord("sk-ticket-triage", "ticket_auto_triage", "Automated Support Ticket Classification", "core", "Triase keluhan pelanggan berdasarkan sentimen & urgensi", listOf("jt-cs", "jt-crm"), listOf("str-rec", "str-cmp"), listOf("ticket_tool")),
            AiAgentSkillRecord("sk-sentiment", "sentiment_escalation", "Sentiment-Based Escalation Guard", "core", "Mendeteksi kemarahan pelanggan dan eskalasi instan ke human supervisor", listOf("jt-cs"), listOf("str-cmp"), listOf("sentiment_analyzer")),
            AiAgentSkillRecord("sk-doc-parse", "document_ocr_extraction", "Document Structure & OCR Extractor", "core", "Mengekstraksi invoice, PO, SOP, dan kontrak ke Company Brain", listOf("jt-know", "jt-proc"), listOf("str-doc", "str-pot"), listOf("ocr_tool")),
            AiAgentSkillRecord("sk-cashflow", "runway_forecasting", "Predictive Runway & Cashflow Forecaster", "core", "Menghitung proyeksi arus kas 30-90 hari ke depan", listOf("jt-fin"), listOf("str-csh"), listOf("financial_calc_tool")),
            AiAgentSkillRecord("sk-report-synth", "executive_briefing_synth", "Executive Synthesis & Daily Briefing", "core", "Mensintesis matriks seluruh divisi menjadi ringkasan pimpinan", listOf("jt-cos", "jt-rep"), listOf("str-viz"), listOf("reporting_tool"))
        )
        aiAgentSkills.addAll(skills)
    }

    suspend fun getDepartmentCategories(): List<DepartmentCategoryRecord> {
        if (supabase.isConfigured()) {
            val res = supabase.queryTableGlobal("department_categories", extraParams = mapOf("order" to "category_code.asc"))
            if (res.isSuccess) {
                try {
                    val parsed = json.decodeFromString<List<DepartmentCategoryRecord>>(res.getOrThrow())
                    if (parsed.isNotEmpty()) {
                        return (parsed + departmentCategories).distinctBy { it.category_code }
                    }
                } catch (e: Exception) {
                    logger.warn("Failed parsing department_categories from Supabase: ${e.message}")
                }
            }
        }
        return departmentCategories
    }

    suspend fun addDepartmentCategory(record: DepartmentCategoryRecord) {
        val validId = if (runCatching { java.util.UUID.fromString(record.id) }.isSuccess) record.id else java.util.UUID.randomUUID().toString()
        val validRecord = record.copy(id = validId)
        departmentCategories.removeIf { it.category_code == validRecord.category_code }
        departmentCategories.add(validRecord)
        if (supabase.isConfigured()) {
            val payload = json.encodeToString(DepartmentCategoryRecord.serializer(), validRecord)
            supabase.insertRecord("department_categories", "global", payload)
        }
    }

    suspend fun getIndustryCatalog(): List<IndustryCatalogRecord> {
        if (supabase.isConfigured()) {
            val res = supabase.queryTableGlobal("industry_catalog", extraParams = mapOf("order" to "industry_code.asc"))
            if (res.isSuccess) {
                try {
                    val parsed = json.decodeFromString<List<IndustryCatalogRecord>>(res.getOrThrow())
                    if (parsed.isNotEmpty()) return parsed
                } catch (e: Exception) {
                    logger.warn("Failed parsing industry_catalog from Supabase: ${e.message}")
                }
            }
        }
        return industryCatalog
    }

    suspend fun getJobLevelCatalog(): List<JobLevelCatalogRecord> {
        if (supabase.isConfigured()) {
            val res = supabase.queryTableGlobal("job_level_catalog", extraParams = mapOf("order" to "hierarchy_order.asc"))
            if (res.isSuccess) {
                try {
                    val parsed = json.decodeFromString<List<JobLevelCatalogRecord>>(res.getOrThrow())
                    if (parsed.isNotEmpty()) return parsed
                } catch (e: Exception) {
                    logger.warn("Failed parsing job_level_catalog from Supabase: ${e.message}")
                }
            }
        }
        return jobLevelCatalog.sortedBy { it.hierarchy_order }
    }

    suspend fun getJobSubTitleCatalog(levelId: String? = null, departmentCategoryId: String? = null): List<JobSubTitleCatalogRecord> {
        if (supabase.isConfigured()) {
            val params = mutableMapOf<String, String>()
            if (!levelId.isNullOrBlank()) params["parent_level_id"] = "eq.$levelId"
            if (!departmentCategoryId.isNullOrBlank()) params["department_category_id"] = "eq.$departmentCategoryId"
            val res = supabase.queryTableGlobal("job_sub_title_catalog", extraParams = params)
            if (res.isSuccess) {
                try {
                    val parsed = json.decodeFromString<List<JobSubTitleCatalogRecord>>(res.getOrThrow())
                    if (parsed.isNotEmpty()) return parsed
                } catch (e: Exception) {
                    logger.warn("Failed parsing job_sub_title_catalog from Supabase: ${e.message}")
                }
            }
        }
        var list = jobSubTitleCatalog.toList()
        if (!levelId.isNullOrBlank()) {
            list = list.filter { it.parent_level_id == levelId }
        }
        if (!departmentCategoryId.isNullOrBlank()) {
            list = list.filter { it.department_category_id == departmentCategoryId }
        }
        return list
    }

    suspend fun getAiJobTitles(tenantId: String): List<AiJobTitleRecord> {
        if (supabase.isConfigured()) {
            val res = supabase.queryTableGlobal("ai_job_titles", extraParams = mapOf("order" to "star_rating.desc"))
            if (res.isSuccess) {
                try {
                    val parsed = json.decodeFromString<List<AiJobTitleRecord>>(res.getOrThrow())
                    if (parsed.isNotEmpty()) {
                        return (parsed + aiJobTitles).distinctBy { it.job_code }
                    }
                } catch (e: Exception) {
                    logger.warn("Failed parsing ai_job_titles from Supabase: ${e.message}")
                }
            }
        }
        return aiJobTitles
    }

    suspend fun addAiJobTitle(record: AiJobTitleRecord) {
        val validId = if (runCatching { java.util.UUID.fromString(record.id) }.isSuccess) record.id else java.util.UUID.randomUUID().toString()
        val validDeptId = if (record.relevant_department_category_id != null && runCatching { java.util.UUID.fromString(record.relevant_department_category_id) }.isSuccess) record.relevant_department_category_id else null
        val validRecord = record.copy(id = validId, relevant_department_category_id = validDeptId)
        aiJobTitles.removeIf { it.job_code == validRecord.job_code }
        aiJobTitles.add(validRecord)
        if (supabase.isConfigured()) {
            val payload = json.encodeToString(AiJobTitleRecord.serializer(), validRecord)
            supabase.insertRecord("ai_job_titles", "global", payload)
        }
    }

    suspend fun getAiStructuralRoles(tenantId: String, jobTitleId: String): List<AiStructuralRoleRecord> {
        if (supabase.isConfigured()) {
            val res = supabase.queryTableGlobal("ai_structural_roles", extraParams = mapOf("parent_job_title_id" to "eq.$jobTitleId"))
            if (res.isSuccess) {
                try {
                    val parsed = json.decodeFromString<List<AiStructuralRoleRecord>>(res.getOrThrow())
                    if (parsed.isNotEmpty()) return parsed
                } catch (e: Exception) {
                    logger.warn("Failed parsing ai_structural_roles from Supabase: ${e.message}")
                }
            }
        }
        return aiStructuralRoles.filter { it.parent_job_title_id == jobTitleId }
    }

    suspend fun getAiAvailableSkills(tenantId: String, jobTitleId: String): List<AiAgentSkillRecord> {
        if (supabase.isConfigured()) {
            val res = supabase.queryTableGlobal("ai_agent_skills", extraParams = mapOf("is_active" to "eq.true"))
            if (res.isSuccess) {
                try {
                    val parsed = json.decodeFromString<List<AiAgentSkillRecord>>(res.getOrThrow())
                    if (parsed.isNotEmpty()) {
                        return parsed.filter { it.applicable_job_title_ids.isEmpty() || it.applicable_job_title_ids.contains(jobTitleId) }
                    }
                } catch (e: Exception) {
                    logger.warn("Failed parsing ai_agent_skills from Supabase: ${e.message}")
                }
            }
        }
        return aiAgentSkills.filter { it.applicable_job_title_ids.isEmpty() || it.applicable_job_title_ids.contains(jobTitleId) }
    }
}
