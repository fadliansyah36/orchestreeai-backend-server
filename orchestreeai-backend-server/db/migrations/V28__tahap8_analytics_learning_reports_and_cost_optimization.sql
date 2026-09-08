-- ==============================================================================
-- OrchestreeAI Migration: Tahap 8 — Omnichannel Analytics, Closed-Loop Learning, Automatic Reports & Cost Optimization Engine
-- File: V28__tahap8_analytics_learning_reports_and_cost_optimization.sql
-- PRD Source: Master Bagian 18 (Token & Cost Optimization), Addendum 1 Bagian 45 (Omnichannel Analytics & AI Sales Coach), 
--             Addendum 2 Bagian 65 (Automatic Enterprise Reports), Addendum 2 Bagian 74 (Closed-Loop Learning)
-- Deskripsi: Skema analitik eksperimen A/B pesan, AI Sales Coach playbooks, closed-loop continuous learning agen, 
--            laporan eksekutif otomatis terverifikasi data mentah, prompt caching, dan semantic response cache.
-- ==============================================================================

-- 1. Message Experiments (A/B Testing Pesan Komunikasi & Sales)
CREATE TABLE IF NOT EXISTS message_experiments (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    name TEXT NOT NULL,
    hypothesis TEXT NOT NULL,
    channel_type TEXT NOT NULL DEFAULT 'ALL', -- 'ALL', 'WHATSAPP', 'INSTAGRAM', 'TELEGRAM', 'EMAIL'
    goal_metric TEXT NOT NULL DEFAULT 'CONVERSION_RATE', -- 'CONVERSION_RATE', 'REPLY_RATE', 'REVENUE_PER_MESSAGE'
    status TEXT NOT NULL DEFAULT 'ACTIVE', -- 'DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED'
    sample_size_min_threshold INT NOT NULL DEFAULT 30,
    winning_variant_id TEXT,
    confidence_level_pct DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    z_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    p_value DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    is_statistically_significant BOOLEAN NOT NULL DEFAULT FALSE,
    conclusion_note TEXT NOT NULL DEFAULT '',
    start_date BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    end_date BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_msg_exp_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_msg_exp_tenant_id ON message_experiments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_msg_exp_status ON message_experiments(status);
CREATE INDEX IF NOT EXISTS idx_msg_exp_channel ON message_experiments(channel_type);
CREATE INDEX IF NOT EXISTS idx_msg_exp_created_at ON message_experiments(created_at);

-- 2. Message Experiment Variants
CREATE TABLE IF NOT EXISTS message_experiment_variants (
    id TEXT PRIMARY KEY,
    experiment_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    variant_code TEXT NOT NULL, -- 'A', 'B', 'C'
    variant_name TEXT NOT NULL,
    message_template TEXT NOT NULL,
    hook_type TEXT NOT NULL DEFAULT 'PROBLEM_AGITATION', -- 'PROBLEM_AGITATION', 'DISCOUNT_OFFER', 'SOCIAL_PROOF', 'QUESTION', 'DIRECT_BENEFIT'
    call_to_action TEXT NOT NULL DEFAULT 'Beli Sekarang',
    sent_count INT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    checkout_count INT NOT NULL DEFAULT 0,
    paid_order_count INT NOT NULL DEFAULT 0,
    total_revenue DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    traffic_share_pct DOUBLE PRECISION NOT NULL DEFAULT 50.0,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_msg_exp_var_exp FOREIGN KEY (experiment_id)
        REFERENCES message_experiments(id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_exp_var_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_msg_var_exp_id ON message_experiment_variants(experiment_id);
CREATE INDEX IF NOT EXISTS idx_msg_var_tenant_id ON message_experiment_variants(tenant_id);
CREATE INDEX IF NOT EXISTS idx_msg_var_code ON message_experiment_variants(variant_code);

-- 3. Message Experiment Send Logs
CREATE TABLE IF NOT EXISTS message_experiment_send_logs (
    id TEXT PRIMARY KEY,
    experiment_id TEXT NOT NULL,
    variant_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    customer_id TEXT NOT NULL,
    customer_name TEXT NOT NULL,
    conversation_id TEXT,
    order_id TEXT,
    channel_type TEXT NOT NULL,
    replied BOOLEAN NOT NULL DEFAULT FALSE,
    checked_out BOOLEAN NOT NULL DEFAULT FALSE,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    revenue_generated DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    sent_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    converted_at BIGINT,
    CONSTRAINT fk_msg_log_exp FOREIGN KEY (experiment_id)
        REFERENCES message_experiments(id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_log_var FOREIGN KEY (variant_id)
        REFERENCES message_experiment_variants(id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_log_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_log_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_msg_log_exp_id ON message_experiment_send_logs(experiment_id);
CREATE INDEX IF NOT EXISTS idx_msg_log_var_id ON message_experiment_send_logs(variant_id);
CREATE INDEX IF NOT EXISTS idx_msg_log_tenant_id ON message_experiment_send_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_msg_log_customer_id ON message_experiment_send_logs(customer_id);
CREATE INDEX IF NOT EXISTS idx_msg_log_sent_at ON message_experiment_send_logs(sent_at);

-- 4. AI Sales Coach Analyses (Playbook & Evaluasi Sales Performance)
CREATE TABLE IF NOT EXISTS ai_sales_coach_analyses (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    period TEXT NOT NULL DEFAULT 'Current 30 Days',
    total_staff_evaluated INT NOT NULL DEFAULT 0,
    top_performer_staff_id TEXT,
    top_performer_name TEXT,
    top_conversion_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    low_performer_staff_id TEXT,
    low_performer_name TEXT,
    low_conversion_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    winning_tactics_json TEXT NOT NULL DEFAULT '[]',
    common_mistakes_json TEXT NOT NULL DEFAULT '[]',
    recommended_playbook_doc_id TEXT,
    playbook_title TEXT NOT NULL DEFAULT '',
    playbook_summary TEXT NOT NULL DEFAULT '',
    analyzed_by_model TEXT NOT NULL DEFAULT 'gemini-3.5-pro',
    analyzed_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_sales_coach_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sales_coach_tenant_id ON ai_sales_coach_analyses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sales_coach_analyzed_at ON ai_sales_coach_analyses(analyzed_at);

-- 5. Agent Decision Outcomes (Closed-Loop Learning: Ground Truth Feedback)
CREATE TABLE IF NOT EXISTS agent_decision_outcomes (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    agent_id TEXT NOT NULL,
    agent_name TEXT NOT NULL,
    node_id TEXT NOT NULL,
    execution_id TEXT NOT NULL,
    workflow_id TEXT NOT NULL,
    scenario_context TEXT NOT NULL,
    action_type TEXT NOT NULL,
    predicted_impact TEXT NOT NULL,
    actual_outcome TEXT NOT NULL,
    outcome_source TEXT NOT NULL, -- 'PAYMENT_WEBHOOK', 'MONITORING_LOOP_RESULT', 'HUMAN_EXPLICIT_APPROVAL', 'HUMAN_EXPLICIT_REJECT', 'SOURCE_SYSTEM_VERIFICATION', 'SOP_PRECHECK'
    outcome_classification TEXT NOT NULL, -- 'REINFORCE', 'CORRECT', 'LEARN_FROM_REJECTION'
    verified_by TEXT NOT NULL,
    confidence_delta DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    metric_impact_json TEXT NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_agent_outcome_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_outcome_agent FOREIGN KEY (agent_id)
        REFERENCES ai_agents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_agent_outcome_tenant ON agent_decision_outcomes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_outcome_agent ON agent_decision_outcomes(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_outcome_workflow ON agent_decision_outcomes(workflow_id);
CREATE INDEX IF NOT EXISTS idx_agent_outcome_action ON agent_decision_outcomes(action_type);

-- 6. Agent Lesson Learned (Knowledge Base Pembelajaran Otonom Hasil Evaluasi)
CREATE TABLE IF NOT EXISTS agent_lesson_learned (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    agent_id TEXT NOT NULL,
    skill_id TEXT NOT NULL,
    domain_context TEXT NOT NULL,
    failure_pattern TEXT NOT NULL,
    root_cause_analysis TEXT NOT NULL,
    corrective_guidance TEXT NOT NULL,
    sample_count INT NOT NULL DEFAULT 1,
    confidence_score DOUBLE PRECISION NOT NULL DEFAULT 0.85,
    status TEXT NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'SUPERSEDED', 'INVALIDATED_BY_ADMIN'
    validated_by TEXT NOT NULL DEFAULT 'CONTINUOUS_LEARNING_CORE',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_agent_lesson_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_lesson_agent FOREIGN KEY (agent_id)
        REFERENCES ai_agents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_agent_lesson_tenant ON agent_lesson_learned(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_lesson_agent ON agent_lesson_learned(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_lesson_skill ON agent_lesson_learned(skill_id);
CREATE INDEX IF NOT EXISTS idx_agent_lesson_status ON agent_lesson_learned(status);

-- 7. Agent Skill Confidence (Skor Keyakinan Skill Per Agen)
CREATE TABLE IF NOT EXISTS agent_skill_confidence (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    agent_id TEXT NOT NULL,
    skill_id TEXT NOT NULL,
    skill_name TEXT NOT NULL,
    current_confidence_score DOUBLE PRECISION NOT NULL DEFAULT 50.0, -- Skala 10.0 - 99.0
    sample_size INT NOT NULL DEFAULT 0,
    reinforce_count INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    rejection_count INT NOT NULL DEFAULT 0,
    last_evaluated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    decay_factor DOUBLE PRECISION NOT NULL DEFAULT 0.98,
    CONSTRAINT fk_agent_conf_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_conf_agent FOREIGN KEY (agent_id)
        REFERENCES ai_agents(id) ON DELETE CASCADE,
    CONSTRAINT uq_agent_skill_confidence UNIQUE (tenant_id, agent_id, skill_id)
);

CREATE INDEX IF NOT EXISTS idx_agent_conf_tenant ON agent_skill_confidence(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_conf_agent ON agent_skill_confidence(agent_id);

-- 8. Agent Skill Growth Log (Riwayat Fluktuasi Keyakinan Skill)
CREATE TABLE IF NOT EXISTS agent_skill_growth_log (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    agent_id TEXT NOT NULL,
    skill_id TEXT NOT NULL,
    previous_score DOUBLE PRECISION NOT NULL,
    new_score DOUBLE PRECISION NOT NULL,
    delta DOUBLE PRECISION NOT NULL,
    trigger_outcome_id TEXT NOT NULL,
    reason TEXT NOT NULL,
    logged_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_agent_growth_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_growth_agent FOREIGN KEY (agent_id)
        REFERENCES ai_agents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_agent_growth_tenant ON agent_skill_growth_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_growth_agent ON agent_skill_growth_log(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_growth_skill ON agent_skill_growth_log(skill_id);

-- 9. Automatic Reports (Laporan Otomatis Eksekutif & Departemen)
CREATE TABLE IF NOT EXISTS automatic_reports (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    report_type TEXT NOT NULL, -- 'daily', 'weekly', 'monthly', 'management', 'proactive'
    scope TEXT NOT NULL DEFAULT 'company', -- 'company', 'department', 'project'
    title TEXT NOT NULL,
    executive_summary TEXT NOT NULL,
    content_ref TEXT,
    delivered_channels_json TEXT NOT NULL DEFAULT '["APP_FEED"]',
    status TEXT NOT NULL DEFAULT 'DELIVERED', -- 'GENERATED', 'DELIVERED', 'ACKNOWLEDGED'
    date_string TEXT NOT NULL DEFAULT '', -- 'YYYY-MM-DD'
    data_points_count INT NOT NULL DEFAULT 0,
    overall_health_score DOUBLE PRECISION NOT NULL DEFAULT 90.0,
    risk_severity TEXT NOT NULL DEFAULT 'LOW', -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    generated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    delivered_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_auto_reports_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auto_reports_tenant ON automatic_reports(tenant_id);
CREATE INDEX IF NOT EXISTS idx_auto_reports_type ON automatic_reports(report_type);
CREATE INDEX IF NOT EXISTS idx_auto_reports_scope ON automatic_reports(scope);
CREATE INDEX IF NOT EXISTS idx_auto_reports_tenant_gen ON automatic_reports(tenant_id, generated_at);
CREATE INDEX IF NOT EXISTS idx_auto_reports_type_gen ON automatic_reports(tenant_id, report_type, generated_at);

-- 10. Report Data Points (Metrik Nyata Terverifikasi Pelindung Anti-Halusinasi)
CREATE TABLE IF NOT EXISTS report_data_points (
    id TEXT PRIMARY KEY,
    report_id TEXT NOT NULL,
    category TEXT NOT NULL, -- 'operations', 'project', 'equipment', 'hse', 'procurement', 'finance', 'risk'
    metric_key TEXT NOT NULL,
    metric_name TEXT NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    formatted_value TEXT NOT NULL,
    source_reference TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'NORMAL', -- 'NORMAL', 'WARNING', 'CRITICAL'
    recorded_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_report_datapoints_report FOREIGN KEY (report_id)
        REFERENCES automatic_reports(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_report_dp_report_id ON report_data_points(report_id);
CREATE INDEX IF NOT EXISTS idx_report_dp_category ON report_data_points(category);
CREATE INDEX IF NOT EXISTS idx_report_dp_key ON report_data_points(metric_key);

-- 11. Prompt Cache Registry (Native Provider Prompt Caching)
CREATE TABLE IF NOT EXISTS prompt_cache_registry (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    system_prompt_hash TEXT NOT NULL,
    cache_provider_ref TEXT NOT NULL,
    token_count INT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    expires_at BIGINT NOT NULL DEFAULT ((EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT + 1800000), -- 30 menit
    hit_count INT NOT NULL DEFAULT 0,
    last_hit_at BIGINT,
    total_tokens_saved INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_prompt_cache_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_prompt_cache_tenant ON prompt_cache_registry(tenant_id);
CREATE INDEX IF NOT EXISTS idx_prompt_cache_hash ON prompt_cache_registry(system_prompt_hash);
CREATE INDEX IF NOT EXISTS idx_prompt_cache_expires ON prompt_cache_registry(expires_at);

-- 12. Semantic Response Cache (Cache Respons Semantik Berbasis Kemiripan Vektor)
CREATE TABLE IF NOT EXISTS semantic_response_cache (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    query_embedding_json TEXT NOT NULL DEFAULT '[]',
    query_text TEXT NOT NULL,
    response_text TEXT NOT NULL,
    source_data_hash TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'FAQ_CUSTOMER_SERVICE', -- 'FAQ_CUSTOMER_SERVICE', 'KNOWLEDGE_RULE_SOP', 'GENERAL_INFO'
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    expires_at BIGINT NOT NULL DEFAULT ((EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT + 3600000), -- 1 jam
    hit_count INT NOT NULL DEFAULT 0,
    last_hit_at BIGINT,
    total_cost_saved_usd DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    CONSTRAINT fk_semantic_cache_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_semantic_cache_tenant ON semantic_response_cache(tenant_id);
CREATE INDEX IF NOT EXISTS idx_semantic_cache_hash ON semantic_response_cache(source_data_hash);
CREATE INDEX IF NOT EXISTS idx_semantic_cache_category ON semantic_response_cache(category);
CREATE INDEX IF NOT EXISTS idx_semantic_cache_expires ON semantic_response_cache(expires_at);

-- 13. Batch Task Queue (Antrean Pemrosesan Batch Async Diskon 50%)
CREATE TABLE IF NOT EXISTS batch_task_queue (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    task_type TEXT NOT NULL, -- 'DAILY_REPORT', 'BULK_EMBEDDING', 'SALES_COACH_ANALYSIS'
    payload_json TEXT NOT NULL DEFAULT '{}',
    status TEXT NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SUBMITTED_BATCH', 'COMPLETED', 'FAILED'
    batch_job_id TEXT,
    result_json TEXT,
    discount_pct DOUBLE PRECISION NOT NULL DEFAULT 50.0,
    cost_without_discount_usd DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    actual_cost_usd DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    completed_at BIGINT,
    CONSTRAINT fk_batch_task_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_batch_task_tenant ON batch_task_queue(tenant_id);
CREATE INDEX IF NOT EXISTS idx_batch_task_status ON batch_task_queue(status);
CREATE INDEX IF NOT EXISTS idx_batch_task_type ON batch_task_queue(task_type);

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================
ALTER TABLE message_experiments ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_experiment_variants ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_experiment_send_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_sales_coach_analyses ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_decision_outcomes ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_lesson_learned ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_skill_confidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_skill_growth_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE automatic_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_data_points ENABLE ROW LEVEL SECURITY;
ALTER TABLE prompt_cache_registry ENABLE ROW LEVEL SECURITY;
ALTER TABLE semantic_response_cache ENABLE ROW LEVEL SECURITY;
ALTER TABLE batch_task_queue ENABLE ROW LEVEL SECURITY;

-- 1. Policies for message_experiments
DROP POLICY IF EXISTS tenant_isolation_msg_exp ON message_experiments;
CREATE POLICY tenant_isolation_msg_exp ON message_experiments FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 2. Policies for message_experiment_variants
DROP POLICY IF EXISTS tenant_isolation_msg_var ON message_experiment_variants;
CREATE POLICY tenant_isolation_msg_var ON message_experiment_variants FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 3. Policies for message_experiment_send_logs
DROP POLICY IF EXISTS tenant_isolation_msg_logs ON message_experiment_send_logs;
CREATE POLICY tenant_isolation_msg_logs ON message_experiment_send_logs FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 4. Policies for ai_sales_coach_analyses
DROP POLICY IF EXISTS tenant_isolation_sales_coach ON ai_sales_coach_analyses;
CREATE POLICY tenant_isolation_sales_coach ON ai_sales_coach_analyses FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 5. Policies for agent_decision_outcomes
DROP POLICY IF EXISTS tenant_isolation_agent_outcomes ON agent_decision_outcomes;
CREATE POLICY tenant_isolation_agent_outcomes ON agent_decision_outcomes FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 6. Policies for agent_lesson_learned
DROP POLICY IF EXISTS tenant_isolation_agent_lessons ON agent_lesson_learned;
CREATE POLICY tenant_isolation_agent_lessons ON agent_lesson_learned FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 7. Policies for agent_skill_confidence
DROP POLICY IF EXISTS tenant_isolation_agent_confidence ON agent_skill_confidence;
CREATE POLICY tenant_isolation_agent_confidence ON agent_skill_confidence FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 8. Policies for agent_skill_growth_log
DROP POLICY IF EXISTS tenant_isolation_agent_growth ON agent_skill_growth_log;
CREATE POLICY tenant_isolation_agent_growth ON agent_skill_growth_log FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 9. Policies for automatic_reports
DROP POLICY IF EXISTS tenant_isolation_auto_reports ON automatic_reports;
CREATE POLICY tenant_isolation_auto_reports ON automatic_reports FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 10. Policies for report_data_points (Via parent report)
DROP POLICY IF EXISTS p_report_data_points_access ON report_data_points;
CREATE POLICY p_report_data_points_access ON report_data_points FOR ALL USING (
    EXISTS (SELECT 1 FROM automatic_reports r WHERE r.id = report_id AND app_has_tenant_access(r.tenant_id))
    OR auth.role() = 'service_role'
);

-- 11. Policies for prompt_cache_registry
DROP POLICY IF EXISTS tenant_isolation_prompt_cache ON prompt_cache_registry;
CREATE POLICY tenant_isolation_prompt_cache ON prompt_cache_registry FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 12. Policies for semantic_response_cache
DROP POLICY IF EXISTS tenant_isolation_semantic_cache ON semantic_response_cache;
CREATE POLICY tenant_isolation_semantic_cache ON semantic_response_cache FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 13. Policies for batch_task_queue
DROP POLICY IF EXISTS tenant_isolation_batch_tasks ON batch_task_queue;
CREATE POLICY tenant_isolation_batch_tasks ON batch_task_queue FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');
