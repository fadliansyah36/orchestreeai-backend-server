package ai.orchestree.backend.models

import kotlinx.serialization.Serializable

@Serializable
enum class AgentStatus {
    ONLINE,
    OFFLINE,
    BUSY,
    IDLE,
    MAINTENANCE,
    ACTIVE,
    PAUSED,
    WORKING,
    ERROR;

    val label: String get() = name
}

@Serializable
enum class TaskColumn {
    BACKLOG,
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    REVIEW,
    DONE;

    val label: String
        get() = when (this) {
            BACKLOG -> "Backlog"
            TODO -> "To Do"
            IN_PROGRESS -> "In Progress"
            IN_REVIEW, REVIEW -> "Review"
            DONE -> "Done"
        }
}

@Serializable
enum class TaskPriority(val label: String) {
    LOW("Rendah"),
    MEDIUM("Sedang"),
    HIGH("Tinggi"),
    CRITICAL("Kritis")
}

@Serializable
enum class InsightCategory(val label: String) {
    PRICING("Penetapan Harga"),
    PROMO_MARKETING("Promosi & Pemasaran"),
    PRODUCT_LAUNCH("Peluncuran Produk"),
    CAMPAIGN("Kampanye"),
    CONTENT_STRATEGY("Strategi Konten"),
    GENERAL("Umum"),
    OTHER("Lainnya")
}

@Serializable
enum class InsightDecision(val label: String) {
    PENDING_REVIEW("Menunggu Tinjauan"),
    APPROVED("Disetujui"),
    REJECTED("Ditolak"),
    AUTO_APPLIED("Diterapkan Otomatis"),
    IGNORED("Diabaikan")
}

@Serializable
enum class InsightImportance {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

@Serializable
enum class IntegrationPlatform(val label: String) {
    SHOPIFY("Shopify"),
    TOKOPEDIA("Tokopedia"),
    SHOPEE("Shopee"),
    TIKTOK_SHOP("TikTok Shop"),
    WOOCOMMERCE("WooCommerce"),
    WHATSAPP("WhatsApp"),
    TELEGRAM("Telegram"),
    INSTAGRAM("Instagram"),
    FACEBOOK("Facebook"),
    FACEBOOK_MESSENGER("Facebook Messenger"),
    META_ADS("Meta Ads"),
    MARKETPLACE_SHOPEE("Shopee Marketplace"),
    MARKETPLACE_TOKOPEDIA("Tokopedia Marketplace"),
    MS_TEAMS("Microsoft Teams"),
    TRELLO("Trello"),
    TIKTOK("TikTok"),
    WEBSITE("Website Webhook"),
    SLACK("Slack"),
    EMAIL("Email"),
    HUBSPOT("HubSpot"),
    ZENDESK("Zendesk"),
    JIRA("Jira"),
    NOTION("Notion"),
    GOOGLE_SHEETS("Google Sheets"),
    CUSTOM_WEBHOOK("Custom Webhook")
}

@Serializable
enum class IntegrationStatus(val label: String) {
    CONNECTED("Connected"),
    EXPIRING_SOON("Expiring Soon"),
    REQUIRES_MIGRATION("Requires Migration"),
    DISCONNECTED("Disconnected"),
    ERROR("Error"),
    SYNCING("Syncing"),
    PENDING_AUTH("Pending Auth")
}

@Serializable
enum class ContentPublishStatus {
    DRAFT,
    SCHEDULED,
    PUBLISHED,
    FAILED,
    APPROVED,
    PENDING_APPROVAL,
    CANCELLED;

    val label: String
        get() = when (this) {
            DRAFT -> "Draft"
            SCHEDULED -> "Scheduled"
            PUBLISHED -> "Published"
            FAILED -> "Failed"
            APPROVED -> "Approved"
            PENDING_APPROVAL -> "Pending Approval"
            CANCELLED -> "Cancelled"
        }
}

@Serializable
enum class SyncType(val label: String = "") {
    FULL("Sinkronisasi Penuh"),
    INCREMENTAL("Inkremental"),
    REALTIME("Realtime"),
    HEALTH_CHECK("Pemeriksaan Koneksi"),
    CONTENT_PUBLISH("Publikasi Konten"),
    SCOPE_AUDIT("Audit Akses"),
    METADATA_OBSERVATION("Observasi Metadata"),
    TOKEN_REFRESH("Pembaruan Token")
}

@Serializable
enum class SyncStatus(val label: String = "") {
    SUCCESS("Sukses"),
    WARNING("Peringatan"),
    ERROR("Gagal"),
    FAILED("Gagal"),
    IN_PROGRESS("Sedang Berjalan"),
    PENDING("Menunggu")
}

@Serializable
enum class MemoryType {
    SEMANTIC_COMPANY,
    COMPETITIVE,
    EPISODIC,
    SEMANTIC,
    PROCEDURAL,
    WORKING,
    PROCEDURAL_SOP,
    EXECUTIVE_DIRECTIVE;

    val sourceType: String
        get() = when (this) {
            SEMANTIC_COMPANY, SEMANTIC -> "semantic_company"
            COMPETITIVE -> "competitive"
            EPISODIC -> "episodic"
            PROCEDURAL, PROCEDURAL_SOP -> "procedural"
            WORKING -> "working"
            EXECUTIVE_DIRECTIVE -> "executive"
        }
}

@Serializable
enum class LlmProvider {
    OPENROUTER,
    GROQ,
    DEEPSEEK,
    GEMINI,
    ANTHROPIC,
    OPENAI
}
