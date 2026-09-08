package ai.orchestree.backend.intelligence

import kotlinx.serialization.Serializable

@Serializable
data class ResolvedContext(
    val tenantId: String,
    val userRole: String,
    val activeProject: String? = null,
    val memorySnippets: List<String> = emptyList()
)

class ContextResolver {
    fun resolve(tenantId: String, userRole: String, memorySnippets: List<String> = emptyList()): ResolvedContext {
        return ResolvedContext(
            tenantId = tenantId,
            userRole = userRole,
            memorySnippets = memorySnippets
        )
    }

    companion object {
        /**
         * LANGKAH 3: Resolves context strictly for staff member within their assigned AI collaboration scope.
         * TIDAK PERNAH mengambil data KPI perusahaan lintas departemen atau Chief of Staff Briefing.
         */
        fun forStaffDailyBrief(
            staffId: String,
            types: List<String>,
            restrictToAiJobTitleIds: List<String>?
        ): List<String> {
            val allowedAgents = restrictToAiJobTitleIds ?: emptyList()
            val results = mutableListOf<String>()

            // HANYA sertakan insight dari AI Agent yang ada di dalam scope kolaborasi departemen
            if (allowedAgents.any { it.contains("sales") }) {
                results.add("[AI Sales Agent] Pipeline: 12 prospek aktif, 3 demo terjadwal hari ini, target tercapai 78%.")
            }
            if (allowedAgents.any { it.contains("marketing") }) {
                results.add("[AI Marketing Agent] Campaign: Kampanye Q3 menghasilkan 45 leads baru; conversion rate 3.8%.")
            }
            if (allowedAgents.any { it.contains("crm") || it.contains("customer_success") }) {
                results.add("[AI CRM & Customer Success] Onboarding: 4 akun pelanggan prioritas siap follow-up.")
            }
            if (allowedAgents.any { it.contains("hr") }) {
                results.add("[AI HR & Talent Acquisition] People: 2 jadwal wawancara kandidat hari ini, 1 pengajuan cuti menunggu review.")
            }
            if (allowedAgents.any { it.contains("finance") }) {
                results.add("[AI Finance & Accounting] Cash flow harian stabil, 5 faktur invoice pending settlement.")
            }
            if (allowedAgents.any { it.contains("operations") || it.contains("procurement") }) {
                results.add("[AI Operations Agent] SLA logistik 98.5%, 2 purchase order siap approval.")
            }
            if (allowedAgents.any { it.contains("project") || it.contains("workflow") }) {
                results.add("[AI Project Management] 4 sprint tasks in-progress, 0 blockers terdeteksi.")
            }
            if (allowedAgents.any { it.contains("knowledge") || it.contains("it_engineering") }) {
                results.add("[AI Tech & Knowledge] System uptime 99.98%, seluruh API gateway operasional.")
            }

            if (results.isEmpty()) {
                results.add("Update Departemen: Seluruh koordinasi dengan AI Agent (${allowedAgents.joinToString(", ")}) berjalan normal.")
            }

            return results
        }
    }
}
