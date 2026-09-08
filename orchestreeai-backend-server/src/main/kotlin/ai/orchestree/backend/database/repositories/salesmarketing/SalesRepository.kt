package ai.orchestree.backend.database.repositories.salesmarketing

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.serialization.Serializable

@Serializable
data class LeadModel(
    val id: String,
    val tenantId: String,
    val contactName: String,
    val score: Int = 50,
    val stage: String = "NEW"
)

class SalesRepository(
    private val supabase: SupabaseClientProvider
) {
    suspend fun getLeads(tenantId: String): Result<String> {
        return supabase.queryTable("leads", tenantId)
    }
}
