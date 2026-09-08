package ai.orchestree.backend.orchestration

import ai.orchestree.backend.billing.CentralCreditLedgerService
import ai.orchestree.backend.database.repositories.selection.SelectionAnalyticsSummaryRecord
import ai.orchestree.backend.database.repositories.selection.SelectionCriterionRecord
import ai.orchestree.backend.database.repositories.selection.SelectionRepository
import ai.orchestree.backend.database.repositories.selection.SelectionResultRecord
import ai.orchestree.backend.intelligence.CalibrationItem
import ai.orchestree.backend.intelligence.ConfidenceEngine
import ai.orchestree.backend.intelligence.DataRow
import ai.orchestree.backend.intelligence.DetectedField
import ai.orchestree.backend.intelligence.ExtractedDataset
import ai.orchestree.backend.intelligence.LlmDatasetDetector
import ai.orchestree.backend.intelligence.OutputValidator
import ai.orchestree.backend.intelligence.RankedResult
import ai.orchestree.backend.intelligence.RiskEngine
import ai.orchestree.backend.intelligence.ScoringResult
import ai.orchestree.backend.intelligence.SelectionAnalyticsEngine
import ai.orchestree.backend.intelligence.SelectionCalibrationService
import ai.orchestree.backend.intelligence.SelectionAiJobTitleRegistry
import ai.orchestree.backend.intelligence.WeightedCriterion
import ai.orchestree.backend.intelligence.normalizeNumericScore
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.nodes.GenericStepWorkflowNode
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Universal AI Selection & Ranking 10-Stage Workflow Nodes (PRD Master Bagian 20.1 & Bagian E)
 *
 * Node Chain:
 * Node 1 (READ_DATA) -> Node 2 (UNDERSTAND, Bagian C) -> Node 3 (VALIDATE, data quality gate)
 * -> Node 4 (SELECT, terapkan kriteria) -> Node 5 (SCORE) -> Node 6 (RANK)
 * -> Node 7 (ANALYZE, Bagian G) -> Node 8 (VISUALIZE, Bagian H) -> Node 9 (RECOMMEND, Bagian I)
 * -> Node 10 (RESULT, simpan ke selection_results)
 */
object UniversalAiSelectionWorkflowNodes {
    private val logger = LoggerFactory.getLogger(UniversalAiSelectionWorkflowNodes::class.java)

