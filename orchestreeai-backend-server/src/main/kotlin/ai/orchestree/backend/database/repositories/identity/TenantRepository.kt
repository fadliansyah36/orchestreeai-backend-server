package ai.orchestree.backend.database.repositories.identity

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.serialization.Serializable

@Serializable
data class TenantModel(
    val id: String,
    val name: String,
    val tier: String = "GROWTH",
    val status: String = "ACTIVE"
)

class TenantRepository(
    private val supabase: SupabaseClientProvider
) {
    suspend fun getTenant(tenantId: String): Result<String> {
        return supabase.queryTable("tenants", tenantId)
    }
}
