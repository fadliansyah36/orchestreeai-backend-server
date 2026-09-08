-- ==============================================================================
-- OrchestreeAI Migration: Tahap 1 — Master Data & Katalog
-- File: V22__tahap1_master_data_katalog.sql
-- PRD Source: Master 16.2/16.3, Addendum 2 Bagian 81.4, Generative Studio Fase 73
-- Deskripsi: Skema tabel katalog master global & seed data resmi platform.
-- ==============================================================================

-- 1. Job Level Catalog
CREATE TABLE IF NOT EXISTS job_level_catalog (
    id TEXT PRIMARY KEY,
    level_code TEXT UNIQUE NOT NULL,
    level_name TEXT NOT NULL,
    hierarchy_order INT NOT NULL
);

-- 2. Job Sub-Title Catalog
CREATE TABLE IF NOT EXISTS job_sub_title_catalog (
    id TEXT PRIMARY KEY,
    parent_level_id TEXT NOT NULL,
    sub_title_name TEXT NOT NULL,
    CONSTRAINT fk_job_sub_title_parent_level FOREIGN KEY (parent_level_id) 
        REFERENCES job_level_catalog(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_job_sub_title_parent_level_id ON job_sub_title_catalog(parent_level_id);

-- 3. Industry Catalog
CREATE TABLE IF NOT EXISTS industry_catalog (
    id TEXT PRIMARY KEY,
    industry_code TEXT UNIQUE NOT NULL,
    industry_name TEXT NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_industry_catalog_code ON industry_catalog(industry_code);

-- 4. Creative Layout Templates
CREATE TABLE IF NOT EXISTS creative_layout_templates (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    subcategory TEXT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    aspect_ratio TEXT NOT NULL,
    page_count INT NOT NULL DEFAULT 1,
    slots_json TEXT NOT NULL DEFAULT '[]',
    is_official_global BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_role TEXT NOT NULL DEFAULT 'SUPER_ADMIN',
    preview_thumbnail_url TEXT NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);
CREATE INDEX IF NOT EXISTS idx_creative_layout_templates_cat ON creative_layout_templates(category);
CREATE INDEX IF NOT EXISTS idx_creative_layout_templates_official ON creative_layout_templates(is_official_global);

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- Master catalogs are readable by all authenticated users, writable by service_role
-- ==============================================================================
ALTER TABLE job_level_catalog ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS p_job_level_catalog_select ON job_level_catalog;
CREATE POLICY p_job_level_catalog_select ON job_level_catalog FOR SELECT USING (true);
DROP POLICY IF EXISTS p_job_level_catalog_service ON job_level_catalog;
CREATE POLICY p_job_level_catalog_service ON job_level_catalog FOR ALL USING (auth.role() = 'service_role');

ALTER TABLE job_sub_title_catalog ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS p_job_sub_title_catalog_select ON job_sub_title_catalog;
CREATE POLICY p_job_sub_title_catalog_select ON job_sub_title_catalog FOR SELECT USING (true);
DROP POLICY IF EXISTS p_job_sub_title_catalog_service ON job_sub_title_catalog;
CREATE POLICY p_job_sub_title_catalog_service ON job_sub_title_catalog FOR ALL USING (auth.role() = 'service_role');

ALTER TABLE industry_catalog ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS p_industry_catalog_select ON industry_catalog;
CREATE POLICY p_industry_catalog_select ON industry_catalog FOR SELECT USING (true);
DROP POLICY IF EXISTS p_industry_catalog_service ON industry_catalog;
CREATE POLICY p_industry_catalog_service ON industry_catalog FOR ALL USING (auth.role() = 'service_role');

ALTER TABLE creative_layout_templates ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS p_creative_layout_templates_select ON creative_layout_templates;
CREATE POLICY p_creative_layout_templates_select ON creative_layout_templates FOR SELECT USING (true);
DROP POLICY IF EXISTS p_creative_layout_templates_service ON creative_layout_templates;
CREATE POLICY p_creative_layout_templates_service ON creative_layout_templates FOR ALL USING (auth.role() = 'service_role');

-- ==============================================================================
-- MASTER DATA SEEDS (OFFICIAL PLATFORM LOOKUPS)
-- ==============================================================================

-- 1. Seed Job Level Catalog
INSERT INTO job_level_catalog (id, level_code, level_name, hierarchy_order) VALUES
    ('jl-owner', 'owner', 'Owner / Pendiri', 1),
    ('jl-clevel', 'c_level', 'Direksi / C-Level', 2),
    ('jl-gm', 'gm', 'General Manager', 3),
    ('jl-manager', 'manager', 'Manajer', 4),
    ('jl-spv', 'supervisor', 'Supervisor / Team Lead', 5),
    ('jl-staff', 'staff', 'Staff / Karyawan', 6),
    ('jl-intern', 'intern', 'Magang / Intern', 7)
ON CONFLICT (id) DO UPDATE SET
    level_code = EXCLUDED.level_code,
    level_name = EXCLUDED.level_name,
    hierarchy_order = EXCLUDED.hierarchy_order;

-- 2. Seed Job Sub-Title Catalog
INSERT INTO job_sub_title_catalog (id, parent_level_id, sub_title_name) VALUES
    -- Owner
    ('jst-own-1', 'jl-owner', 'Founder & CEO'),
    ('jst-own-2', 'jl-owner', 'Business Owner'),
    ('jst-own-3', 'jl-owner', 'Managing Director'),
    -- C-Level
    ('jst-cl-1', 'jl-clevel', 'Chief Executive Officer (CEO)'),
    ('jst-cl-2', 'jl-clevel', 'Chief Operating Officer (COO)'),
    ('jst-cl-3', 'jl-clevel', 'Chief Financial Officer (CFO)'),
    ('jst-cl-4', 'jl-clevel', 'Chief Technology Officer (CTO)'),
    ('jst-cl-5', 'jl-clevel', 'Chief Marketing Officer (CMO)'),
    ('jst-cl-6', 'jl-clevel', 'Chief HR Officer (CHRO)'),
    -- General Manager
    ('jst-gm-1', 'jl-gm', 'General Manager Operations'),
    ('jst-gm-2', 'jl-gm', 'General Manager Business'),
    -- Manajer
    ('jst-mgr-1', 'jl-manager', 'Sales Manager'),
    ('jst-mgr-2', 'jl-manager', 'Marketing Manager'),
    ('jst-mgr-3', 'jl-manager', 'HR Manager'),
    ('jst-mgr-4', 'jl-manager', 'Finance Manager'),
    ('jst-mgr-5', 'jl-manager', 'Operations Manager'),
    ('jst-mgr-6', 'jl-manager', 'Project Manager'),
    ('jst-mgr-7', 'jl-manager', 'IT Manager'),
    -- Supervisor
    ('jst-spv-1', 'jl-spv', 'Sales Supervisor'),
    ('jst-spv-2', 'jl-spv', 'Marketing Lead'),
    ('jst-spv-3', 'jl-spv', 'Operations Supervisor'),
    ('jst-spv-4', 'jl-spv', 'Customer Support Lead'),
    -- Staff
    ('jst-stf-1', 'jl-staff', 'Sales Executive'),
    ('jst-stf-2', 'jl-staff', 'Digital Marketer & Copywriter'),
    ('jst-stf-3', 'jl-staff', 'HR Specialist'),
    ('jst-stf-4', 'jl-staff', 'Account Officer'),
    ('jst-stf-5', 'jl-staff', 'Operations Specialist'),
    ('jst-stf-6', 'jl-staff', 'Customer Care Officer'),
    -- Intern
    ('jst-int-1', 'jl-intern', 'Marketing Intern'),
    ('jst-int-2', 'jl-intern', 'Sales Intern'),
    ('jst-int-3', 'jl-intern', 'HR Intern'),
    ('jst-int-4', 'jl-intern', 'Tech / Engineering Intern')
ON CONFLICT (id) DO UPDATE SET
    parent_level_id = EXCLUDED.parent_level_id,
    sub_title_name = EXCLUDED.sub_title_name;

-- 3. Seed Industry Catalog
INSERT INTO industry_catalog (id, industry_code, industry_name, description, is_active) VALUES
    ('ind-kesehatan', 'kesehatan', 'Kesehatan & Rumah Sakit', 'Pelayanan medis, rumah sakit, klinik, farmasi, dan alat kesehatan', true),
    ('ind-fnb', 'fnb', 'F&B (Makanan & Minuman)', 'Restoran, kafe, katering, produksi kuliner, dan franchise F&B', true),
    ('ind-umkm', 'umkm', 'UMKM & Bisnis Lokal', 'Usaha mikro, kecil, menengah, dan perdagangan ritel lokal', true),
    ('ind-startup', 'startup', 'Startup Teknologi & SaaS', 'Software, artificial intelligence, aplikasi digital, dan layanan cloud', true),
    ('ind-pendidikan', 'pendidikan', 'Pendidikan & Pelatihan', 'Sekolah, universitas, bimbingan belajar, dan platform edutech', true),
    ('ind-konstruksi', 'konstruksi', 'Konstruksi & Infrastruktur', 'EPC, kontraktor umum, arsitektur, dan pengembangan infrastruktur', true),
    ('ind-manufaktur', 'manufaktur', 'Manufaktur & Pabrikasi', 'Lini perakitan pabrik, smelter, komponen industri, dan QA/QC', true),
    ('ind-retail', 'retail', 'Retail & E-Commerce', 'Toko ritel, supermarket, e-commerce omnichannel, dan distribusi konsumen', true),
    ('ind-logistik', 'logistik', 'Logistik & Transportasi', 'Pengiriman ekspedisi, pergudangan, freight forwarding, dan armada darat/laut', true),
    ('ind-jasa', 'jasa', 'Jasa Profesional & Konsultan', 'Konsultan hukum, akuntansi, agensi kreatif, dan riset bisnis', true),
    ('ind-pertambangan', 'pertambangan', 'Pertambangan & Energi', 'Batubara, mineral, migas, pembangkit listrik, dan energi terbarukan', true),
    ('ind-pertanian', 'pertanian', 'Pertanian & Agribisnis', 'Perkebunan, budidaya tanaman pangan, peternakan, dan perikanan', true),
    ('ind-keuangan', 'keuangan', 'Keuangan & Finansial', 'Perbankan, multifinance, asuransi, fintech, dan pasar modal', true),
    ('ind-realestate', 'realestate', 'Real Estate & Properti', 'Pengembang properti, manajemen gedung, perumahan, dan agen properti', true),
    ('ind-otomotif', 'otomotif', 'Otomotif & Transportasi', 'Dealer kendaraan, bengkel, suku cadang, dan karoseri', true),
    ('ind-perhotelan', 'perhotelan', 'Perhotelan & Pariwisata', 'Hotel, resort, travel agent, atraksi wisata, dan MICE', true),
    ('ind-lainnya', 'lainnya', 'Lainnya', 'Sektor bisnis spesifik atau multidisiplin lainnya', true)
ON CONFLICT (id) DO UPDATE SET
    industry_code = EXCLUDED.industry_code,
    industry_name = EXCLUDED.industry_name,
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active;

-- 4. Seed Creative Layout Templates
INSERT INTO creative_layout_templates (
    id, name, category, subcategory, width, height, aspect_ratio, page_count, slots_json, is_official_global, created_by_role, preview_thumbnail_url, description
) VALUES
    (
        'tmpl-company-profile-a4',
        'Official Corporate Capability Statement (A4)',
        'COMPANY_PROFILE',
        'A4_MULTI_PAGE',
        1240,
        1754,
        'A4',
        4,
        '[{"slotId":"slot-cover-logo","type":"fixed_logo","pageIndex":1,"x":60,"y":70,"width":200,"height":80},{"slotId":"slot-cover-hero","type":"ai_image","pageIndex":1,"x":60,"y":380,"width":1120,"height":1094},{"slotId":"slot-cover-address","type":"fixed_text","pageIndex":1,"x":60,"y":1534,"width":1120,"height":160},{"slotId":"slot-p2-depts","type":"data_table","pageIndex":2,"x":60,"y":320,"width":1120,"height":400},{"slotId":"slot-p3-arch","type":"ai_image","pageIndex":3,"x":60,"y":440,"width":1120,"height":1114},{"slotId":"slot-p4-kpi","type":"data_table","pageIndex":4,"x":60,"y":210,"width":1120,"height":400},{"slotId":"slot-p4-qr","type":"fixed_logo","pageIndex":4,"x":60,"y":750,"width":140,"height":140}]',
        true,
        'SUPER_ADMIN',
        'https://storage.orchestree.io/templates/company-profile-preview.png',
        'Template resmi Company Profile multi-halaman (Cover + Ringkasan Eksekutif + Kapabilitas AI + Indikator Kinerja & Pengesahan).'
    ),
    (
        'tmpl-premium-content-cover',
        'Executive Whitepaper & Research Report Cover (4:5)',
        'PREMIUM_CONTENT_COVER',
        'COVER_STANDARD',
        1080,
        1350,
        '4:5',
        1,
        '[{"slotId":"slot-cover-logo","type":"fixed_logo","pageIndex":1,"x":40,"y":50,"width":180,"height":70},{"slotId":"slot-cover-hero","type":"ai_image","pageIndex":1,"x":40,"y":300,"width":1000,"height":800},{"slotId":"slot-cover-meta","type":"fixed_text","pageIndex":1,"x":40,"y":1150,"width":1000,"height":150}]',
        true,
        'SUPER_ADMIN',
        'https://storage.orchestree.io/templates/whitepaper-cover-preview.png',
        'Template sampul laporan eksekutif dan whitepaper resmi platform dengan slot AI visual dan header logo terkunci.'
    ),
    (
        'tmpl-feed-square-1080',
        'Social Feed Campaign & Promo Banner (1080x1080)',
        'BANNER_FEED',
        'FEED_1080_1080',
        1080,
        1080,
        '1:1',
        1,
        '[{"slotId":"slot-feed-logo","type":"fixed_logo","pageIndex":1,"x":32,"y":32,"width":190,"height":80},{"slotId":"slot-feed-bg","type":"ai_image","pageIndex":1,"x":0,"y":0,"width":1080,"height":1080},{"slotId":"slot-feed-tagline","type":"fixed_text","pageIndex":1,"x":32,"y":980,"width":600,"height":60}]',
        true,
        'SUPER_ADMIN',
        'https://storage.orchestree.io/templates/feed-square-preview.png',
        'Ukuran standar Instagram/LinkedIn feed square 1080x1080 dengan penempatan logo terkunci presisi di pojok atas.'
    ),
    (
        'tmpl-feed-portrait-1350',
        'Product Showcase Portrait Banner (1080x1350)',
        'BANNER_FEED',
        'FEED_1080_1350',
        1080,
        1350,
        '4:5',
        1,
        '[{"slotId":"slot-portrait-logo","type":"fixed_logo","pageIndex":1,"x":32,"y":32,"width":190,"height":80},{"slotId":"slot-portrait-bg","type":"ai_image","pageIndex":1,"x":0,"y":0,"width":1080,"height":1350}]',
        true,
        'SUPER_ADMIN',
        'https://storage.orchestree.io/templates/feed-portrait-preview.png',
        'Ukuran portrait 4:5 1080x1350 untuk optimalisasi engagement feed vertikal di Instagram & LinkedIn.'
    ),
    (
        'tmpl-banner-story-1920',
        'Story, Reels & Vertical Screen Banner (1080x1920)',
        'BANNER_FEED',
        'BANNER_1080_1920',
        1080,
        1920,
        '9:16',
        1,
        '[{"slotId":"slot-story-logo","type":"fixed_logo","pageIndex":1,"x":36,"y":60,"width":200,"height":80},{"slotId":"slot-story-bg","type":"ai_image","pageIndex":1,"x":0,"y":0,"width":1080,"height":1920},{"slotId":"slot-story-cta","type":"fixed_text","pageIndex":1,"x":36,"y":1780,"width":1008,"height":80}]',
        true,
        'SUPER_ADMIN',
        'https://storage.orchestree.io/templates/story-tall-preview.png',
        'Ukuran full-screen 9:16 1080x1920 untuk Instagram Story, TikTok Ads, dan digital signage vertikal.'
    )
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    category = EXCLUDED.category,
    subcategory = EXCLUDED.subcategory,
    width = EXCLUDED.width,
    height = EXCLUDED.height,
    aspect_ratio = EXCLUDED.aspect_ratio,
    page_count = EXCLUDED.page_count,
    slots_json = EXCLUDED.slots_json,
    is_official_global = EXCLUDED.is_official_global,
    description = EXCLUDED.description;
