-- Migration: V118__nvidia_nim_dynamic_catalog_and_disable_anthropic_deepseek.sql
-- Description: Implement dynamic model catalog for NVIDIA NIM & OpenRouter, disable Anthropic and DeepSeek providers

-- 1. Table: llm_provider_models
CREATE TABLE IF NOT EXISTS llm_provider_models (
    id VARCHAR(255) PRIMARY KEY,
    provider_id VARCHAR(255) REFERENCES llm_providers(id) ON DELETE CASCADE,
    provider_code VARCHAR(100) NOT NULL,
    model_identifier TEXT NOT NULL,
    complexity_tier VARCHAR(50) NOT NULL CHECK (complexity_tier IN ('trivial','simple','moderate','complex','frontier')),
    context_window INT DEFAULT 131072,
    supports_tool_calling BOOLEAN DEFAULT TRUE,
    supports_vision BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    last_verified_at TIMESTAMPTZ DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_llm_models_provider ON llm_provider_models(provider_id);
CREATE INDEX IF NOT EXISTS idx_llm_models_prov_code ON llm_provider_models(provider_code);
CREATE INDEX IF NOT EXISTS idx_llm_models_tier ON llm_provider_models(complexity_tier);
CREATE INDEX IF NOT EXISTS idx_llm_models_active ON llm_provider_models(is_active);

ALTER TABLE llm_provider_models ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS select_llm_provider_models ON llm_provider_models;
CREATE POLICY select_llm_provider_models ON llm_provider_models
    FOR SELECT USING (true);

DROP POLICY IF EXISTS service_role_llm_provider_models ON llm_provider_models;
CREATE POLICY service_role_llm_provider_models ON llm_provider_models
    FOR ALL TO service_role USING (true) WITH CHECK (true);

-- 2. Deactivate Anthropic and DeepSeek in llm_providers
UPDATE llm_providers 
SET is_enabled = false, 
    health_status = 'disabled' 
WHERE UPPER(provider_code) IN ('ANTHROPIC', 'DEEPSEEK');

-- 3. Upsert NVIDIA NIM provider (Priority 1)
INSERT INTO llm_providers (
    id, name, provider_code, api_base_url, api_key_env, priority, fallback_priority,
    task_specialization, is_enabled, latency_ms, error_rate_pct, health_status
) VALUES (
    'llm-nvidia-nim',
    'NVIDIA NIM Microservices Gateway',
    'NVIDIA_NIM',
    'https://integrate.api.nvidia.com/v1',
    'NVIDIA_API_KEY',
    1,
    1,
    'frontier_reasoning,complex_analysis,code_generation,fast_inference,multimodal',
    true,
    95,
    0.05,
    'healthy'
) ON CONFLICT (provider_code) DO UPDATE SET
    api_base_url = 'https://integrate.api.nvidia.com/v1',
    api_key_env = 'NVIDIA_API_KEY',
    priority = 1,
    fallback_priority = 1,
    is_enabled = true,
    health_status = 'healthy';

-- 4. Re-align fallback priorities: OpenRouter as Tier 2, Groq as Tier 3
UPDATE llm_providers 
SET priority = 2, fallback_priority = 2 
WHERE UPPER(provider_code) = 'OPENROUTER';

UPDATE llm_providers 
SET priority = 3, fallback_priority = 3 
WHERE UPPER(provider_code) = 'GROQ';
