-- ====================================================================
-- OrchestreeAI Database Migration V5: Billing Service & Payment Gateway
-- PostgreSQL 16 + Row-Level Security (RLS)
-- Covers Subscriptions, Invoices, Usage Records, Payment Methods, Dunning
-- ====================================================================

-- 1. Payment Methods Table
CREATE TABLE IF NOT EXISTS payment_methods (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    gateway VARCHAR(32) NOT NULL DEFAULT 'MIDTRANS',
    method_type VARCHAR(64) NOT NULL, -- CREDIT_CARD, BANK_TRANSFER_BCA, BANK_TRANSFER_MANDIRI, GOPAY, QRIS
    account_mask VARCHAR(64) NOT NULL,
    card_brand VARCHAR(32),
    expiry_info VARCHAR(16),
    is_default BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Enhance Tenant Subscriptions
CREATE TABLE IF NOT EXISTS subscriptions (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plan_id VARCHAR(64) NOT NULL REFERENCES subscription_plans(id),
    plan_name VARCHAR(128) NOT NULL,
    tier VARCHAR(64) NOT NULL DEFAULT 'PROFESSIONAL',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, TRIALING, PAST_DUE, CANCELLED, UNPAID
    billing_cycle VARCHAR(16) NOT NULL DEFAULT 'MONTHLY', -- MONTHLY, YEARLY
    price_idr NUMERIC(15, 2) NOT NULL DEFAULT 5999000.0,
    current_period_start TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_period_end TIMESTAMPTZ NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    payment_gateway VARCHAR(32) NOT NULL DEFAULT 'MIDTRANS',
    gateway_customer_id VARCHAR(128),
    gateway_subscription_id VARCHAR(128),
    grace_period_ends_at TIMESTAMPTZ,
    dunning_retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Invoices Table
CREATE TABLE IF NOT EXISTS invoices (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subscription_id VARCHAR(64) REFERENCES subscriptions(id) ON DELETE SET NULL,
    invoice_number VARCHAR(64) NOT NULL UNIQUE,
    plan_name VARCHAR(128) NOT NULL,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    base_amount_idr NUMERIC(15, 2) NOT NULL,
    overage_tokens_billed BIGINT NOT NULL DEFAULT 0,
    overage_amount_idr NUMERIC(15, 2) NOT NULL DEFAULT 0.0,
    tax_idr NUMERIC(15, 2) NOT NULL DEFAULT 0.0, -- PPN 11%
    total_amount_idr NUMERIC(15, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- DRAFT, PENDING, PAID, FAILED, VOID, REFUNDED
    payment_gateway VARCHAR(32) NOT NULL DEFAULT 'MIDTRANS',
    gateway_order_id VARCHAR(128) NOT NULL UNIQUE,
    gateway_transaction_id VARCHAR(128),
    gateway_payment_url TEXT,
    gateway_snap_token TEXT,
    payment_method_type VARCHAR(64),
    paid_at TIMESTAMPTZ,
    due_date TIMESTAMPTZ NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    dunning_status VARCHAR(32) NOT NULL DEFAULT 'NONE', -- NONE, IN_GRACE_PERIOD, RETRY_EXHAUSTED, RESOLVED
    error_message TEXT,
    raw_gateway_response_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Usage-based Records Table (LLM Token Overage Tracking)
CREATE TABLE IF NOT EXISTS usage_records (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id VARCHAR(64) REFERENCES invoices(id) ON DELETE SET NULL,
    period_month VARCHAR(32) NOT NULL, -- '2026-08'
    base_tokens_quota BIGINT NOT NULL,
    actual_tokens_used BIGINT NOT NULL,
    overage_tokens BIGINT NOT NULL DEFAULT 0,
    cost_per_1k_tokens_idr NUMERIC(10, 4) NOT NULL DEFAULT 15.0,
    calculated_overage_charge_idr NUMERIC(15, 2) NOT NULL DEFAULT 0.0,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Dunning Logs (Recovery & Retry Audit)
CREATE TABLE IF NOT EXISTS dunning_logs (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id VARCHAR(64) NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL,
    error_code VARCHAR(64),
    error_message TEXT,
    action_taken VARCHAR(128) NOT NULL,
    next_retry_scheduled_at TIMESTAMPTZ,
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Apply Row Level Security (RLS)
ALTER TABLE payment_methods ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE usage_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE dunning_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_payment_methods ON payment_methods;
DROP POLICY IF EXISTS tenant_isolation_payment_methods ON payment_methods;
CREATE POLICY tenant_isolation_payment_methods ON payment_methods 
    USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_subscriptions ON subscriptions;
DROP POLICY IF EXISTS tenant_isolation_subscriptions ON subscriptions;
CREATE POLICY tenant_isolation_subscriptions ON subscriptions 
    USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_invoices ON invoices;
DROP POLICY IF EXISTS tenant_isolation_invoices ON invoices;
CREATE POLICY tenant_isolation_invoices ON invoices 
    USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_usage_records ON usage_records;
DROP POLICY IF EXISTS tenant_isolation_usage_records ON usage_records;
CREATE POLICY tenant_isolation_usage_records ON usage_records 
    USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_dunning_logs ON dunning_logs;
DROP POLICY IF EXISTS tenant_isolation_dunning_logs ON dunning_logs;
CREATE POLICY tenant_isolation_dunning_logs ON dunning_logs 
    USING (app_has_tenant_access(tenant_id));
