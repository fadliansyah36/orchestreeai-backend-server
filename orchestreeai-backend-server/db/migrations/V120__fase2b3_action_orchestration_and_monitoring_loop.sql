-- ====================================================================
-- OrchestreeAI Database Migration V120: FASE 2B.3
-- AI Action Orchestration, Automatic Task Creation & Monitoring Loop
-- Compliant with PRD Addendum 2 Bagian 67, 68, 70, 71
-- Active for ALL tiers
-- ====================================================================

-- 1. Automatic Task Creation columns (Bagian 68.1)
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS created_by_ai_agent_id VARCHAR(64) DEFAULT NULL;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS detection_reason TEXT DEFAULT NULL;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS recommended_quantity DOUBLE PRECISION DEFAULT NULL;

CREATE INDEX IF NOT EXISTS idx_tasks_created_by_ai_agent ON tasks(tenant_id, created_by_ai_agent_id);

-- 2. Approved Actions (Bagian 67)
CREATE TABLE IF NOT EXISTS approved_actions (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    agent_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    target_system VARCHAR(64) NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    risk_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    approval_id VARCHAR(64) DEFAULT NULL,
    approved_by VARCHAR(64) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    executed_at TIMESTAMPTZ DEFAULT NULL,
    execution_result TEXT DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approved_actions_tenant ON approved_actions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_approved_actions_status ON approved_actions(status);
CREATE INDEX IF NOT EXISTS idx_approved_actions_agent ON approved_actions(agent_id);

-- 3. Monitoring Loops (Bagian 68)
CREATE TABLE IF NOT EXISTS monitoring_loops (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    anomaly_or_metric_type VARCHAR(128) NOT NULL,
    entity_reference VARCHAR(128) NOT NULL,
    source_system VARCHAR(64) NOT NULL DEFAULT 'INTERNAL',
    baseline_value DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    detected_value DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    target_resolved_value DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    task_id VARCHAR(64) REFERENCES tasks(id) ON DELETE SET NULL,
    assigned_agent_or_human_id VARCHAR(64) DEFAULT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'DETECTED',
    verification_attempts INT NOT NULL DEFAULT 0,
    max_verification_attempts INT NOT NULL DEFAULT 3,
    escalation_reason TEXT DEFAULT NULL,
    source_verified_at TIMESTAMPTZ DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_monitoring_loops_tenant ON monitoring_loops(tenant_id);
CREATE INDEX IF NOT EXISTS idx_monitoring_loops_state ON monitoring_loops(state);
CREATE INDEX IF NOT EXISTS idx_monitoring_loops_entity ON monitoring_loops(entity_reference);

-- Enable RLS
ALTER TABLE approved_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE monitoring_loops ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_approved_actions ON approved_actions;
CREATE POLICY tenant_isolation_approved_actions ON approved_actions FOR ALL USING (true);

DROP POLICY IF EXISTS tenant_isolation_monitoring_loops ON monitoring_loops;
CREATE POLICY tenant_isolation_monitoring_loops ON monitoring_loops FOR ALL USING (true);
