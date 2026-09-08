-- ====================================================================
-- OrchestreeAI Database Migration V13: Feature Tiering & Gating Architecture
-- PRD Addendum 2 Bagian 57: Feature Capability Matrix, Gating & Downgrade Handling
-- ====================================================================

-- 1. Subscription Plans Table (PRD Section 57.3)
CREATE TABLE IF NOT EXISTS subscription_plans (
    id VARCHAR(64) PRIMARY KEY,
    plan_code VARCHAR(64) UNIQUE,
    tier_level INT DEFAULT 1,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    price_monthly_idr NUMERIC(14, 2) NOT NULL DEFAULT 0,
    max_users INT NOT NULL DEFAULT 10,
    max_agents INT NOT NULL DEFAULT 5,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS plan_code VARCHAR(64);
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS tier_level INT DEFAULT 1;
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS tier VARCHAR(32) DEFAULT 'STARTER';
ALTER TABLE subscription_plans ALTER COLUMN tier DROP NOT NULL;
ALTER TABLE subscription_plans ALTER COLUMN price_idr_monthly DROP NOT NULL;
ALTER TABLE subscription_plans ALTER COLUMN price_idr_yearly DROP NOT NULL;
ALTER TABLE subscription_plans ALTER COLUMN max_human_seats DROP NOT NULL;
ALTER TABLE subscription_plans ALTER COLUMN monthly_llm_token_limit DROP NOT NULL;
ALTER TABLE subscription_plans ALTER COLUMN features_json DROP NOT NULL;
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS price_monthly_idr NUMERIC(14, 2) DEFAULT 0;
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS max_users INT DEFAULT 10;
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS max_agents INT DEFAULT 5;
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'subscription_plans_plan_code_key') THEN
        BEGIN
            ALTER TABLE subscription_plans ADD CONSTRAINT subscription_plans_plan_code_key UNIQUE (plan_code);
        EXCEPTION WHEN others THEN NULL;
        END;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_subscription_plans_tier ON subscription_plans(tier_level);