    fun registerAll(
        engine: OrchestrationEngine,
        modelRouter: ModelRouter,
        selectionRepo: SelectionRepository = SelectionRepository(),
        calibrationService: SelectionCalibrationService = SelectionCalibrationService(selectionRepo, modelRouter),
        riskEngine: RiskEngine = RiskEngine(),
        confidenceEngine: ConfidenceEngine = ConfidenceEngine(),
        llm: LlmDatasetDetector = LlmDatasetDetector(modelRouter),
        analyticsEngine: SelectionAnalyticsEngine = SelectionAnalyticsEngine(modelRouter = modelRouter, selectionRepo = selectionRepo),
        outputValidator: OutputValidator = OutputValidator()
    ) {
        // Node 1: READ_DATA
        engine.registerNode(
            GenericStepWorkflowNode("n1-read-data", WorkflowNodeType.TOOL_CALL, nextNodeId = "n2-understand") { ctx ->
                val rows: List<DataRow> = when {
                    ctx["dataRows"] is List<*> -> (ctx["dataRows"] as List<*>).filterIsInstance<DataRow>()
                    ctx["dataset"] is ExtractedDataset -> {
                        val ds = ctx["dataset"] as ExtractedDataset
                        ds.rows.map { DataRow(fields = it) }
                    }
                    ctx["rows"] is List<*> -> {
                        (ctx["rows"] as List<*>).mapNotNull { item ->
                            when (item) {
                                is DataRow -> item
                                is Map<*, *> -> {
                                    @Suppress("UNCHECKED_CAST")
                                    val map = item as Map<String, Any?>
                                    DataRow(fields = map.mapValues { it.value?.toString() ?: "" })
                                }
                                else -> null
                            }
                        }
                    }
                    else -> emptyList()
                }
                ctx["dataRows"] = rows
                NodeExecutionResult(NodeExecutionStatus.SUCCESS, "READ_DATA: Berhasil membaca ${rows.size} baris dataset.", data = mapOf("rowCount" to rows.size))
            }
        )

        // Node 2: UNDERSTAND (Bagian C)
        engine.registerNode(
            GenericStepWorkflowNode("n2-understand", WorkflowNodeType.LLM_GENERATE, nextNodeId = "n3-validate") { ctx ->
                val dataRows = (ctx["dataRows"] as? List<*>)?.filterIsInstance<DataRow>() ?: emptyList()
                val sampleRows = dataRows.take(15).map { it.fields }
                val detection = llm.detectSchemaAndFields(sampleRows)
                val detectedFields = detection.fields
                val inferredDomain = detection.inferredDomainCategory
                val assignedJob = SelectionAiJobTitleRegistry.mapDomainToAiJobTitle(inferredDomain)

                ctx["detectedFields"] = detectedFields
                ctx["inferredDomain"] = inferredDomain
                ctx["assignedJobTitle"] = assignedJob.jobName
                ctx["assignedJobTitleCode"] = assignedJob.jobCode
                ctx["assignedJobTitleId"] = assignedJob.id

                NodeExecutionResult(NodeExecutionStatus.SUCCESS, "UNDERSTAND: Domain terdeteksi '$inferredDomain', persona AI: '${assignedJob.jobName}' (${assignedJob.id}).")
            }
        )

        // Node 3: VALIDATE (data quality gate)
        engine.registerNode(
            GenericStepWorkflowNode("n3-validate", WorkflowNodeType.PLAN, nextNodeId = "n4-select") { ctx ->
                val dataRows = (ctx["dataRows"] as? List<*>)?.filterIsInstance<DataRow>() ?: emptyList()
                val validatedRows = dataRows.map { row ->
                    val totalCols = row.fields.size.coerceAtLeast(1)
                    val missingCount = row.fields.values.count { it.isBlank() }
                    val quality = ((totalCols - missingCount).toDouble() / totalCols * 100.0).coerceIn(0.0, 100.0)
                    row.copy(qualityScore = Math.round(quality * 100.0) / 100.0)
                }
                val avgQuality = if (validatedRows.isNotEmpty()) validatedRows.map { it.qualityScore }.average() else 100.0
                ctx["dataRows"] = validatedRows
                ctx["dataQualityScore"] = avgQuality

                if (avgQuality < 30.0) {
                    NodeExecutionResult(NodeExecutionStatus.FAILED, "VALIDATE Quality Gate Gagal: Skor kualitas data rata-rata $avgQuality% < 30%.")
                } else {
                    NodeExecutionResult(NodeExecutionStatus.SUCCESS, "VALIDATE: Quality Gate lulus dengan rata-rata kualitas ${"%.1f".format(avgQuality)}%.")
                }
            }
        )

        // Node 4: SELECT (terapkan kriteria)
        engine.registerNode(
            GenericStepWorkflowNode("n4-select", WorkflowNodeType.PLAN, nextNodeId = "n5-score") { ctx ->
                val tenantId = ctx["tenantId"]?.toString() ?: "tenant-enterprise-001"
                val promptText = ctx["promptText"]?.toString() ?: "Seleksi data terbaik"
                val selectionRequestId = ctx["selectionRequestId"]?.toString()
                val assignedJobTitle = ctx["assignedJobTitle"]?.toString() ?: "agent-procurement-specialist"
                val detectedFields = (ctx["detectedFields"] as? List<*>)?.filterIsInstance<DetectedField>() ?: emptyList()
                val dataRows = (ctx["dataRows"] as? List<*>)?.filterIsInstance<DataRow>() ?: emptyList()
                val availableCols = dataRows.firstOrNull()?.fields?.keys?.toList() ?: detectedFields.map { it.name }

                // Check pre-supplied criteria in context
                val explicitCriteria = (ctx["criteria"] as? List<*>)?.filterIsInstance<WeightedCriterion>()
                val weightedCriteria: List<WeightedCriterion> = if (!explicitCriteria.isNullOrEmpty()) {
                    explicitCriteria
                } else {
                    val reqRecord = selectionRequestId?.let { selectionRepo.getSelectionRequestById(it, tenantId) }
                    val calibrationSettingsId = reqRecord?.calibration_settings_id ?: ctx["calibrationSettingsId"]?.toString()

                    if (!calibrationSettingsId.isNullOrBlank()) {
                        val calibItems = selectionRepo.getCalibrationItems(calibrationSettingsId, tenantId)
                        if (calibItems.isNotEmpty()) {
                            val itemsList = calibItems.map { CalibrationItem(it.field_type_name, it.percentage) }
                            val validation = calibrationService.validateCalibrationAgainstDataset(itemsList, detectedFields)
                            if (!validation.isSuccess) {
                                val errorMsg = validation.warningMessage ?: "Kriteria kalibrasi tidak cocok dengan dataset"
                                return@GenericStepWorkflowNode NodeExecutionResult(NodeExecutionStatus.FAILED, errorMsg)
                            }
                            calibItems.map { c ->
                                val mapped = validation.mappedCriteria[c.field_type_name]
                                    ?: availableCols.find { it.equals(c.field_type_name, ignoreCase = true) }
                                    ?: c.field_type_name
                                val fieldType = detectedFields.find { it.name.equals(mapped, ignoreCase = true) }?.inferred_type?.lowercase() ?: "numeric"
                                val dType = when (fieldType) {
                                    "number", "currency" -> "numeric"
                                    "boolean" -> "boolean"
                                    else -> "text"
                                }
                                val dir = if (c.field_type_name.lowercase().let { it.contains("harga") || it.contains("biaya") || it.contains("cost") || it.contains("price") }) "lower_is_better" else "higher_is_better"
                                WeightedCriterion(
                                    criterionName = c.field_type_name,
                                    weightPercentage = c.percentage,
                                    source = "user_calibrated",
                                    dataType = dType,
                                    mappedField = mapped,
                                    direction = dir
                                )
                            }
                        } else {
                            buildDefaultAiCriteria(promptText, detectedFields, assignedJobTitle, availableCols, calibrationService)
                        }
                    } else {
                        buildDefaultAiCriteria(promptText, detectedFields, assignedJobTitle, availableCols, calibrationService)
                    }
                }

                if (selectionRequestId != null) {
                    val records = weightedCriteria.map { c ->
                        SelectionCriterionRecord(
                            selection_request_id = selectionRequestId,
                            criterion_name = c.criterionName,
                            criterion_source = c.source,
                            weight_percentage = c.weightPercentage,
                            is_user_calibrated = (c.source == "user_calibrated")
                        )
                    }
                    selectionRepo.insertCriteria(records, tenantId)
                }

                ctx["criteria"] = weightedCriteria
                NodeExecutionResult(NodeExecutionStatus.SUCCESS, "SELECT: Menerapkan ${weightedCriteria.size} kriteria evaluasi (total bobot ${weightedCriteria.sumOf { it.weightPercentage }}%).")
            }
        )

        // Node 5: SCORE
        engine.registerNode(
            GenericStepWorkflowNode("n5-score", WorkflowNodeType.LLM_GENERATE, nextNodeId = "n6-rank") { ctx ->
                val dataRows = (ctx["dataRows"] as? List<*>)?.filterIsInstance<DataRow>() ?: emptyList()
                val criteria = (ctx["criteria"] as? List<*>)?.filterIsInstance<WeightedCriterion>() ?: emptyList()

                val scoringResults = dataRows.map { row ->
                    scoreAndRankRowInternal(row, criteria, llm, riskEngine, confidenceEngine)
                }
                ctx["scoringResults"] = scoringResults
                NodeExecutionResult(NodeExecutionStatus.SUCCESS, "SCORE: Menilai ${scoringResults.size} baris dataset.")
            }
        )

        // Node 6: RANK
        engine.registerNode(
            GenericStepWorkflowNode("n6-rank", WorkflowNodeType.PLAN, nextNodeId = "n7-analyze") { ctx ->
                val scoringResults = (ctx["scoringResults"] as? List<*>)?.filterIsInstance<ScoringResult>() ?: emptyList()
                val rankedResults = rankAllResultsInternal(scoringResults)
                ctx["rankedResults"] = rankedResults

                val selectedCount = rankedResults.count { it.classification == "selected" }
                val reviewCount = rankedResults.count { it.classification == "review" }
                val rejectedCount = rankedResults.count { it.classification == "rejected" }
                NodeExecutionResult(NodeExecutionStatus.SUCCESS, "RANK: Menghasilkan peringkat #1-#${rankedResults.size} ($selectedCount selected, $reviewCount review, $rejectedCount rejected).")
            }
        )

        // Node 7: ANALYZE (Bagian G & Bagian F)
        engine.registerNode(
            GenericStepWorkflowNode("n7-analyze", WorkflowNodeType.LLM_GENERATE, nextNodeId = "n8-visualize") { ctx ->
                val ranked = (ctx["rankedResults"] as? List<*>)?.filterIsInstance<RankedResult>() ?: emptyList()
                val dataRows = (ctx["dataRows"] as? List<*>)?.filterIsInstance<DataRow>() ?: emptyList()
                val criteria = (ctx["criteria"] as? List<*>)?.filterIsInstance<WeightedCriterion>() ?: emptyList()
                val scores = ranked.map { it.result.totalScore }
                val scoredResults = ranked.map { it.result }

                val dist = analyticsEngine.calculateDistribution(scores)
                val corr = analyticsEngine.calculateCorrelationMatrix(dataRows, criteria, scores)
                val anomalies = analyticsEngine.detectAnomalies(dataRows, scoredResults)
                val trend = analyticsEngine.calculateTrend(dataRows, scoredResults)
                val kpi = analyticsEngine.calculateKpiSummary(ranked)

                ctx["distributionMetrics"] = mapOf(
                    "count" to dist.count,
                    "avgScore" to dist.mean,
                    "medianScore" to dist.median,
                    "minScore" to dist.min,
                    "maxScore" to dist.max,
                    "stdDev" to dist.stdDev,
                    "iqr" to dist.iqr,
                    "skewness" to dist.skewness
                )
                ctx["analyticsDistribution"] = dist
                ctx["analyticsCorrelation"] = corr
                ctx["analyticsAnomalies"] = anomalies
                ctx["analyticsTrend"] = trend
                ctx["analyticsKpi"] = kpi

                NodeExecutionResult(
                    NodeExecutionStatus.SUCCESS,
                    "ANALYZE: Analisis statistik selesai (Mean: ${dist.mean}, Median: ${dist.median}, StdDev: ${dist.stdDev}, IQR: ${dist.iqr}, Anomali terdeteksi: ${anomalies.outliers.size})."
                )
            }
        )

        // Node 8: VISUALIZE (Bagian H & Bagian F)
        engine.registerNode(
            GenericStepWorkflowNode("n8-visualize", WorkflowNodeType.PLAN, nextNodeId = "n9-recommend") { ctx ->
                val selectionRequestId = ctx["selectionRequestId"]?.toString() ?: "req-${UUID.randomUUID().toString().take(8)}"
                val tenantId = ctx["tenantId"]?.toString() ?: "tenant-enterprise-001"
                val ranked = (ctx["rankedResults"] as? List<*>)?.filterIsInstance<RankedResult>() ?: emptyList()
                val dataRows = (ctx["dataRows"] as? List<*>)?.filterIsInstance<DataRow>() ?: emptyList()
                val criteria = (ctx["criteria"] as? List<*>)?.filterIsInstance<WeightedCriterion>() ?: emptyList()

                val analyticsRecords = analyticsEngine.processFullSelectionAnalytics(
                    selectionRequestId = selectionRequestId,
                    tenantId = tenantId,
                    rows = dataRows,
                    criteria = criteria,
                    rankedResults = ranked
                )

                ctx["analyticsRecords"] = analyticsRecords
                analyticsRecords.firstOrNull()?.let { ctx["visualizationData"] = it }
                val chartTypes = analyticsRecords.joinToString(", ") { "${it.metric_type} -> ${it.suggested_chart_type}" }
                NodeExecutionResult(
                    NodeExecutionStatus.SUCCESS,
                    "VISUALIZE: ${analyticsRecords.size} visualisasi optimal berhasil ditentukan oleh AI ($chartTypes)."
                )
            }
        )

        // Node 9: RECOMMEND (Bagian I & LANGKAH 1 Grounded AI Insight Composer)
        engine.registerNode(
            GenericStepWorkflowNode("n9-recommend", WorkflowNodeType.LLM_GENERATE, nextNodeId = "n10-result") { ctx ->
                val ranked = (ctx["rankedResults"] as? List<*>)?.filterIsInstance<RankedResult>() ?: emptyList()
                val criteria = (ctx["criteria"] as? List<*>)?.filterIsInstance<WeightedCriterion>() ?: emptyList()
                val assignedJobTitle = ctx["assignedJobTitle"]?.toString() ?: "agent-procurement-specialist"

                val updatedRanked = ranked.map { r ->
                    val candidateName = r.result.row?.fields?.values?.firstOrNull() ?: "Item #${r.rankPosition}"
                    val rawInsight = llm.generateSelectionInsight(r, criteria)

                    // Grounding verification against source data
                    val sourceData = mutableMapOf<String, Any?>()
                    r.result.row?.fields?.forEach { (k, v) -> sourceData[k] = v }
                    r.result.scoreBreakdown.forEach { (crit, score) ->
                        val weighted = Math.round(score * (crit.weightPercentage / 100.0) * 100.0) / 100.0
                        sourceData[crit.criterionName] = score
                        sourceData["${crit.criterionName}_weighted"] = weighted
                    }
                    sourceData["total_score"] = r.result.totalScore
                    sourceData["rank_position"] = r.rankPosition
                    sourceData["risk_score"] = r.result.riskScore
                    sourceData["confidence_score"] = r.result.confidenceScore

                    val validation = outputValidator.validateGrounding(rawInsight, sourceData)
                    val groundedInsight = if (validation.isGrounded) {
                        rawInsight
                    } else {
                        outputValidator.sanitizeOrAnchorInsight(
                            originalInsight = rawInsight,
                            sourceData = sourceData,
                            candidateName = candidateName,
                            rankPosition = r.rankPosition,
                            totalScore = r.result.totalScore
                        )
                    }
                    r.copy(aiInsightText = groundedInsight)
                }
                ctx["rankedResults"] = updatedRanked
                NodeExecutionResult(NodeExecutionStatus.SUCCESS, "RECOMMEND: Rekomendasi strategis dan insight AI ter-grounding anti-halusinasi berhasil disematkan.")
            }
        )

        // Node 10: RESULT (simpan ke selection_results)
        engine.registerNode(
            GenericStepWorkflowNode("n10-result", WorkflowNodeType.DELIVER, nextNodeId = null) { ctx ->
                val selectionRequestId = ctx["selectionRequestId"]?.toString()
                val tenantId = ctx["tenantId"]?.toString() ?: "tenant-enterprise-001"
                val ranked = (ctx["rankedResults"] as? List<*>)?.filterIsInstance<RankedResult>() ?: emptyList()

                if (selectionRequestId != null) {
                    val resultRecords = ranked.map { r ->
                        val rowJson = buildJsonObject {
                            r.result.row?.fields?.forEach { (k, v) -> put(k, v) }
                        }
                        val breakdownJson = buildJsonObject {
                            r.result.scoreBreakdown.forEach { (criterion, score) ->
                                val weighted = Math.round(score * (criterion.weightPercentage / 100.0) * 100.0) / 100.0
                                put(criterion.criterionName, weighted)
                            }
                        }
                        SelectionResultRecord(
                            selection_request_id = selectionRequestId,
                            row_reference = rowJson,
                            total_score = r.result.totalScore,
                            rank_position = r.rankPosition,
                            priority_level = r.priorityLevel,
                            recommendation_classification = r.classification,
                            risk_score = r.result.riskScore,
                            confidence_score = r.result.confidenceScore,
                            score_breakdown = breakdownJson,
                            ai_insight_text = r.aiInsightText,
                            human_review_status = "pending_review"
                        )
                    }
                    selectionRepo.insertResults(resultRecords, tenantId)

                    val analyticsKpi = SelectionAnalyticsSummaryRecord(
                        selection_request_id = selectionRequestId,
                        metric_type = "kpi",
                        metric_data = buildJsonObject {
                            put("avg_score", ranked.map { it.result.totalScore }.average())
                            put("highest_score", ranked.maxOfOrNull { it.result.totalScore } ?: 0.0)
                            put("lowest_score", ranked.minOfOrNull { it.result.totalScore } ?: 0.0)
                        },
                        suggested_chart_type = "ranking_chart"
                    )
                    selectionRepo.insertAnalytics(listOf(analyticsKpi), tenantId)

                    selectionRepo.insertAuditLog(
                        ai.orchestree.backend.database.repositories.selection.SelectionAuditLogRecord(
                            selection_request_id = selectionRequestId,
                            action_type = "ai_selection_completed",
                            actor_type = "ai_agent",
                            detail = buildJsonObject {
                                put("workflow", "universal_ai_selection")
                                put("status", "completed")
                                put("candidates_evaluated", ranked.size)
                            }
                        ),
                        tenantId
                    )

                    selectionRepo.updateSelectionRequestStatus(
                        requestId = selectionRequestId,
                        status = "completed",
                        tenantId = tenantId,
                        domainCategory = ctx["inferredDomain"]?.toString(),
                        assignedJobTitleId = ctx["assignedJobTitleId"]?.toString()
                    )
                }
                val output = "RESULT: Berhasil memproses dan menyimpan ${ranked.size} hasil seleksi ke database."
                ctx["finalOutput"] = output
                NodeExecutionResult(NodeExecutionStatus.SUCCESS, output, data = mapOf("resultCount" to ranked.size))
            }
        )
    }

