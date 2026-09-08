package ai.orchestree.backend.cost

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.security.MessageDigest

@Serializable
data class CacheLookupResult(
    val hit: Boolean,
    val cachedResponse: String? = null,
    val similarityScore: Double = 0.0,
    val tokensSaved: Int = 0
)

object SemanticResponseCacheManager {
    private val logger = LoggerFactory.getLogger(SemanticResponseCacheManager::class.java)

    fun hashPrompt(prompt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(prompt.trim().lowercase().toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    suspend fun lookupCache(tenantId: String, prompt: String): CacheLookupResult = withContext(Dispatchers.IO) {
        val hash = hashPrompt(prompt)
        val conn = DatabaseManager.getConnection()

        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        SELECT response_text, tokens_count FROM semantic_response_cache 
                        WHERE tenant_id = ? AND prompt_hash = ? AND expires_at > ?
                        LIMIT 1
                    """.trimIndent()).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setString(2, hash)
                        ps.setLong(3, System.currentTimeMillis())
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                val resp = rs.getString("response_text")
                                val tokens = rs.getInt("tokens_count")
                                return@withContext CacheLookupResult(
                                    hit = true,
                                    cachedResponse = resp,
                                    similarityScore = 1.0,
                                    tokensSaved = tokens
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Cache lookup error: ${e.message}")
            }
        }

        CacheLookupResult(hit = false)
    }

    suspend fun storeCache(
        tenantId: String,
        prompt: String,
        responseText: String,
        tokensCount: Int,
        ttlHours: Int = 24
    ) = withContext(Dispatchers.IO) {
        val hash = hashPrompt(prompt)
        val now = System.currentTimeMillis()
        val expiresAt = now + (ttlHours * 3600 * 1000L)

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        INSERT INTO semantic_response_cache 
                        (id, tenant_id, prompt_hash, prompt_raw, response_text, tokens_count, created_at, expires_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET 
                            response_text = EXCLUDED.response_text,
                            expires_at = EXCLUDED.expires_at
                    """.trimIndent()).use { ps ->
                        ps.setString(1, "cache-$hash")
                        ps.setString(2, tenantId)
                        ps.setString(3, hash)
                        ps.setString(4, prompt.take(500))
                        ps.setString(5, responseText)
                        ps.setInt(6, tokensCount)
                        ps.setLong(7, now)
                        ps.setLong(8, expiresAt)
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not persist response cache: ${e.message}")
            }
        }
    }
}
