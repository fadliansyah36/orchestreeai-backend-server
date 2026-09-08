-- ==============================================================================
-- OrchestreeAI Migration: Tahap 3 — Conversation & Channel Sub-system
-- File: V24__tahap3_conversation_and_channel_subsystem.sql
-- PRD Source: Master Bagian 16/18, Addendum 1 Bagian 36, Fase 66.5
-- Deskripsi: Skema conversation rolling memory, persona handoff rules, dan live chat messages.
-- ==============================================================================

-- 1. Conversation Rolling Summary (Rolling Memory Engine - PRD Fase 66.5)
CREATE TABLE IF NOT EXISTS conversation_rolling_summary (
    id TEXT PRIMARY KEY,
    conversation_id TEXT NOT NULL,
    summary_text TEXT NOT NULL,
    summarized_up_to_message_id TEXT NOT NULL,
    total_messages_summarized INT NOT NULL DEFAULT 20,
    token_count_saved INT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_rolling_summary_conversation FOREIGN KEY (conversation_id)
        REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_rolling_summary_conv_id ON conversation_rolling_summary(conversation_id);

-- 2. Persona Handoff Rules (Dynamic Commercial & Service Persona Switcher - PRD Addendum 1 Bagian 36)
CREATE TABLE IF NOT EXISTS persona_handoff_rules (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    from_persona TEXT NOT NULL,
    to_persona TEXT NOT NULL,
    trigger_intent TEXT NOT NULL,
    condition_expression TEXT NOT NULL DEFAULT '',
    confidence_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.70,
    priority INT NOT NULL DEFAULT 1,
    reason_template TEXT NOT NULL DEFAULT 'Intent komersial terdeteksi: {intent}',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_persona_handoff_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_persona_handoff_tenant_id ON persona_handoff_rules(tenant_id);
CREATE INDEX IF NOT EXISTS idx_persona_handoff_personas ON persona_handoff_rules(from_persona, to_persona);
CREATE INDEX IF NOT EXISTS idx_persona_handoff_intent ON persona_handoff_rules(trigger_intent);

-- 3. Chat Messages (Omnichannel Mirror & Live In-App Chat Stream)
CREATE TABLE IF NOT EXISTS chat_messages (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    sender_type TEXT NOT NULL, -- 'USER', 'AGENT', 'SYSTEM'
    sender_name TEXT NOT NULL,
    message TEXT NOT NULL,
    channel TEXT NOT NULL DEFAULT 'WEB', -- 'WEB', 'WHATSAPP', 'TELEGRAM', 'INSTAGRAM'
    thread_id TEXT NOT NULL DEFAULT 'default-thread',
    media_url TEXT,
    artifact_type TEXT, -- 'IMAGE', 'TASK', 'DOCUMENT'
    artifact_id TEXT,
    workflow_execution_id TEXT,
    task_id TEXT,
    status TEXT NOT NULL DEFAULT 'SENT', -- 'SENT', 'STREAMING', 'COMPLETED', 'FAILED'
    timestamp BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_chat_messages_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_tenant_id ON chat_messages(tenant_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_thread_id ON chat_messages(thread_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_timestamp ON chat_messages(timestamp);

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================
ALTER TABLE conversation_rolling_summary ENABLE ROW LEVEL SECURITY;
ALTER TABLE persona_handoff_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_messages ENABLE ROW LEVEL SECURITY;

-- 1. Policies for conversation_rolling_summary (tied to parent conversation tenant)
DROP POLICY IF EXISTS tenant_isolation_rolling_summary ON conversation_rolling_summary;
CREATE POLICY tenant_isolation_rolling_summary ON conversation_rolling_summary
    FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = conversation_rolling_summary.conversation_id
            AND app_has_tenant_access(c.tenant_id)
        )
        OR auth.role() = 'service_role'
    );

-- 2. Policies for persona_handoff_rules
DROP POLICY IF EXISTS tenant_isolation_persona_handoff ON persona_handoff_rules;
CREATE POLICY tenant_isolation_persona_handoff ON persona_handoff_rules
    FOR ALL
    USING (
        app_has_tenant_access(tenant_id)
        OR auth.role() = 'service_role'
    );

-- 3. Policies for chat_messages
DROP POLICY IF EXISTS tenant_isolation_chat_messages ON chat_messages;
CREATE POLICY tenant_isolation_chat_messages ON chat_messages
    FOR ALL
    USING (
        app_has_tenant_access(tenant_id)
        OR auth.role() = 'service_role'
    );
