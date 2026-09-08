package ai.orchestree.backend.models

import java.time.Instant

/**
 * Workforce & Department Domain Models (PRD 8.4 & 5.2.1)
 */
data class DepartmentDto(
    val id: String,
    val tenantId: String,
    val name: String,
    val description: String,
    val parentDepartmentId: String? = null,
    val managerUserId: String? = null,
    val managerName: String = "",
    val colorHex: String = "#1E6FE0",
    val staffCount: Int = 0,
    val activeAgentsCount: Int = 0,
    val activeTasksCount: Int = 0,
    val createdAt: Instant = Instant.now()
)

data class CreateDepartmentRequest(
    val name: String,
    val description: String,
    val parentDepartmentId: String? = null,
    val managerUserId: String? = null,
    val colorHex: String = "#1E6FE0"
)

data class UpdateDepartmentRequest(
    val name: String? = null,
    val description: String? = null,
    val parentDepartmentId: String? = null,
    val managerUserId: String? = null,
    val colorHex: String? = null
)

data class BulkReassignStaffRequest(
    val fromDepartmentId: String,
    val toDepartmentId: String,
    val userIds: List<String>? = null // null means reassign all
)

data class StaffProfileDto(
    val id: String,
    val userId: String,
    val tenantId: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val departmentId: String? = null,
    val departmentName: String = "",
    val jobTitle: String = "Staff Operasional",
    val phone: String = "",
    val telegramChatId: String = "",
    val avatarUrl: String = "",
    val skills: List<String> = emptyList(),
    val status: String = "ACTIVE",
    val createdAt: Instant = Instant.now()
)

data class CreateStaffRequest(
    val email: String,
    val name: String,
    val password: String = "OrchestreeStaff2026!",
    val role: UserRole = UserRole.STAFF_HUMAN,
    val departmentId: String? = null,
    val jobTitle: String = "Staff Operasional",
    val phone: String = "",
    val telegramChatId: String = "",
    val skills: List<String> = emptyList()
)

data class UpdateStaffRequest(
    val name: String? = null,
    val role: UserRole? = null,
    val departmentId: String? = null,
    val jobTitle: String? = null,
    val phone: String? = null,
    val telegramChatId: String? = null,
    val skills: List<String>? = null,
    val status: String? = null
)

data class AiAgentDto(
    val id: String,
    val tenantId: String,
    val name: String,
    val roleTitle: String,
    val departmentId: String? = null,
    val departmentName: String = "",
    val avatarIcon: String = "robot",
    val status: String = "ONLINE",
    val skills: List<String> = emptyList(),
    val toolsGranted: List<String> = emptyList(),
    val currentLiveAction: String = "Siap menerima penugasan",
    val completedTasksCount: Int = 0,
    val qualityRating: Double = 95.0,
    val uptimePercent: Double = 99.9,
    val createdAt: Instant = Instant.now()
)

data class RegisterAiAgentRequest(
    val name: String,
    val roleTitle: String,
    val departmentId: String? = null,
    val skills: List<String> = emptyList(),
    val toolsGranted: List<String> = emptyList(),
    val avatarIcon: String = "robot"
)

data class BoardDto(
    val id: String,
    val tenantId: String,
    val name: String,
    val description: String,
    val isDefault: Boolean,
    val columns: List<BoardColumnDto> = emptyList()
)

data class BoardColumnDto(
    val id: String,
    val boardId: String,
    val name: String,
    val label: String,
    val position: Int,
    val colorHex: String,
    val isDefault: Boolean
)

data class TaskDto(
    val id: String,
    val tenantId: String,
    val departmentId: String? = null,
    val departmentName: String = "",
    val title: String,
    val description: String,
    val column: String = "TODO",
    val priority: String = "MEDIUM",
    val assigneeType: String = "AI_AGENT", // "AI_AGENT" or "HUMAN"
    val assigneeId: String,
    val assigneeName: String,
    val dueDate: String = "",
    val progressPct: Int = 0,
    val liveStatusLine: String = "",
    val requiresApproval: Boolean = false,
    val isApproved: Boolean = false,
    val riskReason: String? = null,
    val outputResult: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class CreateTaskRequest(
    val departmentId: String? = null,
    val title: String,
    val description: String,
    val column: String = "TODO",
    val priority: String = "MEDIUM",
    val assigneeType: String = "AI_AGENT",
    val assigneeId: String,
    val assigneeName: String,
    val dueDate: String = "",
    val requiresApproval: Boolean = false,
    val riskReason: String? = null
)

data class MoveTaskRequest(
    val targetColumn: String,
    val actorId: String,
    val actorName: String,
    val actorRole: String,
    val reason: String? = null
)

data class TaskEventDto(
    val id: String,
    val taskId: String,
    val actorId: String,
    val actorName: String,
    val actorRole: String,
    val eventType: String,
    val fromColumn: String?,
    val toColumn: String?,
    val details: String,
    val createdAt: Instant
)
