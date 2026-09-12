package ai.orchestree.backend.channels.isolation

import ai.orchestree.backend.channels.ChannelAccountConfig
import ai.orchestree.backend.database.repositories.workforce.StaffProfileRepository
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

enum class AudienceScope {
    CUSTOMER_FACING, // Omnichannel Sales & Marketing (Melayani Calon Pembeli & Pelanggan Eksternal)
    INTERNAL_STAFF   // Proactive Daily & Management (Melayani & Mempelajari Perusahaan & Staf Internal)
}

class ChannelIsolationEngine(
    private val staffProfileRepo: StaffProfileRepository = StaffProfileRepository.defaultInstance
) {
    private val logger = LoggerFactory.getLogger(ChannelIsolationEngine::class.java)
    private val tenantAccounts = ConcurrentHashMap<String, MutableMap<String, ChannelAccountConfig>>()

    fun registerAccount(account: ChannelAccountConfig) {
        val map = tenantAccounts.getOrPut(account.tenantId) { mutableMapOf() }
        map[account.accountId] = account
    }

    fun getAccount(tenantId: String, accountId: String): ChannelAccountConfig? {
        return tenantAccounts[tenantId]?.get(accountId)
    }

    fun validateTenantOwnership(tenantId: String, accountId: String): Boolean {
        val account = tenantAccounts[tenantId]?.get(accountId)
        if (account == null) {
            logger.warn("Channel account $accountId is not owned by tenant $tenantId (Isolation check failed)")
            return false
        }
        return true
    }

    /**
     * ISOLASI MUTLAK: Memisahkan saluran Omnichannel Sales (Customer-Facing)
     * dari saluran Proactive Daily (Internal Staff).
     *
     * Mencegah data privat perusahaan (taskboard internal, metrik performa karyawan,
     * daily briefing eksekutif, strategi rahasia tenant) bocor ke pelanggan eksternal.
     */
    suspend fun validateAudienceScope(
        tenantId: String,
        channelType: String,
        senderIdentifier: String,
        requestedScope: AudienceScope
    ): Boolean {
        val normalizedChannel = channelType.lowercase()
        val isStaff = staffProfileRepo.findByChannelSender(normalizedChannel, senderIdentifier) != null

        return when (requestedScope) {
            AudienceScope.CUSTOMER_FACING -> {
                // Sesi Pelanggan: Hanya boleh mengakses katalog publik dan riwayat percakapan sendiri
                true
            }
            AudienceScope.INTERNAL_STAFF -> {
                // Sesi Internal: WAJIB terverifikasi sebagai staf terdaftar tenant
                if (!isStaff) {
                    logger.warn("[SECURITY ISOLATION] Akses ditolak: Pengirim $senderIdentifier di channel $channelType bukan staf internal tenant $tenantId. Tidak diizinkan mengakses data Proactive Daily / Internal.")
                    false
                } else {
                    true
                }
            }
        }
    }
}

