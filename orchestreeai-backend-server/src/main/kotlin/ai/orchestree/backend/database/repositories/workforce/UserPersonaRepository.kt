package ai.orchestree.backend.database.repositories.workforce

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class UserPersona(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String,
    val tenantId: String = "",
    val displayName: String = "",
    val jobLevel: String = "Staff",
    val jobLevelCode: String = normalizeJobLevelCode(jobLevel),
    val jobSubTitle: String? = null
) {
    companion object {
        fun normalizeJobLevelCode(level: String): String {
            val l = level.lowercase().trim()
            return when {
                l.contains("owner") || l.contains("founder") -> "owner"
                l.contains("direk") || l.contains("director") || l.contains("ceo") || l.contains("c-level") || l.contains("komisaris") -> "direksi"
                l.contains("vp") || l.contains("vice president") -> "vp"
                l.contains("gm") || l.contains("general manager") -> "gm"
                l.contains("manajer") || l.contains("manager") -> "manajer"
                l.contains("supervisor") || l.contains("spv") -> "supervisor"
                l.contains("magang") || l.contains("intern") -> "magang"
                else -> "staff"
            }
        }
    }
}

class UserPersonaRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val staffProfileRepo: StaffProfileRepository = StaffProfileRepository()
) {
    private val logger = LoggerFactory.getLogger(UserPersonaRepository::class.java)

    companion object {
        private val personasStore = ConcurrentHashMap<String, UserPersona>()

        init {
            seedInitialPersonas()
        }

        private fun seedInitialPersonas() {
            if (personasStore.isNotEmpty()) return

            personasStore["usr-owner-01"] = UserPersona(
                userId = "usr-owner-01",
                tenantId = "tenant-enterprise-001",
                displayName = "Prasetyo Utomo",
                jobLevel = "Owner / Founder",
                jobLevelCode = "owner",
                jobSubTitle = "CEO & President Director"
            )

            val mgrPersona = UserPersona(
                userId = "usr-manager-sales-01",
                tenantId = "tenant-enterprise-001",
                displayName = "Ahmad Dani",
                jobLevel = "Manajer",
                jobLevelCode = "manajer",
                jobSubTitle = "Head of Sales Department"
            )
            personasStore["usr-manager-sales-01"] = mgrPersona
            personasStore["usr-mgr-sales-01"] = mgrPersona.copy(userId = "usr-mgr-sales-01")

            personasStore["usr-staff-sales-01"] = UserPersona(
                userId = "usr-staff-sales-01",
                tenantId = "tenant-enterprise-001",
                displayName = "Budi Hartono",
                jobLevel = "Staff",
                jobLevelCode = "staff",
                jobSubTitle = "Field Sales Specialist"
            )

            personasStore["usr-staff-finance-01"] = UserPersona(
                userId = "usr-staff-finance-01",
                tenantId = "tenant-enterprise-001",
                displayName = "Siti Rahmawati",
                jobLevel = "Staff",
                jobLevelCode = "staff",
                jobSubTitle = "Finance Officer"
            )

            personasStore["usr-01"] = UserPersona(
                userId = "usr-01",
                tenantId = "tenant-enterprise-001",
                displayName = "Standard Staff",
                jobLevel = "Staff",
                jobLevelCode = "staff"
            )
        }

        val defaultInstance: UserPersonaRepository by lazy { UserPersonaRepository() }
    }

    suspend fun getByUserId(userId: String): UserPersona {
        return getByUserIdSync(userId) ?: UserPersona(
            userId = userId,
            tenantId = "",
            displayName = "User $userId",
            jobLevel = "Staff",
            jobLevelCode = "staff"
        )
    }

    fun getByUserIdSync(userId: String): UserPersona? {
        val existing = personasStore[userId]
        if (existing != null) return existing

        // Check staff profile
        val profile = staffProfileRepo.findByIdSync(userId)
        if (profile != null) {
            val code = UserPersona.normalizeJobLevelCode(profile.jobTitle)
            val persona = UserPersona(
                userId = userId,
                tenantId = profile.tenantId,
                displayName = profile.jobTitle,
                jobLevel = profile.jobTitle,
                jobLevelCode = code
            )
            personasStore[userId] = persona
            return persona
        }

        return null
    }

    fun save(persona: UserPersona) {
        personasStore[persona.userId] = persona
    }

    fun getAll(): List<UserPersona> = personasStore.values.toList()
}
