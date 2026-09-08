package ai.orchestree.backend.intelligence

import kotlinx.serialization.Serializable

@Serializable
data class CompetitorPriceDiff(
    val sku: String,
    val internalPrice: Double,
    val competitorPrice: Double,
    val differenceAmount: Double,
    val percentageDiff: Double,
    val competitorName: String,
    val recommendation: String
)

object CompetitorDiffEngine {

    fun computePriceDeltas(
        sku: String,
        internalPrice: Double,
        competitorPrices: Map<String, Double>
    ): List<CompetitorPriceDiff> {
        val diffs = mutableListOf<CompetitorPriceDiff>()

        for ((competitor, compPrice) in competitorPrices) {
            val delta = internalPrice - compPrice
            val pct = if (internalPrice > 0) (delta / internalPrice) * 100.0 else 0.0

            val rec = when {
                delta > 0 && pct > 10.0 -> "Harga kita lebih mahal ${String.format("%.1f", pct)}% dibanding $competitor. Evaluasi penyesuaian promo/bundling."
                delta < 0 && pct < -10.0 -> "Harga kita lebih murah ${String.format("%.1f", -pct)}% dibanding $competitor. Peluang margin atau keunggulan daya saing tinggi."
                else -> "Harga bersaing dalam batas toleransi wajar (±10%)."
            }

            diffs.add(
                CompetitorPriceDiff(
                    sku = sku,
                    internalPrice = internalPrice,
                    competitorPrice = compPrice,
                    differenceAmount = delta,
                    percentageDiff = pct,
                    competitorName = competitor,
                    recommendation = rec
                )
            )
        }

        return diffs
    }
}
