-- ====================================================================
-- OrchestreeAI Database Migration V19: Multi-Provider Creative Studio & Iterative Refinement Engine
-- Architecture: PRD Master Section 23 & Addendum Multi-Provider Image Generation
-- ====================================================================

-- 1. Image Provider Registry (Super Admin Platform-Level & Tenant Level)
CREATE TABLE IF NOT EXISTS image_provider_registry (
    id VARCHAR(64) PRIMARY KEY,
    provider_code VARCHAR(64) NOT NULL UNIQUE,
    provider_name VARCHAR(128) NOT NULL,
    api_base_url TEXT NOT NULL,
    api_key_env VARCHAR(128) NOT NULL DEFAULT '',
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    supports_reference_image BOOLEAN NOT NULL DEFAULT TRUE,
    supports_text_rendering BOOLEAN NOT NULL DEFAULT TRUE,
    default_model VARCHAR(128) NOT NULL,
    latency_ms BIGINT NOT NULL DEFAULT 1200,
    cost_per_image_usd DOUBLE PRECISION NOT NULL DEFAULT 0.04,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_img_provider_enabled ON image_provider_registry(is_enabled);

-- 2. Creative Generation Requests Table
CREATE TABLE IF NOT EXISTS creative_generation_requests (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    task_id VARCHAR(64),
    workflow_execution_id VARCHAR(64),
    user_brief TEXT NOT NULL,
    composed_prompt TEXT NOT NULL,
    brand_guideline_id VARCHAR(64),
    brand_guideline_name VARCHAR(128) NOT NULL DEFAULT 'Official Brand',
    platform_preset VARCHAR(128) NOT NULL DEFAULT 'Instagram Feed (1:1)',
    aspect_ratio VARCHAR(32) NOT NULL DEFAULT '1:1',
    resolution VARCHAR(32) NOT NULL DEFAULT '1080x1080',
    max_iterations INT NOT NULL DEFAULT 3,
    current_iteration INT NOT NULL DEFAULT 1,
    status VARCHAR(64) NOT NULL DEFAULT 'IN_PROGRESS', -- 'IN_PROGRESS', 'COMPLETED', 'MAX_ITERATION_REACHED', 'FAILED'
    selected_candidate_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_creative_req_tenant ON creative_generation_requests(tenant_id);
CREATE INDEX IF NOT EXISTS idx_creative_req_status ON creative_generation_requests(status);

-- 3. Creative Generation Candidates Table (Parallel Multi-Provider Outputs)
CREATE TABLE IF NOT EXISTS creative_generation_candidates (
    id VARCHAR(64) PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL REFERENCES creative_generation_requests(id) ON DELETE CASCADE,
    iteration_number INT NOT NULL DEFAULT 1,
    provider_code VARCHAR(64) NOT NULL,
    provider_name VARCHAR(128) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    label VARCHAR(64) NOT NULL, -- e.g. 'Opsi A (Google Imagen 3)', 'Opsi B (OpenAI DALL-E 3)'
    image_url TEXT NOT NULL,
    prompt_used TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 1048576,
    mime_type VARCHAR(64) NOT NULL DEFAULT 'image/png',
    generation_time_ms BIGINT NOT NULL DEFAULT 1200,
    generation_start_timestamp BIGINT NOT NULL,
    generation_end_timestamp BIGINT NOT NULL,
    validation_status VARCHAR(32) NOT NULL DEFAULT 'PASSED', -- 'PASSED', 'FLAGGED', 'REJECTED'
    validation_score DOUBLE PRECISION NOT NULL DEFAULT 95.0,
    brand_palette_adherence_pct DOUBLE PRECISION NOT NULL DEFAULT 94.0,
    legal_risk_score DOUBLE PRECISION NOT NULL DEFAULT 0.02,
    rejected_reason TEXT,
    c2pa_metadata_json TEXT NOT NULL DEFAULT '{}',
    is_selected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_creative_cand_req ON creative_generation_candidates(request_id);
CREATE INDEX IF NOT EXISTS idx_creative_cand_iter ON creative_generation_candidates(iteration_number);
CREATE INDEX IF NOT EXISTS idx_creative_cand_selected ON creative_generation_candidates(is_selected);

-- 4. Creative Refinement Iterations Table
CREATE TABLE IF NOT EXISTS creative_refinement_iterations (
    id VARCHAR(64) PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL REFERENCES creative_generation_requests(id) ON DELETE CASCADE,
    iteration_number INT NOT NULL,
    parent_candidate_id VARCHAR(64) NOT NULL,
    feedback_notes TEXT NOT NULL,
    refined_prompt TEXT NOT NULL,
    actor_name VARCHAR(128) NOT NULL DEFAULT 'User / Content Lead',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_creative_refine_req ON creative_refinement_iterations(request_id);

-- 5. Seed Official Image Generation Providers
INSERT INTO image_provider_registry (
    id, provider_code, provider_name, api_base_url, api_key_env, is_enabled,
    supports_reference_image, supports_text_rendering, default_model, latency_ms, cost_per_image_usd
) VALUES 
(
    'img-prov-google-imagen',
    'GOOGLE_IMAGEN',
    'Google Imagen 3',
    'https://generativelanguage.googleapis.com',
    'GEMINI_API_KEY',
    TRUE,
    TRUE,
    TRUE,
    'imagen-3.0-generate-002',
    1150,
    0.030
),
(
    'img-prov-openai-dalle',
    'OPENAI_DALLE',
    'OpenAI DALL-E 3',
    'https://api.openai.com/v1',
    'OPENAI_API_KEY',
    TRUE,
    FALSE,
    TRUE,
    'dall-e-3',
    1380,
    0.040
),
(
    'img-prov-stability-ultra',
    'STABILITY_AI',
    'Stability Ultra AI',
    'https://api.stability.ai/v2beta/stable-image/generate/ultra',
    'STABILITY_API_KEY',
    TRUE,
    TRUE,
    FALSE,
    'stable-image-ultra',
    1050,
    0.035
)
ON CONFLICT (provider_code) DO NOTHING;
