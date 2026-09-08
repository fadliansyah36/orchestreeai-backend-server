-- ====================================================================
-- OrchestreeAI Database Migration V15: Attribute-Based Access Control (ABAC) for AI Data Permission
-- PRD Addendum 2 Bagian 59: AI Data Permission Policies & Access Requests
-- ====================================================================

-- 1. AI Data Permission Policies Table (PRD Section 59.2)
-- Default-Deny: AI Agent has ZERO access to external data without explicit rows here
CREATE TABLE IF NOT EXISTS ai_data_permission_policies (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    agent_id VARCHAR(64) NOT NULL, -- or persona code e.g. 'CHIEF_OF_STAFF', 'HR_SPECIALIST', 'FINANCE_SPECIALIST'
    connection_id VARCHAR(64) NOT NULL REFERENCES enterprise_system_connections(id) ON DELETE CASCADE,
    access_level VARCHAR(32) NOT NULL DEFAULT 'READ_ONLY', -- 'READ_ONLY', 'ANALYZE', 'RECOMMEND', 'EXECUTE'
    allowed_tables_or_types JSONB NOT NULL DEFAULT '["*"]',
    allowed_fields JSONB NOT NULL DEFAULT '["*"]',
    condition_rules JSONB NOT NULL DEFAULT '{}',
    granted_by_user_id VARCHAR(64) NOT NULL DEFAULT 'system',
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_data_policy UNIQUE (tenant_id, agent_id, connection_id)
);

CREATE INDEX IF NOT EXISTS idx_ai_data_policies_lookup ON ai_data_permission_policies(tenant_id, agent_id, connection_id);

-- 2. AI Data Access Requests Table (PRD Section 59.2 & 59.4)
-- AI Agent requests access to external system; Admin approves/rejects
CREATE TABLE IF NOT EXISTS ai_data_access_requests (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    agent_id VARCHAR(64) NOT NULL,
    connection_id VARCHAR(64) NOT NULL REFERENCES enterprise_system_connections(id) ON DELETE CASCADE,
    requested_access_level VARCHAR(32) NOT NULL DEFAULT 'READ_ONLY',
    requested_scope JSONB NOT NULL DEFAULT '["*"]',
    business_reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    reviewed_by_user_id VARCHAR(64),
    reviewed_at TIMESTAMPTZ,
    rejection_reason TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_data_req_tenant_agent ON ai_data_access_requests(tenant_id, agent_id, status);
