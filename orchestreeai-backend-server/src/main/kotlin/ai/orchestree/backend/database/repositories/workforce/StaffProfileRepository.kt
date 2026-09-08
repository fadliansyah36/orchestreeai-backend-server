package ai.orchestree.backend.database.repositories.workforce

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class StaffProfile(
    val id: String,
    val userId: String,
    val tenantId: String,
    val departmentId: String? = null,
    val jobTitle: String = "Staff Operasional",
    val phone: String = "",
    val telegramChatId: String = "",
    val avatarUrl: String = "",
    val skills: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

typealias StaffProfileModel = StaffProfile

class StaffProfileRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(StaffProfileRepository::class.java)
    private val profiles = ConcurrentHashMap<String, StaffProfile>()

    init {
        // Seed default staff profiles for realistic channel resolution
        profiles["staff-01"] = StaffProfile(
            id = "staff-01",
            userId = "usr-01",
            tenantId = "tenant-enterprise-001",
            departmentId = "dept-ops",
            jobTitle = "Operations Manager",
            phone = "+6281234567890",
            telegramChatId = "11112222"
        )
        profiles["staff-02"] = StaffProfile(
            id = "staff-02",
            userId = "usr-02",
            tenantId = "tenant-enterprise-001",
            departmentId = "dept-sales",
            jobTitle = "Account Executive",
            phone = "+6281987654321",
            telegramChatId = "87654321"
        )
    }

    suspend fun findByTelegramChatId(chatId: String): StaffProfile? {
        val clean = chatId.removePrefix("tg-").trim()
        return profiles.values.reversed().firstOrNull { it.telegramChatId == clean || it.telegramChatId == chatId }
    }

    suspend fun findByPhone(phone: String): StaffProfile? {
        val clean = phone.replace("+", "").replace("-", "").replace(" ", "").trim()
        return profiles.values.reversed().firstOrNull {
            val targetClean = it.phone.replace("+", "").replace("-", "").replace(" ", "").trim()
            targetClean == clean || targetClean.endsWith(clean) || clean.endsWith(targetClean)
        }
    }

    suspend fun findByChannelSender(channel: String, senderId: String): StaffProfile? {
        return when (channel.lowercase()) {
            "telegram", "tg" -> findByTelegramChatId(senderId)
            "whatsapp", "wa" -> findByPhone(senderId)
            else -> profiles.values.reversed().firstOrNull { it.userId == senderId || it.id == senderId }
        }
    }

    suspend fun getStaffProfile(id: String): StaffProfile? = profiles[id]
    suspend fun findById(id: String): StaffProfile? = profiles[id] ?: profiles.values.firstOrNull { it.userId == id }
    fun findByIdSync(id: String): StaffProfile? = profiles[id] ?: profiles.values.firstOrNull { it.userId == id }

    suspend fun save(profile: StaffProfile): StaffProfile {
        if (!profile.telegramChatId.isNullOrBlank()) {
            profiles.values.removeIf { it.id != profile.id && it.telegramChatId == profile.telegramChatId }
        }
        if (!profile.phone.isNullOrBlank()) {
            profiles.values.removeIf { it.id != profile.id && it.phone == profile.phone }
        }
        profiles[profile.id] = profile
        return profile
    }

    companion object {
        val defaultInstance: StaffProfileRepository by lazy { StaffProfileRepository() }
    }
}
