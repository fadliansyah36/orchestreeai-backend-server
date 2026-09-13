package ai.orchestree.backend

import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.security.AuditLogger
import ai.orchestree.backend.security.EncryptionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Phase2CoreInfrastructureVerificationTest {

    @Test
    fun testRealEncryptionServiceAESGCM() {
        val service = EncryptionService()
        val plainText = "enterprise-secret-api-key-998822"
        val encrypted = service.encrypt(plainText)

        assertTrue(encrypted.startsWith("enc:v1:"), "Encrypted string must have enc:v1: prefix")
        assertNotEquals(plainText, encrypted, "Encryption must not be plaintext no-op")

        val decrypted = service.decrypt(encrypted)
        assertEquals(plainText, decrypted, "Decryption must restore original text")
    }

    @Test
    fun testAuditLoggerPersistence() = runBlocking {
        val logger = AuditLogger()
        val actionName = "ACTION_VERIFY_CORE_INFRA_" + System.currentTimeMillis()
        logger.log("tenant-p2-test", "user-admin", actionName, "Details test", "SUCCESS")
        assertEquals(1, logger.inMemoryLogs.size)

        // Verify direct DB connection and persistence
        val conn = DatabaseManager.getConnection()
        assertNotNull(conn, "Database connection must not be null")
        delay(500) // Wait for async DB persist
        
        conn.use { c ->
            c.prepareStatement("SELECT action, tenant_id FROM audit_logs WHERE action = ?").use { ps ->
                ps.setString(1, actionName)
                ps.executeQuery().use { rs ->
                    assertTrue(rs.next(), "Persisted audit log should exist in audit_logs table")
                    assertEquals(actionName, rs.getString("action"))
                    assertEquals("tenant-p2-test", rs.getString("tenant_id"))
                }
            }
        }
    }
}

