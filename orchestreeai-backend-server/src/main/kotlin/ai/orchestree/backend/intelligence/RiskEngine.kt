package ai.orchestree.backend.intelligence

data class OutboundMessageRiskCheck(
    val passed: Boolean,
    val reason: String? = null
)

class RiskEngine {
    fun calculateRiskScore(intent: String, isFinancialAction: Boolean, amountIdr: Long = 0): Double {
        var score = 0.1
        if (intent == "NOTIFICATION_BROADCAST") score += 0.3
        if (isFinancialAction) score += 0.4
        if (amountIdr > 10_000_000) score += 0.2
        return score.coerceIn(0.0, 1.0)
    }

    fun evaluateOutboundMessage(message: String): OutboundMessageRiskCheck {
        if (message.isBlank()) {
            return OutboundMessageRiskCheck(passed = false, reason = "Pesan proaktif tidak boleh kosong.")
        }
        val lower = message.lowercase()
        val prohibited = listOf("bocorkan data", "rahasia bank", "dump password")
        if (prohibited.any { lower.contains(it) }) {
            return OutboundMessageRiskCheck(passed = false, reason = "Pesan terdeteksi memuat konten berisiko tinggi.")
        }
        return OutboundMessageRiskCheck(passed = true)
    }

    fun evaluateSelectionRisk(row: DataRow, criterionScores: List<Pair<WeightedCriterion, Double>>): Double {
        var risk = 10.0
        val missingCount = row.fields.values.count { it.isBlank() }
        risk += missingCount * 5.0

        val failedCriteriaCount = criterionScores.count { (_, score) -> score < 50.0 }
        risk += failedCriteriaCount * 12.0

        val avgScore = if (criterionScores.isNotEmpty()) criterionScores.map { it.second }.average() else 70.0
        if (avgScore < 60.0) {
            risk += (60.0 - avgScore) * 0.5
        }

        if (row.qualityScore < 80.0) {
            risk += (80.0 - row.qualityScore) * 0.3
        }

        val res = risk.coerceIn(0.0, 100.0)
        return Math.round(res * 100.0) / 100.0
    }
}
