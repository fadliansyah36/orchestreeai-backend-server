-- V35__create_V35_OrchestreeMigration.sql
-- OrchestreeAI Autonomous AI Workforce Operating System
-- Target: Enterprise OS, Omnichannel Sales, Multi-Agent Swarm, & Governance

-- 1. Ensure extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- 2. Audit Trail & System Version Sync
CREATE TABLE IF NOT EXISTS schema_migration_audit_ledger (
    id VARCHAR(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    version_tag VARCHAR(64) NOT NULL,
    migration_name VARCHAR(255) NOT NULL,
    applied_by VARCHAR(128) NOT NULL DEFAULT 'system_migration_runner',
    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checksum VARCHAR(128),
    execution_time_ms BIGINT DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    notes TEXT
);

-- Record this migration
INSERT INTO schema_migration_audit_ledger (id, version_tag, migration_name, applied_by, notes)
VALUES (
    gen_random_uuid()::text,
    'V35',
    'create_V35_OrchestreeMigration',
    'system_orchestrator',
    'Full Enterprise OS, Multi-Agent Collaboration, Realtime Voice & Cognitive Loop alignment'
) ON CONFLICT (id) DO NOTHING;

-- 3. Ensure Index & Performance Optimizations across core tables
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'conversations') THEN
        CREATE INDEX IF NOT EXISTS idx_conversations_tenant_stage ON conversations(tenant_id, sales_stage);
        CREATE INDEX IF NOT EXISTS idx_conversations_assigned_agent ON conversations(assigned_agent_id);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tasks') THEN
        CREATE INDEX IF NOT EXISTS idx_tasks_tenant_status ON tasks(tenant_id, status);
        CREATE INDEX IF NOT EXISTS idx_tasks_assigned_agent ON tasks(assigned_agent_id);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chief_of_staff_briefings') THEN
        CREATE INDEX IF NOT EXISTS idx_cos_briefings_period ON chief_of_staff_briefings(tenant_id, period_title);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'enterprise_system_connections') THEN
        CREATE INDEX IF NOT EXISTS idx_enterprise_connections_type ON enterprise_system_connections(tenant_id, system_type);
    END IF;
END $$;

COMMENT ON TABLE schema_migration_audit_ledger IS 'Audit history of applied database migrations and schema states for OrchestreeAI.';