    suspend fun scoreAndRankRowInternal(
        row: DataRow,
        criteria: List<WeightedCriterion>,
        llm: LlmDatasetDetector,
        riskEngine: RiskEngine,
        confidenceEngine: ConfidenceEngine
    ): ScoringResult {
        val criterionScores = criteria.map { criterion ->
            val rawScore = when (criterion.dataType) {
                "numeric" -> normalizeNumericScore(row.getField(criterion.mappedField), criterion.direction)
                "text" -> llm.scoreTextualCriterion(row.getField(criterion.mappedField), criterion.fieldTypeName)
                "boolean" -> if (row.getField(criterion.mappedField) == true) 100.0 else 0.0
                else -> normalizeNumericScore(row.getField(criterion.mappedField), criterion.direction)
            }
            criterion to rawScore
        }

        val totalScore = criterionScores.sumOf { (criterion, score) -> score * (criterion.weightPercentage / 100.0) }
        val roundedTotal = Math.round(totalScore * 100.0) / 100.0
        val riskScore = riskEngine.evaluateSelectionRisk(row, criterionScores)
        val confidenceScore = confidenceEngine.evaluate(criterionScores, dataQualityScore = row.qualityScore)

        return ScoringResult(
            totalScore = roundedTotal,
            riskScore = riskScore,
            confidenceScore = confidenceScore,
            scoreBreakdown = criterionScores,
            row = row
        )
    }

