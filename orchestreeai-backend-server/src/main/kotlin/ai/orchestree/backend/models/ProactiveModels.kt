package ai.orchestree.backend.models

import java.time.Instant

/**
 * Proactive Agent Domain Models (PRD Section 11)
 */
enum class ProactiveChannel(val label: String, val protocol: String) {
    WHATSAPP("WhatsApp", "Meta Cloud API (v20.0)"),
    TELEGRAM("Telegram", "Telegram Bot API")
}

enum class ProactiveNotifType(val label: String, val description: String) {
    DAILY_REPORT_PAGI("Daily Report Pagi", "Ringkasan agenda, task prioritas, dan deadline hari ini"),
    TASK_REMINDER("Task Reminder", "Peringatan task to-do dan in-progress sebelum jatuh tempo"),
    COMPETITOR_INTEL("Competitor Intel", "Peringatan real-time sinyal & aksi diskon/produk kompetitor"),
    COMPANY_NEWS("Company News", "Pengumuman dan buletin internal perusahaan"),
    DEADLINE_ALERT("Deadline Alert", "Eskalasi darurat task yang mendekati SLA batas akhir"),
    OWNER_WEEKLY_BRIEF("Owner Weekly Brief", "Executive summary performa tim, KPI, dan insight mingguan")
}

data class ProactiveSubscriptionDto(
    val id: String,
    val tenantId: String,
    val staffId: String,
    val staffName: String,
    val staffRole: String,
    val channel: ProactiveChannel,
    val destinationNumber: String,
    val enabledNotifTypes: List<ProactiveNotifType>,
    val sendTimes: List<String>,
    val timezone: String = "Asia/Jakarta (WIB)",
    val isVerified: Boolean = true,
    val isActive: Boolean = true,
    val maxDailyMessages: Int = 5,
    val messagesSentToday: Int = 0,
    val assignedAgentId: String? = null,
    val assignedAgentName: String = "Proactive People Agent",
    val lastSentAt: Instant? = null,
    val optOutAt: Instant? = null
)

data class CreateProactiveSubscriptionRequest(
    val staffId: String,
    val staffName: String,
    val staffRole: String,
    val channel: ProactiveChannel,
    val destinationNumber: String,
    val enabledNotifTypes: List<ProactiveNotifType>,
    val sendTimes: List<String>,
    val timezone: String = "Asia/Jakarta (WIB)",
    val assignedAgentId: String? = null,
    val assignedAgentName: String? = null
)

data class UpdateProactiveSubscriptionRequest(
    val channel: ProactiveChannel? = null,
    val destinationNumber: String? = null,
    val enabledNotifTypes: List<ProactiveNotifType>? = null,
    val sendTimes: List<String>? = null,
    val timezone: String? = null,
    val isActive: Boolean? = null,
    val maxDailyMessages: Int? = null,
    val assignedAgentId: String? = null,
    val assignedAgentName: String? = null
)

data class ProactiveMessageLogDto(
    val id: String,
    val tenantId: String,
    val staffId: String,
    val staffName: String,
    val channel: ProactiveChannel,
    val messageType: ProactiveNotifType,
    val content: String,
    val sentAt: Instant = Instant.now(),
    val status: String = "DELIVERED",
    val agentSenderName: String,
    val jobId: String? = null,
    val characterCount: Int = 0,
    val latencyMs: Long = 0,
    val errorMessage: String? = null
)

data class ChannelVerificationRequest(
    val staffId: String,
    val staffName: String,
    val channel: ProactiveChannel,
    val destination: String
)

data class ChannelVerificationResponse(
    val verificationId: String,
    val channel: ProactiveChannel,
    val destination: String,
    val isOtpSent: Boolean,
    val deepLinkUrl: String? = null,
    val message: String
)

data class VerifyOtpRequest(
    val verificationId: String,
    val otpCode: String
)

data class InboundWebhookPayload(
    val channel: ProactiveChannel,
    val fromDestination: String,
    val messageText: String,
    val rawTimestamp: Long = System.currentTimeMillis()
)

data class WebhookProcessResult(
    val actionTaken: String,
    val responseMessage: String,
    val success: Boolean
)
