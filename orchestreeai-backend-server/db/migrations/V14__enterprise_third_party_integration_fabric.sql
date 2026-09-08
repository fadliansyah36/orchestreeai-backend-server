-- ====================================================================
-- OrchestreeAI Database Migration V14: Third-Party Enterprise Integration Fabric
-- PRD Addendum 2 Bagian 58: Enterprise System Connections, Data Sync Jobs, Ingested Records
-- ====================================================================

-- 1. Enterprise System Connections Table (PRD Section 58.2)
CREATE TABLE IF NOT EXISTS enterprise_system_connections (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    system_name VARCHAR(128) NOT NULL,
    system_type VARCHAR(64) NOT NULL, -- 'erp', 'hris', 'crm', 'cmms', 'fms', 'fleet', 'hse', 'project', 'finance', 'procurement', 'warehouse', 'iot', 'document', 'email', 'messaging'
    connector_kind VARCHAR(64) NOT NULL, -- 'API_CONNECTOR', 'WEBHOOK_CONNECTOR', 'MESSAGING_CONNECTOR', 'OAUTH_CONNECTOR', 'SECURE_VPN_CONNECTOR', 'DATABASE_CONNECTOR', 'FILE_CONNECTOR', 'EMAIL_CONNECTOR'
    credential_ref TEXT NOT NULL DEFAULT '', -- Enkripsi envelope per koneksi
    endpoint_url TEXT NOT NULL DEFAULT '',
    status VARCHAR(32) NOT NULL DEFAULT 'HEALTHY', -- 'HEALTHY', 'ERROR', 'UNREACHABLE', 'SUSPENDED', 'DISCONNECTED'
    consecutive_failures INT NOT NULL DEFAULT 0,
    circuit_breaker_tripped BOOLEAN NOT NULL DEFAULT FALSE,
    error_reason TEXT NOT NULL DEFAULT '',
    last_sync_at TIMESTAMPTZ,
    last_ping_at TIMESTAMPTZ,
    created_by_user_id VARCHAR(64) NOT NULL DEFAULT 'system',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ent_connections_tenant ON enterprise_system_connections(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ent_connections_status ON enterprise_system_connections(status);

-- 2. Enterprise Data Sync Jobs Table (PRD Section 58.2)
CREATE TABLE IF NOT EXISTS enterprise_data_sync_jobs (
    id VARCHAR(64) PRIMARY KEY,
    connection_id VARCHAR(64) NOT NULL REFERENCES enterprise_system_connections(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    sync_mode VARCHAR(32) NOT NULL DEFAULT 'NEAR_REALTIME', -- 'REALTIME', 'NEAR_REALTIME', 'HISTORICAL'
    last_cursor TEXT NOT NULL DEFAULT '',
    last_run_at TIMESTAMPTZ,
    last_run_status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- 'SUCCESS', 'FAILED', 'IN_PROGRESS', 'PENDING'
    records_synced_count INT NOT NULL DEFAULT 0,
    error_message TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ent_sync_jobs_conn ON enterprise_data_sync_jobs(connection_id);

-- 3. Enterprise Ingested Records Table (PRD Section 58.2)
CREATE TABLE IF NOT EXISTS enterprise_ingested_records (
    id VARCHAR(64) PRIMARY KEY,
    connection_id VARCHAR(64) NOT NULL REFERENCES enterprise_system_connections(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    record_type VARCHAR(64) NOT NULL, -- 'PO', 'EQUIPMENT', 'PROJECT', 'EMPLOYEE', 'INVOICE', 'FLEET_TELEMETRY', 'HSE_INCIDENT', 'INVENTORY_STOCK', 'MESSAGE'
    external_record_id VARCHAR(128) NOT NULL,
    entity_reference VARCHAR(128) NOT NULL DEFAULT '',
    data_mode VARCHAR(32) NOT NULL DEFAULT 'NEAR_REALTIME', -- 'REALTIME', 'NEAR_REALTIME', 'HISTORICAL'
    normalized_payload JSONB NOT NULL DEFAULT '{}',
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ent_ingested_record UNIQUE (connection_id, record_type, external_record_id)
);

CREATE INDEX IF NOT EXISTS idx_ent_ingested_records_tenant ON enterprise_ingested_records(tenant_id, record_type);
CREATE INDEX IF NOT EXISTS idx_ent_ingested_records_entity ON enterprise_ingested_records(entity_reference);
