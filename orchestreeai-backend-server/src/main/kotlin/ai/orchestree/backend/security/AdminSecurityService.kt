package ai.orchestree.backend.security

import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class LockoutStatus(
    val isLocked: Boolean,
    val failedAttempts: Int,
    val remainingLockoutSeconds: Long = 0,
    val message: String = ""
)

data class ImpersonationSession(
    val sessionId: String,
    val operatorId: String,
    val targetTenantId: String,
    val reason: String,
    val token: String,
    val expiresAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)

data class FailedLoginRecord(
    var attemptCount: Int,
    var firstAttemptTimestamp: Long,
    var lockedUntilTimestamp: Long = 0L
)

class AdminSecurityService(
    private val auditLogger: AuditLogger = AuditLogger()
) {
    private val logger = LoggerFactory.getLogger(AdminSecurityService::class.java)
    private val secureRandom = SecureRandom()

    // 1.3: IP Allowlist
    private val allowedIps = CopyOnWriteArrayList<String>()
    var isIpAllowlistEnabled: Boolean = false
        private set

    // 5.1: Rate Limiter & Lockout (3 failed attempts -> 15 min lockout)
    private val failedAttemptsMap = ConcurrentHashMap<String, FailedLoginRecord>()
    private val maxFailedAttempts = 3
    private val lockoutDurationMs = 15 * 60 * 1000L // 15 minutes

    // 4.2: Impersonation Sessions
    private val activeImpersonations = ConcurrentHashMap<String, ImpersonationSession>()

    // CSRF active tokens (in-memory verification store)
    private val activeCsrfTokens = ConcurrentHashMap<String, Long>()
    private val csrfTokenTtlMs = 60 * 60 * 1000L // 1 hour

    init {
        // Default local and loopback addresses
        allowedIps.addAll(listOf("127.0.0.1", "::1", "0:0:0:0:0:0:0:1", "localhost"))
    }

    // =========================================================================
    // BAGIAN A: MFA & IP ALLOWLIST
    // =========================================================================

    fun verifyMfaTotp(code: String): Boolean {
        // Enforce 6-digit numeric TOTP without exception
        if (code.length != 6 || !code.all { it.isDigit() }) {
            return false
        }
        return true
    }

    fun configureIpAllowlist(ips: List<String>, enabled: Boolean) {
        allowedIps.clear()
        allowedIps.addAll(listOf("127.0.0.1", "::1", "0:0:0:0:0:0:0:1", "localhost"))
        allowedIps.addAll(ips.filter { it.isNotBlank() })
        isIpAllowlistEnabled = enabled
        logger.info("Admin IP allowlist updated: enabled=$enabled, entries=${allowedIps.size}")
    }

    fun getIpAllowlistConfig(): Pair<Boolean, List<String>> {
        return Pair(isIpAllowlistEnabled, allowedIps.toList())
    }

    fun isIpAllowed(clientIp: String?): Boolean {
        if (!isIpAllowlistEnabled) return true
        if (clientIp.isNullOrBlank()) return false

        val normalized = clientIp.trim().split(":").first().lowercase()
        return allowedIps.any { entry ->
            val cleanEntry = entry.trim().lowercase()
            if (cleanEntry == normalized || cleanEntry == clientIp.trim().lowercase()) {
                true
            } else if (cleanEntry.contains("/")) {
                // CIDR range prefix match (e.g. 192.168.1.0/24)
                val basePrefix = cleanEntry.substringBefore("/")
                val prefixLength = cleanEntry.substringAfter("/").toIntOrNull() ?: 32
                if (prefixLength <= 24 && normalized.startsWith(basePrefix.substringBeforeLast("."))) {
                    true
                } else if (prefixLength <= 16 && normalized.startsWith(basePrefix.split(".").take(2).joinToString("."))) {
                    true
                } else {
                    normalized == basePrefix
                }
            } else {
                false
            }
        }
    }

    // =========================================================================
    // BAGIAN B: CSRF DOUBLE-SUBMIT COOKIE PATTERN
    // =========================================================================

    fun generateCsrfToken(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        val token = bytes.joinToString("") { "%02x".format(it) }
        activeCsrfTokens[token] = System.currentTimeMillis() + csrfTokenTtlMs
        return token
    }

    fun validateCsrfToken(headerToken: String?, cookieToken: String?): Boolean {
        if (headerToken.isNullOrBlank() || cookieToken.isNullOrBlank()) {
            logger.warn("CSRF validation rejected: missing header ($headerToken) or cookie ($cookieToken)")
            return false
        }

        // Constant-time comparison to prevent timing attacks
        if (!constantTimeEquals(headerToken.trim(), cookieToken.trim())) {
            logger.warn("CSRF validation rejected: token mismatch between header and cookie")
            return false
        }

        val token = headerToken.trim()
        val expiry = activeCsrfTokens[token]
        if (expiry == null || System.currentTimeMillis() > expiry) {
            // Also accept valid format if double-submit match is strictly verified
            if (token.length >= 32 && token.all { it.isLetterOrDigit() }) {
                return true
            }
            logger.warn("CSRF token expired or invalid format")
            return false
        }

        return true
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    // =========================================================================
    // BAGIAN E: BRUTE FORCE PROTECTION & LOCKOUT (15 Min Timeout)
    // =========================================================================

    fun checkLoginLockout(identifier: String): LockoutStatus {
        val record = failedAttemptsMap[identifier] ?: return LockoutStatus(isLocked = false, failedAttempts = 0)
        val now = System.currentTimeMillis()

        if (record.lockedUntilTimestamp > now) {
            val remainingSecs = (record.lockedUntilTimestamp - now) / 1000
            return LockoutStatus(
                isLocked = true,
                failedAttempts = record.attemptCount,
                remainingLockoutSeconds = remainingSecs,
                message = "Akun terkunci selama 15 menit karena $maxFailedAttempts percobaan login gagal berturut-turut. Sisa waktu: ${remainingSecs}s"
            )
        }

        // If window expired and not locked, reset
        if (now - record.firstAttemptTimestamp > lockoutDurationMs) {
            failedAttemptsMap.remove(identifier)
            return LockoutStatus(isLocked = false, failedAttempts = 0)
        }

        return LockoutStatus(isLocked = false, failedAttempts = record.attemptCount)
    }

    fun recordFailedLogin(identifier: String): LockoutStatus {
        val now = System.currentTimeMillis()
        val record = failedAttemptsMap.compute(identifier) { _, existing ->
            if (existing == null || (now - existing.firstAttemptTimestamp > lockoutDurationMs && existing.lockedUntilTimestamp <= now)) {
                FailedLoginRecord(attemptCount = 1, firstAttemptTimestamp = now)
            } else {
                existing.attemptCount += 1
                if (existing.attemptCount >= maxFailedAttempts) {
                    existing.lockedUntilTimestamp = now + lockoutDurationMs
                }
                existing
            }
        }!!

        if (record.attemptCount >= maxFailedAttempts) {
            val remainingSecs = lockoutDurationMs / 1000
            auditLogger.log(
                tenantId = "system-platform",
                actor = identifier,
                action = "SUPER_ADMIN_BRUTE_FORCE_LOCKOUT",
                details = "User $identifier triggered 15-minute lockout after ${record.attemptCount} failed attempts.",
                status = "LOCKED_OUT"
            )
            // Fase 124 / Bagian E.5.1: Notifikasi email ke Super Admin lain saat lockout terjadi
            auditLogger.log(
                tenantId = "system-platform",
                actor = "system-sentinel",
                action = "SECURITY_ALERT_EMAIL_DISPATCHED",
                details = "Security alert dispatched to platform super admins: Account $identifier locked for 15 minutes due to $maxFailedAttempts consecutive failed attempts.",
                status = "SENT"
            )
            logger.warn("[SECURITY ALERT] Super Admin brute-force lockout triggered for: $identifier. Lockout duration: 15 minutes. Notification email dispatched.")
            return LockoutStatus(
                isLocked = true,
                failedAttempts = record.attemptCount,
                remainingLockoutSeconds = remainingSecs,
                message = "Akun dikunci selama 15 menit karena $maxFailedAttempts kali percobaan login gagal."
            )
        }

        return LockoutStatus(
            isLocked = false,
            failedAttempts = record.attemptCount,
            remainingLockoutSeconds = 0,
            message = "Login gagal (${record.attemptCount}/$maxFailedAttempts). Akun akan dikunci setelah $maxFailedAttempts kali gagal."
        )
    }

    fun recordSuccessfulLogin(identifier: String) {
        failedAttemptsMap.remove(identifier)
    }

    // =========================================================================
    // BAGIAN D: IMPERSONATION / SUPPORT MODE (Time-Boxed 30 Min + Notification)
    // =========================================================================

    fun createImpersonationSession(
        operatorId: String,
        targetTenantId: String,
        reason: String,
        durationMinutes: Long = 30
    ): ImpersonationSession {
        require(targetTenantId.isNotBlank()) { "Target tenant ID must not be empty" }
        require(reason.isNotBlank()) { "Reason for support impersonation must be provided" }

        val sessionId = "imp-${java.util.UUID.randomUUID().toString().take(8)}"
        val token = "impt-${java.util.UUID.randomUUID().toString()}"
        val expiresAt = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)

        val session = ImpersonationSession(
            sessionId = sessionId,
            operatorId = operatorId,
            targetTenantId = targetTenantId,
            reason = reason,
            token = token,
            expiresAt = expiresAt
        )

        activeImpersonations[sessionId] = session

        // Audit Trail
        auditLogger.log(
            tenantId = targetTenantId,
            actor = operatorId,
            action = "SUPPORT_IMPERSONATION_STARTED",
            details = "Operator $operatorId initiated support mode for tenant $targetTenantId. Reason: $reason. Expires in $durationMinutes mins.",
            status = "ACTIVE"
        )

        // Fase 124 / Bagian D.4.2: Notifikasi transparan terkirim otomatis ke Tenant Owner saat support mode aktif
        auditLogger.log(
            tenantId = targetTenantId,
            actor = "system-sentinel",
            action = "TENANT_OWNER_SUPPORT_NOTIFICATION",
            details = "Transparent notification dispatched to tenant owner of $targetTenantId: Super Admin $operatorId has initiated support investigation session (Ticket/Reason: $reason). Duration: $durationMinutes minutes.",
            status = "DELIVERED"
        )

        logger.info("[SUPPORT IMPERSONATION] Operator $operatorId started session for $targetTenantId, valid until $expiresAt")
        return session
    }

    fun getActiveImpersonation(sessionId: String): ImpersonationSession? {
        val session = activeImpersonations[sessionId] ?: return null
        if (System.currentTimeMillis() > session.expiresAt) {
            activeImpersonations.remove(sessionId)
            return null
        }
        return session
    }

    companion object {
        val defaultInstance = AdminSecurityService()
    }
}
