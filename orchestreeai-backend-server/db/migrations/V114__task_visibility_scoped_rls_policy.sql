-- ==============================================================================
-- OrchestreeAI Migration V114: Task Visibility Scoped RLS Policy (LANGKAH 1.2)
-- Sesuai prinsip Defense-in-Depth & Task Board Scoping
-- ==============================================================================

-- 1. Pastikan kolom team_id, board_id, ai_job_title_id tersedia pada tabel tasks
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS team_id VARCHAR(64);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS board_id VARCHAR(64) DEFAULT 'default';
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS ai_job_title_id VARCHAR(64);

-- 2. Tabel task_ai_collaboration_scope
CREATE TABLE IF NOT EXISTS task_ai_collaboration_scope (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id VARCHAR(64) REFERENCES tasks(id) ON DELETE CASCADE,
    ai_job_title_id TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_task_ai_collab_scope_task_id ON task_ai_collaboration_scope(task_id);
CREATE INDEX IF NOT EXISTS idx_task_ai_collab_scope_job ON task_ai_collaboration_scope(ai_job_title_id);

-- 3. SQL helper functions pembaca JWT session / context
CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS VARCHAR AS $$
BEGIN
    RETURN COALESCE(
        current_setting('app.current_tenant_id', true),
        (current_setting('request.jwt.claims', true)::jsonb ->> 'tenant_id'),
        ''
    );
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION current_user_id() RETURNS VARCHAR AS $$
BEGIN
    RETURN COALESCE(
        current_setting('app.current_user_id', true),
        (current_setting('request.jwt.claims', true)::jsonb ->> 'sub'),
        ''
    );
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION current_user_job_level() RETURNS VARCHAR AS $$
BEGIN
    RETURN COALESCE(
        current_setting('app.current_user_job_level', true),
        (current_setting('request.jwt.claims', true)::jsonb ->> 'job_level'),
        (SELECT LOWER(job_level) FROM user_persona WHERE user_id = current_user_id() LIMIT 1),
        (SELECT LOWER(job_title) FROM staff_profiles WHERE user_id = current_user_id() LIMIT 1),
        'staff'
    );
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION current_user_department_id() RETURNS VARCHAR AS $$
BEGIN
    RETURN COALESCE(
        current_setting('app.current_user_department_id', true),
        (current_setting('request.jwt.claims', true)::jsonb ->> 'department_id'),
        (SELECT department_id FROM users WHERE id = current_user_id() LIMIT 1),
        (SELECT department_id FROM staff_profiles WHERE user_id = current_user_id() LIMIT 1),
        ''
    );
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION current_user_team_id() RETURNS VARCHAR AS $$
BEGIN
    RETURN COALESCE(
        current_setting('app.current_user_team_id', true),
        (current_setting('request.jwt.claims', true)::jsonb ->> 'team_id'),
        ''
    );
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION current_user_collaboration_scope() RETURNS TEXT[] AS $$
BEGIN
    RETURN COALESCE(
        (SELECT array_agg(t.job_code) FROM (
            SELECT unnest(collaborating_ai_job_title_ids)::text AS job_code 
            FROM proactive_collaboration_scope 
            WHERE staff_id = current_user_id()
        ) t),
        ARRAY[]::TEXT[]
    );
END;
$$ LANGUAGE plpgsql STABLE;

-- 4. Enable RLS dan Definisikan Policy task_visibility_scoped PERSIS sesuai PRD & Mandat
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS task_visibility_scoped ON tasks;

CREATE POLICY task_visibility_scoped ON tasks
    USING (
        tenant_id = current_tenant_id() AND (
            current_user_job_level() IN ('owner','direksi','vp','gm')
            OR (current_user_job_level() IN ('manajer','supervisor')
                AND department_id = current_user_department_id())
            OR assignee_id = current_user_id()
            OR team_id = current_user_team_id()
            OR id IN (SELECT task_id FROM task_ai_collaboration_scope
                      WHERE ai_job_title_id = ANY(current_user_collaboration_scope()))
        )
    );
