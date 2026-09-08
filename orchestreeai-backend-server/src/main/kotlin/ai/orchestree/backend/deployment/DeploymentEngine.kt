package ai.orchestree.backend.deployment

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
enum class DeploymentStage {
    DEV,
    STAGING,
    PILOT,
    GA_PRODUCTION
}

@Serializable
data class TenantDeploymentStatus(
    val deploymentId: String,
    val tenantId: String,
    val version: String,
    val stage: DeploymentStage,
    val isCanaryActive: Boolean,
    val canaryTrafficPercentage: Int,
    val healthCheckPassed: Boolean,
    val deployedAt: Long = System.currentTimeMillis()
)

object DeploymentEngine {
    private val logger = LoggerFactory.getLogger(DeploymentEngine::class.java)

    suspend fun promoteDeployment(
        tenantId: String,
        targetStage: DeploymentStage,
        version: String,
        canaryPct: Int = 100
    ): TenantDeploymentStatus = withContext(Dispatchers.IO) {
        val depId = "dep-" + UUID.randomUUID().toString().take(8)
        val now = System.currentTimeMillis()

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        INSERT INTO tenant_deployments 
                        (id, tenant_id, version, stage, canary_traffic_percentage, health_check_passed, created_at)
                        VALUES (?, ?, ?, ?, ?, true, ?)
                    """.trimIndent()).use { ps ->
                        ps.setString(1, depId)
                        ps.setString(2, tenantId)
                        ps.setString(3, version)
                        ps.setString(4, targetStage.name)
                        ps.setInt(5, canaryPct)
                        ps.setLong(6, now)
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not insert tenant deployment: ${e.message}")
            }
        }

        TenantDeploymentStatus(
            deploymentId = depId,
            tenantId = tenantId,
            version = version,
            stage = targetStage,
            isCanaryActive = canaryPct < 100,
            canaryTrafficPercentage = canaryPct,
            healthCheckPassed = true,
            deployedAt = now
        )
    }
}
