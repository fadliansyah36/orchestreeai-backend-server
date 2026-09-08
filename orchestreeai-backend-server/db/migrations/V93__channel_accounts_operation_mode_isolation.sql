-- ====================================================================
-- OrchestreeAI Database Migration V93: Strict Channel Account Operation Mode Isolation
-- PRD Addendum 1 & 2 - 5-Layer Isolation Architecture (Langkah 1: Skema Data)
-- Enforces structural separation between customer_facing_omnichannel and internal_proactive_reporting
-- ====================================================================

-- 1. Add operation_mode column with mandatory CHECK constraint
ALTER TABLE channel_accounts 
    ADD COLUMN IF NOT EXISTS operation_mode TEXT NOT NULL DEFAULT 'customer_facing_omnichannel'
    CONSTRAINT chk_channel_account_operation_mode 
    CHECK (operation_mode IN ('customer_facing_omnichannel', 'internal_proactive_reporting'));

-- 2. Backfill and migrate existing data strictly based on connection_mode
UPDATE channel_accounts 
    SET operation_mode = 'customer_facing_omnichannel'
    WHERE connection_mode IN ('qr_session', 'bot_token_tenant_owned', 'oauth_platform_app', 'manual_link');

UPDATE channel_accounts 
    SET operation_mode = 'internal_proactive_reporting'
    WHERE connection_mode = 'official_orchestree_channel';

-- 3. Add Index for high performance mode filtering and isolation
CREATE INDEX IF NOT EXISTS idx_channel_acc_operation_mode ON channel_accounts(tenant_id, operation_mode, status);
