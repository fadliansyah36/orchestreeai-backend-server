package ai.orchestree.backend.intelligence

import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import kotlinx.serialization.Serializable

enum class IntentCategory {
    TASK_CREATION_COMMAND,
    TASK_CREATION,
    COMPETITOR_AUDIT,
    MARKETING_CAMPAIGN,
    NOTIFICATION_BROADCAST,
    MANAGEMENT_QUERY,
    DATA_SELECTION_REQUEST,
    GENERAL_INQUIRY
}

@Serializable
data class ClassifiedIntent(
    val intentCode: String,
    val confidence: Double,
    val modality: String = "TEXT",
    val entities: Map<String, String> = emptyMap()
)

class IntentClassifier(
    private val modelRouter: ModelRouter = ModelRouter()
) {
    fun classifyCategory(prompt: String): IntentCategory {
        val p = prompt.lowercase()
        return when {
            p.startsWith("/task") || p.startsWith("/tugas") || p.contains("buat task") || p.contains("buatkan tugas") || p.contains("tambahkan task") || p.contains("jadwalkan tugas") -> IntentCategory.TASK_CREATION_COMMAND
            p.contains("seleksi") || p.contains("ranking") || p.contains("rekrutmen") || p.contains("kandidat terbaik") ||
                p.contains("supplier terbaik") || p.contains("scoring") || p.contains("evaluasi vendor") ||
                p.contains("pilih kandidat") || p.contains("shortlist") || p.contains("peringkat supplier") -> IntentCategory.DATA_SELECTION_REQUEST
            p.contains("laba") || p.contains("profit") || p.contains("revenue") || p.contains("pendapatan") ||
                p.contains("kpi perusahaan") || p.contains("laporan keuangan") || p.contains("rekap gaji") ||
                p.contains("performa tim") || p.contains("evaluasi divisi") || p.contains("cash flow") ||
                p.contains("kinerja perusahaan") || p.contains("overview bisnis") || p.contains("rekap omset") ||
                p.contains("laporan eksekutif") || p.contains("briefing direksi") -> IntentCategory.MANAGEMENT_QUERY
            p.contains("pantau kompetitor") || p.contains("audit harga") || p.contains("cek kompetitor") -> IntentCategory.COMPETITOR_AUDIT
            p.contains("iklan") || p.contains("kampanye") || p.contains("promosi") -> IntentCategory.MARKETING_CAMPAIGN
            p.contains("broadcast") || p.contains("kirim pengumuman") -> IntentCategory.NOTIFICATION_BROADCAST
            p.contains("task") || p.contains("tugas") || p.contains("kerjakan") -> IntentCategory.TASK_CREATION
            else -> IntentCategory.GENERAL_INQUIRY
        }
    }

    suspend fun classify(prompt: String, tenantId: String = "tenant-default"): ClassifiedIntent {
        val systemPrompt = "Klasifikasikan prompt pengguna menjadi SATU kategori berikut: TASK_CREATION_COMMAND, TASK_CREATION, DATA_SELECTION_REQUEST, COMPETITOR_AUDIT, MARKETING_CAMPAIGN, NOTIFICATION_BROADCAST, MANAGEMENT_QUERY, GENERAL_INQUIRY. Berikan HANYA nama kategorinya."
        val req = ModelRouteRequest(
            taskCategory = "FAST_CLASSIFICATION",
            prompt = prompt,
            systemInstruction = systemPrompt,
            tenantId = tenantId
        )
        val res = modelRouter.execute(req)
        val category = if (res.isSuccess) {
            val text = res.getOrThrow().text.trim().uppercase()
            when {
                text.contains("SELECTION") || text.contains("SELEKSI") || text.contains("RANKING") -> "DATA_SELECTION_REQUEST"
                text.contains("TASK_CREATION_COMMAND") || (text.contains("COMMAND") && text.contains("TASK")) -> "TASK_CREATION_COMMAND"
                text.contains("MANAGEMENT") || text.contains("FINANCE") || text.contains("LABA") -> "MANAGEMENT_QUERY"
                text.contains("COMPETITOR") || text.contains("PANTAU") -> "COMPETITOR_AUDIT"
                text.contains("MARKETING") || text.contains("CAMPAIGN") || text.contains("IKLAN") -> "MARKETING_CAMPAIGN"
                text.contains("NOTIF") || text.contains("BROADCAST") || text.contains("KIRIM") -> "NOTIFICATION_BROADCAST"
                text.contains("TASK") || text.contains("TUGAS") -> "TASK_CREATION"
                else -> "GENERAL_INQUIRY"
            }
        } else {
            // Rule-based fallback
            classifyCategory(prompt).name
        }

        return ClassifiedIntent(
            intentCode = category,
            confidence = 0.94
        )
    }
}

class ModalityClassifier {
    fun classify(prompt: String): String {
        return when {
            prompt.contains("gambar", true) || prompt.contains("visual", true) || prompt.contains("banner", true) -> "IMAGE_ASSET"
            prompt.contains("laporan", true) || prompt.contains("analisis", true) || prompt.contains("dokumen", true) -> "STRUCTURED_REPORT"
            prompt.contains("telegram", true) || prompt.contains("pesan", true) || prompt.contains("kirim", true) -> "NOTIFICATION"
            else -> "KANBAN_TASK"
        }
    }
}
