package ai.orchestree.backend.intelligence

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.selection.SelectionAnalyticsSummaryRecord
import ai.orchestree.backend.database.repositories.selection.SelectionRepository
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Data shape description for automatic chart selection.
 */
@Serializable
data class DataShapeDescription(
    val dimensionCount: Int,
    val primaryDataTypes: List<String>, // e.g. ["time-series", "numeric"], ["categorical", "numeric"]
    val dataPointCount: Int,
    val isTimeSeries: Boolean = false,
    val isRanking: Boolean = false,
    val isCorrelationMatrix: Boolean = false,
    val isDistributionBins: Boolean = false,
    val summary: String = ""
)

/**
 * Encapsulation of analytical data ready for visualization evaluation.
 */
@Serializable
data class AnalyticsMetric(
    val metricType: String, // 'distribution', 'trend', 'ranking', 'correlation', 'anomaly', 'kpi'
    val metricData: JsonElement,
    val shape: DataShapeDescription
) {
    fun describeShape(): DataShapeDescription = shape
}

/**
 * Recommendation produced by AI for the optimal chart type and rendered payload.
 */
@Serializable
data class ChartRecommendation(
    val suggestedChartType: String, // 'line', 'area', 'bar', 'ranking_chart', 'histogram', 'pie', 'scatter', 'heatmap', 'box_plot', 'metric_cards'
    val confidence: Double,
    val reasoning: String,
    val chartDataPayload: JsonElement
)

/**
 * LLM Interface for Visualization Recommendation.
 */
interface ChartRecommendationLlm {
    suspend fun recommendVisualizationType(
        dataShape: DataShapeDescription,
        analysisGoal: String
    ): ChartRecommendation
}

/**
 * Statistical Data Models for Generic Selection Analytics.
 */
@Serializable
data class HistogramBin(
    val binLabel: String,
    val minVal: Double,
    val maxVal: Double,
    val count: Int,
    val percentage: Double
)

@Serializable
data class DistributionAnalysisResult(
    val count: Int,
    val min: Double,
    val max: Double,
    val mean: Double,
    val median: Double,
    val variance: Double,
    val stdDev: Double,
    val q1: Double,
    val q3: Double,
    val iqr: Double,
    val skewness: Double,
    val bins: List<HistogramBin>
)

@Serializable
data class CorrelationMatrixResult(
    val variables: List<String>,
    val matrix: Map<String, Map<String, Double>>,
    val criterionOutcomeCorrelations: Map<String, Double>
)

@Serializable
data class StatisticalOutlier(
    val entityId: String,
    val entityLabel: String,
    val score: Double,
    val zScore: Double,
    val iqrDeviation: Double,
    val detectionMethod: String, // 'Z_SCORE', 'IQR', 'BOTH'
    val reason: String
)

@Serializable
data class StatisticalAnomalyResult(
    val totalEvaluated: Int,
    val zScoreThreshold: Double = 2.0,
    val iqrMultiplier: Double = 1.5,
    val lowerFence: Double,
    val upperFence: Double,
    val outliers: List<StatisticalOutlier>
)

@Serializable
data class TimeSeriesPoint(
    val date: String,
    val value: Double,
    val movingAvg: Double? = null,
    val label: String? = null
)

@Serializable
data class TrendAnalysisResult(
    val hasTimeSeries: Boolean,
    val timeField: String?,
    val dataPoints: List<TimeSeriesPoint>,
    val direction: String, // 'UPWARD', 'DOWNWARD', 'STABLE'
    val slope: Double,
    val percentageChange: Double
)

@Serializable
data class KpiSummaryResult(
    val totalCandidates: Int,
    val selectedCount: Int,
    val selectedRate: Double,
    val reviewCount: Int,
    val reviewRate: Double,
    val rejectedCount: Int,
    val rejectedRate: Double,
    val highPriorityCount: Int,
    val mediumPriorityCount: Int,
    val lowPriorityCount: Int,
    val avgScore: Double,
    val avgRiskScore: Double,
    val avgConfidenceScore: Double
)

/**
 * Concrete LLM Implementation for Automatic Chart Type Selection via Model Router.
 */
