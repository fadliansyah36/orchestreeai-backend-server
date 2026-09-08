package ai.orchestree.backend.channels

import ai.orchestree.backend.channels.isolation.ChannelIsolationEngine
import ai.orchestree.backend.database.repositories.taskboard.TaskActivityLogRecord
import ai.orchestree.backend.database.repositories.taskboard.TaskChecklistRecord
import ai.orchestree.backend.database.repositories.taskboard.TaskColumn
import ai.orchestree.backend.database.repositories.taskboard.TaskRecord
import ai.orchestree.backend.database.repositories.taskboard.TaskRepository
import ai.orchestree.backend.database.repositories.workforce.StaffProfileRepository
import ai.orchestree.backend.intelligence.IntentCategory
import ai.orchestree.backend.intelligence.IntentClassifier
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.orchestration.OrchestrationEngine
import ai.orchestree.backend.resilience.executeWithRetry
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class InboundProactiveMessageResult(
    val processed: Boolean = true,
    val success: Boolean = true,
    val intent: String,
    val taskId: String? = null,
    val taskTitle: String? = null,
    val taskCreated: TaskRecord? = null,
    val replyText: String,
    val sourceChannel: String
)

typealias ChannelHandlingResult = InboundProactiveMessageResult

class ChannelGateway(
    private val isolationEngine: ChannelIsolationEngine = ChannelIsolationEngine(),
    private val modelRouter: ModelRouter = ModelRouter(),
    private val staffProfileRepo: StaffProfileRepository = StaffProfileRepository.defaultInstance
) {
    private val logger = LoggerFactory.getLogger(ChannelGateway::class.java)

    suspend fun processInbound(message: InboundMessage): OutboundMessage? {
        logger.info("Processing Inbound message from [${message.channelType}] sender: ${message.senderId} (tenant: ${message.tenantId})")

        if (message.channelAccountId != null) {
            val isOwned = isolationEngine.validateTenantOwnership(message.tenantId, message.channelAccountId)
            if (!isOwned) {
                logger.error("Channel isolation violation: tenant ${message.tenantId} does not own account ${message.channelAccountId}")
                return null
            }
        }

        // Generate Automated Response via ModelRouter with unified retry resilience and Commercial Credit Lifecycle
        val prompt = "Anda adalah Asisten AI Bisnis Pelanggan. Balas pesan pelanggan berikut secara ramah, profesional, dan ringkas:\n'${message.text}'"
        val activeProvider = ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository.instance
            .getLlmProvidersOrderedByFallbackPriority().firstOrNull()?.providerCode?.lowercase() ?: "openrouter"
        val defaultActiveModel = when (activeProvider.uppercase()) {
            "GROQ" -> "llama-3.3-70b-versatile"
            "OPENROUTER" -> "anthropic/claude-3.5-sonnet"
            "DEEPSEEK" -> "deepseek-chat"
            "ANTHROPIC" -> "claude-3-5-sonnet-20241022"
            else -> "anthropic/claude-3.5-sonnet"
        }

        val replyText = try {
            val costContext = ai.orchestree.backend.billing.CreditCostContext(
                activityType = "omnichannel_ai_task",
                modelUsed = defaultActiveModel,
                toolsInvoked = 0,
                complexityLevel = "standard",
                executionType = "interactive"
            )
            ai.orchestree.backend.billing.CommercialCreditEngine.defaultInstance.executeWithCreditLifecycle<String>(
                tenantId = message.tenantId,
                context = costContext
            ) {
                val generated = executeWithRetry(
                    maxAttempts = 3,
                    initialDelayMs = 500L,
                    backoffMultiplier = 2.0
                ) {
                    val res = modelRouter.execute(
                        ModelRouteRequest(
                            taskCategory = "GENERAL_CHAT",
                            prompt = prompt,
                            tenantId = message.tenantId
                        )
                    )
                    if (res.isSuccess) {
                        res.getOrThrow().text
                    } else {
                        throw res.exceptionOrNull() ?: RuntimeException("Channel gateway response generation failed")
                    }
                }
                Pair(
                    generated,
                    ai.orchestree.backend.billing.TaskExecutionResult(
                        referenceId = message.channelAccountId ?: "omnichannel-inbound",
                        llmUsageDetail = mapOf("total_tokens" to 150)
                    )
                )
            }
        } catch (e: Throwable) {
            logger.warn("CommercialCreditEngine lifecycle failed (${e.message}), executing model router directly for omnichannel inbound: ${e.message}")
            val res = modelRouter.execute(
                ModelRouteRequest(
                    taskCategory = "GENERAL_CHAT",
                    prompt = prompt,
                    tenantId = message.tenantId
                )
            )
            if (res.isSuccess) {
                res.getOrThrow().text
            } else {
                "Halo! Terima kasih telah menghubungi kami. Tim kami akan segera menindaklanjuti pesan Anda."
            }
        }

        return OutboundMessage(
            tenantId = message.tenantId,
            channelType = message.channelType,
            recipientId = message.senderId,
            text = replyText
        )
    }

    /**
     * LANGKAH 4: Inbound Proactive Channel Integration
     * Menangani pesan inbound dari Telegram, WhatsApp, atau Dashboard.
     * Mengklasifikasikan intent, dan jika merupakan perintah pembuatan tugas (IntentCategory.TASK_CREATION_COMMAND),
     * secara otomatis membuat task di papan Kanban, dekomposisi checklist, dan pencatatan activity log.
     */
    suspend fun handleInboundProactiveChannelMessage(
        tenantId: String,
        channelType: String,
        senderId: String,
        senderName: String? = null,
        messageText: String,
        orchestrationEngine: OrchestrationEngine? = null,
        taskRepo: TaskRepository = TaskRepository.defaultInstance
    ): InboundProactiveMessageResult {
        val normalizedChannel = when (channelType.lowercase()) {
            "telegram", "tg" -> "telegram"
            "whatsapp", "wa" -> "whatsapp"
            else -> "dashboard"
        }

        val intentClassifier = IntentClassifier(modelRouter)
        val classified = intentClassifier.classify(messageText, tenantId)
        val classifiedCat = intentClassifier.classifyCategory(messageText)
        val isTaskCommand = classified.intentCode == IntentCategory.TASK_CREATION_COMMAND.name ||
                classified.intentCode == IntentCategory.TASK_CREATION.name ||
                classifiedCat == IntentCategory.TASK_CREATION_COMMAND

        val staffUser = staffProfileRepo.findByChannelSender(normalizedChannel, senderId)
        val staffUserId = staffUser?.userId ?: senderId
        val staffName = senderName ?: staffUser?.jobTitle ?: "Staff ($senderId)"

        if (classifiedCat == IntentCategory.DATA_SELECTION_REQUEST || classified.intentCode == IntentCategory.DATA_SELECTION_REQUEST.name) {
            val selectionRepo = ai.orchestree.backend.database.repositories.selection.SelectionRepository(modelRouter = modelRouter)
            val selectionEngine = ai.orchestree.backend.intelligence.SelectionEngine(modelRouter = modelRouter, selectionRepo = selectionRepo)
            val reqRecord = ai.orchestree.backend.database.repositories.selection.SelectionRequestRecord(
                tenant_id = tenantId,
                requested_by_user_id = staffUserId,
                prompt_text = messageText,
                source_type = normalizedChannel
            )
            val createdReq = selectionRepo.createSelectionRequest(reqRecord).getOrDefault(reqRecord)
            selectionEngine.processPromptOnlySelection(createdReq.id, tenantId, messageText)
            val results = selectionRepo.getSelectionResults(createdReq.id, tenantId)
            val topResultsSummary = if (results.isNotEmpty()) {
                val top3 = results.take(3).joinToString("\n") { r ->
                    "• Rank #${r.rank_position}: Skor ${r.total_score} [${r.recommendation_classification.uppercase()}] - ${r.ai_insight_text?.take(80) ?: ""}"
                }
                "\n\n🏆 *Hasil Seleksi Utama:*\n$top3\n\nID Request: ${createdReq.id}"
            } else {
                "\n\nPermintaan seleksi telah diproses (ID: ${createdReq.id})."
            }

            val reply = "🔍 *Analisis Seleksi AI Selesai* ($normalizedChannel)\nPermintaan: \"${messageText.take(60)}...\"$topResultsSummary"
            return InboundProactiveMessageResult(
                processed = true,
                intent = IntentCategory.DATA_SELECTION_REQUEST.name,
                taskId = null,
                taskTitle = "Seleksi: ${messageText.take(50)}",
                replyText = reply,
                sourceChannel = normalizedChannel
            )
        } else if (classifiedCat == IntentCategory.MANAGEMENT_QUERY || classified.intentCode == IntentCategory.MANAGEMENT_QUERY.name) {
            val queryEngine = ai.orchestree.backend.intelligence.ManagementQueryEngine(modelRouter = modelRouter)
            val queryRes = queryEngine.handleQuery(
                tenantId = tenantId,
                staffId = staffUser?.id ?: staffUserId,
                staffProfile = staffUser,
                queryText = messageText
            )
            return InboundProactiveMessageResult(
                processed = true,
                intent = IntentCategory.MANAGEMENT_QUERY.name,
                taskId = null,
                taskTitle = null,
                replyText = queryRes.responseText,
                sourceChannel = normalizedChannel
            )
        } else if (isTaskCommand) {
            // 1. Ekstrak entity dari pesan (title, priority, due_date, assignee jika disebut)
            var cleanText = messageText.trim()
            for (prefix in listOf("/task", "/tugas", "buat task", "buat tugas", "buatkan tugas", "tambahkan task")) {
                if (cleanText.startsWith(prefix, ignoreCase = true)) {
                    cleanText = cleanText.substring(prefix.length).trim().removePrefix(":").trim()
                    break
                }
            }
            val titleCandidate = cleanText.lines().firstOrNull()?.take(100) ?: "Tugas Baru dari $normalizedChannel"
            val title = if (titleCandidate.isBlank()) "Tugas Baru dari $normalizedChannel" else titleCandidate

            val priority = when {
                messageText.contains("urgent", ignoreCase = true) || messageText.contains("prioritas tinggi", ignoreCase = true) || messageText.contains("high", ignoreCase = true) -> "HIGH"
                messageText.contains("rendah", ignoreCase = true) || messageText.contains("low", ignoreCase = true) -> "LOW"
                else -> "MEDIUM"
            }

            val dueDate = when {
                messageText.contains("besok", ignoreCase = true) -> "Besok"
                messageText.contains("lusa", ignoreCase = true) -> "Lusa"
                messageText.contains("minggu depan", ignoreCase = true) -> "Minggu Depan"
                messageText.contains("hari ini", ignoreCase = true) -> "Hari ini"
                else -> ""
            }

            val resolvedAssigneeId = when {
                messageText.contains("radar", ignoreCase = true) || messageText.contains("kompetitor", ignoreCase = true) -> "agent-radar-competitor"
                messageText.contains("chief", ignoreCase = true) || messageText.contains("cos", ignoreCase = true) -> "agent-chief-of-staff"
                messageText.contains("budi", ignoreCase = true) -> "staff-01"
                messageText.contains("sari", ignoreCase = true) -> "staff-02"
                else -> staffUser?.userId ?: "agent-orchestrator"
            }

            // 3. Buat task baru di tasks table
            val task = taskRepo.create(
                title = title,
                assigneeId = resolvedAssigneeId,
                createdByType = "human",
                column = TaskColumn.TODO,
                aiActivityStatusLine = "Dibuat via $normalizedChannel oleh $staffName",
                sourceChannel = normalizedChannel,
                dueDate = dueDate,
                priority = priority,
                departmentId = staffUser?.departmentId ?: "dept-ops",
                tenantId = tenantId
            )

            // Checklist dekomposisi
            taskRepo.createChecklist(
                TaskChecklistRecord(
                    taskId = task.id,
                    tenantId = tenantId,
                    itemText = "Analisis dan klarifikasi kebutuhan tugas dari $normalizedChannel",
                    orderIndex = 1
                )
            )
            taskRepo.createChecklist(
                TaskChecklistRecord(
                    taskId = task.id,
                    tenantId = tenantId,
                    itemText = "Eksekusi tindakan tugas oleh $resolvedAssigneeId",
                    orderIndex = 2
                )
            )
            taskRepo.createChecklist(
                TaskChecklistRecord(
                    taskId = task.id,
                    tenantId = tenantId,
                    itemText = "Kirim laporan penyelesaian kembali ke $normalizedChannel",
                    orderIndex = 3
                )
            )

            // 4. Catat task_activity_log:
            taskRepo.taskActivityLogRepo.record(
                taskId = task.id,
                actorId = staffUser?.userId ?: staffUserId,
                actorType = "human",
                activityType = "created",
                description = "Dibuat via $normalizedChannel",
                tenantId = tenantId
            )

            // 5. Kirim balasan konfirmasi ke channel pengirim:
            val dueDisplay = if (task.dueDate.isNotBlank()) task.dueDate else "Belum ditentukan"
            val assigneeDisplay = task.assigneeName.ifBlank { task.assigneeId }
            val reply = "✅ Task *${task.title}* berhasil dibuat di Kanban Board (ID: ${task.id}).\nAssignee: $assigneeDisplay | Priority: ${task.priority} | Due: $dueDisplay"

            // 6. Return ChannelHandlingResult(success=true, taskCreated=task)
            return ChannelHandlingResult(
                processed = true,
                success = true,
                intent = IntentCategory.TASK_CREATION_COMMAND.name,
                taskId = task.id,
                taskTitle = task.title,
                taskCreated = task,
                replyText = reply,
                sourceChannel = normalizedChannel
            )
        } else {
            // General inbound response
            val outbound = processInbound(
                InboundMessage(
                    tenantId = tenantId,
                    channelType = normalizedChannel.uppercase(),
                    senderId = senderId,
                    text = messageText
                )
            )
            return InboundProactiveMessageResult(
                processed = true,
                intent = classified.intentCode,
                taskId = null,
                taskTitle = null,
                replyText = outbound?.text ?: "Pesan berhasil diproses.",
                sourceChannel = normalizedChannel
            )
        }
    }
}
