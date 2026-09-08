-- ====================================================================
-- OrchestreeAI Database Migration V111: Proactive AI Self-Initiated Tasks & Tracking
-- PRD Master Fase 110: Task Management Expansion
-- ====================================================================

-- 1. Extend tasks table
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS created_by_type TEXT DEFAULT 'human'
    CHECK (created_by_type IN ('human', 'ai_agent_self_initiated', 'orchestration_engine'));

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS ai_activity_status_line TEXT;

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS third_party_monitoring_target TEXT;

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS source_channel TEXT DEFAULT 'dashboard'
    CHECK (source_channel IN ('dashboard', 'telegram', 'whatsapp'));

-- 1.1 Extend performance_metrics_daily with proactive task breakdowns
ALTER TABLE performance_metrics_daily ADD COLUMN IF NOT EXISTS human_tasks_count INT DEFAULT 0;
ALTER TABLE performance_metrics_daily ADD COLUMN IF NOT EXISTS ai_self_initiated_tasks_count INT DEFAULT 0;
ALTER TABLE performance_metrics_daily ADD COLUMN IF NOT EXISTS orchestration_tasks_count INT DEFAULT 0;
ALTER TABLE performance_metrics_daily ADD COLUMN IF NOT EXISTS telegram_tasks_count INT DEFAULT 0;
ALTER TABLE performance_metrics_daily ADD COLUMN IF NOT EXISTS whatsapp_tasks_count INT DEFAULT 0;
ALTER TABLE performance_metrics_daily ADD COLUMN IF NOT EXISTS dashboard_tasks_count INT DEFAULT 0;

-- 2. Task Checklists
CREATE TABLE IF NOT EXISTS task_checklists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id VARCHAR(64) REFERENCES tasks(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) REFERENCES tenants(id) ON DELETE CASCADE,
    item_text TEXT NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    completed_by_id VARCHAR(64),
    completed_by_type TEXT,
    order_index INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 3. Task Activity Log
CREATE TABLE IF NOT EXISTS task_activity_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id VARCHAR(64) REFERENCES tasks(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) REFERENCES tenants(id) ON DELETE CASCADE,
    actor_id VARCHAR(64),
    actor_type TEXT,
    action TEXT,
    detail TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 4. Indices for Performance
CREATE INDEX IF NOT EXISTS idx_task_checklists_task_id ON task_checklists(task_id);
CREATE INDEX IF NOT EXISTS idx_task_activity_log_task_id ON task_activity_log(task_id);
CREATE INDEX IF NOT EXISTS idx_tasks_monitoring_target ON tasks(third_party_monitoring_target);
CREATE INDEX IF NOT EXISTS idx_tasks_created_by_type ON tasks(created_by_type);
CREATE INDEX IF NOT EXISTS idx_tasks_source_channel ON tasks(source_channel);

-- 5. Row Level Security
ALTER TABLE task_checklists ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_activity_log ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_task_checklists ON task_checklists;
CREATE POLICY tenant_isolation_task_checklists ON task_checklists FOR ALL USING (true);

DROP POLICY IF EXISTS tenant_isolation_task_activity_log ON task_activity_log;
CREATE POLICY tenant_isolation_task_activity_log ON task_activity_log FOR ALL USING (true);

-- 6. Register to supabase_realtime publication
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        CREATE PUBLICATION supabase_realtime;
    END IF;
END $$;

DO $$
DECLARE
    tbl text;
    tables text[] := ARRAY[
        'tasks',
        'task_checklists',
        'task_activity_log'
    ];
BEGIN
    FOREACH tbl IN ARRAY tables LOOP
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = tbl) THEN
            EXECUTE format('ALTER TABLE public.%I REPLICA IDENTITY FULL;', tbl);
            BEGIN
                EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I;', tbl);
            EXCEPTION WHEN duplicate_object THEN
                NULL;
            END;
        END IF;
    END LOOP;
END $$;
