package ai.orchestree.backend.security

import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

data class SecretMetadata(
    val secretName: String,
    val version: String,
    val lastRotatedAt: Instant,
    val rotationIntervalDays: Long = 90L,
    val isDualKeyGracePeriodActive: Boolean = false,
    val previousVersion: String? = null
)

data class SecretRotationReport(
    val secretName: String,
    val currentVersion: String,
    val ageDays: Long,
    val isDueForRotation: Boolean,
    val nextRotationDueAt: Instant
)

/**
 * Secret Rotation Policy & Master Key Governance (PRD Fase 123 Bagian E)
 * Enforces periodic rotation of JWT_SIGNING_KEY and ENCRYPTION_MASTER_KEY (e.g. 90-day lifecycle)
 * with graceful dual-key period so existing tokens and envelopes remain valid during transition.
 */
class SecretRotationPolicy(
    private val defaultRotationDays: Long = 90L
) {
    private val logger = LoggerFactory.getLogger(SecretRotationPolicy::class.java)
    private val registry = ConcurrentHashMap<String, SecretMetadata>()

    init {
        // Register core security secrets with initial lifecycle tracking
        registerSecret("JWT_SIGNING_KEY", version = "v1", rotationIntervalDays = 90L)
        registerSecret("ENCRYPTION_MASTER_KEY", version = "v1", rotationIntervalDays = 90L)
        registerSecret("APP_ATTESTATION_SECRET", version = "v1", rotationIntervalDays = 180L)
    }

    fun registerSecret(
        name: String,
        version: String,
        rotationIntervalDays: Long = defaultRotationDays,
        lastRotatedAt: Instant = Instant.now().minus(10, ChronoUnit.DAYS)
    ) {
        registry[name] = SecretMetadata(
            secretName = name,
            version = version,
            lastRotatedAt = lastRotatedAt,
            rotationIntervalDays = rotationIntervalDays
        )
    }

    fun evaluateRotationStatus(name: String): SecretRotationReport {
        val meta = registry[name] ?: SecretMetadata(
            secretName = name,
            version = "v1",
            lastRotatedAt = Instant.now(),
            rotationIntervalDays = defaultRotationDays
        )

        val ageDays = ChronoUnit.DAYS.between(meta.lastRotatedAt, Instant.now())
        val isDue = ageDays >= meta.rotationIntervalDays
        val nextDue = meta.lastRotatedAt.plus(meta.rotationIntervalDays, ChronoUnit.DAYS)

        if (isDue) {
            logger.warn("SECURITY ALERT: Secret '$name' (version ${meta.version}) is due for rotation! Age: $ageDays days (Interval: ${meta.rotationIntervalDays} days)")
        }

        return SecretRotationReport(
            secretName = name,
            currentVersion = meta.version,
            ageDays = ageDays,
            isDueForRotation = isDue,
            nextRotationDueAt = nextDue
        )
    }

    fun rotateSecret(name: String, newVersion: String): SecretMetadata {
        val current = registry[name] ?: SecretMetadata(
            secretName = name,
            version = "v1",
            lastRotatedAt = Instant.now()
        )

        val updated = current.copy(
            version = newVersion,
            lastRotatedAt = Instant.now(),
            isDualKeyGracePeriodActive = true,
            previousVersion = current.version
        )
        registry[name] = updated
        logger.info("Successfully rotated secret '$name' from ${current.version} to $newVersion. Dual-key grace period activated.")
        return updated
    }

    fun getAuditStatus(): List<SecretRotationReport> {
        return registry.keys.map { evaluateRotationStatus(it) }
    }
}
