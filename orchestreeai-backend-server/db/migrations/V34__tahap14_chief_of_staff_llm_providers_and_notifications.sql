-- V34__tahap14_chief_of_staff_llm_providers_and_notifications.sql
-- PRD Addendum 2 Bagian 65.1/71.1/72.2/73.2/74.2/76.3/81.4 & Fase 70.B: Chief of Staff, LLM Providers, Notifications, and Trial Governance

-- 1. Table: chief_of_staff_briefings
CREATE TABLE IF NOT EXISTS chief_of_staff_briefings (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    period_title VARCHAR(255) NOT NULL,
    headline VARCHAR(255) NOT NULL,
    executive_summary TEXT NOT NULL,
    key_findings TEXT NOT NULL,
    strategic_recommendations TEXT NOT NULL,
    human_workforce_summary TEXT NOT NULL,
    ai_workforce_summary TEXT NOT NULL,
    data_availability_state VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    data_limitations_notice TEXT NOT NULL DEFAULT '',
    grounding_sources_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    explainability_reasoning TEXT NOT NULL DEFAULT '',
    requires_human_approval BOOLEAN NOT NULL DEFAULT false,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'NOT_REQUIRED',
    approved_by VARCHAR(255),
    approval_timestamp BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_cos_briefings_tenant_created ON chief_of_staff_briefings(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_cos_briefings_tenant_approval ON chief_of_staff_briefings(tenant_id, approval_status);

ALTER TABLE chief_of_staff_briefings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_chief_of_staff_briefings ON chief_of_staff_briefings;
CREATE POLICY tenant_isolation_chief_of_staff_briefings ON chief_of_staff_briefings
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_chief_of_staff_briefings ON chief_of_staff_briefings;
CREATE POLICY service_role_chief_of_staff_briefings ON chief_of_staff_briefings
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 2. Table: chief_of_staff_research_directives
CREATE TABLE IF NOT EXISTS chief_of_staff_research_directives (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    objective TEXT NOT NULL,
    target_department VARCHAR(100) NOT NULL,
    participating_agent_ids TEXT NOT NULL,
    participating_agent_names TEXT NOT NULL DEFAULT '',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    pattern_identified TEXT NOT NULL,
    proposed_knowledge_rule TEXT,
    knowledge_rule_status VARCHAR(50) NOT NULL DEFAULT 'NONE',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_cos_directives_tenant_status ON chief_of_staff_research_directives(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_cos_directives_tenant_created ON chief_of_staff_research_directives(tenant_id, created_at);

ALTER TABLE chief_of_staff_research_directives ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_chief_of_staff_research_directives ON chief_of_staff_research_directives;
CREATE POLICY tenant_isolation_chief_of_staff_research_directives ON chief_of_staff_research_directives
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_chief_of_staff_research_directives ON chief_of_staff_research_directives;
CREATE POLICY service_role_chief_of_staff_research_directives ON chief_of_staff_research_directives
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 3. Table: llm_providers
CREATE TABLE IF NOT EXISTS llm_providers (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    provider_code VARCHAR(100) NOT NULL UNIQUE,
    api_base_url TEXT NOT NULL DEFAULT 'https://generativelanguage.googleapis.com',
    api_key_env VARCHAR(100) NOT NULL DEFAULT '',
    priority INT NOT NULL DEFAULT 1,
    fallback_priority INT NOT NULL DEFAULT 1,
    task_specialization TEXT NOT NULL DEFAULT 'general_chat',
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    latency_ms BIGINT NOT NULL DEFAULT 120,
    error_rate_pct DOUBLE PRECISION NOT NULL DEFAULT 0.1,
    health_status VARCHAR(50) NOT NULL DEFAULT 'healthy',
    consecutive_failures INT NOT NULL DEFAULT 0,
    last_health_check_at BIGINT,
    total_tokens_used BIGINT NOT NULL DEFAULT 0,
    monthly_budget_usd DOUBLE PRECISION NOT NULL DEFAULT 500.0,
    used_budget_usd DOUBLE PRECISION NOT NULL DEFAULT 0.0
);

CREATE INDEX IF NOT EXISTS idx_llm_prov_code ON llm_providers(provider_code);
CREATE INDEX IF NOT EXISTS idx_llm_prov_enabled ON llm_providers(is_enabled);
CREATE INDEX IF NOT EXISTS idx_llm_prov_health ON llm_providers(health_status);
CREATE INDEX IF NOT EXISTS idx_llm_prov_fallback ON llm_providers(fallback_priority);

ALTER TABLE llm_providers ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS select_llm_providers ON llm_providers;
CREATE POLICY select_llm_providers ON llm_providers
    FOR SELECT
    USING (true);

DROP POLICY IF EXISTS service_role_llm_providers ON llm_providers;
CREATE POLICY service_role_llm_providers ON llm_providers
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 4. Table: in_app_notifications
CREATE TABLE IF NOT EXISTS in_app_notifications (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    severity VARCHAR(50) NOT NULL DEFAULT 'INFO',
    action_route VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT false,
    source_event_id VARCHAR(255),
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_in_app_notif_tenant_user ON in_app_notifications(tenant_id, user_id, source_event_id);
CREATE INDEX IF NOT EXISTS idx_in_app_notif_tenant_read ON in_app_notifications(tenant_id, is_read, created_at);

ALTER TABLE in_app_notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_in_app_notifications ON in_app_notifications;
CREATE POLICY tenant_isolation_in_app_notifications ON in_app_notifications
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_in_app_notifications ON in_app_notifications;
CREATE POLICY service_role_in_app_notifications ON in_app_notifications
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 5. Table: trial_daily_usage
CREATE TABLE IF NOT EXISTS trial_daily_usage (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    usage_date VARCHAR(20) NOT NULL,
    task_count INT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    CONSTRAINT uq_trial_daily_usage UNIQUE (tenant_id, usage_date)
);

CREATE INDEX IF NOT EXISTS idx_trial_daily_usage_tenant_date ON trial_daily_usage(tenant_id, usage_date);

ALTER TABLE trial_daily_usage ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_trial_daily_usage ON trial_daily_usage;
CREATE POLICY tenant_isolation_trial_daily_usage ON trial_daily_usage
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_trial_daily_usage ON trial_daily_usage;
CREATE POLICY service_role_trial_daily_usage ON trial_daily_usage
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);
