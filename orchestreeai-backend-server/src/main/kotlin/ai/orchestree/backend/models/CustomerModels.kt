package ai.orchestree.backend.models

import java.time.Instant

/**
 * Unified Customer Profile & Identity Resolution Domain Models
 * PRD Addendum Section 34 & Section 50
 */

enum class CustomerChannelType(val label: String, val iconKey: String) {
    WHATSAPP("WhatsApp Business", "whatsapp"),
    TELEGRAM("Telegram Bot", "telegram"),
    INSTAGRAM("Instagram DM", "instagram"),
    TIKTOK("TikTok Direct", "tiktok"),
    WEBSITE("Website Live Chat", "web"),
    SHOPEE("Shopee Chat & Orders", "shopee"),
    TOKOPEDIA("Tokopedia Seller Chat", "tokopedia"),
    BLIBLI("Blibli Commerce Chat", "blibli"),
    EMAIL("Official Email", "email")
}

enum class CustomerFunnelStage(val label: String) {
    AWARENESS("Awareness"),
    LEAD("Lead"),
    OPPORTUNITY("Opportunity"),
    CUSTOMER("Customer (Paid)"),
    REPEAT("Repeat Buyer")
}

enum class MatchConfidence {
    EXACT,
    STRONG,
    WEAK,
    NONE
}

data class IdentitySignal(
    val channelType: CustomerChannelType,
    val externalId: String, // Phone, email, IG handle, or web visitor ID
    val phoneNumber: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val deviceFingerprint: String? = null,
    val selfDeclaredLinks: List<String> = emptyList()
)

data class MatchResult(
    val customerId: String?,
    val confidence: MatchConfidence,
    val candidateIds: List<String> = emptyList(),
    val reason: String = ""
)

data class CustomerChannelIdentityDto(
    val id: String,
    val customerId: String,
    val channelType: CustomerChannelType,
    val channelExternalIdMasked: String,
    val channelExternalIdEncrypted: String,
    val verifiedAt: Instant? = null,
    val createdAt: Instant = Instant.now()
)

data class CustomerAttributeDto(
    val id: String,
    val customerId: String,
    val attributeKey: String,
    val attributeValue: String,
    val source: String,
    val confidence: Double,
    val updatedAt: Instant = Instant.now(),
    val isDecayed: Boolean = false
)

data class CustomerSegmentDto(
    val id: String,
    val customerId: String,
    val segmentCode: String,
    val assignedAt: Instant = Instant.now(),
    val assignedBy: String = "SYSTEM"
)

data class CustomerFunnelStateDto(
    val id: String,
    val customerId: String,
    val funnelStage: CustomerFunnelStage,
    val enteredAt: Instant = Instant.now(),
    val previousStage: CustomerFunnelStage? = null
)

data class CustomerMergeCandidateDto(
    val id: String,
    val tenantId: String,
    val primaryCustomerId: String,
    val primaryCustomerName: String,
    val primaryChannel: CustomerChannelType,
    val candidateCustomerId: String,
    val candidateCustomerName: String,
    val candidateChannel: CustomerChannelType,
    val similarityScore: Double,
    val matchReason: String,
    val status: String = "PENDING",
    val createdAt: Instant = Instant.now()
)

data class CustomerMergeAuditLogDto(
    val id: String,
    val tenantId: String,
    val sourceCustomerId: String,
    val targetCustomerId: String,
    val action: String, // MERGED, UNMERGED, REJECTED
    val performedBy: String,
    val performedAt: Instant = Instant.now(),
    val detailsJson: String = "{}"
)

data class CustomerDto(
    val id: String,
    val tenantId: String,
    val displayName: String,
    val primaryChannel: CustomerChannelType,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val mergedIntoId: String? = null,
    val isActive: Boolean = true,
    val identities: List<CustomerChannelIdentityDto> = emptyList(),
    val attributes: List<CustomerAttributeDto> = emptyList(),
    val segments: List<CustomerSegmentDto> = emptyList(),
    val funnelState: CustomerFunnelStateDto = CustomerFunnelStateDto(
        id = "funnel-init",
        customerId = id,
        funnelStage = CustomerFunnelStage.AWARENESS
    )
)

data class ResolveCustomerRequest(
    val channelType: CustomerChannelType,
    val externalId: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val deviceFingerprint: String? = null,
    val selfDeclaredLinks: List<String> = emptyList(),
    val initialMessage: String? = null
)

data class ResolveCustomerResponse(
    val customer: CustomerDto,
    val matchResult: MatchResult,
    val wasMergedAutomatically: Boolean = false,
    val pendingWeakCandidates: List<CustomerMergeCandidateDto> = emptyList()
)

data class CreateCustomerRequest(
    val displayName: String,
    val primaryChannel: CustomerChannelType,
    val phoneNumber: String? = null,
    val email: String? = null,
    val initialAttributes: Map<String, String> = emptyMap(),
    val initialSegments: List<String> = emptyList()
)

data class ManualMergeRequest(
    val sourceCustomerId: String,
    val targetCustomerId: String,
    val reason: String
)

data class ManualUnmergeRequest(
    val sourceCustomerId: String,
    val targetCustomerId: String,
    val reason: String
)
