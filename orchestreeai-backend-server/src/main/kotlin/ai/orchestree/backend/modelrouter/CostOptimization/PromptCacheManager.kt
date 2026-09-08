package ai.orchestree.backend.modelrouter.CostOptimization

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

data class CachedPromptEntry(
    val hash: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hitCount: Int = 1
)

class PromptCacheManager(
    private val ttlMs: Long = 1000 * 60 * 60 // 1 hour
) {
    private val cache = ConcurrentHashMap<String, CachedPromptEntry>()

    fun get(prompt: String, systemInstruction: String?): String? {
        val hash = hashKey(prompt, systemInstruction)
        val entry = cache[hash] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > ttlMs) {
            cache.remove(hash)
            return null
        }
        cache[hash] = entry.copy(hitCount = entry.hitCount + 1)
        return entry.response
    }

    fun put(prompt: String, systemInstruction: String?, response: String) {
        val hash = hashKey(prompt, systemInstruction)
        cache[hash] = CachedPromptEntry(hash = hash, response = response)
    }

    private fun hashKey(prompt: String, systemInstruction: String?): String {
        val raw = "${systemInstruction ?: ""}:$prompt"
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
