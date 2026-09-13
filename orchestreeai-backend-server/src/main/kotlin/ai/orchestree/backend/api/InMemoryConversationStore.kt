package ai.orchestree.backend.api

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class StoredMessage(
    val id: String,
    val conversationId: String,
    val tenantId: String,
    val role: String, // "user" or "assistant"
    val senderName: String,
    val text: String,
    val intent: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class StoredConversation(
    val id: String,
    val tenantId: String,
    val senderId: String,
    val senderName: String,
    val lastSnippet: String,
    val intent: String?,
    val updatedAt: Long = System.currentTimeMillis()
)

object InMemoryConversationStore {
    private val conversations = ConcurrentHashMap<String, StoredConversation>()
    private val messages = ConcurrentHashMap<String, CopyOnWriteArrayList<StoredMessage>>()

    fun recordExchange(
        convId: String,
        tenantId: String,
        senderId: String,
        senderName: String,
        userMessage: String,
        aiReply: String,
        intent: String? = null
    ) {
        conversations[convId] = StoredConversation(
            id = convId,
            tenantId = tenantId,
            senderId = senderId,
            senderName = senderName,
            lastSnippet = aiReply.take(120),
            intent = intent,
            updatedAt = System.currentTimeMillis()
        )

        val list = messages.computeIfAbsent(convId) { CopyOnWriteArrayList() }
        list.add(
            StoredMessage(
                id = "msg-u-${System.currentTimeMillis()}",
                conversationId = convId,
                tenantId = tenantId,
                role = "user",
                senderName = senderName,
                text = userMessage,
                intent = intent
            )
        )
        list.add(
            StoredMessage(
                id = "msg-a-${System.currentTimeMillis()}",
                conversationId = convId,
                tenantId = tenantId,
                role = "assistant",
                senderName = "AI Sales Assistant",
                text = aiReply,
                intent = intent
            )
        )
    }

    fun getHistory(convId: String): List<StoredMessage> {
        return messages[convId]?.toList() ?: emptyList()
    }

    fun listConversations(tenantId: String): List<StoredConversation> {
        return conversations.values.filter { it.tenantId == tenantId || tenantId == "all" }
            .sortedByDescending { it.updatedAt }
    }
}
