-- V31__tahap11_campaigns_and_service_requests.sql
-- PRD Addendum 1 Bagian 41 & 42.1: Omnichannel Marketing Campaigns & Customer Service Requests

-- 1. Table: campaigns
CREATE TABLE IF NOT EXISTS campaigns (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    prompt_instruction TEXT NOT NULL,
    target_segment_code VARCHAR(100),
    target_channel_type VARCHAR(50) NOT NULL DEFAULT 'ALL',
    channel_account_id VARCHAR(255),
    generated_query TEXT NOT NULL DEFAULT '',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    total_audience INT NOT NULL DEFAULT 0,
    sent_count INT NOT NULL DEFAULT 0,
    delivered_count INT NOT NULL DEFAULT 0,
    read_count INT NOT NULL DEFAULT 0,
    converted_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    risk_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    risk_evaluation_status VARCHAR(50) NOT NULL DEFAULT 'PASSED',
    brand_voice_score DOUBLE PRECISION NOT NULL DEFAULT 95.0,
    scheduled_at BIGINT,
    executed_at BIGINT,
    completed_at BIGINT,
    created_by VARCHAR(255) NOT NULL DEFAULT 'Admin',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_campaigns_tenant ON campaigns(tenant_id);
CREATE INDEX IF NOT EXISTS idx_campaigns_status ON campaigns(status);
CREATE INDEX IF NOT EXISTS idx_campaigns_created_at ON campaigns(created_at);

ALTER TABLE campaigns ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_campaigns ON campaigns;
CREATE POLICY tenant_isolation_campaigns ON campaigns
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_campaigns ON campaigns;
CREATE POLICY service_role_campaigns ON campaigns
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 2. Table: campaign_audiences
CREATE TABLE IF NOT EXISTS campaign_audiences (
    id VARCHAR(255) PRIMARY KEY,
    campaign_id VARCHAR(255) NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id VARCHAR(255) NOT NULL,
    customer_display_name VARCHAR(255) NOT NULL,
    primary_channel VARCHAR(50) NOT NULL,
    destination_identifier VARCHAR(255) NOT NULL,
    matched_criteria TEXT NOT NULL,
    personalized_attributes_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL DEFAULT 'RESOLVED',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_campaign_audiences_campaign ON campaign_audiences(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_audiences_tenant ON campaign_audiences(tenant_id);
CREATE INDEX IF NOT EXISTS idx_campaign_audiences_customer ON campaign_audiences(customer_id);
CREATE INDEX IF NOT EXISTS idx_campaign_audiences_status ON campaign_audiences(status);

ALTER TABLE campaign_audiences ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_campaign_audiences ON campaign_audiences;
CREATE POLICY tenant_isolation_campaign_audiences ON campaign_audiences
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_campaign_audiences ON campaign_audiences;
CREATE POLICY service_role_campaign_audiences ON campaign_audiences
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 3. Table: campaign_messages
CREATE TABLE IF NOT EXISTS campaign_messages (
    id VARCHAR(255) PRIMARY KEY,
    campaign_id VARCHAR(255) NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    channel_type VARCHAR(50) NOT NULL,
    message_template TEXT NOT NULL,
    generated_copy TEXT NOT NULL,
    media_url TEXT,
    cta_button_text VARCHAR(255),
    cta_button_url TEXT,
    risk_check_result_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_approved BOOLEAN NOT NULL DEFAULT true,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_campaign_messages_campaign ON campaign_messages(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_messages_tenant ON campaign_messages(tenant_id);
CREATE INDEX IF NOT EXISTS idx_campaign_messages_channel ON campaign_messages(channel_type);

ALTER TABLE campaign_messages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_campaign_messages ON campaign_messages;
CREATE POLICY tenant_isolation_campaign_messages ON campaign_messages
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_campaign_messages ON campaign_messages;
CREATE POLICY service_role_campaign_messages ON campaign_messages
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 4. Table: campaign_sends
CREATE TABLE IF NOT EXISTS campaign_sends (
    id VARCHAR(255) PRIMARY KEY,
    campaign_id VARCHAR(255) NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    audience_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    channel_type VARCHAR(50) NOT NULL,
    channel_account_id VARCHAR(255),
    destination VARCHAR(255) NOT NULL,
    sent_message_text TEXT NOT NULL,
    external_message_id VARCHAR(255),
    send_status VARCHAR(50) NOT NULL DEFAULT 'SENT',
    error_message TEXT,
    sent_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    delivered_at BIGINT,
    read_at BIGINT,
    converted_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_campaign_sends_campaign ON campaign_sends(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_sends_tenant ON campaign_sends(tenant_id);
CREATE INDEX IF NOT EXISTS idx_campaign_sends_audience ON campaign_sends(audience_id);
CREATE INDEX IF NOT EXISTS idx_campaign_sends_customer ON campaign_sends(customer_id);
CREATE INDEX IF NOT EXISTS idx_campaign_sends_status ON campaign_sends(send_status);
CREATE INDEX IF NOT EXISTS idx_campaign_sends_sent_at ON campaign_sends(sent_at);

ALTER TABLE campaign_sends ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_campaign_sends ON campaign_sends;
CREATE POLICY tenant_isolation_campaign_sends ON campaign_sends
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_campaign_sends ON campaign_sends;
CREATE POLICY service_role_campaign_sends ON campaign_sends
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 5. Table: service_requests
CREATE TABLE IF NOT EXISTS service_requests (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    ticket_number VARCHAR(100) NOT NULL UNIQUE,
    customer_id VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_contact VARCHAR(255) NOT NULL,
    order_id VARCHAR(255),
    order_number VARCHAR(100),
    conversation_id VARCHAR(255),
    channel_type VARCHAR(50) NOT NULL DEFAULT 'WHATSAPP',
    request_type VARCHAR(100) NOT NULL,
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    requested_amount NUMERIC(15, 2),
    approved_amount NUMERIC(15, 2),
    reason_code VARCHAR(100),
    resolution_notes TEXT,
    assigned_staff_id VARCHAR(255),
    assigned_staff_name VARCHAR(255),
    handled_by_ai BOOLEAN NOT NULL DEFAULT true,
    human_approved_by VARCHAR(255),
    human_approved_at BIGINT,
    resolved_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_service_requests_tenant ON service_requests(tenant_id);
CREATE INDEX IF NOT EXISTS idx_service_requests_ticket ON service_requests(ticket_number);
CREATE INDEX IF NOT EXISTS idx_service_requests_customer ON service_requests(customer_id);
CREATE INDEX IF NOT EXISTS idx_service_requests_order ON service_requests(order_id);
CREATE INDEX IF NOT EXISTS idx_service_requests_conversation ON service_requests(conversation_id);
CREATE INDEX IF NOT EXISTS idx_service_requests_type ON service_requests(request_type);
CREATE INDEX IF NOT EXISTS idx_service_requests_status ON service_requests(status);
CREATE INDEX IF NOT EXISTS idx_service_requests_priority ON service_requests(priority);
CREATE INDEX IF NOT EXISTS idx_service_requests_created_at ON service_requests(created_at);

ALTER TABLE service_requests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_service_requests ON service_requests;
CREATE POLICY tenant_isolation_service_requests ON service_requests
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_service_requests ON service_requests;
CREATE POLICY service_role_service_requests ON service_requests
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 6. Table: service_request_attachments
CREATE TABLE IF NOT EXISTS service_request_attachments (
    id VARCHAR(255) PRIMARY KEY,
    service_request_id VARCHAR(255) NOT NULL REFERENCES service_requests(id) ON DELETE CASCADE,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    file_url TEXT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL DEFAULT 'IMAGE',
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    uploaded_by VARCHAR(100) NOT NULL DEFAULT 'CUSTOMER',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_sr_attachments_req ON service_request_attachments(service_request_id);
CREATE INDEX IF NOT EXISTS idx_sr_attachments_tenant ON service_request_attachments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sr_attachments_type ON service_request_attachments(file_type);

ALTER TABLE service_request_attachments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_service_request_attachments ON service_request_attachments;
CREATE POLICY tenant_isolation_service_request_attachments ON service_request_attachments
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_service_request_attachments ON service_request_attachments;
CREATE POLICY service_role_service_request_attachments ON service_request_attachments
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);
