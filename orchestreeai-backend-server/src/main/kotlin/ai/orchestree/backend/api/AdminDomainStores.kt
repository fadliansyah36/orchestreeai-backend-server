package ai.orchestree.backend.api

import ai.orchestree.backend.mcptools.SkillPluginUploadEngine
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class AdminImageProviderItem(
    val id: String,
    val name: String,
    val providerType: String,
    val models: List<String> = listOf("image-gen-v1"),
    val status: String = "ACTIVE",
    val priority: Int = 1,
    val apiKeySecretRef: String = "••••••••",
    val latencyMs: Long = 320
)

@Serializable
data class AdminImageProviderCreateRequest(
    val name: String,
    val providerType: String,
    val models: List<String> = listOf("image-gen-v1"),
    val priority: Int = 1,
    val apiKey: String? = null
)

@Serializable
data class AdminMcpToolItem(
    val id: String,
    val name: String,
    val description: String,
    val inputSchema: String = "{}",
    val riskLevel: String = "LOW",
    val requiredRole: String = "STAFF_HUMAN",
    val restrictedToOperationMode: String = "UNRESTRICTED",
    val status: String = "ACTIVE",
    val invocationCount24h: Long = 0,
    val errorCount24h: Long = 0,
    val avgLatencyMs24h: Long = 45
)

@Serializable
data class AdminSkillPluginUploadRequest(
    val pluginName: String,
    val version: String = "1.0.0",
    val author: String = "Super Admin",
    val manifestJson: String,
    val skillDefinitionMd: String = "",
    val zipBase64: String? = null
)

@Serializable
data class AdminSkillPluginUploadResponse(
    val success: Boolean,
    val pluginId: String,
    val pluginName: String,
    val version: String,
    val securityScanPassed: Boolean,
    val declaredTools: List<String>,
    val validationErrors: List<String> = emptyList(),
    val status: String
)

@Serializable
data class MasterDataCategoryInfo(
    val categoryId: String,
    val displayName: String,
    val description: String,
    val itemCount: Int,
    val keyLabel: String = "Code / Key",
    val valueLabel: String = "Name / Value"
)

@Serializable
data class DepartmentCountItem(
    val departmentName: String,
    val count: Int,
    val percentage: Double
)

@Serializable
data class AiJobTitleCountItem(
    val jobTitle: String,
    val count: Int,
    val percentage: Double
)

@Serializable
data class AdminWorkforceMonitoringSummary(
    val totalActiveDepartments: Int,
    val totalAiAgents: Int,
    val humanToAiRatio: String,
    val departmentDistribution: List<DepartmentCountItem>,
    val aiJobTitleDistribution: List<AiJobTitleCountItem>
)

@Serializable
data class ServerHealthMetrics(
    val podStatus: String,
    val cpuUsagePercent: Double,
    val memoryUsageMb: Long,
    val memoryMaxMb: Long,
    val activeConnections: Int
)

@Serializable
data class JobQueueStatusMetrics(
    val activeJobs: Int,
    val deadLetterCount: Int,
    val processedJobs24h: Int,
    val failureRatePercent: Double
)

@Serializable
data class SecurityIncidentsMetrics(
    val suspiciousAuthAttempts24h: Int,
    val abacViolations24h: Int,
    val highRiskMcpExecutions24h: Int,
    val biometricAnomalies24h: Int
)

@Serializable
data class RateLimitViolationsMetrics(
    val totalViolations24h: Int,
    val topViolatingTenants: List<String>,
    val currentThrottleState: String
)

@Serializable
data class SystemMonitoringOverview(
    val status: String,
    val uptimeSeconds: Long,
    val timestamp: Long,
    val providerHealth: List<AdminHealthReportItem>,
    val serverHealth: ServerHealthMetrics,
    val jobQueueStatus: JobQueueStatusMetrics,
    val securityIncidents: SecurityIncidentsMetrics,
    val rateLimitViolations: RateLimitViolationsMetrics
)

