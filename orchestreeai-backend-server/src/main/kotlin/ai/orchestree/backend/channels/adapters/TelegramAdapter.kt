package ai.orchestree.backend.channels.adapters

import ai.orchestree.backend.channels.InboundMessage

class TelegramAdapter {
    fun normalize(payload: Map<String, Any>): InboundMessage {
        return InboundMessage(
            tenantId = payload["tenant_id"]?.toString() ?: "default",
            channelType = "TELEGRAM",
            senderId = payload["chat_id"]?.toString() ?: "unknown",
            text = payload["text"]?.toString() ?: ""
        )
    }
}
