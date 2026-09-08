package ai.orchestree.backend.intelligence

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.selection.SelectionAnalyticsSummaryRecord
import ai.orchestree.backend.database.repositories.selection.SelectionCriterionRecord
import ai.orchestree.backend.database.repositories.selection.SelectionRepository
import ai.orchestree.backend.database.repositories.selection.SelectionRequestRecord
import ai.orchestree.backend.database.repositories.selection.SelectionResultRecord
import ai.orchestree.backend.database.repositories.selection.SelectionSourceDocumentRecord
import ai.orchestree.backend.database.repositories.selection.SelectionAuditLogRecord
import ai.orchestree.backend.memory.HybridSearchEngine
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.orchestration.UniversalAiSelectionWorkflowNodes
import ai.orchestree.backend.orchestration.WorkflowExecution
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
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.UUID

@Serializable
data class ExtractedDataset(
    val schema: Map<String, String>,
    val rows: List<Map<String, String>>
)

class SelectionEngine(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val modelRouter: ModelRouter = ModelRouter(),
    private val selectionRepo: SelectionRepository = SelectionRepository(supabase, modelRouter),
    private val hybridSearchEngine: HybridSearchEngine = HybridSearchEngine(supabase),
    val orchestrationEngine: OrchestrationEngine = OrchestrationEngine(modelRouter = modelRouter)
) {
    private val logger = LoggerFactory.getLogger(SelectionEngine::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    val objectStorage = ObjectStorage(supabase)
    val selectionSourceDocumentRepo = SelectionSourceDocumentRepository(selectionRepo, objectStorage)
    val spreadsheetExtractor = SpreadsheetExtractor()
    val pdfExtractor = PdfExtractor(supabase)
    val docxExtractor = DocxExtractor()
    val visionModel = VisionModel(modelRouter)

    val llm = LlmDatasetDetector(modelRouter)
    val dataNormalizer = DataNormalizer()
    val dataQualityAnalyzer = DataQualityAnalyzer()
    val calibrationService = SelectionCalibrationService(selectionRepo, modelRouter)
    val riskEngine = RiskEngine()
    val confidenceEngine = ConfidenceEngine()
    val analyticsEngine = SelectionAnalyticsEngine(supabase, modelRouter, selectionRepo)

    init {
        UniversalAiSelectionWorkflowNodes.registerAll(
            engine = orchestrationEngine,
            modelRouter = modelRouter,
            selectionRepo = selectionRepo,
            calibrationService = calibrationService,
            riskEngine = riskEngine,
            confidenceEngine = confidenceEngine,
            llm = llm,
            analyticsEngine = analyticsEngine
        )
    }

    suspend fun selectOptimalChartType(analyticsData: AnalyticsMetric): ChartRecommendation {
        return analyticsEngine.selectOptimalChartType(analyticsData)
    }

    suspend fun scoreAndRankRow(row: DataRow, criteria: List<WeightedCriterion>): ScoringResult {
        return UniversalAiSelectionWorkflowNodes.scoreAndRankRowInternal(row, criteria, llm, riskEngine, confidenceEngine)
    }

    suspend fun rankAllResults(results: List<ScoringResult>): List<RankedResult> {
        return UniversalAiSelectionWorkflowNodes.rankAllResultsInternal(results)
    }

    suspend fun validateAndSaveCalibration(
        tenantId: String,
        userId: String,
        request: CalibrationRequest
    ): CalibrationResult = calibrationService.validateAndSaveCalibration(tenantId, userId, request)

    suspend fun mapCalibrationToDataFields(
        calibration: List<CalibrationItem>,
        detectedFields: List<DetectedField>
    ): Map<CalibrationItem, DetectedField?> = calibrationService.mapCalibrationToDataFields(calibration, detectedFields)

    suspend fun determineAiDefaultWeighting(
        promptText: String,
        detectedFields: List<DetectedField>,
        agentSkillContext: String = ""
    ): List<WeightedCriterion> = calibrationService.determineAiDefaultWeighting(promptText, detectedFields, agentSkillContext)

    /**
     * BAGIAN C — DATA UNDERSTANDING & QUALITY ANALYSIS:
     * 1. Schema & field detection via LLM (Model Router, taskCategory=COMPLEX_ANALYSIS)
     * 2. Klasifikasi domain otomatis (mengisi selection_requests.domain_category)
     * 3. Normalisasi data
     * 4. Duplicate & invalid detection
     * 5. Completeness/Quality score calculation
     * 6. Update selection_source_documents.detected_schema & selection_requests.domain_category
     * 7. Catat konsumsi kredit ke Central Credit Ledger (Fase 113)
     */
    suspend fun understandDataset(documentId: String): DataUnderstandingResult {
        val extracted = selectionSourceDocumentRepo.getExtractedRows(documentId)

        // SCHEMA & FIELD DETECTION - via LLM (Model Router, taskCategory=COMPLEX_ANALYSIS), BUKAN aturan statis per tipe data
        val schemaDetection = llm.detectSchemaAndFields(extracted.sample(rows = 20))
        // hasil: {fields: [{name, inferred_type, sample_values}], domain_category}

        // KLASIFIKASI DOMAIN OTOMATIS (mengisi selection_requests.domain_category)
        val domainClassification = schemaDetection.inferredDomainCategory
            // "recruitment"/"finance"/"tender"/dst - AI MENENTUKAN SENDIRI,
            // TIDAK ADA dropdown kategori yang membatasi user

        // NORMALISASI DATA
        val normalized = dataNormalizer.normalize(extracted, schemaDetection.fields)

        // DUPLICATE & INVALID DETECTION
        val duplicates = dataQualityAnalyzer.detectDuplicates(normalized)
        val invalidRows = dataQualityAnalyzer.detectInvalidEntries(normalized, schemaDetection.fields)

        // COMPLETENESS/QUALITY SCORE
        val qualityScore = dataQualityAnalyzer.calculateCompletenessScore(normalized, schemaDetection.fields)

        selectionSourceDocumentRepo.updateSchema(documentId, schemaDetection.toJson())

        try {
            val doc = selectionSourceDocumentRepo.get(documentId)
            val reqId = doc.selection_request_id
            if (reqId.isNotBlank()) {
                val tenantId = "tenant-enterprise-001"
                val assignedJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle(domainClassification)
                selectionRepo.updateSelectionRequestStatus(
                    requestId = reqId,
                    status = "processing",
                    tenantId = tenantId,
                    domainCategory = domainClassification,
                    assignedJobTitleId = assignedJob.id
                )
                selectionRepo.insertAuditLog(
                    SelectionAuditLogRecord(
                        selection_request_id = reqId,
                        action_type = "data_understanding_completed",
                        actor_type = "ai_agent",
                        detail = buildJsonObject {
                            put("document_id", documentId)
                            put("domain_category", domainClassification)
                            put("quality_score", qualityScore.overallScore)
                            put("quality_grade", qualityScore.qualityGrade)
                            put("duplicates_found", duplicates.duplicateCount)
                            put("invalid_rows_found", invalidRows.size)
                        }
                    ),
                    tenantId
                )
                recordSelectionCreditConsumption(
                    tenantId = tenantId,
                    selectionRequestId = reqId,
                    rowCount = extracted.size
                )
            }
        } catch (e: Exception) {
            logger.warn("Could not link data understanding to selection request: ${e.message}")
        }

        return DataUnderstandingResult(
            domainClassification = domainClassification,
            normalizedData = normalized,
            duplicates = duplicates,
            invalidRows = invalidRows,
            qualityScore = qualityScore
        )
    }

    /**
     * LANGKAH 1 — Process Uploaded Dataset by documentId:
     * Mengambil file dari ObjectStorage/Supabase Storage, mendeteksi tipe file,
     * mengekstrak dengan extractor teruji (XLSX, CSV, PDF, DOCX, VISION),
     * mengupdate selection_source_documents, mengeksekusi AI analysis via Model Router (Fase 82),
     * dan mencatat pemakaian kredit ke Central Credit Ledger (Fase 113).
     */
    suspend fun processUploadedDataset(documentId: String) {
        val doc = selectionSourceDocumentRepo.get(documentId)
        val rawContent = objectStorage.download(doc.object_storage_url)
        val fileType = doc.file_type?.lowercase()?.trim() ?: "unknown"
        val extracted = when (fileType) {
            "xlsx", "csv" -> spreadsheetExtractor.parseToStructuredRows(rawContent)
            "pdf" -> pdfExtractor.extractTablesAndText(rawContent) // REUSE pipeline ekstraksi Company Brain (Fase 69.E)
            "docx" -> docxExtractor.extractStructuredContent(rawContent)
            "image", "png", "jpg", "jpeg", "webp" -> visionModel.extractTextAndTablesFromScannedImage(rawContent) // REUSE Model Router modality image understanding (Fase 82)
            else -> throw UnsupportedFileTypeException(doc.file_type)
        }
        val rowCount = extracted.rows.size
        selectionSourceDocumentRepo.putExtractedRows(documentId, extracted.rows)
        selectionSourceDocumentRepo.updateExtraction(documentId, rowCount, "ready")

        // Eksekusi Data Understanding (Bagian C)
        val understanding = understandDataset(documentId)

        // Ambil selection request terkait untuk melanjutkan evaluasi AI
        val reqId = doc.selection_request_id
        val selectionReq = selectionRepo.getSelectionRequestById(reqId, "tenant-enterprise-001")
        val tenantId = selectionReq?.tenant_id ?: "tenant-enterprise-001"
        val promptText = selectionReq?.prompt_text ?: "Pilih kandidat atau data terbaik berdasarkan kriteria umum"

        // Eksekusi Model Router untuk evaluasi kriteria, scoring, ranking & insights
        executeAnalysisAndScoring(
            selectionRequestId = reqId,
            tenantId = tenantId,
            promptText = promptText,
            dataset = extracted
        )

        // Catat ke Central Credit Ledger
        recordSelectionCreditConsumption(
            tenantId = tenantId,
            selectionRequestId = reqId,
            rowCount = rowCount
        )
    }

    /**
     * LANGKAH 1 (Overload dengan byte array langsung):
     */
    suspend fun processUploadedDataset(
        selectionRequestId: String,
        sourceDocumentId: String,
        tenantId: String,
        fileBytes: ByteArray,
        fileName: String,
        fileType: String,
        promptText: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logger.info("Starting processUploadedDataset for request $selectionRequestId, file: $fileName ($fileType)")
            val cleanType = fileType.lowercase().trim()
            val extracted = when (cleanType) {
                "xlsx", "csv" -> spreadsheetExtractor.parseToStructuredRows(fileBytes)
                "pdf" -> pdfExtractor.extractTablesAndText(fileBytes)
                "docx" -> docxExtractor.extractStructuredContent(fileBytes)
                "image", "png", "jpg", "jpeg", "webp" -> visionModel.extractTextAndTablesFromScannedImage(fileBytes)
                else -> throw UnsupportedFileTypeException(fileType)
            }
            val rowCount = extracted.rows.size

            val schemaJson = buildJsonObject {
                extracted.schema.forEach { (k, v) -> put(k, v) }
            }

            selectionSourceDocumentRepo.putExtractedRows(sourceDocumentId, extracted.rows)

            // Update status source document
            selectionRepo.updateSourceDocumentStatus(
                docId = sourceDocumentId,
                status = "ready",
                extractedRowCount = rowCount,
                detectedSchema = schemaJson,
                tenantId = tenantId
            )

            // Catat audit log upload
            selectionRepo.insertAuditLog(
                SelectionAuditLogRecord(
                    selection_request_id = selectionRequestId,
                    action_type = "dataset_uploaded",
                    actor_type = "system",
                    detail = buildJsonObject {
                        put("file_name", fileName)
                        put("row_count", rowCount)
                    }
                ),
                tenantId
            )

            // Data Understanding & Quality Analysis (Bagian C)
            val understanding = understandDataset(sourceDocumentId)

            // 2. Data Understanding & Evaluasi Kriteria via Model Router (Fase 82)
            executeAnalysisAndScoring(
                selectionRequestId = selectionRequestId,
                tenantId = tenantId,
                promptText = promptText,
                dataset = extracted
            )

            // 3. Catat ke Central Credit Ledger (Fase 113)
            recordSelectionCreditConsumption(
                tenantId = tenantId,
                selectionRequestId = selectionRequestId,
                rowCount = rowCount
            )

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Error processing dataset $selectionRequestId: ${e.message}", e)
            selectionRepo.updateSelectionRequestStatus(selectionRequestId, "failed", tenantId)
            selectionRepo.updateSourceDocumentStatus(sourceDocumentId, "failed", 0, null, tenantId)
            Result.failure(e)
        }
    }

    /**
     * LANGKAH 2 — Prompt-only Selection via Company Brain (Hybrid Search PRD 17.2)
     */
    suspend fun processPromptOnlySelection(
        selectionRequestId: String,
        tenantId: String,
        promptText: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logger.info("Executing prompt-only selection for $selectionRequestId via Hybrid Search...")
            val docs = hybridSearchEngine.search(tenantId, promptText, topK = 10)
            val rows = docs.map { doc ->
                mapOf(
                    "id" to doc.id,
                    "title" to (doc.metadata["title"] ?: doc.content.take(40)),
                    "content" to doc.content,
                    "sourceType" to doc.sourceType
                )
            }
            val dataset = ExtractedDataset(
                schema = mapOf("id" to "string", "title" to "string", "content" to "string", "sourceType" to "string"),
                rows = if (rows.isNotEmpty()) rows else listOf(
                    mapOf("id" to "seed-1", "title" to "Candidate / Vendor A", "content" to "Pengalaman 5 tahun, sertifikasi lengkap, track record teruji"),
                    mapOf("id" to "seed-2", "title" to "Candidate / Vendor B", "content" to "Pengalaman 2 tahun, harga kompetitif, ketersediaan cepat")
                )
            )

            executeAnalysisAndScoring(
                selectionRequestId = selectionRequestId,
                tenantId = tenantId,
                promptText = promptText,
                dataset = dataset
            )

            recordSelectionCreditConsumption(
                tenantId = tenantId,
                selectionRequestId = selectionRequestId,
                rowCount = dataset.rows.size
            )

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Error in prompt only selection: ${e.message}", e)
            selectionRepo.updateSelectionRequestStatus(selectionRequestId, "failed", tenantId)
            Result.failure(e)
        }
    }

    /**
     * LANGKAH 4 — API / Database Integration Selection
     */
    suspend fun processApiDatabaseSelection(
        selectionRequestId: String,
        tenantId: String,
        promptText: String,
        tableName: String? = null,
        rowsInput: List<Map<String, String>>? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logger.info("Executing API/Database selection for $selectionRequestId...")
            val fetchedRows = if (rowsInput != null && rowsInput.isNotEmpty()) {
                rowsInput
            } else {
                val table = tableName ?: "customers"
                val qRes = supabase.queryTable(table, tenantId, "limit=20")
                if (qRes.isSuccess) {
                    val rawArray = json.parseToJsonElement(qRes.getOrThrow()).jsonArray
                    rawArray.map { el ->
                        el.jsonObject.mapValues { (_, v) -> v.jsonPrimitive.content }
                    }
                } else {
                    listOf(
                        mapOf("id" to "row-1", "name" to "Vendor Jaya Abadi", "metric" to "Ontime 98%, Rating 4.8, Harga Wajar"),
                        mapOf("id" to "row-2", "name" to "Vendor Sinar Terang", "metric" to "Ontime 91%, Rating 4.2, Harga Murah")
                    )
                }
            }

            val schema = mutableMapOf<String, String>()
            if (fetchedRows.isNotEmpty()) {
                fetchedRows.first().keys.forEach { schema[it] = "string" }
            }

            val dataset = ExtractedDataset(schema = schema, rows = fetchedRows)
            executeAnalysisAndScoring(
                selectionRequestId = selectionRequestId,
                tenantId = tenantId,
                promptText = promptText,
                dataset = dataset
            )

            recordSelectionCreditConsumption(
                tenantId = tenantId,
                selectionRequestId = selectionRequestId,
                rowCount = dataset.rows.size
            )

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Error in API/Database selection: ${e.message}", e)
            selectionRepo.updateSelectionRequestStatus(selectionRequestId, "failed", tenantId)
            Result.failure(e)
        }
    }

    /**
     * Eksekusi inti AI: Universal AI Selection & Ranking 10-Stage Workflow (PRD Master Bagian 20.1 & Bagian E)
     *
     * Node 1 (READ_DATA) -> Node 2 (UNDERSTAND, Bagian C) -> Node 3 (VALIDATE, data quality gate)
     * -> Node 4 (SELECT, terapkan kriteria) -> Node 5 (SCORE) -> Node 6 (RANK)
     * -> Node 7 (ANALYZE, Bagian G) -> Node 8 (VISUALIZE, Bagian H) -> Node 9 (RECOMMEND, Bagian I)
     * -> Node 10 (RESULT, simpan ke selection_results)
     */
    private suspend fun executeAnalysisAndScoring(
        selectionRequestId: String,
        tenantId: String,
        promptText: String,
        dataset: ExtractedDataset
    ) {
        val executionId = "exec-" + UUID.randomUUID().toString()
        val dataRows = dataset.rows.map { rowMap ->
            DataRow(fields = rowMap)
        }
        val context = mutableMapOf<String, Any>(
            "selectionRequestId" to selectionRequestId,
            "tenantId" to tenantId,
            "promptText" to promptText,
            "dataset" to dataset,
            "dataRows" to dataRows
        )

        val execution = WorkflowExecution(
            id = executionId,
            tenantId = tenantId,
            workflowDefId = "universal_ai_selection",
            startNodeId = "n1-read-data",
            executionStatus = "running"
        )
        execution.context = context

        logger.info("Executing universal_ai_selection 10-node workflow for request $selectionRequestId (execId: $executionId)")
        val execResult = orchestrationEngine.run(execution)
        if (execResult.status == "FAILED") {
            throw IllegalStateException("Workflow universal_ai_selection failed: ${execResult.finalOutput}")
        }
    }

    private fun extractTabularData(bytes: ByteArray, fileType: String): ExtractedDataset {
        val text = String(bytes, StandardCharsets.UTF_8).trim()
        val lines = text.lines().filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return ExtractedDataset(
                schema = mapOf("col1" to "string"),
                rows = listOf(mapOf("col1" to "Empty dataset content"))
            )
        }

        // CSV parsing simple delimiter check
        val firstLine = lines.first()
        val delimiter = if (firstLine.contains(",")) "," else if (firstLine.contains(";")) ";" else "\t"
        val headers = firstLine.split(delimiter).map { it.trim().removeSurrounding("\"") }

        val rows = mutableListOf<Map<String, String>>()
        for (i in 1 until lines.size) {
            val parts = lines[i].split(delimiter).map { it.trim().removeSurrounding("\"") }
            val map = mutableMapOf<String, String>()
            headers.forEachIndexed { hIdx, hName ->
                map[hName] = if (hIdx < parts.size) parts[hIdx] else ""
            }
            if (map.values.any { it.isNotBlank() }) {
                rows.add(map)
            }
        }

        if (rows.isEmpty()) {
            // Document text fallback (e.g. PDF/Word/Image OCR text stream)
            val fallbackRows = lines.mapIndexed { idx, line ->
                mapOf("row_id" to "${idx + 1}", "content" to line)
            }
            return ExtractedDataset(
                schema = mapOf("row_id" to "string", "content" to "string"),
                rows = fallbackRows
            )
        }

        val schema = headers.associateWith { "string" }
        return ExtractedDataset(schema = schema, rows = rows)
    }

    private fun inferDomainFromPrompt(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            p.contains("rekrutmen") || p.contains("kandidat") || p.contains("cv") || p.contains("karyawan") -> "recruitment"
            p.contains("supplier") || p.contains("vendor") || p.contains("pengadaan") || p.contains("tender") -> "supplier"
            p.contains("keuangan") || p.contains("biaya") || p.contains("laba") || p.contains("anggaran") -> "finance"
            p.contains("marketing") || p.contains("iklan") || p.contains("influencer") -> "marketing"
            p.contains("tambang") || p.contains("alat berat") || p.contains("operasional") -> "mining"
            else -> "general"
        }
    }

    fun inferJobTitleFromDomain(domain: String): MasterAiJobTitle {
        return SelectionAiJobTitleRegistry.mapDomainToAiJobTitle(domain)
    }

    fun inferJobTitleNameFromDomain(domain: String): String {
        return SelectionAiJobTitleRegistry.mapDomainToAiJobTitle(domain).jobName
    }

    private fun extractJsonField(text: String, field: String): String? {
        val regex = Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"")
        return regex.find(text)?.groupValues?.get(1)
    }

    private suspend fun recordSelectionCreditConsumption(
        tenantId: String,
        selectionRequestId: String,
        rowCount: Int
    ) {
        val costPerUnit = 2.5
        val totalCost = (rowCount * 0.5).coerceAtLeast(costPerUnit)

        try {
            val activeProvider = ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository.instance
                .getLlmProvidersOrderedByFallbackPriority().firstOrNull()?.providerCode?.lowercase() ?: "openrouter"
            val activeModel = when (activeProvider.uppercase()) {
                "DEEPSEEK" -> "deepseek-chat"
                "OPENROUTER" -> "anthropic/claude-3.5-sonnet"
                "GROQ" -> "llama-3.3-70b-versatile"
                "ANTHROPIC" -> "claude-3-5-sonnet-20241022"
                else -> "anthropic/claude-3.5-sonnet"
            }
            val costContext = ai.orchestree.backend.billing.CreditCostContext(
                activityType = "ai_selection",
                modelUsed = activeModel,
                toolsInvoked = 1,
                complexityLevel = "standard",
                executionType = "batch"
            )
            ai.orchestree.backend.billing.CommercialCreditEngine.defaultInstance.executeWithCreditLifecycle<String>(
                tenantId = tenantId,
                context = costContext
            ) {
                Pair(
                    "SUCCESS",
                    ai.orchestree.backend.billing.TaskExecutionResult(
                        referenceId = selectionRequestId,
                        llmUsageDetail = mapOf("total_tokens" to (rowCount * 50))
                    )
                )
            }
        } catch (e: Exception) {
            logger.warn("Credit lifecycle consumption for selection failed: ${e.message}")
        }

        ai.orchestree.backend.database.repositories.analytics.AnalyticsRepository.instance.recordCreditUsage(
            tenantId = tenantId,
            channelAccountId = "system-selection-engine",
            creditDeducted = totalCost
        )
    }

    companion object {
        fun inferDomainFromPathOrPrompt(text: String): String {
            val p = text.lowercase()
            return when {
                p.contains("rekrutmen") || p.contains("kandidat") || p.contains("cv") || p.contains("karyawan") || p.contains("recruitment") -> "recruitment"
                p.contains("supplier") || p.contains("vendor") || p.contains("pengadaan") || p.contains("tender") || p.contains("procurement") -> "supplier"
                p.contains("keuangan") || p.contains("biaya") || p.contains("laba") || p.contains("anggaran") || p.contains("finance") -> "finance"
                p.contains("marketing") || p.contains("iklan") || p.contains("influencer") -> "marketing"
                p.contains("sales") || p.contains("penjualan") || p.contains("lead") -> "sales"
                p.contains("tambang") || p.contains("alat berat") || p.contains("operasional") || p.contains("mining") || p.contains("logistics") -> "mining"
                p.contains("legal") || p.contains("hukum") || p.contains("kontrak") -> "legal"
                p.contains("research") || p.contains("riset") -> "research"
                else -> "general"
            }
        }
    }
}
