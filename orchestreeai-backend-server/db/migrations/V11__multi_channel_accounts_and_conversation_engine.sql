-- ====================================================================
-- OrchestreeAI Database Migration V11: Multi-Channel Accounts & Conversation Engine
-- PRD Addendum Section 49 & Section 35 (Omnichannel AI Sales & Marketing Workforce)
-- ====================================================================

-- 1. Channel Accounts Table (PRD Section 49.2)
CREATE TABLE IF NOT EXISTS channel_accounts (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    channel_type VARCHAR(64) NOT NULL, -- WHATSAPP, TELEGRAM, INSTAGRAM, TIKTOK, FACEBOOK, WEBSITE, SHOPEE, TOKOPEDIA, BLIBLI
    account_label VARCHAR(255) NOT NULL,
    external_identifier VARCHAR(512) NOT NULL,
    external_identifier_hash VARCHAR(64) NOT NULL,
    credential_ref VARCHAR(255),
    credentials_encrypted TEXT NOT NULL DEFAULT '',
    department_id VARCHAR(64) REFERENCES departments(id) ON DELETE SET NULL,
    status VARCHAR(64) NOT NULL DEFAULT 'PENDING_VERIFICATION', -- PENDING_VERIFICATION, ACTIVE, PENDING_OWNER_APPROVAL, SUSPENDED, ERROR, DEACTIVATED
    created_by_user_id VARCHAR(64) NOT NULL,
    requires_owner_approval BOOLEAN NOT NULL DEFAULT FALSE,
    total_credit_used NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_channel_account UNIQUE (tenant_id, channel_type, external_identifier_hash)
);

