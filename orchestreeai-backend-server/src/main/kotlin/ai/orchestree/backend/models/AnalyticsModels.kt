package ai.orchestree.backend.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Analytics & Ranking Score Domain Models (PRD Section 9)
 */

data class PerformanceMetricDailyDto(
    val id: String,
    val tenantId: String,
    val entityType: String, // "HUMAN" or "AI_AGENT"
    val entityId: String,
    val entityName: String,
    val departmentId: String? = null,
    val departmentName: String = "",
    val date: String, // "YYYY-MM-DD"
    val tasksTotal: Int = 0,
    val tasksOnTime: Int = 0,
    val qualitySum: Double = 0.0,
    val collaborationEvents: Int = 0,
    val uptimeMinutes: Int = 480,
    val createdAt: Instant = Instant.now()
)

data class PerformanceScoreMonthlyDto(
    val id: String,
    val tenantId: String,
    val entityType: String, // "HUMAN" or "AI_AGENT"
    val entityId: String,
    val entityName: String,
    val departmentId: String? = null,
    val departmentName: String = "",
    val period: String, // e.g. "Agustus 2026"
    val completionRate: Double, // Weight 25%
    val qualityScore: Double, // Weight 20%
    val deadlineDiscipline: Double, // Weight 15%
    val productivityVolume: Double, // Weight 15%
    val collaborationScore: Double, // Weight 15%
    val attendanceUptime: Double, // Weight 10%
    val totalScore: Double, // PRD 9.2 Formula Score
    val rank: Int = 1,
    val trend: String = "0.0%",
    val historicalScores: List<Double> = emptyList()
)

data class PerformanceAlertDto(
    val id: String,
    val tenantId: String,
    val entityId: String,
    val entityName: String,
    val entityType: String,
    val departmentName: String = "",
    val triggerReason: String,
    val scoreDropPct: Double = 0.0,
    val createdAt: Instant = Instant.now(),
    val acknowledgedBy: String? = null,
    val isAcknowledged: Boolean = false
)

data class DepartmentScoreSummaryDto(
    val departmentId: String,
    val departmentName: String,
    val colorHex: String,
    val averageTotalScore: Double,
    val averageCompletion: Double,
    val averageQuality: Double,
    val averageDeadline: Double,
    val averageProductivity: Double,
    val averageCollaboration: Double,
    val averageUptime: Double,
    val memberCount: Int,
    val activeTasksCount: Int,
    val rank: Int
)

