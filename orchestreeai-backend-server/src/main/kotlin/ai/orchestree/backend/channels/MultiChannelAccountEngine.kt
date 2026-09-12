package ai.orchestree.backend.channels

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.UUID

@Serializable
data class ChannelAccount(
    val id: String,
    val tenantId: String,
    val channelType: String, // WHATSAPP, TELEGRAM, SHOPEE, TIKTOK, INSTAGRAM, EMAIL, WEB_CHAT
    val accountName: String,
    val externalIdentifier: String,
    val externalIdentifierHash: String,
    val operationMode: ChannelOperationMode = ChannelOperationMode.AI_AUTOPILOT,
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

object MultiChannelAccountEngine {
    private val logger = LoggerFactory.getLogger(MultiChannelAccountEngine::class.java)

    fun hashExternalIdentifier(externalId: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(externalId.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    suspend fun registerChannelAccount(
        tenantId: String,
        channelType: String,
        accountName: String,
        externalIdentifier: String,
        operationMode: ChannelOperationMode = ChannelOperationMode.AI_AUTOPILOT,
        credentialsEncrypted: String = ""
    ): ChannelAccount = withContext(Dispatchers.IO) {
        val id = "acc-${channelType.lowercase()}-${UUID.randomUUID().toString().take(6)}"
        val hash = hashExternalIdentifier(externalIdentifier)
        val now = System.currentTimeMillis()

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        INSERT INTO channel_accounts 
                        (id, tenant_id, channel_type, account_label, external_identifier, external_identifier_hash, 
                         operation_mode, credentials_encrypted, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                        ON CONFLICT (id) DO UPDATE SET 
                            account_label = EXCLUDED.account_label,
                            operation_mode = EXCLUDED.operation_mode,
                            updated_at = EXCLUDED.updated_at
                    """.trimIndent()).use { ps ->
                        ps.setString(1, id)
                        ps.setString(2, tenantId)
                        ps.setString(3, channelType.uppercase())
                        ps.setString(4, accountName)
                        ps.setString(5, externalIdentifier)
                        ps.setString(6, hash)
                        ps.setString(7, operationMode.name)
                        ps.setString(8, credentialsEncrypted)
                        ps.setTimestamp(9, java.sql.Timestamp(now))
                        ps.setTimestamp(10, java.sql.Timestamp(now))
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not insert channel account into DB: ${e.message}")
            }
        }

        ChannelAccount(
            id = id,
            tenantId = tenantId,
            channelType = channelType.uppercase(),
            accountName = accountName,
            externalIdentifier = externalIdentifier,
            externalIdentifierHash = hash,
            operationMode = operationMode,
            status = "ACTIVE",
            createdAt = now
        )
    }

    suspend fun getAccountByExternalId(tenantId: String, externalId: String): ChannelAccount? = withContext(Dispatchers.IO) {
        val hash = hashExternalIdentifier(externalId)
        val conn = DatabaseManager.getConnection() ?: return@withContext null
        try {
            conn.use { c ->
                c.prepareStatement("""
                    SELECT id, tenant_id, channel_type, account_label, external_identifier, external_identifier_hash, operation_mode, status, created_at
                    FROM channel_accounts
                    WHERE tenant_id = ? AND (external_identifier = ? OR external_identifier_hash = ?)
                    LIMIT 1
                """.trimIndent()).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, externalId)
                    ps.setString(3, hash)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val modeStr = rs.getString("operation_mode") ?: "AI_AUTOPILOT"
                            val mode = try { ChannelOperationMode.valueOf(modeStr) } catch (_: Exception) { ChannelOperationMode.AI_AUTOPILOT }
                            val createdTs = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis()
                            return@withContext ChannelAccount(
                                id = rs.getString("id"),
                                tenantId = rs.getString("tenant_id"),
                                channelType = rs.getString("channel_type"),
                                accountName = rs.getString("account_label") ?: "Account",
                                externalIdentifier = rs.getString("external_identifier") ?: externalId,
                                externalIdentifierHash = rs.getString("external_identifier_hash") ?: hash,
                                operationMode = mode,
                                status = rs.getString("status") ?: "ACTIVE",
                                createdAt = createdTs
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Could not retrieve channel account: ${e.message}")
        }
        null
    }
}