CREATE INDEX IF NOT EXISTS idx_channel_acc_tenant ON channel_accounts(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_channel_acc_lookup ON channel_accounts(tenant_id, channel_type, external_identifier_hash);

-- 2. Channel Account Permissions Table (Staff Role-based access)
CREATE TABLE IF NOT EXISTS channel_account_permissions (
    id VARCHAR(64) PRIMARY KEY,
    channel_account_id VARCHAR(64) NOT NULL REFERENCES channel_accounts(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_level VARCHAR(32) NOT NULL DEFAULT 'OPERATE', -- MANAGE, OPERATE, VIEW_ONLY
    granted_by VARCHAR(64) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_channel_acc_user_perm UNIQUE (channel_account_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_channel_acc_perm_user ON channel_account_permissions(user_id);

-- 3. Channel Account Persona Assignments Table
CREATE TABLE IF NOT EXISTS channel_account_persona_assignments (
    id VARCHAR(64) PRIMARY KEY,
    channel_account_id VARCHAR(64) NOT NULL REFERENCES channel_accounts(id) ON DELETE CASCADE,
    ai_agent_id VARCHAR(64) NOT NULL REFERENCES ai_agents(id) ON DELETE CASCADE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_channel_acc_agent_assignment UNIQUE (channel_account_id, ai_agent_id)
);

-- 4. Channel Account Usage Ledger Table (PRD Section 49.2 & 49.5)
CREATE TABLE IF NOT EXISTS channel_account_usage_ledger (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    channel_account_id VARCHAR(64) NOT NULL REFERENCES channel_accounts(id) ON DELETE CASCADE,
    conversation_id VARCHAR(64),
    message_id VARCHAR(64),
    action_type VARCHAR(64) NOT NULL, -- MESSAGE_SENT, MESSAGE_RECEIVED, LLM_TOKEN, IMAGE_GENERATED, TOOL_INVOCATION
    credit_deducted NUMERIC(10, 4) NOT NULL,
    unit_count INT NOT NULL DEFAULT 1,
    cost_details_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usage_ledger_tenant ON channel_account_usage_ledger(tenant_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_usage_ledger_account ON channel_account_usage_ledger(channel_account_id, timestamp);

-- 5. Tenant Credit Wallet Table (Single Source of Truth per Tenant - PRD 49.2)
CREATE TABLE IF NOT EXISTS tenant_credit_wallet (
    tenant_id VARCHAR(64) PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    balance_credits NUMERIC(12, 2) NOT NULL DEFAULT 5000.00,
    reserved_credits NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(16) NOT NULL DEFAULT 'IDR',
    low_balance_threshold NUMERIC(12, 2) NOT NULL DEFAULT 200.00,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Tenant Credit Transactions Table (Audit trail & traceability)
CREATE TABLE IF NOT EXISTS tenant_credit_transactions (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    channel_account_id VARCHAR(64) REFERENCES channel_accounts(id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL,
    balance_after NUMERIC(12, 2) NOT NULL,
    transaction_type VARCHAR(64) NOT NULL, -- USAGE_DEDUCTION, TOPUP, ADJUSTMENT, REFUND
    reference_id VARCHAR(128),
    description TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_credit_tx_tenant ON tenant_credit_transactions(tenant_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_credit_tx_channel ON tenant_credit_transactions(channel_account_id);

-- 7. Conversations Table (PRD Section 35.1)
CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    channel_account_id VARCHAR(64) NOT NULL REFERENCES channel_accounts(id) ON DELETE CASCADE,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    customer_channel_identifier VARCHAR(255) NOT NULL,
    channel_type VARCHAR(64) NOT NULL,
    assigned_persona VARCHAR(64) NOT NULL DEFAULT 'RECEPTIONIST',
    assigned_agent_id VARCHAR(64) REFERENCES ai_agents(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN', -- OPEN, HANDED_OVER, CLOSED
    lead_score NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    sentiment_score NUMERIC(4, 3) NOT NULL DEFAULT 0.000,
    last_message_snippet TEXT,
    unread_count INT NOT NULL DEFAULT 0,
    current_intent VARCHAR(128),
    sales_stage VARCHAR(64) NOT NULL DEFAULT 'GREETING',
    source_reference VARCHAR(255),
    source_campaign_id VARCHAR(64),
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conv_tenant_status ON conversations(tenant_id, status, last_activity_at);
CREATE INDEX IF NOT EXISTS idx_conv_customer ON conversations(customer_id);
CREATE INDEX IF NOT EXISTS idx_conv_channel_account ON conversations(channel_account_id);

-- 8. Conversation Messages Table (PRD Section 35.1)
CREATE TABLE IF NOT EXISTS conversation_messages (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    sender_type VARCHAR(32) NOT NULL, -- CUSTOMER, AI_AGENT, HUMAN_STAFF
    sender_id VARCHAR(64) NOT NULL,
    sender_name VARCHAR(255) NOT NULL,
    message_text TEXT NOT NULL,
    message_type VARCHAR(32) NOT NULL DEFAULT 'TEXT', -- TEXT, IMAGE, PRODUCT_CARD, ORDER_CARD
    media_url TEXT,
    intent_detected VARCHAR(128),
    confidence NUMERIC(4, 3) NOT NULL DEFAULT 1.000,
    entities_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    external_message_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conv_msgs_convo ON conversation_messages(conversation_id, created_at);

-- 9. Conversation Intents Table
CREATE TABLE IF NOT EXISTS conversation_intents (
    id VARCHAR(64) PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL REFERENCES conversation_messages(id) ON DELETE CASCADE,
    conversation_id VARCHAR(64) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    detected_intent VARCHAR(128) NOT NULL,
    confidence NUMERIC(4, 3) NOT NULL,
    extracted_entities JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. Conversation Handovers Table
CREATE TABLE IF NOT EXISTS conversation_handovers (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    from_id VARCHAR(64) NOT NULL,
    to_id VARCHAR(64) NOT NULL,
    from_type VARCHAR(32) NOT NULL, -- AI_AGENT, HUMAN_STAFF
    to_type VARCHAR(32) NOT NULL, -- AI_AGENT, HUMAN_STAFF
    reason TEXT NOT NULL,
    summary_context TEXT NOT NULL,
    handed_over_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_by_staff VARCHAR(64),
    acknowledged_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_conv_handovers_tenant ON conversation_handovers(tenant_id, handed_over_at);

-- Multi-Tenant Row Level Security (RLS)
ALTER TABLE channel_accounts ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS channel_accounts_tenant_isolation ON channel_accounts;
CREATE POLICY channel_accounts_tenant_isolation ON channel_accounts
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE channel_account_usage_ledger ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS usage_ledger_tenant_isolation ON channel_account_usage_ledger;
CREATE POLICY usage_ledger_tenant_isolation ON channel_account_usage_ledger
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE tenant_credit_wallet ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS credit_wallet_tenant_isolation ON tenant_credit_wallet;
CREATE POLICY credit_wallet_tenant_isolation ON tenant_credit_wallet
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE tenant_credit_transactions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS credit_tx_tenant_isolation ON tenant_credit_transactions;
CREATE POLICY credit_tx_tenant_isolation ON tenant_credit_transactions
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS conversations_tenant_isolation ON conversations;
CREATE POLICY conversations_tenant_isolation ON conversations
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE conversation_messages ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS conv_messages_tenant_isolation ON conversation_messages;
CREATE POLICY conv_messages_tenant_isolation ON conversation_messages
    USING (tenant_id = current_setting('app.current_tenant_id', true));
