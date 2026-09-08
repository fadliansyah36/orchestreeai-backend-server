-- ====================================================================
-- OrchestreeAI Database Migration V115: Autonomous AI Selection Subsystem
-- PRD Module: Universal Autonomous Selection, Ranking, Calibration & Realtime Analytics
-- ====================================================================

-- 1. Selection Requests (Master Header Table)
CREATE TABLE IF NOT EXISTS selection_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id),
    requested_by_user_id VARCHAR(64) NOT NULL,
    domain_category TEXT,  -- 'recruitment'/'supplier'/'finance'/'tender'/'competitor'/'mining'/'marketing'/'sales'/'research'/'general'/dst - DIISI OTOMATIS oleh AI dari Data Understanding
    prompt_text TEXT NOT NULL,  -- instruksi bebas user
    source_type TEXT NOT NULL CHECK (source_type IN
        ('file_upload','prompt_only','whatsapp','telegram','api_database',
         'integration_workflow')),
    assigned_ai_job_title_id VARCHAR(64) REFERENCES ai_job_titles(id),  -- REUSE Katalog 15 Jabatan Utama (Fase 91.H)
    calibration_settings_id UUID,  -- NULL jika user tidak setting kalibrasi
    workflow_execution_id VARCHAR(128) REFERENCES workflow_executions(id),  -- REUSE Orchestration Engine
    status TEXT DEFAULT 'processing' CHECK (status IN
        ('processing','awaiting_review','completed','failed','cancelled')),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 2. Selection Source Documents (File Upload & Object Storage)
CREATE TABLE IF NOT EXISTS selection_source_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    selection_request_id UUID REFERENCES selection_requests(id) ON DELETE CASCADE,
    file_name TEXT,
    file_type TEXT,  -- 'xlsx'/'csv'/'pdf'/'docx'/'image'
    object_storage_url TEXT NOT NULL,  -- Supabase Storage, file NYATA
    extracted_row_count INT,
    extraction_status TEXT DEFAULT 'pending',  -- pending/processing/ready/failed
    detected_schema JSONB,  -- hasil Automatic Schema Detection
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 3. Selection Criteria (Scoring Criteria Matrix)
CREATE TABLE IF NOT EXISTS selection_criteria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    selection_request_id UUID REFERENCES selection_requests(id) ON DELETE CASCADE,
    criterion_name TEXT NOT NULL,
    criterion_source TEXT NOT NULL CHECK (criterion_source IN
        ('user_prompt','company_brain','database_pattern','ai_generated','user_calibration')),
    weight_percentage NUMERIC,  -- diisi dari kalibrasi ATAU AI-determined
    is_user_calibrated BOOLEAN DEFAULT FALSE
);

-- 4. Selection Calibration Settings (Preset Kalibrasi Multi-Tenant)
CREATE TABLE IF NOT EXISTS selection_calibration_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    created_by_user_id VARCHAR(64) NOT NULL,
    calibration_name TEXT,  -- nama preset, agar bisa dipakai ulang
    is_saved_as_preset BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 5. Selection Calibration Items (Bobot & Prioritas Item)
CREATE TABLE IF NOT EXISTS selection_calibration_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    calibration_settings_id UUID REFERENCES selection_calibration_settings(id) ON DELETE CASCADE,
    field_type_name TEXT NOT NULL,  -- "Field Nama Tipe" dari form user
    percentage NUMERIC NOT NULL CHECK (percentage > 0 AND percentage <= 100),
    order_index INT
);

-- Add optional FK between selection_requests and calibration_settings
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_selection_requests_calibration'
    ) THEN
        ALTER TABLE selection_requests
            ADD CONSTRAINT fk_selection_requests_calibration
            FOREIGN KEY (calibration_settings_id)
            REFERENCES selection_calibration_settings(id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- 6. Selection Results (Scored Candidates / Items & Transparansi AI)
CREATE TABLE IF NOT EXISTS selection_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    selection_request_id UUID REFERENCES selection_requests(id) ON DELETE CASCADE,
    row_reference JSONB,  -- data baris asli (nama kandidat/supplier/dst.)
    total_score NUMERIC,
    rank_position INT,
    priority_level TEXT,  -- 'high'/'medium'/'low'
    recommendation_classification TEXT,  -- 'selected'/'review'/'rejected'
    risk_score NUMERIC,
    confidence_score NUMERIC,
    score_breakdown JSONB,  -- skor per kriteria, untuk transparansi
    ai_insight_text TEXT,  -- narasi insight per item
    human_review_status TEXT DEFAULT 'pending_review'
        CHECK (human_review_status IN ('pending_review','approved','rejected','overridden')),
    human_reviewer_id VARCHAR(64),
    human_review_notes TEXT,
    human_reviewed_at TIMESTAMPTZ
);

