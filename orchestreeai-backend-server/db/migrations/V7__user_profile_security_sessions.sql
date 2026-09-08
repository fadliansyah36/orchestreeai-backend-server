-- ====================================================================
-- OrchestreeAI Database Migration V7: User Profile, Active Sessions & Password Reset Tokens
-- ====================================================================

-- 1. Add profile preferences & language to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS theme_preference VARCHAR(16) NOT NULL DEFAULT 'DARK';
ALTER TABLE users ADD COLUMN IF NOT EXISTS language_preference VARCHAR(8) NOT NULL DEFAULT 'id';
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_password_changed_at TIMESTAMPTZ;

-- 2. Password Reset Tokens Table (Cryptographically hashed, with expiry and rate limiting)
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pwd_reset_hash ON password_reset_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_pwd_reset_user ON password_reset_tokens(user_id, expires_at, is_used);

-- 3. Password Reset Rate Limiting Ledger (Track request timestamps per email/IP for brute-force/enumeration defense)
CREATE TABLE IF NOT EXISTS password_reset_rate_limits (
    id VARCHAR(64) PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL, -- email or IP address
    attempt_count INT NOT NULL DEFAULT 1,
    last_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_until TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_pwd_rate_limit ON password_reset_rate_limits(identifier);

-- 4. User Active Sessions Extended Metadata (device_name, os_name, location_approx, last_active_at)
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS device_name VARCHAR(128) DEFAULT 'Android Client';
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS os_name VARCHAR(64) DEFAULT 'Android';
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS location_approx VARCHAR(128) DEFAULT 'Jakarta, Indonesia';
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

-- Enable RLS on password_reset_tokens if needed
ALTER TABLE sessions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_sessions ON sessions;
DROP POLICY IF EXISTS tenant_isolation_sessions ON sessions;
CREATE POLICY tenant_isolation_sessions ON sessions USING (app_has_tenant_access(tenant_id));
