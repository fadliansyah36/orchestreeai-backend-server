package ai.orchestree.backend.models

import java.time.Instant

enum class UserRole(val label: String, val scope: String) {
    SUPER_ADMIN("Super Admin", "Global Platform Control"),
    TENANT_OWNER("Tenant Owner", "Company & Billing Owner"),
    TENANT_ADMIN("Tenant Admin / HR", "Staff, Dept & Operations"),
    DEPT_MANAGER("Dept Manager", "Department Tasks & Approvals"),
    STAFF_HUMAN("Staff Human", "Operational Worker"),
    AI_AGENT("AI Agent", "Autonomous Digital Worker")
}

data class UserPrincipal(
    val userId: String,
    val tenantId: String,
    val email: String,
    val role: UserRole,
    val permissions: Set<String> = emptySet(),
    val isSuperAdmin: Boolean = (role == UserRole.SUPER_ADMIN)
)

data class RlsContext(
    val tenantId: String?,
    val isSuperAdmin: Boolean = false
)

data class LoginRequest(
    val email: String,
    val password: String,
    val mfaCode: String? = null
)

data class RegisterRequest(
    val tenantId: String,
    val email: String,
    val password: String,
    val name: String,
    val departmentId: String? = null
)

data class TenantOnboardRequest(
    val companyName: String,
    val domain: String,
    val adminEmail: String,
    val adminPassword: String,
    val adminName: String,
    val selectedPlanId: String
)

data class TokenRefreshRequest(
    val refreshToken: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long = 900, // 15 minutes
    val tokenType: String = "Bearer",
    val user: UserProfileDto,
    val requiresMfa: Boolean = false
)

data class UserProfileDto(
    val id: String,
    val tenantId: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val capabilities: List<String>,
    val departmentName: String = ""
)

data class SubscriptionPlanDto(
    val id: String,
    val name: String,
    val tier: String,
    val priceIdrMonthly: Double,
    val priceIdrYearly: Double,
    val maxAgents: Int,
    val maxHumanSeats: Int,
    val monthlyLlmTokenLimit: Long,
    val features: List<String>,
    val isActive: Boolean = true
)

data class RbacMatrixDto(
    val roles: List<UserRole>,
    val permissions: List<PermissionDto>,
    val roleCapabilityMap: Map<String, List<String>>
)

data class PermissionDto(
    val id: String,
    val capabilityString: String,
    val category: String,
    val description: String
)

data class UpdateRoleCapabilitiesRequest(
    val role: UserRole,
    val capabilityStrings: List<String>
)

data class MfaSetupResponse(
    val secretKey: String,
    val qrCodeUri: String,
    val backupCodes: List<String>
)

data class MfaVerifyRequest(
    val totpCode: String
)
