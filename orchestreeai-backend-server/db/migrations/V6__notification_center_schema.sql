-- ====================================================================
-- OrchestreeAI Database Migration V6: Notification Center Schema
-- PostgreSQL 16 + Row-Level Security (RLS) + Idempotency Index
-- Covers Centralized Notifications, Fanout Subscriptions, Preferences
-- ====================================================================

-- 1. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(64) NOT NULL, -- TASK, COMPETITOR_INSIGHT, PERFORMANCE_ALERT, INTEGRATION_HEALTH, SECURITY, BILLING, SYSTEM
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    deep_link_route VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    severity VARCHAR(32) NOT NULL DEFAULT 'INFO', -- INFO, WARNING, CRITICAL
    source_event_id VARCHAR(128), -- For deduplication and idempotency
    metadata_json JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMPTZ
);

-- Index for idempotency / dedup check
CREATE UNIQUE INDEX IF NOT EXISTS uq_notifications_source_event_user 
    ON notifications(tenant_id, user_id, source_event_id) 
    WHERE source_event_id IS NOT NULL;

-- Index for fast user inbox queries
CREATE INDEX IF NOT EXISTS idx_notifications_user_inbox 
    ON notifications(tenant_id, user_id, is_read, created_at DESC);

-- 2. Notification Preferences Table (In-App Channel)
CREATE TABLE IF NOT EXISTS notification_preferences (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(64) NOT NULL, -- TASK, COMPETITOR_INSIGHT, PERFORMANCE_ALERT, INTEGRATION_HEALTH, SECURITY, BILLING, SYSTEM
    is_in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notification_pref_user_cat UNIQUE(tenant_id, user_id, category)
);

-- 3. Row Level Security (RLS)
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_notifications ON notifications;
DROP POLICY IF EXISTS tenant_isolation_notifications ON notifications;
CREATE POLICY tenant_isolation_notifications ON notifications 
    USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_notification_preferences ON notification_preferences;
DROP POLICY IF EXISTS tenant_isolation_notification_preferences ON notification_preferences;
CREATE POLICY tenant_isolation_notification_preferences ON notification_preferences 
    USING (app_has_tenant_access(tenant_id));
