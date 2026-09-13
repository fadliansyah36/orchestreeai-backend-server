package ai.orchestree.backend.enterprise

import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.security.EnvelopeEncryptionService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class EnterpriseConnection(
    val id: String,
    val tenantId: String,
    val systemName: String,
    val systemType: String, // 'erp', 'crm', 'hris', 'cmms', 'fleet', 'iot', 'finance'
    val connectorKind: String, // 'API_CONNECTOR', 'OAUTH_CONNECTOR', 'WEBHOOK_CONNECTOR', etc.
    val credentialRef: String, // Envelope encrypted
    val endpointUrl: String,
    var status: String = "HEALTHY", // 'HEALTHY', 'ERROR', 'UNREACHABLE', 'SUSPENDED', 'DISCONNECTED'
    var consecutiveFailures: Int = 0,
    var circuitBreakerTripped: Boolean = false,
    var errorReason: String = "",
    var lastSyncAt: Instant? = null,
    var lastPingAt: Instant? = null,
    val createdByUserId: String = "system",
    val createdAt: Instant = Instant.now()
)

data class DataSyncJob(
    val id: String,
    val connectionId: String,
    val tenantId: String,
    val syncMode: String = "NEAR_REALTIME",
    var lastCursor: String = "",
    var lastRunAt: Instant? = null,
    var lastRunStatus: String = "PENDING", // 'SUCCESS', 'FAILED', 'IN_PROGRESS', 'PENDING', 'SUSPENDED'
    var recordsSyncedCount: Int = 0,
    var errorMessage: String = "",
    val createdAt: Instant = Instant.now()
)

data class IngestedRecord(
    val id: String,
    val connectionId: String,
    val tenantId: String,
    val recordType: String, // 'PO', 'EQUIPMENT', 'PROJECT', 'EMPLOYEE', 'INVOICE', 'FLEET_TELEMETRY'
    val externalRecordId: String,
    val entityReference: String = "",
    val dataMode: String = "NEAR_REALTIME",
    val normalizedPayloadJson: String = "{}",
    val ingestedAt: Instant = Instant.now()
)

/**
 * Enterprise Third-Party Integration Fabric Service
 * Implements PRD Addendum 2 Bagian 58:
 * - Reuses Integration Hub (Bagian 10) for OAuth/API Key authentication
 * - Applies Feature Tiering Gate (enforceCapabilityGate("integration_fabric"))
 * - Envelope Encryption for credentials (reusing Bagian 49.6 pattern)
 * - Circuit breaker & rate limiting per connection (reusing Bagian 7.6 pattern)
 */
object EnterpriseIntegrationFabricService {
    private val logger = LoggerFactory.getLogger(EnterpriseIntegrationFabricService::class.java)

    private const val CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5
    private const val RATE_LIMIT_PER_MINUTE = 120

    // In-memory registry for rapid lookups and runtime circuit state
    private val connectionsStore = ConcurrentHashMap<String, EnterpriseConnection>()
    private val syncJobsStore = ConcurrentHashMap<String, DataSyncJob>()
    private val ingestedRecordsStore = ConcurrentHashMap<String, IngestedRecord>()
    private val rateLimitCounters = ConcurrentHashMap<String, MutableList<Long>>()
    private val encryptionService = EnvelopeEncryptionService()