object AdminDomainStores {
    // 1. LLM Providers & Image Providers Store (Bagian A)
    val llmProviders = CopyOnWriteArrayList<AdminLlmProviderItem>().apply {
        val nimModels = ai.orchestree.backend.database.repositories.modelrouter.LlmProviderModelRepository.instance
            .getActiveModelsForProvider("NVIDIA_NIM").map { it.modelIdentifier }
        val openRouterModels = ai.orchestree.backend.database.repositories.modelrouter.LlmProviderModelRepository.instance
            .getActiveModelsForProvider("OPENROUTER").map { it.modelIdentifier }

        addAll(
            listOf(
                AdminLlmProviderItem(
                    id = "prov-nvidia-nim",
                    provider = "NVIDIA NIM",
                    providerType = "NVIDIA_NIM",
                    models = nimModels,
                    status = "ACTIVE",
                    latencyMs = 95,
                    priority = 1,
                    taskSpecialization = "frontier_reasoning,complex_analysis",
                    apiKeySecretRef = "••••••••",
                    baseUrl = "https://integrate.api.nvidia.com/v1",
                    isHealthy = true
                ),
                AdminLlmProviderItem(
                    id = "prov-openrouter",
                    provider = "OpenRouter",
                    providerType = "OPENROUTER",
                    models = openRouterModels,
                    status = "ACTIVE",
                    latencyMs = 240,
                    priority = 2,
                    taskSpecialization = "general",
                    apiKeySecretRef = "••••••••",
                    baseUrl = "https://openrouter.ai/api/v1",
                    isHealthy = true
                ),
                AdminLlmProviderItem(
                    id = "prov-groq",
                    provider = "Groq",
                    providerType = "GROQ",
                    models = emptyList(),
                    status = "ACTIVE",
                    latencyMs = 110,
                    priority = 3,
                    taskSpecialization = "speed_inference",
                    apiKeySecretRef = "••••••••",
                    baseUrl = "https://api.groq.com/openai/v1",
                    isHealthy = true
                ),
                AdminLlmProviderItem(
                    id = "prov-deepseek",
                    provider = "DeepSeek (Disabled)",
                    providerType = "DEEPSEEK",
                    models = emptyList(),
                    status = "DISABLED",
                    latencyMs = 380,
                    priority = 99,
                    taskSpecialization = "legacy",
                    apiKeySecretRef = "••••••••",
                    baseUrl = "https://api.deepseek.com/v1",
                    isHealthy = false
                ),
                AdminLlmProviderItem(
                    id = "prov-apimart",
                    provider = "Apimart",
                    providerType = "CUSTOM_OPENAI",
                    models = emptyList(),
                    status = "ACTIVE",
                    latencyMs = 420,
                    priority = 4,
                    taskSpecialization = "creative",
                    apiKeySecretRef = "••••••••",
                    baseUrl = "https://api.apimart.ai/v1",
                    isHealthy = true
                )
            )
        )
    }

    val imageProviders = CopyOnWriteArrayList<AdminImageProviderItem>().apply {
        addAll(
            listOf(
                AdminImageProviderItem("img-apimart-flux", "Apimart Flux Pro", "APIMART_FLUX", listOf("flux-1-schnell", "flux-1-dev"), "ACTIVE", 1, "••••••••", 410),
                AdminImageProviderItem("img-stability", "Stability AI SDXL", "STABILITY_AI", listOf("sdxl-turbo", "stable-diffusion-3"), "ACTIVE", 2, "••••••••", 520),
                AdminImageProviderItem("img-dalle", "OpenAI DALL-E 3", "OPENAI", listOf("dall-e-3"), "ACTIVE", 3, "••••••••", 680)
            )
        )
    }

