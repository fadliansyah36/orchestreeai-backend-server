package ai.orchestree.backend.database.repositories.identity

import ai.orchestree.backend.config.EnvLoader
import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.sql.DriverManager

@Serializable
data class TenantModel(
    val id: String,
    val name: String,
    val tier: String = "GROWTH",
    val status: String = "ACTIVE"
)

open class TenantRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(TenantRepository::class.java)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getTenant(tenantId: String): Result<String> {
        return supabase.queryTable("tenants", tenantId)
    }

    open suspend fun getActiveTenantIds(): List<String> = withContext(Dispatchers.IO) {
        // 1. Try Supabase REST query
        if (supabase.isConfigured()) {
            val res = supabase.queryTableGlobal("tenants", select = "id,status", extraParams = mapOf("status" to "eq.ACTIVE"))
            if (res.isSuccess) {
                try {
                    val arr = json.parseToJsonElement(res.getOrThrow()).jsonArray
                    val ids = arr.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                    if (ids.isNotEmpty()) return@withContext ids
                } catch (e: Exception) {
                    logger.warn("Failed to parse tenants JSON from Supabase: ${e.message}")
                }
            }
        }

        // 2. Try direct JDBC connection if DATABASE_URL is configured
        val dbUrl = EnvLoader.get("DATABASE_URL")
        if (dbUrl.isNotBlank() && dbUrl != "placeholder") {
            try {
                val jdbcUrl = if (!dbUrl.startsWith("jdbc:")) "jdbc:$dbUrl" else dbUrl
                Class.forName("org.postgresql.Driver")
                DriverManager.getConnection(jdbcUrl).use { conn ->
                    val stmt = conn.createStatement()
                    val rs = stmt.executeQuery("SELECT id FROM tenants WHERE status = 'ACTIVE'")
                    val ids = mutableListOf<String>()
                    while (rs.next()) {
                        ids.add(rs.getString("id"))
                    }
                    if (ids.isNotEmpty()) return@withContext ids
                }
            } catch (e: Exception) {
                logger.warn("Failed to query tenants via JDBC: ${e.message}")
            }
        }

        emptyList()
    }

    companion object {
        val instance by lazy { TenantRepository() }
    }
}
