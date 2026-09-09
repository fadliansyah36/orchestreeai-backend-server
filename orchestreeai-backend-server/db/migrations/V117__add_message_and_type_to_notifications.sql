-- ====================================================================
-- OrchestreeAI Database Migration V117: Notifications Message & Type Alignment
-- Aligns 'notifications' table schema with backend notification dispatchers
-- ====================================================================

-- 1. Add message column and type column to notifications table
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS message TEXT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS type VARCHAR(64);

-- 2. Relax constraints on legacy fields (body, category, user_id) so system and tenant-wide notifications succeed
ALTER TABLE notifications ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE notifications ALTER COLUMN category DROP NOT NULL;
ALTER TABLE notifications ALTER COLUMN body DROP NOT NULL;

-- 3. Performance index for tenant and notification type filtering
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_type ON notifications(tenant_id, type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at_desc ON notifications(tenant_id, created_at DESC);

-- 4. Reload PostgREST schema cache
NOTIFY pgrst, 'reload schema';
