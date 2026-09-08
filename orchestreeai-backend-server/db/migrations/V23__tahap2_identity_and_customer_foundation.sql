-- ==============================================================================
-- OrchestreeAI Migration: Tahap 2 — Identity & Customer Foundation
-- File: V23__tahap2_identity_and_customer_foundation.sql
-- PRD Source: Master Bagian 16, Addendum 1 Bagian 34 & 35, Addendum 2 Bagian 81.4
-- Deskripsi: Skema onboarding user persona, customer identity resolution, dan audit logs.
-- ==============================================================================

-- 1. User Persona (Onboarding, Role, Company Profile Input)
CREATE TABLE IF NOT EXISTS user_persona (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    tenant_id TEXT,
    display_name TEXT NOT NULL,
    job_level TEXT NOT NULL,
    job_sub_title TEXT,
    company_name_input TEXT,
    company_code_input TEXT,
    industry_category TEXT,
    company_size_range TEXT,
    company_description TEXT,
    business_url TEXT,
    usage_goal TEXT NOT NULL DEFAULT '',
    work_focus_area TEXT NOT NULL DEFAULT '',
    automation_needs TEXT,
    channels_used TEXT NOT NULL DEFAULT '',
    usage_preference TEXT,
    primary_target TEXT,
    whatsapp_number TEXT,
    telegram_username TEXT,
    is_complete BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_user_persona_user_id ON user_persona(user_id);
CREATE INDEX IF NOT EXISTS idx_user_persona_tenant_id ON user_persona(tenant_id);

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================
ALTER TABLE user_persona ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_user_persona ON user_persona;
CREATE POLICY tenant_isolation_user_persona ON user_persona
    FOR ALL
    USING (
        tenant_id IS NULL 
        OR app_has_tenant_access(tenant_id)
        OR auth.role() = 'service_role'
    );

-- ==============================================================================
-- SEED DATA (DEFAULT ONBOARDED USER PERSONA)
-- ==============================================================================
INSERT INTO user_persona (
    id, user_id, tenant_id, display_name, job_level, job_sub_title,
    company_name_input, company_code_input, industry_category, company_size_range,
    company_description, business_url, usage_goal, work_focus_area,
    automation_needs, channels_used, usage_preference, primary_target,
    whatsapp_number, telegram_username, is_complete, created_at, updated_at
) VALUES (
    'persona-owner-01',
    'user-01',
    'tenant-nusantara',
    'Budi Santoso',
    'Owner / Pendiri',
    'Founder & CEO',
    'Nusantara Enterprise Corp',
    'ORCH-NUSANTARA',
    'ind-startup',
    '51-200 karyawan',
    'Nusantara Enterprise Corp adalah penyedia platform Autonomous AI Workforce terdepan di Asia Tenggara, mengintegrasikan agen AI otonom dengan tim manusia lintas divisi Sales, Marketing, Operasional, dan HR.',
    'https://nusantara.ai',
    'Meningkatkan efisiensi operasional dan otomatisasi multi-channel perusahaan dengan kolaborasi AI.',
    'Sales, Marketing, Operasional, HR',
    'Autonomous lead prospecting, customer support AI, inventory reconciliation, and dynamic reporting',
    'Web, WhatsApp, Telegram, Instagram',
    'perusahaan',
    'automation',
    '+6281234567890',
    '@nusantara_ai_ceo',
    true,
    (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT - 2592000000,
    (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT - 172800000
) ON CONFLICT (id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    job_level = EXCLUDED.job_level,
    job_sub_title = EXCLUDED.job_sub_title,
    company_name_input = EXCLUDED.company_name_input,
    company_code_input = EXCLUDED.company_code_input,
    industry_category = EXCLUDED.industry_category,
    company_size_range = EXCLUDED.company_size_range,
    company_description = EXCLUDED.company_description,
    business_url = EXCLUDED.business_url,
    usage_goal = EXCLUDED.usage_goal,
    work_focus_area = EXCLUDED.work_focus_area,
    automation_needs = EXCLUDED.automation_needs,
    channels_used = EXCLUDED.channels_used,
    is_complete = EXCLUDED.is_complete,
    updated_at = EXCLUDED.updated_at;
