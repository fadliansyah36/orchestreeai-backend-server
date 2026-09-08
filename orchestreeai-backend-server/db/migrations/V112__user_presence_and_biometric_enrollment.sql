-- ====================================================================
-- OrchestreeAI Database Migration V112: User Presence & Biometric Enrollment
-- PRD Fase 112: Biometric Verification & Login Risk Gating
-- ====================================================================

DO $$
BEGIN
    -- Ensure users table has auth_user_id column if not present
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users') THEN
        ALTER TABLE public.users ADD COLUMN IF NOT EXISTS auth_user_id UUID;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS user_presence_enrollment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(64) REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    is_enabled BOOLEAN DEFAULT FALSE,
    enrolled_methods TEXT[],
    face_embedding_ref TEXT,  -- embedding terenkripsi, BUKAN foto mentah
    fingerprint_registered_device_ids TEXT[],
    enrolled_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS presence_check_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(64) REFERENCES users(id),
    check_type TEXT NOT NULL, 
    method_used TEXT,
    verification_result TEXT NOT NULL,
    device_id TEXT, 
    ip_address TEXT, 
    location_approx TEXT,
    checked_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_presence_enrollment_user ON user_presence_enrollment(user_id);
CREATE INDEX IF NOT EXISTS idx_presence_check_log_user_date ON presence_check_log(user_id, checked_at);

ALTER TABLE user_presence_enrollment ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS own_presence_data ON user_presence_enrollment;
CREATE POLICY own_presence_data ON user_presence_enrollment
    USING (user_id = (SELECT id FROM users WHERE auth_user_id = auth.uid() OR id = auth.uid()::text));

ALTER TABLE presence_check_log ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_presence_check_log ON presence_check_log;
CREATE POLICY tenant_presence_check_log ON presence_check_log FOR ALL USING (true);

-- Register to supabase_realtime publication
DO $$
DECLARE
    tbl text;
    tables text[] := ARRAY[
        'user_presence_enrollment',
        'presence_check_log'
    ];
BEGIN
    FOREACH tbl IN ARRAY tables LOOP
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = tbl) THEN
            EXECUTE format('ALTER TABLE public.%I REPLICA IDENTITY FULL;', tbl);
            BEGIN
                EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I;', tbl);
            EXCEPTION WHEN duplicate_object THEN
                NULL;
            END;
        END IF;
    END LOOP;
END $$;
