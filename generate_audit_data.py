import os, re

base = "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend"

def find_snippet(rel_path, pattern, context_lines=2):
    path = os.path.join(base, rel_path)
    if not os.path.exists(path):
        # try without base
        path = rel_path
        if not os.path.exists(path):
            return f"FILE_NOT_FOUND: {rel_path}"
    with open(path) as f:
        lines = f.readlines()
    for i, l in enumerate(lines):
        if re.search(pattern, l):
            start = max(0, i)
            end = min(len(lines), i + context_lines)
            snip = " ".join([lines[k].strip() for k in range(start, end)])
            return f"{rel_path}:{i+1} `{snip[:140]}`"
    return f"{rel_path} (pattern not found: {pattern})"

checks = [
    # 1. Core Infra
    ("Core Infrastructure", "Fail-Closed Startup Gate", "startup/StartupValidator.kt", "fun validateStartupEnvironment|class StartupValidator",
     "Terhubung Penuh", "Mengecek environment variables kritis (JWT, Supabase, Redis, LLM keys) saat startup, fail-closed jika shouldExitProcessOnFailure=true."),
    
    ("Core Infrastructure", "Supabase Client (service_role)", "database/SupabaseClientProvider.kt", "service_role|SUPABASE_SERVICE_ROLE_KEY",
     "Terhubung Penuh", "SupabaseClientProvider menginisialisasi client dengan SUPABASE_SERVICE_ROLE_KEY untuk akses bypass RLS backend."),
    
    ("Core Infrastructure", "Redis", "database/RedisService.kt", "class RedisService|JedisPool",
     "Terhubung Penuh", "RedisService mengelola JedisPool untuk caching token, rate limiting, dan session cache."),
    
    ("Core Infrastructure", "JWT Authentication", "plugins/Authentication.kt", "configureAuthentication|jwt\(",
     "Terhubung Penuh", "Authentication.kt memasang verifier Ktor JWT dengan HMAC256 dan validasi claim userId/tenantId."),
    
    ("Core Infrastructure", "CORS", "plugins/HTTP.kt", "install\(CORS\)|configureCORS",
     "Terhubung Penuh", "HTTP.kt mengonfigurasi Ktor CORS plugin dengan allowHost, allowHeaders, dan allowMethod."),
    
    ("Core Infrastructure", "Rate Limiter", "security/RateLimiter.kt", "class RateLimiter|fun checkRateLimit",
     "Terhubung Penuh", "RateLimiter memeriksa limit IP & Tenant per menit dengan sliding window Redis dan fallback in-memory."),
    
    ("Core Infrastructure", "Encryption Service", "security/EncryptionService.kt", "class EncryptionService|fun encrypt",
     "Terhubung Penuh", "EncryptionService mengenkripsi payload dengan format enc:v1: menggunakan EnvelopeEncryptionService (AES-256-GCM + Master Key)."),
    
    ("Core Infrastructure", "Audit Logger", "security/AuditLogger.kt", "fun log|persistToDatabase",
     "Terhubung Penuh", "AuditLogger mencatat audit trail dan explainability trace ke database (tabel audit_logs) dengan fallback in-memory."),

    # 2. Orchestration Core/Engine
    ("Orchestration Core/Engine", "OrchestrationEngine", "orchestration/OrchestrationEngine.kt", "class OrchestrationEngine|fun runWorkflow",
     "Terhubung Penuh", "Engine mengeksekusi DAG WorkflowNode, melacak context state, dan mencatat eksekusi ke WorkflowExecutionRepository."),
    
    ("Orchestration Core/Engine", "WorkflowNode (Classify/Plan/Tool Call/Generate/Human Approval/Deliver)", "orchestration/StandardWorkflowNodes.kt", "class ClassifyWorkflowNode|class ToolCallWorkflowNode",
     "Terhubung Penuh", "Semua tipe node standar (Classify, Plan, ToolCall, LlmGenerate, HumanApproval, Deliver) diimplementasikan."),
    
    ("Orchestration Core/Engine", "Durable Execution/Checkpointing", "orchestration/WorkflowExecutionRepository.kt", "suspend fun save|checkpoint",
     "Terhubung Penuh", "Menyimpan state eksekusi & context JSON ke tabel workflow_executions dan memulihkan saat startup (recoverInterruptedWorkflows)."),
    
    ("Orchestration Core/Engine", "Human-in-the-Loop Interrupt/Resume", "orchestration/HumanInTheLoopGate.kt", "class HumanInTheLoopGate|fun pauseForApproval",
     "Terhubung Penuh", "HumanApprovalWorkflowNode menghentikan alur dan membuat record PendingApproval di DB; saat di-approve, workflow di-resume."),

    # 3. Model Router (Multi-LLM)
    ("Model Router (Multi-LLM)", "Dynamic Fallback Chain", "modelrouter/ModelRouter.kt", "suspend fun execute|class ModelRouter",
     "Terhubung Penuh", "Fallback cascade dinamis antar provider dengan circuit breaker jika primary provider error/timeout."),
    
    ("Model Router (Multi-LLM)", "NVIDIA NIM", "modelrouter/providers/OpenAiCompatibleLlmClient.kt", "NvidiaNim|api.nvcf.nvidia.com",
     "Terhubung Penuh", "Klien OpenAI-compatible yang mendukung endpoint NVIDIA NIM (Llama-3, Mistral, dll.) via HTTP."),
    
    ("Model Router (Multi-LLM)", "OpenRouter", "modelrouter/providers/OpenAiCompatibleLlmClient.kt", "OpenRouter|openrouter.ai",
     "Terhubung Penuh", "Klien OpenAI-compatible yang memanggil OpenRouter API dengan dynamic model selection."),
    
    ("Model Router (Multi-LLM)", "GPT-Image-2 + fallback", "modelrouter/providers/GptImage2Client.kt", "class GptImage2Client|fun generateImage",
     "Terhubung Penuh", "GptImage2Client memanggil Apimart/GPT-Image-2 dengan fallback routing ke Dall-E-3 dan SDXL."),
    
    ("Model Router (Multi-LLM)", "Cost Optimization (Prompt Caching, Semantic Cache, Model Tiering)", "cost/SemanticResponseCacheManager.kt", "class SemanticResponseCacheManager|class ModelTieringEngine",
     "Terhubung Penuh", "SemanticResponseCacheManager, PromptCacheManager, dan ModelTieringEngine mengoptimalkan biaya query LLM."),
    
    ("Model Router (Multi-LLM)", "Health Check Engine", "modelrouter/HealthCheckEngine.kt", "class HealthCheckEngine|suspend fun checkHealth",
     "Terhubung Penuh", "HealthCheckEngine menguji latency & responsivitas setiap provider AI secara berkala."),

    # 4. Intelligence Layer
    ("Intelligence Layer", "Intent Classifier", "intelligence/IntentClassifier.kt", "class IntentClassifier|fun classify",
     "Terhubung Penuh", "Klasifikasi intent pengguna berbasis aturan cepat & ModelRouter LLM jika ambigu."),
    
    ("Intelligence Layer", "Context Resolver", "intelligence/ContextResolver.kt", "class ContextResolver|fun forStaffDailyBrief",
     "Setengah Jalan", "ContextResolver mengumpulkan metadata & memory snippets; forStaffDailyBrief memiliki fallback hardcode string."),
    
    ("Intelligence Layer", "Risk Engine", "intelligence/RiskEngine.kt", "class RiskEngine|fun evaluateOutboundMessage",
     "Setengah Jalan", "Heuristik blacklist kata kunci statis ('bocorkan data', 'rahasia bank') dan rumus risiko numerik."),
    
    ("Intelligence Layer", "Confidence Engine (+ Calibration)", "intelligence/ConfidenceEngine.kt", "class ConfidenceEngine|fun calculateConfidence",
     "Terhubung Penuh", "ConfidenceEngine menghitung skor keyakinan & ConfidenceCalibrationJob mengkalibrasi probabilitas ke database."),
    
    ("Intelligence Layer", "Output Validator", "intelligence/OutputValidator.kt", "class OutputValidator|fun validateOutput",
     "Terhubung Penuh", "Memvalidasi ketersediaan data, grounding fakta, dan formatting output AI sebelum dikirim."),
    
    ("Intelligence Layer", "Cross-System Correlator", "intelligence/CrossSystemCorrelator.kt", "class CrossSystemCorrelator|fun correlate",
     "Terhubung Penuh", "Mengkorelasikan data lintas domain (task, order, lead) dari database untuk analisis anomali."),
    
    ("Intelligence Layer", "Continuous Learning Core", "learning/ContinuousLearningCore.kt", "class ContinuousLearningCore|fun recordCorrection",
     "Terhubung Penuh", "Mencatat human corrections & feedback loop ke database untuk peningkatan akurasi berkelanjutan."),

    # 5. Semantic Memory & RAG
    ("Semantic Memory & RAG", "Hybrid Search Engine (pgvector + full-text)", "memory/HybridSearchEngine.kt", "class HybridSearchEngine|suspend fun search",
     "Terhubung Penuh", "Memanggil Supabase RPC hybrid_match_documents (pgvector + FTS) dengan fallback in-memory RRF."),
    
    ("Semantic Memory & RAG", "Memory Consolidator", "memory/MemoryConsolidator.kt", "class MemoryConsolidator|fun evaluateInteraction",
     "Terhubung Penuh", "Mengevaluasi interaksi berdasarkan importance, novelty, specificity sebelum disimpan ke memory."),
    
    ("Semantic Memory & RAG", "Memory Decay Job", "scheduler/jobs/MemoryDecayJob.kt", "class MemoryDecayJob|suspend fun execute",
     "Terhubung Penuh", "Job scheduler berkala yang mengurangi relevance weight memori lama dan mengarsipkan di database."),

    # 6. MCP Tool Registry & Executor
    ("MCP Tool Registry & Executor", "Tool CRUD", "mcptools/McpToolRegistry.kt", "class McpToolRegistry|fun register",
     "Terhubung Penuh", "Registrasi, pembaruan, dan pengambilan definisi tool dari database mcp_tools dan in-memory cache."),
    
    ("MCP Tool Registry & Executor", "Risk Tier", "mcptools/McpToolDefinition.kt", "enum class McpRiskLevel|val riskLevel",
     "Terhubung Penuh", "Tingkat risiko LOW, MEDIUM, HIGH, CRITICAL terdefinisi dan diperiksa oleh McpGovernanceEngine."),
    
    ("MCP Tool Registry & Executor", "Kill-Switch", "api/AdminRoutes.kt", "patch\(\"/mcp-tools/\{id\}/kill-switch\"|is_kill_switched",
     "Setengah Jalan", "Endpoint kill-switch mengupdate kolom is_kill_switched di DB, tapi McpToolExecutor belum mengecek flag ini saat execute."),
    
    ("MCP Tool Registry & Executor", "Restricted Operation Mode", "mcptools/McpGovernanceEngine.kt", "class McpGovernanceEngine|fun evaluateGovernance",
     "Setengah Jalan", "Hanya mengecek role TENANT_ADMIN untuk CRITICAL tools, restricted operation mode belum ditegakkan penuh di executor."),

    # 7. Channel Gateway
    ("Channel Gateway", "WhatsApp Adapter (QR/Official)", "channels/adapters/WhatsAppAdapter.kt", "class WhatsAppAdapter|fun normalize",
     "Setengah Jalan", "Hanya kelas normalizer payload Map ke InboundMessage (15 baris), belum ada integrasi HTTP outbound Cloud API / QR."),
    
    ("Channel Gateway", "Telegram Adapter (Bot Pribadi/Official)", "channels/adapters/TelegramOfficialBotService.kt", "class TelegramOfficialBotService|class TelegramAdapter",
     "Terhubung Penuh", "TelegramOfficialBotService mengirim pesan via Telegram Bot API HTTP client; TelegramAdapter menormalisasi payload."),
    
    ("Channel Gateway", "Instagram/TikTok/Marketplace Adapter", "channels/adapters/InstagramAdapter.kt", "class InstagramAdapter|class TikTokAdapter",
     "Setengah Jalan", "Hanya berupa data class / payload normalizer tanpa pemanggilan HTTP API langsung ke platform eksternal."),
    
    ("Channel Gateway", "Isolation Layer (5 Lapisan)", "channels/isolation/ChannelIsolationEngine.kt", "class ChannelIsolationEngine|fun validateIsolation",
     "Terhubung Penuh", "Memvalidasi isolasi tenant, credential separation, audit tagging, dan session isolation pada channel."),

    # 8. Webhook Handlers
    ("Webhook Handlers", "Payment (Midtrans/Xendit)", "sales/PaymentWebhookEngine.kt", "class PaymentWebhookEngine|suspend fun handleWebhook",
     "Terhubung Penuh", "Memproses notifikasi status pembayaran, memvalidasi signature SHA-512 Midtrans, dan mengupdate saldo wallet/order di DB."),
    
    ("Webhook Handlers", "Telegram", "webhooks/TelegramWebhookHandler.kt", "class TelegramWebhookHandler|suspend fun handleUpdate",
     "Terhubung Penuh", "Menerima update webhook Telegram, memvalidasi bot token, dan meneruskan ke OrchestrationEngine."),
    
    ("Webhook Handlers", "Meta", "webhooks/WhatsAppWebhookHandler.kt", "class WhatsAppWebhookHandler|suspend fun handleWebhook",
     "Terhubung Penuh", "Menerima webhook WhatsApp/Meta, verifikasi challenge token, dan meneruskan pesan masuk ke ChannelGateway."),
    
    ("Webhook Handlers", "Signature Validator", "webhooks/WebhookSignatureValidator.kt", "class WebhookSignatureValidator|fun isValidMidtransSignature",
     "Terhubung Penuh", "Memvalidasi tanda tangan kriptografis HMAC / SHA-512 untuk setiap payload webhook penyedia pembayaran."),
    
    ("Webhook Handlers", "Payment Reconciliation Job", "sales/PaymentReconciliationEngine.kt", "class PaymentReconciliationEngine|class PaymentReconciliationJob",
     "Terhubung Penuh", "Memeriksa selisih invoice vs order di DB, mendeteksi payment anomaly, dan mencatat ke DLQ queue jika gagal."),

    # 9. Scheduler Engine
    ("Scheduler Engine", "Proactive Daily Report Job", "scheduler/jobs/ProactiveDailyReportJob.kt", "class ProactiveDailyReportJob|suspend fun execute",
     "Terhubung Penuh", "Dieksekusi loop setiap 24 jam oleh SchedulerEngine untuk setiap active tenant, menyintesis KPI harian."),
    
    ("Scheduler Engine", "Health Check Job", "scheduler/jobs/HealthCheckJob.kt", "class HealthCheckJob|suspend fun execute",
     "Terhubung Penuh", "Dieksekusi setiap 15 menit oleh SchedulerEngine, memeriksa kesehatan semua provider AI."),
    
    ("Scheduler Engine", "Competitor Crawl Job", "scheduler/jobs/CompetitorCrawlJob.kt", "class CompetitorCrawlJob|suspend fun execute",
     "Setengah Jalan", "Dieksekusi setiap 6 jam, tapi di SchedulerEngine di-hardcode tenant-default dan https://example.com jika tidak ada target."),
    
    ("Scheduler Engine", "Trial Expiry Job", "scheduler/jobs/TrialExpiryJob.kt", "class TrialExpiryJob|suspend fun execute",
     "Terhubung Penuh", "Dieksekusi setiap 12 jam, mencari langganan yang sudah berakhir dan mengubah status ke EXPIRED di database."),
    
    ("Scheduler Engine", "Credit Expiration Job", "scheduler/jobs/CreditExpirationJob.kt", "class CreditExpirationJob|suspend fun execute",
     "Terhubung Penuh", "Dieksekusi setiap 24 jam, menghanguskan kuota subscription credit yang melewati billing cycle di tabel ai_credit_wallets."),
    
    ("Scheduler Engine", "Dead Letter Queue", "database/repositories/scheduler/DeadLetterQueueRepository.kt", "class DeadLetterQueueRepository|suspend fun pushToDlq",
     "Terhubung Penuh", "Semua 11 job scheduler dibungkus executeWithDlq(), jika retry gagal 3x otomatis masuk tabel dead_letter_queue."),

    # 10. Universal AI Selection Engine
    ("Universal AI Selection Engine", "Data Ingestion (Multi-source)", "intelligence/SelectionEngine.kt", "fun parseMultipartOrJson|fun ingestData",
     "Terhubung Penuh", "Menerima CSV, JSON, Multipart files dan mem-parsing schema tabel ke struktur DataRow."),
    
    ("Universal AI Selection Engine", "Data Understanding", "intelligence/DataUnderstandingSubsystem.kt", "class DataUnderstandingSubsystem|fun analyzeDataset",
     "Terhubung Penuh", "Menganalisis tipe data tiap kolom, menghitung completeness, distribusi nilai, dan anomali data."),
    
    ("Universal AI Selection Engine", "Calibration Engine", "intelligence/SelectionCalibrationSubsystem.kt", "class SelectionCalibrationSubsystem|fun calibrateWeights",
     "Terhubung Penuh", "Menyesuaikan bobot kriteria seleksi berbasis pair-wise ranking atau ModelRouter LLM."),
    
    ("Universal AI Selection Engine", "Scoring & Ranking", "intelligence/SelectionEngine.kt", "fun evaluateCandidate|fun rankResults",
     "Terhubung Penuh", "Menghitung weighted score, confidence level, risk score, dan menghasilkan peringkat kandidat."),
    
    ("Universal AI Selection Engine", "Analytics/Chart Recommendation", "intelligence/SelectionAnalyticsEngine.kt", "class SelectionAnalyticsEngine|fun recommendCharts",
     "Terhubung Penuh", "Menghasilkan rekomendasi grafik (Bar, Pie, Radar, Trend) beserta agregasi metrik dari data seleksi."),

    # 11. Generative Studio Backend
    ("Generative Studio Backend", "Content Planning Layer", "generativestudio/ContentPlanningLayer.kt", "class ContentPlanningLayer|fun planContent",
     "Hardcode/Statis", "Hanya mereturn template string statis tanpa memanggil LLM ('Content caption for $topic', '#OrchestreeAI')."),
    
    ("Generative Studio Backend", "Image Provider Router", "generativestudio/ImageProviderRouter.kt", "class ImageProviderRouter|fun routeProvider",
     "Setengah Jalan", "Hanya string matching 'dall-e-3' / 'sdxl-turbo' / 'gpt-image-2', eksekusi generate image sesungguhnya ada di GenerativeStudioService."),
    
    ("Generative Studio Backend", "Deterministic Renderer", "generativestudio/DeterministicRenderer.kt", "class DeterministicRenderer|fun renderWatermark",
     "Terhubung Penuh", "Melakukan overlay logo/watermark kanvas gambar deterministic murni menggunakan Java BufferedImage / Graphics2D."),
    
    ("Generative Studio Backend", "Metadata Stripping", "generativestudio/MetadataStripper.kt", "MetadataStripper",
     "Tidak Ada Sama Sekali", "File MetadataStripper.kt tidak ditemukan di codebase generative studio."),

    # 12. Commercial & Billing
    ("Commercial & Billing", "Plans/Entitlements", "billing/EntitlementEngine.kt", "object EntitlementEngine|fun checkEntitlement",
     "Terhubung Penuh", "EntitlementEngine memeriksa kuota feature flag per tier plan dari database tabel plan_feature_entitlements."),
    
    ("Commercial & Billing", "Subscription State Machine", "billing/CommercialCreditEngine.kt", "suspend fun startSubscription|suspend fun cancelSubscription",
     "Terhubung Penuh", "Mengelola lifecycle subscription (TRIAL, ACTIVE, PAST_DUE, EXPIRED, CANCELLED) dan transisi upgrade/downgrade di DB."),
    
    ("Commercial & Billing", "Unified AI Credit Ledger (Reserve→Execute→Consume→Refund)", "billing/CommercialCreditEngine.kt", "suspend fun <T> executeWithCreditLifecycle|suspend fun consumeFromWalletWithPriority",
     "Terhubung Penuh", "Siklus 4 tahap kredit: Reserve kuota -> Jalankan aksi AI -> Consume saldo wallet aktual -> Refund jika gagal."),
    
    ("Commercial & Billing", "Credit Metering Rules", "billing/CreditRepositoryManager.kt", "suspend fun getCreditMeteringRule|class CreditMeteringRule",
     "Terhubung Penuh", "Aturan konsumsi kredit per jenis aktivitas (LLM call, Image gen, Selection row) tersimpan di tabel credit_metering_rules."),
    
    ("Commercial & Billing", "Topup", "billing/CommercialCreditEngine.kt", "suspend fun topupCredits|suspend fun handleSubscriptionPaymentWebhook",
     "Terhubung Penuh", "Membuat payment order Midtrans/Xendit dan menambah topup balance di ai_credit_wallets setelah webhook diverifikasi."),
    
    ("Commercial & Billing", "Invoice", "billing/CreditRepositoryManager.kt", "suspend fun getInvoices|suspend fun createInvoice",
     "Terhubung Penuh", "Mencatat commercial invoice ke tabel commercial_invoices dan menyediakan endpoint riwayat tagihan."),
    
    ("Commercial & Billing", "Prospect Registration", "api/AdminRoutes.kt", "post\(\"/prospects\"|prospect_registrations",
     "Terhubung Penuh", "Menyimpan form pendaftaran prospek enterprise ke tabel prospect_registrations di database."),

    # 13. AI Job Titles & Master Data
    ("AI Job Titles & Master Data", "Department Categories", "database/repositories/masterdata/MasterDataRepository.kt", "suspend fun getDepartmentCategories|val depts = listOf",
     "Terhubung Penuh", "14 kategori departemen standar dengan query Supabase dan fallback in-memory catalog."),
    
    ("AI Job Titles & Master Data", "Job Level/Sub-Title Catalog", "database/repositories/masterdata/MasterDataRepository.kt", "suspend fun getJobLevelCatalog|suspend fun getJobSubTitleCatalog",
     "Terhubung Penuh", "Katalog jenjang karir dan sub-title jabatan dengan query Supabase dan fallback in-memory."),
    
    ("AI Job Titles & Master Data", "Industry Catalog", "database/repositories/masterdata/MasterDataRepository.kt", "suspend fun getIndustryCatalog|val industries = listOf",
     "Terhubung Penuh", "20 kategori industri resmi dengan query Supabase dan fallback in-memory catalog."),
    
    ("AI Job Titles & Master Data", "15 AI Job Titles", "database/repositories/masterdata/MasterDataRepository.kt", "suspend fun getAiJobTitles|ai_job_titles",
     "Terhubung Penuh", "15 taksonomi peran AI resmi disimpan dan di-query dari tabel ai_job_titles Supabase."),
    
    ("AI Job Titles & Master Data", "Structural Roles", "database/repositories/masterdata/MasterDataRepository.kt", "suspend fun getAiStructuralRoles|ai_structural_roles",
     "Terhubung Penuh", "Peran struktural turunan dari setiap AI Job Title di-query dari tabel ai_structural_roles Supabase."),
    
    ("AI Job Titles & Master Data", "Skill Plugin Ingestion Engine", "mcptools/SkillPluginUploadEngine.kt", "class SkillPluginUploadEngine|suspend fun uploadAndRegisterSkill",
     "Terhubung Penuh", "Mengunggah file plugin skill (ZIP/JSON), memvalidasi manifest, dan mendaftarkannya ke DB."),

    # 14. Enterprise Modules
    ("Enterprise Modules", "Integration Fabric", "enterprise/EnterpriseIntegrationFabricService.kt", "class EnterpriseIntegrationFabricService|fun registerConnector",
     "Terhubung Penuh", "Konektor integrasi enterprise pihak ketiga (SAP, Salesforce, Slack) tersimpan di DB & memori."),
    
    ("Enterprise Modules", "ABAC Permission Engine", "enterprise/AiDataPermissionService.kt", "class AiDataPermissionService|fun evaluateAccess",
     "Terhubung Penuh", "Mengevaluasi izin akses berbasis atribut (role, department, confidentiality, data scope) ke database."),
    
    ("Enterprise Modules", "Company Context Fabric", "enterprise/CompanyContextFabricService.kt", "class CompanyContextFabricService|fun getCompanyContext",
     "Terhubung Penuh", "Mengambil SOP, profil perusahaan, dan parameter regulasi dari database untuk grounding AI."),
    
    ("Enterprise Modules", "AI Chief of Staff Synthesis", "intelligence/ChiefOfStaffService.kt", "class ChiefOfStaffService|suspend fun generateExecutiveBriefing",
     "Terhubung Penuh", "Menghubungkan data lintas departemen, memanggil ModelRouter, dan menghasilkan briefing eksekutif."),
    
    ("Enterprise Modules", "Specialist Agents", "collaboration/SpecialistAgentCollaborationService.kt", "class SpecialistAgentCollaborationService|suspend fun dispatchCollaboration",
     "Terhubung Penuh", "Mengatur orkestrasi antar AI agent spesialis (Sales, Finance, Ops) melalui message bus kolaborasi."),
    
    ("Enterprise Modules", "Knowledge+Operational Fusion", "intelligence/KnowledgeOperationalFusionEngine.kt", "class KnowledgeOperationalFusionEngine|fun fuseContext",
     "Terhubung Penuh", "Menggabungkan dokumen pengetahuan (SOP) dengan data transaksi riil dari database sebelum inferensi."),
    
    ("Enterprise Modules", "AI Event Engine", "events/AiEventEngine.kt", "class AiEventEngine|fun publishEvent",
     "Terhubung Penuh", "Event bus internal untuk memicu automasi antar agen AI dan mencatat ke company_activity_stream."),

    # 15. Security
    ("Security", "Play Integrity Verification", "security/AppAttestationService.kt", "class GooglePlayIntegrityClient|fun decodeIntegrityToken",
     "Setengah Jalan", "Parsing JWT lokal dan dummy verdict (PLAY_RECOGNIZED) jika token dev/panjang > 20, belum memanggil Google API resmi."),
    
    ("Security", "Prompt Injection Defense", "security/RequestValidationMiddleware.kt", "class PromptInjectionGuard|fun inspect",
     "Terhubung Penuh", "PromptInjectionGuard mendeteksi pola jailbreak, override, dan token injection pada setiap input teks AI."),
    
    ("Security", "SQL Injection Prevention", "security/RequestValidationMiddleware.kt", "fun validateSqlSafe|dangerousTokens",
     "Terhubung Penuh", "Pemeriksaan token SQL berbahaya di middleware dan penggunaan parameter binding (PreparedStatement) di seluruh repository."),
    
    ("Security", "Secrets Management (KMS)", "security/EnvelopeEncryptionService.kt", "class EnvelopeEncryptionService|fun generateDataKey",
     "Terhubung Penuh", "Envelope encryption dengan Data Encryption Key (DEK) lokal yang dienkripsi oleh Master Key."),

    # 16. REST API Layer
    ("REST API Layer", "Auth routes", "api/AuthRoutes.kt", "fun Route.authRoutes|post\(\"/login\"",
     "Terhubung Penuh", "Endpoint login, register, refresh token, tenant onboard terhubung penuh ke UserRepository dan JWT generator."),
    
    ("REST API Layer", "Orchestration routes", "api/OrchestrationRoutes.kt", "fun Route.orchestrationRoutes|post\(\"/workflows/run\"",
     "Terhubung Penuh", "Endpoint trigger workflow, approve/reject HITL, inspect execution history terhubung ke OrchestrationEngine."),
    
    ("REST API Layer", "Chat routes", "api/ChatRoutes.kt", "fun Route.chatRoutes|post\(\"/send\"",
     "Terhubung Penuh", "Endpoint chat conversation terhubung ke ModelRouter, IntentClassifier, dan Session Repository."),
    
    ("REST API Layer", "Generative Studio routes", "api/GenerativeStudioRoutes.kt", "fun Route.generativeStudioRoutes|post\(\"/studio/generate-image\"",
     "Terhubung Penuh", "Endpoint studio generate image, compose prompt, brand asset upload terhubung ke Studio Service & OrchestrationEngine."),
    
    ("REST API Layer", "Billing routes", "api/BillingRoutes.kt", "fun Route.billingRoutes|get\(\"/wallet\"",
     "Terhubung Penuh", "Endpoint wallet, plans, subscribe, topup, downgrade, ledger history terhubung ke CommercialCreditEngine."),
    
    ("REST API Layer", "Admin routes", "api/AdminRoutes.kt", "fun Route.adminRoutes|get\(\"/tenants\"",
     "Terhubung Penuh", "Endpoint kelola tenant, user, MCP tools, system health terhubung ke database dan AdminDomainStores."),
    
    ("REST API Layer", "Public routes", "api/MasterDataRoutes.kt", "fun Route.masterDataPublicRoutes|get\(\"/department-categories\"",
     "Terhubung Penuh", "Endpoint katalog publik departemen, industri, dan jenjang karir terhubung ke MasterDataRepository."),

    # 17. Omnichannel & Sales Marketing (PRD Addendum)
    ("Omnichannel & Sales Marketing", "Unified Customer Profile", "api/OmnichannelSalesRoutes.kt", "get\(\"/customers/search\"|post\(\"/customers/resolve\"",
     "Terhubung Penuh", "Pencarian, resolusi identitas, dan penggabungan profil pelanggan terhubung ke SalesRepository di Supabase."),
    
    ("Omnichannel & Sales Marketing", "Conversation Engine", "api/OmnichannelSalesRoutes.kt", "get\(\"/inbox/conversations\"|post\(\"/conversations/\{convId\}/persona-reply\"",
     "Terhubung Penuh", "Manajemen inbox multi-channel dan AI persona reply terhubung ke SalesRepository & ModelRouter."),
    
    ("Omnichannel & Sales Marketing", "AI Employee Persona", "api/OmnichannelSalesRoutes.kt", "patch\(\"/ai-agents/\{agentId\}/persona\"|class SalesPersonaEngine",
     "Terhubung Penuh", "Konfigurasi persona AI (tone, greeting, closing, boundaries) tersimpan di database dan di-inject ke prompt LLM."),
    
    ("Omnichannel & Sales Marketing", "Lead Scoring", "api/OmnichannelSalesRoutes.kt", "get\(\"/leads\"|post\(\"/leads\"",
     "Terhubung Penuh", "Pengambilan daftar lead dan penambahan lead baru beserta kalkulasi skor kualifikasi di SalesRepository."),
    
    ("Omnichannel & Sales Marketing", "Sales Engine", "sales/ObjectionHandlingEngine.kt", "class ObjectionHandlingEngine|class LeadQualificationEngine",
     "Terhubung Penuh", "Engine penanganan keberatan prospek (objection handling) dan kualifikasi prospek otomatis terhubung ke LLM."),
    
    ("Omnichannel & Sales Marketing", "Commerce Engine", "api/OmnichannelSalesRoutes.kt", "post\(\"/checkout\"|class RealCartCreateTool",
     "Terhubung Penuh", "Alur checkout belanja, keranjang belanja (cart), diskon, dan pembuatan order terhubung ke OrderRepository."),
    
    ("Omnichannel & Sales Marketing", "Marketplace Adapter", "channels/adapters/MarketplaceCommerceAdapter.kt", "class MarketplaceCommerceAdapter|class MarketplaceSocialCommerceEngine",
     "Setengah Jalan", "Adapter struktur data pesanan Tokopedia/Shopee/TikTok Shop ada, namun sinkronisasi API external live masih mock/stub."),
    
    ("Omnichannel & Sales Marketing", "Marketing Campaign Engine", "api/OmnichannelSalesRoutes.kt", "get\(\"/campaigns\"|post\(\"/campaigns\"",
     "Terhubung Penuh", "CRUD dan dispatch kampanye pemasaran multi-channel tersimpan di tabel campaigns database."),
    
    ("Omnichannel & Sales Marketing", "Customer Service & Handover", "api/OmnichannelSalesRoutes.kt", "post\(\"/takeover\"|get\(\"/service-requests\"",
     "Terhubung Penuh", "Pengalihan percakapan dari AI ke agen manusia (human takeover) dan antrean tiket service request di DB."),
    
    ("Omnichannel & Sales Marketing", "Revenue Intelligence", "api/OmnichannelSalesRoutes.kt", "get\(\"/analytics/revenue-intelligence\"|get\(\"/analytics/sales-coach\"",
     "Terhubung Penuh", "Analitik pendapatan, konversi funnel penjualan, dan sales coaching insights terhubung ke DatabaseManager."),
    
    ("Omnichannel & Sales Marketing", "Guardrails", "sales/GroundingOutputValidator.kt", "class GroundingOutputValidator|class PrivacyGuardrailValidator",
     "Terhubung Penuh", "Validasi harga, stok produk, privasi data PII, dan batas toleransi klaim sales sebelum dikirim ke pelanggan."),
    
    ("Omnichannel & Sales Marketing", "Multi-Channel Account Management", "api/OmnichannelSalesRoutes.kt", "get\(\"/channel-accounts\"|post\(\"/channel-accounts\"",
     "Terhubung Penuh", "Pendaftaran akun channel (WhatsApp, Telegram, IG, Marketplace), verifikasi, dan audit health status di DB."),

    # 18. Proactive & Proactive Daily
    ("Proactive & Proactive Daily", "Scope Isolation (Executive Full Summary vs Department Scoped)", "intelligence/ContextResolver.kt", "fun forStaffDailyBrief|restrictToAiJobTitleIds",
     "Setengah Jalan", "Scope isolation membatasi konteks sesuai AI Job Title ID staf, namun logika filter masih fallback ke string statis."),
    
    ("Proactive & Proactive Daily", "Two-Way Messaging", "proactive/ProactiveTwoWayHandler.kt", "class ProactiveTwoWayHandler|suspend fun handleReply",
     "Terhubung Penuh", "Menangani balasan pesan proaktif dari pengguna di channel WhatsApp/Telegram dan mengarahkan ke workflow terkait."),
    
    ("Proactive & Proactive Daily", "Chief of Staff Briefing 5-Node Workflow", "orchestration/OrchestrationEngine.kt", "n1-aggregate-metrics|n4-synthesize-briefing",
     "Setengah Jalan", "Workflow 5-node (Aggregate->Anomaly->Correlate->Synthesize->Deliver) terdaftar di engine, namun node deliver n5 hanya mencatat log string ('Delivered to Slack #exec-leadership').")
]

for domain, subfeat, rel_path, pattern, status, desc in checks:
    evidence = find_snippet(rel_path, pattern)
    print(f"{domain} || {subfeat} || {rel_path} || {status} || {evidence}")
