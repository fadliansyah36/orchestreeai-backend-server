package ai.orchestree.backend.api

import ai.orchestree.backend.billing.enforceEntitlementGate
import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.selection.SelectionCriterionRecord
import ai.orchestree.backend.database.repositories.selection.SelectionRepository
import ai.orchestree.backend.database.repositories.selection.SelectionRequestRecord
import ai.orchestree.backend.database.repositories.selection.SelectionResultRecord
import ai.orchestree.backend.database.repositories.selection.SelectionSourceDocumentRecord
import ai.orchestree.backend.generativestudio.DeterministicRenderer
import ai.orchestree.backend.intelligence.SelectionEngine
import ai.orchestree.backend.intelligence.CalibrationRequest
import ai.orchestree.backend.intelligence.CalibrationResult
import ai.orchestree.backend.intelligence.CalibrationItem
import ai.orchestree.backend.intelligence.DetectedField
import ai.orchestree.backend.intelligence.UnmatchedCalibrationCriterionException
import ai.orchestree.backend.database.repositories.selection.SelectionCalibrationSettingRecord
import ai.orchestree.backend.database.repositories.selection.SelectionCalibrationItemRecord
import ai.orchestree.backend.modelrouter.ModelRouter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.toByteArray
import ai.orchestree.backend.database.repositories.selection.AutoSelectionConfigRepository
import ai.orchestree.backend.database.repositories.selection.AutoSelectionFolderConfig
import ai.orchestree.backend.intelligence.SelectionAiJobTitleRegistry
import ai.orchestree.backend.intelligence.MasterAiJobTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class PromptOnlySelectionRequest(
    val prompt: String,
    val calibrationSettingsId: String? = null
)

@Serializable
data class ApiDatabaseSelectionRequest(
    val prompt: String,
    val tableName: String? = null,
    val items: List<Map<String, String>>? = null
)

@Serializable
data class SelectionUploadResponse(
    val requestId: String,
    val documentId: String,
    val fileName: String,
    val objectStorageUrl: String,
    val status: String
)

@Serializable
data class SelectionDetailsResponse(
    val request: SelectionRequestRecord,
    val criteria: List<SelectionCriterionRecord>,
    val results: List<SelectionResultRecord>
)

@Serializable
data class CalibrationDetailResponse(
    val setting: SelectionCalibrationSettingRecord,
    val items: List<SelectionCalibrationItemRecord>
)

@Serializable
data class ReviewSelectionResultRequest(
    val action: String, // "approve", "reject", "override_rank", "add_notes"
    val notes: String? = null,
    val overrideRankPosition: Int? = null,
    val overrideClassification: String? = null
)

@Serializable
data class AutoSelectionConfigCreateRequest(
    val folder_path: String,
    val domain_category: String? = null,
    val default_prompt: String = "Lakukan evaluasi dan ranking otomatis untuk dokumen yang diunggah",
    val is_enabled: Boolean = true,
    val calibration_settings_id: String? = null,
    val auto_execute_downstream: Boolean = false
)

@Serializable
data class StorageUploadWebhookPayload(
    val type: String? = null,
    val table: String? = null,
    val schema: String? = null,
    val record: StorageObjectRecord? = null,
    val tenant_id: String? = null,
    val bucket: String? = null,
    val path: String? = null,
    val file_name: String? = null,
    val file_type: String? = null,
    val storage_url: String? = null,
    val file_content_base64: String? = null
)

@Serializable
data class StorageObjectRecord(
    val id: String? = null,
    val bucket_id: String? = null,
    val name: String? = null,
    val owner: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val last_accessed_at: String? = null,
    val metadata: JsonObject? = null
)

@Serializable
data class IntegrationFabricWebhookPayload(
    val tenant_id: String? = null,
    val source_system: String = "ERP",
    val domain: String? = null,
    val prompt: String = "Evaluasi dan ranking otomatis dari Integration Fabric",
    val table_name: String? = null,
    val items: List<Map<String, String>> = emptyList(),
    val calibration_settings_id: String? = null
)

