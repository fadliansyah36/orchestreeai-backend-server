-- ==============================================================================
-- OrchestreeAI Migration: Tahap 4 — Lead & Sales Engine
-- File: V25__tahap4_lead_and_sales_engine.sql
-- PRD Source: Master Bagian 16, Addendum 1 Bagian 37 (Lead Engine & BANTD Scoring), PRD 22.2
-- Deskripsi: Skema tabel pipeline lead, kualifikasi BANTD, skor histori, dan vibe prospecting cards.
-- ==============================================================================

-- 1. Leads (Pipeline Prospek & Kualifikasi Komersial - PRD Addendum 1 Bagian 37)
CREATE TABLE IF NOT EXISTS leads (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    customer_id TEXT NOT NULL,
    conversation_id TEXT,
    channel_account_id TEXT,
    channel_type TEXT NOT NULL,
    customer_display_name TEXT NOT NULL,
    customer_identifier TEXT NOT NULL,
    funnel_stage TEXT NOT NULL DEFAULT 'COLD_LEAD', -- 'COLD_LEAD', 'WARM_LEAD', 'HOT_LEAD', 'QUALIFIED_LEAD', 'CONVERTED', 'DROPPED'
    lead_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    score_category TEXT NOT NULL DEFAULT 'COLD', -- 'COLD' (<40), 'WARM' (40-69), 'HOT' (>=70)
    qualification_completeness_pct DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    buying_intent_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    engagement_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    budget_match_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    channel_quality_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    purchase_history_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    bantd_budget TEXT,
    bantd_authority TEXT,
    bantd_need TEXT,
    bantd_timeline TEXT,
    bantd_decision TEXT,
    status TEXT NOT NULL DEFAULT 'OPEN', -- 'OPEN', 'IN_PROGRESS', 'WON', 'LOST', 'UNRESPONSIVE'
    assigned_staff_id TEXT,
    assigned_staff_name TEXT,
    hot_lead_notified_at BIGINT,
    last_evaluated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_leads_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_leads_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_leads_tenant_id ON leads(tenant_id);
CREATE INDEX IF NOT EXISTS idx_leads_customer_id ON leads(customer_id);
CREATE INDEX IF NOT EXISTS idx_leads_conversation_id ON leads(conversation_id);
CREATE INDEX IF NOT EXISTS idx_leads_funnel_stage ON leads(funnel_stage);
CREATE INDEX IF NOT EXISTS idx_leads_lead_score ON leads(lead_score);
CREATE INDEX IF NOT EXISTS idx_leads_status ON leads(status);

-- 2. Lead Qualification Answers (Ekstraksi Kualifikasi BANTD & Pain Point)
CREATE TABLE IF NOT EXISTS lead_qualification_answers (
    id TEXT PRIMARY KEY,
    lead_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    dimension_key TEXT NOT NULL, -- 'NEED', 'BUDGET', 'TIMELINE', 'AUTHORITY', 'DECISION', 'PAIN_POINT', 'PRODUCT_PREFERENCE'
    raw_answer_text TEXT NOT NULL,
    extracted_value TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    source_message_id TEXT,
    extracted_by_model TEXT NOT NULL DEFAULT 'gemini-3.5-flash',
    answered_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_lead_answers_lead FOREIGN KEY (lead_id)
        REFERENCES leads(id) ON DELETE CASCADE,
    CONSTRAINT fk_lead_answers_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT uq_lead_dimension UNIQUE (lead_id, dimension_key)
);

CREATE INDEX IF NOT EXISTS idx_lead_answers_lead_id ON lead_qualification_answers(lead_id);
CREATE INDEX IF NOT EXISTS idx_lead_answers_tenant_id ON lead_qualification_answers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_lead_answers_dimension ON lead_qualification_answers(dimension_key);

-- 3. Lead Score History (Audit Log Perhitungan Skor & Bobot)
CREATE TABLE IF NOT EXISTS lead_score_history (
    id TEXT PRIMARY KEY,
    lead_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    total_score DOUBLE PRECISION NOT NULL,
    previous_score DOUBLE PRECISION NOT NULL,
    score_delta DOUBLE PRECISION NOT NULL,
    funnel_stage TEXT NOT NULL,
    score_breakdown TEXT NOT NULL, -- JSON String
    trigger_event TEXT NOT NULL, -- 'MESSAGE_RECEIVED', 'ANSWER_EXTRACTED', 'STAGE_TRANSITION', 'RECALCULATED'
    calculated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_lead_score_history_lead FOREIGN KEY (lead_id)
        REFERENCES leads(id) ON DELETE CASCADE,
    CONSTRAINT fk_lead_score_history_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lead_score_history_lead_id ON lead_score_history(lead_id);
CREATE INDEX IF NOT EXISTS idx_lead_score_history_tenant_id ON lead_score_history(tenant_id);
CREATE INDEX IF NOT EXISTS idx_lead_score_history_calculated_at ON lead_score_history(calculated_at);

-- 4. Prospect Lead Cards (Vibe Prospecting & External Social Signals - PRD 22.2)
CREATE TABLE IF NOT EXISTS prospect_lead_cards (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    public_platform TEXT NOT NULL, -- 'HackerNews', 'Reddit Public RSS', 'GitHub Discussions', 'Public App Reviews'
    public_source_url TEXT NOT NULL,
    public_author_handle TEXT NOT NULL,
    detected_need TEXT NOT NULL,
    target_category TEXT NOT NULL,
    opportunity_score DOUBLE PRECISION NOT NULL, -- 0.0 to 100.0
    sentiment_intensity TEXT NOT NULL, -- 'HIGH_PAIN_POINT', 'ACTIVE_EVALUATION', 'INQUIRING_ALTERNATIVE', 'DISSATISFIED_WITH_COMPETITOR'
    urgency_level TEXT NOT NULL, -- 'HIGH', 'MEDIUM', 'LOW'
    raw_public_snippet TEXT NOT NULL,
    recommended_outreach TEXT NOT NULL,
    follow_up_status TEXT NOT NULL DEFAULT 'NEW', -- 'NEW', 'CONTACTED', 'QUALIFIED', 'NOT_INTERESTED', 'CONVERTED'
    sales_notes TEXT NOT NULL DEFAULT '',
    is_consent_compliant BOOLEAN NOT NULL DEFAULT TRUE, -- PRD 22.2 guardrail: verified no PII scraped
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_prospect_cards_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_prospect_cards_tenant_id ON prospect_lead_cards(tenant_id);
CREATE INDEX IF NOT EXISTS idx_prospect_cards_opportunity ON prospect_lead_cards(opportunity_score DESC);
CREATE INDEX IF NOT EXISTS idx_prospect_cards_follow_up ON prospect_lead_cards(follow_up_status);

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================
ALTER TABLE leads ENABLE ROW LEVEL SECURITY;
ALTER TABLE lead_qualification_answers ENABLE ROW LEVEL SECURITY;
ALTER TABLE lead_score_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE prospect_lead_cards ENABLE ROW LEVEL SECURITY;

-- 1. Policies for leads
DROP POLICY IF EXISTS tenant_isolation_leads ON leads;
CREATE POLICY tenant_isolation_leads ON leads
    FOR ALL
    USING (
        app_has_tenant_access(tenant_id)
        OR auth.role() = 'service_role'
    );

-- 2. Policies for lead_qualification_answers
DROP POLICY IF EXISTS tenant_isolation_lead_answers ON lead_qualification_answers;
CREATE POLICY tenant_isolation_lead_answers ON lead_qualification_answers
    FOR ALL
    USING (
        app_has_tenant_access(tenant_id)
        OR auth.role() = 'service_role'
    );

-- 3. Policies for lead_score_history
DROP POLICY IF EXISTS tenant_isolation_lead_score_history ON lead_score_history;
CREATE POLICY tenant_isolation_lead_score_history ON lead_score_history
    FOR ALL
    USING (
        app_has_tenant_access(tenant_id)
        OR auth.role() = 'service_role'
    );

-- 4. Policies for prospect_lead_cards
DROP POLICY IF EXISTS tenant_isolation_prospect_cards ON prospect_lead_cards;
CREATE POLICY tenant_isolation_prospect_cards ON prospect_lead_cards
    FOR ALL
    USING (
        app_has_tenant_access(tenant_id)
        OR auth.role() = 'service_role'
    );
