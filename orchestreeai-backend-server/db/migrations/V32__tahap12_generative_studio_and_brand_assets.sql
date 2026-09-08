-- V32__tahap12_generative_studio_and_brand_assets.sql
-- PRD Generative Studio & Addendum 2 Bagian 73.2/76.3: Creative Assets & Brand Style Overlays

-- 1. Table: generation_requests
CREATE TABLE IF NOT EXISTS generation_requests (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    requested_by_user_id VARCHAR(255),
    prompt_text TEXT NOT NULL,
    attached_reference_files JSONB NOT NULL DEFAULT '[]'::jsonb,
    output_format VARCHAR(50) NOT NULL,
    use_case_category VARCHAR(100) NOT NULL DEFAULT 'general',
    status VARCHAR(50) NOT NULL DEFAULT 'planning',
    content_plan_json JSONB,
    max_iterations INT NOT NULL DEFAULT 3,
    current_iteration INT NOT NULL DEFAULT 0,
    source_channel VARCHAR(50) NOT NULL DEFAULT 'app',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_generation_requests_tenant ON generation_requests(tenant_id);
CREATE INDEX IF NOT EXISTS idx_generation_requests_status ON generation_requests(status);
CREATE INDEX IF NOT EXISTS idx_generation_requests_format ON generation_requests(output_format);
CREATE INDEX IF NOT EXISTS idx_generation_requests_created_at ON generation_requests(created_at);

ALTER TABLE generation_requests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_generation_requests ON generation_requests;
CREATE POLICY tenant_isolation_generation_requests ON generation_requests
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_generation_requests ON generation_requests;
CREATE POLICY service_role_generation_requests ON generation_requests
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 2. Table: generation_outputs
CREATE TABLE IF NOT EXISTS generation_outputs (
    id VARCHAR(255) PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL REFERENCES generation_requests(id) ON DELETE CASCADE,
    file_url TEXT NOT NULL,
    file_format VARCHAR(50) NOT NULL,
    version_number INT NOT NULL DEFAULT 1,
    generated_by_renderer VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL DEFAULT '',
    preview_data_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    file_size_bytes BIGINT NOT NULL DEFAULT 1024,
    is_approved BOOLEAN NOT NULL DEFAULT false,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_generation_outputs_request ON generation_outputs(request_id);
CREATE INDEX IF NOT EXISTS idx_generation_outputs_format ON generation_outputs(file_format);
CREATE INDEX IF NOT EXISTS idx_generation_outputs_created_at ON generation_outputs(created_at);

ALTER TABLE generation_outputs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_generation_outputs ON generation_outputs;
CREATE POLICY tenant_isolation_generation_outputs ON generation_outputs
    FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM generation_requests gr
            WHERE gr.id = generation_outputs.request_id
            AND app_has_tenant_access(gr.tenant_id)
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM generation_requests gr
            WHERE gr.id = generation_outputs.request_id
            AND app_has_tenant_access(gr.tenant_id)
        )
    );

DROP POLICY IF EXISTS service_role_generation_outputs ON generation_outputs;
CREATE POLICY service_role_generation_outputs ON generation_outputs
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 3. Table: brand_style_references
CREATE TABLE IF NOT EXISTS brand_style_references (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    reference_image_url TEXT NOT NULL,
    style_notes TEXT NOT NULL DEFAULT '',
    weight REAL NOT NULL DEFAULT 0.85,
    is_locked BOOLEAN NOT NULL DEFAULT true,
    category VARCHAR(100) NOT NULL DEFAULT 'GENERAL',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_brand_style_refs_tenant ON brand_style_references(tenant_id);
CREATE INDEX IF NOT EXISTS idx_brand_style_refs_locked ON brand_style_references(is_locked);

ALTER TABLE brand_style_references ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_brand_style_references ON brand_style_references;
CREATE POLICY tenant_isolation_brand_style_references ON brand_style_references
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_brand_style_references ON brand_style_references;
CREATE POLICY service_role_brand_style_references ON brand_style_references
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- 4. Table: brand_asset_overlays
CREATE TABLE IF NOT EXISTS brand_asset_overlays (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    asset_name VARCHAR(255) NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    asset_file_url TEXT NOT NULL,
    default_position VARCHAR(50) NOT NULL DEFAULT 'TOP_RIGHT',
    custom_pos_x_percent REAL NOT NULL DEFAULT 0.85,
    custom_pos_y_percent REAL NOT NULL DEFAULT 0.05,
    target_scale_percent REAL NOT NULL DEFAULT 0.15,
    opacity REAL NOT NULL DEFAULT 1.0,
    blend_mode VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    min_margin_px INT NOT NULL DEFAULT 32,
    is_default_active BOOLEAN NOT NULL DEFAULT true,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW())*1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_brand_asset_overlays_tenant ON brand_asset_overlays(tenant_id);
CREATE INDEX IF NOT EXISTS idx_brand_asset_overlays_active ON brand_asset_overlays(is_default_active);
CREATE INDEX IF NOT EXISTS idx_brand_asset_overlays_type ON brand_asset_overlays(asset_type);

ALTER TABLE brand_asset_overlays ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_brand_asset_overlays ON brand_asset_overlays;
CREATE POLICY tenant_isolation_brand_asset_overlays ON brand_asset_overlays
    FOR ALL
    USING (app_has_tenant_access(tenant_id))
    WITH CHECK (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS service_role_brand_asset_overlays ON brand_asset_overlays;
CREATE POLICY service_role_brand_asset_overlays ON brand_asset_overlays
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);
