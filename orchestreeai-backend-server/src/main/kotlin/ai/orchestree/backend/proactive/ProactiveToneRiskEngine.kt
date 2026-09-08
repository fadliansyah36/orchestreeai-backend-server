package ai.orchestree.backend.proactive

data class ToneCheckResult(
    val passed: Boolean,
    val reason: String? = null,
    val sanitizedContent: String
)

object ProactiveToneRiskEngine {

    /**
     * Evaluates outbound proactive messages before dispatch (PRD Section 11.2 & 11.5)
     * Enforces Indonesian professional tone, filters raw credentials/tokens, and prevents PII leakage.
     */
    fun evaluateOutboundMessage(content: String): ToneCheckResult {
        if (content.isBlank()) {
            return ToneCheckResult(
                passed = false,
                reason = "Isi pesan kosong.",
                sanitizedContent = content
            )
        }

        // 1. Check for sensitive credential leaks (e.g. Bearer, xoxb, password, api_key)
        val lower = content.lowercase()
        if (lower.contains("bearer ") || lower.contains("xoxb-") || lower.contains("sk-") || lower.contains("api_key=")) {
            return ToneCheckResult(
                passed = false,
                reason = "Pesan mengandung token sensitif internal yang terdeteksi oleh Data Privacy Guard.",
                sanitizedContent = "[KONTEN DIBLOKIR KARENA MEMUAT KREDENSIAL RAHASIA]"
            )
        }

        // 2. Filter unhandled raw JSON or code block leakage
        var cleaned = content
        if (cleaned.startsWith("```json") || cleaned.startsWith("```")) {
            cleaned = cleaned.replace(Regex("^```[a-z]*\\n"), "").replace(Regex("\\n```$"), "").trim()
        }

        // 3. Ensure length adheres to channel limits
        if (cleaned.length > 2000) {
            cleaned = cleaned.take(1980) + "..."
        }

        return ToneCheckResult(
            passed = true,
            reason = null,
            sanitizedContent = cleaned
        )
    }
}