data class ManagerNoteDto(
    val id: String,
    val tenantId: String,
    val staffId: String,
    val staffName: String,
    val managerId: String,
    val managerName: String,
    val note: String,
    val actionItems: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class CreateManagerNoteRequest(
    val staffId: String,
    val staffName: String,
    val managerId: String,
    val managerName: String,
    val note: String,
    val actionItems: String = ""
)

data class UpdateManagerNoteRequest(
    val note: String? = null,
    val actionItems: String? = null
)

data class ExportReportRequest(
    val period: String = "Agustus 2026",
    val format: String = "PDF" // "PDF", "XLSX", "CSV"
)

// =============================================================================
// PRD Master Section 9 & Admin Platform Aggregated Analytics DTOs
// =============================================================================

@Serializable
data class AnalyticsOverviewResponse(
    @SerialName("total_transaction_value")
    val total_transaction_value: Double,
    @SerialName("total_revenue_this_month")
    val total_revenue_this_month: Double,
    @SerialName("total_tenants_active")
    val total_tenants_active: Int,
    @SerialName("total_staff_human")
    val total_staff_human: Int,
    @SerialName("total_ai_agents_active")
    val total_ai_agents_active: Int,
    @SerialName("total_repeat_orders")
    val total_repeat_orders: Int,
    @SerialName("total_transactions")
    val total_transactions: Int
)

@Serializable
data class TenantUsageCreditItem(
    val id: String,
    val name: String,
    val balance: Double,
    @SerialName("total_usage_this_month")
    val total_usage_this_month: Double
)

@Serializable
data class LlmUsagePlatformWideResponse(
    @SerialName("total_input_tokens")
    val total_input_tokens: Long,
    @SerialName("total_output_tokens")
    val total_output_tokens: Long,
    @SerialName("total_tokens")
    val total_tokens: Long,
    @SerialName("total_cost_usd")
    val total_cost_usd: Double,
    @SerialName("breakdown_by_provider")
    val breakdown_by_provider: List<ProviderUsageSummary>,
    @SerialName("breakdown_by_tenant")
    val breakdown_by_tenant: List<TenantLlmUsageSummary>,
    @SerialName("daily_trend")
    val daily_trend: List<DailyLlmTrendItem>
)

@Serializable
data class ProviderUsageSummary(
    val provider: String,
    @SerialName("total_tokens")
    val total_tokens: Long,
    @SerialName("input_tokens")
    val input_tokens: Long,
    @SerialName("output_tokens")
    val output_tokens: Long,
    @SerialName("total_cost_usd")
    val total_cost_usd: Double,
    @SerialName("request_count")
    val request_count: Int
)

@Serializable
data class TenantLlmUsageSummary(
    @SerialName("tenant_id")
    val tenant_id: String,
    @SerialName("tenant_name")
    val tenant_name: String,
    @SerialName("total_tokens")
    val total_tokens: Long,
    @SerialName("total_cost_usd")
    val total_cost_usd: Double,
    @SerialName("request_count")
    val request_count: Int
)

@Serializable
data class DailyLlmTrendItem(
    val date: String,
    @SerialName("total_tokens")
    val total_tokens: Long,
    @SerialName("total_cost_usd")
    val total_cost_usd: Double,
    @SerialName("request_count")
    val request_count: Int
)

@Serializable
data class KpiSummaryResponse(
    @SerialName("platform_average_score")
    val platform_average_score: Double,
    @SerialName("tenants_count")
    val tenants_count: Int,
    @SerialName("total_evaluated_entities")
    val total_evaluated_entities: Int,
    @SerialName("average_metrics")
    val average_metrics: KpiMetricsBreakdown,
    @SerialName("human_distribution")
    val human_distribution: EntityKpiSummary,
    @SerialName("ai_agent_distribution")
    val ai_agent_distribution: EntityKpiSummary,
    @SerialName("tier_distribution")
    val tier_distribution: Map<String, Int>
)

@Serializable
data class KpiMetricsBreakdown(
    @SerialName("completion_rate")
    val completion_rate: Double,
    @SerialName("quality_score")
    val quality_score: Double,
    @SerialName("deadline_discipline")
    val deadline_discipline: Double,
    @SerialName("productivity_volume")
    val productivity_volume: Double,
    @SerialName("collaboration_score")
    val collaboration_score: Double,
    @SerialName("attendance_uptime")
    val attendance_uptime: Double
)

@Serializable
data class EntityKpiSummary(
    val count: Int,
    @SerialName("average_score")
    val average_score: Double,
    @SerialName("completion_rate")
    val completion_rate: Double,
    @SerialName("quality_score")
    val quality_score: Double,
    @SerialName("discipline_or_uptime")
    val discipline_or_uptime: Double
)

object KpiScoreEngine {
    /**
     * PRD Master Section 9.2: Formula Skor Bulanan:
     * - Completion Rate: 25%
     * - Quality Score: 20%
     * - Deadline Discipline: 15%
     * - Productivity Volume: 15%
     * - Collaboration Score: 15%
     * - Attendance/Uptime: 10%
     */
    fun monthlyScore(
        completion: Double,
        quality: Double,
        deadline: Double,
        productivity: Double,
        collaboration: Double,
        uptime: Double
    ): Double {
        val score = (0.25 * completion) +
                (0.20 * quality) +
                (0.15 * deadline) +
                (0.15 * productivity) +
                (0.15 * collaboration) +
                (0.10 * uptime)
        return (score * 10.0).let { Math.round(it) / 10.0 }
    }
}

// =============================================================================
// FASE 110 / BAGIAN C: Platform-Wide Task Activity Aggregation (Addendum 2, 25.2)
// Menampilkan ringkasan agregat aktivitas tugas lintas-tenant tanpa membocorkan konten
// =============================================================================

@Serializable
data class TenantTaskActivitySummaryItem(
    @SerialName("tenant_id")
    val tenantId: String,
    @SerialName("tenant_name")
    val tenantName: String,
    @SerialName("total_tasks")
    val totalTasks: Int,
    @SerialName("active_tasks")
    val activeTasks: Int,
    @SerialName("completed_tasks")
    val completedTasks: Int,
    @SerialName("human_created_tasks")
    val humanCreatedTasks: Int,
    @SerialName("ai_agent_created_tasks")
    val aiAgentCreatedTasks: Int,
    @SerialName("orchestration_created_tasks")
    val orchestrationCreatedTasks: Int,
    @SerialName("completion_rate")
    val completionRate: Double,
    @SerialName("adoption_health_status")
    val adoptionHealthStatus: String // "HEALTHY", "MODERATE", "LOW_ACTIVITY"
)

@Serializable
data class TaskActivitySummaryResponse(
    @SerialName("total_tasks")
    val totalTasks: Int,
    @SerialName("total_active_tasks")
    val totalActiveTasks: Int,
    @SerialName("total_completed_tasks")
    val totalCompletedTasks: Int,
    @SerialName("overall_completion_rate")
    val overallCompletionRate: Double,
    @SerialName("human_created_tasks")
    val humanCreatedTasks: Int,
    @SerialName("ai_created_tasks")
    val aiCreatedTasks: Int,
    @SerialName("human_ratio_percentage")
    val humanRatioPercentage: Double,
    @SerialName("ai_ratio_percentage")
    val aiRatioPercentage: Double,
    @SerialName("by_status")
    val byStatus: Map<String, Int>,
    @SerialName("by_channel")
    val byChannel: Map<String, Int>,
    @SerialName("tenants_activity")
    val tenantsActivity: List<TenantTaskActivitySummaryItem>
)

// =============================================================================
// FASE 114 / BAGIAN J / LANGKAH 1: Universal AI Selection & Ranking Usage Aggregation
// GET /api/v1/admin/analytics/universal-selection-usage
// (Addendum 2 Bagian 25.2: Agregat Platform — Privasi Tenant Terjaga)
// =============================================================================

@Serializable
data class TenantSelectionUsageItem(
    @SerialName("tenant_id")
    val tenantId: String,
    @SerialName("tenant_name")
    val tenantName: String,
    @SerialName("total_requests")
    val totalRequests: Int,
    @SerialName("completed_requests")
    val completedRequests: Int,
    @SerialName("processing_requests")
    val processingRequests: Int,
    @SerialName("failed_requests")
    val failedRequests: Int,
    @SerialName("total_credits_consumed")
    val totalCreditsConsumed: Double,
    @SerialName("last_activity_at")
    val lastActivityAt: String? = null
)

@Serializable
data class DomainCategoryUsageItem(
    @SerialName("category")
    val category: String,
    @SerialName("count")
    val count: Int,
    @SerialName("percentage")
    val percentage: Double
)

@Serializable
data class UniversalSelectionUsageResponse(
    @SerialName("total_requests")
    val totalRequests: Int,
    @SerialName("total_completed_requests")
    val totalCompletedRequests: Int,
    @SerialName("total_processing_requests")
    val totalProcessingRequests: Int,
    @SerialName("total_failed_requests")
    val totalFailedRequests: Int,
    @SerialName("total_credits_consumed")
    val totalCreditsConsumed: Double,
    @SerialName("most_used_domain_category")
    val mostUsedDomainCategory: String,
    @SerialName("domain_categories")
    val domainCategories: List<DomainCategoryUsageItem>,
    @SerialName("tenants_usage")
    val tenantsUsage: List<TenantSelectionUsageItem>,
    @SerialName("privacy_notice")
    val privacyNotice: String = "Agregat Platform — Privasi Konten Tenant Terjaga (Addendum 2 Bagian 25.2)"
)