    // 2. 10 Master Data Categories Store (Bagian B)
    val masterData = ConcurrentHashMap<String, CopyOnWriteArrayList<AdminMasterDataItem>>().apply {
        // 1. Department Categories (14 official)
        put("department_categories", CopyOnWriteArrayList(listOf(
            AdminMasterDataItem("dept-exec", "department_categories", "executive", "Executive & Leadership", "Direksi, C-Level, dan kepemimpinan strategis"),
            AdminMasterDataItem("dept-sales", "department_categories", "sales", "Sales & Business Development", "Penjualan, akuisisi pelanggan, dan pengembangan bisnis"),
            AdminMasterDataItem("dept-mkt", "department_categories", "marketing", "Marketing & Brand", "Pemasaran, branding, dan komunikasi produk"),
            AdminMasterDataItem("dept-cs", "department_categories", "customer_service", "Customer Service & Success", "Layanan pelanggan dan keberhasilan pelanggan"),
            AdminMasterDataItem("dept-hr", "department_categories", "hr", "Human Resources & People", "Sumber daya manusia, rekrutmen, dan pengembangan talenta"),
            AdminMasterDataItem("dept-fin", "department_categories", "finance", "Finance & Accounting", "Keuangan, akuntansi, dan pengendalian anggaran"),
            AdminMasterDataItem("dept-ops", "department_categories", "operations", "Operations & Production", "Operasional harian, produksi, dan efisiensi proses"),
            AdminMasterDataItem("dept-proc", "department_categories", "procurement", "Procurement & Supply Chain", "Pengadaan, rantai pasok, dan manajemen vendor"),
            AdminMasterDataItem("dept-proj", "department_categories", "project", "Project & Program Management", "Manajemen proyek dan program lintas tim"),
            AdminMasterDataItem("dept-it", "department_categories", "it_engineering", "IT & Engineering", "Teknologi informasi dan pengembangan sistem"),
            AdminMasterDataItem("dept-legal", "department_categories", "legal", "Legal & Compliance", "Hukum, kepatuhan regulasi, dan manajemen risiko"),
            AdminMasterDataItem("dept-rnd", "department_categories", "research", "Research & Development", "Riset, inovasi, dan pengembangan produk baru"),
            AdminMasterDataItem("dept-hse", "department_categories", "hse", "Health, Safety & Environment", "Kesehatan, keselamatan kerja, dan lingkungan"),
            AdminMasterDataItem("dept-qa", "department_categories", "quality", "Quality Assurance & Control", "Jaminan dan pengendalian kualitas")
        )))

        // 2. Job Level Catalog (9 official)
        put("job_level_catalog", CopyOnWriteArrayList(listOf(
            AdminMasterDataItem("lvl-owner", "job_level_catalog", "owner", "Owner / Pendiri", "Tingkat 1 - Pemilik perusahaan atau pendiri utama"),
            AdminMasterDataItem("lvl-direksi", "job_level_catalog", "direksi", "Direksi / C-Level", "Tingkat 2 - CEO, COO, CFO, CTO, CMO, CHRO"),
            AdminMasterDataItem("lvl-vp", "job_level_catalog", "vp", "Vice President / Senior Director", "Tingkat 3 - Kepemimpinan senior divisi"),
            AdminMasterDataItem("lvl-gm", "job_level_catalog", "gm", "General Manager", "Tingkat 4 - Kepala operasional unit bisnis"),
            AdminMasterDataItem("lvl-mgr", "job_level_catalog", "manajer", "Manajer / Head of Department", "Tingkat 5 - Kepala departemen fungsional"),
            AdminMasterDataItem("lvl-spv", "job_level_catalog", "supervisor", "Supervisor / Team Lead", "Tingkat 6 - Pemimpin tim operasional"),
            AdminMasterDataItem("lvl-spec", "job_level_catalog", "staff_senior", "Staff Senior / Spesialis", "Tingkat 7 - Karyawan keahlian spesifik"),
            AdminMasterDataItem("lvl-staff", "job_level_catalog", "staff", "Staff / Karyawan", "Tingkat 8 - Karyawan operasional umum"),
            AdminMasterDataItem("lvl-intern", "job_level_catalog", "magang", "Magang / Intern", "Tingkat 9 - Peserta magang atau pelatihan")
        )))

        // 3. Job Sub Title Catalog (Official subsets)
        put("job_sub_title_catalog", CopyOnWriteArrayList(listOf(
            AdminMasterDataItem("sub-ceo", "job_sub_title_catalog", "CEO", "Chief Executive Officer", "Executive leadership title"),
            AdminMasterDataItem("sub-coo", "job_sub_title_catalog", "COO", "Chief Operating Officer", "Operations leadership title"),
            AdminMasterDataItem("sub-cfo", "job_sub_title_catalog", "CFO", "Chief Financial Officer", "Finance leadership title"),
            AdminMasterDataItem("sub-sales-mgr", "job_sub_title_catalog", "SALES_MGR", "Sales Manager", "Leading revenue generation and sales pipeline"),
            AdminMasterDataItem("sub-sdr", "job_sub_title_catalog", "SDR", "Sales Development Representative", "Prospecting and lead qualification"),
            AdminMasterDataItem("sub-mkt-mgr", "job_sub_title_catalog", "MKT_MGR", "Marketing Manager", "Campaign strategy and brand positioning"),
            AdminMasterDataItem("sub-cs-lead", "job_sub_title_catalog", "CS_LEAD", "Customer Care Lead", "Omnichannel support supervision")
        )))

        // 4. Industry Catalog (20 official)
        put("industry_catalog", CopyOnWriteArrayList(listOf(
            AdminMasterDataItem("ind-fnb", "industry_catalog", "fnb", "Food & Beverage", "Restoran, kafe, katering, dan makanan-minuman"),
            AdminMasterDataItem("ind-retail", "industry_catalog", "retail", "Retail & E-Commerce", "Perdagangan eceran dan toko online"),
            AdminMasterDataItem("ind-health", "industry_catalog", "healthcare", "Kesehatan & Rumah Sakit", "Klinik, apotek, dan layanan medis"),
            AdminMasterDataItem("ind-const", "industry_catalog", "construction", "Konstruksi & Real Estate", "Kontraktor dan properti"),
            AdminMasterDataItem("ind-mfg", "industry_catalog", "manufacturing", "Manufaktur & Pabrik", "Produksi barang dan industri"),
            AdminMasterDataItem("ind-tech", "industry_catalog", "startup_tech", "Startup Teknologi & SaaS", "Layanan digital dan software"),
            AdminMasterDataItem("ind-umkm", "industry_catalog", "umkm", "UMKM Lintas Sektor", "Usaha mikro kecil menengah"),
            AdminMasterDataItem("ind-prof", "industry_catalog", "professional_services", "Jasa Profesional", "Konsultan hukum, pajak, akuntansi"),
            AdminMasterDataItem("ind-log", "industry_catalog", "logistics", "Logistik & Transportasi", "Ekspedisi dan rantai pasok"),
            AdminMasterDataItem("ind-mine", "industry_catalog", "mining_energy", "Pertambangan & Energi", "Sumber daya alam dan perminyakan")
        )))

        // 5. Subscription Plans
        put("commercial_plans", CopyOnWriteArrayList(listOf(
            AdminMasterDataItem("plan-starter", "commercial_plans", "STARTER", "Starter Plan (Rp 499.000/bln)", "500.000 kredit, 5 Human Seat, 2 AI Agent"),
            AdminMasterDataItem("plan-growth", "commercial_plans", "GROWTH", "Growth Plan (Rp 1.499.000/bln)", "2.000.000 kredit, 20 Human Seat, 6 AI Agent"),
            AdminMasterDataItem("plan-enterprise", "commercial_plans", "ENTERPRISE", "Enterprise Plan (Rp 4.999.000/bln)", "10.000.000 kredit, Unlimited Seat, 15 AI Agent"),
            AdminMasterDataItem("plan-custom", "commercial_plans", "CUSTOM", "Custom Enterprise Contract", "Kustomisasi kuota kredit dan SLA")
        )))

        // 6. Feature Capabilities
        put("feature_capabilities", CopyOnWriteArrayList(listOf(
            AdminMasterDataItem("feat-multimodal", "feature_capabilities", "MULTIMODAL_IMAGE_STUDIO", "Multimodal Creative Studio", "Akses pembuatan banner dan konten visual beresolusi tinggi"),
            AdminMasterDataItem("feat-rag-memory", "feature_capabilities", "RAG_COMPANY_BRAIN", "Company Brain & Memory Decay", "Vector store dan ekstraksi dokumen tenant"),
            AdminMasterDataItem("feat-custom-mcp", "feature_capabilities", "CUSTOM_MCP_TOOLS", "Custom MCP Tool Registry", "Eksekusi fungsi custom sandboxed"),
            AdminMasterDataItem("feat-biometric", "feature_capabilities", "BIOMETRIC_ATTENDANCE", "Biometric Presence Verification", "Validasi wajah dan geolokasi anti-spoofing"),
            AdminMasterDataItem("feat-dlq-replay", "feature_capabilities", "DLQ_WORKFLOW_REPLAY", "Dead Letter Queue & Replay", "Audit trail dan replay deterministik")
        )))

        // 7. Creative Layout Templates
        put("creative_layout_templates", CopyOnWriteArrayList(listOf(
            AdminMasterDataItem("tmpl-hero-banner", "creative_layout_templates", "HERO_BANNER", "E-Commerce Hero Banner", "16:9 Banner promosi diskon musiman dengan typography modern"),
            AdminMasterDataItem("tmpl-product-card", "creative_layout_templates", "PRODUCT_CARD", "Social Media Product Showcase", "1:1 Feed Instagram kartu produk dengan border aksen"),
            AdminMasterDataItem("tmpl-exec-summary", "creative_layout_templates", "EXECUTIVE_BRIEF", "Executive Daily Briefing Layout", "Header ringkas dengan matriks KPI 4 kuadran")
        )))

        // 8. AI Job Titles (15 official)
        put("ai_job_titles", CopyOnWriteArrayList(listOf(
            AdminMasterDataItem("jt-cos", "ai_job_titles", "chief_of_staff", "AI Chief of Staff", "Koordinator seluruh Staff AI Agent dan Executive Briefing"),
            AdminMasterDataItem("jt-intel", "ai_job_titles", "company_intelligence", "AI Company Intelligence", "Pemantau pasar, kompetitor, dan sinyal peluang"),
            AdminMasterDataItem("jt-ops", "ai_job_titles", "operations", "AI Operations", "Monitoring mesin, armada, produksi, dan efisiensi"),
            AdminMasterDataItem("jt-sales", "ai_job_titles", "sales", "AI Sales", "Discovery, rekomendasi produk, upsell/cross-sell, closing"),
            AdminMasterDataItem("jt-cs", "ai_job_titles", "customer_service", "AI Customer Service", "Resolusi komplain, FAQ, refund, dan omnichannel support"),
            AdminMasterDataItem("jt-hr", "ai_job_titles", "hr_recruitment", "AI HR & Recruitment", "Analisis kebutuhan karyawan, kehadiran, dan rekrutmen"),
            AdminMasterDataItem("jt-fin", "ai_job_titles", "finance", "AI Finance", "Cash flow, AR/AP, budget variance, dan tax e-faktur")
        )))

        // 9. AI Structural Roles (20 official)
        put("ai_structural_roles", CopyOnWriteArrayList(listOf(
            AdminMasterDataItem("str-sdr", "ai_structural_roles", "sdr", "AI SDR", "Kualifikasi awal lead dan penggalian profil calon pembeli"),
            AdminMasterDataItem("str-cons", "ai_structural_roles", "sales_consultant", "AI Sales Consultant", "Konsultasi produk dan analisis kebutuhan teknis"),
            AdminMasterDataItem("str-cls", "ai_structural_roles", "closer", "AI Closer", "Menangani keberatan harga dan mendorong konversi closing"),
            AdminMasterDataItem("str-flw", "ai_structural_roles", "follow_up", "AI Follow-Up Agent", "Follow-up lead dingin dan abandoned cart checkout"),
            AdminMasterDataItem("str-cmp", "ai_structural_roles", "campaign_specialist", "AI Campaign Specialist", "Perencanaan dan otomatisasi pengiriman campaign marketing")
        )))

        // 10. Approved External Sources
        put("approved_external_sources", CopyOnWriteArrayList(listOf(
            AdminMasterDataItem("src-idx", "approved_external_sources", "IDX_FINANCIALS", "Indonesia Stock Exchange Data", "Laporan keuangan emiten resmi dan keterbukaan informasi"),
            AdminMasterDataItem("src-bi-rates", "approved_external_sources", "BANK_INDONESIA_RATES", "Bank Indonesia Kurs & Suku Bunga", "Nilai tukar mata uang JISDOR dan suku bunga acuan"),
            AdminMasterDataItem("src-bpom", "approved_external_sources", "BPOM_REGULATIONS", "BPOM Official Drug & Food Directory", "Database sertifikasi dan registrasi produk BPOM RI"),
            AdminMasterDataItem("src-kemendag", "approved_external_sources", "KEMENDAG_TRADE_DATA", "Kementerian Perdagangan Pasar Domestik", "Data harga komoditas pangan pokok nasional")
        )))
    }

