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
        val hashedExternalId = hashIdentifier(externalUserId)

        // 1. Direct search in PostgreSQL customer_channel_identities & customers tables
        var foundCustomerId: String? = null
        var foundDisplayName: String = senderDisplayName
        var foundPhone: String? = rawPhone
        var foundEmail: String? = rawEmail

        try {
            val conn = DatabaseManager.getConnection()
            if (conn != null) {
                conn.use { c ->
                    // 1a. Check by channel identity
                    val queryIdentity = """
                        SELECT c.id, c.display_name, c.primary_channel 
                        FROM customer_channel_identities cci
                        JOIN customers c ON c.id = cci.customer_id
                        WHERE c.tenant_id = ? AND cci.channel_type = ? 
                          AND (cci.channel_external_id = ? OR cci.channel_identifier_hash = ?)
                          AND (c.merged_into_id IS NULL)
                        LIMIT 1
                    """.trimIndent()
                    c.prepareStatement(queryIdentity).use { ps ->
                        ps.setString(1, tenantId)
                        ps.setString(2, channelType.uppercase())
                        ps.setString(3, externalUserId)
                        ps.setString(4, hashedExternalId)
                        ps.executeQuery().use { rs ->
                            if (rs.next()) {
                                foundCustomerId = rs.getString("id")
                                foundDisplayName = rs.getString("display_name") ?: senderDisplayName
                            }
                        }
                    }

                    // 1b. If not found, match by verified phone/email in customer_attributes
                    if (foundCustomerId == null && (rawPhone != null || rawEmail != null)) {
                        val queryAttr = """
                            SELECT c.id, c.display_name 
                            FROM customer_attributes ca
                            JOIN customers c ON c.id = ca.customer_id
                            WHERE c.tenant_id = ? AND (
                                (ca.attribute_key = 'phone' AND ca.attribute_value = ? AND ? IS NOT NULL) OR
                                (ca.attribute_key = 'email' AND ca.attribute_value = ? AND ? IS NOT NULL)
                            )
                            AND (c.merged_into_id IS NULL)
                            LIMIT 1
                        """.trimIndent()
                        c.prepareStatement(queryAttr).use { ps ->
                            ps.setString(1, tenantId)
                            ps.setString(2, rawPhone)
                            ps.setString(3, rawPhone)
                            ps.setString(4, rawEmail)
                            ps.setString(5, rawEmail)
                            ps.executeQuery().use { rs ->
                                if (rs.next()) {
                                    foundCustomerId = rs.getString("id")
                                    foundDisplayName = rs.getString("display_name") ?: senderDisplayName
                                }
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

        // 2. Create new customer record in PostgreSQL
        val newCustomerId = "cust-${UUID.randomUUID().toString().take(8)}"
        val profile = ResolvedCustomerProfile(
            id = newCustomerId,
            tenantId = tenantId,
            displayName = senderDisplayName.ifBlank { "User ${maskIdentifier(externalUserId)}" },
            primaryChannel = channelType,
            verifiedPhone = rawPhone,
            verifiedEmail = rawEmail,
            confidenceScore = 0.90,
            resolutionStatus = "SINGLETON"
        )

        try {
            val conn = DatabaseManager.getConnection()
            if (conn != null) {
                conn.use { c ->
                    c.autoCommit = false
                    try {
                        // Insert into customers
                        c.prepareStatement("""
                            INSERT INTO customers (id, tenant_id, display_name, primary_channel, is_active, created_at, updated_at)
                            VALUES (?, ?, ?, ?, true, NOW(), NOW())
                        """.trimIndent()).use { psCust ->
                            psCust.setString(1, newCustomerId)
                            psCust.setString(2, tenantId)
                            psCust.setString(3, profile.displayName)
                            psCust.setString(4, channelType.uppercase())
                            psCust.executeUpdate()
                        }

                        // Insert into customer_channel_identities
                        val identityId = "cci-${UUID.randomUUID().toString().take(8)}"
                        c.prepareStatement("""
                            INSERT INTO customer_channel_identities (id, customer_id, channel_type, channel_external_id, channel_identifier_hash, verified_at, created_at)
                            VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                        """.trimIndent()).use { psIdent ->
                            psIdent.setString(1, identityId)
                            psIdent.setString(2, newCustomerId)
                            psIdent.setString(3, channelType.uppercase())
                            psIdent.setString(4, externalUserId)
                            psIdent.setString(5, hashedExternalId)
                            psIdent.executeUpdate()
                        }

                        // Optional attribute records for phone & email
                        if (!rawPhone.isNullOrBlank()) {
                            c.prepareStatement("""
                                INSERT INTO customer_attributes (id, customer_id, attribute_key, attribute_value, source, confidence, updated_at)
                                VALUES (?, ?, 'phone', ?, 'inbound_channel', 0.95, NOW())
                            """.trimIndent()).use { psAttr ->
                                psAttr.setString(1, "attr-${UUID.randomUUID().toString().take(8)}")
                                psAttr.setString(2, newCustomerId)
                                psAttr.setString(3, rawPhone)
                                psAttr.executeUpdate()
                            }
                        }
                        if (!rawEmail.isNullOrBlank()) {
                            c.prepareStatement("""
                                INSERT INTO customer_attributes (id, customer_id, attribute_key, attribute_value, source, confidence, updated_at)
                                VALUES (?, ?, 'email', ?, 'inbound_channel', 0.95, NOW())
                            """.trimIndent()).use { psAttr ->
                                psAttr.setString(1, "attr-${UUID.randomUUID().toString().take(8)}")
                                psAttr.setString(2, newCustomerId)
                                psAttr.setString(3, rawEmail)
                                psAttr.executeUpdate()
                            }
                        }

                        c.commit()
                    } catch (e: Exception) {
                        c.rollback()
                        throw e
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Could not insert resolved customer profile to DB: ${e.message}")
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
                // Re-link conversations, orders, channel identities to targetCustomerId
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
                    conn.prepareStatement("UPDATE customer_channel_identities SET customer_id = ? WHERE customer_id = ?").use { ps ->
                        ps.setString(1, targetCustomerId)
                        ps.setString(2, candId)
                        ps.executeUpdate()
                    }
                    // Mark merged in customers with correct column merged_into_id
                    conn.prepareStatement("UPDATE customers SET merged_into_id = ?, is_active = false, updated_at = NOW() WHERE tenant_id = ? AND id = ?").use { ps ->
                        ps.setString(1, targetCustomerId)
                        ps.setString(2, tenantId)
                        ps.setString(3, candId)
                        ps.executeUpdate()
                    }
                    // Audit log
                    conn.prepareStatement("""
                        INSERT INTO customer_merge_audit_logs (id, tenant_id, source_customer_id, target_customer_id, action, performed_by, performed_at, details_json)
                        VALUES (?, ?, ?, ?, 'MERGE_APPROVED', 'SYSTEM', NOW(), ?::jsonb)
                    """.trimIndent()).use { psAudit ->
                        psAudit.setString(1, "cma-${UUID.randomUUID().toString().take(8)}")
                        psAudit.setString(2, tenantId)
                        psAudit.setString(3, candId)
                        psAudit.setString(4, targetCustomerId)
                        psAudit.setString(5, """{"reason": "Admin or automated identity resolution merge"}""")
                        psAudit.executeUpdate()
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
