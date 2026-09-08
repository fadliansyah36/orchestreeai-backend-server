-- ==============================================================================
-- OrchestreeAI Migration: Tahap 10 — Workforce Performance, Attendance & Geofencing, Workflow Engine, Brand & Briefs
-- File: V30__tahap10_workforce_campaigns_knowledge_and_enterprise_briefs.sql
-- PRD Source: Master Bagian 3, 4, 11, 14, 21, Addendum 1 Bagian 39-44, Addendum 2 Bagian 60-75
-- Deskripsi: Skema kehadiran biometrik & geofence, evaluasi kinerja & KPI, eksekutif briefs, workflow DAG orchestrator, 
--            LLM model registry, brand guidelines, intelijen kompetitor, dan sesi pengguna enterprise.
-- ==============================================================================

-- 1. Work Locations (Lokasi Kantor/Cabang Perusahaan)
CREATE TABLE IF NOT EXISTS work_locations (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    radius_meters DOUBLE PRECISION NOT NULL DEFAULT 75.0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_work_loc_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_work_loc_tenant ON work_locations(tenant_id);

-- 2. Geofence Zones (Zona Absensi & Perimeter Lapangan)
CREATE TABLE IF NOT EXISTS geofence_zones (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    location_id TEXT NOT NULL,
    zone_name TEXT NOT NULL,
    center_lat DOUBLE PRECISION NOT NULL,
    center_lng DOUBLE PRECISION NOT NULL,
    radius_meters DOUBLE PRECISION NOT NULL DEFAULT 50.0,
    allowed_roles_json TEXT NOT NULL DEFAULT '["ALL"]',
    is_strict BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_geofence_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_geofence_loc FOREIGN KEY (location_id)
        REFERENCES work_locations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_geofence_tenant ON geofence_zones(tenant_id);
CREATE INDEX IF NOT EXISTS idx_geofence_loc ON geofence_zones(location_id);

-- 3. Attendance Records (Pencatatan Kehadiran Biometrik & GPS)
CREATE TABLE IF NOT EXISTS attendance_records (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    user_name TEXT NOT NULL,
    department_name TEXT NOT NULL DEFAULT 'Operasional',
    attendance_date TEXT NOT NULL, -- 'YYYY-MM-DD'
    check_in_time BIGINT,
    check_in_lat DOUBLE PRECISION,
    check_in_lng DOUBLE PRECISION,
    check_in_photo_url TEXT,
    check_in_face_verified BOOLEAN NOT NULL DEFAULT FALSE,
    check_in_inside_geofence BOOLEAN NOT NULL DEFAULT FALSE,
    check_out_time BIGINT,
    check_out_lat DOUBLE PRECISION,
    check_out_lng DOUBLE PRECISION,
    check_out_photo_url TEXT,
    check_out_face_verified BOOLEAN NOT NULL DEFAULT FALSE,
    check_out_inside_geofence BOOLEAN NOT NULL DEFAULT FALSE,
    status TEXT NOT NULL DEFAULT 'PRESENT', -- 'PRESENT', 'LATE', 'LEAVE', 'ABSENT', 'WFH'
    work_duration_minutes INT NOT NULL DEFAULT 0,
    notes TEXT NOT NULL DEFAULT '',
    anomaly_flag BOOLEAN NOT NULL DEFAULT FALSE,
    anomaly_reason TEXT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_att_rec_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_att_rec_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_att_rec_tenant ON attendance_records(tenant_id);
CREATE INDEX IF NOT EXISTS idx_att_rec_user ON attendance_records(user_id);
CREATE INDEX IF NOT EXISTS idx_att_rec_date ON attendance_records(attendance_date);
CREATE INDEX IF NOT EXISTS idx_att_rec_tenant_date ON attendance_records(tenant_id, attendance_date);

-- 4. Attendance Anomalies (Deteksi Fake GPS / Clock Tampering)
CREATE TABLE IF NOT EXISTS attendance_anomalies (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    user_name TEXT NOT NULL,
    anomaly_type TEXT NOT NULL, -- 'MOCK_LOCATION_DETECTED', 'IMPOSSIBLE_TRAVEL_SPEED', 'FACE_MISMATCH', 'OFF_HOURS_CLOCK'
    details TEXT NOT NULL,
    severity TEXT NOT NULL DEFAULT 'HIGH', -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by TEXT,
    resolved_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_att_anom_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_att_anom_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_att_anom_tenant ON attendance_anomalies(tenant_id);
CREATE INDEX IF NOT EXISTS idx_att_anom_user ON attendance_anomalies(user_id);

-- 5. GPS Location Tracks (Jejak Koordinat Lapangan Staf)
CREATE TABLE IF NOT EXISTS gps_location_tracks (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy REAL NOT NULL DEFAULT 5.0,
    is_inside_work_geofence BOOLEAN NOT NULL DEFAULT TRUE,
    recorded_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_gps_track_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_gps_track_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_gps_track_tenant ON gps_location_tracks(tenant_id);
CREATE INDEX IF NOT EXISTS idx_gps_track_user ON gps_location_tracks(user_id);
CREATE INDEX IF NOT EXISTS idx_gps_track_recorded ON gps_location_tracks(recorded_at);

-- 6. Goals KPI (Indikator Kinerja Utama Individu & Departemen)
CREATE TABLE IF NOT EXISTS goals_kpi (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    target_type TEXT NOT NULL, -- 'STAFF', 'DEPARTMENT', 'COMPANY'
    target_id TEXT NOT NULL,
    target_name TEXT NOT NULL,
    title TEXT NOT NULL,
    category TEXT NOT NULL, -- 'PRODUCTIVITY', 'QUALITY', 'SALES', 'ATTENDANCE', 'COLLABORATION'
    metric_key TEXT NOT NULL,
    target_value DOUBLE PRECISION NOT NULL,
    current_value DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    unit TEXT NOT NULL DEFAULT 'Tasks',
    weight_pct DOUBLE PRECISION NOT NULL DEFAULT 20.0,
    period TEXT NOT NULL DEFAULT 'Agustus 2026',
    status TEXT NOT NULL DEFAULT 'ON_TRACK', -- 'ON_TRACK', 'AT_RISK', 'BEHIND', 'ACHIEVED'
    start_date BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    end_date BIGINT NOT NULL DEFAULT ((EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT + 2592000000),
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_goals_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_goals_tenant ON goals_kpi(tenant_id);
CREATE INDEX IF NOT EXISTS idx_goals_target ON goals_kpi(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_goals_status ON goals_kpi(status);

-- 7. Performance Reviews (Siklus Evaluasi Kinerja Bulanan/Triwulanan)
CREATE TABLE IF NOT EXISTS performance_reviews (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    staff_id TEXT NOT NULL,
    staff_name TEXT NOT NULL,
    department_name TEXT NOT NULL,
    reviewer_id TEXT NOT NULL,
    reviewer_name TEXT NOT NULL,
    review_period TEXT NOT NULL DEFAULT 'Agustus 2026',
    cycle_type TEXT NOT NULL DEFAULT 'MONTHLY', -- 'MONTHLY', 'QUARTERLY', 'ANNUAL'
    self_rating_score DOUBLE PRECISION NOT NULL DEFAULT 4.0,
    self_strengths TEXT NOT NULL DEFAULT '',
    self_blockers TEXT NOT NULL DEFAULT '',
    self_goals_next_period TEXT NOT NULL DEFAULT '',
    manager_rating_score DOUBLE PRECISION NOT NULL DEFAULT 4.2,
    manager_feedback TEXT NOT NULL DEFAULT '',
    manager_action_plan TEXT NOT NULL DEFAULT '',
    actual_calculated_score DOUBLE PRECISION NOT NULL DEFAULT 90.0,
    status TEXT NOT NULL DEFAULT 'COMPLETED', -- 'DRAFT', 'SUBMITTED', 'MANAGER_REVIEWED', 'COMPLETED'
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_reviews_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_staff FOREIGN KEY (staff_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_reviews_tenant ON performance_reviews(tenant_id);
CREATE INDEX IF NOT EXISTS idx_reviews_staff ON performance_reviews(staff_id);
CREATE INDEX IF NOT EXISTS idx_reviews_period ON performance_reviews(review_period);

-- 8. Coaching Sessions (Sesi Bimbingan Kinerja AI & Manajer)
CREATE TABLE IF NOT EXISTS coaching_sessions (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    staff_id TEXT NOT NULL,
    staff_name TEXT NOT NULL,
    department_name TEXT NOT NULL,
    coach_type TEXT NOT NULL DEFAULT 'AI_PERFORMANCE_COACH', -- 'AI_PERFORMANCE_COACH', 'MANAGER'
    focus_area TEXT NOT NULL,
    trigger_metric TEXT NOT NULL,
    coaching_tips TEXT NOT NULL,
    action_items_json TEXT NOT NULL DEFAULT '[]',
    status TEXT NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'IN_PROGRESS', 'COMPLETED'
    scheduled_follow_up BIGINT NOT NULL DEFAULT ((EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT + 604800000),
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_coaching_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_coaching_staff FOREIGN KEY (staff_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_coaching_tenant ON coaching_sessions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_coaching_staff ON coaching_sessions(staff_id);

-- 9. Development Recommendations (Rekomendasi Pelatihan Berbasis Data)
CREATE TABLE IF NOT EXISTS development_recommendations (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    staff_id TEXT NOT NULL,
    staff_name TEXT NOT NULL,
    department_name TEXT NOT NULL,
    category TEXT NOT NULL, -- 'TRAINING', 'MENTORING', 'CERTIFICATION', 'TOOL_MASTERY'
    title TEXT NOT NULL,
    rationale TEXT NOT NULL,
    target_skill TEXT NOT NULL,
    priority TEXT NOT NULL DEFAULT 'MEDIUM', -- 'HIGH', 'MEDIUM', 'LOW'
    estimated_hours INT NOT NULL DEFAULT 12,
    provider_or_course TEXT NOT NULL DEFAULT 'Internal Masterclass & AI Sandbox',
    status TEXT NOT NULL DEFAULT 'RECOMMENDED', -- 'RECOMMENDED', 'ENROLLED', 'COMPLETED', 'DISMISSED'
    source_score_correlation TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_dev_rec_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_dev_rec_staff FOREIGN KEY (staff_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dev_rec_tenant ON development_recommendations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dev_rec_staff ON development_recommendations(staff_id);

-- 10. Performance Metrics Daily (Metrik Kinerja Harian Human & AI Agent)
CREATE TABLE IF NOT EXISTS performance_metrics_daily (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    entity_type TEXT NOT NULL, -- 'HUMAN', 'AI_AGENT'
    entity_id TEXT NOT NULL,
    entity_name TEXT NOT NULL,
    department_id TEXT NOT NULL DEFAULT 'dept-gen',
    department_name TEXT NOT NULL DEFAULT 'General',
    date TEXT NOT NULL, -- 'YYYY-MM-DD'
    tasks_total INT NOT NULL DEFAULT 0,
    tasks_on_time INT NOT NULL DEFAULT 0,
    quality_sum DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    collaboration_events INT NOT NULL DEFAULT 0,
    uptime_minutes INT NOT NULL DEFAULT 480,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_perf_metrics_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_perf_metrics_tenant ON performance_metrics_daily(tenant_id);
CREATE INDEX IF NOT EXISTS idx_perf_metrics_entity ON performance_metrics_daily(entity_id, date);

-- 11. Performance Scores (Skor Kinerja Komposit Berbobot)
CREATE TABLE IF NOT EXISTS performance_scores (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    entity_type TEXT NOT NULL, -- 'HUMAN', 'AI_AGENT'
    entity_id TEXT NOT NULL,
    entity_name TEXT NOT NULL,
    department_id TEXT NOT NULL DEFAULT 'dept-gen',
    department_name TEXT NOT NULL,
    period TEXT NOT NULL DEFAULT 'Agustus 2026',
    completion_rate DOUBLE PRECISION NOT NULL DEFAULT 90.0,
    deadline_compliance_rate DOUBLE PRECISION NOT NULL DEFAULT 92.0,
    average_quality_rating DOUBLE PRECISION NOT NULL DEFAULT 4.5,
    collaboration_factor DOUBLE PRECISION NOT NULL DEFAULT 88.0,
    attendance_uptime_score DOUBLE PRECISION NOT NULL DEFAULT 98.0,
    composite_score DOUBLE PRECISION NOT NULL DEFAULT 92.5,
    tier_grade TEXT NOT NULL DEFAULT 'Bintang 5 (Top Tier)',
    rank_in_department INT NOT NULL DEFAULT 1,
    rank_in_company INT NOT NULL DEFAULT 1,
    calculated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_perf_scores_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_perf_scores_tenant ON performance_scores(tenant_id);
CREATE INDEX IF NOT EXISTS idx_perf_scores_entity ON performance_scores(entity_id, period);

-- 12. Performance Alerts (Peringatan Dini Penurunan Kinerja)
CREATE TABLE IF NOT EXISTS performance_alerts (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    entity_name TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    department_name TEXT NOT NULL DEFAULT 'General',
    trigger_reason TEXT NOT NULL,
    score_drop_pct DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    severity TEXT NOT NULL DEFAULT 'MEDIUM', -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_perf_alerts_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_perf_alerts_tenant ON performance_alerts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_perf_alerts_entity ON performance_alerts(entity_id);

-- 13. Manager One-on-One Notes (Catatan Pribadi Supervisi Manajer)
CREATE TABLE IF NOT EXISTS manager_one_on_one_notes (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    staff_id TEXT NOT NULL,
    staff_name TEXT NOT NULL,
    manager_id TEXT NOT NULL,
    manager_name TEXT NOT NULL,
    note TEXT NOT NULL,
    action_items TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_mgr_notes_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_mgr_notes_staff FOREIGN KEY (staff_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mgr_notes_tenant ON manager_one_on_one_notes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mgr_notes_staff ON manager_one_on_one_notes(staff_id);

-- 14. Work Reports Daily (Laporan Kerja Harian Staf)
CREATE TABLE IF NOT EXISTS work_reports_daily (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    staff_id TEXT NOT NULL,
    staff_name TEXT NOT NULL,
    department_name TEXT NOT NULL,
    report_date TEXT NOT NULL, -- 'YYYY-MM-DD'
    work_summary TEXT NOT NULL,
    completed_tasks_summary TEXT NOT NULL,
    linked_task_ids_json TEXT NOT NULL DEFAULT '[]',
    blockers_and_challenges TEXT NOT NULL DEFAULT '',
    plan_for_tomorrow TEXT NOT NULL DEFAULT '',
    hours_worked DOUBLE PRECISION NOT NULL DEFAULT 8.0,
    sentiment_rating TEXT NOT NULL DEFAULT 'PRODUCTIVE',
    photo_attachment_base64 TEXT,
    submitted_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    reviewed_by_manager_id TEXT,
    manager_feedback TEXT,
    reviewed_at BIGINT,
    CONSTRAINT fk_work_rep_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_work_rep_staff FOREIGN KEY (staff_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_work_rep_tenant ON work_reports_daily(tenant_id);
CREATE INDEX IF NOT EXISTS idx_work_rep_staff ON work_reports_daily(staff_id);
CREATE INDEX IF NOT EXISTS idx_work_rep_date ON work_reports_daily(report_date);

-- 15. Executive Briefs (Ringkasan Eksekutif Harian & Mingguan AI)
CREATE TABLE IF NOT EXISTS executive_briefs (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    brief_type TEXT NOT NULL, -- 'MANAGER_DAILY', 'OWNER_WEEKLY'
    brief_date TEXT NOT NULL, -- 'YYYY-MM-DD'
    title TEXT NOT NULL,
    executive_summary TEXT NOT NULL,
    key_achievements TEXT NOT NULL,
    risk_alerts TEXT NOT NULL,
    kpi_progress_snapshot TEXT NOT NULL,
    ai_strategic_recommendations TEXT NOT NULL,
    generated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_exec_briefs_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_exec_briefs_tenant ON executive_briefs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_exec_briefs_type_date ON executive_briefs(tenant_id, brief_type, brief_date);

-- 16. Performance Risk Predictions (Prediksi Risiko & Burnout Berbasis AI)
CREATE TABLE IF NOT EXISTS performance_risk_predictions (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    entity_name TEXT NOT NULL,
    entity_type TEXT NOT NULL, -- 'HUMAN', 'AI_AGENT'
    department_name TEXT NOT NULL,
    current_score DOUBLE PRECISION NOT NULL,
    predicted_score_next_month DOUBLE PRECISION NOT NULL,
    risk_level TEXT NOT NULL, -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    risk_probability_pct DOUBLE PRECISION NOT NULL,
    confidence_pct DOUBLE PRECISION NOT NULL DEFAULT 88.5,
    top_risk_factors_json TEXT NOT NULL DEFAULT '[]',
    preventive_actions_json TEXT NOT NULL DEFAULT '[]',
    evaluated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_perf_risk_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_perf_risk_tenant ON performance_risk_predictions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_perf_risk_entity ON performance_risk_predictions(entity_id);

-- 17. Workflow Definitions (Definisi Alur Kerja DAG Multi-Step)
CREATE TABLE IF NOT EXISTS workflow_definitions (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'Operations',
    trigger_type TEXT NOT NULL DEFAULT 'MANUAL', -- 'MANUAL', 'SCHEDULED', 'EVENT_TRIGGERED', 'WEBHOOK'
    cron_expression TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_wf_def_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wf_def_tenant ON workflow_definitions(tenant_id);

-- 18. Workflow Executions (Riwayat Eksekusi Workflow DAG)
CREATE TABLE IF NOT EXISTS workflow_executions (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    workflow_def_id TEXT NOT NULL,
    trigger_source TEXT NOT NULL DEFAULT 'ChatScreen',
    input_payload TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'RUNNING', 'WAITING_APPROVAL', 'COMPLETED', 'FAILED', 'CANCELLED'
    current_step_index INT NOT NULL DEFAULT 0,
    total_steps INT NOT NULL DEFAULT 6,
    started_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    completed_at BIGINT,
    result_summary TEXT,
    CONSTRAINT fk_wf_exec_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_wf_exec_def FOREIGN KEY (workflow_def_id)
        REFERENCES workflow_definitions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wf_exec_tenant ON workflow_executions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_wf_exec_def ON workflow_executions(workflow_def_id);
CREATE INDEX IF NOT EXISTS idx_wf_exec_status ON workflow_executions(status);

-- 19. Workflow Nodes (Node Langkah dalam DAG)
CREATE TABLE IF NOT EXISTS workflow_nodes (
    id TEXT PRIMARY KEY,
    workflow_def_id TEXT NOT NULL,
    node_key TEXT NOT NULL,
    node_type TEXT NOT NULL, -- 'CLASSIFY', 'PLAN', 'TOOL_CALL', 'LLM_GENERATE', 'HUMAN_APPROVAL', 'DELIVER'
    label TEXT NOT NULL,
    tool_name TEXT,
    model_preference TEXT,
    required_role TEXT,
    retry_policy TEXT NOT NULL DEFAULT 'MAX_RETRIES_3',
    CONSTRAINT fk_wf_nodes_def FOREIGN KEY (workflow_def_id)
        REFERENCES workflow_definitions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wf_nodes_def ON workflow_nodes(workflow_def_id);

-- 20. Workflow Node Runs (Log Eksekusi Node Satuan)
CREATE TABLE IF NOT EXISTS workflow_node_runs (
    id TEXT PRIMARY KEY,
    execution_id TEXT NOT NULL,
    task_id TEXT,
    node_key TEXT NOT NULL,
    node_type TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'WAITING_APPROVAL'
    input_json TEXT NOT NULL DEFAULT '{}',
    output_json TEXT NOT NULL DEFAULT '{}',
    error_detail TEXT,
    started_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    completed_at BIGINT,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_wf_node_runs_exec FOREIGN KEY (execution_id)
        REFERENCES workflow_executions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_wf_node_runs_exec ON workflow_node_runs(execution_id);

-- 21. Agent Templates (Template Agen AI Siap Pakai)
CREATE TABLE IF NOT EXISTS agent_templates (
    id TEXT PRIMARY KEY,
    role TEXT NOT NULL,
    default_name TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL, -- 'Executive', 'Engineering', 'Marketing', 'Operations', 'Intelligence'
    icon_res TEXT NOT NULL DEFAULT 'robot',
    base_prompt TEXT NOT NULL,
    default_risk_tier TEXT NOT NULL DEFAULT 'LOW',
    allowed_mcp_tools TEXT NOT NULL DEFAULT 'web.fetch, company_brain.query',
    version TEXT NOT NULL DEFAULT 'v1.0.0',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    usage_count INT NOT NULL DEFAULT 1
);

-- 22. LLM Models (Katalog Model AI Multi-Provider)
CREATE TABLE IF NOT EXISTS llm_models (
    id TEXT PRIMARY KEY,
    provider_id TEXT NOT NULL,
    model_code TEXT NOT NULL,
    display_name TEXT NOT NULL,
    context_window INT NOT NULL DEFAULT 128000,
    cost_in_per_million DOUBLE PRECISION NOT NULL DEFAULT 0.15,
    cost_out_per_million DOUBLE PRECISION NOT NULL DEFAULT 0.60,
    supports_streaming BOOLEAN NOT NULL DEFAULT TRUE,
    supports_function_calling BOOLEAN NOT NULL DEFAULT TRUE,
    supports_vision BOOLEAN NOT NULL DEFAULT TRUE,
    supports_embedding BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_experimental BOOLEAN NOT NULL DEFAULT FALSE,
    is_routing_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 23. LLM Usage Logs (Log Penggunaan Token & Penghematan Biaya)
CREATE TABLE IF NOT EXISTS llm_usage_logs (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    task_id TEXT,
    workflow_execution_id TEXT,
    provider TEXT NOT NULL,
    model_name TEXT NOT NULL,
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    estimated_cost_usd DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    cost_without_cache_estimate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    actual_cost DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    optimization_technique TEXT NOT NULL DEFAULT 'NONE',
    cached_tokens_saved INT NOT NULL DEFAULT 0,
    timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_llm_usage_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_llm_usage_tenant ON llm_usage_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_llm_usage_time ON llm_usage_logs(timestamp);

-- 24. Tool Permissions (Hak Akses MCP Tool Per Agen)
CREATE TABLE IF NOT EXISTS tool_permissions (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    agent_id TEXT NOT NULL,
    tool_id TEXT NOT NULL,
    is_granted BOOLEAN NOT NULL DEFAULT TRUE,
    granted_by TEXT NOT NULL DEFAULT 'Admin System',
    granted_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_tool_perm_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_tool_perm_agent FOREIGN KEY (agent_id)
        REFERENCES ai_agents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tool_perm_tenant ON tool_permissions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tool_perm_agent ON tool_permissions(agent_id);

-- 25. Tool Invocations (Audit Eksekusi Tool MCP)
CREATE TABLE IF NOT EXISTS tool_invocations (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    tool_name TEXT NOT NULL,
    caller_type TEXT NOT NULL, -- 'AI_AGENT', 'WORKFLOW_ENGINE', 'USER'
    caller_id TEXT NOT NULL,
    caller_name TEXT NOT NULL,
    input_json TEXT NOT NULL,
    output_json TEXT NOT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    status TEXT NOT NULL, -- 'SUCCESS', 'FAILED', 'BLOCKED_RISK', 'KILL_SWITCHED'
    risk_level TEXT NOT NULL, -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    approval_status TEXT NOT NULL DEFAULT 'AUTO_APPROVED',
    approved_by TEXT,
    timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_tool_invoc_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tool_invoc_tenant ON tool_invocations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tool_invoc_status ON tool_invocations(status);

-- 26. Tool Health Checks (Pemeriksaan Kesehatan MCP Tool)
CREATE TABLE IF NOT EXISTS tool_health_checks (
    id TEXT PRIMARY KEY,
    tool_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'HEALTHY', -- 'HEALTHY', 'DEGRADED', 'UNHEALTHY'
    latency_ms BIGINT NOT NULL DEFAULT 150,
    last_checked_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

-- 27. File Artifacts (File Output Hasil Generate AI)
CREATE TABLE IF NOT EXISTS file_artifacts (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    task_id TEXT,
    workflow_execution_id TEXT,
    title TEXT NOT NULL,
    prompt TEXT NOT NULL,
    composed_prompt TEXT NOT NULL,
    brand_guideline_name TEXT NOT NULL DEFAULT 'Nusantara Modern',
    platform_preset TEXT NOT NULL DEFAULT 'Instagram Feed (1:1)',
    file_type TEXT NOT NULL DEFAULT 'IMAGE_PNG',
    file_url TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_file_artifacts_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_file_artifacts_tenant ON file_artifacts(tenant_id);

-- 28. Brand Guidelines (Pedoman Merek Perusahaan)
CREATE TABLE IF NOT EXISTS brand_guidelines (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    primary_color_hex TEXT NOT NULL,
    secondary_color_hex TEXT NOT NULL,
    accent_color_hex TEXT NOT NULL,
    background_color_hex TEXT NOT NULL,
    tone_voice TEXT NOT NULL,
    typography TEXT NOT NULL,
    negative_keywords TEXT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_brand_guide_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_brand_guide_tenant ON brand_guidelines(tenant_id);

-- 29. Content Calendar Items (Jadwal Publikasi Konten Omnichannel)
CREATE TABLE IF NOT EXISTS content_calendar_items (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    connection_id TEXT NOT NULL,
    platform TEXT NOT NULL, -- 'INSTAGRAM', 'TIKTOK', 'WHATSAPP', 'FACEBOOK'
    title TEXT NOT NULL,
    content_text TEXT NOT NULL,
    media_urls_json TEXT NOT NULL DEFAULT '[]',
    image_source_type TEXT NOT NULL DEFAULT 'external_url',
    image_ref TEXT,
    scheduled_at BIGINT NOT NULL,
    published_at BIGINT,
    status TEXT NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'SCHEDULED', 'PUBLISHED', 'FAILED'
    created_by_agent_id TEXT NOT NULL DEFAULT 'agent-creative-01',
    created_by_agent_name TEXT NOT NULL DEFAULT 'Alya - Creative Specialist',
    approved_by_user_id TEXT,
    approved_by_user_name TEXT,
    platform_post_id TEXT,
    error_message TEXT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_content_cal_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_content_cal_tenant ON content_calendar_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_content_cal_status ON content_calendar_items(status);
CREATE INDEX IF NOT EXISTS idx_content_cal_sched ON content_calendar_items(scheduled_at);

-- 30. World Trend Clusters (Kluster Tren Global & Industri)
CREATE TABLE IF NOT EXISTS world_trend_clusters (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    cluster_title TEXT NOT NULL,
    category TEXT NOT NULL, -- 'TECH_INNOVATION', 'MARKET_SHIFT', 'REGULATION', 'COMPETITOR_ECOSYSTEM', 'MACRO_ECONOMY'
    summary TEXT NOT NULL,
    impact_assessment TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    relevance_score DOUBLE PRECISION NOT NULL DEFAULT 85.0,
    sentiment TEXT NOT NULL DEFAULT 'POSITIVE', -- 'POSITIVE', 'NEUTRAL', 'NEGATIVE_RISK', 'OPPORTUNITY'
    article_count INT NOT NULL DEFAULT 1,
    source_domains TEXT NOT NULL DEFAULT 'Google News, TechCrunch, CNBC',
    last_updated BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_world_trends_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_world_trends_tenant ON world_trend_clusters(tenant_id);

-- 31. World News Articles (Artikel Sumber Pemantauan Global)
CREATE TABLE IF NOT EXISTS world_news_articles (
    id TEXT PRIMARY KEY,
    cluster_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    source_url TEXT NOT NULL,
    source_name TEXT NOT NULL,
    published_at BIGINT NOT NULL,
    relevance_score DOUBLE PRECISION NOT NULL DEFAULT 85.0,
    keyword_matched TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_world_news_cluster FOREIGN KEY (cluster_id)
        REFERENCES world_trend_clusters(id) ON DELETE CASCADE,
    CONSTRAINT fk_world_news_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_world_news_cluster ON world_news_articles(cluster_id);
CREATE INDEX IF NOT EXISTS idx_world_news_tenant ON world_news_articles(tenant_id);

-- 32. Competitor Snapshots (Snapshot Harga & Promo Kompetitor)
CREATE TABLE IF NOT EXISTS competitor_snapshots (
    id TEXT PRIMARY KEY,
    target_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    snapshot_date TEXT NOT NULL, -- 'YYYY-MM-DD'
    captured_data_json TEXT NOT NULL,
    change_summary TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_comp_snap_target FOREIGN KEY (target_id)
        REFERENCES competitor_targets(id) ON DELETE CASCADE,
    CONSTRAINT fk_comp_snap_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comp_snap_tenant ON competitor_snapshots(tenant_id);
CREATE INDEX IF NOT EXISTS idx_comp_snap_target ON competitor_snapshots(target_id);

-- 33. Competitor Change Events (Peristiwa Perubahan Harga/Fitur Pesaing)
CREATE TABLE IF NOT EXISTS competitor_change_events (
    id TEXT PRIMARY KEY,
    target_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    target_name TEXT NOT NULL,
    event_type TEXT NOT NULL, -- 'PRICE_DROP', 'PRICE_INCREASE', 'NEW_CAMPAIGN', 'NEW_FEATURE', 'HIRING_SURGE'
    headline TEXT NOT NULL,
    before_value TEXT,
    after_value TEXT,
    impact_level TEXT NOT NULL DEFAULT 'MEDIUM', -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    recommended_counter_strategy TEXT NOT NULL DEFAULT '',
    detected_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_comp_change_target FOREIGN KEY (target_id)
        REFERENCES competitor_targets(id) ON DELETE CASCADE,
    CONSTRAINT fk_comp_change_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comp_change_tenant ON competitor_change_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_comp_change_target ON competitor_change_events(target_id);

-- 34. Competitor Reports (Laporan Analisis Persaingan Pasar Komprehensif)
CREATE TABLE IF NOT EXISTS competitor_reports (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    report_title TEXT NOT NULL,
    period TEXT NOT NULL,
    executive_summary TEXT NOT NULL,
    market_threat_level TEXT NOT NULL DEFAULT 'MEDIUM',
    pricing_comparison_json TEXT NOT NULL DEFAULT '[]',
    strategic_counter_actions_json TEXT NOT NULL DEFAULT '[]',
    generated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_comp_rep_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comp_rep_tenant ON competitor_reports(tenant_id);

-- 35. Impersonation Sessions (Audit Sesi Dukungan Super Admin)
CREATE TABLE IF NOT EXISTS impersonation_sessions (
    id TEXT PRIMARY KEY,
    super_admin_id TEXT NOT NULL,
    super_admin_name TEXT NOT NULL,
    target_tenant_id TEXT NOT NULL,
    target_tenant_name TEXT NOT NULL,
    target_user_id TEXT NOT NULL,
    target_user_name TEXT NOT NULL,
    reason TEXT NOT NULL,
    ticket_reference TEXT NOT NULL,
    session_token TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'EXPIRED', 'TERMINATED'
    started_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    ended_at BIGINT,
    CONSTRAINT fk_impersonate_tenant FOREIGN KEY (target_tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_impersonate_tenant ON impersonation_sessions(target_tenant_id);
CREATE INDEX IF NOT EXISTS idx_impersonate_admin ON impersonation_sessions(super_admin_id);

-- 36. User Sessions (Sesi Perangkat Pengguna Aktif)
CREATE TABLE IF NOT EXISTS user_sessions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    device_name TEXT NOT NULL,
    os_name TEXT NOT NULL,
    ip_address TEXT NOT NULL,
    location_approx TEXT NOT NULL,
    is_current_session BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_user_sess_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_sess_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_sess_tenant ON user_sessions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_sess_user ON user_sessions(user_id);

-- 37. User Preferences (Preferensi Tampilan & Notifikasi Akun)
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    theme_preference TEXT NOT NULL DEFAULT 'LIGHT', -- 'DARK', 'LIGHT', 'SYSTEM'
    language_preference TEXT NOT NULL DEFAULT 'id', -- 'id', 'en'
    last_password_changed_at BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_user_pref_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_pref_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_pref_tenant ON user_preferences(tenant_id);

-- 38. Company Code History (Riwayat Validasi Kode Perusahaan Onboarding)
CREATE TABLE IF NOT EXISTS company_code_history (
    id TEXT PRIMARY KEY,
    company_code TEXT NOT NULL,
    company_name TEXT NOT NULL,
    domain TEXT NOT NULL,
    verified_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_code_hist_code ON company_code_history(company_code);

-- 39. Search History (Riwayat Pencarian Global Aplikasi)
CREATE TABLE IF NOT EXISTS search_history (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    query TEXT NOT NULL,
    selected_category TEXT,
    result_count INT NOT NULL DEFAULT 0,
    searched_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_search_hist_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_search_hist_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_search_hist_tenant ON search_history(tenant_id);
CREATE INDEX IF NOT EXISTS idx_search_hist_user ON search_history(user_id);

-- 40. Trial Subscriptions (Status Uji Coba Gratis Tenant)
CREATE TABLE IF NOT EXISTS trial_subscriptions (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    started_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    expires_at BIGINT NOT NULL DEFAULT ((EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT + 1209600000), -- 14 hari
    status TEXT NOT NULL DEFAULT 'active', -- 'active', 'expired', 'converted'
    daily_task_limit INT NOT NULL DEFAULT 5,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_trial_sub_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_trial_sub_tenant ON trial_subscriptions(tenant_id);

-- 41. Integration Sync Logs (Log Sinkronisasi Integrasi Eksternal)
CREATE TABLE IF NOT EXISTS integration_sync_logs (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    connection_id TEXT NOT NULL,
    platform TEXT NOT NULL,
    sync_type TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'SUCCESS',
    response_code INT NOT NULL DEFAULT 200,
    latency_ms BIGINT NOT NULL DEFAULT 120,
    records_synced INT NOT NULL DEFAULT 0,
    details TEXT NOT NULL DEFAULT '',
    timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_int_sync_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_int_sync_tenant ON integration_sync_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_int_sync_conn ON integration_sync_logs(connection_id);

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================
ALTER TABLE work_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE geofence_zones ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance_anomalies ENABLE ROW LEVEL SECURITY;
ALTER TABLE gps_location_tracks ENABLE ROW LEVEL SECURITY;
ALTER TABLE goals_kpi ENABLE ROW LEVEL SECURITY;
ALTER TABLE performance_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE coaching_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE development_recommendations ENABLE ROW LEVEL SECURITY;
ALTER TABLE performance_metrics_daily ENABLE ROW LEVEL SECURITY;
ALTER TABLE performance_scores ENABLE ROW LEVEL SECURITY;
ALTER TABLE performance_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE manager_one_on_one_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE work_reports_daily ENABLE ROW LEVEL SECURITY;
ALTER TABLE executive_briefs ENABLE ROW LEVEL SECURITY;
ALTER TABLE performance_risk_predictions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_definitions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_nodes ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_node_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE agent_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE llm_models ENABLE ROW LEVEL SECURITY;
ALTER TABLE llm_usage_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE tool_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE tool_invocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE tool_health_checks ENABLE ROW LEVEL SECURITY;
ALTER TABLE file_artifacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE brand_guidelines ENABLE ROW LEVEL SECURITY;
ALTER TABLE content_calendar_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE world_trend_clusters ENABLE ROW LEVEL SECURITY;
ALTER TABLE world_news_articles ENABLE ROW LEVEL SECURITY;
ALTER TABLE competitor_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE competitor_change_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE competitor_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE impersonation_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_code_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE search_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE trial_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE integration_sync_logs ENABLE ROW LEVEL SECURITY;

-- Tenant Isolation Policies
DROP POLICY IF EXISTS tenant_isolation_work_loc ON work_locations;
CREATE POLICY tenant_isolation_work_loc ON work_locations FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_geofence ON geofence_zones;
CREATE POLICY tenant_isolation_geofence ON geofence_zones FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_att_rec ON attendance_records;
CREATE POLICY tenant_isolation_att_rec ON attendance_records FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_att_anom ON attendance_anomalies;
CREATE POLICY tenant_isolation_att_anom ON attendance_anomalies FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_gps_track ON gps_location_tracks;
CREATE POLICY tenant_isolation_gps_track ON gps_location_tracks FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_goals ON goals_kpi;
CREATE POLICY tenant_isolation_goals ON goals_kpi FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_reviews ON performance_reviews;
CREATE POLICY tenant_isolation_reviews ON performance_reviews FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_coaching ON coaching_sessions;
CREATE POLICY tenant_isolation_coaching ON coaching_sessions FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_dev_rec ON development_recommendations;
CREATE POLICY tenant_isolation_dev_rec ON development_recommendations FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_perf_metrics ON performance_metrics_daily;
CREATE POLICY tenant_isolation_perf_metrics ON performance_metrics_daily FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_perf_scores ON performance_scores;
CREATE POLICY tenant_isolation_perf_scores ON performance_scores FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_perf_alerts ON performance_alerts;
CREATE POLICY tenant_isolation_perf_alerts ON performance_alerts FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_mgr_notes ON manager_one_on_one_notes;
CREATE POLICY tenant_isolation_mgr_notes ON manager_one_on_one_notes FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_work_rep ON work_reports_daily;
CREATE POLICY tenant_isolation_work_rep ON work_reports_daily FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_exec_briefs ON executive_briefs;
CREATE POLICY tenant_isolation_exec_briefs ON executive_briefs FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_perf_risk ON performance_risk_predictions;
CREATE POLICY tenant_isolation_perf_risk ON performance_risk_predictions FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_wf_def ON workflow_definitions;
CREATE POLICY tenant_isolation_wf_def ON workflow_definitions FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_wf_exec ON workflow_executions;
CREATE POLICY tenant_isolation_wf_exec ON workflow_executions FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS p_wf_nodes_access ON workflow_nodes;
CREATE POLICY p_wf_nodes_access ON workflow_nodes FOR ALL USING (
    EXISTS (SELECT 1 FROM workflow_definitions d WHERE d.id = workflow_def_id AND app_has_tenant_access(d.tenant_id))
    OR auth.role() = 'service_role'
);

DROP POLICY IF EXISTS p_wf_node_runs_access ON workflow_node_runs;
CREATE POLICY p_wf_node_runs_access ON workflow_node_runs FOR ALL USING (
    EXISTS (SELECT 1 FROM workflow_executions e WHERE e.id = execution_id AND app_has_tenant_access(e.tenant_id))
    OR auth.role() = 'service_role'
);

-- Catalog/Template policies (Public read)
DROP POLICY IF EXISTS p_agent_templates_select ON agent_templates;
CREATE POLICY p_agent_templates_select ON agent_templates FOR SELECT USING (true);
DROP POLICY IF EXISTS p_agent_templates_service ON agent_templates;
CREATE POLICY p_agent_templates_service ON agent_templates FOR ALL USING (auth.role() = 'service_role');

DROP POLICY IF EXISTS p_llm_models_select ON llm_models;
CREATE POLICY p_llm_models_select ON llm_models FOR SELECT USING (true);
DROP POLICY IF EXISTS p_llm_models_service ON llm_models;
CREATE POLICY p_llm_models_service ON llm_models FOR ALL USING (auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_llm_usage ON llm_usage_logs;
CREATE POLICY tenant_isolation_llm_usage ON llm_usage_logs FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_tool_perm ON tool_permissions;
CREATE POLICY tenant_isolation_tool_perm ON tool_permissions FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_tool_invoc ON tool_invocations;
CREATE POLICY tenant_isolation_tool_invoc ON tool_invocations FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS p_tool_health_select ON tool_health_checks;
CREATE POLICY p_tool_health_select ON tool_health_checks FOR SELECT USING (true);
DROP POLICY IF EXISTS p_tool_health_service ON tool_health_checks;
CREATE POLICY p_tool_health_service ON tool_health_checks FOR ALL USING (auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_file_artifacts ON file_artifacts;
CREATE POLICY tenant_isolation_file_artifacts ON file_artifacts FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_brand_guide ON brand_guidelines;
CREATE POLICY tenant_isolation_brand_guide ON brand_guidelines FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_content_cal ON content_calendar_items;
CREATE POLICY tenant_isolation_content_cal ON content_calendar_items FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_world_trends ON world_trend_clusters;
CREATE POLICY tenant_isolation_world_trends ON world_trend_clusters FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_world_news ON world_news_articles;
CREATE POLICY tenant_isolation_world_news ON world_news_articles FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_comp_snap ON competitor_snapshots;
CREATE POLICY tenant_isolation_comp_snap ON competitor_snapshots FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_comp_change ON competitor_change_events;
CREATE POLICY tenant_isolation_comp_change ON competitor_change_events FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_comp_rep ON competitor_reports;
CREATE POLICY tenant_isolation_comp_rep ON competitor_reports FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_impersonate ON impersonation_sessions;
CREATE POLICY tenant_isolation_impersonate ON impersonation_sessions FOR ALL USING (app_has_tenant_access(target_tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_user_sess ON user_sessions;
CREATE POLICY tenant_isolation_user_sess ON user_sessions FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_user_pref ON user_preferences;
CREATE POLICY tenant_isolation_user_pref ON user_preferences FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS p_company_code_hist_select ON company_code_history;
CREATE POLICY p_company_code_hist_select ON company_code_history FOR SELECT USING (true);
DROP POLICY IF EXISTS p_company_code_hist_service ON company_code_history;
CREATE POLICY p_company_code_hist_service ON company_code_history FOR ALL USING (auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_search_hist ON search_history;
CREATE POLICY tenant_isolation_search_hist ON search_history FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_trial_sub ON trial_subscriptions;
CREATE POLICY tenant_isolation_trial_sub ON trial_subscriptions FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

DROP POLICY IF EXISTS tenant_isolation_int_sync ON integration_sync_logs;
CREATE POLICY tenant_isolation_int_sync ON integration_sync_logs FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');
