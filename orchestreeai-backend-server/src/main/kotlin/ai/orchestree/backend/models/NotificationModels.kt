package ai.orchestree.backend.models

import java.time.Instant

enum class NotificationCategory(val label: String, val description: String) {
    TASK("Tugas & Workflow", "Pembaruan status task, delegasi AI, dan approval permintaan"),
    COMPETITOR_INSIGHT("Intelijen Kompetitor", "Deteksi perubahan strategi, harga, dan fitur kompetitor"),
    PERFORMANCE_ALERT("Performa & Skor", "Anomali produktivitas, penurunan skor KPI, dan review mingguan"),
    INTEGRATION_HEALTH("Kesehatan Integrasi", "Status koneksi webhook, degradasi API eksternal, dan token kedaluwarsa"),
    SECURITY("Keamanan & Audit", "Insiden login mencurigakan, eskalasi hak akses, dan sesi impersonasi"),
    BILLING("Tagihan & Pembayaran", "Peringatan invoice jatuh tempo, kegagalan autodebet, dan limit token"),
    SYSTEM("Sistem & Pemeliharaan", "Pengumuman pemeliharaan sistem, pembaruan versi, dan broadcast")
}

enum class NotificationSeverity(val label: String) {
    INFO("Info"),
    WARNING("Peringatan"),
    CRITICAL("Kritis")
}

data class NotificationDto(
    val id: String,
    val tenantId: String,
    val userId: String,
    val category: NotificationCategory,
    val title: String,
    val body: String,
    val deepLinkRoute: String? = null,
    val isRead: Boolean = false,
    val severity: NotificationSeverity = NotificationSeverity.INFO,
    val sourceEventId: String? = null,
    val metadataJson: String = "{}",
    val createdAt: Instant = Instant.now(),
    val readAt: Instant? = null
)

data class NotificationPreferenceDto(
    val id: String,
    val tenantId: String,
    val userId: String,
    val category: NotificationCategory,
    val isInAppEnabled: Boolean = true,
    val updatedAt: Instant = Instant.now()
)

data class UpdateNotificationPreferenceRequest(
    val category: NotificationCategory,
    val isInAppEnabled: Boolean
)

data class NotificationPageResponse(
    val items: List<NotificationDto>,
    val totalCount: Int,
    val unreadCount: Int,
    val page: Int,
    val pageSize: Int
)

data class FanoutEventPayload(
    val tenantId: String,
    val category: NotificationCategory,
    val title: String,
    val body: String,
    val deepLinkRoute: String? = null,
    val severity: NotificationSeverity = NotificationSeverity.INFO,
    val sourceEventId: String,
    val targetUserIds: List<String> = emptyList(), // If empty, resolved via targetRole or targetDepartmentId
    val targetRole: UserRole? = null,
    val targetDepartmentId: String? = null,
    val metadataJson: String = "{}"
)
