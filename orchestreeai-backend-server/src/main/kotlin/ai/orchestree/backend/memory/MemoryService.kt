package ai.orchestree.backend.memory

import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRecord
import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class MemoryDocument(
    val id: String,
    val tenantId: String,
    val sourceType: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

class MemoryService(
    val memoryRepo: MemoryDocumentRepository = MemoryDocumentRepository(),
    val consolidator: MemoryConsolidator = MemoryConsolidator(memoryRepo),
    val decayEngine: MemoryDecayEngine = MemoryDecayEngine(memoryRepo),
    val hybridSearchEngine: HybridMemorySearchEngine = HybridMemorySearchEngine(memoryRepo)
) {
    private val memoryStore = ConcurrentHashMap<String, MutableList<MemoryDocument>>()

    fun upsertDocument(doc: MemoryDocument): String {
        val tenantList = memoryStore.getOrPut(doc.tenantId) { mutableListOf() }
        tenantList.removeIf { it.id == doc.id }
        tenantList.add(doc)

        runBlocking {
            memoryRepo.upsert(
                MemoryDocumentRecord(
                    id = doc.id,
                    tenantId = doc.tenantId,
                    sourceType = doc.sourceType,
                    title = doc.content.take(50),
                    content = doc.content,
                    metadata = doc.metadata,
                    createdAt = doc.timestamp
                )
            )
        }
        return doc.id
    }

    fun queryByTenant(tenantId: String): List<MemoryDocument> {
        val inMem = memoryStore[tenantId]?.toList()
        if (!inMem.isNullOrEmpty()) return inMem

        return runBlocking {
            memoryRepo.listByTenant(tenantId).map {
                MemoryDocument(
                    id = it.id,
                    tenantId = it.tenantId,
                    sourceType = it.sourceType,
                    content = it.content,
                    metadata = it.metadata,
                    timestamp = it.createdAt
                )
            }
        }
    }
}
