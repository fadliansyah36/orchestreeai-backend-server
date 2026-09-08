package ai.orchestree.backend.models

import kotlinx.serialization.Serializable

@Serializable
enum class PresenceRequirementState {
    NOT_REQUIRED,
    REQUIRED_LOGIN_CHECKIN,
    REQUIRED_CHECKOUT,
    ALREADY_CHECKED_IN_TODAY
}

@Serializable
enum class LoginRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

@Serializable
data class UserPresenceEnrollment(
    val id: String,
    val userId: String,
    val isEnabled: Boolean = false,
    val enrolledMethods: List<String> = emptyList(), // "FACE", "FINGERPRINT"
    val faceEmbeddingRef: String? = null,            // encrypted embedding string
    val fingerprintRegisteredDeviceIds: List<String> = emptyList(),
    val enrolledAt: Long? = null
)

@Serializable
data class PresenceCheckLog(
    val id: String,
    val userId: String,
    val checkType: String,                           // "CHECK_IN", "CHECK_OUT", "LOGIN"
    val methodUsed: String? = null,                  // "FACE", "FINGERPRINT"
    val verificationResult: String,                  // "SUCCESS", "FAILED_SIMILARITY_BELOW_THRESHOLD", "FAILED_DEVICE_NOT_REGISTERED", "FAILED_BIOMETRIC_UNSUCCESSFUL"
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val locationApprox: String? = null,
    val checkedAt: Long = System.currentTimeMillis()
)

@Serializable
data class PresenceEnrollRequest(
    val userId: String,
    val method: String? = null,                      // "FACE", "FINGERPRINT"
    val faceEmbedding: List<Float>? = null,
    val deviceId: String? = null,
    val isEnabled: Boolean = true
)

@Serializable
data class PresenceEnrollResponse(
    val success: Boolean,
    val message: String,
    val userId: String,
    val enrolledMethods: List<String>,
    val isEnabled: Boolean,
    val enrolledAt: Long
)

@Serializable
data class PresenceRequirementCheckResponse(
    val userId: String,
    val deviceId: String,
    val ip: String,
    val state: PresenceRequirementState,
    val message: String,
    val loginRiskLevel: LoginRiskLevel,
    val checkedInToday: Boolean,
    val checkedOutToday: Boolean,
    val checkoutTime: String
)

@Serializable
data class PresenceVerifyRequest(
    val userId: String,
    val checkType: String = "CHECK_IN",              // "CHECK_IN", "CHECK_OUT", "LOGIN"
    val methodUsed: String,                          // "FACE", "FINGERPRINT"
    val biometricSuccess: Boolean? = null,           // for fingerprint from BiometricPrompt
    val faceEmbedding: List<Float>? = null,          // on-device generated face embedding
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val locationApprox: String? = null
)

@Serializable
data class PresenceVerifyResponse(
    val success: Boolean,
    val verificationResult: String,
    val message: String,
    val similarityScore: Float? = null,
    val logId: String,
    val checkedAt: Long
)

@Serializable
data class PresenceMethodBreakdown(
    val face: Int = 0,
    val fingerprint: Int = 0,
    val passwordFallback: Int = 0
)

@Serializable
data class PresenceSecurityAuditSummary(
    val totalEnrolledUsers: Int,
    val totalVerificationChecks: Int,
    val totalSuccessfulChecks: Int,
    val totalFailedChecks: Int,
    val consecutiveFailures: Int,
    val potentialUnauthorizedAttempts: Int,
    val methodBreakdown: PresenceMethodBreakdown,
    val lastIncidentTimestamp: Long? = null,
    val lastSuccessfulCheckTimestamp: Long? = null,
    val securityRiskLevel: String = "NORMAL"
)

data class PresenceSecurityAuditStats(
    val totalChecks: Int,
    val totalSuccess: Int,
    val totalFailed: Int,
    val consecutiveFailures: Int,
    val potentialUnauthorizedAttempts: Int,
    val faceChecks: Int,
    val fingerprintChecks: Int,
    val passwordFallbackChecks: Int,
    val lastIncidentTimestamp: Long?,
    val lastSuccessfulCheckTimestamp: Long?,
    val securityRiskLevel: String
)
