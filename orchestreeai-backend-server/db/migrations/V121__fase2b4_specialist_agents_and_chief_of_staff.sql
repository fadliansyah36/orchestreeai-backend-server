-- V121__fase2b4_specialist_agents_and_chief_of_staff.sql
-- PRD Addendum 2 Bagian 72-76: Specialist AI Agents, Multi-Agent Collaboration, AI Chief of Staff, Continuous Learning, Data Quality Governance

-- 1. Extend ai_agents for Specialist Heavy Industry roles (Bagian 72.1-72.2)
ALTER TABLE ai_agents ADD COLUMN IF NOT EXISTS persona_type VARCHAR(64);
ALTER TABLE ai_agents ADD COLUMN IF NOT EXISTS persona_config JSONB DEFAULT '{}'::jsonb;
ALTER TABLE ai_agents ADD COLUMN IF NOT EXISTS specialist_domain VARCHAR(64) DEFAULT 'GENERAL_OPERATIONS';
ALTER TABLE ai_agents ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ai_agents_specialist ON ai_agents(tenant_id, persona_type, specialist_domain);

-- 2. View: chief_of_staff_agent_registry_view (Bagian 73.1)
CREATE OR REPLACE VIEW chief_of_staff_agent_registry_view AS
SELECT 
    a.id AS agent_id,
    a.tenant_id,
    a.name AS agent_name,
    a.role_title,
    COALESCE(a.persona_type, 'GENERAL_ASSISTANT') AS persona_type,
    COALESCE(a.specialist_domain, 'GENERAL_OPERATIONS') AS specialist_domain,
    a.status,
    a.completed_tasks_count,
    a.quality_rating,
    a.uptime_percent,
    COALESCE(c.avg_confidence, 85.0) AS skill_confidence_score,
    COALESCE(c.sample_count, 0) AS skill_sample_count
FROM ai_agents a
LEFT JOIN (
    SELECT 
        agent_id, 
        tenant_id, 
        ROUND(AVG(current_confidence_score)::numeric, 1) AS avg_confidence,
        SUM(sample_size) AS sample_count
    FROM agent_skill_confidence
    GROUP BY agent_id, tenant_id
) c ON a.id = c.agent_id AND a.tenant_id = c.tenant_id;

-- 3. Table: project_health_scores (Bagian 72.3)
CREATE TABLE IF NOT EXISTS project_health_scores (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    project_id VARCHAR(64) NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    schedule_score NUMERIC(5, 2) NOT NULL DEFAULT 100.0,
    budget_score NUMERIC(5, 2) NOT NULL DEFAULT 100.0,
    risk_score NUMERIC(5, 2) NOT NULL DEFAULT 100.0,
    workforce_score NUMERIC(5, 2) NOT NULL DEFAULT 100.0,
    composite_health_score NUMERIC(5, 2) NOT NULL DEFAULT 100.0,
    health_status VARCHAR(32) NOT NULL DEFAULT 'ON_TRACK',
    blockers_count INT NOT NULL DEFAULT 0,
    details_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    evaluated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_project_health_tenant_proj ON project_health_scores(tenant_id, project_id, evaluated_at DESC);

-- 4. Table: multi_agent_collaborations (Bagian 72.4)
CREATE TABLE IF NOT EXISTS multi_agent_collaborations (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    initiating_agent_id VARCHAR(64) NOT NULL,
    initiating_persona VARCHAR(64) NOT NULL,
    participating_agents JSONB NOT NULL DEFAULT '[]'::jsonb,
    target_entity_reference VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'INITIATED',
    consensus_summary TEXT,
    action_plan_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_multi_agent_collab_tenant ON multi_agent_collaborations(tenant_id, status);

-- 5. Table: data_quality_issues (Bagian 76.2)
CREATE TABLE IF NOT EXISTS data_quality_issues (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity_type VARCHAR(64) NOT NULL,
    entity_reference VARCHAR(128) NOT NULL,
    issue_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    description TEXT NOT NULL,
    detected_by_agent VARCHAR(64) NOT NULL,
    source_systems_involved JSONB NOT NULL DEFAULT '[]'::jsonb,
    conflicting_values_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    resolved_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_data_quality_issues_tenant ON data_quality_issues(tenant_id, status);

-- Ensure RLS on all new tables
ALTER TABLE project_health_scores ENABLE ROW LEVEL SECURITY;
ALTER TABLE multi_agent_collaborations ENABLE ROW LEVEL SECURITY;
ALTER TABLE data_quality_issues ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_project_health_scores ON project_health_scores;
CREATE POLICY tenant_isolation_project_health_scores ON project_health_scores
    FOR ALL USING (app_has_tenant_access(tenant_id)) WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_multi_agent_collaborations ON multi_agent_collaborations;
CREATE POLICY tenant_isolation_multi_agent_collaborations ON multi_agent_collaborations
    FOR ALL USING (app_has_tenant_access(tenant_id)) WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_data_quality_issues ON data_quality_issues;
CREATE POLICY tenant_isolation_data_quality_issues ON data_quality_issues
    FOR ALL USING (app_has_tenant_access(tenant_id)) WITH CHECK (app_has_tenant_access(tenant_id));
