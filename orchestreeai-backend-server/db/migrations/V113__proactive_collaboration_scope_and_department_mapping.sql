-- ==============================================================================
-- OrchestreeAI Migration V113: Proactive Collaboration Scope & Department AI Mapping
-- PRD Section 11 & Fase 91 Alignment
-- ==============================================================================

-- 1. Tabel department_ai_collaboration_mapping
CREATE TABLE IF NOT EXISTS department_ai_collaboration_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_category_id UUID REFERENCES department_categories(id) ON DELETE CASCADE,
    ai_job_title_id VARCHAR(64) REFERENCES ai_job_titles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (department_category_id, ai_job_title_id)
);

-- 2. Seed Mapping Resmi untuk SELURUH 14 department_categories
-- Sales -> sales, marketing, crm_customer_success
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'sales' AND jt.job_code IN ('sales', 'marketing', 'crm_customer_success')
ON CONFLICT DO NOTHING;

-- Marketing -> marketing, sales, company_intelligence
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'marketing' AND jt.job_code IN ('marketing', 'sales', 'company_intelligence')
ON CONFLICT DO NOTHING;

-- Customer Service -> customer_service, crm_customer_success
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'customer_service' AND jt.job_code IN ('customer_service', 'crm_customer_success')
ON CONFLICT DO NOTHING;

-- HR -> hr_recruitment
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'hr' AND jt.job_code IN ('hr_recruitment')
ON CONFLICT DO NOTHING;

-- Finance -> finance
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'finance' AND jt.job_code IN ('finance')
ON CONFLICT DO NOTHING;

-- Operations -> operations, procurement
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'operations' AND jt.job_code IN ('operations', 'procurement')
ON CONFLICT DO NOTHING;

-- Procurement -> procurement, operations, finance
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'procurement' AND jt.job_code IN ('procurement', 'operations', 'finance')
ON CONFLICT DO NOTHING;

-- Project -> project, task_workflow
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'project' AND jt.job_code IN ('project', 'task_workflow')
ON CONFLICT DO NOTHING;

-- IT & Engineering -> knowledge_document, task_workflow
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'it_engineering' AND jt.job_code IN ('knowledge_document', 'task_workflow')
ON CONFLICT DO NOTHING;

-- Legal -> knowledge_document
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'legal' AND jt.job_code IN ('knowledge_document')
ON CONFLICT DO NOTHING;

-- Research -> research, company_intelligence
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'research' AND jt.job_code IN ('research', 'company_intelligence')
ON CONFLICT DO NOTHING;

-- HSE -> operations
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'hse' AND jt.job_code IN ('operations')
ON CONFLICT DO NOTHING;

-- Quality -> operations, knowledge_document
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'quality' AND jt.job_code IN ('operations', 'knowledge_document')
ON CONFLICT DO NOTHING;

-- Executive -> chief_of_staff, reporting, company_intelligence
INSERT INTO department_ai_collaboration_mapping (department_category_id, ai_job_title_id)
SELECT dc.id, jt.id FROM department_categories dc, ai_job_titles jt
WHERE dc.category_code = 'executive' AND jt.job_code IN ('chief_of_staff', 'reporting', 'company_intelligence')
ON CONFLICT DO NOTHING;

-- 3. Tabel proactive_collaboration_scope
CREATE TABLE IF NOT EXISTS proactive_collaboration_scope (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id VARCHAR(64) REFERENCES users(id) ON DELETE CASCADE,
    scope_type TEXT NOT NULL CHECK (scope_type IN ('department_scoped', 'executive_full_summary')),
    collaborating_ai_job_title_ids TEXT[],
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_proactive_collab_scope_staff_id ON proactive_collaboration_scope(staff_id);

ALTER TABLE department_ai_collaboration_mapping ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_dept_ai_collab_mapping ON department_ai_collaboration_mapping;
CREATE POLICY read_dept_ai_collab_mapping ON department_ai_collaboration_mapping FOR SELECT USING (true);

ALTER TABLE proactive_collaboration_scope ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_proactive_collab_scope ON proactive_collaboration_scope;
CREATE POLICY read_proactive_collab_scope ON proactive_collaboration_scope FOR SELECT USING (true);