-- 7. Selection Analytics Summary (Agregasi & Chart Metadata)
CREATE TABLE IF NOT EXISTS selection_analytics_summary (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    selection_request_id UUID REFERENCES selection_requests(id) ON DELETE CASCADE,
    metric_type TEXT NOT NULL,  -- 'kpi'/'distribution'/'trend'/'correlation'/'anomaly'
    metric_data JSONB NOT NULL,  -- data mentah metrik
    suggested_chart_type TEXT,  -- 'bar'/'line'/'pie'/'donut'/'area'/'scatter'/'funnel'/'heatmap'/'ranking_chart'
    chart_data_payload JSONB  -- data terstruktur siap-render untuk chart
);

-- 8. Selection Audit Log (Audit Trail Aksi Lengkap)
CREATE TABLE IF NOT EXISTS selection_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    selection_request_id UUID REFERENCES selection_requests(id),
    action_type TEXT NOT NULL,  -- 'prompt_submitted'/'dataset_uploaded'/'ai_agent_assigned'/'result_generated'/'human_approved'/'human_rejected'/'exported'
    actor_id VARCHAR(64),
    actor_type TEXT,
    detail JSONB,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ====================================================================
-- Indexes for Performance and RLS Filtering
-- ====================================================================
CREATE INDEX IF NOT EXISTS idx_selection_req_tenant_status ON selection_requests(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_selection_req_domain ON selection_requests(domain_category);
CREATE INDEX IF NOT EXISTS idx_selection_source_docs_req ON selection_source_documents(selection_request_id);
CREATE INDEX IF NOT EXISTS idx_selection_criteria_req ON selection_criteria(selection_request_id);
CREATE INDEX IF NOT EXISTS idx_selection_calib_tenant ON selection_calibration_settings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_selection_calib_items_set ON selection_calibration_items(calibration_settings_id);
CREATE INDEX IF NOT EXISTS idx_selection_results_req_rank ON selection_results(selection_request_id, rank_position);
CREATE INDEX IF NOT EXISTS idx_selection_analytics_req ON selection_analytics_summary(selection_request_id);
CREATE INDEX IF NOT EXISTS idx_selection_audit_req ON selection_audit_log(selection_request_id, action_type);

-- ====================================================================
-- Row-Level Security (RLS) Policies (MANDATORI: Tenant Isolation)
-- ====================================================================
ALTER TABLE selection_requests ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_selection ON selection_requests;
CREATE POLICY tenant_isolation_selection ON selection_requests
    USING (tenant_id = current_tenant_id());

ALTER TABLE selection_source_documents ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_source_documents ON selection_source_documents;
CREATE POLICY tenant_isolation_source_documents ON selection_source_documents
    USING (EXISTS (
        SELECT 1 FROM selection_requests sr
        WHERE sr.id = selection_source_documents.selection_request_id
          AND sr.tenant_id = current_tenant_id()
    ));

ALTER TABLE selection_criteria ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_criteria ON selection_criteria;
CREATE POLICY tenant_isolation_criteria ON selection_criteria
    USING (EXISTS (
        SELECT 1 FROM selection_requests sr
        WHERE sr.id = selection_criteria.selection_request_id
          AND sr.tenant_id = current_tenant_id()
    ));

ALTER TABLE selection_calibration_settings ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_calibration_settings ON selection_calibration_settings;
CREATE POLICY tenant_isolation_calibration_settings ON selection_calibration_settings
    USING (tenant_id = current_tenant_id());

ALTER TABLE selection_calibration_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_calibration_items ON selection_calibration_items;
CREATE POLICY tenant_isolation_calibration_items ON selection_calibration_items
    USING (EXISTS (
        SELECT 1 FROM selection_calibration_settings scs
        WHERE scs.id = selection_calibration_items.calibration_settings_id
          AND scs.tenant_id = current_tenant_id()
    ));

ALTER TABLE selection_results ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_results ON selection_results;
CREATE POLICY tenant_isolation_results ON selection_results
    USING (EXISTS (
        SELECT 1 FROM selection_requests sr
        WHERE sr.id = selection_results.selection_request_id
          AND sr.tenant_id = current_tenant_id()
    ));

ALTER TABLE selection_analytics_summary ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_analytics ON selection_analytics_summary;
CREATE POLICY tenant_isolation_analytics ON selection_analytics_summary
    USING (EXISTS (
        SELECT 1 FROM selection_requests sr
        WHERE sr.id = selection_analytics_summary.selection_request_id
          AND sr.tenant_id = current_tenant_id()
    ));

ALTER TABLE selection_audit_log ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_audit_log ON selection_audit_log;
CREATE POLICY tenant_isolation_audit_log ON selection_audit_log
    USING (EXISTS (
        SELECT 1 FROM selection_requests sr
        WHERE sr.id = selection_audit_log.selection_request_id
          AND sr.tenant_id = current_tenant_id()
    ));

-- ====================================================================
-- Realtime Publication (Supabase Realtime Broadcast)
-- ====================================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'selection_requests'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE selection_requests;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'selection_results'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE selection_results;
    END IF;
END $$;