    suspend fun rankAllResultsInternal(results: List<ScoringResult>): List<RankedResult> {
        return results.sortedByDescending { it.totalScore }
            .mapIndexed { index, result ->
                val priorityLevel = when {
                    index < results.size * 0.2 -> "high"     // top 20%
                    index < results.size * 0.6 -> "medium"
                    else -> "low"
                }
                val classification = when {
                    result.totalScore >= 75 && result.riskScore < 30 -> "selected"
                    result.totalScore in 50.0..74.9 -> "review"
                    else -> "rejected"
                }
                RankedResult(
                    rankPosition = index + 1,
                    priorityLevel = priorityLevel,
                    classification = classification,
                    result = result
                )
            }
    }

    private suspend fun buildDefaultAiCriteria(
        promptText: String,
        detectedFields: List<DetectedField>,
        assignedJobTitle: String,
        availableCols: List<String>,
        calibrationService: SelectionCalibrationService
    ): List<WeightedCriterion> {
        val aiCriteria = calibrationService.determineAiDefaultWeighting(
            promptText = promptText,
            detectedFields = detectedFields,
            agentSkillContext = assignedJobTitle
        )
        return aiCriteria.map { c ->
            val mapped = availableCols.find { it.contains(c.criterionName, ignoreCase = true) }
                ?: detectedFields.find { it.name.contains(c.criterionName, ignoreCase = true) }?.name
                ?: availableCols.firstOrNull()
                ?: c.criterionName
            val fieldType = detectedFields.find { it.name.equals(mapped, ignoreCase = true) }?.inferred_type?.lowercase() ?: "numeric"
            val dType = when (fieldType) {
                "number", "currency" -> "numeric"
                "boolean" -> "boolean"
                else -> "text"
            }
            val dir = if (c.criterionName.lowercase().let { it.contains("harga") || it.contains("biaya") || it.contains("cost") || it.contains("price") }) "lower_is_better" else "higher_is_better"
            WeightedCriterion(
                criterionName = c.criterionName,
                weightPercentage = c.weightPercentage,
                rationale = c.rationale,
                source = c.source,
                dataType = dType,
                mappedField = mapped,
                direction = dir
            )
        }
    }

