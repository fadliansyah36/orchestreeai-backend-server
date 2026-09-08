package ai.orchestree.backend.database.repositories.enterprise

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.serialization.Serializable

@Serializable
data class StrategicInsightModel(
    val id: String,
    val tenantId: String,
    val domain: String,
    val summary: String,
    val riskLevel: String
)

class CrossDomainRepository(
    private val supabase: SupabaseClientProvider
) {
    suspend fun getInsights(tenantId: String): Result<String> {
        return supabase.queryTable("strategic_insights", tenantId)
    }
}