@Serializable
data class StorageWebhookResponse(
    val status: String,
    val auto_selection_triggered: Boolean,
    val selection_request_id: String? = null,
    val document_id: String? = null,
    val folder_path: String? = null,
    val domain_category: String? = null,
    val assigned_ai_job_title: String? = null,
    val assigned_ai_job_code: String? = null,
    val assigned_ai_job_id: String? = null,
    val message: String? = null,
    val tenant_id: String? = null
)

@Serializable
data class IntegrationFabricWebhookResponse(
    val status: String,
    val selection_request_id: String,
    val source_type: String,
    val source_system: String,
    val domain_category: String,
    val assigned_ai_job_title: String,
    val assigned_ai_job_code: String,
    val assigned_ai_job_id: String,
    val items_received: Int
)

fun Route.selectionRoutes() {
    val logger = LoggerFactory.getLogger("SelectionRoutes")
    val supabase = SupabaseClientProvider.fromEnv()
    val modelRouter = ModelRouter()
    val selectionRepo = SelectionRepository(supabase, modelRouter)
    val selectionEngine = SelectionEngine(supabase, modelRouter, selectionRepo)
    val autoSelectionRepo = AutoSelectionConfigRepository.defaultInstance
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    route("/selection") {

        /**
         * LANGKAH 1 — POST /api/v1/selection/upload
         * Terima multipart file (Excel/CSV/PDF/Word/Gambar), simpan ke Supabase Storage
         * bucket "selection-datasets", buat row di selection_source_documents & selection_requests,
         * dan picu proses asinkron processUploadedDataset.
         */
        post("/upload") {
            if (!call.enforceEntitlementGate("universal_selection")) return@post
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"
            val userId = principal?.payload?.getClaim("sub")?.asString()
                ?: call.request.headers["X-User-Id"]
                ?: "user-default"

            var fileName = "dataset.csv"
            var fileBytes: ByteArray? = null
            var promptText = "Pilih kandidat atau data terbaik berdasarkan kriteria umum"
            var calibrationId: String? = null
            var folderPath: String = ""

            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        fileName = part.originalFileName ?: "uploaded_dataset"
                        fileBytes = part.provider().toByteArray()
                    }
                    is PartData.FormItem -> {
                        when (part.name) {
                            "prompt", "prompt_text" -> promptText = part.value
                            "calibration_settings_id" -> calibrationId = part.value
                            "folder", "folder_path" -> folderPath = part.value
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (fileBytes == null || fileBytes!!.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file uploaded or file is empty"))
                return@post
            }

            val fileExt = fileName.substringAfterLast('.', "csv").lowercase()
            val fileType = when (fileExt) {
                "xlsx", "xls" -> "xlsx"
                "csv" -> "csv"
                "pdf" -> "pdf"
                "docx", "doc" -> "docx"
                "png", "jpg", "jpeg" -> "image"
                else -> fileExt
            }

            // Cek apakah ada konfigurasi Auto-Selection yang cocok untuk folder ini
            val matchingConfig = autoSelectionRepo.getConfigForPath(tenantId, folderPath)
                ?: autoSelectionRepo.getConfigForPath(tenantId, fileName)

            val effectivePrompt = if (promptText == "Pilih kandidat atau data terbaik berdasarkan kriteria umum" && matchingConfig != null) {
                matchingConfig.defaultPrompt
            } else {
                promptText
            }
            val effectiveCalibrationId = calibrationId ?: matchingConfig?.calibrationSettingsId
            val effectiveDomain = matchingConfig?.domainCategory ?: SelectionEngine.inferDomainFromPathOrPrompt("$folderPath/$fileName")
            val assignedJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle(effectiveDomain)

            // 1. Simpan ke Supabase Storage bucket 'selection-datasets'
            val storagePath = if (folderPath.isNotBlank()) "$tenantId/$folderPath/${System.currentTimeMillis()}_$fileName" else "$tenantId/${System.currentTimeMillis()}_$fileName"
            val uploadRes = supabase.uploadStorageObject(
                bucketName = "selection-datasets",
                filePath = storagePath,
                fileBytes = fileBytes!!,
                contentType = when (fileType) {
                    "csv" -> "text/csv"
                    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    "pdf" -> "application/pdf"
                    else -> "application/octet-stream"
                }
            )

            val objectUrl = "https://exfvfyiwftywqjcsofgf.supabase.co/storage/v1/object/public/selection-datasets/$storagePath"

            // 2. Buat row selection_requests
            val reqId = UUID.randomUUID().toString()
            val reqRecord = SelectionRequestRecord(
                id = reqId,
                tenant_id = tenantId,
                requested_by_user_id = userId,
                prompt_text = effectivePrompt,
                domain_category = effectiveDomain,
                source_type = if (matchingConfig != null) "storage_auto_selection" else "file_upload",
                assigned_ai_job_title_id = assignedJob.id,
                calibration_settings_id = effectiveCalibrationId,
                status = "processing"
            )
            selectionRepo.createSelectionRequest(reqRecord)

            // 3. Buat row selection_source_documents
            val docId = UUID.randomUUID().toString()
            val docRecord = SelectionSourceDocumentRecord(
                id = docId,
                selection_request_id = reqId,
                file_name = fileName,
                file_type = fileType,
                object_storage_url = objectUrl,
                extraction_status = "processing"
            )
            selectionRepo.createSourceDocument(docRecord, tenantId)

            // 4. Picu pemrosesan dataset asinkron
            val rawBytes = fileBytes!!
            selectionEngine.objectStorage.putMemoryObject(objectUrl, rawBytes)
            scope.launch {
                try {
                    selectionEngine.processUploadedDataset(docId)
                } catch (e: Exception) {
                    logger.error("Error running processUploadedDataset: ${e.message}", e)
                }
            }

            call.respond(
                HttpStatusCode.Accepted,
                SelectionUploadResponse(
                    requestId = reqId,
                    documentId = docId,
                    fileName = fileName,
                    objectStorageUrl = objectUrl,
                    status = "processing"
                )
            )
        }

        /**
         * LANGKAH 2 — POST /api/v1/selection/prompt-only
         * Seleksi tanpa upload, membaca data dari Company Brain (Hybrid Search)
         */
        post("/prompt-only") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"
            val userId = principal?.payload?.getClaim("sub")?.asString()
                ?: call.request.headers["X-User-Id"]
                ?: "user-default"

            val body = call.receive<PromptOnlySelectionRequest>()
            val reqId = UUID.randomUUID().toString()
            val reqRecord = SelectionRequestRecord(
                id = reqId,
                tenant_id = tenantId,
                requested_by_user_id = userId,
                prompt_text = body.prompt,
                source_type = "prompt_only",
                calibration_settings_id = body.calibrationSettingsId,
                status = "processing"
            )
            selectionRepo.createSelectionRequest(reqRecord)

            scope.launch {
                selectionEngine.processPromptOnlySelection(reqId, tenantId, body.prompt)
            }

            call.respond(
                HttpStatusCode.Accepted,
                mapOf(
                    "requestId" to reqId,
                    "status" to "processing",
                    "sourceType" to "prompt_only"
                )
            )
        }

        /**
         * LANGKAH 4 — POST /api/v1/selection/api-database
         * Seleksi dari integrasi database / external API
         */
        post("/api-database") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"
            val userId = principal?.payload?.getClaim("sub")?.asString()
                ?: call.request.headers["X-User-Id"]
                ?: "user-default"

            val body = call.receive<ApiDatabaseSelectionRequest>()
            val reqId = UUID.randomUUID().toString()
            val reqRecord = SelectionRequestRecord(
                id = reqId,
                tenant_id = tenantId,
                requested_by_user_id = userId,
                prompt_text = body.prompt,
                source_type = "api_database",
                status = "processing"
            )
            selectionRepo.createSelectionRequest(reqRecord)

            scope.launch {
                selectionEngine.processApiDatabaseSelection(
                    selectionRequestId = reqId,
                    tenantId = tenantId,
                    promptText = body.prompt,
                    tableName = body.tableName,
                    rowsInput = body.items
                )
            }

            call.respond(
                HttpStatusCode.Accepted,
                mapOf(
                    "requestId" to reqId,
                    "status" to "processing",
                    "sourceType" to "api_database"
                )
            )
        }

        /**
         * GET /api/v1/selection/requests — List all selection requests for tenant
         */
        get("/requests") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"

            val list = selectionRepo.listSelectionRequests(tenantId)
            call.respond(HttpStatusCode.OK, list)
        }

        /**
         * GET /api/v1/selection/{id} — Ambil detail request, kriteria penilaian, dan hasil seleksi/ranking
         */
        get("/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"
            val reqId = call.parameters["id"] ?: ""

            val request = selectionRepo.getSelectionRequestById(reqId, tenantId)
            if (request == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Selection request not found"))
                return@get
            }

            val criteria = selectionRepo.getSelectionCriteria(reqId, tenantId)
            val results = selectionRepo.getSelectionResults(reqId, tenantId)

            call.respond(
                HttpStatusCode.OK,
                SelectionDetailsResponse(
                    request = request,
                    criteria = criteria,
                    results = results
                )
            )
        }

        /**
         * GET /api/v1/selection/{id}/results — Ambil spesifik hasil evaluasi dan ranking
         */
        get("/{id}/results") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"
            val reqId = call.parameters["id"] ?: ""

            val results = selectionRepo.getSelectionResults(reqId, tenantId)
            call.respond(HttpStatusCode.OK, results)
        }

        /**
         * GET /api/v1/selection/{id}/analytics — Ambil ringkasan analitik dan distribusi
         */
        get("/{id}/analytics") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"
            val reqId = call.parameters["id"] ?: ""

            val analytics = selectionRepo.getSelectionAnalytics(reqId, tenantId)
            call.respond(HttpStatusCode.OK, analytics)
        }

        /**
         * POST /api/v1/selection/documents/{documentId}/understand — Analisis pemahaman dataset & kualitas data (Bagian C)
         */
        post("/documents/{documentId}/understand") {
            val documentId = call.parameters["documentId"] ?: ""
            if (documentId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "documentId is required"))
                return@post
            }

            try {
                val understanding = selectionEngine.understandDataset(documentId)
                call.respond(HttpStatusCode.OK, understanding)
            } catch (e: NoSuchElementException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "Document not found")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Data understanding error")))
            }
        }

        /**
         * GET /api/v1/selection/documents/{documentId}/understanding — Ambil hasil pemahaman dataset & kualitas data
         */
        get("/documents/{documentId}/understanding") {
            val documentId = call.parameters["documentId"] ?: ""
            if (documentId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "documentId is required"))
                return@get
            }

            try {
                val understanding = selectionEngine.understandDataset(documentId)
                call.respond(HttpStatusCode.OK, understanding)
            } catch (e: NoSuchElementException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "Document not found")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Data understanding error")))
            }
        }

        /**
         * POST /api/v1/selection/calibration — Buat kalibrasi bobot kriteria oleh pengguna (Bagian D)
         */
        post("/calibration") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"
            val userId = principal?.payload?.getClaim("user_id")?.asString()
                ?: call.request.headers["X-User-Id"]
                ?: "user-001"
            val userRole = principal?.payload?.getClaim("role")?.asString()
            if (userRole.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Role claim missing from authenticated token"))
                return@post
            }

            // RBAC verification
            if (userRole == "STAFF_HUMAN") {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Staff role cannot create calibration settings"))
                return@post
            }

            try {
                val body = call.receive<CalibrationRequest>()
                if (body.items.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Calibration items cannot be empty"))
                    return@post
                }
                val result = selectionEngine.validateAndSaveCalibration(tenantId, userId, body)
                call.respond(HttpStatusCode.Created, result)
            } catch (e: Exception) {
                logger.error("Error creating calibration: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed to save calibration")))
            }
        }

        /**
         * GET /api/v1/selection/calibration — Daftar konfigurasi kalibrasi tenant
         */
        get("/calibration") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"

            val list = selectionRepo.listCalibrationSettings(tenantId)
            call.respond(HttpStatusCode.OK, list)
        }

        /**
         * GET /api/v1/selection/calibration/{id} — Ambil detail kalibrasi beserta item kriteria bobotnya
         */
        get("/calibration/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"
            val calibId = call.parameters["id"] ?: ""

            val setting = selectionRepo.getCalibrationSetting(calibId, tenantId)
            if (setting == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Calibration setting not found"))
                return@get
            }
            val items = selectionRepo.getCalibrationItems(calibId, tenantId)
            call.respond(HttpStatusCode.OK, CalibrationDetailResponse(
                setting = setting,
                items = items
            ))
        }

        /**
         * POST /api/v1/selection/calibration/{id}/validate-dataset/{documentId} — Validasi pemetaan kalibrasi ke dataset
         */
        post("/calibration/{id}/validate-dataset/{documentId}") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"
            val calibId = call.parameters["id"] ?: ""
            val documentId = call.parameters["documentId"] ?: ""

            val setting = selectionRepo.getCalibrationSetting(calibId, tenantId)
            if (setting == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Calibration setting not found"))
                return@post
            }
            val items = selectionRepo.getCalibrationItems(calibId, tenantId)
            val doc = selectionRepo.getSourceDocument(documentId, tenantId)
            if (doc == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Source document not found"))
                return@post
            }

            val extracted = selectionEngine.selectionSourceDocumentRepo.getExtractedRows(documentId)
            val schemaDetection = selectionEngine.llm.detectSchemaAndFields(extracted)
            val detectedFields = schemaDetection.fields
            val calibrationItems = items.map { CalibrationItem(it.field_type_name, it.percentage) }

            val validation = selectionEngine.calibrationService.validateCalibrationAgainstDataset(calibrationItems, detectedFields)
            if (!validation.isSuccess) {
                call.respond(HttpStatusCode.BadRequest, validation)
            } else {
                call.respond(HttpStatusCode.OK, validation)
            }
        }

        val deterministicRenderer = DeterministicRenderer()

        /**
         * LANGKAH 2.1: POST /api/v1/selection/results/{id}/review
         * Approve / Reject / Override Ranking / Add Notes
         * SETIAP aksi tercatat ke selection_audit_log DAN Audit Ledger global.
         */
        post("/results/{id}/review") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"
            val userId = principal?.payload?.getClaim("sub")?.asString()
                ?: principal?.payload?.getClaim("user_id")?.asString()
                ?: call.request.headers["X-User-Id"]
                ?: "user-reviewer"
            val resultId = call.parameters["id"] ?: ""

            val body = call.receive<ReviewSelectionResultRequest>()
            val updated = selectionRepo.updateResultReview(
                resultId = resultId,
                tenantId = tenantId,
                action = body.action,
                reviewerId = userId,
                notes = body.notes,
                overrideRankPosition = body.overrideRankPosition,
                overrideClassification = body.overrideClassification
            )

            if (updated.isSuccess) {
                call.respond(HttpStatusCode.OK, updated.getOrThrow())
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (updated.exceptionOrNull()?.message ?: "Review update failed")))
            }
        }

        /**
         * LANGKAH 2.2: POST /api/v1/selection/results/{id}/execute-downstream
         * TEGASKAN: keputusan FINAL tetap di tangan manusia — AI recommendation_classification="selected"
         * TIDAK PERNAH otomatis mengeksekusi aksi lanjutan TANPA human_review_status="approved" eksplisit.
         */
        post("/results/{id}/execute-downstream") {
            val principal = call.principal<JWTPrincipal>()
            val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                ?: call.request.headers["X-Tenant-Id"]
                ?: "tenant-enterprise-001"
            val resultId = call.parameters["id"] ?: ""

            val resultRecord = selectionRepo.getSelectionResultById(resultId, tenantId)
            if (resultRecord == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Selection result not found"))
                return@post
            }

            if (!selectionRepo.canExecuteDownstreamAction(resultRecord)) {
                call.respond(
                    HttpStatusCode.PreconditionFailed,
                    mapOf(
                        "error" to "Downstream execution prohibited: Selection result requires explicit human approval (human_review_status='approved') before auto-execution.",
                        "current_status" to resultRecord.human_review_status,
                        "recommendation_classification" to resultRecord.recommendation_classification
                    )
                )
                return@post
            }

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "executed",
                    "result_id" to resultId,
                    "action" to "downstream_action_approved_and_executed",
                    "reviewer_id" to resultRecord.human_reviewer_id,
                    "reviewed_at" to resultRecord.human_reviewed_at
                )
            )
        }

        /**
         * LANGKAH 3: POST /api/v1/selection/{id}/export (dan GET /{id}/export)
         * Export & Report Generator Deterministic (PDF, XLSX, CSV)
         */
        route("/{id}/export") {
            suspend fun handleExport(call: io.ktor.server.application.ApplicationCall) {
                val principal = call.principal<JWTPrincipal>()
                val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                    ?: call.request.headers["X-Tenant-Id"]
                    ?: "tenant-enterprise-001"
                val reqId = call.parameters["id"] ?: ""
                val format = call.request.queryParameters["format"]?.lowercase() ?: "csv"

                val results = selectionRepo.getSelectionResults(reqId, tenantId)
                val criteria = selectionRepo.getSelectionCriteria(reqId, tenantId)

                when (format) {
                    "xlsx", "excel" -> {
                        val bytes = deterministicRenderer.renderSelectionXlsx(reqId, results, criteria)
                        call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"selection_$reqId.xlsx\"")
                        call.respondBytes(bytes, ContentType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    }
                    "pdf" -> {
                        val bytes = deterministicRenderer.renderSelectionPdf(reqId, results, criteria)
                        call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"selection_$reqId.pdf\"")
                        call.respondBytes(bytes, ContentType.Application.Pdf)
                    }
                    else -> { // Default "csv"
                        val csv = deterministicRenderer.renderSelectionCsv(reqId, results, criteria)
                        call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"selection_$reqId.csv\"")
                        call.respondText(csv, ContentType.Text.CSV)
                    }
                }
            }

            get { handleExport(call) }
            post { handleExport(call) }
        }

        /**
         * LANGKAH 2.1: Auto-Selection Configuration Endpoints
         * GET /api/v1/selection/auto-selection/configs
         * POST /api/v1/selection/auto-selection/configs
         * DELETE /api/v1/selection/auto-selection/configs/{id}
         */
        route("/auto-selection") {
            get("/configs") {
                val principal = call.principal<JWTPrincipal>()
                val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                    ?: call.request.headers["X-Tenant-Id"]
                    ?: "tenant-enterprise-001"

                val configs = autoSelectionRepo.getConfigsForTenant(tenantId)
                call.respond(HttpStatusCode.OK, configs)
            }

            post("/configs") {
                val principal = call.principal<JWTPrincipal>()
                val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                    ?: call.request.headers["X-Tenant-Id"]
                    ?: "tenant-enterprise-001"

                val req = call.receive<AutoSelectionConfigCreateRequest>()
                val config = AutoSelectionFolderConfig(
                    id = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    folderPath = req.folder_path,
                    domainCategory = req.domain_category,
                    defaultPrompt = req.default_prompt,
                    isEnabled = req.is_enabled,
                    calibrationSettingsId = req.calibration_settings_id,
                    autoExecuteDownstream = req.auto_execute_downstream
                )
                val saved = autoSelectionRepo.saveConfig(config).getOrThrow()
                call.respond(HttpStatusCode.Created, saved)
            }

            delete("/configs/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val tenantId = principal?.payload?.getClaim("tenant_id")?.asString()
                    ?: call.request.headers["X-Tenant-Id"]
                    ?: "tenant-enterprise-001"
                val id = call.parameters["id"] ?: ""
                val deleted = autoSelectionRepo.deleteConfig(tenantId, id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("status" to "deleted", "id" to id))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Config $id not found"))
                }
            }
        }

        /**
         * LANGKAH 2.1: Automation Trigger Endpoints
         * POST /api/v1/selection/webhook/storage
         * Trigger: file baru masuk (webhook Supabase Storage upload event) -> otomatis picu Selection Workflow
         */
        route("/webhook") {
            post("/storage") {
                val principal = call.principal<JWTPrincipal>()
                val headerTenant = principal?.payload?.getClaim("tenant_id")?.asString()
                    ?: call.request.headers["X-Tenant-Id"]

                val payload = try {
                    call.receive<StorageUploadWebhookPayload>()
                } catch (e: Exception) {
                    logger.warn("Failed to parse storage webhook payload: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid storage webhook payload: ${e.message}"))
                    return@post
                }

                // Resolusi raw path & file name
                val objectName = payload.record?.name ?: payload.path ?: payload.file_name ?: ""
                if (objectName.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file name or storage path in payload"))
                    return@post
                }

                // Format path bisa "tenant-enterprise-001/recruitment/kandidat.csv" atau "recruitment/kandidat.csv"
                val segments = objectName.split("/").filter { it.isNotBlank() }
                val resolvedTenantId = when {
                    payload.tenant_id != null -> payload.tenant_id
                    headerTenant != null -> headerTenant
                    segments.size >= 2 && segments[0].startsWith("tenant-") -> segments[0]
                    else -> "tenant-enterprise-001"
                }

                val folderPath = if (segments.size >= 2 && segments[0] == resolvedTenantId) {
                    segments.drop(1).dropLast(1).joinToString("/")
                } else if (segments.size >= 2) {
                    segments.dropLast(1).joinToString("/")
                } else {
                    ""
                }

                val fileName = segments.lastOrNull() ?: "dataset.csv"
                val fullRelPath = if (folderPath.isNotBlank()) "$folderPath/$fileName" else fileName

                // Cek konfigurasi Auto-Selection untuk path ini
                val matchingConfig = autoSelectionRepo.getConfigForPath(resolvedTenantId, fullRelPath)
                    ?: autoSelectionRepo.getConfigForPath(resolvedTenantId, folderPath)

                if (matchingConfig == null || !matchingConfig.isEnabled) {
                    logger.info("Storage event for path '$fullRelPath' ignored: No active auto-selection config for tenant $resolvedTenantId")
                    call.respond(
                        HttpStatusCode.OK,
                        StorageWebhookResponse(
                            status = "ignored",
                            auto_selection_triggered = false,
                            message = "No active auto-selection configured for path: $fullRelPath",
                            tenant_id = resolvedTenantId
                        )
                    )
                    return@post
                }

                val fileExt = fileName.substringAfterLast('.', "csv").lowercase()
                val fileType = when (fileExt) {
                    "xlsx", "xls" -> "xlsx"
                    "csv" -> "csv"
                    "pdf" -> "pdf"
                    "docx", "doc" -> "docx"
                    "png", "jpg", "jpeg", "webp" -> "image"
                    else -> fileExt
                }

                // Petakan domain ke salah satu dari 15 Jabatan Utama AI
                val domain = matchingConfig.domainCategory
                    ?: SelectionEngine.inferDomainFromPathOrPrompt("$folderPath/$fileName")
                val assignedJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle(domain)

                val reqId = UUID.randomUUID().toString()
                val docId = UUID.randomUUID().toString()
                val storageUrl = payload.storage_url
                    ?: "https://exfvfyiwftywqjcsofgf.supabase.co/storage/v1/object/public/${payload.bucket ?: "selection-datasets"}/$objectName"

                // Jika payload menyediakan file content langsung (base64)
                if (!payload.file_content_base64.isNullOrBlank()) {
                    try {
                        val bytes = java.util.Base64.getDecoder().decode(payload.file_content_base64)
                        selectionEngine.objectStorage.putMemoryObject(storageUrl, bytes)
                    } catch (e: Exception) {
                        logger.warn("Could not decode file_content_base64: ${e.message}")
                    }
                }

                val reqRecord = SelectionRequestRecord(
                    id = reqId,
                    tenant_id = resolvedTenantId,
                    requested_by_user_id = "auto-selection-daemon",
                    prompt_text = matchingConfig.defaultPrompt,
                    domain_category = domain,
                    source_type = "storage_auto_selection",
                    assigned_ai_job_title_id = assignedJob.id,
                    calibration_settings_id = matchingConfig.calibrationSettingsId,
                    status = "processing"
                )
                selectionRepo.createSelectionRequest(reqRecord)

                val docRecord = SelectionSourceDocumentRecord(
                    id = docId,
                    selection_request_id = reqId,
                    file_name = fileName,
                    file_type = fileType,
                    object_storage_url = storageUrl,
                    extraction_status = "processing"
                )
                selectionRepo.createSourceDocument(docRecord, resolvedTenantId)

                // Audit log trigger otomatis
                selectionRepo.insertAuditLog(
                    ai.orchestree.backend.database.repositories.selection.SelectionAuditLogRecord(
                        selection_request_id = reqId,
                        action_type = "auto_selection_triggered",
                        actor_type = "system_automation",
                        actor_id = "storage_webhook",
                        detail = buildJsonObject {
                            put("matched_folder", matchingConfig.folderPath)
                            put("file_name", fileName)
                            put("domain_category", domain)
                            put("assigned_job_title", assignedJob.jobName)
                            put("assigned_job_id", assignedJob.id)
                        }
                    ),
                    resolvedTenantId
                )

                // Jalankan Selection Workflow di background
                scope.launch {
                    try {
                        selectionEngine.processUploadedDataset(docId)
                        logger.info("Auto-selection successfully executed for doc: $docId, req: $reqId")
                    } catch (e: Exception) {
                        logger.error("Auto-selection workflow execution failed for doc $docId: ${e.message}", e)
                    }
                }

                call.respond(
                    HttpStatusCode.Accepted,
                    StorageWebhookResponse(
                        status = "triggered",
                        auto_selection_triggered = true,
                        selection_request_id = reqId,
                        document_id = docId,
                        folder_path = matchingConfig.folderPath,
                        domain_category = domain,
                        assigned_ai_job_title = assignedJob.jobName,
                        assigned_ai_job_code = assignedJob.jobCode,
                        assigned_ai_job_id = assignedJob.id,
                        tenant_id = resolvedTenantId
                    )
                )
            }

            /**
             * Trigger: webhook dari Integration Fabric (data baru masuk dari ERP/CRM eksternal)
             */
            post("/integration-fabric") {
                val principal = call.principal<JWTPrincipal>()
                val headerTenant = principal?.payload?.getClaim("tenant_id")?.asString()
                    ?: call.request.headers["X-Tenant-Id"]

                val payload = try {
                    call.receive<IntegrationFabricWebhookPayload>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid integration fabric payload: ${e.message}"))
                    return@post
                }

                val resolvedTenant = payload.tenant_id ?: headerTenant ?: "tenant-enterprise-001"
                val domain = payload.domain ?: "procurement"
                val assignedJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle(domain)

                val reqId = UUID.randomUUID().toString()
                val reqRecord = SelectionRequestRecord(
                    id = reqId,
                    tenant_id = resolvedTenant,
                    requested_by_user_id = "integration-fabric-${payload.source_system}",
                    prompt_text = payload.prompt,
                    domain_category = domain,
                    source_type = "integration_fabric",
                    assigned_ai_job_title_id = assignedJob.id,
                    calibration_settings_id = payload.calibration_settings_id,
                    status = "processing"
                )
                selectionRepo.createSelectionRequest(reqRecord)

                // Audit log
                selectionRepo.insertAuditLog(
                    ai.orchestree.backend.database.repositories.selection.SelectionAuditLogRecord(
                        selection_request_id = reqId,
                        action_type = "integration_fabric_selection_triggered",
                        actor_type = "integration_fabric",
                        actor_id = payload.source_system,
                        detail = buildJsonObject {
                            put("source_system", payload.source_system)
                            put("domain_category", domain)
                            put("assigned_job_title", assignedJob.jobName)
                            put("assigned_job_id", assignedJob.id)
                            put("item_count", payload.items.size)
                        }
                    ),
                    resolvedTenant
                )

                scope.launch {
                    try {
                        selectionEngine.processApiDatabaseSelection(
                            selectionRequestId = reqId,
                            tenantId = resolvedTenant,
                            promptText = payload.prompt,
                            tableName = payload.table_name ?: "integration_records",
                            rowsInput = payload.items
                        )
                        logger.info("Integration Fabric selection completed for req: $reqId")
                    } catch (e: Exception) {
                        logger.error("Integration Fabric selection failed for req $reqId: ${e.message}", e)
                    }
                }

                call.respond(
                    HttpStatusCode.Accepted,
                    IntegrationFabricWebhookResponse(
                        status = "triggered",
                        selection_request_id = reqId,
                        source_type = "integration_fabric",
                        source_system = payload.source_system,
                        domain_category = domain,
                        assigned_ai_job_title = assignedJob.jobName,
                        assigned_ai_job_code = assignedJob.jobCode,
                        assigned_ai_job_id = assignedJob.id,
                        items_received = payload.items.size
                    )
                )
            }
        }
    }
}
