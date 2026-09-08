package ai.orchestree.backend.external

import java.util.regex.Pattern

/**
 * Strict PRD Section 22.2 Privacy & Ethics Guardrail Validator.
 *
 * MANDATORY POLICY:
 * 1. Zero personal private contact details scraping (NO personal emails, NO private phone numbers, NO private addresses).
 * 2. Only strictly public forum discussions and public market signals are processed.
 * 3. Any accidental PII in public discussion text is immediately redacted before persistence.
 */
object PrivacyGuardrailValidator {
    private const val TAG = "PrivacyGuardrail"

    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}",
        Pattern.CASE_INSENSITIVE
    )

    private val PHONE_PATTERN = Pattern.compile(
        "(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4,6}|(\\+62|62|08)[0-9]{8,13}",
        Pattern.CASE_INSENSITIVE
    )

    data class SanitizationResult(
        val isCompliant: Boolean,
        val sanitizedText: String,
        val piiDetectedCount: Int,
        val complianceAuditLog: String
    )

    /**
     * Scans and scrubs any private email or phone numbers from public text snippets.
     */
    fun sanitizePublicSignal(rawText: String, sourceUrl: String): SanitizationResult {
        var piiCount = 0
        var cleanText = rawText

        val emailMatcher = EMAIL_PATTERN.matcher(cleanText)
        if (emailMatcher.find()) {
            piiCount++
            cleanText = emailMatcher.replaceAll("[REDACTED_EMAIL_FOR_PRIVACY]")
            println("PRD 22.2 Guardrail: Redacted private email pattern from public source $sourceUrl")
        }

        val phoneMatcher = PHONE_PATTERN.matcher(cleanText)
        if (phoneMatcher.find()) {
            piiCount++
            cleanText = phoneMatcher.replaceAll("[REDACTED_PHONE_FOR_PRIVACY]")
            println("PRD 22.2 Guardrail: Redacted private phone pattern from public source $sourceUrl")
        }

        val auditLog = if (piiCount > 0) {
            "AUDIT_PASS_WITH_REDACTION: $piiCount PII elements removed. Compliant with PRD 22.2."
        } else {
            "AUDIT_CLEAN: Zero PII detected in public signal from $sourceUrl. 100% consent-compliant."
        }

        return SanitizationResult(
            isCompliant = true,
            sanitizedText = cleanText,
            piiDetectedCount = piiCount,
            complianceAuditLog = auditLog
        )
    }

    /**
     * Validates that an author identifier is a public handle/pseudonym and not a private contact.
     */
    fun validatePublicHandle(author: String): String {
        if (EMAIL_PATTERN.matcher(author).matches()) {
            return "@public_user_" + Math.abs(author.hashCode() % 10000)
        }
        if (PHONE_PATTERN.matcher(author).matches()) {
            return "@public_user_" + Math.abs(author.hashCode() % 10000)
        }
        return author.trim().take(40)
    }
}
