-- =====================================================================
-- Fase 127: Prospect Registrations & Lead Capture System (Public & Super Admin)
-- =====================================================================

CREATE TABLE IF NOT EXISTS prospect_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Data Diri & Kontak
    full_name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone_number TEXT NOT NULL,
    whatsapp_number TEXT,
    address TEXT,
    -- Data Perusahaan
    company_name TEXT NOT NULL,
    job_title TEXT NOT NULL,
    industry_category_id TEXT REFERENCES industry_catalog(id) ON DELETE SET NULL,
    company_size_range TEXT,
    -- Opsi Pilihan
    interest_option TEXT NOT NULL CHECK (interest_option IN
        ('schedule_meeting_presentation', 'direct_trial_or_subscription')),
    interested_plan_id UUID REFERENCES commercial_plans(id) ON DELETE SET NULL,
    -- Status Pengelolaan Super Admin
    trial_selection_status TEXT DEFAULT 'not_selected'
        CHECK (trial_selection_status IN ('not_selected','selected_for_trial','trial_activated','rejected')),
    meeting_status TEXT DEFAULT 'not_scheduled'
        CHECK (meeting_status IN ('not_scheduled','scheduled','completed','cancelled')),
    meeting_scheduled_at TIMESTAMPTZ,
    admin_notes TEXT,
    contacted_by_admin_id UUID,
    contacted_at TIMESTAMPTZ,
    activated_tenant_id VARCHAR(64) REFERENCES tenants(id) ON DELETE SET NULL,
    ip_address TEXT,
    submitted_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Indexing for high-efficiency querying & search
CREATE INDEX IF NOT EXISTS idx_prospect_email ON prospect_registrations(email);
CREATE INDEX IF NOT EXISTS idx_prospect_submitted_at ON prospect_registrations(submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_prospect_trial_status ON prospect_registrations(trial_selection_status);
CREATE INDEX IF NOT EXISTS idx_prospect_meeting_status ON prospect_registrations(meeting_status);
CREATE INDEX IF NOT EXISTS idx_prospect_interest_option ON prospect_registrations(interest_option);

-- Row Level Security (RLS)
ALTER TABLE prospect_registrations ENABLE ROW LEVEL SECURITY;

-- 1. Anyone (public anonymous) can INSERT a registration
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'prospect_registrations' AND policyname = 'insert_public_prospect'
    ) THEN
        CREATE POLICY insert_public_prospect ON prospect_registrations
            FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- 2. Read only for Super Admin or Service Role
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'prospect_registrations' AND policyname = 'read_only_super_admin'
    ) THEN
        CREATE POLICY read_only_super_admin ON prospect_registrations
            FOR SELECT USING (
                auth.role() = 'service_role' OR
                (auth.jwt() ->> 'role') = 'SUPER_ADMIN' OR
                current_setting('request.jwt.claim.role', true) = 'SUPER_ADMIN' OR
                current_user = 'postgres'
            );
    END IF;
END $$;

-- 3. Update only for Super Admin or Service Role
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'prospect_registrations' AND policyname = 'update_only_super_admin'
    ) THEN
        CREATE POLICY update_only_super_admin ON prospect_registrations
            FOR UPDATE USING (
                auth.role() = 'service_role' OR
                (auth.jwt() ->> 'role') = 'SUPER_ADMIN' OR
                current_setting('request.jwt.claim.role', true) = 'SUPER_ADMIN' OR
                current_user = 'postgres'
            );
    END IF;
END $$;

-- Realtime publication for immediate live synchronization in Admin Dashboard
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime' AND tablename = 'prospect_registrations'
        ) THEN
            ALTER PUBLICATION supabase_realtime ADD TABLE prospect_registrations;
        END IF;
    END IF;
END $$;
