package ai.orchestree.backend.memory

import java.util.concurrent.ConcurrentHashMap

class ConversationRollingMemoryEngine(
    private val maxTurns: Int = 10
) {
    private val conversations = ConcurrentHashMap<String, MutableList<Pair<String, String>>>()

    fun appendMessage(conversationId: String, role: String, content: String) {
        val list = conversations.getOrPut(conversationId) { mutableListOf() }
        list.add(role to content)
        while (list.size > maxTurns * 2) {
            list.removeAt(0)
        }
    }

    fun getRollingHistory(conversationId: String): List<Pair<String, String>> {
        return conversations[conversationId]?.toList() ?: emptyList()
    }
}
