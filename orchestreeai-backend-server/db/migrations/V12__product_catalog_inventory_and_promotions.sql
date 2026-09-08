-- ====================================================================
-- OrchestreeAI Database Migration V12: Product Catalog, Inventory, Relations & Promotions
-- PRD Addendum Section 39.1 & Section 38 (Real Product Catalog & Sales Stage Foundation)
-- ====================================================================

-- 1. Products Table (PRD Section 39.1)
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(128) NOT NULL,
    category VARCHAR(128) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    base_price NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(16) NOT NULL DEFAULT 'IDR',
    unit VARCHAR(64) NOT NULL DEFAULT 'pcs',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    attributes_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    image_url TEXT,
    tags TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_product_sku UNIQUE (tenant_id, sku)
);

CREATE INDEX IF NOT EXISTS idx_products_tenant_cat ON products(tenant_id, category, is_active);
CREATE INDEX IF NOT EXISTS idx_products_tenant_sku ON products(tenant_id, sku);

-- 2. Product Variants Table
CREATE TABLE IF NOT EXISTS product_variants (
    id VARCHAR(64) PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    variant_name VARCHAR(255) NOT NULL,
    sku VARCHAR(128) NOT NULL,
    price NUMERIC(14, 2) NOT NULL,
    attributes_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_variant_sku UNIQUE (tenant_id, sku)
);

CREATE INDEX IF NOT EXISTS idx_variants_product ON product_variants(product_id, is_active);

-- 3. Inventory Stock Table
CREATE TABLE IF NOT EXISTS inventory_stock (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id VARCHAR(64) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_id VARCHAR(64) REFERENCES product_variants(id) ON DELETE SET NULL,
    warehouse_location VARCHAR(128) NOT NULL DEFAULT 'MAIN_WAREHOUSE',
    available_stock INT NOT NULL DEFAULT 0,
    reserved_stock INT NOT NULL DEFAULT 0,
    safety_stock_threshold INT NOT NULL DEFAULT 5,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_product_variant_warehouse UNIQUE (tenant_id, product_id, variant_id, warehouse_location)
);

CREATE INDEX IF NOT EXISTS idx_inventory_product ON inventory_stock(tenant_id, product_id, available_stock);

-- 4. Product Relations Table (Cross-sell, Upsell, Bundles)
CREATE TABLE IF NOT EXISTS product_relations (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    source_product_id VARCHAR(64) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    target_product_id VARCHAR(64) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    relation_type VARCHAR(64) NOT NULL, -- UPSELL, COMPLEMENTARY, BUNDLE, ALTERNATIVE
    score NUMERIC(4, 3) NOT NULL DEFAULT 1.000,
    discount_pct NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_relation UNIQUE (tenant_id, source_product_id, target_product_id, relation_type)
);

CREATE INDEX IF NOT EXISTS idx_prod_relations_source ON product_relations(tenant_id, source_product_id, relation_type);

-- 5. Promotions Table (Guardrails & Discount Thresholds)
CREATE TABLE IF NOT EXISTS promotions (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    code VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    discount_type VARCHAR(32) NOT NULL, -- PERCENTAGE, FIXED_AMOUNT
    discount_value NUMERIC(14, 2) NOT NULL,
    max_discount_amount NUMERIC(14, 2),
    min_purchase_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    max_discount_cap_pct NUMERIC(5, 2) NOT NULL DEFAULT 20.00,
    requires_approval_above_pct NUMERIC(5, 2) NOT NULL DEFAULT 15.00,
    applicable_product_ids TEXT,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_promo_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_promotions_tenant ON promotions(tenant_id, is_active, start_date, end_date);

-- Multi-Tenant Row Level Security (RLS)
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS products_tenant_isolation ON products;
CREATE POLICY products_tenant_isolation ON products
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE product_variants ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS product_variants_tenant_isolation ON product_variants;
CREATE POLICY product_variants_tenant_isolation ON product_variants
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE inventory_stock ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS inventory_stock_tenant_isolation ON inventory_stock;
CREATE POLICY inventory_stock_tenant_isolation ON inventory_stock
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE product_relations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS product_relations_tenant_isolation ON product_relations;
CREATE POLICY product_relations_tenant_isolation ON product_relations
    USING (tenant_id = current_setting('app.current_tenant_id', true));

ALTER TABLE promotions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS promotions_tenant_isolation ON promotions;
CREATE POLICY promotions_tenant_isolation ON promotions
    USING (tenant_id = current_setting('app.current_tenant_id', true));
