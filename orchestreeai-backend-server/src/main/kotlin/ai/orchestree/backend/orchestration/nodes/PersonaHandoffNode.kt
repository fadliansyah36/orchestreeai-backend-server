package ai.orchestree.backend.orchestration.nodes

import ai.orchestree.backend.billing.DatabaseManager
import ai.orchestree.backend.orchestration.NodeExecutionStatus
import ai.orchestree.backend.orchestration.WorkflowNodeRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID

@Serializable
data class PersonaHandoffContext(
    val sourcePersona: String,
    val targetPersona: String,
    val handoffReason: String,
    val conversationState: Map<String, String>,
    val transferredAt: Long = System.currentTimeMillis()
)

class PersonaHandoffNode {
    private val logger = LoggerFactory.getLogger(PersonaHandoffNode::class.java)

    suspend fun executeHandoff(
        tenantId: String,
        conversationId: String,
        sourcePersona: String,
        targetPersona: String,
        reason: String,
        payload: Map<String, String>
    ): WorkflowNodeRun = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val nodeId = "node-handoff-${UUID.randomUUID().toString().take(6)}"

        logger.info("[HANDOFF] Transferring conversation $conversationId from $sourcePersona to $targetPersona (Reason: $reason)")

        val conn = DatabaseManager.getConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    c.prepareStatement("""
                        UPDATE conversations 
                        SET assigned_agent_id = ?, updated_at = ?
                        WHERE id = ? AND tenant_id = ?
                    """.trimIndent()).use { ps ->
                        ps.setString(1, targetPersona)
                        ps.setLong(2, System.currentTimeMillis())
                        ps.setString(3, conversationId)
                        ps.setString(4, tenantId)
                        ps.executeUpdate()
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not update assigned agent in conversation: ${e.message}")
            }
        }

        val outputJson = """{"handoff_status":"COMPLETED","from":"$sourcePersona","to":"$targetPersona","reason":"$reason"}"""

        WorkflowNodeRun(
            nodeId = nodeId,
            nodeType = "PERSONA_HANDOFF",
            status = NodeExecutionStatus.SUCCESS.name,
            input = "from: $sourcePersona, to: $targetPersona, reason: $reason",
            output = outputJson,
            durationMs = System.currentTimeMillis() - startTime
        )
    }
}
