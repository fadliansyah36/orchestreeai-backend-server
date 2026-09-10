package ai.orchestree.backend.database.repositories.workforce

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.identity.TenantRepository
import ai.orchestree.backend.models.UserRole
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class User(
    val id: String,
    val tenantId: String = "",
    val departmentId: String = "sales",
    val teamId: String? = null,
    val name: String = "",
    val email: String = "",
    val role: String = "STAFF_HUMAN"
)

class UserRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    private val staffProfileRepo: StaffProfileRepository = StaffProfileRepository()
) {
    private val logger = LoggerFactory.getLogger(UserRepository::class.java)

    companion object {
        private val usersStore = ConcurrentHashMap<String, User>()
        val defaultInstance: UserRepository by lazy { UserRepository() }

        init {
            seedInitialUsers()
        }

        private fun seedInitialUsers() {
            if (usersStore.isNotEmpty()) return

            // 1. Owner / Direksi
            usersStore["usr-owner-01"] = User(
                id = "usr-owner-01",
                tenantId = TenantRepository.SEED_ENTERPRISE_TENANT_ID, // allowed: in-memory fallback seed store
                departmentId = "executive",
                teamId = "team-c-level",
                name = "Prasetyo Utomo (CEO & Owner)",
                email = "prasetyo@enterprise.co.id",
                role = UserRole.TENANT_OWNER.name
            )

            // 2. Dept Manager Sales
            val mgrSales = User(
                id = "usr-manager-sales-01",
                tenantId = TenantRepository.SEED_ENTERPRISE_TENANT_ID, // allowed: in-memory fallback seed store
                departmentId = "sales",
                teamId = "team-sales-leads",
                name = "Ahmad Dani (Sales Manager)",
                email = "ahmad.dani@enterprise.co.id",
                role = UserRole.DEPT_MANAGER.name
            )
            usersStore["usr-manager-sales-01"] = mgrSales
            usersStore["usr-mgr-sales-01"] = mgrSales.copy(id = "usr-mgr-sales-01")

            // 3. Staff Human Sales
            usersStore["usr-staff-sales-01"] = User(
                id = "usr-staff-sales-01",
                tenantId = TenantRepository.SEED_ENTERPRISE_TENANT_ID, // allowed: in-memory fallback seed store
                departmentId = "sales",
                teamId = "team-sales-field-01",
                name = "Budi Hartono (Sales Representative)",
                email = "budi.hartono@enterprise.co.id",
                role = UserRole.STAFF_HUMAN.name
            )

            // 4. Staff Human Finance
            usersStore["usr-staff-finance-01"] = User(
                id = "usr-staff-finance-01",
                tenantId = TenantRepository.SEED_ENTERPRISE_TENANT_ID, // allowed: in-memory fallback seed store
                departmentId = "finance",
                teamId = "team-finance-accounting",
                name = "Siti Rahmawati (Finance Officer)",
                email = "siti.rahma@enterprise.co.id",
                role = UserRole.STAFF_HUMAN.name
            )

            // 5. Default generic user
            usersStore["usr-01"] = User(
                id = "usr-01",
                tenantId = TenantRepository.SEED_ENTERPRISE_TENANT_ID, // allowed: in-memory fallback seed store
                departmentId = "sales",
                teamId = "team-sales-field-01",
                name = "Standard Staff User",
                email = "staff@enterprise.co.id",
                role = UserRole.STAFF_HUMAN.name
            )
        }
    }

    suspend fun get(userId: String): User {
        return getSync(userId) ?: User(
            id = userId,
            tenantId = "",
            departmentId = "sales",
            name = "User $userId",
            role = "STAFF_HUMAN"
        )
    }

    fun getSync(userId: String): User? {
        val existing = usersStore[userId]
        if (existing != null) return existing

        // Fallback to StaffProfileRepository
        val staffProfile = staffProfileRepo.findByIdSync(userId)
        if (staffProfile != null) {
            val user = User(
                id = staffProfile.userId.ifBlank { staffProfile.id },
                tenantId = staffProfile.tenantId,
                departmentId = staffProfile.departmentId ?: "default",
                name = staffProfile.jobTitle,
                role = if (staffProfile.jobTitle.contains("Manager", ignoreCase = true)) UserRole.DEPT_MANAGER.name else UserRole.STAFF_HUMAN.name
            )
            usersStore[userId] = user
            return user
        }

        return null
    }

    fun save(user: User) {
        usersStore[user.id] = user
    }

    fun getAll(): List<User> = usersStore.values.toList()

    fun countActiveByTenant(tenantId: String): Int {
        return usersStore.values.count { it.tenantId == tenantId }
    }

    fun findByTenant(tenantId: String): List<User> {
        return usersStore.values.filter { it.tenantId == tenantId }
    }

    fun delete(userId: String): Boolean {
        return usersStore.remove(userId) != null
    }
}
