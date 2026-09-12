package ai.orchestree.backend.billing

import ai.orchestree.backend.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

object DatabaseManager {
    private val logger = LoggerFactory.getLogger(DatabaseManager::class.java)

    @JvmName("fetchDatabaseUrl")
    fun getDatabaseUrl(): String {
        val candidate = ai.orchestree.backend.config.EnvLoader.get("DATABASE_POOL_URL").ifBlank {
            ai.orchestree.backend.config.EnvLoader.get("DATABASE_DIRECT_URL").ifBlank {
                ai.orchestree.backend.config.EnvLoader.get("DATABASE_URL").ifBlank {
                    AppConfig.load().supabase.databaseDirectUrl.ifBlank {
                        AppConfig.load().supabase.databaseUrl
                    }
                }
            }
        }
        return candidate
    }

    val databaseUrl: String get() = getDatabaseUrl()

    fun getConnection(): Connection? {
        return try {
            val url = databaseUrl
            if (url.isNotBlank() && (url.startsWith("postgres") || url.startsWith("jdbc:postgres"))) {
                val clean = url.removePrefix("jdbc:")
                val uri = java.net.URI(clean)
                val host = uri.host
                val port = if (uri.port != -1) uri.port else 5432
                val path = uri.path.trimStart('/')
                val userInfo = uri.userInfo ?: ""
                val user = if (userInfo.contains(":")) userInfo.substringBefore(":") else userInfo
                val pass = if (userInfo.contains(":")) userInfo.substringAfter(":") else ""

                val jdbcUrl = "jdbc:postgresql://$host:$port/$path"
                val props = java.util.Properties().apply {
                    if (user.isNotBlank()) setProperty("user", user)
                    if (pass.isNotBlank()) setProperty("password", pass)
                    setProperty("ssl", "true")
                    setProperty("sslmode", "require")
                }
                Class.forName("org.postgresql.Driver")
                DriverManager.getConnection(jdbcUrl, props)
            } else null
        } catch (e: Throwable) {
            val target = try { java.net.URI(databaseUrl.removePrefix("jdbc:")).let { "${it.host}:${if (it.port != -1) it.port else 5432}" } } catch (_: Exception) { "unknown-target" }
            logger.warn("Database direct connection to target $target could not be established (check GKE VPC connector / firewall egress rules): ${e.message}")
            null
        }
    }

    suspend fun <T> transaction(block: suspend (Connection) -> T): T = withContext(Dispatchers.IO) {
        val conn = getConnection() ?: error("Database connection unavailable for transaction")
        conn.autoCommit = false
        try {
            val result = block(conn)
            conn.commit()
            result
        } catch (e: Exception) {
            try {
                conn.rollback()
            } catch (rbEx: Exception) {
                logger.error("Failed to rollback transaction: ${rbEx.message}", rbEx)
            }
            throw e
        } finally {
            try {
                conn.close()
            } catch (_: Exception) {}
        }
    }
}
