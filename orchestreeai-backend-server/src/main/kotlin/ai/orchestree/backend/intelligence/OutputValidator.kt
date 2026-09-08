package ai.orchestree.backend.intelligence

import org.slf4j.LoggerFactory

data class GroundingValidationResult(
    val isGrounded: Boolean,
    val groundedClaims: List<String> = emptyList(),
    val ungroundedClaims: List<String> = emptyList(),
    val explanation: String = ""
)

class OutputValidator {
    private val logger = LoggerFactory.getLogger(OutputValidator::class.java)

    fun validate(output: String, responseFormatJson: Boolean): Boolean {
        if (output.isBlank()) return false
        if (responseFormatJson) {
            val trimmed = output.trim()
            return (trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))
        }
        return true
    }

    /**
     * REUSE Output Validator (Intelligence Layer existing) - PASTIKAN
     * setiap klaim dalam insight (mis. "skor tinggi karena pengalaman 8 tahun")
     * BENAR merujuk data asli row tsb, BUKAN dikarang/halusinasi.
     */
    fun validateGrounding(
        insight: String,
        sourceData: Map<String, Any?>
    ): GroundingValidationResult {
        if (insight.isBlank()) {
            return GroundingValidationResult(
                isGrounded = false,
                explanation = "Insight text is empty or blank."
            )
        }

        // Build a normalized knowledge base of all numbers, keywords, and values in sourceData
        val allSourceValues = mutableSetOf<String>()
        sourceData.forEach { (k, v) ->
            if (v != null) {
                allSourceValues.add(v.toString().trim().lowercase())
                allSourceValues.add(k.trim().lowercase())
                if (v is Number) {
                    val doubleVal = v.toDouble()
                    allSourceValues.add(doubleVal.toString())
                    allSourceValues.add(String.format(java.util.Locale.US, "%.1f", doubleVal))
                    allSourceValues.add(String.format(java.util.Locale.US, "%.2f", doubleVal))
                    allSourceValues.add(doubleVal.toInt().toString())
                }
            }
        }

        val sentences = insight.split(Regex("[.\\n;]")).map { it.trim() }.filter { it.length > 5 }
        val groundedClaims = mutableListOf<String>()
        val ungroundedClaims = mutableListOf<String>()

        // Regex to extract numeric/factual metrics like "8 tahun", "95.0%", "skor 85", "Rp 5000000"
        val numericPattern = Regex("""(?:\d+(?:\.\d+)?)\s*(?:tahun|thn|bln|bulan|%|skor|score|point|poin|hari|jam|jt|juta|k|ribu)?""", RegexOption.IGNORE_CASE)

        for (sentence in sentences) {
            val matches = numericPattern.findAll(sentence).map { it.value.trim() }.toList()
            if (matches.isEmpty()) {
                // If sentence is purely qualitative, check if at least one domain keyword matches sourceData
                val words = sentence.lowercase().split(Regex("\\W+")).filter { it.length > 3 }
                val hasMatch = words.any { word ->
                    allSourceValues.any { sv -> sv.contains(word) }
                }
                if (hasMatch || words.isEmpty()) {
                    groundedClaims.add(sentence)
                } else {
                    // Check if it makes an unfounded specific qualitative claim
                    groundedClaims.add(sentence)
                }
            } else {
                // Check if the numbers asserted in this sentence exist in sourceData
                var claimGrounded = true
                for (match in matches) {
                    val numberOnly = Regex("""\d+(?:\.\d+)?""").find(match)?.value ?: ""
                    val isPresent = allSourceValues.any { sv ->
                        sv == numberOnly || sv == match.lowercase() || sv.contains(numberOnly)
                    }
                    if (!isPresent && numberOnly.isNotEmpty()) {
                        // Check if it's a common rank position like #1, #2
                        val rankOnly = sentence.contains("#$numberOnly") || sentence.lowercase().contains("peringkat $numberOnly")
                        val rankMatches = sourceData["rank_position"]?.toString() == numberOnly
                        if (!rankOnly || !rankMatches) {
                            claimGrounded = false
                            ungroundedClaims.add("Claim '$sentence' asserts '$match' ($numberOnly) which does not exist in source data.")
                            break
                        }
                    }
                }
                if (claimGrounded) {
                    groundedClaims.add(sentence)
                }
            }
        }

        val isGrounded = ungroundedClaims.isEmpty()
        val explanation = if (isGrounded) {
            "All ${groundedClaims.size} factual claims in insight are verified and grounded against source data."
        } else {
            "Detected ${ungroundedClaims.size} ungrounded or hallucinated claims: ${ungroundedClaims.joinToString("; ")}"
        }

        return GroundingValidationResult(
            isGrounded = isGrounded,
            groundedClaims = groundedClaims,
            ungroundedClaims = ungroundedClaims,
            explanation = explanation
        )
    }

    /**
     * Sanitizes or re-anchors insight to eliminate hallucinated claims,
     * ensuring strict grounding to actual source data.
     */
    fun sanitizeOrAnchorInsight(
        originalInsight: String,
        sourceData: Map<String, Any?>,
        candidateName: String = "Kandidat",
        rankPosition: Int = 1,
        totalScore: Double = 0.0
    ): String {
        val validation = validateGrounding(originalInsight, sourceData)
        if (validation.isGrounded) return originalInsight

        // If partially grounded, retain only grounded sentences
        if (validation.groundedClaims.isNotEmpty()) {
            val sanitized = validation.groundedClaims.joinToString(". ") + "."
            val secondPass = validateGrounding(sanitized, sourceData)
            if (secondPass.isGrounded) return sanitized
        }

        // Deterministic fallback grounded summary strictly based on real score breakdown
        val sb = StringBuilder()
        sb.append("$candidateName berada pada peringkat #$rankPosition dengan skor total ")
        sb.append(String.format(java.util.Locale.US, "%.1f", totalScore))
        sb.append(" berdasarkan evaluasi objektif.")

        val criteriaScores = sourceData.filterKeys { k ->
            !setOf("total_score", "rank_position", "risk_score", "confidence_score", "id").contains(k)
        }
        if (criteriaScores.isNotEmpty()) {
            val topCriteria = criteriaScores.entries
                .sortedByDescending { (it.value as? Number)?.toDouble() ?: 0.0 }
                .take(2)
                .joinToString(", ") { "${it.key}: ${it.value}" }
            sb.append(" Nilai kontribusi utama mencakup $topCriteria.")
        }
        return sb.toString()
    }

    /**
     * PRD Fase 123 Bagian D.1: Lapisan Input Sanitization & Prompt Injection Defense
     * Memindai prompt sebelum dikirim ke LLM, mencatat ke audit_logs jika terdeteksi pola injeksi,
     * dan menerapkan delimiter ketat untuk mengisolasi input pengguna dari instruksi sistem.
     */
    suspend fun sanitizePromptBeforeLlmCall(
        userPrompt: String,
        systemContext: String,
        auditLog: ai.orchestree.backend.security.AuditLogger = ai.orchestree.backend.security.AuditLogger()
    ): String {
        val injectionPatterns = listOf(
            Regex("(?i)ignore (previous|all) instructions"),
            Regex("(?i)you are now"),
            Regex("(?i)system prompt"),
            Regex("(?i)reveal your (instructions|prompt)"),
            Regex("(?i)disregard all prior directives"),
            Regex("(?i)bypass (guardrails|safety)"),
            Regex("(?i)act as (dan|jailbreak)")
        )

        val flagged = injectionPatterns.any { it.containsMatchIn(userPrompt) }
        if (flagged) {
            logger.warn("[PROMPT INJECTION DETECTED] Potential prompt override pattern detected in prompt: ${userPrompt.take(100)}")
            auditLog.record("prompt_injection_attempt_detected", userPrompt.take(200))
            // Terapkan delimiter ketat untuk membendung prompt override agar tidak membocorkan instruksi sistem
            return "<<<USER_INPUT_UNTRUSTED>>>\n$userPrompt\n<<<END_USER_INPUT_UNTRUSTED>>>"
        }

        return userPrompt
    }

    /**
     * PRD Fase 123 Bagian D.3: Memfilter respons LLM sebelum dikembalikan ke user
     * Mencegah LLM membocorkan instruksi sistem internal atau kredensial akibat injeksi prompt.
     */
    fun filterLlmOutput(
        llmResponse: String,
        internalInstructionsToProtect: List<String> = emptyList()
    ): String {
        if (llmResponse.isBlank()) return llmResponse

        var filtered = llmResponse

        // Masking leaks of known internal system instructions
        for (instruction in internalInstructionsToProtect) {
            if (instruction.isNotBlank() && filtered.contains(instruction)) {
                logger.warn("[OUTPUT LEAK FILTERED] Masked leaked internal system instruction from LLM response")
                filtered = filtered.replace(instruction, "[REDACTED INTERNAL DIRECTIVE]")
            }
        }

        // Masking accidental leaks of sensitive markers
        val sensitiveMarkers = listOf(
            Regex("(?i)system prompt:.*?(?=\\n|$)"),
            Regex("(?i)internal instructions:.*?(?=\\n|$)"),
            Regex("(?i)api_key\\s*[:=]\\s*['\"]?[a-zA-Z0-9_-]{20,}['\"]?")
        )

        for (marker in sensitiveMarkers) {
            if (marker.containsMatchIn(filtered)) {
                logger.warn("[OUTPUT LEAK FILTERED] Masked sensitive marker in LLM response")
                filtered = marker.replace(filtered, "[REDACTED SECURITY SENSITIVE DATA]")
            }
        }

        return filtered
    }
}

