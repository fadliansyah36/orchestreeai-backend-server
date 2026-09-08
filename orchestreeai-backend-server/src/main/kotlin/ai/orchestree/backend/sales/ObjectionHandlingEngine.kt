package ai.orchestree.backend.sales

import kotlinx.serialization.Serializable

@Serializable
data class ObjectionAnalysis(
    val objectionCategory: String, // PRICE, TRUST, TIMING, COMPETITOR, FEATURE_MISMATCH
    val confidence: Double,
    val recommendedStrategy: String,
    val responseTemplate: String
)

object ObjectionHandlingEngine {

    fun analyzeObjection(customerMessage: String): ObjectionAnalysis {
        val lower = customerMessage.lowercase()

        return when {
            lower.contains("mahal") || lower.contains("budget") || lower.contains("harga tinggi") || lower.contains("kurang murah") -> {
                ObjectionAnalysis(
                    objectionCategory = "PRICE",
                    confidence = 0.92,
                    recommendedStrategy = "VALUE_ROI_REFRAMING",
                    responseTemplate = "Kami memahami pertimbangan investasi Anda. Dengan efisiensi yang dihasilkan, solusi ini umumnya memberikan ROI positif dalam 60 hari pertama."
                )
            }
            lower.contains("kompetitor") || lower.contains("sebelah") || lower.contains("vendor lain") -> {
                ObjectionAnalysis(
                    objectionCategory = "COMPETITOR",
                    confidence = 0.88,
                    recommendedStrategy = "DIFFERENTIATOR_HIGHLIGHT",
                    responseTemplate = "Kelebihan utama platform kami terletak pada arsitektur hibrida AI & Human Workforce dengan SLA 99.9% dan dukungan integrasi lokal instan."
                )
            }
            lower.contains("nanti") || lower.contains("bulan depan") || lower.contains("belum butuh") || lower.contains("tunggu") -> {
                ObjectionAnalysis(
                    objectionCategory = "TIMING",
                    confidence = 0.85,
                    recommendedStrategy = "URGENCY_CREATION",
                    responseTemplate = "Tentu tidak masalah. Namun perlu kami infokan bahwa alokasi onboarding batch bulan ini terbatas pada 5 tenant pertama dengan bonus setup gratis."
                )
            }
            lower.contains("fitur") || lower.contains("tidak ada") || lower.contains("bisa integrasi") -> {
                ObjectionAnalysis(
                    objectionCategory = "FEATURE_MISMATCH",
                    confidence = 0.80,
                    recommendedStrategy = "CUSTOM_WORKFLOW_DEMO",
                    responseTemplate = "Platform kami mendukung integrasi API terbuka dan custom MCP tools yang dapat disesuaikan persis dengan arsitektur sistem Anda."
                )
            }
            else -> {
                ObjectionAnalysis(
                    objectionCategory = "TRUST",
                    confidence = 0.75,
                    recommendedStrategy = "CASE_STUDY_ASSURANCE",
                    responseTemplate = "Kami telah dipercaya oleh lebih dari 50+ enterprise di Indonesia dengan standar ISO 27001 dan enkripsi end-to-end terverifikasi."
                )
            }
        }
    }
}