    /**
     * Create and register an external enterprise system connection.
     * MUST pass enforceCapabilityGate(tenantId, "integration_fabric").
     */
    fun createConnection(
        tenantId: String,
        systemName: String,
        systemType: String,
        connectorKind: String,
        plainCredentialsJson: String,
        endpointUrl: String,
        createdByUserId: String = "system"
    ): EnterpriseConnection {
        // Step 1: Enforce Capability Gate per PRD Bagian 58.1 & 57.3
        FeatureCapabilityService.enforceCapabilityGate(tenantId, "integration_fabric")

        // Step 2: Apply Envelope Encryption for credential storage (Bagian 49.6 / 58.2)
        val encryptedCredentialRef = try {
            val envelope = encryptionService.encrypt(plainCredentialsJson)
            "enc:${envelope.encryptedData.take(16)}:${envelope.encryptedKey.take(16)}"
        } catch (e: Exception) {
            logger.warn("Envelope encryption fallback: ${e.message}")
            "enc:${plainCredentialsJson.hashCode()}"
        }

        val connectionId = "conn-${UUID.randomUUID().toString().take(12)}"
        val connection = EnterpriseConnection(
            id = connectionId,
            tenantId = tenantId,
            systemName = systemName,
            systemType = systemType.lowercase(),
            connectorKind = connectorKind,
            credentialRef = encryptedCredentialRef,
            endpointUrl = endpointUrl,
            status = "HEALTHY",
            createdByUserId = createdByUserId
        )

        connectionsStore[connectionId] = connection

        // Persist to Database
        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = """
                    INSERT INTO enterprise_system_connections (
                        id, tenant_id, system_name, system_type, connector_kind, credential_ref, endpoint_url,
                        status, consecutive_failures, circuit_breaker_tripped, created_by_user_id, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, connection.id)
                    ps.setString(2, connection.tenantId)
                    ps.setString(3, connection.systemName)
                    ps.setString(4, connection.systemType)
                    ps.setString(5, connection.connectorKind)
                    ps.setString(6, connection.credentialRef)
                    ps.setString(7, connection.endpointUrl)
                    ps.setString(8, connection.status)
                    ps.setInt(9, 0)
                    ps.setBoolean(10, false)
                    ps.setString(11, connection.createdByUserId)
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to insert connection into DB: ${e.message}")
        }

        logger.info("[INTEGRATION_FABRIC] Successfully registered connection '$connectionId' ($systemName, type: $systemType) for tenant '$tenantId'")
        return connection
    }

    /**
     * Check rate limiting and circuit breaker before dispatching call.
     */
    fun checkConnectionHealthAndRateLimit(connectionId: String): Boolean {
        val conn = connectionsStore[connectionId] ?: getConnectionById(connectionId)
        if (conn == null) return false

        // Check if suspended
        if (conn.status == "SUSPENDED" || conn.circuitBreakerTripped) {
            logger.warn("[CIRCUIT_BREAKER] Connection '$connectionId' call blocked. Status: ${conn.status}, BreakerTripped: ${conn.circuitBreakerTripped}")
            return false
        }

        // Rate limit sliding window (60s)
        val now = System.currentTimeMillis()
        val timestamps = rateLimitCounters.computeIfAbsent(connectionId) { mutableListOf() }
        synchronized(timestamps) {
            timestamps.removeAll { it < now - 60_000L }
            if (timestamps.size >= RATE_LIMIT_PER_MINUTE) {
                logger.warn("[RATE_LIMIT] Connection '$connectionId' exceeded rate limit ($RATE_LIMIT_PER_MINUTE/min)")
                return false
            }
            timestamps.add(now)
        }

        return true
    }

    fun recordConnectionSuccess(connectionId: String) {
        val conn = connectionsStore[connectionId] ?: return
        conn.consecutiveFailures = 0
        conn.circuitBreakerTripped = false
        conn.status = "HEALTHY"
        conn.lastPingAt = Instant.now()
    }

    fun recordConnectionFailure(connectionId: String, reason: String) {
        val conn = connectionsStore[connectionId] ?: return
        conn.consecutiveFailures += 1
        conn.errorReason = reason
        conn.lastPingAt = Instant.now()

        if (conn.consecutiveFailures >= CIRCUIT_BREAKER_FAILURE_THRESHOLD) {
            conn.circuitBreakerTripped = true
            conn.status = "ERROR"
            logger.error("[CIRCUIT_BREAKER_TRIPPED] Connection '$connectionId' tripped after ${conn.consecutiveFailures} consecutive failures: $reason")
        }
    }

    fun ingestRecord(
        connectionId: String,
        tenantId: String,
        recordType: String,
        externalRecordId: String,
        entityReference: String,
        payloadJson: String
    ): IngestedRecord {
        val recordId = "rec-${UUID.randomUUID().toString().take(12)}"
        val record = IngestedRecord(
            id = recordId,
            connectionId = connectionId,
            tenantId = tenantId,
            recordType = recordType,
            externalRecordId = externalRecordId,
            entityReference = entityReference,
            normalizedPayloadJson = payloadJson
        )
        ingestedRecordsStore[recordId] = record

        try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = """
                    INSERT INTO enterprise_ingested_records (
                        id, connection_id, tenant_id, record_type, external_record_id, entity_reference, data_mode, normalized_payload, ingested_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)
                    ON CONFLICT (connection_id, record_type, external_record_id) DO UPDATE SET
                        normalized_payload = EXCLUDED.normalized_payload,
                        entity_reference = EXCLUDED.entity_reference,
                        ingested_at = CURRENT_TIMESTAMP
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, record.id)
                    ps.setString(2, record.connectionId)
                    ps.setString(3, record.tenantId)
                    ps.setString(4, record.recordType)
                    ps.setString(5, record.externalRecordId)
                    ps.setString(6, record.entityReference)
                    ps.setString(7, record.dataMode)
                    ps.setString(8, record.normalizedPayloadJson)
                    ps.executeUpdate()
                }
            }
        } catch (_: Exception) {}

        return record
    }

    fun listConnections(tenantId: String): List<EnterpriseConnection> {
        val list = connectionsStore.values.filter { it.tenantId == tenantId }.toMutableList()
        if (list.isEmpty()) {
            try {
                DatabaseManager.getConnection()?.use { conn ->
                    val sql = "SELECT * FROM enterprise_system_connections WHERE tenant_id = ?"
                    conn.prepareStatement(sql).use { ps ->
                        ps.setString(1, tenantId)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                val item = EnterpriseConnection(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id"),
                                    systemName = rs.getString("system_name"),
                                    systemType = rs.getString("system_type"),
                                    connectorKind = rs.getString("connector_kind"),
                                    credentialRef = rs.getString("credential_ref") ?: "",
                                    endpointUrl = rs.getString("endpoint_url") ?: "",
                                    status = rs.getString("status") ?: "HEALTHY",
                                    consecutiveFailures = rs.getInt("consecutive_failures"),
                                    circuitBreakerTripped = rs.getBoolean("circuit_breaker_tripped"),
                                    errorReason = rs.getString("error_reason") ?: ""
                                )
                                connectionsStore[item.id] = item
                                list.add(item)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return list
    }

    fun getConnectionById(connectionId: String): EnterpriseConnection? {
        return connectionsStore[connectionId] ?: try {
            DatabaseManager.getConnection()?.use { conn ->
                val sql = "SELECT * FROM enterprise_system_connections WHERE id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, connectionId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val item = EnterpriseConnection(
                                id = rs.getString("id"),
                                tenantId = rs.getString("tenant_id"),
                                systemName = rs.getString("system_name"),
                                systemType = rs.getString("system_type"),
                                connectorKind = rs.getString("connector_kind"),
                                credentialRef = rs.getString("credential_ref") ?: "",
                                endpointUrl = rs.getString("endpoint_url") ?: "",
                                status = rs.getString("status") ?: "HEALTHY",
                                consecutiveFailures = rs.getInt("consecutive_failures"),
                                circuitBreakerTripped = rs.getBoolean("circuit_breaker_tripped"),
                                errorReason = rs.getString("error_reason") ?: ""
                            )
                            connectionsStore[item.id] = item
                            item
                        } else null
                    }
                }
            }
        } catch (_: Exception) { null }
    }

    fun listIngestedRecords(tenantId: String, recordType: String? = null): List<IngestedRecord> {
        return ingestedRecordsStore.values.filter { 
            it.tenantId == tenantId && (recordType == null || it.recordType.equals(recordType, ignoreCase = true))
        }
    }
}
