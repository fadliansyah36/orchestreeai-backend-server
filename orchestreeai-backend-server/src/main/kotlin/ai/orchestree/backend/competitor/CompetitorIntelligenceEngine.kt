package ai.orchestree.backend.competitor

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.UUID

object CompetitorIntelligenceEngine {
    private val logger = LoggerFactory.getLogger(CompetitorIntelligenceEngine::class.java)
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Inspects target URL, analyzes shifts with ModelRouter, and generates structured insight.
     */
    suspend fun analyzeCompetitorTarget(
        target: CompetitorTarget,
        rawWebText: String? = null,
        modelRouter: ModelRouter = ModelRouter(),
        supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
    ): CompetitorInsight = withContext(Dispatchers.IO) {
        val adapter = WebIngestionAdapter()
        val ingestionResult = if (rawWebText == null) {
            adapter.ingest(target.url)
        } else null

        val contentSample = when (ingestionResult) {
            is IngestionResult.Success -> ingestionResult.parsedContent
            is IngestionResult.Failure -> "Gagal mengambil data langsung: ${ingestionResult.reason}"
            else -> rawWebText ?: "Target URL: ${target.url}"
        }

        val prompt = """
            Anda adalah OrchestreeAI Competitor Intelligence Agent.
            Analisis data publik aktual dari target kompetitor berikut:
            Target: ${target.name}
            Kategori: ${target.category}
            URL: ${target.url}
            Preferensi Insight: ${target.insightPrefs}
            Konten Terkini: ${contentSample.take(2000)}
            
            Hasilkan JSON murni tanpa markdown dengan schema berikut:
            {
              "category": "PRODUCT_NEW" | "FEATURE_UPDATE" | "CONTENT_POST" | "PROMO_MARKETING" | "PRICE_CHANGE" | "REVIEW_SENTIMENT" | "OTHER",
              "summary": "Ringkasan tajam 1 kalimat tentang apa yang dilakukan kompetitor",
              "narrative": "Penjelasan detail dampak bisnis dan perbandingannya",
              "confidence": 0.85,
              "impactScore": 0.90,
              "recommendedAction": "Rekomendasi aksi taktis yang konkret untuk tenant",
              "importance": "HIGH" | "MEDIUM" | "LOW"
            }
        """.trimIndent()

        var insightCategory = "PROMO_MARKETING"
        var summary = "${target.name}: Analisis pergerakan kompetitor berdasarkan inspeksi URL ${target.url}"
        var narrative = "Terdeteksi status terkini kompetitor pada ${target.url} berdasarkan analisis intelijen web."
        var confidence = 0.85
        var impactScore = 0.80
        var importance = "HIGH"
        var recommendedAction = "Tinjau positioning pasar dan selaraskan strategi penawaran harga/produk."

        val routeReq = ModelRouteRequest(
            taskCategory = "REASONING",
            prompt = prompt,
            tenantId = target.tenantId
        )
        val routeResult = modelRouter.execute(routeReq)

        if (routeResult.isSuccess) {
            val rawResponse = routeResult.getOrThrow().text
            try {
                val cleanJson = rawResponse.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val rootObj = jsonParser.parseToJsonElement(cleanJson).jsonObject
                insightCategory = rootObj["category"]?.jsonPrimitive?.content ?: insightCategory
                summary = rootObj["summary"]?.jsonPrimitive?.content ?: summary
                narrative = rootObj["narrative"]?.jsonPrimitive?.content ?: narrative
                confidence = rootObj["confidence"]?.jsonPrimitive?.doubleOrNull?.coerceIn(0.1, 1.0) ?: 0.85
                impactScore = rootObj["impactScore"]?.jsonPrimitive?.doubleOrNull?.coerceIn(0.1, 1.0) ?: 0.80
                recommendedAction = rootObj["recommendedAction"]?.jsonPrimitive?.content ?: recommendedAction
                importance = rootObj["importance"]?.jsonPrimitive?.content ?: importance
            } catch (e: Exception) {
                logger.warn("JSON parse fallback for competitor response: ${e.message}")
            }
        }

        val finalScore = (0.6 * confidence) + (0.4 * impactScore)
        val decision = when {
            finalScore >= 0.75 -> "SEND_IMMEDIATE"
            finalScore >= 0.45 -> "INCLUDE_DIGEST"
            else -> "DISCARD_AS_NOISE"
        }

        val keyBytes = MessageDigest.getInstance("SHA-256").digest("${target.id}:$insightCategory".toByteArray())
        val idKey = keyBytes.joinToString("") { "%02x".format(it) }.take(8)

        val insight = CompetitorInsight(
            id = "insight-$idKey",
            targetId = target.id,
            tenantId = target.tenantId,
            competitorName = target.name,
            category = insightCategory,
            summary = summary,
            narrative = narrative,
            confidence = confidence,
            impactScore = impactScore,
            finalScore = finalScore,
            decision = decision,
            importance = importance,
            recommendedAction = recommendedAction,
            sourceUrl = target.url,
            detectedAt = System.currentTimeMillis(),
            deliveredChannels = "Dashboard, Realtime"
        )

        // Persist to Supabase
        try {
            val payload = buildJsonObject {
                put("id", insight.id)
                put("tenant_id", insight.tenantId)
                put("target_id", insight.targetId)
                put("competitor_name", insight.competitorName)
                put("category", insight.category)
                put("summary", insight.summary)
                put("narrative", insight.narrative)
                put("confidence", insight.confidence)
                put("impact_score", insight.impactScore)
                put("final_score", insight.finalScore)
                put("decision", insight.decision)
                put("importance", insight.importance)
                put("recommended_action", insight.recommendedAction)
                put("source_url", insight.sourceUrl)
            }.toString()
            supabase.insertRecord("competitor_insights", target.tenantId, payload)
        } catch (e: Exception) {
            logger.warn("Failed persisting competitor insight to Supabase: ${e.message}")
        }

        insight
    }
}
