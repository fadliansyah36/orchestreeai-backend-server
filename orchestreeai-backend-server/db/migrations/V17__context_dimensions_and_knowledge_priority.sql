-- ====================================================================
-- OrchestreeAI Database Migration V17: 8 Context Dimensions & Knowledge Priority Hierarchy
-- PRD Addendum 2 Bagian 63-64: 8 Company Context Dimensions & 6-Tier Knowledge Hierarchy
-- ====================================================================

-- 1. Extend memory_documents with context_dimension, entity_reference, and metadata_json
ALTER TABLE memory_documents 
    ADD COLUMN IF NOT EXISTS context_dimension VARCHAR(32),
    ADD COLUMN IF NOT EXISTS entity_reference VARCHAR(128),
    ADD COLUMN IF NOT EXISTS metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE INDEX IF NOT EXISTS idx_memory_docs_dimension ON memory_documents(tenant_id, context_dimension);
CREATE INDEX IF NOT EXISTS idx_memory_docs_entity_ref ON memory_documents(tenant_id, entity_reference);

-- 2. Ensure memory_embeddings table exists (PRD Section 16.3) and extend with context_dimension
CREATE TABLE IF NOT EXISTS memory_embeddings (
    id VARCHAR(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    document_id VARCHAR(64) REFERENCES memory_documents(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL,
    embedding VECTOR(1536),
    model_used VARCHAR(64) NOT NULL DEFAULT 'text-embedding-3-small',
    context_dimension VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE memory_embeddings
    ADD COLUMN IF NOT EXISTS context_dimension VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_memory_emb_dimension ON memory_embeddings(tenant_id, context_dimension);

-- 3. Extend tenants table with allow_public_web_research (PRD Section 64.2 Level 6 Web Gate)
ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS allow_public_web_research BOOLEAN NOT NULL DEFAULT FALSE;

-- 4. Approved External Sources (PRD Section 64.1 & 64.2 Level 5)
CREATE TABLE IF NOT EXISTS approved_external_sources (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    source_name VARCHAR(255) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL, -- 'MANUFACTURER', 'REGULATORY', 'INDUSTRY_STANDARD', 'VENDOR_PORTAL', 'TECHNICAL_MANUAL'
    base_url TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    trust_score DOUBLE PRECISION NOT NULL DEFAULT 0.95,
    auth_config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approved_sources_tenant ON approved_external_sources(tenant_id, is_active);
CREATE INDEX IF NOT EXISTS idx_approved_sources_domain ON approved_external_sources(tenant_id, domain);

-- 5. Seed Official Taxonomy for Approved External Sources (Permitted platform configuration)
INSERT INTO tenants (id, name, domain, tier, status)
VALUES ('tenant-nusantara', 'Nusantara Resources Enterprise', 'nusantara.orchestree.ai', 'ENTERPRISE', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO approved_external_sources (id, tenant_id, source_name, domain, category, base_url, description, is_active, trust_score, created_at, updated_at)
VALUES 
('src-komatsu-01', 'tenant-nusantara', 'Komatsu Official Technical Manuals', 'manuals.komatsu.com', 'MANUFACTURER', 'https://manuals.komatsu.com/heavy-equipment/pc200', 'Dokumentasi resmi shop manual & spesifikasi excavator PC200', TRUE, 0.98, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('src-cat-01', 'tenant-nusantara', 'Caterpillar Operation & Maintenance (OMM)', 'cat.com', 'MANUFACTURER', 'https://www.cat.com/en_US/support/operations.html', 'Panduan operasional dan interval perawatan armada alat berat Cat', TRUE, 0.96, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('src-esdm-01', 'tenant-nusantara', 'Kementerian ESDM - Kepmen Minerba 1827 K/30/MEM/2018', 'minerba.esdm.go.id', 'REGULATORY', 'https://minerba.esdm.go.id/regulasi/kaidah-teknik-pertambangan', 'Pedoman pelaksanaan kaidah teknik pertambangan yang baik dan keselamatan operasi', TRUE, 1.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('src-iso-01', 'tenant-nusantara', 'ISO 55001 Asset Management Standards', 'iso.org', 'INDUSTRY_STANDARD', 'https://www.iso.org/standard/55001.html', 'Standar internasional sistem manajemen aset industri dan keandalan operasional', TRUE, 0.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('src-cummins-01', 'tenant-nusantara', 'Cummins Engine Technical Service Portal (QSK Series)', 'quickserve.cummins.com', 'MANUFACTURER', 'https://quickserve.cummins.com/info/qsk60', 'Spesifikasi kalibrasi pompa bahan bakar dan diagnostic fault codes mesin genset', TRUE, 0.97, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