    // 3. MCP Tools Store (Bagian D)
    val mcpTools = CopyOnWriteArrayList<AdminMcpToolItem>().apply {
        addAll(
            listOf(
                AdminMcpToolItem(
                    id = "tool-crm-lead",
                    name = "crm_fetch_lead",
                    description = "Fetches CRM lead record by ID or customer email from Supabase/Hubspot",
                    inputSchema = """{"type": "object", "properties": {"leadId": {"type": "string"}}, "required": ["leadId"]}""",
                    riskLevel = "LOW",
                    requiredRole = "STAFF_HUMAN",
                    restrictedToOperationMode = "UNRESTRICTED",
                    status = "ACTIVE",
                    invocationCount24h = 1420,
                    errorCount24h = 3,
                    avgLatencyMs24h = 42
                ),
                AdminMcpToolItem(
                    id = "tool-db-rev",
                    name = "db_query_revenue",
                    description = "Queries aggregated daily/monthly revenue metrics with tenant data isolation",
                    inputSchema = """{"type": "object", "properties": {"period": {"type": "string"}}, "required": ["period"]}""",
                    riskLevel = "LOW",
                    requiredRole = "STAFF_HUMAN",
                    restrictedToOperationMode = "UNRESTRICTED",
                    status = "ACTIVE",
                    invocationCount24h = 890,
                    errorCount24h = 1,
                    avgLatencyMs24h = 65
                ),
                AdminMcpToolItem(
                    id = "tool-pay-refund",
                    name = "payment_refund_transaction",
                    description = "Processes transaction refund via Midtrans/Xendit payment gateway with mandatory approval",
                    inputSchema = """{"type": "object", "properties": {"trxId": {"type": "string"}, "amount": {"type": "number"}}, "required": ["trxId", "amount"]}""",
                    riskLevel = "CRITICAL",
                    requiredRole = "TENANT_ADMIN",
                    restrictedToOperationMode = "ONLINE_SANDBOX",
                    status = "ACTIVE",
                    invocationCount24h = 14,
                    errorCount24h = 0,
                    avgLatencyMs24h = 380
                ),
                AdminMcpToolItem(
                    id = "tool-telegram-send",
                    name = "telegram_send_broadcast",
                    description = "Sends broadcast notification to registered tenant Telegram bot channels",
                    inputSchema = """{"type": "object", "properties": {"message": {"type": "string"}}, "required": ["message"]}""",
                    riskLevel = "MEDIUM",
                    requiredRole = "STAFF_HUMAN",
                    restrictedToOperationMode = "UNRESTRICTED",
                    status = "ACTIVE",
                    invocationCount24h = 345,
                    errorCount24h = 2,
                    avgLatencyMs24h = 110
                ),
                AdminMcpToolItem(
                    id = "tool-ocr-extractor",
                    name = "document_ocr_scanner",
                    description = "Extracts structured key-value entities from invoices, purchase orders, and contracts",
                    inputSchema = """{"type": "object", "properties": {"documentUrl": {"type": "string"}}, "required": ["documentUrl"]}""",
                    riskLevel = "LOW",
                    requiredRole = "STAFF_HUMAN",
                    restrictedToOperationMode = "OFFLINE_ONLY",
                    status = "ACTIVE",
                    invocationCount24h = 620,
                    errorCount24h = 5,
                    avgLatencyMs24h = 520
                )
            )
        )
    }