-- 2. Feature Capabilities Table (PRD Section 57.2 & 57.3)
CREATE TABLE IF NOT EXISTS feature_capabilities (
    id VARCHAR(64) PRIMARY KEY,
    capability_key VARCHAR(128) NOT NULL UNIQUE,
    min_tier_level INT NOT NULL,          -- 1: All-Tier, 2: Growth+, 3: Enterprise-only, 4: Custom
    description TEXT NOT NULL DEFAULT '',
    category VARCHAR(64) NOT NULL DEFAULT 'ALL_TIER', -- 'ALL_TIER', 'ENTERPRISE_ONLY'
    is_system_core BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feature_capabilities_tier ON feature_capabilities(min_tier_level);

-- 3. Tenant Capability Overrides Table (PRD Section 57.3)
CREATE TABLE IF NOT EXISTS tenant_capability_overrides (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    capability_key VARCHAR(128) NOT NULL REFERENCES feature_capabilities(capability_key) ON DELETE CASCADE,
    enabled_override BOOLEAN,            -- NULL: follow plan, TRUE: whitelist/trial, FALSE: force disable
    reason TEXT NOT NULL DEFAULT '',
    set_by VARCHAR(128) NOT NULL DEFAULT 'Super Admin',
    expires_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_capability UNIQUE (tenant_id, capability_key)
);

CREATE INDEX IF NOT EXISTS idx_tenant_capability_overrides ON tenant_capability_overrides(tenant_id, capability_key);

-- 4. Seed Standard Subscription Plans
INSERT INTO subscription_plans (id, plan_code, tier_level, tier, name, description, price_monthly_idr, max_users, max_agents)
VALUES
    ('plan-starter', 'starter', 1, 'STARTER', 'Starter Plan', 'Akses operasional esensial dengan AI Agent bawaan untuk bisnis berkembang.', 999000.00, 5, 2),
    ('plan-growth', 'growth', 2, 'GROWTH', 'Growth Plan', 'Otomasi alur kerja AI, multi-channel sales marketing, dan analitik performa kolaboratif.', 2999000.00, 25, 10),
    ('plan-enterprise', 'enterprise', 3, 'ENTERPRISE', 'Enterprise Plan', 'Enterprise AI Workforce Operating System lengkap dengan Third-Party Integration Fabric, AI Chief of Staff & Cross-System Intelligence.', 7999000.00, 100, 50),
    ('plan-custom', 'custom', 4, 'CUSTOM', 'Custom Enterprise Plan', 'Solusi enterprise dedicated dengan integrasi on-premise, SLA terjamin, dan kapasitas nirbatas.', 15999000.00, 9999, 9999)
ON CONFLICT (id) DO UPDATE SET
    plan_code = EXCLUDED.plan_code,
    tier_level = EXCLUDED.tier_level,
    tier = EXCLUDED.tier,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    price_monthly_idr = EXCLUDED.price_monthly_idr,
    max_users = EXCLUDED.max_users,
    max_agents = EXCLUDED.max_agents;

-- 5. Seed Feature Capability Matrix (PRD Table 57.2)
INSERT INTO feature_capabilities (id, capability_key, min_tier_level, description, category, is_system_core)
VALUES
    -- All-Tier Capabilities (min_tier_level = 1)
    ('cap-exec-layer', 'ai_execution_layer', 1, 'Eksekusi alur kerja dan tindakan AI dengan verifikasi persetujuan manusia.', 'ALL_TIER', TRUE),
    ('cap-act-orch', 'ai_action_orchestration', 1, 'Orkestrasi alur kerja multi-langkah dan integrasi internal OrchestreeAI.', 'ALL_TIER', TRUE),
    ('cap-mon-loop', 'ai_monitoring_loop', 1, 'Closed-Loop Workforce Monitoring dari deteksi hingga verifikasi penyelesaian objektif.', 'ALL_TIER', TRUE),
    ('cap-learn-core', 'continuous_learning_core', 1, 'Closed-loop Continuous Learning Core untuk peningkatan skill AI berbasis hasil terverifikasi.', 'ALL_TIER', TRUE),
    ('cap-fin-intel', 'ai_finance_intelligence', 1, 'Analitik tekanan arus kas, umur piutang/utang (AR/AP), dan peramalan keuangan data internal.', 'ALL_TIER', TRUE),
    ('cap-know-fusion', 'ai_knowledge_operational_fusion', 1, 'Penyatuan dinamis SOP Company Brain dengan metrik operasional internal.', 'ALL_TIER', TRUE),
    ('cap-event-eng', 'ai_event_engine', 1, 'Event-driven AI dispatcher untuk aktivasi proaktif agen spesialis sesuai kejadian.', 'ALL_TIER', TRUE),

    -- Enterprise-Only Capabilities (min_tier_level = 3)
    ('cap-int-fabric', 'integration_fabric', 3, 'Third-Party Integration Fabric penuh ke SAP, Oracle, MS Dynamics, Odoo, CMMS, Fleet, & HRIS eksternal.', 'ENTERPRISE_ONLY', TRUE),
    ('cap-ctx-fabric', 'company_context_fabric', 3, 'Penyatuan 8 dimensi konteks perusahaan secara holistik dari sistem pihak ketiga.', 'ENTERPRISE_ONLY', TRUE),
    ('cap-cross-sys', 'cross_system_intelligence', 3, 'Korelasi sinyal anomali dan deteksi risiko lintas sistem enterprise pihak ketiga.', 'ENTERPRISE_ONLY', TRUE),
    ('cap-spec-agents', 'specialist_agents_heavy_industry', 3, 'Agen AI Spesialis industri berat (Maintenance, Fleet, Project, HSE, Engineering).', 'ENTERPRISE_ONLY', TRUE),
    ('cap-chief-staff', 'ai_chief_of_staff', 3, 'AI Chief of Staff (5-Bintang) untuk koordinasi eksekutif dan sintesis briefing lintas departemen.', 'ENTERPRISE_ONLY', TRUE),
    ('cap-cmd-center', 'enterprise_command_center', 3, 'Enterprise Command Center Dashboard dengan visualisasi Health Score lintas entitas.', 'ENTERPRISE_ONLY', TRUE),
    ('cap-ent-reporting', 'enterprise_reporting', 3, 'Automatic Reporting lintas sistem pihak ketiga dan pelaporan proaktif seketika.', 'ENTERPRISE_ONLY', TRUE)
ON CONFLICT (capability_key) DO UPDATE SET
    min_tier_level = EXCLUDED.min_tier_level,
    description = EXCLUDED.description,
    category = EXCLUDED.category,
    updated_at = CURRENT_TIMESTAMP;
