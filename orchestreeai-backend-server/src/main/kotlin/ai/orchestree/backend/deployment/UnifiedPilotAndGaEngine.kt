package ai.orchestree.backend.deployment

import kotlinx.serialization.Serializable

@Serializable
data class CanaryGateDecision(
    val allowProgression: Boolean,
    val currentCanaryPercentage: Int,
    val recommendedNextPercentage: Int,
    val errorRatePercentage: Double,
    val p99LatencyMs: Long,
    val rollbackTriggered: Boolean
)

object UnifiedPilotAndGaEngine {

    fun evaluateCanaryHealth(
        currentTrafficPct: Int,
        sampleRequestsCount: Int,
        failedRequestsCount: Int,
        p99LatencyMs: Long
    ): CanaryGateDecision {
        val errorRate = if (sampleRequestsCount > 0) (failedRequestsCount.toDouble() / sampleRequestsCount) * 100.0 else 0.0

        // Guardrails:
        // If error rate > 1.5% or P99 latency > 3000ms -> Trigger automated rollback
        if (errorRate > 1.5 || p99LatencyMs > 3000) {
            return CanaryGateDecision(
                allowProgression = false,
                currentCanaryPercentage = currentTrafficPct,
                recommendedNextPercentage = 0,
                errorRatePercentage = errorRate,
                p99LatencyMs = p99LatencyMs,
                rollbackTriggered = true
            )
        }

        // Progression ladder: 10% -> 25% -> 50% -> 100% (GA)
        val nextPct = when {
            currentTrafficPct < 25 -> 25
            currentTrafficPct < 50 -> 50
            else -> 100
        }

        return CanaryGateDecision(
            allowProgression = true,
            currentCanaryPercentage = currentTrafficPct,
            recommendedNextPercentage = nextPct,
            errorRatePercentage = errorRate,
            p99LatencyMs = p99LatencyMs,
            rollbackTriggered = false
        )
    }
}