    // 4. Third-Party App Registry Store (Bagian E)
    val appRegistry = CopyOnWriteArrayList<AdminAppRegistryItem>().apply {
        addAll(
            listOf(
                AdminAppRegistryItem(
                    id = "app-sap-erp",
                    appName = "SAP ERP Enterprise Connector",
                    appType = "ERP",
                    clientId = "client_sap_981",
                    scopes = listOf("erp:read", "inventory:sync", "orders:write"),
                    status = "ACTIVE",
                    capabilityStatus = "SUPPORTED",
                    authType = "OAUTH2"
                ),
                AdminAppRegistryItem(
                    id = "app-salesforce",
                    appName = "Salesforce CRM Sync",
                    appType = "CRM",
                    clientId = "client_sf_421",
                    scopes = listOf("crm:leads:write", "crm:contacts:read"),
                    status = "ACTIVE",
                    capabilityStatus = "SUPPORTED",
                    authType = "OAUTH2"
                ),
                AdminAppRegistryItem(
                    id = "app-tokopedia-omni",
                    appName = "Tokopedia Marketplace Gateway",
                    appType = "MARKETPLACE",
                    clientId = "client_tokped_019",
                    scopes = listOf("orders:read", "inventory:update"),
                    status = "ACTIVE",
                    capabilityStatus = "MIGRATION_REQUIRED",
                    manualLinkMigrationNotice = "Perlu migrasi ke webhook manual link sesuai kebijakan API v2 Marketplace",
                    authType = "OAUTH2"
                ),
                AdminAppRegistryItem(
                    id = "app-shopee-seller",
                    appName = "Shopee Open Platform Connector",
                    appType = "MARKETPLACE",
                    clientId = "client_shopee_842",
                    scopes = listOf("shop:orders", "logistics:tracking"),
                    status = "ACTIVE",
                    capabilityStatus = "SUPPORTED",
                    authType = "OAUTH2"
                ),
                AdminAppRegistryItem(
                    id = "app-legacy-midtrans",
                    appName = "Midtrans SNAP Legacy Connector",
                    appType = "PAYMENT",
                    clientId = "client_midtrans_v1",
                    scopes = listOf("payment:charge"),
                    status = "DEPRECATED",
                    capabilityStatus = "DEPRECATED",
                    manualLinkMigrationNotice = "Konektor ini telah digantikan oleh Payment Reconciliation Engine v2",
                    authType = "API_KEY"
                )
            )
        )
    }

