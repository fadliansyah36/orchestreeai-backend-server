package ai.orchestree.backend.channels.adapters

import ai.orchestree.backend.channels.InboundMessage

class WhatsAppAdapter {
    fun normalize(payload: Map<String, Any>): InboundMessage {
        return InboundMessage(
            tenantId = payload["tenant_id"]?.toString() ?: "default",
            channelType = "WHATSAPP",
            senderId = payload["from"]?.toString() ?: "unknown",
            text = payload["body"]?.toString() ?: ""
        )
    }
}