    private fun inferJobTitleFromDomain(domain: String): String {
        return SelectionAiJobTitleRegistry.mapDomainToAiJobTitle(domain).jobName
    }

    /**
     * LANGKAH 4: Eksekusi rangkaian Workflow Node dengan Central Credit Ledger
     * Menggunakan siklus Reserve -> Execute -> Consume Actual -> Release Selisih.
     */
    suspend fun executeSelectionWorkflowWithCredit(
        engine: OrchestrationEngine,
        tenantId: String,
        context: MutableMap<String, Any>,
        nodeIds: List<String> = listOf(
            "n1-read-data", "n2-understand", "n3-validate", "n4-select", "n5-score",
            "n6-rank", "n7-analyze", "n8-visualize", "n9-recommend", "n10-result"
        )
    ): List<NodeResult> {
        val ledger = CentralCreditLedgerService.getInstance()
        val results = mutableListOf<NodeResult>()
        for (nodeId in nodeIds) {
            val node = engine.getNode(nodeId) ?: continue
            val res = ledger.executeSelectionNodeWithCredit(node, tenantId, context)
            results.add(res)
            if (res is NodeResult.Failed) {
                logger.warn("Workflow node $nodeId failed execution: ${res.errorMessage}")
                break
            }
        }
        return results
    }
}