    // 5. Skill Plugins Store (Bagian C)
    val skillPlugins = CopyOnWriteArrayList<AdminSkillPluginItem>().apply {
        addAll(
            listOf(
                AdminSkillPluginItem(
                    id = "skill-sentiment-analyser",
                    name = "Enterprise Sentiment Analysis & Triage",
                    version = "1.4.0",
                    author = "Orchestree Core",
                    runtime = "WASM",
                    status = "APPROVED",
                    downloads = 1420,
                    declaredTools = listOf("sentiment_analyzer", "ticket_auto_triage"),
                    riskScore = 0.05
                ),
                AdminSkillPluginItem(
                    id = "skill-tax-faktur",
                    name = "Indonesian E-Faktur Generator & Tax Calc",
                    version = "2.1.0",
                    author = "Nusantara FinTech",
                    runtime = "JVM_NATIVE",
                    status = "APPROVED",
                    downloads = 890,
                    declaredTools = listOf("efaktur_builder", "djp_validator"),
                    riskScore = 0.12
                ),
                AdminSkillPluginItem(
                    id = "skill-predictive-churn",
                    name = "B2B Churn Predictor & Retention Alert",
                    version = "0.9.1",
                    author = "AI Labs Partner",
                    runtime = "PYTHON_CONTAINER",
                    status = "PENDING_APPROVAL",
                    downloads = 12,
                    declaredTools = listOf("churn_model_v1"),
                    riskScore = 0.28
                ),
                AdminSkillPluginItem(
                    id = "skill-whatsapp-catalogue",
                    name = "WhatsApp Catalog & Cart Syncer",
                    version = "1.0.5",
                    author = "Commerce Boost",
                    runtime = "WASM",
                    status = "APPROVED",
                    downloads = 640,
                    declaredTools = listOf("wa_catalog_tool"),
                    riskScore = 0.08
                )
            )
        )
    }
}
