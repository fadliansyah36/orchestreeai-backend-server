package ai.orchestree.backend.customer

import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

@Serializable
data class ResolvedCustomerProfile(
    val id: String,
    val tenantId: String,
    val displayName: String,
    val primaryChannel: String,
    val verifiedPhone: String?,
    val verifiedEmail: String?,
    val confidenceScore: Double,
    val resolutionStatus: String, // 'RESOLVED_AUTOMATIC', 'PENDING_MANUAL_REVIEW', 'SINGLETON'
    val linkedChannelAccountIds: List<String> = emptyList()
)

@Serializable
data class IdentityResolutionCandidate(
    val existingCustomerId: String,
    val candidateCustomerId: String,
    val confidenceScore: Double,
    val matchReason: String,
    val status: String = "PENDING_MERGE"
)

class CustomerIdentityResolutionEngine(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(CustomerIdentityResolutionEngine::class.java)

    fun hashIdentifier(identifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(identifier.trim().lowercase().toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun maskIdentifier(identifier: String): String {
        if (identifier.length <= 4) return "****"
        val prefix = identifier.take(2)
        val suffix = identifier.takeLast(2)
        val masked = "*".repeat(max(4, identifier.length - 4))
        return "$prefix$masked$suffix"
    }

    /**
     * Jaro-Winkler string similarity calculation for name matching (0.0 to 1.0)
     */
    fun calculateNameSimilarity(s1: String, s2: String): Double {
        val str1 = s1.trim().lowercase()
        val str2 = s2.trim().lowercase()
        if (str1 == str2) return 1.0
        if (str1.isEmpty() || str2.isEmpty()) return 0.0

        val maxDist = max(str1.length, str2.length) / 2 - 1
        val str1Matches = BooleanArray(str1.length)
        val str2Matches = BooleanArray(str2.length)

        var matches = 0
        for (i in str1.indices) {
            val start = max(0, i - maxDist)
            val end = min(i + maxDist + 1, str2.length)
            for (j in start until end) {
                if (str2Matches[j]) continue
                if (str1[i] != str2[j]) continue
                str1Matches[i] = true
                str2Matches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        var transpositions = 0
        var k = 0
        for (i in str1.indices) {
            if (!str1Matches[i]) continue
            while (!str2Matches[k]) k++
            if (str1[i] != str2[k]) transpositions++
            k++
        }

        val m = matches.toDouble()
        val jaro = (m / str1.length + m / str2.length + (m - transpositions / 2.0) / m) / 3.0

        // Winkler prefix bonus
        var prefix = 0
        val maxPrefix = min(4, min(str1.length, str2.length))
        for (i in 0 until maxPrefix) {
            if (str1[i] == str2[i]) prefix++ else break
        }

        return jaro + (prefix * 0.1 * (1.0 - jaro))
    }

    /**
     * Resolves incoming channel message author to unified customer identity.
     */
    suspend fun resolveCustomerIdentity(
        tenantId: String,
        channelType: String,
        externalUserId: String,
        senderDisplayName: String,
        rawPhone: String? = null,
        rawEmail: String? = null
    ): ResolvedCustomerProfile = withContext(Dispatchers.IO) {
        val hashedPhone = rawPhone?.let { hashIdentifier(it) }
        val hashedEmail = rawEmail?.let { hashIdentifier(it) }

        // 1. Direct search in PostgreSQL customers table
        var foundCustomerId: String? = null
        var foundDisplayName: String = senderDisplayName
        var foundPhone: String? = rawPhone
        var foundEmail: String? = rawEmail

        try {
            val conn = DatabaseManager.getConnection()
            if (conn != null) {
                conn.use { c ->
                    val query = """
                        SELECT id, display_name, verified_phone, verified_email 
                        FROM customers 
                        WHERE tenant_id = ? AND (
                            external_id = ? OR 
                            (verified_phone = ? AND ? IS NOT NULL) OR 
                            (verified_email = ? AND ? IS NOT NULL)
                        )
                        LIMIT 1
                    """.trimIndent()
                    c.prepareStatement(query).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setString(2, externalUserId)
                        ps.setString(3, rawPhone)
                        ps.setString(4, rawPhone)
                        ps.setString(5, rawEmail)
                        ps.setString(6, rawEmail)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                foundCustomerId = rs.getString("id")
                                foundDisplayName = rs.getString("display_name") ?: senderDisplayName
                                foundPhone = rs.getString("verified_phone")
                                foundEmail = rs.getString("verified_email")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Database lookup failed in identity resolution: ${e.message}")
        }

        if (foundCustomerId != null) {
            return@withContext ResolvedCustomerProfile(
                id = foundCustomerId!!,
                tenantId = tenantId,
                displayName = foundDisplayName,
                primaryChannel = channelType,
                verifiedPhone = foundPhone,
                verifiedEmail = foundEmail,
                confidenceScore = 1.0,
                resolutionStatus = "RESOLVED_AUTOMATIC"
            )
        }

        // 2. Create new customer record
        val newId = "cust-${UUID.randomUUID().toString().take(8)}"
        val profile = ResolvedCustomerProfile(
            id = newId,
            tenantId = tenantId,
            displayName = senderDisplayName.ifBlank { "User ${maskIdentifier(externalUserId)}" },
            primaryChannel = channelType,
            verifiedPhone = rawPhone,
            verifiedEmail = rawEmail,
            confidenceScore = 0.90,
            resolutionStatus = "SINGLETON"
        )

        try {
            val payload = buildJsonObject {
                put("id", profile.id)
                put("tenant_id", profile.tenantId)
                put("display_name", profile.displayName)
                put("primary_channel", profile.primaryChannel)
                put("external_id", externalUserId)
                rawPhone?.let { put("verified_phone", it) }
                rawEmail?.let { put("verified_email", it) }
            }.toString()
            supabase.insertRecord("customers", tenantId, payload)
        } catch (e: Exception) {
            logger.warn("Could not insert resolved customer profile: ${e.message}")
        }

        profile
    }

    /**
     * Merges candidate customer IDs into target customer ID in PostgreSQL.
     */
    suspend fun mergeCustomers(
        tenantId: String,
        targetCustomerId: String,
        candidateCustomerIds: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        logger.info("Merging customers $candidateCustomerIds into $targetCustomerId for tenant $tenantId")
        try {
            val conn = DatabaseManager.getConnection() ?: return@withContext false
            conn.autoCommit = false
            try {
                // Re-link conversations and orders to targetCustomerId
                for (candId in candidateCustomerIds) {
                    conn.prepareStatement("UPDATE conversations SET customer_id = ? WHERE tenant_id = ? AND customer_id = ?").use { ps ->
                        ps.setString(1, targetCustomerId)
                        ps.setString(2, tenantId)
                        ps.setString(3, candId)
                        ps.executeUpdate()
                    }
                    conn.prepareStatement("UPDATE orders SET customer_id = ? WHERE tenant_id = ? AND customer_id = ?").use { ps ->
                        ps.setString(1, targetCustomerId)
                        ps.setString(2, tenantId)
                        ps.setString(3, candId)
                        ps.executeUpdate()
                    }
                    // Mark merged in customers
                    conn.prepareStatement("UPDATE customers SET merged_into = ? WHERE tenant_id = ? AND id = ?").use { ps ->
                        ps.setString(1, targetCustomerId)
                        ps.setString(2, tenantId)
                        ps.setString(3, candId)
                        ps.executeUpdate()
                    }
                }
                conn.commit()
                true
            } catch (e: Exception) {
                conn.rollback()
                logger.error("Failed merging customers: ${e.message}", e)
                false
            }
        } catch (e: Exception) {
            logger.error("DB connection error during customer merge: ${e.message}", e)
            false
        }
    }
}
