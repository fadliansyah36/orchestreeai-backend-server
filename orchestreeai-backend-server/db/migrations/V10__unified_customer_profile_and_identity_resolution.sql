-- ====================================================================
-- OrchestreeAI Database Migration V10: Unified Customer Profile & Identity Resolution
-- PRD Addendum Section 34 (Omnichannel AI Sales & Marketing Workforce)
-- ====================================================================

-- 1. Customers Master Table
CREATE TABLE IF NOT EXISTS customers (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    display_name VARCHAR(255) NOT NULL,
    primary_channel VARCHAR(64) NOT NULL, -- WHATSAPP, TELEGRAM, INSTAGRAM, TIKTOK, WEBSITE, SHOPEE, TOKOPEDIA, BLIBLI, EMAIL
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    merged_into_id VARCHAR(64) REFERENCES customers(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_customers_tenant_name ON customers(tenant_id, display_name);
CREATE INDEX IF NOT EXISTS idx_customers_merged_into ON customers(merged_into_id);

-- 2. Customer Channel Identities Table (Many-to-One per Customer)
CREATE TABLE IF NOT EXISTS customer_channel_identities (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    channel_type VARCHAR(64) NOT NULL, -- WHATSAPP, TELEGRAM, INSTAGRAM, TIKTOK, WEBSITE, SHOPEE, TOKOPEDIA, BLIBLI
    channel_external_id VARCHAR(512) NOT NULL, -- Envelope-encrypted PII (phone/email/handle)
    channel_identifier_hash VARCHAR(64) NOT NULL, -- SHA-256 hash for fast O(1) exact lookup
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cust_identities_cust_id ON customer_channel_identities(customer_id);
CREATE INDEX IF NOT EXISTS idx_cust_identities_hash ON customer_channel_identities(channel_identifier_hash);

-- 3. Customer Dynamic Attributes Table
CREATE TABLE IF NOT EXISTS customer_attributes (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    attribute_key VARCHAR(128) NOT NULL, -- budget, preferred_category, shoe_size, pain_point, timeline, decision_maker
    attribute_value TEXT NOT NULL,
    source VARCHAR(255) NOT NULL, -- Channel message origin, e.g., 'whatsapp:+628123456789'
    confidence NUMERIC(4, 3) NOT NULL DEFAULT 1.000, -- 0.000 - 1.000 confidence score
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cust_attrs_cust_id ON customer_attributes(customer_id, attribute_key);

-- 4. Customer Segments Table
CREATE TABLE IF NOT EXISTS customer_segments (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    segment_code VARCHAR(128) NOT NULL, -- HIGH_VALUE, HIKING_ENTHUSIAST, PRICE_SENSITIVE, ENTERPRISE_DECISION_MAKER
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(64) NOT NULL DEFAULT 'SYSTEM' -- SYSTEM / USER
);

CREATE INDEX IF NOT EXISTS idx_cust_segments_cust_id ON customer_segments(customer_id, segment_code);

-- 5. Customer Funnel State Table
CREATE TABLE IF NOT EXISTS customer_funnel_state (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    funnel_stage VARCHAR(64) NOT NULL DEFAULT 'AWARENESS', -- AWARENESS, LEAD, OPPORTUNITY, CUSTOMER, REPEAT
    entered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    previous_stage VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_cust_funnel_cust_id ON customer_funnel_state(customer_id);

-- 6. Customer Merge Candidates (For WEAK Match Review)
CREATE TABLE IF NOT EXISTS customer_merge_candidates (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    primary_customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    candidate_customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    similarity_score NUMERIC(4, 3) NOT NULL, -- 0.850 - 0.999
    match_reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by VARCHAR(64),
    reviewed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_cust_merge_cand_tenant ON customer_merge_candidates(tenant_id, status);

-- 7. Customer Merge Audit Logs (Append-Only)
CREATE TABLE IF NOT EXISTS customer_merge_audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    source_customer_id VARCHAR(64) NOT NULL,
    target_customer_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL, -- MERGED, UNMERGED, REJECTED
    performed_by VARCHAR(128) NOT NULL,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details_json JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_cust_merge_audit ON customer_merge_audit_logs(tenant_id, performed_at);

-- Row-Level Security (RLS) Policies for Multi-Tenancy Isolation
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_merge_candidates ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_merge_audit_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_customers ON customers;
CREATE POLICY tenant_isolation_customers ON customers
    USING (tenant_id = current_setting('app.current_tenant_id', true) OR current_setting('app.is_super_admin', true) = 'true');

DROP POLICY IF EXISTS tenant_isolation_merge_candidates ON customer_merge_candidates;
CREATE POLICY tenant_isolation_merge_candidates ON customer_merge_candidates
    USING (tenant_id = current_setting('app.current_tenant_id', true) OR current_setting('app.is_super_admin', true) = 'true');

DROP POLICY IF EXISTS tenant_isolation_merge_audit ON customer_merge_audit_logs;
CREATE POLICY tenant_isolation_merge_audit ON customer_merge_audit_logs
    USING (tenant_id = current_setting('app.current_tenant_id', true) OR current_setting('app.is_super_admin', true) = 'true');
