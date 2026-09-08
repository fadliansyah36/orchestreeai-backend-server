-- ====================================================================
-- OrchestreeAI Database Migration V8: Gamification & Staff Badges
-- ====================================================================

-- 1. Badge Definitions Catalog Table
CREATE TABLE IF NOT EXISTS badge_definitions (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    icon_key VARCHAR(64) NOT NULL DEFAULT 'military_tech',
    criteria_type VARCHAR(64) NOT NULL, -- e.g. TOP_PERFORMER_MONTHLY, TASK_MASTER, CONSISTENT_7_DAYS, TOP_COLLABORATOR, AI_CHAMPION
    criteria_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    tier VARCHAR(16) NOT NULL DEFAULT 'BRONZE', -- BRONZE, SILVER, GOLD
    category VARCHAR(32) NOT NULL DEFAULT 'PERFORMANCE', -- PERFORMANCE, PRODUCTIVITY, ATTENDANCE, COLLABORATION, AI_SYNERGY
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_badge_criteria ON badge_definitions(criteria_type, tier);

-- 2. Staff Badges Awarded Table (Guaranteed unique per staff_id, badge_id, period)
CREATE TABLE IF NOT EXISTS staff_badges (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    staff_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id VARCHAR(64) NOT NULL REFERENCES badge_definitions(id) ON DELETE CASCADE,
    awarded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    period VARCHAR(32) NOT NULL, -- e.g. "Agustus 2026"
    evidence_ref TEXT NOT NULL, -- Direct audit traceability to source data (scores, attendance, tasks)
    CONSTRAINT uq_staff_badge_period UNIQUE (staff_id, badge_id, period)
);

CREATE INDEX IF NOT EXISTS idx_staff_badges_tenant_staff ON staff_badges(tenant_id, staff_id);
CREATE INDEX IF NOT EXISTS idx_staff_badges_period ON staff_badges(period);

-- 3. Row Level Security on staff_badges
ALTER TABLE staff_badges ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_staff_badges ON staff_badges;
DROP POLICY IF EXISTS tenant_isolation_staff_badges ON staff_badges;
CREATE POLICY tenant_isolation_staff_badges ON staff_badges USING (app_has_tenant_access(tenant_id));

-- 4. Seed Default System Badge Definitions (Official Master Catalog)
INSERT INTO badge_definitions (id, name, description, icon_key, criteria_type, criteria_config, tier, category, order_index)
VALUES 
(
    'badge_top_performer_monthly',
    'Top Performer Bulanan',
    'Meraih peringkat #1 dalam evaluasi performa menyeluruh (skor tertinggi) periode bulanan.',
    'military_tech',
    'TOP_PERFORMER_MONTHLY',
    '{"targetRank": 1, "minScore": 85.0}'::jsonb,
    'GOLD',
    'PERFORMANCE',
    1
),
(
    'badge_ai_champion',
    'AI Collaboration Champion',
    'Menyelesaikan tugas strategis bersama AI Agent dengan integrasi workflow otonom dan SLA tepat waktu.',
    'smart_toy',
    'AI_CHAMPION',
    '{"minJointAiTasks": 3}'::jsonb,
    'GOLD',
    'AI_SYNERGY',
    2
),
(
    'badge_best_collaborator',
    'Kolaborator Terbaik',
    'Skor kolaborasi lintas departemen tertinggi atau di atas 90% pada evaluasi performa aktif.',
    'handshake',
    'TOP_COLLABORATOR',
    '{"minCollaborationScore": 90.0}'::jsonb,
    'SILVER',
    'COLLABORATION',
    3
),
(
    'badge_task_master',
    'Task Master',
    'Menuntaskan minimal 5 tugas tepat waktu tanpa keterlambatan SLA dalam satu siklus evaluasi.',
    'task_alt',
    'TASK_MASTER',
    '{"minTasksOnTime": 5}'::jsonb,
    'SILVER',
    'PRODUCTIVITY',
    4
),
(
    'badge_consistent_7_days',
    'Konsisten 7 Hari',
    'Melakukan presensi absensi tepat waktu berturut-turut tanpa keterlambatan atau anomali geofence.',
    'timer',
    'CONSISTENT_7_DAYS',
    '{"minStreakDays": 7}'::jsonb,
    'BRONZE',
    'ATTENDANCE',
    5
)
ON CONFLICT (id) DO NOTHING;
