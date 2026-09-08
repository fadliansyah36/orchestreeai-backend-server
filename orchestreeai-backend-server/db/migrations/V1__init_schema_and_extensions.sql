-- ====================================================================
-- OrchestreeAI Database Migration V1: Core Schema, Extensions & RLS
-- PostgreSQL 16 + pgvector + Row-Level Security (RLS)
-- ====================================================================

-- 1. Enable Core Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- 2. Subscription Plans (System-wide definitions)
CREATE TABLE IF NOT EXISTS subscription_plans (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    tier VARCHAR(64) NOT NULL,
    price_idr_monthly NUMERIC(15, 2) NOT NULL,
    price_idr_yearly NUMERIC(15, 2) NOT NULL,
    max_agents INT NOT NULL,
    max_human_seats INT NOT NULL,
    monthly_llm_token_limit BIGINT NOT NULL,
    features_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tenants (Multi-tenant Master)
CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    domain VARCHAR(255) NOT NULL UNIQUE,
    tier VARCHAR(64) NOT NULL DEFAULT 'STARTER',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    monthly_llm_budget NUMERIC(12, 2) NOT NULL DEFAULT 500.0,
    used_llm_budget NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tenant Subscriptions
CREATE TABLE IF NOT EXISTS tenant_subscriptions (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plan_id VARCHAR(64) NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    billing_cycle VARCHAR(16) NOT NULL DEFAULT 'MONTHLY',
    current_period_start TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_period_end TIMESTAMPTZ NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Roles & Capabilities (RBAC)
CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    label VARCHAR(128) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS permissions (
    id VARCHAR(64) PRIMARY KEY,
    capability_string VARCHAR(128) NOT NULL UNIQUE,
    category VARCHAR(64) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id VARCHAR(64) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id VARCHAR(64) NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- 6. Users
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(32) DEFAULT '',
    telegram_chat_id VARCHAR(64) DEFAULT '',
    avatar_url TEXT DEFAULT '',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id VARCHAR(64) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 7. Sessions & Refresh Tokens
CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255) NOT NULL,
    user_agent TEXT,
    ip_address VARCHAR(45),
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. API Keys
CREATE TABLE IF NOT EXISTS api_keys (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    scopes VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. MFA (TOTP) Credentials
CREATE TABLE IF NOT EXISTS mfa_credentials (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    secret_key VARCHAR(255) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    backup_codes JSONB DEFAULT '[]'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. Departments
CREATE TABLE IF NOT EXISTS departments (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    parent_department_id VARCHAR(64),
    manager_user_id VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    color_hex VARCHAR(16) NOT NULL DEFAULT '#1E6FE0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. AI Agents
CREATE TABLE IF NOT EXISTS ai_agents (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL,
    role_title VARCHAR(128) NOT NULL,
    department_id VARCHAR(64) REFERENCES departments(id) ON DELETE SET NULL,
    avatar_icon VARCHAR(64) NOT NULL DEFAULT 'robot',
    status VARCHAR(32) NOT NULL DEFAULT 'ONLINE',
    skills TEXT NOT NULL DEFAULT '',
    tools_granted TEXT NOT NULL DEFAULT '',
    current_live_action TEXT NOT NULL DEFAULT 'Siap menerima penugasan',
    completed_tasks_count INT NOT NULL DEFAULT 0,
    quality_rating NUMERIC(5, 2) NOT NULL DEFAULT 95.0,
    uptime_percent NUMERIC(5, 2) NOT NULL DEFAULT 99.9,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. Tasks (Kanban & Orchestration)
CREATE TABLE IF NOT EXISTS tasks (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    department_id VARCHAR(64) REFERENCES departments(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    column_name VARCHAR(32) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    assignee_type VARCHAR(32) NOT NULL DEFAULT 'AI_AGENT',
    assignee_id VARCHAR(64) NOT NULL,
    assignee_name VARCHAR(128) NOT NULL,
    due_date VARCHAR(64) NOT NULL DEFAULT '',
    progress_pct INT NOT NULL DEFAULT 0,
    live_status_line TEXT NOT NULL DEFAULT '',
    workflow_execution_id VARCHAR(64),
    workflow_nodes_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    is_approved BOOLEAN NOT NULL DEFAULT FALSE,
    risk_reason TEXT,
    output_result TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 13. Competitor Targets & Insights
CREATE TABLE IF NOT EXISTS competitor_targets (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    url TEXT NOT NULL,
    frequency VARCHAR(32) NOT NULL DEFAULT 'Hourly',
    assigned_agent_id VARCHAR(64) REFERENCES ai_agents(id) ON DELETE SET NULL,
    insight_prefs TEXT NOT NULL DEFAULT '',
    status VARCHAR(32) NOT NULL DEFAULT 'Monitoring Active',
    last_monitored_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS competitor_insights (
    id VARCHAR(64) PRIMARY KEY,
    target_id VARCHAR(64) NOT NULL REFERENCES competitor_targets(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    competitor_name VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    summary TEXT NOT NULL,
    narrative TEXT NOT NULL,
    confidence NUMERIC(5, 3) NOT NULL,
    impact_score NUMERIC(5, 3) NOT NULL,
    final_score NUMERIC(5, 3) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    importance VARCHAR(32) NOT NULL,
    recommended_action TEXT NOT NULL,
    source_url TEXT NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 14. Integrations
CREATE TABLE IF NOT EXISTS integrations (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    platform VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DISCONNECTED',
    connected_account VARCHAR(255) DEFAULT '',
    scopes_granted TEXT DEFAULT '',
    transparency_notice_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    last_health_check_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ping_latency_ms BIGINT NOT NULL DEFAULT 0,
    error_reason TEXT,
    expires_at TIMESTAMPTZ
);

-- 15. Proactive Subscriptions & Messages Log
CREATE TABLE IF NOT EXISTS proactive_subscriptions (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    staff_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    staff_name VARCHAR(255) NOT NULL,
    staff_role VARCHAR(128) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    destination_number VARCHAR(64) NOT NULL,
    enabled_notif_types TEXT NOT NULL,
    send_times VARCHAR(128) NOT NULL DEFAULT '08:00, 17:00',
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Jakarta (WIB)',
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    max_daily_messages INT NOT NULL DEFAULT 5,
    messages_sent_today INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS proactive_messages_log (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    staff_id VARCHAR(64) NOT NULL,
    staff_name VARCHAR(255) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    message_type VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'DELIVERED',
    agent_sender_name VARCHAR(128) NOT NULL
);

-- 16. Semantic Memory Documents (pgvector enabled)
CREATE TABLE IF NOT EXISTS memory_documents (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    tags TEXT NOT NULL DEFAULT '',
    source_reference VARCHAR(255) NOT NULL DEFAULT 'System / Workflow Engine',
    embedding vector(768),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 17. MCP Tools Registry (System & Tenant Level)
CREATE TABLE IF NOT EXISTS mcp_tools (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    version VARCHAR(32) NOT NULL DEFAULT 'v1.0.0',
    description TEXT NOT NULL,
    category VARCHAR(64) NOT NULL,
    input_schema_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
    is_enabled_globally BOOLEAN NOT NULL DEFAULT TRUE,
    total_invocations BIGINT NOT NULL DEFAULT 0,
    error_rate_pct NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    is_kill_switched BOOLEAN NOT NULL DEFAULT FALSE
);

-- 18. LLM Routing Rules
CREATE TABLE IF NOT EXISTS llm_routing_rules (
    id VARCHAR(64) PRIMARY KEY,
    task_type VARCHAR(128) NOT NULL UNIQUE,
    preferred_provider VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    fallback_provider VARCHAR(64) NOT NULL,
    max_tokens INT NOT NULL DEFAULT 4096,
    temperature NUMERIC(3, 2) NOT NULL DEFAULT 0.7,
    cost_per_million_tokens_usd NUMERIC(8, 4) NOT NULL DEFAULT 0.50
);

-- 19. Audit Logs (Immutable Security Ledger)
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    actor_name VARCHAR(255) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    action VARCHAR(128) NOT NULL,
    entity_target VARCHAR(255) NOT NULL,
    details TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ====================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- Ensures strict multi-tenant data isolation per PRD Section 16.1 & 16.3
-- ====================================================================

-- Function to check current tenant context or super admin bypass
CREATE OR REPLACE FUNCTION app_has_tenant_access(record_tenant_id VARCHAR) RETURNS BOOLEAN AS $$
BEGIN
    RETURN (
        current_setting('app.is_super_admin', true) = 'true'
        OR current_setting('app.current_tenant_id', true) = record_tenant_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Enable RLS on Tenant-bound Tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE departments ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_agents ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE competitor_targets ENABLE ROW LEVEL SECURITY;
ALTER TABLE competitor_insights ENABLE ROW LEVEL SECURITY;
ALTER TABLE integrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE proactive_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE proactive_messages_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE memory_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Apply Tenant Isolation Policies
DROP POLICY IF EXISTS tenant_isolation_users ON users;
DROP POLICY IF EXISTS tenant_isolation_users ON users;
CREATE POLICY tenant_isolation_users ON users USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_departments ON departments;
DROP POLICY IF EXISTS tenant_isolation_departments ON departments;
CREATE POLICY tenant_isolation_departments ON departments USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_ai_agents ON ai_agents;
DROP POLICY IF EXISTS tenant_isolation_ai_agents ON ai_agents;
CREATE POLICY tenant_isolation_ai_agents ON ai_agents USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_tasks ON tasks;
DROP POLICY IF EXISTS tenant_isolation_tasks ON tasks;
CREATE POLICY tenant_isolation_tasks ON tasks USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_targets ON competitor_targets;
DROP POLICY IF EXISTS tenant_isolation_targets ON competitor_targets;
CREATE POLICY tenant_isolation_targets ON competitor_targets USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_insights ON competitor_insights;
DROP POLICY IF EXISTS tenant_isolation_insights ON competitor_insights;
CREATE POLICY tenant_isolation_insights ON competitor_insights USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_integrations ON integrations;
DROP POLICY IF EXISTS tenant_isolation_integrations ON integrations;
CREATE POLICY tenant_isolation_integrations ON integrations USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_proactive_sub ON proactive_subscriptions;
DROP POLICY IF EXISTS tenant_isolation_proactive_sub ON proactive_subscriptions;
CREATE POLICY tenant_isolation_proactive_sub ON proactive_subscriptions USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_proactive_logs ON proactive_messages_log;
DROP POLICY IF EXISTS tenant_isolation_proactive_logs ON proactive_messages_log;
CREATE POLICY tenant_isolation_proactive_logs ON proactive_messages_log USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_memory ON memory_documents;
DROP POLICY IF EXISTS tenant_isolation_memory ON memory_documents;
CREATE POLICY tenant_isolation_memory ON memory_documents USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_audit ON audit_logs;
DROP POLICY IF EXISTS tenant_isolation_audit ON audit_logs;
CREATE POLICY tenant_isolation_audit ON audit_logs USING (app_has_tenant_access(tenant_id));
