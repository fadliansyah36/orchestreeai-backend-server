package ai.orchestree.backend.database.repositories.taskboard

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.workforce.UserRepository
import ai.orchestree.backend.database.repositories.workforce.UserPersonaRepository
import ai.orchestree.backend.database.repositories.workforce.ProactiveCollaborationScopeRepository
import ai.orchestree.backend.database.repositories.workforce.StaffProfileRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
enum class TaskColumn {
    BACKLOG,
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE;

    val label: String
        get() = when (this) {
            BACKLOG -> "Backlog"
            TODO -> "To Do"
            IN_PROGRESS -> "In Progress"
            IN_REVIEW -> "In Review"
            DONE -> "Done"
        }
}

@Serializable
data class Task(
    val id: String,
    val tenantId: String,
    val title: String,
    val description: String = "",
    val columnName: String = "IN_PROGRESS",
    val column: String = columnName,
    val priority: String = "MEDIUM",
    val assigneeType: String = "AI_AGENT", // 'AI_AGENT' or 'HUMAN'
    val assigneeId: String = "",
    val assigneeName: String = "",
    val dueDate: String = "",
    val progressPct: Int = 0,
    val liveStatusLine: String = "",
    val workflowExecutionId: String? = null,
    val requiresApproval: Boolean = false,
    val isApproved: Boolean = false,
    val createdByType: String = "human", // 'human', 'ai_agent_self_initiated', 'orchestration_engine'
    val aiActivityStatusLine: String? = null,
    val descriptionRichText: String = "",
    val thirdPartyMonitoringTarget: String? = null,
    val sourceChannel: String = "dashboard", // 'dashboard', 'telegram', 'whatsapp'
    val departmentId: String? = null,
    val teamId: String? = null,
    val boardId: String? = "default",
    val aiJobTitleId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

typealias TaskModel = Task
typealias TaskRecord = Task
typealias TaskChecklistRecord = TaskChecklistItem
typealias TaskActivityLogRecord = TaskActivityLogItem
typealias TaskAttachmentRecord = TaskAttachmentItem

@Serializable
data class TaskAttachmentItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val taskId: String,
    val tenantId: String,
    val fileName: String,
    val fileUrl: String,
    val fileSizeBytes: Long = 0L,
    val uploadedBy: String = "staff",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class TaskChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val taskId: String,
    val tenantId: String,
    val itemText: String,
    val isCompleted: Boolean = false,
    val completedById: String? = null,
    val completedByType: String? = null,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class TaskActivityLogItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val taskId: String,
    val tenantId: String,
    val actorId: String? = null,
    val actorType: String = "ai_agent", // 'human', 'ai_agent', 'orchestration_engine'
    val action: String, // 'created_self_initiated', 'progress_updated', 'created', 'completed', 'node_completed'
    val detail: String,
    val createdAt: Long = System.currentTimeMillis()
)

class TaskRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val userRepo: UserRepository = UserRepository.defaultInstance,
    private val userPersonaRepo: UserPersonaRepository = UserPersonaRepository.defaultInstance,
    private val proactiveCollaborationScopeRepo: ProactiveCollaborationScopeRepository = ProactiveCollaborationScopeRepository.defaultInstance
) {
    private val logger = LoggerFactory.getLogger(TaskRepository::class.java)

    companion object {
        // Synchronized in-memory store matching database state across all instances
        private val tasksStore = ConcurrentHashMap<String, Task>()
        private val checklistsStore = ConcurrentHashMap<String, CopyOnWriteArrayList<TaskChecklistItem>>()
        private val activityLogsStore = ConcurrentHashMap<String, CopyOnWriteArrayList<TaskActivityLogItem>>()
        private val attachmentsStore = ConcurrentHashMap<String, CopyOnWriteArrayList<TaskAttachmentItem>>()
        private val taskAiCollaborationScopeStore = ConcurrentHashMap<String, MutableSet<String>>()

        init {
            seedDefaultDataIfEmpty()
        }

        private fun seedDefaultDataIfEmpty() {
            if (tasksStore.isNotEmpty()) return
            val sampleAiTask = Task(
                id = "tsk-ai-crawl-01",
                tenantId = "tenant-enterprise-001",
                title = "Autonomous Market Intel: Crawl Harga Kompetitor Marketplace",
                description = "Tugas otonom inisiasi AI Agent untuk memantau pergeseran harga katalog kompetitor harian.",
                descriptionRichText = "### Sasaran Pemantauan\n- Memantau pergeseran harga katalog kompetitor secara otonom.\n- Ekstraksi SKU, harga diskon, dan ketersediaan stok produk unggulan.\n- Melakukan validasi anomali potongan harga > 20%.\n- Kirim insight realtime ke Board dan tim pemasaran.",
                columnName = "IN_PROGRESS",
                priority = "HIGH",
                assigneeType = "AI_AGENT",
                assigneeId = "agent-market-intel",
                assigneeName = "Agent Market Intel (AI)",
                dueDate = "Hari ini, 18:00",
                progressPct = 40,
                liveStatusLine = "Crawling https://tokopedia.com/competitor-store - Step 2/5 (Extracting pricing catalog)",
                workflowExecutionId = "wf-crawl-live-01",
                requiresApproval = false,
                isApproved = true,
                createdByType = "ai_agent_self_initiated",
                aiActivityStatusLine = "Crawling https://tokopedia.com/competitor-store - Step 2/5 (Extracting pricing catalog)",
                thirdPartyMonitoringTarget = "https://tokopedia.com/competitor-store",
                sourceChannel = "dashboard",
                departmentId = "sales",
                teamId = "team-sales-field-01",
                aiJobTitleId = "marketing"
            )
            tasksStore[sampleAiTask.id] = sampleAiTask
            taskAiCollaborationScopeStore.computeIfAbsent(sampleAiTask.id) { ConcurrentHashMap.newKeySet() }.add("marketing")

            val sampleHumanTask = Task(
                id = "tsk-human-rev-02",
                tenantId = "tenant-enterprise-001",
                title = "Finalisasi Kontrak Kerjasama Vendor Cloud Q3",
                description = "Review klausul SLA dan enkripsi data dengan tim legal.",
                descriptionRichText = "### Review Kontrak\n1. Pastikan RPO < 5 menit dan RTO < 15 menit.\n2. Validasi klausul kerahasiaan data pelanggan.\n3. Tandatangani dokumen digital.",
                columnName = "TODO",
                priority = "MEDIUM",
                assigneeType = "HUMAN",
                assigneeId = "staff-legal-01",
                assigneeName = "Rian Legal Specialist",
                dueDate = "Besok, 12:00",
                progressPct = 10,
                liveStatusLine = "Menunggu review tim procurement",
                departmentId = "legal",
                createdByType = "human"
            )
            tasksStore[sampleHumanTask.id] = sampleHumanTask

            val sampleSalesStaffTask = Task(
                id = "tsk-staff-sales-01",
                tenantId = "tenant-enterprise-001",
                title = "Follow up Klien Prioritas Bank Mandiri",
                description = "Presentasi proposal solusi enterprise dan demo sistem.",
                columnName = "IN_PROGRESS",
                priority = "HIGH",
                assigneeType = "HUMAN",
                assigneeId = "usr-staff-sales-01",
                assigneeName = "Budi Hartono",
                departmentId = "sales",
                teamId = "team-sales-field-01",
                createdByType = "human"
            )
            tasksStore[sampleSalesStaffTask.id] = sampleSalesStaffTask

            val sampleSalesTeamTask = Task(
                id = "tsk-team-sales-01",
                tenantId = "tenant-enterprise-001",
                title = "Penyusunan Target Kuartal IV Tim Field Sales",
                description = "Konsolidasi pipeline dan alokasi wilayah.",
                columnName = "TODO",
                priority = "MEDIUM",
                assigneeType = "HUMAN",
                assigneeId = "usr-staff-sales-field-02",
                assigneeName = "Rudi Field Sales",
                departmentId = "sales",
                teamId = "team-sales-field-01",
                createdByType = "human"
            )
            tasksStore[sampleSalesTeamTask.id] = sampleSalesTeamTask

            val sampleSalesLeadsTask = Task(
                id = "tsk-dept-sales-leads-01",
                tenantId = "tenant-enterprise-001",
                title = "Kualifikasi Inbound Leads Enterprise Q3",
                description = "Validasi prospek masuk dari webinar.",
                columnName = "IN_PROGRESS",
                priority = "MEDIUM",
                assigneeType = "HUMAN",
                assigneeId = "usr-sales-leads-01",
                assigneeName = "Hendra Leads",
                departmentId = "sales",
                teamId = "team-sales-leads",
                createdByType = "human"
            )
            tasksStore[sampleSalesLeadsTask.id] = sampleSalesLeadsTask

            val sampleFinanceTask = Task(
                id = "tsk-finance-01",
                tenantId = "tenant-enterprise-001",
                title = "Rekonsiliasi Pajak dan Faktur Penjualan Bulanan",
                description = "Sinkronisasi e-faktur dengan laporan neraca.",
                columnName = "IN_PROGRESS",
                priority = "HIGH",
                assigneeType = "HUMAN",
                assigneeId = "usr-staff-finance-01",
                assigneeName = "Siti Rahmawati",
                departmentId = "finance",
                teamId = "team-finance-accounting",
                createdByType = "human"
            )
            tasksStore[sampleFinanceTask.id] = sampleFinanceTask

            val sampleFinanceAiTask = Task(
                id = "tsk-finance-ai-01",
                tenantId = "tenant-enterprise-001",
                title = "Autonomous Financial Audit: Deteksi Anomali Pengeluaran",
                description = "AI Agent monitoring audit transaksi GL.",
                columnName = "TODO",
                priority = "MEDIUM",
                assigneeType = "AI_AGENT",
                assigneeId = "agent-finance-ai",
                assigneeName = "Finance Intelligence Bot",
                departmentId = "finance",
                teamId = "team-finance-accounting",
                aiJobTitleId = "finance",
                createdByType = "ai_agent_self_initiated"
            )
            tasksStore[sampleFinanceAiTask.id] = sampleFinanceAiTask
            taskAiCollaborationScopeStore.computeIfAbsent(sampleFinanceAiTask.id) { ConcurrentHashMap.newKeySet() }.add("finance")

            // Additional multi-tenant tasks for platform-wide analytics
            val sampleEnterpriseOrchTask = Task(
                id = "tsk-orch-route-03",
                tenantId = "tenant-enterprise-001",
                title = "Optimasi Rute Armada Distribusi Logistik Jawa Barat",
                columnName = "DONE",
                priority = "HIGH",
                assigneeType = "AI_AGENT",
                assigneeId = "agent-logistics-ai",
                assigneeName = "Logistics Engine",
                progressPct = 100,
                createdByType = "orchestration_engine",
                sourceChannel = "dashboard"
            )
            tasksStore[sampleEnterpriseOrchTask.id] = sampleEnterpriseOrchTask

            val sampleGrowthHumanTask = Task(
                id = "tsk-growth-01",
                tenantId = "tenant-growth-002",
                title = "Evaluasi Katalog Corak Batik Tradisional Q3",
                columnName = "IN_PROGRESS",
                priority = "MEDIUM",
                assigneeType = "HUMAN",
                assigneeId = "usr-04",
                assigneeName = "Rina Wulandari",
                progressPct = 60,
                createdByType = "human",
                sourceChannel = "dashboard"
            )
            tasksStore[sampleGrowthHumanTask.id] = sampleGrowthHumanTask

            val sampleGrowthAiTask = Task(
                id = "tsk-growth-02",
                tenantId = "tenant-growth-002",
                title = "Generasi Deskripsi Produk Otomatis dari Foto Kain",
                columnName = "DONE",
                priority = "MEDIUM",
                assigneeType = "AI_AGENT",
                assigneeId = "usr-ai-02",
                assigneeName = "Agent CS Bot",
                progressPct = 100,
                createdByType = "ai_agent_self_initiated",
                sourceChannel = "telegram"
            )
            tasksStore[sampleGrowthAiTask.id] = sampleGrowthAiTask

            val sampleScaleAiTask = Task(
                id = "tsk-scale-01",
                tenantId = "tenant-scale-003",
                title = "Analisis Sentimen Ulasan Pembeli Roastery",
                columnName = "DONE",
                priority = "LOW",
                assigneeType = "AI_AGENT",
                assigneeId = "usr-ai-01",
                assigneeName = "Agent Roastery",
                progressPct = 100,
                createdByType = "ai_agent_self_initiated",
                sourceChannel = "whatsapp"
            )
            tasksStore[sampleScaleAiTask.id] = sampleScaleAiTask

            val sampleScaleHumanTask = Task(
                id = "tsk-scale-02",
                tenantId = "tenant-scale-003",
                title = "Restock Green Beans Kopi Gayo 50kg",
                columnName = "TODO",
                priority = "HIGH",
                assigneeType = "HUMAN",
                assigneeId = "usr-06",
                assigneeName = "Eko Saputra",
                progressPct = 0,
                createdByType = "human",
                sourceChannel = "dashboard"
            )
            tasksStore[sampleScaleHumanTask.id] = sampleScaleHumanTask

            val sampleStarterTask = Task(
                id = "tsk-starter-01",
                tenantId = "tenant-starter-004",
                title = "Setup Akun Sandbox Midtrans dan Callback URL",
                columnName = "BACKLOG",
                priority = "MEDIUM",
                assigneeType = "HUMAN",
                assigneeId = "usr-08",
                assigneeName = "Hendro Wijaya",
                progressPct = 0,
                createdByType = "human",
                sourceChannel = "dashboard"
            )
            tasksStore[sampleStarterTask.id] = sampleStarterTask

            // Seed checklists for AI Task
            val aiChecklists = CopyOnWriteArrayList(
                listOf(
                    TaskChecklistItem(
                        id = "chk-01",
                        taskId = sampleAiTask.id,
                        tenantId = sampleAiTask.tenantId,
                        itemText = "Inisiasi scraping browser headless & bypass captcha",
                        isCompleted = true,
                        orderIndex = 0
                    ),
                    TaskChecklistItem(
                        id = "chk-02",
                        taskId = sampleAiTask.id,
                        tenantId = sampleAiTask.tenantId,
                        itemText = "Ekstraksi DOM harga & diskon marketplace",
                        isCompleted = false,
                        orderIndex = 1
                    ),
                    TaskChecklistItem(
                        id = "chk-03",
                        taskId = sampleAiTask.id,
                        tenantId = sampleAiTask.tenantId,
                        itemText = "Deteksi lonjakan diskon komparatif > 15%",
                        isCompleted = false,
                        orderIndex = 2
                    ),
                    TaskChecklistItem(
                        id = "chk-04",
                        taskId = sampleAiTask.id,
                        tenantId = sampleAiTask.tenantId,
                        itemText = "Sinkronisasi insight ke database & alert Telegram",
                        isCompleted = false,
                        orderIndex = 3
                    )
                )
            )
            checklistsStore[sampleAiTask.id] = aiChecklists

            // Seed activity logs
            val aiLogs = CopyOnWriteArrayList(
                listOf(
                    TaskActivityLogItem(
                        id = "log-01",
                        taskId = sampleAiTask.id,
                        tenantId = sampleAiTask.tenantId,
                        actorId = "agent-market-intel",
                        actorType = "ai_agent",
                        action = "created_self_initiated",
                        detail = "Task diinisiasi secara otonom oleh AI Agent setelah mendeteksi jadwal pemantauan aktif",
                        createdAt = System.currentTimeMillis() - 3600000L
                    ),
                    TaskActivityLogItem(
                        id = "log-02",
                        taskId = sampleAiTask.id,
                        tenantId = sampleAiTask.tenantId,
                        actorId = "agent-market-intel",
                        actorType = "ai_agent",
                        action = "node_started",
                        detail = "Membuka headless browser dan navigasi ke URL target kompetitor",
                        createdAt = System.currentTimeMillis() - 2400000L
                    ),
                    TaskActivityLogItem(
                        id = "log-03",
                        taskId = sampleAiTask.id,
                        tenantId = sampleAiTask.tenantId,
                        actorId = "agent-market-intel",
                        actorType = "ai_agent",
                        action = "progress_updated",
                        detail = "Berhasil mengekstrak 45 SKU awal dari katalog Tokopedia",
                        createdAt = System.currentTimeMillis() - 1200000L
                    )
                )
            )
            activityLogsStore[sampleAiTask.id] = aiLogs

            // Seed attachment
            val aiAttachments = CopyOnWriteArrayList(
                listOf(
                    TaskAttachmentItem(
                        id = "att-01",
                        taskId = sampleAiTask.id,
                        tenantId = sampleAiTask.tenantId,
                        fileName = "competitor_price_matrix_sample.csv",
                        fileUrl = "https://storage.orchestree.biz.id/v1/object/public/file_artifacts/tenant-enterprise-001/competitor_price_matrix_sample.csv",
                        fileSizeBytes = 14280L,
                        uploadedBy = "agent-market-intel"
                    )
                )
            )
            attachmentsStore[sampleAiTask.id] = aiAttachments
        }

        val defaultInstance: TaskRepository by lazy { TaskRepository() }
    }

    val taskRepo: TaskRepository get() = this

    fun recordTaskAiCollaboration(taskId: String, aiJobTitleId: String) {
        taskAiCollaborationScopeStore.computeIfAbsent(taskId) { ConcurrentHashMap.newKeySet() }.add(aiJobTitleId)
    }

    fun getTaskAiCollaborationScope(taskId: String): Set<String> {
        return taskAiCollaborationScopeStore[taskId] ?: emptySet()
    }

    fun getAllByTenant(tenantId: String, boardId: String = "default"): List<Task> {
        return tasksStore.values
            .filter { it.tenantId == tenantId && (it.boardId == null || it.boardId == boardId || boardId == "default") }
            .toList()
    }

    fun isDeptManagerOf(userId: String, departmentId: String): Boolean {
        val user = userRepo.getSync(userId)
        val persona = userPersonaRepo.getByUserIdSync(userId)
        val normalizedTarget = departmentId.lowercase().replace("dept-", "").replace(" ", "_")
        if (user != null && user.departmentId.lowercase().replace("dept-", "").replace(" ", "_") == normalizedTarget &&
            (user.role == "DEPT_MANAGER" || persona?.jobLevelCode in listOf("manajer", "supervisor"))) {
            return true
        }
        val profile = StaffProfileRepository.defaultInstance.findByIdSync(userId)
        if (profile != null && profile.departmentId?.lowercase()?.replace("dept-", "")?.replace(" ", "_") == normalizedTarget &&
            (profile.jobTitle.contains("Manager", ignoreCase = true) || profile.jobTitle.contains("Supervisor", ignoreCase = true))) {
            return true
        }
        return false
    }

    fun getByDepartment(tenantId: String, departmentId: String, boardId: String = "default"): List<Task> {
        val normalizedDept = departmentId.lowercase().replace("dept-", "").replace(" ", "_")
        return tasksStore.values
            .filter { task ->
                task.tenantId == tenantId &&
                (task.boardId == null || task.boardId == boardId || boardId == "default") &&
                task.departmentId != null &&
                (task.departmentId.equals(departmentId, ignoreCase = true) ||
                 task.departmentId.lowercase().replace("dept-", "").replace(" ", "_") == normalizedDept)
            }
            .toList()
    }

    fun getByAssignee(userId: String, boardId: String = "default"): List<Task> {
        return tasksStore.values
            .filter { task ->
                task.assigneeId == userId &&
                (task.boardId == null || task.boardId == boardId || boardId == "default")
            }
            .toList()
    }

    fun getByTeam(userId: String, teamId: String, boardId: String = "default"): List<Task> {
        val normalizedTeam = teamId.lowercase().replace("dept-", "").replace(" ", "_")
        return tasksStore.values
            .filter { task ->
                (task.boardId == null || task.boardId == boardId || boardId == "default") &&
                (
                    task.teamId?.equals(teamId, ignoreCase = true) == true ||
                    (task.teamId == null && task.departmentId != null && 
                     task.departmentId.lowercase().replace("dept-", "").replace(" ", "_") == normalizedTeam)
                )
            }
            .toList()
    }

    fun getByCollaboratingAiAgents(userId: String, collaboratingAiJobTitleIds: List<String>?, boardId: String = "default"): List<Task> {
        if (collaboratingAiJobTitleIds.isNullOrEmpty()) return emptyList()
        val normalizedAiCodes = collaboratingAiJobTitleIds.map { it.lowercase().trim() }.toSet()

        return tasksStore.values
            .filter { task ->
                (task.boardId == null || task.boardId == boardId || boardId == "default") &&
                (task.createdByType == "ai_agent_self_initiated" || task.assigneeType == "AI_AGENT") &&
                (
                    normalizedAiCodes.any { code ->
                        task.aiJobTitleId?.equals(code, ignoreCase = true) == true ||
                        task.assigneeId.contains(code, ignoreCase = true) ||
                        task.departmentId?.lowercase()?.replace("dept-", "")?.contains(code) == true ||
                        taskAiCollaborationScopeStore[task.id]?.contains(code) == true
                    }
                )
            }
            .toList()
    }

    suspend fun getTasksForUser(userId: String, boardId: String = "default"): List<Task> {
        val user = userRepo.get(userId)
        val persona = userPersonaRepo.getByUserId(userId)

        return when {
            persona.jobLevelCode in listOf("owner", "direksi", "vp", "gm") -> {
                // Pimpinan tingkat atas -> AKSES LINTAS SELURUH departemen
                taskRepo.getAllByTenant(user.tenantId, boardId)
            }
            persona.jobLevelCode in listOf("manajer", "supervisor") &&
                taskRepo.isDeptManagerOf(userId, user.departmentId) -> {
                // Manager/Supervisor -> HANYA departemennya sendiri (LINTAS
                // TIM dalam satu departemen yang sama)
                taskRepo.getByDepartment(user.tenantId, user.departmentId, boardId)
            }
            else -> {
                // STAFF BAWAH (staff, magang) -> KOMBINASI 3 SUMBER SAJA:
                val personalTasks = taskRepo.getByAssignee(userId, boardId)
                val collaboratedAiTasks = taskRepo.getByCollaboratingAiAgents(
                    userId, proactiveCollaborationScopeRepo.get(userId).collaboratingAiJobTitleIds, boardId
                )  // task self-created AI Agent (Fase 103) DALAM SCOPE kolaborasi staff ini
                val teamTasks = taskRepo.getByTeam(userId, user.teamId ?: user.departmentId, boardId)
                (personalTasks + collaboratedAiTasks + teamTasks).distinctBy { it.id }
                // TIDAK PERNAH mengembalikan task dari departemen/tim LAIN
            }
        }
    }

    suspend fun getTasks(tenantId: String): Result<String> {
        if (supabase.isConfigured()) {
            val res = supabase.queryTable("tasks", tenantId)
            if (res.isSuccess) return res
        }
        val tenantTasks = tasksStore.values.filter { it.tenantId == tenantId }
        return Result.success(Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Task.serializer()), tenantTasks))
    }

    suspend fun createTask(tenantId: String, payloadJson: String): Result<String> {
        if (supabase.isConfigured()) {
            val res = supabase.insertRecord("tasks", tenantId, payloadJson)
            if (res.isSuccess) return res
        }
        return Result.success(payloadJson)
    }

    suspend fun createTask(task: Task): Task {
        tasksStore[task.id] = task
        if (supabase.isConfigured()) {
            try {
                val payload = buildJsonObject {
                    put("id", task.id)
                    put("tenant_id", task.tenantId)
                    put("title", task.title)
                    put("description", task.description)
                    put("column_name", task.columnName)
                    put("priority", task.priority)
                    put("assignee_type", task.assigneeType)
                    put("assignee_id", task.assigneeId)
                    put("assignee_name", task.assigneeName)
                    put("due_date", task.dueDate)
                    put("progress_pct", task.progressPct)
                    put("live_status_line", task.liveStatusLine)
                    put("workflow_execution_id", task.workflowExecutionId)
                    put("created_by_type", task.createdByType)
                    put("ai_activity_status_line", task.aiActivityStatusLine)
                    put("third_party_monitoring_target", task.thirdPartyMonitoringTarget)
                    put("source_channel", task.sourceChannel)
                    task.departmentId?.let { put("department_id", it) }
                }.toString()
                supabase.insertRecord("tasks", task.tenantId, payload)
            } catch (e: Exception) {
                logger.warn("Supabase insert task failed, local store retained: ${e.message}")
            }
        }
        return task
    }

    suspend fun updateTask(task: Task): Task {
        tasksStore[task.id] = task
        if (supabase.isConfigured()) {
            try {
                val payload = buildJsonObject {
                    put("title", task.title)
                    put("column_name", task.columnName)
                    put("progress_pct", task.progressPct)
                    put("live_status_line", task.liveStatusLine)
                    put("ai_activity_status_line", task.aiActivityStatusLine)
                    put("workflow_execution_id", task.workflowExecutionId)
                }.toString()
                supabase.updateRecord("tasks", task.tenantId, "id=eq.${task.id}", payload)
            } catch (e: Exception) {
                logger.warn("Supabase update task failed, local store retained: ${e.message}")
            }
        }
        return task
    }

    suspend fun getById(taskId: String): Task? {
        return tasksStore[taskId]
    }

    suspend fun findByWorkflowExecutionId(workflowExecutionId: String): Task? {
        return tasksStore.values.firstOrNull { it.workflowExecutionId == workflowExecutionId }
    }

    suspend fun findActiveByMonitoringTarget(agentId: String, monitoringTarget: String?): Task? {
        if (monitoringTarget.isNullOrBlank()) return null
        return tasksStore.values.firstOrNull {
            it.thirdPartyMonitoringTarget == monitoringTarget &&
                (it.columnName == TaskColumn.IN_PROGRESS.name || it.columnName == TaskColumn.TODO.name)
        }
    }

    suspend fun addChecklistItem(item: TaskChecklistItem): TaskChecklistItem {
        val list = checklistsStore.computeIfAbsent(item.taskId) { CopyOnWriteArrayList() }
        list.add(item)
        if (supabase.isConfigured()) {
            try {
                val payload = buildJsonObject {
                    put("id", item.id)
                    put("task_id", item.taskId)
                    put("tenant_id", item.tenantId)
                    put("item_text", item.itemText)
                    put("is_completed", item.isCompleted)
                    put("order_index", item.orderIndex)
                    item.completedById?.let { put("completed_by_id", it) }
                    item.completedByType?.let { put("completed_by_type", it) }
                }.toString()
                supabase.insertRecord("task_checklists", item.tenantId, payload)
            } catch (e: Exception) {
                logger.warn("Supabase insert checklist item failed: ${e.message}")
            }
        }
        return item
    }

    suspend fun getChecklists(taskId: String): List<TaskChecklistItem> {
        return checklistsStore[taskId] ?: emptyList()
    }

    suspend fun recordActivityLog(log: TaskActivityLogItem): TaskActivityLogItem {
        val list = activityLogsStore.computeIfAbsent(log.taskId) { CopyOnWriteArrayList() }
        list.add(log)
        if (supabase.isConfigured()) {
            try {
                val payload = buildJsonObject {
                    put("id", log.id)
                    put("task_id", log.taskId)
                    put("tenant_id", log.tenantId)
                    log.actorId?.let { put("actor_id", it) }
                    put("actor_type", log.actorType)
                    put("action", log.action)
                    put("detail", log.detail)
                }.toString()
                supabase.insertRecord("task_activity_log", log.tenantId, payload)
            } catch (e: Exception) {
                logger.warn("Supabase insert activity log failed: ${e.message}")
            }
        }
        return log
    }

    suspend fun getActivityLogs(taskId: String): List<TaskActivityLogItem> {
        return activityLogsStore[taskId] ?: emptyList()
    }

    suspend fun listAllTasks(tenantId: String? = null): List<Task> {
        return if (tenantId != null) {
            tasksStore.values.filter { it.tenantId == tenantId }
        } else {
            tasksStore.values.toList()
        }
    }

    suspend fun listAll(): List<Task> = listAllTasks(null)
    suspend fun listByTenant(tenantId: String): List<Task> = listAllTasks(tenantId)
    suspend fun findByWorkflowExecution(workflowExecutionId: String): Task? = findByWorkflowExecutionId(workflowExecutionId)

    suspend fun updateActivityStatusLine(taskId: String, activityStatusLine: String): Task? {
        val existing = tasksStore[taskId] ?: return null
        val updated = existing.copy(
            aiActivityStatusLine = activityStatusLine,
            liveStatusLine = activityStatusLine
        )
        return updateTask(updated)
    }

    suspend fun create(
        title: String,
        assigneeId: String = "",
        createdByType: String = "human",
        column: TaskColumn = TaskColumn.IN_PROGRESS,
        aiActivityStatusLine: String? = null,
        thirdPartyMonitoringTarget: String? = null,
        departmentId: String? = null,
        sourceChannel: String = "dashboard",
        dueDate: String = "",
        priority: String = "MEDIUM",
        workflowExecutionId: String? = null,
        tenantId: String = "tenant-enterprise-001"
    ): Task {
        val taskId = "tsk-${java.util.UUID.randomUUID().toString().take(8)}"
        val task = Task(
            id = taskId,
            tenantId = tenantId,
            title = title,
            columnName = column.name,
            assigneeType = if (createdByType.startsWith("ai")) "AI_AGENT" else "HUMAN",
            assigneeId = assigneeId,
            assigneeName = if (assigneeId.isNotBlank()) assigneeId else "Unassigned",
            createdByType = createdByType,
            aiActivityStatusLine = aiActivityStatusLine,
            liveStatusLine = aiActivityStatusLine ?: "",
            thirdPartyMonitoringTarget = thirdPartyMonitoringTarget,
            departmentId = departmentId,
            sourceChannel = sourceChannel,
            dueDate = dueDate,
            priority = priority,
            workflowExecutionId = workflowExecutionId
        )
        return createTask(task)
    }

    suspend fun createChecklist(item: TaskChecklistItem): TaskChecklistItem = addChecklistItem(item)
    suspend fun getChecklistsForTask(taskId: String): List<TaskChecklistItem> = getChecklists(taskId)

    suspend fun toggleChecklistCompletion(
        checklistId: String,
        isCompleted: Boolean,
        completedById: String? = null,
        completedByType: String? = null
    ): Boolean {
        for ((_, list) in checklistsStore) {
            val idx = list.indexOfFirst { it.id == checklistId }
            if (idx != -1) {
                val old = list[idx]
                val updated = old.copy(
                    isCompleted = isCompleted,
                    completedById = completedById,
                    completedByType = completedByType
                )
                list[idx] = updated
                if (supabase.isConfigured()) {
                    try {
                        val payload = buildJsonObject {
                            put("is_completed", isCompleted)
                            completedById?.let { put("completed_by_id", it) }
                            completedByType?.let { put("completed_by_type", it) }
                        }.toString()
                        supabase.updateRecord("task_checklists", old.tenantId, "id=eq.$checklistId", payload)
                    } catch (e: Exception) {
                        logger.warn("Supabase toggle checklist failed: ${e.message}")
                    }
                }
                return true
            }
        }
        return false
    }

    suspend fun createActivityLog(item: TaskActivityLogItem): TaskActivityLogItem = recordActivityLog(item)
    suspend fun getActivityLogsForTask(taskId: String): List<TaskActivityLogItem> = getActivityLogs(taskId)

    suspend fun updateTaskDescription(taskId: String, descriptionRichText: String): Result<Task> {
        val existing = tasksStore[taskId] ?: return Result.failure(Exception("Task $taskId not found"))
        val updated = existing.copy(
            descriptionRichText = descriptionRichText,
            description = descriptionRichText
        )
        tasksStore[taskId] = updated
        if (supabase.isConfigured()) {
            try {
                val payload = buildJsonObject {
                    put("description_rich_text", descriptionRichText)
                    put("description", descriptionRichText)
                }.toString()
                supabase.updateRecord("tasks", updated.tenantId, "id=eq.$taskId", payload)
            } catch (e: Exception) {
                logger.warn("Supabase update description failed: ${e.message}")
            }
        }
        return Result.success(updated)
    }

    suspend fun listAttachments(taskId: String): List<TaskAttachmentItem> {
        return attachmentsStore[taskId] ?: emptyList()
    }

    suspend fun addAttachment(item: TaskAttachmentItem): TaskAttachmentItem {
        val list = attachmentsStore.computeIfAbsent(item.taskId) { CopyOnWriteArrayList() }
        list.add(item)
        if (supabase.isConfigured()) {
            try {
                val payload = buildJsonObject {
                    put("id", item.id)
                    put("task_id", item.taskId)
                    put("tenant_id", item.tenantId)
                    put("file_name", item.fileName)
                    put("file_url", item.fileUrl)
                    put("file_size_bytes", item.fileSizeBytes)
                    put("uploaded_by", item.uploadedBy)
                }.toString()
                supabase.insertRecord("task_attachments", item.tenantId, payload)
            } catch (e: Exception) {
                logger.warn("Supabase insert attachment failed: ${e.message}")
            }
        }
        return item
    }

    val taskActivityLogRepo: TaskActivityLogRepository by lazy { TaskActivityLogRepository(this) }
}

class TaskActivityLogRepository(
    private val taskRepo: TaskRepository = TaskRepository.defaultInstance
) {
    suspend fun record(
        taskId: String,
        actorId: String?,
        actorType: String,
        activityType: String,
        description: String,
        tenantId: String = "tenant-enterprise-001"
    ): TaskActivityLogItem {
        val task = taskRepo.getById(taskId)
        val resolvedTenant = task?.tenantId ?: tenantId
        val item = TaskActivityLogItem(
            taskId = taskId,
            tenantId = resolvedTenant,
            actorId = actorId,
            actorType = actorType,
            action = activityType,
            detail = description
        )
        return taskRepo.recordActivityLog(item)
    }
}
