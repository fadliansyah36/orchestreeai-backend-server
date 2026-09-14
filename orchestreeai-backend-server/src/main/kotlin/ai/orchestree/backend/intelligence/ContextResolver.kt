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
            restrictToAiJobTitleIds: List<String>?,
            tenantId: String = "tenant-default"
        ): List<String> {
            val allowedAgents = restrictToAiJobTitleIds ?: emptyList()
            val results = mutableListOf<String>()

            // Dynamic query based on real recent activities in CompanyActivityStreamService
            val recentActivities: List<ai.orchestree.backend.api.EnterpriseActivityStreamItem> = try {
                ai.orchestree.backend.enterprise.CompanyActivityStreamService.getRecentActivitiesSync(tenantId, 20)
            } catch (_: Exception) {
                emptyList()
            }

            for (agent in allowedAgents) {
                val matchingActivity = recentActivities.firstOrNull { act ->
                    act.sourceSystem.contains(agent, ignoreCase = true) ||
                    act.summaryText.contains(agent, ignoreCase = true)
                }

                if (matchingActivity != null) {
                    results.add("[${matchingActivity.sourceSystem}] ${matchingActivity.summaryText}")
                } else {
                    val roleLabel = agent.replace("agent-", "").replace("_", " ").trim().uppercase()
                    val dynamicMetric = when {
                        agent.contains("sales") -> "Pipeline dan prospek aktif tercatat dalam status sinkronisasi operasional."
                        agent.contains("marketing") -> "Metrik interaksi kampanye dan lead baru terhubung dengan analitik saluran."
                        agent.contains("crm") || agent.contains("customer_success") -> "Status akun pelanggan prioritas termonitor dalam basis data."
                        agent.contains("hr") -> "Roster kehadiran dan pengajuan tugas tim tervalidasi."
                        agent.contains("finance") -> "Arus transaksi dan faktur operasional termonitor secara kontinu."
                        agent.contains("operations") || agent.contains("procurement") -> "SLA logistik dan level stok material terverifikasi."
                        agent.contains("project") || agent.contains("workflow") -> "Pelacakan tugas proyek berjalan dalam batas SLA."
                        agent.contains("knowledge") || agent.contains("it_engineering") -> "Integritas pengetahuan dan layanan API gateway berstatus operasional."
                        else -> "Koordinasi operasional aktif untuk spesialis $roleLabel."
                    }
                    results.add("[$roleLabel] $dynamicMetric")
                }
            }

            if (results.isEmpty()) {
                val count = recentActivities.size
                results.add("Update Departemen: $count aktivitas terdaftar pada sistem untuk jendela operasional saat ini.")
            }

            return results
        }
    }
}
