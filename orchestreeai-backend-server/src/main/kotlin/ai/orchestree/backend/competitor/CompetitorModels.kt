package ai.orchestree.backend.competitor

import kotlinx.serialization.Serializable

sealed interface IngestionResult {
    data class Success(
        val rawHtmlHash: String,
        val parsedContent: String,
        val httpStatusCode: Int,
        val responseTimeMs: Long,
        val title: String,
        val metaDescription: String,
        val productsCatalogJson: String = "[]",
        val postFeedJson: String = "[]",
        val adapterUsed: String = "WebIngestionAdapter"
    ) : IngestionResult

    data class Failure(
        val reason: String,
        val statusCode: Int = 0,
        val isBlockedOrCompliance: Boolean = false,
        val requiresManualReview: Boolean = false
    ) : IngestionResult
}

@Serializable
data class CompetitorTarget(
    val id: String,
    val tenantId: String,
    val name: String,
    val url: String,
    val category: String = "GENERAL",
    val insightPrefs: String = "ALL",
    val crawlFrequencyHours: Int = 24,
    val isActive: Boolean = true
)

@Serializable
data class CompetitorSnapshot(
    val id: String,
    val targetId: String,
    val tenantId: String,
    val url: String,
    val rawHtmlHash: String,
    val parsedContent: String,
    val httpStatusCode: Int,
    val responseTimeMs: Long,
    val title: String,
    val metaDescription: String,
    val adapterUsed: String,
    val capturedAt: Long = System.currentTimeMillis()
)

data class DiffResult(
    val hasChanged: Boolean,
    val changeType: String,
    val summary: String,
    val detailsJson: String,
    val magnitudePct: Double
)

@Serializable
data class CompetitorInsight(
    val id: String,
    val targetId: String,
    val tenantId: String,
    val competitorName: String,
    val category: String,
    val summary: String,
    val narrative: String,
    val confidence: Double,
    val impactScore: Double,
    val finalScore: Double,
    val decision: String,
    val importance: String,
    val recommendedAction: String,
    val sourceUrl: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val deliveredChannels: String = "Dashboard, Realtime"
)
