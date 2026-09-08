-- ====================================================================
-- OrchestreeAI Database Migration V4: Proactive Agent & Notification Schema
-- Compliant with PRD Section 11 (Proactive Agent OrchestreeAI)
-- ====================================================================

-- 1. Channel Verifications Table (WhatsApp OTP & Telegram Deep-Link)
CREATE TABLE IF NOT EXISTS channel_verifications (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    staff_name VARCHAR(255) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    destination VARCHAR(128) NOT NULL,
    otp_code VARCHAR(16) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Enhance Proactive Subscriptions with Assigned Agent and Opt-out timestamps
ALTER TABLE proactive_subscriptions 
    ADD COLUMN IF NOT EXISTS assigned_agent_id VARCHAR(64) REFERENCES ai_agents(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS assigned_agent_name VARCHAR(128) DEFAULT 'Proactive People Agent',
    ADD COLUMN IF NOT EXISTS last_sent_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS opt_out_at TIMESTAMPTZ;

-- 3. Enhance Proactive Messages Log with Audit & Idempotency fields
ALTER TABLE proactive_messages_log 
    ADD COLUMN IF NOT EXISTS job_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS character_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS latency_ms BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS error_message TEXT;

-- 4. Enable RLS on channel_verifications
ALTER TABLE channel_verifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_channel_verifications ON channel_verifications;
DROP POLICY IF EXISTS tenant_isolation_channel_verifications ON channel_verifications;
CREATE POLICY tenant_isolation_channel_verifications ON channel_verifications 
    USING (app_has_tenant_access(tenant_id));
