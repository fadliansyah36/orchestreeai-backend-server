package ai.orchestree.backend.prospect

import kotlinx.serialization.Serializable

@Serializable
data class ProspectRegistrationRequest(
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val whatsappNumber: String? = null,
    val address: String? = null,
    val companyName: String,
    val jobTitle: String,
    val industryCategoryId: String? = null,
    val companySizeRange: String? = null,
    val interestOption: String, // 'schedule_meeting_presentation' or 'direct_trial_or_subscription'
    val interestedPlanId: String? = null,
    val captchaToken: String? = null
)

@Serializable
data class ProspectRegistrationDto(
    val id: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val whatsappNumber: String? = null,
    val address: String? = null,
    val companyName: String,
    val jobTitle: String,
    val industryCategoryId: String? = null,
    val industryName: String? = null,
    val companySizeRange: String? = null,
    val interestOption: String,
    val interestedPlanId: String? = null,
    val interestedPlanName: String? = null,
    val trialSelectionStatus: String,
    val meetingStatus: String,
    val meetingScheduledAt: String? = null,
    val adminNotes: String? = null,
    val contactedByAdminId: String? = null,
    val contactedAt: String? = null,
    val activatedTenantId: String? = null,
    val ipAddress: String? = null,
    val submittedAt: String,
    val updatedAt: String? = null
)

@Serializable
data class SelectTrialRequest(
    val status: String = "selected_for_trial", // 'selected_for_trial', 'not_selected', 'rejected'
    val adminNotes: String? = null
)

@Serializable
data class ScheduleMeetingRequest(
    val meetingScheduledAt: String, // ISO timestamp or formatted string
    val meetingStatus: String = "scheduled",
    val adminNotes: String? = null
)

@Serializable
data class ProspectRegistrationResponse(
    val success: Boolean,
    val data: ProspectRegistrationDto,
    val confirmationMessage: String
)

@Serializable
data class ActivateTrialResponse(
    val success: Boolean,
    val prospectId: String,
    val tenantId: String,
    val companyName: String,
    val adminEmail: String,
    val planCode: String = "trial",
    val trialExpiresAt: String,
    val initialCredits: Long = 1000,
    val message: String
)

@Serializable
data class ProspectAnalyticsResponse(
    val totalRegistered: Int,
    val selectedForTrialCount: Int,
    val maxTrialQuota: Int = 36,
    val activatedTrialCount: Int,
    val scheduledMeetingCount: Int,
    val breakdownByInterestOption: Map<String, Int>,
    val breakdownByPlan: Map<String, Int>,
    val breakdownByIndustry: Map<String, Int>,
    val breakdownByCompanySize: Map<String, Int>
)