class LlmChartSelector(
    private val modelRouter: ModelRouter = ModelRouter()
) : ChartRecommendationLlm {

    private val logger = LoggerFactory.getLogger(LlmChartSelector::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun recommendVisualizationType(
        dataShape: DataShapeDescription,
        analysisGoal: String
    ): ChartRecommendation = withContext(Dispatchers.IO) {
        val prompt = """
            Analyze the following dataset characteristics and analysis goal to determine the single most effective, modern dashboard chart type:
            
            Analysis Goal: $analysisGoal
            Dimension Count: ${dataShape.dimensionCount}
            Primary Data Types: ${dataShape.primaryDataTypes.joinToString(", ")}
            Data Point Count: ${dataShape.dataPointCount}
            Is Time-Series: ${dataShape.isTimeSeries}
            Is Ranking Comparison: ${dataShape.isRanking}
            Has Correlation Matrix: ${dataShape.isCorrelationMatrix}
            Has Distribution Bins: ${dataShape.isDistributionBins}
            Data Summary: ${dataShape.summary}
            
            CHART MAPPING CRITERIA:
            - Time-Series / Chronological sequence (dates/periods) -> 'line' or 'area'
            - Ranking / Entity-by-entity performance comparisons -> 'bar' or 'ranking_chart'
            - Frequency Distribution / Continuous score spreads -> 'histogram' or 'box_plot'
            - Multi-variable Correlation Matrix -> 'heatmap' or 'scatter'
            - Anomaly / Outlier Distribution -> 'scatter' or 'box_plot'
            - KPI Aggregations / High-level status -> 'metric_cards' or 'pie'
            
            Respond strictly with valid JSON conforming to this schema:
            {
              "suggested_chart_type": "<line|area|bar|ranking_chart|histogram|pie|scatter|heatmap|box_plot|metric_cards>",
              "confidence": 0.95,
              "reasoning": "<concise analytical explanation why this chart type is optimal for this exact data shape>"
            }
        """.trimIndent()

        val routeRequest = ModelRouteRequest(
            taskCategory = "STRUCTURED_REASONING",
            prompt = prompt,
            systemInstruction = "You are Orchestree AI Visualization Architect. Recommend optimal visual chart representations strictly based on dataset dimensions, time-series presence, distribution bins, and statistical correlation structures.",
            responseFormatJson = true,
            temperature = 0.2
        )

        try {
            val responseResult = modelRouter.execute(routeRequest)
            if (responseResult.isSuccess) {
                val rawText = responseResult.getOrThrow().text
                val parsed = parseLlmRecommendation(rawText, analysisGoal, dataShape)
                if (parsed != null) {
                    return@withContext parsed
                }
            }
        } catch (e: Exception) {
            logger.warn("ModelRouter execution for chart selection failed: ${e.message}")
        }

        // Robust deterministic rule-based resolution adhering to data characteristics
        buildDeterministicChartRecommendation(dataShape, analysisGoal)
    }

    private fun parseLlmRecommendation(
        rawText: String,
        analysisGoal: String,
        dataShape: DataShapeDescription
    ): ChartRecommendation? {
        return try {
            val cleanJson = rawText.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
            val element = json.parseToJsonElement(cleanJson).jsonObject
            val chartType = element["suggested_chart_type"]?.jsonPrimitive?.content ?: return null
            val confidence = element["confidence"]?.jsonPrimitive?.doubleOrNull ?: 0.92
            val reasoning = element["reasoning"]?.jsonPrimitive?.content ?: "Optimal visualization selected for $analysisGoal."

            ChartRecommendation(
                suggestedChartType = chartType,
                confidence = confidence,
                reasoning = reasoning,
                chartDataPayload = buildChartDataPayload(chartType, dataShape, analysisGoal)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun buildDeterministicChartRecommendation(
        dataShape: DataShapeDescription,
        analysisGoal: String
    ): ChartRecommendation {
        val (chartType, reasoning) = when {
            dataShape.isTimeSeries || analysisGoal.equals("trend", ignoreCase = true) -> {
                "line" to "Data memiliki karakteristik deret waktu berurutan (time-series), sehingga grafik garis (line chart) optimal untuk memvisualisasikan tren dan volatilitas metrik."
            }
            dataShape.isRanking || analysisGoal.equals("ranking", ignoreCase = true) -> {
                "bar" to "Data merepresentasikan peringkat dan komparasi skor antar entitas/kandidat, sehingga diagram batang (bar/ranking chart) memberikan kontras perbandingan yang paling jelas."
            }
            dataShape.isCorrelationMatrix || analysisGoal.equals("correlation", ignoreCase = true) -> {
                "heatmap" to "Data berisi matriks korelasi multi-variabel antar kriteria evaluasi, sehingga visualisasi heatmap matrix adalah representasi optimal untuk mengidentifikasi derajat kovarian."
            }
            analysisGoal.equals("anomaly", ignoreCase = true) -> {
                "scatter" to "Data mencakup sebaran titik observasi dengan deteksi outlier statistik Z-Score/IQR, sehingga diagram sebar (scatter plot) dengan penandaan anomali adalah yang paling efektif."
            }
            dataShape.isDistributionBins || analysisGoal.equals("distribution", ignoreCase = true) -> {
                "histogram" to "Data mencakup sebaran frekuensi skor continuous yang dikelompokkan ke dalam interval/bin, sehingga histogram optimal untuk memperlihatkan skewness dan normalitas data."
            }
            analysisGoal.equals("kpi", ignoreCase = true) -> {
                "metric_cards" to "Data merupakan ringkasan agregasi KPI eksekutif, optimal disajikan dalam kartu metrik ringkasan (metric cards) dan visualisasi komposisi."
            }
            else -> {
                "bar" to "Karakteristik data kategorikal dan nilai numerik paling sesuai disajikan dengan diagram batang."
            }
        }

        return ChartRecommendation(
            suggestedChartType = chartType,
            confidence = 0.94,
            reasoning = reasoning,
            chartDataPayload = buildChartDataPayload(chartType, dataShape, analysisGoal)
        )
    }

    private fun buildChartDataPayload(
        chartType: String,
        dataShape: DataShapeDescription,
        analysisGoal: String
    ): JsonElement {
        return buildJsonObject {
            put("chart_type", chartType)
            put("analysis_goal", analysisGoal)
            put("dimension_count", dataShape.dimensionCount)
            put("data_point_count", dataShape.dataPointCount)
            put("ready_for_render", true)
        }
    }
}

/**
 * Universal Analytics Engine for KPI, Distribution, Trend, Correlation, and Anomaly Analysis.
 * Extends PRD Master Section 9 principles for generic structured selection datasets.
 */
class SelectionAnalyticsEngine(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val modelRouter: ModelRouter = ModelRouter(),
    private val selectionRepo: SelectionRepository = SelectionRepository(supabase, modelRouter),
    val llm: ChartRecommendationLlm = LlmChartSelector(modelRouter)
) {
    private val logger = LoggerFactory.getLogger(SelectionAnalyticsEngine::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Entry point required by user specification:
     * Automatic Chart Type Selection where AI dynamically chooses optimal visual presentation.
     */
    suspend fun selectOptimalChartType(analyticsData: AnalyticsMetric): ChartRecommendation {
        val rec = llm.recommendVisualizationType(
            dataShape = analyticsData.describeShape(),
            analysisGoal = analyticsData.metricType
        )

        // Enrich the chartDataPayload with actual metric data values
        val enrichedPayload = buildJsonObject {
            put("chart_type", rec.suggestedChartType)
            put("confidence", rec.confidence)
            put("reasoning", rec.reasoning)
            put("metric_type", analyticsData.metricType)
            put("metric_data", analyticsData.metricData)
            put("shape", buildJsonObject {
                put("dimension_count", analyticsData.shape.dimensionCount)
                put("data_point_count", analyticsData.shape.dataPointCount)
                put("is_time_series", analyticsData.shape.isTimeSeries)
                put("is_ranking", analyticsData.shape.isRanking)
                put("summary", analyticsData.shape.summary)
            })
        }

        return rec.copy(chartDataPayload = enrichedPayload)
    }

    /**
     * 1. Distribution Calculation:
     * Computes Mean, Median, Variance, StdDev, Q1, Q3, IQR, Skewness, and Histogram Frequency Bins.
     */
    fun calculateDistribution(scores: List<Double>): DistributionAnalysisResult {
        if (scores.isEmpty()) {
            return DistributionAnalysisResult(
                count = 0, min = 0.0, max = 0.0, mean = 0.0, median = 0.0,
                variance = 0.0, stdDev = 0.0, q1 = 0.0, q3 = 0.0, iqr = 0.0,
                skewness = 0.0, bins = emptyList()
            )
        }

        val sorted = scores.sorted()
        val count = sorted.size
        val min = sorted.first()
        val max = sorted.last()
        val mean = sorted.average()

        val median = if (count % 2 == 1) {
            sorted[count / 2]
        } else {
            (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
        }

        val variance = if (count > 1) {
            sorted.map { (it - mean) * (it - mean) }.sum() / (count - 1)
        } else 0.0
        val stdDev = Math.sqrt(variance)

        val q1 = percentile(sorted, 0.25)
        val q3 = percentile(sorted, 0.75)
        val iqr = Math.max(0.0, q3 - q1)

        val skewness = if (count > 2 && stdDev > 0.0001) {
            val sumCubed = sorted.sumOf { Math.pow((it - mean) / stdDev, 3.0) }
            (count.toDouble() / ((count - 1) * (count - 2))) * sumCubed
        } else 0.0

        // Build 5 histogram bins
        val binCount = 5
        val binWidth = if (max > min) (max - min) / binCount else 20.0
        val bins = mutableListOf<HistogramBin>()
        for (i in 0 until binCount) {
            val bMin = min + (i * binWidth)
            val bMax = if (i == binCount - 1) max + 0.0001 else min + ((i + 1) * binWidth)
            val inBin = sorted.count { it >= bMin && it < bMax }
            val pct = Math.round((inBin.toDouble() / count * 100.0) * 10.0) / 10.0
            val label = "${Math.round(bMin * 10.0) / 10.0} - ${Math.round(bMax * 10.0) / 10.0}"
            bins.add(HistogramBin(label, Math.round(bMin * 10.0) / 10.0, Math.round(bMax * 10.0) / 10.0, inBin, pct))
        }

        return DistributionAnalysisResult(
            count = count,
            min = Math.round(min * 100.0) / 100.0,
            max = Math.round(max * 100.0) / 100.0,
            mean = Math.round(mean * 100.0) / 100.0,
            median = Math.round(median * 100.0) / 100.0,
            variance = Math.round(variance * 100.0) / 100.0,
            stdDev = Math.round(stdDev * 100.0) / 100.0,
            q1 = Math.round(q1 * 100.0) / 100.0,
            q3 = Math.round(q3 * 100.0) / 100.0,
            iqr = Math.round(iqr * 100.0) / 100.0,
            skewness = Math.round(skewness * 100.0) / 100.0,
            bins = bins
        )
    }

    /**
     * 2. Correlation Matrix Calculation:
     * Calculates Pearson correlation coefficients across criteria variables and total scores.
     */
    fun calculateCorrelationMatrix(
        dataRows: List<DataRow>,
        criteria: List<WeightedCriterion>,
        totalScores: List<Double>
    ): CorrelationMatrixResult {
        val numericCriteria = criteria.map { it.criterionName }
        val matrix = mutableMapOf<String, MutableMap<String, Double>>()

        // Extract series for each criterion
        val seriesMap = mutableMapOf<String, List<Double>>()
        for (crit in criteria) {
            val key = crit.mappedField.ifBlank { crit.criterionName }
            val values = dataRows.map { row ->
                val fieldVal = row.getField(key)
                when (fieldVal) {
                    is Number -> fieldVal.toDouble()
                    is Boolean -> if (fieldVal) 100.0 else 0.0
                    else -> 50.0
                }
            }
            seriesMap[crit.criterionName] = values
        }

        for (v1 in numericCriteria) {
            matrix[v1] = mutableMapOf()
            val s1 = seriesMap[v1] ?: emptyList()
            for (v2 in numericCriteria) {
                if (v1 == v2) {
                    matrix[v1]!![v2] = 1.0
                } else {
                    val s2 = seriesMap[v2] ?: emptyList()
                    val r = pearsonCorrelation(s1, s2)
                    matrix[v1]!![v2] = Math.round(r * 100.0) / 100.0
                }
            }
        }

        // Correlation of each criterion with total score outcome
        val outcomeCorrelations = mutableMapOf<String, Double>()
        for (v in numericCriteria) {
            val s = seriesMap[v] ?: emptyList()
            val r = pearsonCorrelation(s, totalScores)
            outcomeCorrelations[v] = Math.round(r * 100.0) / 100.0
        }

        return CorrelationMatrixResult(
            variables = numericCriteria,
            matrix = matrix,
            criterionOutcomeCorrelations = outcomeCorrelations
        )
    }

    /**
     * 3. Statistical Anomaly Detection:
     * Real statistical outlier detection using both Z-Score (Z >= 2.0) and Tukey's IQR Fences (1.5 * IQR).
     */
    fun detectAnomalies(
        rows: List<DataRow>,
        scoredResults: List<ScoringResult>,
        zScoreThreshold: Double = 2.0,
        iqrMultiplier: Double = 1.5
    ): StatisticalAnomalyResult {
        if (scoredResults.isEmpty()) {
            return StatisticalAnomalyResult(
                totalEvaluated = 0,
                lowerFence = 0.0,
                upperFence = 0.0,
                outliers = emptyList()
            )
        }

        val scores = scoredResults.map { it.totalScore }
        val dist = calculateDistribution(scores)
        val mean = dist.mean
        val stdDev = dist.stdDev
        val q1 = dist.q1
        val q3 = dist.q3
        val iqr = dist.iqr

        val lowerFence = Math.round((q1 - (iqrMultiplier * iqr)) * 100.0) / 100.0
        val upperFence = Math.round((q3 + (iqrMultiplier * iqr)) * 100.0) / 100.0

        val outliers = mutableListOf<StatisticalOutlier>()

        scoredResults.forEachIndexed { idx, res ->
            val row = res.row ?: (if (idx < rows.size) rows[idx] else null)
            val score = res.totalScore
            val entityLabel = row?.fields?.values?.firstOrNull() ?: "Entity #$idx"
            val rowId = row?.id ?: "row-$idx"

            val zScore = if (stdDev > 0.0001) Math.abs(score - mean) / stdDev else 0.0
            val isZOutlier = zScore >= zScoreThreshold

            val isIqrOutlier = score < lowerFence || score > upperFence
            val iqrDeviation = if (iqr > 0.0001) Math.abs(score - dist.median) / iqr else 0.0

            if (isZOutlier || isIqrOutlier) {
                val method = when {
                    isZOutlier && isIqrOutlier -> "BOTH"
                    isZOutlier -> "Z_SCORE"
                    else -> "IQR"
                }
                val reason = when {
                    score > upperFence -> "Skor ($score) secara signifikan melampaui batas atas IQR ($upperFence) dengan deviasi Z-score ${Math.round(zScore * 100.0) / 100.0}σ."
                    score < lowerFence -> "Skor ($score) anomali rendah di bawah batas bawah IQR ($lowerFence) dengan deviasi Z-score ${Math.round(zScore * 100.0) / 100.0}σ."
                    else -> "Skor ($score) menyimpang ${Math.round(zScore * 100.0) / 100.0} deviasi standar dari rata-rata populasi ($mean)."
                }

                outliers.add(
                    StatisticalOutlier(
                        entityId = rowId,
                        entityLabel = entityLabel,
                        score = score,
                        zScore = Math.round(zScore * 100.0) / 100.0,
                        iqrDeviation = Math.round(iqrDeviation * 100.0) / 100.0,
                        detectionMethod = method,
                        reason = reason
                    )
                )
            }
        }

        return StatisticalAnomalyResult(
            totalEvaluated = scoredResults.size,
            zScoreThreshold = zScoreThreshold,
            iqrMultiplier = iqrMultiplier,
            lowerFence = lowerFence,
            upperFence = upperFence,
            outliers = outliers
        )
    }

    /**
     * 4. Trend Analysis:
     * Identifies chronological progression, moving averages, and direction if time fields exist.
     */
    fun calculateTrend(
        rows: List<DataRow>,
        scoredResults: List<ScoringResult>
    ): TrendAnalysisResult {
        if (rows.isEmpty() || scoredResults.isEmpty()) {
            return TrendAnalysisResult(
                hasTimeSeries = false,
                timeField = null,
                dataPoints = emptyList(),
                direction = "STABLE",
                slope = 0.0,
                percentageChange = 0.0
            )
        }

        // Search for date or chronological timestamp field
        val sampleRow = rows.first()
        val timeCandidates = listOf("tanggal", "date", "period", "periode", "timestamp", "created_at", "bulan", "waktu", "month")
        val detectedField = sampleRow.fields.keys.firstOrNull { key ->
            timeCandidates.any { key.contains(it, ignoreCase = true) }
        }

        if (detectedField == null) {
            return TrendAnalysisResult(
                hasTimeSeries = false,
                timeField = null,
                dataPoints = emptyList(),
                direction = "STABLE",
                slope = 0.0,
                percentageChange = 0.0
            )
        }

        // Pair chronological entries
        val paired = rows.mapIndexedNotNull { idx, row ->
            val dateStr = row.fields[detectedField] ?: return@mapIndexedNotNull null
            val score = if (idx < scoredResults.size) scoredResults[idx].totalScore else 0.0
            val label = row.fields.values.firstOrNull() ?: "Item #$idx"
            Triple(dateStr, score, label)
        }.sortedBy { it.first }

        if (paired.size < 2) {
            return TrendAnalysisResult(
                hasTimeSeries = true,
                timeField = detectedField,
                dataPoints = paired.map { TimeSeriesPoint(it.first, it.second, it.second, it.third) },
                direction = "STABLE",
                slope = 0.0,
                percentageChange = 0.0
            )
        }

        // Calculate moving average with window size 3
        val window = 3
        val points = paired.mapIndexed { i, item ->
            val start = Math.max(0, i - window + 1)
            val sub = paired.subList(start, i + 1)
            val mAvg = sub.map { it.second }.average()
            TimeSeriesPoint(
                date = item.first,
                value = item.second,
                movingAvg = Math.round(mAvg * 100.0) / 100.0,
                label = item.third
            )
        }

        // Linear slope
        val n = points.size.toDouble()
        val sumX = (0 until points.size).sum().toDouble()
        val sumY = points.sumOf { it.value }
        val sumXY = points.mapIndexed { idx, pt -> idx.toDouble() * pt.value }.sum()
        val sumXX = (0 until points.size).sumOf { it.toDouble() * it.toDouble() }

        val slope = if ((n * sumXX - sumX * sumX) != 0.0) {
            (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
        } else 0.0

        val firstVal = points.first().value
        val lastVal = points.last().value
        val pctChange = if (firstVal > 0.0001) {
            ((lastVal - firstVal) / firstVal) * 100.0
        } else 0.0

        val direction = when {
            slope > 0.15 -> "UPWARD"
            slope < -0.15 -> "DOWNWARD"
            else -> "STABLE"
        }

        return TrendAnalysisResult(
            hasTimeSeries = true,
            timeField = detectedField,
            dataPoints = points,
            direction = direction,
            slope = Math.round(slope * 1000.0) / 1000.0,
            percentageChange = Math.round(pctChange * 10.0) / 10.0
        )
    }

    /**
     * 5. KPI Summary Calculation:
     * High-level executive aggregates for selection decisions.
     */
    fun calculateKpiSummary(rankedResults: List<RankedResult>): KpiSummaryResult {
        val total = rankedResults.size
        if (total == 0) {
            return KpiSummaryResult(
                totalCandidates = 0, selectedCount = 0, selectedRate = 0.0,
                reviewCount = 0, reviewRate = 0.0, rejectedCount = 0, rejectedRate = 0.0,
                highPriorityCount = 0, mediumPriorityCount = 0, lowPriorityCount = 0,
                avgScore = 0.0, avgRiskScore = 0.0, avgConfidenceScore = 0.0
            )
        }

        val selected = rankedResults.count { it.classification == "selected" }
        val review = rankedResults.count { it.classification == "review" }
        val rejected = rankedResults.count { it.classification == "rejected" }

        val highPri = rankedResults.count { it.priorityLevel == "high" }
        val medPri = rankedResults.count { it.priorityLevel == "medium" }
        val lowPri = rankedResults.count { it.priorityLevel == "low" }

        val avgScore = rankedResults.map { it.result.totalScore }.average()
        val avgRisk = rankedResults.map { it.result.riskScore }.average()
        val avgConf = rankedResults.map { it.result.confidenceScore }.average()

        return KpiSummaryResult(
            totalCandidates = total,
            selectedCount = selected,
            selectedRate = Math.round((selected.toDouble() / total * 100.0) * 10.0) / 10.0,
            reviewCount = review,
            reviewRate = Math.round((review.toDouble() / total * 100.0) * 10.0) / 10.0,
            rejectedCount = rejected,
            rejectedRate = Math.round((rejected.toDouble() / total * 100.0) * 10.0) / 10.0,
            highPriorityCount = highPri,
            mediumPriorityCount = medPri,
            lowPriorityCount = lowPri,
            avgScore = Math.round(avgScore * 100.0) / 100.0,
            avgRiskScore = Math.round(avgRisk * 100.0) / 100.0,
            avgConfidenceScore = Math.round(avgConf * 100.0) / 100.0
        )
    }

    /**
     * Complete Workflow Execution:
     * Analyzes dataset, produces all metrics, obtains AI recommendations for each metric,
     * persists them to Supabase selection_analytics_summary, and deducts credits from Central Credit Ledger.
     */
    suspend fun processFullSelectionAnalytics(
        selectionRequestId: String,
        tenantId: String,
        rows: List<DataRow>,
        criteria: List<WeightedCriterion>,
        rankedResults: List<RankedResult>
    ): List<SelectionAnalyticsSummaryRecord> = withContext(Dispatchers.IO) {
        val scoredResults = rankedResults.map { it.result }
        val totalScores = scoredResults.map { it.totalScore }

        val distribution = calculateDistribution(totalScores)
        val correlation = calculateCorrelationMatrix(rows, criteria, totalScores)
        val anomaly = detectAnomalies(rows, scoredResults)
        val trend = calculateTrend(rows, scoredResults)
        val kpi = calculateKpiSummary(rankedResults)

        val metricsToEvaluate = mutableListOf<AnalyticsMetric>()

        // 1. KPI Metric
        metricsToEvaluate.add(
            AnalyticsMetric(
                metricType = "kpi",
                metricData = buildJsonObject {
                    put("total_candidates", kpi.totalCandidates)
                    put("selected_count", kpi.selectedCount)
                    put("selected_rate", kpi.selectedRate)
                    put("review_count", kpi.reviewCount)
                    put("rejected_count", kpi.rejectedCount)
                    put("high_priority_count", kpi.highPriorityCount)
                    put("avg_score", kpi.avgScore)
                    put("avg_risk_score", kpi.avgRiskScore)
                    put("avg_confidence_score", kpi.avgConfidenceScore)
                },
                shape = DataShapeDescription(
                    dimensionCount = 1,
                    primaryDataTypes = listOf("numeric", "categorical"),
                    dataPointCount = kpi.totalCandidates,
                    isRanking = false,
                    summary = "High-level aggregated key performance indicators for selection candidates"
                )
            )
        )

        // 2. Ranking Metric
        metricsToEvaluate.add(
            AnalyticsMetric(
                metricType = "ranking",
                metricData = buildJsonObject {
                    put("top_candidates", buildJsonArray {
                        rankedResults.take(15).forEach { r ->
                            val label = r.result.row?.fields?.values?.firstOrNull() ?: "Candidate #${r.rankPosition}"
                            add(buildJsonObject {
                                put("rank", r.rankPosition)
                                put("label", label)
                                put("score", r.result.totalScore)
                                put("priority", r.priorityLevel)
                                put("classification", r.classification)
                            })
                        }
                    })
                },
                shape = DataShapeDescription(
                    dimensionCount = 2,
                    primaryDataTypes = listOf("categorical", "numeric"),
                    dataPointCount = Math.min(rankedResults.size, 15),
                    isRanking = true,
                    summary = "Individual candidate score rankings for categorical comparative evaluation"
                )
            )
        )

        // 3. Distribution Metric
        metricsToEvaluate.add(
            AnalyticsMetric(
                metricType = "distribution",
                metricData = buildJsonObject {
                    put("mean", distribution.mean)
                    put("median", distribution.median)
                    put("std_dev", distribution.stdDev)
                    put("min", distribution.min)
                    put("max", distribution.max)
                    put("iqr", distribution.iqr)
                    put("skewness", distribution.skewness)
                    put("bins", buildJsonArray {
                        distribution.bins.forEach { b ->
                            add(buildJsonObject {
                                put("bin_label", b.binLabel)
                                put("count", b.count)
                                put("percentage", b.percentage)
                            })
                        }
                    })
                },
                shape = DataShapeDescription(
                    dimensionCount = 2,
                    primaryDataTypes = listOf("numeric", "continuous"),
                    dataPointCount = distribution.bins.size,
                    isDistributionBins = true,
                    summary = "Continuous score distribution split into histogram frequency bins"
                )
            )
        )

        // 4. Correlation Metric
        if (criteria.size >= 2) {
            metricsToEvaluate.add(
                AnalyticsMetric(
                    metricType = "correlation",
                    metricData = buildJsonObject {
                        put("variables", buildJsonArray { correlation.variables.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
                        put("outcome_correlations", buildJsonObject {
                            correlation.criterionOutcomeCorrelations.forEach { (k, v) -> put(k, v) }
                        })
                    },
                    shape = DataShapeDescription(
                        dimensionCount = correlation.variables.size,
                        primaryDataTypes = listOf("numeric", "matrix"),
                        dataPointCount = correlation.variables.size * correlation.variables.size,
                        isCorrelationMatrix = true,
                        summary = "Multi-variable Pearson correlation matrix between evaluation criteria"
                    )
                )
            )
        }

        // 5. Anomaly Metric
        metricsToEvaluate.add(
            AnalyticsMetric(
                metricType = "anomaly",
                metricData = buildJsonObject {
                    put("outlier_count", anomaly.outliers.size)
                    put("lower_fence", anomaly.lowerFence)
                    put("upperFence", anomaly.upperFence)
                    put("outliers", buildJsonArray {
                        anomaly.outliers.forEach { o ->
                            add(buildJsonObject {
                                put("entity_id", o.entityId)
                                put("label", o.entityLabel)
                                put("score", o.score)
                                put("z_score", o.zScore)
                                put("method", o.detectionMethod)
                                put("reason", o.reason)
                            })
                        }
                    })
                },
                shape = DataShapeDescription(
                    dimensionCount = 2,
                    primaryDataTypes = listOf("numeric", "categorical"),
                    dataPointCount = anomaly.outliers.size,
                    summary = "Statistical outliers flagged by Z-score and Tukey IQR fences"
                )
            )
        )

        // 6. Trend Metric (if dataset is time-series)
        if (trend.hasTimeSeries) {
            metricsToEvaluate.add(
                AnalyticsMetric(
                    metricType = "trend",
                    metricData = buildJsonObject {
                        put("time_field", trend.timeField ?: "")
                        put("direction", trend.direction)
                        put("slope", trend.slope)
                        put("percentage_change", trend.percentageChange)
                        put("points", buildJsonArray {
                            trend.dataPoints.take(20).forEach { pt ->
                                add(buildJsonObject {
                                    put("date", pt.date)
                                    put("value", pt.value)
                                    pt.movingAvg?.let { put("moving_avg", it) }
                                })
                            }
                        })
                    },
                    shape = DataShapeDescription(
                        dimensionCount = 2,
                        primaryDataTypes = listOf("time-series", "numeric"),
                        dataPointCount = trend.dataPoints.size,
                        isTimeSeries = true,
                        summary = "Chronological time series tracking performance score progression"
                    )
                )
            )
        }

        // Evaluate AI Chart Recommendations for each metric
        val recordsToPersist = mutableListOf<SelectionAnalyticsSummaryRecord>()
        for (metric in metricsToEvaluate) {
            val recommendation = selectOptimalChartType(metric)
            recordsToPersist.add(
                SelectionAnalyticsSummaryRecord(
                    selection_request_id = selectionRequestId,
                    metric_type = metric.metricType,
                    metric_data = metric.metricData,
                    suggested_chart_type = recommendation.suggestedChartType,
                    chart_data_payload = recommendation.chartDataPayload
                )
            )
        }

        // Persist records to Supabase selection_analytics_summary
        selectionRepo.insertAnalytics(recordsToPersist, tenantId)

        // Deduct Central Credit Ledger
        ai.orchestree.backend.database.repositories.analytics.AnalyticsRepository.instance.recordCreditUsage(
            tenantId = tenantId,
            channelAccountId = "system-analytics-engine",
            creditDeducted = (metricsToEvaluate.size * 0.25).coerceAtLeast(1.0)
        )

        recordsToPersist
    }

    private fun percentile(sortedList: List<Double>, p: Double): Double {
        if (sortedList.isEmpty()) return 0.0
        val pos = p * (sortedList.size - 1)
        val low = Math.floor(pos).toInt()
        val high = Math.ceil(pos).toInt()
        val weight = pos - low
        return (1.0 - weight) * sortedList[low] + weight * sortedList[high]
    }

    private fun pearsonCorrelation(xs: List<Double>, ys: List<Double>): Double {
        if (xs.size != ys.size || xs.size < 2) return 0.0
        val xMean = xs.average()
        val yMean = ys.average()

        var numerator = 0.0
        var xDenom = 0.0
        var yDenom = 0.0

        for (i in xs.indices) {
            val dx = xs[i] - xMean
            val dy = ys[i] - yMean
            numerator += dx * dy
            xDenom += dx * dx
            yDenom += dy * dy
        }

        val denominator = Math.sqrt(xDenom * yDenom)
        return if (denominator > 0.00001) numerator / denominator else 0.0
    }
}
