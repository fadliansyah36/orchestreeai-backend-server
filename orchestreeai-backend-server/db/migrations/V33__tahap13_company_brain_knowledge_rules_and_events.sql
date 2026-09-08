-- V33__tahap13_company_brain_knowledge_rules_and_events.sql
-- PRD Master Bagian 16.2/16.3 & Addendum 2 Bagian 59.2/62.1/70.1/71.1: Company Brain Documents, Knowledge Rules & AI Event Bus

-- 1. Table: company_brain_documents
CREATE TABLE IF NOT EXISTS company_brain_documents (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    document_category VARCHAR(100) NOT NULL,
    object_storage_url TEXT NOT NULL,
    extraction_status VARCHAR(50) NOT NULL DEFAULT 'processing',
    uploaded_by VARCHAR(255) NOT NULL DEFAULT 'CEO / Super Admin',
    uploaded_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    extracted_summary TEXT,
    chunks_count INT NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_company_brain_docs_tenant ON company_brain_documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_company_brain_docs_category ON company_brain_documents(document_category);
CREATE INDEX IF NOT EXISTS idx_company_brain_docs_status ON company_brain_documents(extraction_status);
CREATE INDEX IF NOT EXISTS idx_company_brain_docs_uploaded_at ON company_brain_documents(uploaded_at);

ALTER TABLE company_brain_documents ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_company_brain_documents ON company_brain_documents;
CREATE POLICY tenant_isolation_company_brain_documents ON company_brain_documents
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_company_brain_documents ON company_brain_documents;
CREATE POLICY service_role_company_brain_documents ON company_brain_documents
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 2. Table: knowledge_rules
CREATE TABLE IF NOT EXISTS knowledge_rules (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity_type VARCHAR(100) NOT NULL,
    condition TEXT NOT NULL,
    comparison_operator VARCHAR(10) NOT NULL DEFAULT '>=',
    threshold_value DOUBLE PRECISION NOT NULL,
    sop_reference VARCHAR(255) NOT NULL,
    rule_description TEXT NOT NULL,
    proposed_by_ai_agent_id VARCHAR(255),
    proposed_by_persona VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    rejection_reason TEXT,
    approved_by_user_id VARCHAR(255),
    approved_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_knowledge_rules_tenant ON knowledge_rules(tenant_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_rules_entity ON knowledge_rules(entity_type);
CREATE INDEX IF NOT EXISTS idx_knowledge_rules_status ON knowledge_rules(status);

ALTER TABLE knowledge_rules ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_knowledge_rules ON knowledge_rules;
CREATE POLICY tenant_isolation_knowledge_rules ON knowledge_rules
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_knowledge_rules ON knowledge_rules;
CREATE POLICY service_role_knowledge_rules ON knowledge_rules
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 3. Table: ai_event_definitions
CREATE TABLE IF NOT EXISTS ai_event_definitions (
    id VARCHAR(255) PRIMARY KEY,
    event_code VARCHAR(100) NOT NULL UNIQUE,
    responsible_persona_type VARCHAR(100) NOT NULL,
    severity_default VARCHAR(50) NOT NULL DEFAULT 'HIGH',
    description TEXT NOT NULL,
    is_multi_agent_candidate BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_ai_event_defs_code ON ai_event_definitions(event_code);

ALTER TABLE ai_event_definitions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS select_ai_event_definitions ON ai_event_definitions;
CREATE POLICY select_ai_event_definitions ON ai_event_definitions
    FOR SELECT
    USING (true);

DROP POLICY IF EXISTS service_role_ai_event_definitions ON ai_event_definitions;
CREATE POLICY service_role_ai_event_definitions ON ai_event_definitions
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 4. Table: ai_event_instances
CREATE TABLE IF NOT EXISTS ai_event_instances (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_code VARCHAR(100) NOT NULL,
    entity_reference VARCHAR(255) NOT NULL,
    source_system VARCHAR(100) NOT NULL DEFAULT 'INTERNAL',
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    severity VARCHAR(50) NOT NULL DEFAULT 'HIGH',
    is_multi_agent_collaborative BOOLEAN NOT NULL DEFAULT false,
    triggered_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    handled_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ai_event_inst_tenant ON ai_event_instances(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_event_inst_code ON ai_event_instances(event_code);
CREATE INDEX IF NOT EXISTS idx_ai_event_inst_ref ON ai_event_instances(entity_reference);
CREATE INDEX IF NOT EXISTS idx_ai_event_inst_status ON ai_event_instances(status);
CREATE INDEX IF NOT EXISTS idx_ai_event_inst_triggered ON ai_event_instances(triggered_at);

ALTER TABLE ai_event_instances ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_ai_event_instances ON ai_event_instances;
CREATE POLICY tenant_isolation_ai_event_instances ON ai_event_instances
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_ai_event_instances ON ai_event_instances;
CREATE POLICY service_role_ai_event_instances ON ai_event_instances
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 5. Table: ai_event_dispatch_log
CREATE TABLE IF NOT EXISTS ai_event_dispatch_log (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_instance_id VARCHAR(255) NOT NULL REFERENCES ai_event_instances(id) ON DELETE CASCADE,
    dispatched_to_agent_id VARCHAR(255) NOT NULL,
    target_persona VARCHAR(100) NOT NULL,
    dispatched_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    response_summary TEXT NOT NULL DEFAULT '',
    execution_success BOOLEAN NOT NULL DEFAULT true,
    is_collaborative_dispatch BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_ai_event_disp_inst ON ai_event_dispatch_log(event_instance_id);
CREATE INDEX IF NOT EXISTS idx_ai_event_disp_tenant ON ai_event_dispatch_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_event_disp_agent ON ai_event_dispatch_log(dispatched_to_agent_id);
CREATE INDEX IF NOT EXISTS idx_ai_event_disp_at ON ai_event_dispatch_log(dispatched_at);

ALTER TABLE ai_event_dispatch_log ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_ai_event_dispatch_log ON ai_event_dispatch_log;
CREATE POLICY tenant_isolation_ai_event_dispatch_log ON ai_event_dispatch_log
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_ai_event_dispatch_log ON ai_event_dispatch_log;
CREATE POLICY service_role_ai_event_dispatch_log ON ai_event_dispatch_log
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);
