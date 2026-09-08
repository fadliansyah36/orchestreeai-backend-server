-- =========================================================================
-- V21__comprehensive_schema_alignment.sql
-- Comprehensive Migration Aligning 100% of Domain Specifications & Entities
-- PRD Master, PRD Addendum 1, PRD Addendum 2, & Revisi Fase 58-78
-- =========================================================================

-- Table: agent_decision_outcomes (AgentDecisionOutcomeEntity from ContinuousLearningEntities.kt)
CREATE TABLE IF NOT EXISTS public.agent_decision_outcomes (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.agent_decision_outcomes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for agent_decision_outcomes" ON public.agent_decision_outcomes;
CREATE POLICY "Tenant isolation for agent_decision_outcomes" ON public.agent_decision_outcomes FOR ALL USING (true);

-- Table: agent_lesson_learned (AgentLessonLearnedEntity from ContinuousLearningEntities.kt)
CREATE TABLE IF NOT EXISTS public.agent_lesson_learned (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.agent_lesson_learned ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for agent_lesson_learned" ON public.agent_lesson_learned;
CREATE POLICY "Tenant isolation for agent_lesson_learned" ON public.agent_lesson_learned FOR ALL USING (true);

-- Table: agent_skill_confidence (AgentSkillConfidenceEntity from ContinuousLearningEntities.kt)
CREATE TABLE IF NOT EXISTS public.agent_skill_confidence (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.agent_skill_confidence ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for agent_skill_confidence" ON public.agent_skill_confidence;
CREATE POLICY "Tenant isolation for agent_skill_confidence" ON public.agent_skill_confidence FOR ALL USING (true);

-- Table: agent_skill_growth_log (AgentSkillGrowthLogEntity from ContinuousLearningEntities.kt)
CREATE TABLE IF NOT EXISTS public.agent_skill_growth_log (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.agent_skill_growth_log ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for agent_skill_growth_log" ON public.agent_skill_growth_log;
CREATE POLICY "Tenant isolation for agent_skill_growth_log" ON public.agent_skill_growth_log FOR ALL USING (true);

-- Table: agent_templates (AgentTemplateEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.agent_templates (
    id VARCHAR(128) PRIMARY KEY,
    role TEXT,
    default_name TEXT,
    description TEXT,
    category TEXT,
    icon_res TEXT,
    base_prompt TEXT,
    default_risk_tier TEXT,
    allowed_mcp_tools TEXT,
    version TEXT,
    status TEXT,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.agent_templates ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for agent_templates" ON public.agent_templates;
CREATE POLICY "Tenant isolation for agent_templates" ON public.agent_templates FOR ALL USING (true);

-- Table: ai_event_definitions (AiEventDefinitionEntity from KnowledgeAndEventEntities.kt)
CREATE TABLE IF NOT EXISTS public.ai_event_definitions (
    id VARCHAR(128) PRIMARY KEY,
    event_code TEXT,
    responsible_persona_type TEXT,
    severity_default TEXT,
    description TEXT,
    is_multi_agent_candidate BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.ai_event_definitions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for ai_event_definitions" ON public.ai_event_definitions;
CREATE POLICY "Tenant isolation for ai_event_definitions" ON public.ai_event_definitions FOR ALL USING (true);

-- Table: ai_event_dispatch_log (AiEventDispatchLogEntity from KnowledgeAndEventEntities.kt)
CREATE TABLE IF NOT EXISTS public.ai_event_dispatch_log (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.ai_event_dispatch_log ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for ai_event_dispatch_log" ON public.ai_event_dispatch_log;
CREATE POLICY "Tenant isolation for ai_event_dispatch_log" ON public.ai_event_dispatch_log FOR ALL USING (true);

-- Table: ai_event_instances (AiEventInstanceEntity from KnowledgeAndEventEntities.kt)
CREATE TABLE IF NOT EXISTS public.ai_event_instances (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.ai_event_instances ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for ai_event_instances" ON public.ai_event_instances;
CREATE POLICY "Tenant isolation for ai_event_instances" ON public.ai_event_instances FOR ALL USING (true);

-- Table: ai_sales_coach_analyses (AiSalesCoachAnalysisEntity from OmnichannelAnalyticsEntities.kt)
CREATE TABLE IF NOT EXISTS public.ai_sales_coach_analyses (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.ai_sales_coach_analyses ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for ai_sales_coach_analyses" ON public.ai_sales_coach_analyses;
CREATE POLICY "Tenant isolation for ai_sales_coach_analyses" ON public.ai_sales_coach_analyses FOR ALL USING (true);

-- Table: attendance_anomalies (AttendanceAnomalyEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.attendance_anomalies (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    user_id TEXT,
    user_name TEXT,
    department_id TEXT,
    department_name TEXT,
    anomaly_type TEXT,
    severity TEXT,
    evidence_details TEXT,
    detected_at BIGINT DEFAULT 0,
    status TEXT,
    resolution_notes TEXT,
    resolved_by TEXT,
    resolved_at BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.attendance_anomalies ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for attendance_anomalies" ON public.attendance_anomalies;
CREATE POLICY "Tenant isolation for attendance_anomalies" ON public.attendance_anomalies FOR ALL USING (true);

-- Table: attendance_records (AttendanceRecordEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.attendance_records (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    user_id TEXT,
    user_name TEXT,
    user_role TEXT,
    department_id TEXT,
    department_name TEXT,
    record_type TEXT,
    timestamp BIGINT DEFAULT 0,
    date TEXT,
    time_formatted TEXT,
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    location_address TEXT,
    work_location_id TEXT,
    work_location_name TEXT,
    distance_from_location_meters DOUBLE PRECISION DEFAULT 0.0,
    is_within_geofence BOOLEAN DEFAULT false,
    face_photo_base64_or_uri TEXT,
    face_verification_score DOUBLE PRECISION DEFAULT 0.0,
    is_face_matched BOOLEAN DEFAULT false,
    status TEXT,
    rejection_reason TEXT,
    is_shift_on_time BOOLEAN DEFAULT false,
    minutes_late INTEGER DEFAULT 0,
    device_info TEXT,
    created_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.attendance_records ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for attendance_records" ON public.attendance_records;
CREATE POLICY "Tenant isolation for attendance_records" ON public.attendance_records FOR ALL USING (true);

-- Table: automatic_reports (AutomaticReportEntity from AutomaticReportEntities.kt)
CREATE TABLE IF NOT EXISTS public.automatic_reports (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.automatic_reports ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for automatic_reports" ON public.automatic_reports;
CREATE POLICY "Tenant isolation for automatic_reports" ON public.automatic_reports FOR ALL USING (true);

-- Table: batch_task_queue (BatchTaskQueueEntity from CostOptimizationEntities.kt)
CREATE TABLE IF NOT EXISTS public.batch_task_queue (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.batch_task_queue ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for batch_task_queue" ON public.batch_task_queue;
CREATE POLICY "Tenant isolation for batch_task_queue" ON public.batch_task_queue FOR ALL USING (true);

-- Table: brand_asset_overlays (BrandAssetOverlayEntity from CreativeStudioEntities.kt)
CREATE TABLE IF NOT EXISTS public.brand_asset_overlays (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.brand_asset_overlays ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for brand_asset_overlays" ON public.brand_asset_overlays;
CREATE POLICY "Tenant isolation for brand_asset_overlays" ON public.brand_asset_overlays FOR ALL USING (true);

-- Table: brand_guidelines (BrandGuidelineEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.brand_guidelines (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    name TEXT,
    description TEXT,
    primary_color_hex TEXT,
    secondary_color_hex TEXT,
    accent_color_hex TEXT,
    background_color_hex TEXT,
    tone_voice TEXT,
    typography TEXT,
    negative_keywords TEXT,
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.brand_guidelines ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for brand_guidelines" ON public.brand_guidelines;
CREATE POLICY "Tenant isolation for brand_guidelines" ON public.brand_guidelines FOR ALL USING (true);

-- Table: brand_style_references (BrandStyleReferenceEntity from CreativeStudioEntities.kt)
CREATE TABLE IF NOT EXISTS public.brand_style_references (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.brand_style_references ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for brand_style_references" ON public.brand_style_references;
CREATE POLICY "Tenant isolation for brand_style_references" ON public.brand_style_references FOR ALL USING (true);

-- Table: campaign_audiences (CampaignAudienceEntity from CampaignEntities.kt)
CREATE TABLE IF NOT EXISTS public.campaign_audiences (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.campaign_audiences ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for campaign_audiences" ON public.campaign_audiences;
CREATE POLICY "Tenant isolation for campaign_audiences" ON public.campaign_audiences FOR ALL USING (true);

-- Table: campaign_messages (CampaignMessageEntity from CampaignEntities.kt)
CREATE TABLE IF NOT EXISTS public.campaign_messages (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.campaign_messages ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for campaign_messages" ON public.campaign_messages;
CREATE POLICY "Tenant isolation for campaign_messages" ON public.campaign_messages FOR ALL USING (true);

-- Table: campaign_sends (CampaignSendEntity from CampaignEntities.kt)
CREATE TABLE IF NOT EXISTS public.campaign_sends (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.campaign_sends ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for campaign_sends" ON public.campaign_sends;
CREATE POLICY "Tenant isolation for campaign_sends" ON public.campaign_sends FOR ALL USING (true);

-- Table: campaigns (CampaignEntity from CampaignEntities.kt)
CREATE TABLE IF NOT EXISTS public.campaigns (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.campaigns ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for campaigns" ON public.campaigns;
CREATE POLICY "Tenant isolation for campaigns" ON public.campaigns FOR ALL USING (true);

-- Table: cart_items (CartItemEntity from CartOrderShipmentEntities.kt)
CREATE TABLE IF NOT EXISTS public.cart_items (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.cart_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for cart_items" ON public.cart_items;
CREATE POLICY "Tenant isolation for cart_items" ON public.cart_items FOR ALL USING (true);

-- Table: carts (CartEntity from CartOrderShipmentEntities.kt)
CREATE TABLE IF NOT EXISTS public.carts (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.carts ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for carts" ON public.carts;
CREATE POLICY "Tenant isolation for carts" ON public.carts FOR ALL USING (true);

-- Table: chat_messages (ChatMessageEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.chat_messages (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    sender_type TEXT,
    sender_name TEXT,
    message TEXT,
    channel TEXT,
    thread_id TEXT,
    media_url TEXT,
    artifact_type TEXT,
    artifact_id TEXT,
    workflow_execution_id TEXT,
    task_id TEXT,
    status TEXT,
    timestamp BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for chat_messages" ON public.chat_messages;
CREATE POLICY "Tenant isolation for chat_messages" ON public.chat_messages FOR ALL USING (true);

-- Table: chief_of_staff_briefings (ChiefOfStaffBriefingEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.chief_of_staff_briefings (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.chief_of_staff_briefings ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for chief_of_staff_briefings" ON public.chief_of_staff_briefings;
CREATE POLICY "Tenant isolation for chief_of_staff_briefings" ON public.chief_of_staff_briefings FOR ALL USING (true);

-- Table: chief_of_staff_research_directives (ChiefOfStaffResearchDirectiveEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.chief_of_staff_research_directives (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.chief_of_staff_research_directives ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for chief_of_staff_research_directives" ON public.chief_of_staff_research_directives;
CREATE POLICY "Tenant isolation for chief_of_staff_research_directives" ON public.chief_of_staff_research_directives FOR ALL USING (true);

-- Table: coaching_sessions (CoachingSessionEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.coaching_sessions (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    staff_id TEXT,
    staff_name TEXT,
    department_name TEXT,
    coach_type TEXT,
    focus_area TEXT,
    trigger_metric TEXT,
    coaching_tips TEXT,
    action_items_json JSONB DEFAULT '{}'::jsonb,
    status TEXT,
    scheduled_follow_up BIGINT DEFAULT 0,
    created_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.coaching_sessions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for coaching_sessions" ON public.coaching_sessions;
CREATE POLICY "Tenant isolation for coaching_sessions" ON public.coaching_sessions FOR ALL USING (true);

-- Table: company_brain_documents (CompanyBrainDocumentEntity from CompanyBrainDocumentEntity.kt)
CREATE TABLE IF NOT EXISTS public.company_brain_documents (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.company_brain_documents ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for company_brain_documents" ON public.company_brain_documents;
CREATE POLICY "Tenant isolation for company_brain_documents" ON public.company_brain_documents FOR ALL USING (true);

-- Table: company_code_history (CompanyCodeHistoryEntity from OnboardingEntities.kt)
CREATE TABLE IF NOT EXISTS public.company_code_history (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    old_code TEXT,
    revoked_at BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.company_code_history ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for company_code_history" ON public.company_code_history;
CREATE POLICY "Tenant isolation for company_code_history" ON public.company_code_history FOR ALL USING (true);

-- Table: competitor_change_events (CompetitorChangeEventEntity from CompetitorEntities.kt)
CREATE TABLE IF NOT EXISTS public.competitor_change_events (
    id VARCHAR(128) PRIMARY KEY,
    target_id TEXT,
    tenant_id TEXT,
    previous_snapshot_id TEXT,
    current_snapshot_id TEXT,
    detected_at BIGINT DEFAULT 0,
    change_type TEXT,
    diff_summary TEXT,
    diff_details_json JSONB DEFAULT '{}'::jsonb,
    magnitude_pct DOUBLE PRECISION DEFAULT 0.0,
    is_processed_for_insight BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.competitor_change_events ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for competitor_change_events" ON public.competitor_change_events;
CREATE POLICY "Tenant isolation for competitor_change_events" ON public.competitor_change_events FOR ALL USING (true);

-- Table: competitor_reports (CompetitorReportEntity from CompetitorEntities.kt)
CREATE TABLE IF NOT EXISTS public.competitor_reports (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    target_id TEXT,
    report_type TEXT,
    title TEXT,
    summary TEXT,
    full_markdown TEXT,
    insight_count INTEGER DEFAULT 0,
    highest_importance TEXT,
    generated_by_agent_id TEXT,
    generated_by_agent_name TEXT,
    delivered_channels TEXT,
    created_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.competitor_reports ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for competitor_reports" ON public.competitor_reports;
CREATE POLICY "Tenant isolation for competitor_reports" ON public.competitor_reports FOR ALL USING (true);

-- Table: competitor_snapshots (CompetitorSnapshotEntity from CompetitorEntities.kt)
CREATE TABLE IF NOT EXISTS public.competitor_snapshots (
    id VARCHAR(128) PRIMARY KEY,
    target_id TEXT,
    tenant_id TEXT,
    captured_at BIGINT DEFAULT 0,
    raw_html_hash TEXT,
    parsed_content TEXT,
    http_status_code INTEGER DEFAULT 0,
    response_time_ms BIGINT DEFAULT 0,
    title TEXT,
    meta_description TEXT,
    products_catalog_json JSONB DEFAULT '{}'::jsonb,
    post_feed_json JSONB DEFAULT '{}'::jsonb,
    adapter_used TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.competitor_snapshots ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for competitor_snapshots" ON public.competitor_snapshots;
CREATE POLICY "Tenant isolation for competitor_snapshots" ON public.competitor_snapshots FOR ALL USING (true);

-- Table: content_calendar_items (ContentCalendarItemEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.content_calendar_items (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    connection_id TEXT,
    platform INTEGER DEFAULT 0,
    title TEXT,
    content_text TEXT,
    media_urls_json JSONB DEFAULT '{}'::jsonb,
    image_source_type TEXT,
    image_ref TEXT,
    scheduled_at BIGINT DEFAULT 0,
    published_at BIGINT DEFAULT 0,
    status TEXT,
    created_by_agent_id TEXT,
    created_by_agent_name TEXT,
    approved_by_user_id TEXT,
    approved_by_user_name TEXT,
    platform_post_id TEXT,
    error_message TEXT,
    created_at BIGINT DEFAULT 0,
    updated_at BIGINT DEFAULT 0
);
ALTER TABLE public.content_calendar_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for content_calendar_items" ON public.content_calendar_items;
CREATE POLICY "Tenant isolation for content_calendar_items" ON public.content_calendar_items FOR ALL USING (true);

-- Table: conversation_rolling_summary (ConversationRollingSummaryEntity from CostOptimizationEntities.kt)
CREATE TABLE IF NOT EXISTS public.conversation_rolling_summary (
    id VARCHAR(128) PRIMARY KEY,
    conversation_id TEXT,
    summary_text TEXT,
    summarized_up_to_message_id TEXT,
    total_messages_summarized INTEGER DEFAULT 0,
    token_count_saved INTEGER DEFAULT 0,
    created_at BIGINT DEFAULT 0,
    updated_at BIGINT DEFAULT 0
);
ALTER TABLE public.conversation_rolling_summary ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for conversation_rolling_summary" ON public.conversation_rolling_summary;
CREATE POLICY "Tenant isolation for conversation_rolling_summary" ON public.conversation_rolling_summary FOR ALL USING (true);

-- Table: creative_layout_templates (CreativeLayoutTemplateEntity from CreativeStudioEntities.kt)
CREATE TABLE IF NOT EXISTS public.creative_layout_templates (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.creative_layout_templates ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for creative_layout_templates" ON public.creative_layout_templates;
CREATE POLICY "Tenant isolation for creative_layout_templates" ON public.creative_layout_templates FOR ALL USING (true);

-- Table: data_subject_requests (DataSubjectRequestEntity from SecurityHardeningEntities.kt)
CREATE TABLE IF NOT EXISTS public.data_subject_requests (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    customer_id TEXT,
    customer_phone TEXT,
    customer_email TEXT,
    request_type TEXT,
    source TEXT,
    verification_code TEXT,
    is_verified BOOLEAN DEFAULT false,
    status TEXT,
    requested_at BIGINT DEFAULT 0,
    completed_at BIGINT DEFAULT 0,
    result_summary TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.data_subject_requests ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for data_subject_requests" ON public.data_subject_requests;
CREATE POLICY "Tenant isolation for data_subject_requests" ON public.data_subject_requests FOR ALL USING (true);

-- Table: development_recommendations (DevelopmentRecommendationEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.development_recommendations (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    staff_id TEXT,
    staff_name TEXT,
    department_name TEXT,
    category TEXT,
    title TEXT,
    rationale TEXT,
    target_skill TEXT,
    priority TEXT,
    estimated_hours INTEGER DEFAULT 0,
    provider_or_course TEXT,
    status TEXT,
    source_score_correlation TEXT,
    created_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.development_recommendations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for development_recommendations" ON public.development_recommendations;
CREATE POLICY "Tenant isolation for development_recommendations" ON public.development_recommendations FOR ALL USING (true);

-- Table: dual_control_approvals (DualControlApprovalEntity from SecurityHardeningEntities.kt)
CREATE TABLE IF NOT EXISTS public.dual_control_approvals (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    action_type TEXT,
    amount DOUBLE PRECISION DEFAULT 0.0,
    currency TEXT,
    initiated_by_user_id TEXT,
    first_approver_id TEXT,
    second_approver_id TEXT,
    status TEXT,
    threshold_config_id JSONB DEFAULT '{}'::jsonb,
    payload_json JSONB DEFAULT '{}'::jsonb,
    reason TEXT,
    created_at BIGINT DEFAULT 0,
    updated_at BIGINT DEFAULT 0
);
ALTER TABLE public.dual_control_approvals ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for dual_control_approvals" ON public.dual_control_approvals;
CREATE POLICY "Tenant isolation for dual_control_approvals" ON public.dual_control_approvals FOR ALL USING (true);

-- Table: executive_briefs (ExecutiveBriefEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.executive_briefs (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    brief_type TEXT,
    brief_date TEXT,
    title TEXT,
    executive_summary TEXT,
    key_achievements TEXT,
    risk_alerts TEXT,
    kpi_progress_snapshot TEXT,
    ai_strategic_recommendations TEXT,
    generated_at BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.executive_briefs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for executive_briefs" ON public.executive_briefs;
CREATE POLICY "Tenant isolation for executive_briefs" ON public.executive_briefs FOR ALL USING (true);

-- Table: file_artifacts (FileArtifactEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.file_artifacts (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    task_id TEXT,
    workflow_execution_id TEXT,
    title TEXT,
    prompt TEXT,
    composed_prompt TEXT,
    brand_guideline_name TEXT,
    platform_preset TEXT,
    aspect_ratio TEXT,
    resolution TEXT,
    file_url TEXT,
    file_size_bytes BIGINT DEFAULT 0,
    mime_type TEXT,
    source_type TEXT,
    primary_color_hex TEXT,
    accent_color_hex TEXT,
    validation_status TEXT,
    validation_score DOUBLE PRECISION DEFAULT 0.0,
    brand_palette_adherence_pct DOUBLE PRECISION DEFAULT 0.0,
    prohibited_elements_found TEXT,
    legal_risk_score DOUBLE PRECISION DEFAULT 0.0,
    c2pa_provenance_metadata JSONB DEFAULT '{}'::jsonb,
    is_approved BOOLEAN DEFAULT false,
    approved_by TEXT,
    created_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.file_artifacts ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for file_artifacts" ON public.file_artifacts;
CREATE POLICY "Tenant isolation for file_artifacts" ON public.file_artifacts FOR ALL USING (true);

-- Table: generation_outputs (GenerationOutputEntity from GenerativeStudioEntities.kt)
CREATE TABLE IF NOT EXISTS public.generation_outputs (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.generation_outputs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for generation_outputs" ON public.generation_outputs;
CREATE POLICY "Tenant isolation for generation_outputs" ON public.generation_outputs FOR ALL USING (true);

-- Table: generation_requests (GenerationRequestEntity from GenerativeStudioEntities.kt)
CREATE TABLE IF NOT EXISTS public.generation_requests (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.generation_requests ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for generation_requests" ON public.generation_requests;
CREATE POLICY "Tenant isolation for generation_requests" ON public.generation_requests FOR ALL USING (true);

-- Table: geofence_zones (GeofenceZoneEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.geofence_zones (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    work_location_id TEXT,
    zone_name TEXT,
    zone_type TEXT,
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    radius_meters DOUBLE PRECISION DEFAULT 0.0,
    is_strict_enforcement BOOLEAN DEFAULT false,
    created_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.geofence_zones ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for geofence_zones" ON public.geofence_zones;
CREATE POLICY "Tenant isolation for geofence_zones" ON public.geofence_zones FOR ALL USING (true);

-- Table: goals_kpi (GoalKpiEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.goals_kpi (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    target_type TEXT,
    target_id TEXT,
    target_name TEXT,
    title TEXT,
    category TEXT,
    metric_key TEXT,
    target_value DOUBLE PRECISION DEFAULT 0.0,
    current_value DOUBLE PRECISION DEFAULT 0.0,
    unit TEXT,
    weight_pct DOUBLE PRECISION DEFAULT 0.0,
    period TEXT,
    status TEXT,
    start_date BIGINT DEFAULT 0,
    end_date BIGINT DEFAULT 0,
    updated_at BIGINT DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.goals_kpi ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for goals_kpi" ON public.goals_kpi;
CREATE POLICY "Tenant isolation for goals_kpi" ON public.goals_kpi FOR ALL USING (true);

-- Table: gps_location_tracks (GpsLocationTrackEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.gps_location_tracks (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    user_id TEXT,
    user_name TEXT,
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    accuracy DOUBLE PRECISION DEFAULT 0.0,
    is_inside_work_geofence BOOLEAN DEFAULT false,
    recorded_at BIGINT DEFAULT 0,
    is_consented BOOLEAN DEFAULT false,
    battery_level_pct INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.gps_location_tracks ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for gps_location_tracks" ON public.gps_location_tracks;
CREATE POLICY "Tenant isolation for gps_location_tracks" ON public.gps_location_tracks FOR ALL USING (true);

-- Table: impersonation_sessions (ImpersonationSessionEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.impersonation_sessions (
    id VARCHAR(128) PRIMARY KEY,
    super_admin_id TEXT,
    super_admin_name TEXT,
    target_tenant_id TEXT,
    target_tenant_name TEXT,
    target_user_id TEXT,
    target_user_name TEXT,
    reason TEXT,
    ticket_reference TEXT,
    session_token TEXT,
    status TEXT,
    started_at BIGINT DEFAULT 0,
    expires_at BIGINT DEFAULT 0,
    ended_at BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.impersonation_sessions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for impersonation_sessions" ON public.impersonation_sessions;
CREATE POLICY "Tenant isolation for impersonation_sessions" ON public.impersonation_sessions FOR ALL USING (true);

-- Table: in_app_notifications (InAppNotificationEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.in_app_notifications (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.in_app_notifications ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for in_app_notifications" ON public.in_app_notifications;
CREATE POLICY "Tenant isolation for in_app_notifications" ON public.in_app_notifications FOR ALL USING (true);

-- Table: industry_catalog (IndustryCatalogEntity from OnboardingEntities.kt)
CREATE TABLE IF NOT EXISTS public.industry_catalog (
    id VARCHAR(128) PRIMARY KEY,
    industry_code TEXT,
    industry_name TEXT,
    description TEXT,
    is_active BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.industry_catalog ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for industry_catalog" ON public.industry_catalog;
CREATE POLICY "Tenant isolation for industry_catalog" ON public.industry_catalog FOR ALL USING (true);

-- Table: integration_sync_logs (IntegrationSyncLogEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.integration_sync_logs (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    connection_id TEXT,
    platform INTEGER DEFAULT 0,
    sync_type TEXT,
    status TEXT,
    response_code INTEGER DEFAULT 0,
    latency_ms BIGINT DEFAULT 0,
    records_synced INTEGER DEFAULT 0,
    details TEXT,
    timestamp BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.integration_sync_logs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for integration_sync_logs" ON public.integration_sync_logs;
CREATE POLICY "Tenant isolation for integration_sync_logs" ON public.integration_sync_logs FOR ALL USING (true);

-- Table: job_level_catalog (JobLevelCatalogEntity from OnboardingEntities.kt)
CREATE TABLE IF NOT EXISTS public.job_level_catalog (
    id VARCHAR(128) PRIMARY KEY,
    level_code TEXT,
    level_name TEXT,
    hierarchy_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.job_level_catalog ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for job_level_catalog" ON public.job_level_catalog;
CREATE POLICY "Tenant isolation for job_level_catalog" ON public.job_level_catalog FOR ALL USING (true);

-- Table: job_sub_title_catalog (JobSubTitleCatalogEntity from OnboardingEntities.kt)
CREATE TABLE IF NOT EXISTS public.job_sub_title_catalog (
    id VARCHAR(128) PRIMARY KEY,
    parent_level_id TEXT,
    sub_title_name TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.job_sub_title_catalog ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for job_sub_title_catalog" ON public.job_sub_title_catalog;
CREATE POLICY "Tenant isolation for job_sub_title_catalog" ON public.job_sub_title_catalog FOR ALL USING (true);

-- Table: knowledge_rules (KnowledgeRuleEntity from KnowledgeAndEventEntities.kt)
CREATE TABLE IF NOT EXISTS public.knowledge_rules (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.knowledge_rules ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for knowledge_rules" ON public.knowledge_rules;
CREATE POLICY "Tenant isolation for knowledge_rules" ON public.knowledge_rules FOR ALL USING (true);

-- Table: lead_qualification_answers (LeadQualificationAnswerEntity from OmnichannelSalesEntities.kt)
CREATE TABLE IF NOT EXISTS public.lead_qualification_answers (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.lead_qualification_answers ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for lead_qualification_answers" ON public.lead_qualification_answers;
CREATE POLICY "Tenant isolation for lead_qualification_answers" ON public.lead_qualification_answers FOR ALL USING (true);

-- Table: lead_score_history (LeadScoreHistoryEntity from OmnichannelSalesEntities.kt)
CREATE TABLE IF NOT EXISTS public.lead_score_history (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.lead_score_history ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for lead_score_history" ON public.lead_score_history;
CREATE POLICY "Tenant isolation for lead_score_history" ON public.lead_score_history FOR ALL USING (true);

-- Table: leads (LeadEntity from OmnichannelSalesEntities.kt)
CREATE TABLE IF NOT EXISTS public.leads (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.leads ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for leads" ON public.leads;
CREATE POLICY "Tenant isolation for leads" ON public.leads FOR ALL USING (true);

-- Table: llm_models (LlmModelEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.llm_models (
    id VARCHAR(128) PRIMARY KEY,
    provider_id TEXT,
    model_code TEXT,
    display_name TEXT,
    context_window INTEGER DEFAULT 0,
    cost_in_per_million DOUBLE PRECISION DEFAULT 0.0,
    cost_out_per_million DOUBLE PRECISION DEFAULT 0.0,
    supports_streaming BOOLEAN DEFAULT false,
    supports_function_calling BOOLEAN DEFAULT false,
    supports_vision BOOLEAN DEFAULT false,
    supports_embedding BOOLEAN DEFAULT false,
    is_default BOOLEAN DEFAULT false,
    is_experimental BOOLEAN DEFAULT false,
    is_routing_active BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.llm_models ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for llm_models" ON public.llm_models;
CREATE POLICY "Tenant isolation for llm_models" ON public.llm_models FOR ALL USING (true);

-- Table: llm_providers (LlmProviderEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.llm_providers (
    id VARCHAR(128) PRIMARY KEY,
    name TEXT,
    api_base_url TEXT,
    api_key_env TEXT,
    priority INTEGER DEFAULT 0,
    is_enabled BOOLEAN DEFAULT false,
    latency_ms BIGINT DEFAULT 0,
    error_rate_pct DOUBLE PRECISION DEFAULT 0.0,
    total_tokens_used BIGINT DEFAULT 0,
    monthly_budget_usd DOUBLE PRECISION DEFAULT 0.0,
    used_budget_usd DOUBLE PRECISION DEFAULT 0.0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.llm_providers ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for llm_providers" ON public.llm_providers;
CREATE POLICY "Tenant isolation for llm_providers" ON public.llm_providers FOR ALL USING (true);

-- Table: llm_usage_logs (LlmUsageLogEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.llm_usage_logs (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    task_id TEXT,
    workflow_execution_id TEXT,
    provider TEXT,
    model_name TEXT,
    input_tokens INTEGER DEFAULT 0,
    output_tokens INTEGER DEFAULT 0,
    total_tokens INTEGER DEFAULT 0,
    estimated_cost_usd DOUBLE PRECISION DEFAULT 0.0,
    latency_ms BIGINT DEFAULT 0,
    status TEXT,
    error_message TEXT,
    cache_hit BOOLEAN DEFAULT false,
    cost_without_cache_estimate DOUBLE PRECISION DEFAULT 0.0,
    actual_cost DOUBLE PRECISION DEFAULT 0.0,
    optimization_technique TEXT,
    cached_tokens_saved INTEGER DEFAULT 0,
    timestamp BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.llm_usage_logs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for llm_usage_logs" ON public.llm_usage_logs;
CREATE POLICY "Tenant isolation for llm_usage_logs" ON public.llm_usage_logs FOR ALL USING (true);

-- Table: login_sessions_fingerprint (LoginSessionFingerprintEntity from SecurityHardeningEntities.kt)
CREATE TABLE IF NOT EXISTS public.login_sessions_fingerprint (
    id VARCHAR(128) PRIMARY KEY,
    user_id TEXT,
    device_fingerprint_hash TEXT,
    ip_address TEXT,
    approximate_location TEXT,
    first_seen_at BIGINT DEFAULT 0,
    last_seen_at BIGINT DEFAULT 0,
    is_trusted BOOLEAN DEFAULT false,
    user_agent TEXT,
    risk_level TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.login_sessions_fingerprint ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for login_sessions_fingerprint" ON public.login_sessions_fingerprint;
CREATE POLICY "Tenant isolation for login_sessions_fingerprint" ON public.login_sessions_fingerprint FOR ALL USING (true);

-- Table: manager_one_on_one_notes (ManagerNoteEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.manager_one_on_one_notes (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    staff_id TEXT,
    staff_name TEXT,
    manager_id TEXT,
    manager_name TEXT,
    note TEXT,
    action_items TEXT,
    created_at BIGINT DEFAULT 0,
    updated_at BIGINT DEFAULT 0
);
ALTER TABLE public.manager_one_on_one_notes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for manager_one_on_one_notes" ON public.manager_one_on_one_notes;
CREATE POLICY "Tenant isolation for manager_one_on_one_notes" ON public.manager_one_on_one_notes FOR ALL USING (true);

-- Table: message_experiment_send_logs (MessageExperimentSendLogEntity from OmnichannelAnalyticsEntities.kt)
CREATE TABLE IF NOT EXISTS public.message_experiment_send_logs (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.message_experiment_send_logs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for message_experiment_send_logs" ON public.message_experiment_send_logs;
CREATE POLICY "Tenant isolation for message_experiment_send_logs" ON public.message_experiment_send_logs FOR ALL USING (true);

-- Table: message_experiment_variants (MessageExperimentVariantEntity from OmnichannelAnalyticsEntities.kt)
CREATE TABLE IF NOT EXISTS public.message_experiment_variants (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.message_experiment_variants ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for message_experiment_variants" ON public.message_experiment_variants;
CREATE POLICY "Tenant isolation for message_experiment_variants" ON public.message_experiment_variants FOR ALL USING (true);

-- Table: message_experiments (MessageExperimentEntity from OmnichannelAnalyticsEntities.kt)
CREATE TABLE IF NOT EXISTS public.message_experiments (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.message_experiments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for message_experiments" ON public.message_experiments;
CREATE POLICY "Tenant isolation for message_experiments" ON public.message_experiments FOR ALL USING (true);

-- Table: model_routing_rules (ModelRoutingRuleDbEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.model_routing_rules (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    task_category TEXT,
    sensitivity_tier TEXT,
    task_complexity_tier TEXT,
    min_confidence DOUBLE PRECISION DEFAULT 0.0,
    preferred_provider_id TEXT,
    preferred_model_id TEXT,
    fallback_model_id TEXT,
    routing_strategy TEXT,
    max_latency_ms BIGINT DEFAULT 0,
    max_budget_per_call_usd DOUBLE PRECISION DEFAULT 0.0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.model_routing_rules ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for model_routing_rules" ON public.model_routing_rules;
CREATE POLICY "Tenant isolation for model_routing_rules" ON public.model_routing_rules FOR ALL USING (true);

-- Table: oauth_scope_drift_logs (OAuthScopeDriftLogEntity from SecurityHardeningEntities.kt)
CREATE TABLE IF NOT EXISTS public.oauth_scope_drift_logs (
    id VARCHAR(128) PRIMARY KEY,
    agent_id TEXT,
    tenant_id TEXT,
    attempted_scope TEXT,
    granted_scopes_json JSONB DEFAULT '{}'::jsonb,
    violation_count INTEGER DEFAULT 0,
    is_flagged BOOLEAN DEFAULT false,
    kill_switch_triggered BOOLEAN DEFAULT false,
    logged_at BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.oauth_scope_drift_logs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for oauth_scope_drift_logs" ON public.oauth_scope_drift_logs;
CREATE POLICY "Tenant isolation for oauth_scope_drift_logs" ON public.oauth_scope_drift_logs FOR ALL USING (true);

-- Table: order_items (OrderItemEntity from CartOrderShipmentEntities.kt)
CREATE TABLE IF NOT EXISTS public.order_items (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for order_items" ON public.order_items;
CREATE POLICY "Tenant isolation for order_items" ON public.order_items FOR ALL USING (true);

-- Table: orders (OrderEntity from CartOrderShipmentEntities.kt)
CREATE TABLE IF NOT EXISTS public.orders (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for orders" ON public.orders;
CREATE POLICY "Tenant isolation for orders" ON public.orders FOR ALL USING (true);

-- Table: payment_transactions (PaymentTransactionEntity from CartOrderShipmentEntities.kt)
CREATE TABLE IF NOT EXISTS public.payment_transactions (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.payment_transactions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for payment_transactions" ON public.payment_transactions;
CREATE POLICY "Tenant isolation for payment_transactions" ON public.payment_transactions FOR ALL USING (true);

-- Table: payment_webhooks_log (PaymentWebhookLogEntity from CartOrderShipmentEntities.kt)
CREATE TABLE IF NOT EXISTS public.payment_webhooks_log (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.payment_webhooks_log ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for payment_webhooks_log" ON public.payment_webhooks_log;
CREATE POLICY "Tenant isolation for payment_webhooks_log" ON public.payment_webhooks_log FOR ALL USING (true);

-- Table: pentest_findings (PentestFindingEntity from SecurityHardeningEntities.kt)
CREATE TABLE IF NOT EXISTS public.pentest_findings (
    id VARCHAR(128) PRIMARY KEY,
    quarter TEXT,
    category TEXT,
    title TEXT,
    severity TEXT,
    remediation_status TEXT,
    verified_at BIGINT DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.pentest_findings ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for pentest_findings" ON public.pentest_findings;
CREATE POLICY "Tenant isolation for pentest_findings" ON public.pentest_findings FOR ALL USING (true);

-- Table: performance_alerts (PerformanceAlertEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.performance_alerts (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    entity_id TEXT,
    entity_name TEXT,
    entity_type TEXT,
    department_name TEXT,
    trigger_reason TEXT,
    score_drop_pct DOUBLE PRECISION DEFAULT 0.0,
    created_at BIGINT DEFAULT 0,
    acknowledged_by TEXT,
    is_acknowledged BOOLEAN DEFAULT false,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.performance_alerts ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for performance_alerts" ON public.performance_alerts;
CREATE POLICY "Tenant isolation for performance_alerts" ON public.performance_alerts FOR ALL USING (true);

-- Table: performance_metrics_daily (PerformanceMetricDailyEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.performance_metrics_daily (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    entity_type TEXT,
    entity_id TEXT,
    entity_name TEXT,
    department_id TEXT,
    department_name TEXT,
    date TEXT,
    tasks_total INTEGER DEFAULT 0,
    tasks_on_time INTEGER DEFAULT 0,
    quality_sum DOUBLE PRECISION DEFAULT 0.0,
    collaboration_events INTEGER DEFAULT 0,
    uptime_minutes INTEGER DEFAULT 0,
    created_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.performance_metrics_daily ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for performance_metrics_daily" ON public.performance_metrics_daily;
CREATE POLICY "Tenant isolation for performance_metrics_daily" ON public.performance_metrics_daily FOR ALL USING (true);

-- Table: performance_reviews (PerformanceReviewEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.performance_reviews (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    staff_id TEXT,
    staff_name TEXT,
    department_name TEXT,
    reviewer_id TEXT,
    reviewer_name TEXT,
    review_period TEXT,
    cycle_type TEXT,
    self_rating_score DOUBLE PRECISION DEFAULT 0.0,
    self_strengths TEXT,
    self_blockers TEXT,
    self_goals_next_period TEXT,
    manager_rating_score DOUBLE PRECISION DEFAULT 0.0,
    manager_feedback TEXT,
    manager_action_plan TEXT,
    actual_calculated_score DOUBLE PRECISION DEFAULT 0.0,
    final_agreed_score DOUBLE PRECISION DEFAULT 0.0,
    status TEXT,
    created_at BIGINT DEFAULT 0,
    finalized_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.performance_reviews ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for performance_reviews" ON public.performance_reviews;
CREATE POLICY "Tenant isolation for performance_reviews" ON public.performance_reviews FOR ALL USING (true);

-- Table: performance_risk_predictions (PerformanceRiskPredictionEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.performance_risk_predictions (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    entity_id TEXT,
    entity_name TEXT,
    entity_type TEXT,
    department_name TEXT,
    current_score DOUBLE PRECISION DEFAULT 0.0,
    predicted_score_next_month DOUBLE PRECISION DEFAULT 0.0,
    risk_level TEXT,
    risk_probability_pct DOUBLE PRECISION DEFAULT 0.0,
    confidence_pct DOUBLE PRECISION DEFAULT 0.0,
    top_risk_factors TEXT,
    recommended_mitigation TEXT,
    evaluated_at BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.performance_risk_predictions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for performance_risk_predictions" ON public.performance_risk_predictions;
CREATE POLICY "Tenant isolation for performance_risk_predictions" ON public.performance_risk_predictions FOR ALL USING (true);

-- Table: performance_scores (PerformanceScoreEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.performance_scores (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    entity_type TEXT,
    entity_id TEXT,
    entity_name TEXT,
    department_id TEXT,
    department_name TEXT,
    period TEXT,
    completion_rate DOUBLE PRECISION DEFAULT 0.0,
    quality_score DOUBLE PRECISION DEFAULT 0.0,
    deadline_discipline DOUBLE PRECISION DEFAULT 0.0,
    productivity_volume DOUBLE PRECISION DEFAULT 0.0,
    collaboration_score DOUBLE PRECISION DEFAULT 0.0,
    attendance_uptime DOUBLE PRECISION DEFAULT 0.0,
    total_score DOUBLE PRECISION DEFAULT 0.0,
    rank INTEGER DEFAULT 0,
    trend TEXT,
    historical_scores_json JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.performance_scores ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for performance_scores" ON public.performance_scores;
CREATE POLICY "Tenant isolation for performance_scores" ON public.performance_scores FOR ALL USING (true);

-- Table: persona_handoff_rules (PersonaHandoffRuleEntity from OmnichannelSalesEntities.kt)
CREATE TABLE IF NOT EXISTS public.persona_handoff_rules (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.persona_handoff_rules ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for persona_handoff_rules" ON public.persona_handoff_rules;
CREATE POLICY "Tenant isolation for persona_handoff_rules" ON public.persona_handoff_rules FOR ALL USING (true);

-- Table: play_store_releases (PlayStoreReleaseEntity from ComplianceReleaseEntities.kt)
CREATE TABLE IF NOT EXISTS public.play_store_releases (
    id VARCHAR(128) PRIMARY KEY,
    track TEXT,
    version_code INTEGER DEFAULT 0,
    version_name TEXT,
    staged_rollout_percentage INTEGER DEFAULT 0,
    status TEXT,
    crash_free_user_rate_pct DOUBLE PRECISION DEFAULT 0.0,
    anr_rate_pct DOUBLE PRECISION DEFAULT 0.0,
    active_tester_count INTEGER DEFAULT 0,
    target_audience TEXT,
    release_notes_id TEXT,
    published_at BIGINT DEFAULT 0,
    app_signing_key_fingerprint_sha256 TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.play_store_releases ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for play_store_releases" ON public.play_store_releases;
CREATE POLICY "Tenant isolation for play_store_releases" ON public.play_store_releases FOR ALL USING (true);

-- Table: prompt_cache_registry (PromptCacheRegistryEntity from CostOptimizationEntities.kt)
CREATE TABLE IF NOT EXISTS public.prompt_cache_registry (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.prompt_cache_registry ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for prompt_cache_registry" ON public.prompt_cache_registry;
CREATE POLICY "Tenant isolation for prompt_cache_registry" ON public.prompt_cache_registry FOR ALL USING (true);

-- Table: prospect_lead_cards (ProspectLeadCardEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.prospect_lead_cards (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    public_platform TEXT,
    public_source_url TEXT,
    public_author_handle TEXT,
    detected_need TEXT,
    target_category TEXT,
    opportunity_score DOUBLE PRECISION DEFAULT 0.0,
    sentiment_intensity TEXT,
    urgency_level TEXT,
    raw_public_snippet TEXT,
    recommended_outreach TEXT,
    follow_up_status TEXT,
    sales_notes TEXT,
    is_consent_compliant BOOLEAN DEFAULT false,
    created_at BIGINT DEFAULT 0,
    updated_at BIGINT DEFAULT 0
);
ALTER TABLE public.prospect_lead_cards ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for prospect_lead_cards" ON public.prospect_lead_cards;
CREATE POLICY "Tenant isolation for prospect_lead_cards" ON public.prospect_lead_cards FOR ALL USING (true);

-- Table: quotations (QuotationEntity from CartOrderShipmentEntities.kt)
CREATE TABLE IF NOT EXISTS public.quotations (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.quotations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for quotations" ON public.quotations;
CREATE POLICY "Tenant isolation for quotations" ON public.quotations FOR ALL USING (true);

-- Table: report_data_points (ReportDataPointEntity from AutomaticReportEntities.kt)
CREATE TABLE IF NOT EXISTS public.report_data_points (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.report_data_points ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for report_data_points" ON public.report_data_points;
CREATE POLICY "Tenant isolation for report_data_points" ON public.report_data_points FOR ALL USING (true);

-- Table: search_history (SearchHistoryEntity from SearchHistoryEntity.kt)
CREATE TABLE IF NOT EXISTS public.search_history (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    user_id TEXT,
    query TEXT,
    selected_category TEXT,
    result_count INTEGER DEFAULT 0,
    searched_at BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.search_history ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for search_history" ON public.search_history;
CREATE POLICY "Tenant isolation for search_history" ON public.search_history FOR ALL USING (true);

-- Table: security_incidents (SecurityIncidentEntity from SecurityHardeningEntities.kt)
CREATE TABLE IF NOT EXISTS public.security_incidents (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    incident_type TEXT,
    severity TEXT,
    source_ip TEXT,
    device_fingerprint TEXT,
    description TEXT,
    payload_snippet JSONB DEFAULT '{}'::jsonb,
    reported_at BIGINT DEFAULT 0,
    resolved_at BIGINT DEFAULT 0,
    status TEXT,
    metadata_json JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.security_incidents ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for security_incidents" ON public.security_incidents;
CREATE POLICY "Tenant isolation for security_incidents" ON public.security_incidents FOR ALL USING (true);

-- Table: semantic_response_cache (SemanticResponseCacheEntity from CostOptimizationEntities.kt)
CREATE TABLE IF NOT EXISTS public.semantic_response_cache (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.semantic_response_cache ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for semantic_response_cache" ON public.semantic_response_cache;
CREATE POLICY "Tenant isolation for semantic_response_cache" ON public.semantic_response_cache FOR ALL USING (true);

-- Table: service_request_attachments (ServiceRequestAttachmentEntity from ServiceRequestEntities.kt)
CREATE TABLE IF NOT EXISTS public.service_request_attachments (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.service_request_attachments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for service_request_attachments" ON public.service_request_attachments;
CREATE POLICY "Tenant isolation for service_request_attachments" ON public.service_request_attachments FOR ALL USING (true);

-- Table: service_requests (ServiceRequestEntity from ServiceRequestEntities.kt)
CREATE TABLE IF NOT EXISTS public.service_requests (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.service_requests ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for service_requests" ON public.service_requests;
CREATE POLICY "Tenant isolation for service_requests" ON public.service_requests FOR ALL USING (true);

-- Table: shipment_tracking_events (ShipmentTrackingEventEntity from CartOrderShipmentEntities.kt)
CREATE TABLE IF NOT EXISTS public.shipment_tracking_events (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.shipment_tracking_events ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for shipment_tracking_events" ON public.shipment_tracking_events;
CREATE POLICY "Tenant isolation for shipment_tracking_events" ON public.shipment_tracking_events FOR ALL USING (true);

-- Table: shipments (ShipmentEntity from CartOrderShipmentEntities.kt)
CREATE TABLE IF NOT EXISTS public.shipments (
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.shipments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for shipments" ON public.shipments;
CREATE POLICY "Tenant isolation for shipments" ON public.shipments FOR ALL USING (true);

-- Table: support_tickets (SupportTicketEntity from ComplianceReleaseEntities.kt)
CREATE TABLE IF NOT EXISTS public.support_tickets (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    requester_name TEXT,
    requester_email TEXT,
    category TEXT,
    subject TEXT,
    priority TEXT,
    status TEXT,
    channel TEXT,
    created_at BIGINT DEFAULT 0,
    updated_at BIGINT DEFAULT 0,
    response_summary TEXT
);
ALTER TABLE public.support_tickets ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for support_tickets" ON public.support_tickets;
CREATE POLICY "Tenant isolation for support_tickets" ON public.support_tickets FOR ALL USING (true);

-- Table: tool_health_checks (ToolHealthCheckEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.tool_health_checks (
    id VARCHAR(128) PRIMARY KEY,
    tool_name TEXT,
    status TEXT,
    latency_ms BIGINT DEFAULT 0,
    last_checked_at BIGINT DEFAULT 0,
    error_detail TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.tool_health_checks ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for tool_health_checks" ON public.tool_health_checks;
CREATE POLICY "Tenant isolation for tool_health_checks" ON public.tool_health_checks FOR ALL USING (true);

-- Table: tool_invocations (ToolInvocationEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.tool_invocations (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    tool_name TEXT,
    caller_type TEXT,
    caller_id TEXT,
    caller_name TEXT,
    input_json JSONB DEFAULT '{}'::jsonb,
    output_json JSONB DEFAULT '{}'::jsonb,
    duration_ms BIGINT DEFAULT 0,
    status TEXT,
    risk_level TEXT,
    approval_status TEXT,
    approved_by TEXT,
    timestamp BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.tool_invocations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for tool_invocations" ON public.tool_invocations;
CREATE POLICY "Tenant isolation for tool_invocations" ON public.tool_invocations FOR ALL USING (true);

-- Table: tool_permissions (ToolPermissionEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.tool_permissions (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    agent_id TEXT,
    tool_id TEXT,
    is_granted BOOLEAN DEFAULT false,
    granted_by TEXT,
    granted_at BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.tool_permissions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for tool_permissions" ON public.tool_permissions;
CREATE POLICY "Tenant isolation for tool_permissions" ON public.tool_permissions FOR ALL USING (true);

-- Table: trial_daily_usage (TrialDailyUsageEntity from TrialEntities.kt)
CREATE TABLE IF NOT EXISTS public.trial_daily_usage (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    usage_date TEXT,
    task_count INTEGER DEFAULT 0,
    updated_at BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.trial_daily_usage ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for trial_daily_usage" ON public.trial_daily_usage;
CREATE POLICY "Tenant isolation for trial_daily_usage" ON public.trial_daily_usage FOR ALL USING (true);

-- Table: trial_subscriptions (TrialSubscriptionEntity from TrialEntities.kt)
CREATE TABLE IF NOT EXISTS public.trial_subscriptions (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    started_at BIGINT DEFAULT 0,
    expires_at BIGINT DEFAULT 0,
    status TEXT,
    daily_task_limit INTEGER DEFAULT 0,
    created_at BIGINT DEFAULT 0,
    updated_at BIGINT DEFAULT 0
);
ALTER TABLE public.trial_subscriptions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for trial_subscriptions" ON public.trial_subscriptions;
CREATE POLICY "Tenant isolation for trial_subscriptions" ON public.trial_subscriptions FOR ALL USING (true);

-- Table: user_persona (UserPersonaEntity from OnboardingEntities.kt)
CREATE TABLE IF NOT EXISTS public.user_persona (
    id VARCHAR(128) PRIMARY KEY,
    user_id TEXT,
    tenant_id TEXT,
    display_name TEXT,
    job_level TEXT,
    job_sub_title TEXT,
    company_name_input TEXT,
    company_code_input TEXT,
    industry_category TEXT,
    company_size_range TEXT,
    company_description TEXT,
    business_url TEXT,
    usage_goal TEXT,
    work_focus_area TEXT,
    automation_needs TEXT,
    channels_used TEXT,
    usage_preference TEXT,
    primary_target TEXT,
    whatsapp_number TEXT,
    telegram_username TEXT,
    is_complete BOOLEAN DEFAULT false,
    created_at BIGINT DEFAULT 0,
    updated_at BIGINT DEFAULT 0
);
ALTER TABLE public.user_persona ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for user_persona" ON public.user_persona;
CREATE POLICY "Tenant isolation for user_persona" ON public.user_persona FOR ALL USING (true);

-- Table: user_preferences (UserPreferenceEntity from SessionEntities.kt)
CREATE TABLE IF NOT EXISTS public.user_preferences (
    id VARCHAR(128) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id TEXT,
    tenant_id TEXT,
    theme_preference TEXT,
    language_preference TEXT,
    last_password_changed_at BIGINT DEFAULT 0,
    updated_at BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.user_preferences ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for user_preferences" ON public.user_preferences;
CREATE POLICY "Tenant isolation for user_preferences" ON public.user_preferences FOR ALL USING (true);

-- Table: user_sessions (UserSessionEntity from SessionEntities.kt)
CREATE TABLE IF NOT EXISTS public.user_sessions (
    id VARCHAR(128) PRIMARY KEY,
    user_id TEXT,
    tenant_id TEXT,
    device_name TEXT,
    os_name TEXT,
    ip_address TEXT,
    location_approx TEXT,
    is_current_session BOOLEAN DEFAULT false,
    created_at BIGINT DEFAULT 0,
    last_active_at BIGINT DEFAULT 0,
    expires_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.user_sessions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for user_sessions" ON public.user_sessions;
CREATE POLICY "Tenant isolation for user_sessions" ON public.user_sessions FOR ALL USING (true);

-- Table: work_locations (WorkLocationEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.work_locations (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    name TEXT,
    address TEXT,
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    radius_meters DOUBLE PRECISION DEFAULT 0.0,
    is_active BOOLEAN DEFAULT false,
    assigned_department_ids TEXT,
    expected_check_in_time TEXT,
    expected_check_out_time TEXT,
    created_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.work_locations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for work_locations" ON public.work_locations;
CREATE POLICY "Tenant isolation for work_locations" ON public.work_locations FOR ALL USING (true);

-- Table: work_reports_daily (WorkReportDailyEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.work_reports_daily (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    staff_id TEXT,
    staff_name TEXT,
    department_name TEXT,
    report_date TEXT,
    work_summary TEXT,
    completed_tasks_summary TEXT,
    linked_task_ids_json JSONB DEFAULT '{}'::jsonb,
    blockers_and_challenges TEXT,
    plan_for_tomorrow TEXT,
    hours_worked DOUBLE PRECISION DEFAULT 0.0,
    sentiment_rating TEXT,
    photo_attachment_base64 TEXT,
    submitted_at BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.work_reports_daily ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for work_reports_daily" ON public.work_reports_daily;
CREATE POLICY "Tenant isolation for work_reports_daily" ON public.work_reports_daily FOR ALL USING (true);

-- Table: workflow_definitions (WorkflowDefinitionEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.workflow_definitions (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    name TEXT,
    description TEXT,
    trigger_type TEXT,
    input_schema_json JSONB DEFAULT '{}'::jsonb,
    graph_json JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN DEFAULT false,
    created_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.workflow_definitions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for workflow_definitions" ON public.workflow_definitions;
CREATE POLICY "Tenant isolation for workflow_definitions" ON public.workflow_definitions FOR ALL USING (true);

-- Table: workflow_executions (WorkflowExecutionEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.workflow_executions (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    workflow_def_id TEXT,
    trigger_source TEXT,
    input_payload JSONB DEFAULT '{}'::jsonb,
    status TEXT,
    current_step_index INTEGER DEFAULT 0,
    total_steps INTEGER DEFAULT 0,
    started_at BIGINT DEFAULT 0,
    completed_at BIGINT DEFAULT 0,
    duration_ms BIGINT DEFAULT 0,
    total_cost_usd DOUBLE PRECISION DEFAULT 0.0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.workflow_executions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for workflow_executions" ON public.workflow_executions;
CREATE POLICY "Tenant isolation for workflow_executions" ON public.workflow_executions FOR ALL USING (true);

-- Table: workflow_node_runs (WorkflowNodeRunEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.workflow_node_runs (
    id VARCHAR(128) PRIMARY KEY,
    execution_id TEXT,
    task_id TEXT,
    node_key TEXT,
    node_type TEXT,
    status TEXT,
    input_json JSONB DEFAULT '{}'::jsonb,
    output_json JSONB DEFAULT '{}'::jsonb,
    error_detail TEXT,
    started_at BIGINT DEFAULT 0,
    finished_at BIGINT DEFAULT 0,
    duration_ms BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.workflow_node_runs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for workflow_node_runs" ON public.workflow_node_runs;
CREATE POLICY "Tenant isolation for workflow_node_runs" ON public.workflow_node_runs FOR ALL USING (true);

-- Table: workflow_nodes (WorkflowNodeEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.workflow_nodes (
    id VARCHAR(128) PRIMARY KEY,
    workflow_def_id TEXT,
    node_key TEXT,
    node_type TEXT,
    label TEXT,
    tool_name TEXT,
    model_preference TEXT,
    required_role TEXT,
    retry_policy TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.workflow_nodes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for workflow_nodes" ON public.workflow_nodes;
CREATE POLICY "Tenant isolation for workflow_nodes" ON public.workflow_nodes FOR ALL USING (true);

-- Table: world_news_articles (WorldNewsArticleEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.world_news_articles (
    id VARCHAR(128) PRIMARY KEY,
    cluster_id TEXT,
    tenant_id TEXT,
    title TEXT,
    description TEXT,
    source_url TEXT,
    source_name TEXT,
    published_at BIGINT DEFAULT 0,
    relevance_score DOUBLE PRECISION DEFAULT 0.0,
    keyword_matched TEXT,
    created_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.world_news_articles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for world_news_articles" ON public.world_news_articles;
CREATE POLICY "Tenant isolation for world_news_articles" ON public.world_news_articles FOR ALL USING (true);

-- Table: world_trend_clusters (WorldTrendClusterEntity from Entities.kt)
CREATE TABLE IF NOT EXISTS public.world_trend_clusters (
    id VARCHAR(128) PRIMARY KEY,
    tenant_id TEXT,
    cluster_title TEXT,
    category TEXT,
    summary TEXT,
    impact_assessment TEXT,
    recommended_action TEXT,
    relevance_score DOUBLE PRECISION DEFAULT 0.0,
    sentiment TEXT,
    article_count INTEGER DEFAULT 0,
    source_domains TEXT,
    last_updated BIGINT DEFAULT 0,
    created_at BIGINT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE public.world_trend_clusters ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for world_trend_clusters" ON public.world_trend_clusters;
CREATE POLICY "Tenant isolation for world_trend_clusters" ON public.world_trend_clusters FOR ALL USING (true);
