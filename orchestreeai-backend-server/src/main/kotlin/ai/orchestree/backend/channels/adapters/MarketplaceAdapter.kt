package ai.orchestree.backend.channels.adapters

import ai.orchestree.backend.channels.InboundMessage

class MarketplaceAdapter {
    fun normalize(payload: Map<String, Any>): InboundMessage {
        return InboundMessage(
            tenantId = payload["tenant_id"]?.toString() ?: "default",
            channelType = "MARKETPLACE",
            senderId = payload["buyer_id"]?.toString() ?: "unknown",
            text = payload["chat_text"]?.toString() ?: ""
        )
    }
}
