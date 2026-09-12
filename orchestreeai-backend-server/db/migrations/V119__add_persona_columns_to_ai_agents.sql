-- V119: Omnichannel AI Persona columns and indexing alignment
ALTER TABLE ai_agents ADD COLUMN IF NOT EXISTS persona_type TEXT;
ALTER TABLE ai_agents ADD COLUMN IF NOT EXISTS persona_config JSONB DEFAULT '{}';

CREATE INDEX IF NOT EXISTS idx_ai_agents_persona_type ON ai_agents(tenant_id, persona_type);
CREATE INDEX IF NOT EXISTS idx_conversations_persona ON conversations(tenant_id, assigned_persona);
