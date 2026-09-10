package ai.orchestree.backend.database.repositories.scheduler

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.orchestration.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class DeadLetterRecord(
    val id: String,
    val jobType: String,
    val originalPayload: String, // JSON string
    val failureReason: String?,
    val failedAt: Long = System.currentTimeMillis(),
    var reprocessed: Boolean = false,
    var reprocessedAt: Long? = null,
    var reprocessResult: String? = null
)

class DeadLetterQueueRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(DeadLetterQueueRepository::class.java)
    private val inMemoryStore = ConcurrentHashMap<String, DeadLetterRecord>()

    init {
        seedSampleDlqItems()
    }

    fun seedSampleDlqItems() {
        if (inMemoryStore.isEmpty()) {
            val sampleId1 = "dlq-sample-proactive-001"
            inMemoryStore[sampleId1] = DeadLetterRecord(
                id = sampleId1,
                jobType = "PROACTIVE_DAILY",
                originalPayload = """{"tenantId":"tenant-sample-001","reportType":"EXECUTIVE_MORNING"}""",
                failureReason = "HttpConnectTimeoutException: Failed to reach external briefing channel after 3 attempts",
                failedAt = System.currentTimeMillis() - 3600_000,
                reprocessed = false
            )
            val sampleId2 = "dlq-sample-crawl-002"
            inMemoryStore[sampleId2] = DeadLetterRecord(
                id = sampleId2,
                jobType = "COMPETITOR_CRAWL",
                originalPayload = """{"tenantId":"tenant-sample-001","url":"https://competitor-retail-x.com"}""",
                failureReason = "429 Too Many Requests: Rate limit exceeded on target domain after 3 attempts",
                failedAt = System.currentTimeMillis() - 7200_000,
                reprocessed = false
            )
        }
    }

    suspend fun insert(record: DeadLetterRecord): Result<DeadLetterRecord> = withContext(Dispatchers.IO) {
        inMemoryStore[record.id] = record
        if (!supabase.isConfigured()) {
            return@withContext Result.success(record)
        }

        try {
            val payload = mapOf(
                "id" to record.id,
                "job_type" to record.jobType,
                "original_payload" to record.originalPayload,
                "failure_reason" to (record.failureReason ?: ""),
                "failed_at" to record.failedAt,
                "reprocessed" to record.reprocessed
            )
            supabase.insertRecord("dead_letter_queue", "system", payload.toJson())
            Result.success(record)
        } catch (e: Exception) {
            logger.warn("Supabase insert dead_letter_queue warning: ${e.message}")
            Result.success(record)
        }
    }

    suspend fun listAll(includeReprocessed: Boolean = true): List<DeadLetterRecord> = withContext(Dispatchers.IO) {
        val memoryList = inMemoryStore.values.filter { includeReprocessed || !it.reprocessed }
        if (!supabase.isConfigured()) {
            return@withContext memoryList.sortedByDescending { it.failedAt }
        }

        try {
            val params = if (!includeReprocessed) mapOf("reprocessed" to "eq.false") else emptyMap()
            val res = supabase.queryTableGlobal(
                tableName = "dead_letter_queue",
                select = "*",
                extraParams = params
            )
            if (res.isSuccess) {
                val parsed = parseSupabaseRecords(res.getOrThrow())
                val combined = (memoryList + parsed).distinctBy { it.id }.sortedByDescending { it.failedAt }
                return@withContext combined
            }
        } catch (e: Exception) {
            logger.warn("Querying dead_letter_queue from Supabase warning: ${e.message}")
        }
        memoryList.sortedByDescending { it.failedAt }
    }

    suspend fun getById(id: String): DeadLetterRecord? = withContext(Dispatchers.IO) {
        inMemoryStore[id] ?: run {
            if (!supabase.isConfigured()) return@run null
            try {
                val res = supabase.queryTableGlobal(
                    tableName = "dead_letter_queue",
                    select = "*",
                    extraParams = mapOf("id" to "eq.$id")
                )
                if (res.isSuccess) {
                    val parsed = parseSupabaseRecords(res.getOrThrow())
                    parsed.firstOrNull()?.also { inMemoryStore[it.id] = it }
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun markReprocessed(id: String, resultSummary: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val existing = inMemoryStore[id]
        val now = System.currentTimeMillis()
        if (existing != null) {
            existing.reprocessed = true
            existing.reprocessedAt = now
            existing.reprocessResult = resultSummary
        }

        if (!supabase.isConfigured()) {
            return@withContext Result.success(true)
        }

        try {
            val payload = mapOf(
                "reprocessed" to true,
                "reprocessed_at" to now,
                "reprocess_result" to resultSummary
            )
            supabase.updateRecord("dead_letter_queue", "system", "id=eq.$id", payload.toJson())
            Result.success(true)
        } catch (e: Exception) {
            logger.warn("Supabase update dead_letter_queue warning: ${e.message}")
            Result.success(true)
        }
    }

    fun clearInMemory() {
        inMemoryStore.clear()
    }

    private fun parseSupabaseRecords(jsonStr: String): List<DeadLetterRecord> {
        val list = mutableListOf<DeadLetterRecord>()
        try {
            val jsonArray = Json.parseToJsonElement(jsonStr).jsonArray
            for (elem in jsonArray) {
                val obj = elem.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: continue
                val jobType = obj["job_type"]?.jsonPrimitive?.content ?: "UNKNOWN"
                val origPayload = obj["original_payload"]?.toString() ?: "{}"
                val reason = obj["failure_reason"]?.jsonPrimitive?.content
                val failedAt = obj["failed_at"]?.jsonPrimitive?.content?.toLongOrNull() ?: System.currentTimeMillis()
                val reprocessed = obj["reprocessed"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                val reprocessedAt = obj["reprocessed_at"]?.jsonPrimitive?.content?.toLongOrNull()
                val reprocessResult = obj["reprocess_result"]?.jsonPrimitive?.content

                list.add(
                    DeadLetterRecord(
                        id = id,
                        jobType = jobType,
                        originalPayload = origPayload,
                        failureReason = reason,
                        failedAt = failedAt,
                        reprocessed = reprocessed,
                        reprocessedAt = reprocessedAt,
                        reprocessResult = reprocessResult
                    )
                )
            }
        } catch (e: Exception) {
            logger.warn("Error parsing dead_letter_queue JSON: ${e.message}")
        }
        return list
    }
}
