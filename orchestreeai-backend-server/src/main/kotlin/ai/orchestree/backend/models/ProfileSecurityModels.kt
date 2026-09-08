package ai.orchestree.backend.models

import java.time.Instant

/**
 * User Profile & Account Security DTOs
 */
data class UserFullProfileDto(
    val id: String,
    val tenantId: String,
    val email: String,
    val name: String,
    val phone: String = "",
    val telegramChatId: String = "",
    val avatarUrl: String = "",
    val role: UserRole,
    val capabilities: List<String> = emptyList(),
    val departmentName: String = "",
    val themePreference: String = "DARK", // DARK, LIGHT, SYSTEM
    val languagePreference: String = "id", // id, en
    val isActive: Boolean = true,
    val lastPasswordChangedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class UpdateProfileRequest(
    val name: String,
    val phone: String = "",
    val telegramChatId: String = "",
    val themePreference: String = "DARK",
    val languagePreference: String = "id"
)

data class GenerateAvatarRequest(
    val promptDescription: String,
    val style: String = "cyberpunk_flat" // cyberpunk_flat, minimalist_vector, 3d_avatar, corporate_clean
)

data class GenerateAvatarResponse(
    val avatarUrl: String,
    val generationPrompt: String,
    val validatedQualityScore: Double,
    val passedSecurityGate: Boolean
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)

data class ForgotPasswordRequest(
    val email: String,
    val clientIp: String? = null
)

data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String,
    val isDeliveredViaEmail: Boolean = true
)

data class ResetPasswordRequest(
    val token: String,
    val newPassword: String,
    val confirmPassword: String,
    val clientIp: String? = null
)

data class ResetPasswordResponse(
    val success: Boolean,
    val message: String
)

data class ValidateResetTokenResponse(
    val isValid: Boolean,
    val emailMasked: String = "",
    val message: String
)

data class UserActiveSessionDto(
    val id: String,
    val userId: String,
    val tenantId: String,
    val deviceName: String,
    val osName: String,
    val ipAddress: String,
    val locationApprox: String,
    val isCurrentSession: Boolean,
    val createdAt: Long,
    val lastActiveAt: Long,
    val expiresAt: Long
)

data class RevokeSessionResponse(
    val success: Boolean,
    val revokedSessionId: String,
    val remainingActiveSessions: Int
)
