package ai.orchestree.backend.database.repositories.selection

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class SelectionRequestRecord(
    val id: String = UUID.randomUUID().toString(),
    val tenant_id: String,
    val requested_by_user_id: String = "",
    val user_id: String = "",
    val domain_category: String? = null,
    val prompt_text: String,
    val source_type: String = "file_upload", // 'file_upload','prompt_only','whatsapp','telegram','api_database','integration_workflow'
    val assigned_ai_job_title_id: String? = null,
    val calibration_settings_id: String? = null,
    val workflow_execution_id: String? = null,
    val status: String = "processing", // 'processing','awaiting_review','completed','failed','cancelled'
    val created_at: String? = null
) {
    val effectiveUserId: String get() = requested_by_user_id.ifBlank { user_id }
}

@Serializable
data class SelectionSourceDocumentRecord(
    val id: String = UUID.randomUUID().toString(),
    val selection_request_id: String,
    val file_name: String?,
    val file_type: String?, // 'xlsx'/'csv'/'pdf'/'docx'/'image'
    val object_storage_url: String,
    val extracted_row_count: Int? = 0,
    val extraction_status: String = "pending", // pending/processing/ready/failed
    val detected_schema: JsonElement? = null,
    val created_at: String? = null
)

@Serializable
data class SelectionCriterionRecord(
    val id: String = UUID.randomUUID().toString(),
    val selection_request_id: String,
    val criterion_name: String,
    val criterion_source: String, // 'user_prompt','company_brain','database_pattern','ai_generated','user_calibration'
    val weight_percentage: Double? = 0.0,
    val is_user_calibrated: Boolean = false
)

@Serializable
data class SelectionCalibrationSettingRecord(
    val id: String = UUID.randomUUID().toString(),
    val tenant_id: String,
    val created_by_user_id: String,
    val calibration_name: String? = null,
    val is_saved_as_preset: Boolean = false,
    val created_at: String? = null
)

@Serializable
data class SelectionCalibrationItemRecord(
    val id: String = UUID.randomUUID().toString(),
    val calibration_settings_id: String,
    val field_type_name: String,
    val percentage: Double,
    val order_index: Int? = 0
)

@Serializable
data class SelectionResultRecord(
    val id: String = UUID.randomUUID().toString(),
    val selection_request_id: String,
    val row_reference: JsonElement? = null,
    val total_score: Double,
    val rank_position: Int,
    val priority_level: String = "medium", // 'high'/'medium'/'low'
    val recommendation_classification: String = "review", // 'selected'/'review'/'rejected'
    val risk_score: Double = 0.0,
    val confidence_score: Double = 0.9,
    val score_breakdown: JsonElement? = null,
    val ai_insight_text: String? = null,
    val human_review_status: String = "pending_review",
    val human_reviewer_id: String? = null,
    val human_review_notes: String? = null,
    val human_reviewed_at: String? = null
)

@Serializable
data class SelectionAnalyticsSummaryRecord(
    val id: String = UUID.randomUUID().toString(),
    val selection_request_id: String,
    val metric_type: String, // 'kpi'/'distribution'/'trend'/'correlation'/'anomaly'
    val metric_data: JsonElement,
    val suggested_chart_type: String? = "bar",
    val chart_data_payload: JsonElement? = null
)

@Serializable
data class SelectionAuditLogRecord(
    val id: String = UUID.randomUUID().toString(),
    val selection_request_id: String,
    val action_type: String,
    val actor_id: String? = null,
    val actor_type: String? = "system",
    val detail: JsonElement? = null,
    val created_at: String? = null
)

class SelectionRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val modelRouter: ModelRouter = ModelRouter()
) {
    private val logger = LoggerFactory.getLogger(SelectionRepository::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        // Synchronized in-memory cache for immediate lookups across repository instances
        val requestsCache = java.util.concurrent.ConcurrentHashMap<String, SelectionRequestRecord>()
        val sourceDocsCache = java.util.concurrent.ConcurrentHashMap<String, SelectionSourceDocumentRecord>()
        val resultsCache = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<SelectionResultRecord>>()
        val criteriaCache = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<SelectionCriterionRecord>>()
        val analyticsCache = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<SelectionAnalyticsSummaryRecord>>()
        val calibrationSettingsCache = java.util.concurrent.ConcurrentHashMap<String, SelectionCalibrationSettingRecord>()
        val calibrationItemsCache = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<SelectionCalibrationItemRecord>>()
    }

    suspend fun createSelectionRequest(record: SelectionRequestRecord): Result<SelectionRequestRecord> = withContext(Dispatchers.IO) {
        try {
            requestsCache[record.id] = record
            val payload = buildJsonObject {
                put("id", record.id)
                put("tenant_id", record.tenant_id)
                put("requested_by_user_id", record.effectiveUserId)
                record.domain_category?.let { put("domain_category", it) }
                put("prompt_text", record.prompt_text)
                put("source_type", record.source_type)
                record.assigned_ai_job_title_id?.let { put("assigned_ai_job_title_id", it) }
                record.calibration_settings_id?.let { put("calibration_settings_id", it) }
                record.workflow_execution_id?.let { put("workflow_execution_id", it) }
                put("status", record.status)
            }
            val res = supabase.insertRecord("selection_requests", record.tenant_id, payload.toString())
            if (res.isSuccess) {
                Result.success(record)
            } else {
                logger.error("Failed to insert selection_requests: ${res.exceptionOrNull()?.message}")
                Result.failure(res.exceptionOrNull() ?: RuntimeException("Insert selection_requests failed"))
            }
        } catch (e: Exception) {
            logger.error("Error creating selection request: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateSelectionRequestStatus(
        requestId: String,
        status: String,
        tenantId: String,
        domainCategory: String? = null,
        assignedJobTitleId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = requestsCache[requestId]
            if (existing != null) {
                requestsCache[requestId] = existing.copy(
                    status = status,
                    domain_category = domainCategory ?: existing.domain_category,
                    assigned_ai_job_title_id = assignedJobTitleId ?: existing.assigned_ai_job_title_id
                )
            }
            val payload = buildJsonObject {
                put("status", status)
                domainCategory?.let { put("domain_category", it) }
                assignedJobTitleId?.let { put("assigned_ai_job_title_id", it) }
            }
            supabase.updateRecord("selection_requests", tenantId, "id=eq.$requestId", payload.toString())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSourceDocument(doc: SelectionSourceDocumentRecord, tenantId: String): Result<SelectionSourceDocumentRecord> = withContext(Dispatchers.IO) {
        try {
            sourceDocsCache[doc.id] = doc
            val payload = buildJsonObject {
                put("id", doc.id)
                put("selection_request_id", doc.selection_request_id)
                doc.file_name?.let { put("file_name", it) }
                doc.file_type?.let { put("file_type", it) }
                put("object_storage_url", doc.object_storage_url)
                put("extracted_row_count", doc.extracted_row_count ?: 0)
                put("extraction_status", doc.extraction_status)
                doc.detected_schema?.let { put("detected_schema", it) }
            }
            val res = supabase.insertRecord("selection_source_documents", tenantId, payload.toString())
            if (res.isSuccess) {
                Result.success(doc)
            } else {
                Result.failure(res.exceptionOrNull() ?: RuntimeException("Insert selection_source_documents failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSourceDocument(docId: String, tenantId: String? = null): SelectionSourceDocumentRecord? = withContext(Dispatchers.IO) {
        val cached = sourceDocsCache[docId]
        if (cached != null) return@withContext cached

        try {
            val tId = tenantId ?: "tenant-enterprise-001"
            val queryRes = supabase.queryTable("selection_source_documents", tId, "id=eq.$docId")
            if (queryRes.isSuccess) {
                val array = json.parseToJsonElement(queryRes.getOrThrow()).jsonArray
                if (array.isNotEmpty()) {
                    val obj = array[0].jsonObject
                    val rec = SelectionSourceDocumentRecord(
                        id = obj["id"]?.jsonPrimitive?.content ?: docId,
                        selection_request_id = obj["selection_request_id"]?.jsonPrimitive?.content ?: "",
                        file_name = obj["file_name"]?.jsonPrimitive?.content,
                        file_type = obj["file_type"]?.jsonPrimitive?.content,
                        object_storage_url = obj["object_storage_url"]?.jsonPrimitive?.content ?: "",
                        extracted_row_count = obj["extracted_row_count"]?.jsonPrimitive?.intOrNull ?: 0,
                        extraction_status = obj["extraction_status"]?.jsonPrimitive?.content ?: "pending",
                        detected_schema = obj["detected_schema"],
                        created_at = obj["created_at"]?.jsonPrimitive?.content
                    )
                    sourceDocsCache[docId] = rec
                    return@withContext rec
                }
            }
            null
        } catch (e: Exception) {
            logger.error("Error getting source document: ${e.message}", e)
            null
        }
    }

    suspend fun updateSourceDocumentStatus(
        docId: String,
        status: String,
        extractedRowCount: Int,
        detectedSchema: JsonElement?,
        tenantId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cached = sourceDocsCache[docId]
            if (cached != null) {
                sourceDocsCache[docId] = cached.copy(
                    extraction_status = status,
                    extracted_row_count = extractedRowCount,
                    detected_schema = detectedSchema ?: cached.detected_schema
                )
            }
            val payload = buildJsonObject {
                put("extraction_status", status)
                put("extracted_row_count", extractedRowCount)
                detectedSchema?.let { put("detected_schema", it) }
            }
            supabase.updateRecord("selection_source_documents", tenantId, "id=eq.$docId", payload.toString())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExtraction(
        docId: String,
        extractedRowCount: Int,
        status: String,
        detectedSchema: JsonElement? = null,
        tenantId: String = "tenant-enterprise-001"
    ): Result<Unit> = updateSourceDocumentStatus(docId, status, extractedRowCount, detectedSchema, tenantId)

    suspend fun insertCriteria(criteria: List<SelectionCriterionRecord>, tenantId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (c in criteria) {
                criteriaCache.computeIfAbsent(c.selection_request_id) { java.util.concurrent.CopyOnWriteArrayList() }.add(c)
                val payload = buildJsonObject {
                    put("id", c.id)
                    put("selection_request_id", c.selection_request_id)
                    put("criterion_name", c.criterion_name)
                    put("criterion_source", c.criterion_source)
                    c.weight_percentage?.let { put("weight_percentage", it) }
                    put("is_user_calibrated", c.is_user_calibrated)
                }
                supabase.insertRecord("selection_criteria", tenantId, payload.toString())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertResults(results: List<SelectionResultRecord>, tenantId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (r in results) {
                resultsCache.computeIfAbsent(r.selection_request_id) { java.util.concurrent.CopyOnWriteArrayList() }.add(r)
                val payload = buildJsonObject {
                    put("id", r.id)
                    put("selection_request_id", r.selection_request_id)
                    r.row_reference?.let { put("row_reference", it) }
                    put("total_score", r.total_score)
                    put("rank_position", r.rank_position)
                    put("priority_level", r.priority_level)
                    put("recommendation_classification", r.recommendation_classification)
                    put("risk_score", r.risk_score)
                    put("confidence_score", r.confidence_score)
                    r.score_breakdown?.let { put("score_breakdown", it) }
                    r.ai_insight_text?.let { put("ai_insight_text", it) }
                    put("human_review_status", r.human_review_status)
                }
                supabase.insertRecord("selection_results", tenantId, payload.toString())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertAnalytics(analytics: List<SelectionAnalyticsSummaryRecord>, tenantId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (a in analytics) {
                analyticsCache.computeIfAbsent(a.selection_request_id) { java.util.concurrent.CopyOnWriteArrayList() }.add(a)
                val payload = buildJsonObject {
                    put("id", a.id)
                    put("selection_request_id", a.selection_request_id)
                    put("metric_type", a.metric_type)
                    put("metric_data", a.metric_data)
                    a.suggested_chart_type?.let { put("suggested_chart_type", it) }
                    a.chart_data_payload?.let { put("chart_data_payload", it) }
                }
                supabase.insertRecord("selection_analytics_summary", tenantId, payload.toString())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertAuditLog(log: SelectionAuditLogRecord, tenantId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject {
                put("id", log.id)
                put("selection_request_id", log.selection_request_id)
                put("action_type", log.action_type)
                log.actor_id?.let { put("actor_id", it) }
                log.actor_type?.let { put("actor_type", it) }
                log.detail?.let { put("detail", it) }
            }
            supabase.insertRecord("selection_audit_log", tenantId, payload.toString())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSelectionRequestById(requestId: String, tenantId: String): SelectionRequestRecord? = withContext(Dispatchers.IO) {
        val cached = requestsCache[requestId]
        if (cached != null) return@withContext cached

        try {
            val res = supabase.queryTable("selection_requests", tenantId, "id=eq.$requestId")
            if (res.isSuccess) {
                val array = json.parseToJsonElement(res.getOrThrow()).jsonArray
                if (array.isNotEmpty()) {
                    val obj = array[0].jsonObject
                    val rec = SelectionRequestRecord(
                        id = obj["id"]?.jsonPrimitive?.content ?: requestId,
                        tenant_id = obj["tenant_id"]?.jsonPrimitive?.content ?: tenantId,
                        requested_by_user_id = obj["requested_by_user_id"]?.jsonPrimitive?.content ?: "",
                        domain_category = obj["domain_category"]?.jsonPrimitive?.content,
                        prompt_text = obj["prompt_text"]?.jsonPrimitive?.content ?: "",
                        source_type = obj["source_type"]?.jsonPrimitive?.content ?: "file_upload",
                        assigned_ai_job_title_id = obj["assigned_ai_job_title_id"]?.jsonPrimitive?.content,
                        calibration_settings_id = obj["calibration_settings_id"]?.jsonPrimitive?.content,
                        workflow_execution_id = obj["workflow_execution_id"]?.jsonPrimitive?.content,
                        status = obj["status"]?.jsonPrimitive?.content ?: "processing",
                        created_at = obj["created_at"]?.jsonPrimitive?.content
                    )
                    requestsCache[requestId] = rec
                    return@withContext rec
                }
            }
            null
        } catch (e: Exception) {
            logger.error("Error reading selection request $requestId: ${e.message}", e)
            null
        }
    }

    suspend fun getSelectionResults(requestId: String, tenantId: String): List<SelectionResultRecord> = withContext(Dispatchers.IO) {
        try {
            val res = supabase.queryTable("selection_results", tenantId, "selection_request_id=eq.$requestId&order=rank_position.asc")
            if (res.isSuccess) {
                val array = json.parseToJsonElement(res.getOrThrow()).jsonArray
                if (array.isNotEmpty()) {
                    return@withContext array.map { el ->
                        val obj = el.jsonObject
                        SelectionResultRecord(
                            id = obj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
                            selection_request_id = obj["selection_request_id"]?.jsonPrimitive?.content ?: requestId,
                            row_reference = obj["row_reference"],
                            total_score = obj["total_score"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                            rank_position = obj["rank_position"]?.jsonPrimitive?.intOrNull ?: 0,
                            priority_level = obj["priority_level"]?.jsonPrimitive?.content ?: "medium",
                            recommendation_classification = obj["recommendation_classification"]?.jsonPrimitive?.content ?: "review",
                            risk_score = obj["risk_score"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                            confidence_score = obj["confidence_score"]?.jsonPrimitive?.doubleOrNull ?: 0.9,
                            score_breakdown = obj["score_breakdown"],
                            ai_insight_text = obj["ai_insight_text"]?.jsonPrimitive?.content,
                            human_review_status = obj["human_review_status"]?.jsonPrimitive?.content ?: "pending_review",
                            human_reviewer_id = obj["human_reviewer_id"]?.jsonPrimitive?.content,
                            human_review_notes = obj["human_review_notes"]?.jsonPrimitive?.content,
                            human_reviewed_at = obj["human_reviewed_at"]?.jsonPrimitive?.content
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching selection results: ${e.message}", e)
        }
        val cached = resultsCache[requestId]
        if (!cached.isNullOrEmpty()) {
            return@withContext cached.sortedBy { it.rank_position }
        }
        emptyList()
    }

    suspend fun getSelectionCriteria(requestId: String, tenantId: String): List<SelectionCriterionRecord> = withContext(Dispatchers.IO) {
        try {
            val res = supabase.queryTable("selection_criteria", tenantId, "selection_request_id=eq.$requestId")
            if (res.isSuccess) {
                val array = json.parseToJsonElement(res.getOrThrow()).jsonArray
                if (array.isNotEmpty()) {
                    return@withContext array.map { el ->
                        val obj = el.jsonObject
                        SelectionCriterionRecord(
                            id = obj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
                            selection_request_id = obj["selection_request_id"]?.jsonPrimitive?.content ?: requestId,
                            criterion_name = obj["criterion_name"]?.jsonPrimitive?.content ?: "",
                            criterion_source = obj["criterion_source"]?.jsonPrimitive?.content ?: "ai_generated",
                            weight_percentage = obj["weight_percentage"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                            is_user_calibrated = obj["is_user_calibrated"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching selection criteria: ${e.message}", e)
        }
        val cached = criteriaCache[requestId]
        if (!cached.isNullOrEmpty()) {
            return@withContext cached
        }
        emptyList()
    }

    suspend fun getSelectionAnalytics(requestId: String, tenantId: String): List<SelectionAnalyticsSummaryRecord> = withContext(Dispatchers.IO) {
        try {
            val res = supabase.queryTable("selection_analytics_summary", tenantId, "selection_request_id=eq.$requestId")
            if (res.isSuccess) {
                val array = json.parseToJsonElement(res.getOrThrow()).jsonArray
                if (array.isNotEmpty()) {
                    return@withContext array.map { el ->
                        val obj = el.jsonObject
                        SelectionAnalyticsSummaryRecord(
                            id = obj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
                            selection_request_id = obj["selection_request_id"]?.jsonPrimitive?.content ?: requestId,
                            metric_type = obj["metric_type"]?.jsonPrimitive?.content ?: "kpi",
                            metric_data = obj["metric_data"] ?: buildJsonObject {},
                            suggested_chart_type = obj["suggested_chart_type"]?.jsonPrimitive?.content ?: "bar",
                            chart_data_payload = obj["chart_data_payload"]
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching selection analytics: ${e.message}", e)
        }
        val cached = analyticsCache[requestId]
        if (!cached.isNullOrEmpty()) {
            return@withContext cached
        }
        emptyList()
    }

    suspend fun listSelectionRequests(tenantId: String): List<SelectionRequestRecord> = withContext(Dispatchers.IO) {
        try {
            val res = supabase.queryTable("selection_requests", tenantId, "tenant_id=eq.$tenantId&order=created_at.desc")
            if (res.isSuccess) {
                val array = json.parseToJsonElement(res.getOrThrow()).jsonArray
                if (array.isNotEmpty()) {
                    return@withContext array.map { el ->
                        val obj = el.jsonObject
                        SelectionRequestRecord(
                            id = obj["id"]?.jsonPrimitive?.content ?: "",
                            tenant_id = obj["tenant_id"]?.jsonPrimitive?.content ?: tenantId,
                            requested_by_user_id = obj["requested_by_user_id"]?.jsonPrimitive?.content ?: "",
                            domain_category = obj["domain_category"]?.jsonPrimitive?.content,
                            assigned_ai_job_title_id = obj["assigned_ai_job_title_id"]?.jsonPrimitive?.content,
                            prompt_text = obj["prompt_text"]?.jsonPrimitive?.content ?: "",
                            source_type = obj["source_type"]?.jsonPrimitive?.content ?: "file_upload",
                            status = obj["status"]?.jsonPrimitive?.content ?: "pending",
                            created_at = obj["created_at"]?.jsonPrimitive?.content
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error listing selection requests: ${e.message}", e)
        }
        requestsCache.values.filter { it.tenant_id == tenantId }.toList()
    }

    suspend fun createCalibrationSetting(setting: SelectionCalibrationSettingRecord): Result<SelectionCalibrationSettingRecord> = withContext(Dispatchers.IO) {
        try {
            calibrationSettingsCache[setting.id] = setting
            val payload = buildJsonObject {
                put("id", setting.id)
                put("tenant_id", setting.tenant_id)
                put("created_by_user_id", setting.created_by_user_id)
                setting.calibration_name?.let { put("calibration_name", it) }
                put("is_saved_as_preset", setting.is_saved_as_preset)
            }
            supabase.insertRecord("selection_calibration_settings", setting.tenant_id, payload.toString())
            Result.success(setting)
        } catch (e: Exception) {
            logger.error("Error creating calibration setting: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createCalibrationItem(item: SelectionCalibrationItemRecord, tenantId: String): Result<SelectionCalibrationItemRecord> = withContext(Dispatchers.IO) {
        try {
            calibrationItemsCache.computeIfAbsent(item.calibration_settings_id) { java.util.concurrent.CopyOnWriteArrayList() }.add(item)
            val payload = buildJsonObject {
                put("id", item.id)
                put("calibration_settings_id", item.calibration_settings_id)
                put("field_type_name", item.field_type_name)
                put("percentage", item.percentage)
                item.order_index?.let { put("order_index", it) }
            }
            supabase.insertRecord("selection_calibration_items", tenantId, payload.toString())
            Result.success(item)
        } catch (e: Exception) {
            logger.error("Error creating calibration item: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getCalibrationSetting(id: String, tenantId: String): SelectionCalibrationSettingRecord? = withContext(Dispatchers.IO) {
        val cached = calibrationSettingsCache[id]
        if (cached != null) return@withContext cached

        try {
            val res = supabase.queryTable("selection_calibration_settings", tenantId, "id=eq.$id")
            if (res.isSuccess) {
                val array = json.parseToJsonElement(res.getOrThrow()).jsonArray
                if (array.isNotEmpty()) {
                    val obj = array[0].jsonObject
                    val rec = SelectionCalibrationSettingRecord(
                        id = obj["id"]?.jsonPrimitive?.content ?: id,
                        tenant_id = obj["tenant_id"]?.jsonPrimitive?.content ?: tenantId,
                        created_by_user_id = obj["created_by_user_id"]?.jsonPrimitive?.content ?: "",
                        calibration_name = obj["calibration_name"]?.jsonPrimitive?.content,
                        is_saved_as_preset = obj["is_saved_as_preset"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                        created_at = obj["created_at"]?.jsonPrimitive?.content
                    )
                    calibrationSettingsCache[id] = rec
                    return@withContext rec
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching calibration setting $id: ${e.message}", e)
        }
        null
    }

    suspend fun getCalibrationItems(calibrationSettingsId: String, tenantId: String): List<SelectionCalibrationItemRecord> = withContext(Dispatchers.IO) {
        val cached = calibrationItemsCache[calibrationSettingsId]
        if (!cached.isNullOrEmpty()) return@withContext cached

        try {
            val res = supabase.queryTable("selection_calibration_items", tenantId, "calibration_settings_id=eq.$calibrationSettingsId&order=order_index.asc")
            if (res.isSuccess) {
                val array = json.parseToJsonElement(res.getOrThrow()).jsonArray
                if (array.isNotEmpty()) {
                    val list = array.map { el ->
                        val obj = el.jsonObject
                        SelectionCalibrationItemRecord(
                            id = obj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
                            calibration_settings_id = obj["calibration_settings_id"]?.jsonPrimitive?.content ?: calibrationSettingsId,
                            field_type_name = obj["field_type_name"]?.jsonPrimitive?.content ?: "",
                            percentage = obj["percentage"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                            order_index = obj["order_index"]?.jsonPrimitive?.intOrNull ?: 0
                        )
                    }
                    calibrationItemsCache[calibrationSettingsId] = java.util.concurrent.CopyOnWriteArrayList(list)
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching calibration items: ${e.message}", e)
        }
        emptyList()
    }

    suspend fun listCalibrationSettings(tenantId: String): List<SelectionCalibrationSettingRecord> = withContext(Dispatchers.IO) {
        try {
            val res = supabase.queryTable("selection_calibration_settings", tenantId, "tenant_id=eq.$tenantId&order=created_at.desc")
            if (res.isSuccess) {
                val array = json.parseToJsonElement(res.getOrThrow()).jsonArray
                if (array.isNotEmpty()) {
                    return@withContext array.map { el ->
                        val obj = el.jsonObject
                        SelectionCalibrationSettingRecord(
                            id = obj["id"]?.jsonPrimitive?.content ?: "",
                            tenant_id = obj["tenant_id"]?.jsonPrimitive?.content ?: tenantId,
                            created_by_user_id = obj["created_by_user_id"]?.jsonPrimitive?.content ?: "",
                            calibration_name = obj["calibration_name"]?.jsonPrimitive?.content,
                            is_saved_as_preset = obj["is_saved_as_preset"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                            created_at = obj["created_at"]?.jsonPrimitive?.content
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error listing calibration settings: ${e.message}", e)
        }
        calibrationSettingsCache.values.filter { it.tenant_id == tenantId }.toList()
    }

    suspend fun getResults(requestId: String, tenantId: String): List<SelectionResultRecord> = getSelectionResults(requestId, tenantId)
    suspend fun getAnalytics(requestId: String, tenantId: String): List<SelectionAnalyticsSummaryRecord> = getSelectionAnalytics(requestId, tenantId)

    suspend fun getSelectionResultById(resultId: String, tenantId: String): SelectionResultRecord? = withContext(Dispatchers.IO) {
        for ((_, list) in resultsCache) {
            val found = list.firstOrNull { it.id == resultId }
            if (found != null) return@withContext found
        }
        try {
            val res = supabase.queryTable("selection_results", tenantId, "id=eq.$resultId")
            if (res.isSuccess) {
                val array = json.parseToJsonElement(res.getOrThrow()).jsonArray
                if (array.isNotEmpty()) {
                    val obj = array[0].jsonObject
                    return@withContext SelectionResultRecord(
                        id = obj["id"]?.jsonPrimitive?.content ?: resultId,
                        selection_request_id = obj["selection_request_id"]?.jsonPrimitive?.content ?: "",
                        row_reference = obj["row_reference"],
                        total_score = obj["total_score"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        rank_position = obj["rank_position"]?.jsonPrimitive?.intOrNull ?: 0,
                        priority_level = obj["priority_level"]?.jsonPrimitive?.content ?: "medium",
                        recommendation_classification = obj["recommendation_classification"]?.jsonPrimitive?.content ?: "review",
                        risk_score = obj["risk_score"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        confidence_score = obj["confidence_score"]?.jsonPrimitive?.doubleOrNull ?: 0.9,
                        score_breakdown = obj["score_breakdown"],
                        ai_insight_text = obj["ai_insight_text"]?.jsonPrimitive?.content,
                        human_review_status = obj["human_review_status"]?.jsonPrimitive?.content ?: "pending_review",
                        human_reviewer_id = obj["human_reviewer_id"]?.jsonPrimitive?.content,
                        human_review_notes = obj["human_review_notes"]?.jsonPrimitive?.content,
                        human_reviewed_at = obj["human_reviewed_at"]?.jsonPrimitive?.content
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn("Query selection_results by id failed: ${e.message}")
        }
        null
    }

    /**
     * LANGKAH 2: Human Review & Approval Gate
     * Approve / Reject / Override Ranking / Add Notes
     * SETIAP aksi tercatat ke selection_audit_log DAN Audit Ledger global.
     */
    suspend fun updateResultReview(
        resultId: String,
        tenantId: String,
        action: String,
        reviewerId: String,
        notes: String? = null,
        overrideRankPosition: Int? = null,
        overrideClassification: String? = null
    ): Result<SelectionResultRecord> = withContext(Dispatchers.IO) {
        val existing = getSelectionResultById(resultId, tenantId)
            ?: return@withContext Result.failure(IllegalArgumentException("SelectionResult with ID $resultId not found"))

        val newReviewStatus = when (action.lowercase()) {
            "approve", "approved" -> "approved"
            "reject", "rejected" -> "rejected"
            "override_rank", "override" -> "overridden"
            "add_notes" -> existing.human_review_status
            else -> action.lowercase()
        }

        val updatedRecord = existing.copy(
            human_review_status = newReviewStatus,
            human_reviewer_id = reviewerId,
            human_review_notes = notes ?: existing.human_review_notes,
            human_reviewed_at = java.time.Instant.now().toString(),
            rank_position = overrideRankPosition ?: existing.rank_position,
            recommendation_classification = overrideClassification ?: existing.recommendation_classification
        )

        // Update in Supabase
        val updatePayload = buildJsonObject {
            put("human_review_status", updatedRecord.human_review_status)
            put("human_reviewer_id", updatedRecord.human_reviewer_id)
            put("human_review_notes", updatedRecord.human_review_notes)
            put("human_reviewed_at", updatedRecord.human_reviewed_at)
            put("rank_position", updatedRecord.rank_position)
            put("recommendation_classification", updatedRecord.recommendation_classification)
        }
        supabase.updateRecord("selection_results", tenantId, "id=eq.$resultId", updatePayload.toString())

        // Update in-memory cache
        val list = resultsCache[existing.selection_request_id]
        if (list != null) {
            val idx = list.indexOfFirst { it.id == resultId }
            if (idx >= 0) {
                list[idx] = updatedRecord
            } else {
                list.add(updatedRecord)
            }
        }

        // 1. Catat ke selection_audit_log
        val auditAction = when (action.lowercase()) {
            "approve", "approved" -> "SELECTION_RESULT_APPROVED"
            "reject", "rejected" -> "SELECTION_RESULT_REJECTED"
            "override_rank", "override" -> "SELECTION_RESULT_RANK_OVERRIDDEN"
            else -> "SELECTION_RESULT_NOTES_ADDED"
        }
        val detailObj = buildJsonObject {
            put("result_id", resultId)
            put("action", action)
            put("reviewer_id", reviewerId)
            notes?.let { put("notes", it) }
            overrideRankPosition?.let { put("override_rank", it) }
            overrideClassification?.let { put("override_classification", it) }
            put("new_status", newReviewStatus)
        }
        insertAuditLog(
            SelectionAuditLogRecord(
                selection_request_id = existing.selection_request_id,
                action_type = auditAction,
                actor_id = reviewerId,
                actor_type = "human_reviewer",
                detail = detailObj
            ),
            tenantId = tenantId
        )

        // 2. Catat ke Audit Ledger Global (PRD Master Bagian 27.1)
        val globalAudit = ai.orchestree.backend.security.AuditLogger()
        globalAudit.log(
            tenantId = tenantId,
            actor = reviewerId,
            action = "HUMAN_APPROVAL_DECISION",
            details = "SelectionResult: $resultId, decision: $newReviewStatus, notes: ${notes ?: "none"}"
        )

        Result.success(updatedRecord)
    }

    /**
     * LANGKAH 2.2: TEGASKAN: keputusan FINAL untuk hal yang membutuhkan validasi
     * TETAP DI TANGAN MANUSIA — status recommendation_classification="selected" dari AI
     * TIDAK PERNAH otomatis mengeksekusi aksi lanjutan (mis. auto-hire, auto-PO)
     * TANPA human_review_status="approved" eksplisit.
     */
    fun canExecuteDownstreamAction(result: SelectionResultRecord): Boolean {
        return result.human_review_status == "approved"
    }
}
