-- ====================================================================
-- OrchestreeAI Database Migration V94: AI Job Title Operation Mode Permission
-- PRD Addendum 1 & 2 - 5-Layer Isolation Architecture (Langkah 2: Level Persona & Jabatan)
-- Mapping tegas jabatan AI ke operation_mode (customer_facing_omnichannel vs internal_proactive_reporting)
-- ====================================================================

CREATE TABLE IF NOT EXISTS ai_job_title_operation_mode_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_title_id UUID REFERENCES ai_job_titles(id) ON DELETE CASCADE,
    allowed_operation_mode TEXT NOT NULL
        CHECK (allowed_operation_mode IN ('customer_facing_omnichannel', 'internal_proactive_reporting')),
    CONSTRAINT uq_job_title_operation_mode UNIQUE (job_title_id, allowed_operation_mode)
);

-- SEED sesuai domain masing-masing:
-- 1. Customer-Facing Omnichannel Permissions (Sales, Customer Service, CRM, Marketing)
INSERT INTO ai_job_title_operation_mode_permission (job_title_id, allowed_operation_mode)
SELECT id, 'customer_facing_omnichannel' FROM ai_job_titles
    WHERE job_code IN ('sales', 'customer_service', 'crm_customer_success', 'marketing')
ON CONFLICT (job_title_id, allowed_operation_mode) DO NOTHING;

-- 2. Internal Proactive Reporting Permissions (Chief of Staff, HR, Finance, Operations, Reporting, Intel, Project, dsb.)
INSERT INTO ai_job_title_operation_mode_permission (job_title_id, allowed_operation_mode)
SELECT id, 'internal_proactive_reporting' FROM ai_job_titles
    WHERE job_code IN ('chief_of_staff', 'reporting', 'company_intelligence', 'hr_recruitment', 'finance', 'operations', 'project', 'knowledge_document', 'task_workflow', 'procurement', 'research')
ON CONFLICT (job_title_id, allowed_operation_mode) DO NOTHING;

-- Index & RLS
CREATE INDEX IF NOT EXISTS idx_job_title_op_mode_perm ON ai_job_title_operation_mode_permission(job_title_id, allowed_operation_mode);

ALTER TABLE ai_job_title_operation_mode_permission ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_ai_job_title_operation_mode_permission ON ai_job_title_operation_mode_permission;
CREATE POLICY read_ai_job_title_operation_mode_permission ON ai_job_title_operation_mode_permission
    FOR SELECT USING (auth.role() = 'authenticated');
DROP POLICY IF EXISTS manage_ai_job_title_operation_mode_permission ON ai_job_title_operation_mode_permission;
CREATE POLICY manage_ai_job_title_operation_mode_permission ON ai_job_title_operation_mode_permission
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');
