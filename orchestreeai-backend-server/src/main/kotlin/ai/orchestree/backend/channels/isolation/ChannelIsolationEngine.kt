package ai.orchestree.backend.channels.isolation

import ai.orchestree.backend.channels.ChannelAccountConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class ChannelIsolationEngine {
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
}
