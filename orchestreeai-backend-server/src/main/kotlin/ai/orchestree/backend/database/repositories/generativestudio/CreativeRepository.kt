package ai.orchestree.backend.database.repositories.generativestudio

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.serialization.Serializable

@Serializable
data class CreativeAssetModel(
    val id: String,
    val tenantId: String,
    val title: String,
    val mediaUrl: String,
    val platform: String
)

class CreativeRepository(
    private val supabase: SupabaseClientProvider
) {
    suspend fun getAssets(tenantId: String): Result<String> {
        return supabase.queryTable("creative_assets", tenantId)
    }
}
