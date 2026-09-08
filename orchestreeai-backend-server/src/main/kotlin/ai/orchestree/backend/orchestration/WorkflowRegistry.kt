package ai.orchestree.backend.orchestration

import ai.orchestree.backend.orchestration.definitions.WorkflowDefinition
import ai.orchestree.backend.orchestration.definitions.WorkflowDefinitions
import org.slf4j.LoggerFactory

class WorkflowIsolationValidator {
    private val logger = LoggerFactory.getLogger(WorkflowIsolationValidator::class.java)

    fun validateTenantAccess(tenantId: String, workflowDef: WorkflowDefinition, tenantTier: String): Boolean {
        if (workflowDef.requiresEnterpriseTier && tenantTier.uppercase() != "ENTERPRISE") {
            logger.warn("Tenant $tenantId on tier $tenantTier blocked from accessing enterprise workflow: ${workflowDef.id}")
            return false
        }
        return true
    }
}

class WorkflowRegistry {
    private val workflows = mutableMapOf<String, WorkflowDefinition>()

    init {
        WorkflowDefinitions.ALL_WORKFLOWS.forEach { register(it) }
    }

    fun register(def: WorkflowDefinition) {
        workflows[def.id] = def
    }

    fun get(id: String): WorkflowDefinition? = workflows[id]

    fun listAll(): List<WorkflowDefinition> = workflows.values.toList()
}
