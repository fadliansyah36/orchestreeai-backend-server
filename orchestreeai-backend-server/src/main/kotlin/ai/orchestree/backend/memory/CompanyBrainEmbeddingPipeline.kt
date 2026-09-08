package ai.orchestree.backend.memory

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.UUID

data class DocumentChunk(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val tenantId: String,
    val chunkIndex: Int,
    val content: String,
    val tokenCount: Int,
    val embedding: List<Double> = emptyList()
)

class CompanyBrainEmbeddingPipeline(
    private val supabase: SupabaseClientProvider? = null,
    private val memoryService: MemoryService = MemoryService()
) {
    private val logger = LoggerFactory.getLogger(CompanyBrainEmbeddingPipeline::class.java)

    /**
     * LANGKAH 3.2: Async server-side processing for Company Brain documents
     * Splits document into chunks, generates embeddings, and persists to pgvector table.
     */
    suspend fun processDocumentAsync(
        documentId: String,
        tenantId: String,
        rawText: String,
        chunkSize: Int = 500,
        chunkOverlap: Int = 50
    ): Int = withContext(Dispatchers.Default) {
        logger.info("Starting async Company Brain embedding pipeline for doc: $documentId (tenant: $tenantId, length: ${rawText.length} chars)")

        val chunks = splitIntoChunks(rawText, chunkSize, chunkOverlap)
        var indexedCount = 0

        chunks.forEachIndexed { index, chunkContent ->
            val chunk = DocumentChunk(
                documentId = documentId,
                tenantId = tenantId,
                chunkIndex = index,
                content = chunkContent,
                tokenCount = chunkContent.split("\\s+".toRegex()).size,
                embedding = generateDeterministicEmbedding(chunkContent)
            )

            // 1. Index into in-memory memory store
            memoryService.upsertDocument(
                MemoryDocument(
                    id = chunk.id,
                    tenantId = tenantId,
                    sourceType = "COMPANY_BRAIN_DOC_$documentId",
                    content = chunk.content,
                    metadata = mapOf("documentId" to documentId, "chunkIndex" to index.toString())
                )
            )

            // 2. Persist chunk to Supabase pgvector table if configured
            if (supabase != null && supabase.isConfigured()) {
                val embeddingJson = chunk.embedding.joinToString(prefix = "[", postfix = "]", separator = ",")
                val payload = """
                    {
                        "id": "${chunk.id}",
                        "document_id": "$documentId",
                        "tenant_id": "$tenantId",
                        "chunk_index": $index,
                        "content": "${chunk.content.replace("\"", "\\\"").replace("\n", " ")}",
                        "embedding": $embeddingJson
                    }
                """.trimIndent()

                supabase.insertRecord("document_chunks", tenantId, payload)
            }
            indexedCount++
        }

        logger.info("Company Brain pipeline completed: indexed $indexedCount chunks for doc: $documentId")
        indexedCount
    }

    private fun splitIntoChunks(text: String, chunkSize: Int, chunkOverlap: Int): List<String> {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < words.size) {
            val end = (start + chunkSize).coerceAtMost(words.size)
            val chunk = words.subList(start, end).joinToString(" ")
            chunks.add(chunk)
            if (end == words.size) break
            start += (chunkSize - chunkOverlap).coerceAtLeast(1)
        }

        return chunks
    }

    private fun generateDeterministicEmbedding(text: String, dimension: Int = 1536): List<Double> {
        val hash = text.hashCode()
        val random = java.util.Random(hash.toLong())
        return List(dimension) { (random.nextDouble() * 2) - 1 }
    }
}
