-- ====================================================================
-- OrchestreeAI Database Migration V16: Company Activity Stream & Cross-System Correlation
-- PRD Addendum 2 Bagian 60-62: 3 Data Modes, Multi-System Correlation & Context Events
-- ====================================================================

-- 1. Company Activity Stream (PRD Section 62.1)
-- Normalized, chronological feed of enterprise activities from all connected systems
CREATE TABLE IF NOT EXISTS company_activity_stream (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    connection_id VARCHAR(64) NOT NULL REFERENCES enterprise_system_connections(id) ON DELETE CASCADE,
    system_type VARCHAR(64) NOT NULL, -- 'cmms', 'fleet', 'erp', 'crm', 'project', 'finance', 'iot', 'hse', etc.
    record_id VARCHAR(64) REFERENCES enterprise_ingested_records(id) ON DELETE CASCADE,
    entity_reference VARCHAR(128) NOT NULL,
    activity_type VARCHAR(64) NOT NULL,
    summary TEXT NOT NULL,
    data_mode VARCHAR(32) NOT NULL DEFAULT 'NEAR_REALTIME', -- 'REALTIME', 'NEAR_REALTIME', 'HISTORICAL' (Table 60.2)
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata_json JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_activity_stream_tenant_entity ON company_activity_stream(tenant_id, entity_reference, event_timestamp);
CREATE INDEX IF NOT EXISTS idx_activity_stream_tenant_conn ON company_activity_stream(tenant_id, connection_id);
CREATE INDEX IF NOT EXISTS idx_activity_stream_tenant_time ON company_activity_stream(tenant_id, event_timestamp DESC);

-- 2. Company Context Events (PRD Section 62.1 & 61.2)
-- High-order synthesized insights generated ONLY when >= 2 distinct systems correlate on the same entity
CREATE TABLE IF NOT EXISTS company_context_events (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    event_code VARCHAR(64) NOT NULL,
    title VARCHAR(256) NOT NULL,
    description TEXT NOT NULL,
    entity_reference VARCHAR(128) NOT NULL,
    contributing_systems JSONB NOT NULL DEFAULT '[]',
    contributing_stream_ids JSONB NOT NULL DEFAULT '[]',
    risk_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    confidence_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    impact_level VARCHAR(32) NOT NULL DEFAULT 'MEDIUM', -- 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'
    status VARCHAR(32) NOT NULL DEFAULT 'NEW', -- 'NEW', 'DISPATCHED', 'RESOLVED', 'IGNORED'
    recommended_action TEXT NOT NULL DEFAULT '',
    assigned_persona_code VARCHAR(64) NOT NULL DEFAULT 'CHIEF_OF_STAFF',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_context_events_tenant_entity ON company_context_events(tenant_id, entity_reference);
CREATE INDEX IF NOT EXISTS idx_context_events_tenant_status ON company_context_events(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_context_events_tenant_created ON company_context_events(tenant_id, created_at DESC);
