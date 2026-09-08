package ai.orchestree.backend.channels

import kotlinx.serialization.Serializable

enum class ChannelType {
    WHATSAPP,
    TELEGRAM,
    INSTAGRAM,
    MARKETPLACE,
    EMAIL,
    WEBHOOK
}

enum class ChannelOperationMode {
    AI_AUTOPILOT,
    COPILOT_SUGGESTION,
    MANUAL_HUMAN
}

@Serializable
data class InboundMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tenantId: String,
    val channelType: String,
    val senderId: String,
    val text: String,
    val channelAccountId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class OutboundMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tenantId: String,
    val channelType: String,
    val recipientId: String,
    val text: String,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ChannelAccountConfig(
    val accountId: String,
    val tenantId: String,
    val channelType: ChannelType,
    val operationMode: ChannelOperationMode = ChannelOperationMode.AI_AUTOPILOT,
    val isEnabled: Boolean = true
)
