package ai.orchestree.backend.channels.adapters

import ai.orchestree.backend.channels.InboundMessage

class InstagramAdapter {
    fun normalize(payload: Map<String, Any>): InboundMessage {
        return InboundMessage(
            tenantId = payload["tenant_id"]?.toString() ?: "default",
            channelType = "INSTAGRAM",
            senderId = payload["ig_user_id"]?.toString() ?: "unknown",
            text = payload["message"]?.toString() ?: ""
        )
    }
}
