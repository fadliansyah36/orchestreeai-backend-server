-- ====================================================================
-- Flyway Migration: V91__fase91_master_data_reconstruction.sql
-- OrchestreeAI: Complete Master Data Taxonomies Reconstruction (Fase 91)
-- ====================================================================

-- Ensure helper functions exist for RLS if not defined
CREATE OR REPLACE FUNCTION current_user_role()
RETURNS TEXT AS $$
BEGIN
    RETURN COALESCE(current_setting('request.jwt.claims', true)::json->>'role', 'authenticated');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ====================================================================
-- BAGIAN A — DEPARTMENT CATEGORIES (14 Kategori Resmi)
-- ====================================================================
CREATE TABLE IF NOT EXISTS department_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_code TEXT UNIQUE NOT NULL,
    category_name TEXT NOT NULL,
    description TEXT,
    icon_key TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT now()
);

INSERT INTO department_categories (category_code, category_name, description, icon_key) VALUES
('executive', 'Executive & Leadership', 'Direksi, C-Level, dan kepemimpinan strategis perusahaan', 'crown'),
('sales', 'Sales & Business Development', 'Penjualan, akuisisi pelanggan, dan pengembangan bisnis', 'trending_up'),
('marketing', 'Marketing & Brand', 'Pemasaran, branding, dan komunikasi produk', 'megaphone'),
('customer_service', 'Customer Service & Success', 'Layanan pelanggan dan keberhasilan pelanggan', 'headset'),
('hr', 'Human Resources & People', 'Sumber daya manusia, rekrutmen, dan pengembangan talenta', 'people'),
('finance', 'Finance & Accounting', 'Keuangan, akuntansi, dan pengendalian anggaran', 'wallet'),
('operations', 'Operations & Production', 'Operasional harian, produksi, dan efisiensi proses', 'settings'),
('procurement', 'Procurement & Supply Chain', 'Pengadaan, rantai pasok, dan manajemen vendor', 'truck'),
('project', 'Project & Program Management', 'Manajemen proyek dan program lintas tim', 'flag'),
('it_engineering', 'IT & Engineering', 'Teknologi informasi dan pengembangan sistem', 'code'),
('legal', 'Legal & Compliance', 'Hukum, kepatuhan regulasi, dan manajemen risiko', 'gavel'),
('research', 'Research & Development', 'Riset, inovasi, dan pengembangan produk baru', 'flask'),
('hse', 'Health, Safety & Environment', 'Kesehatan, keselamatan kerja, dan lingkungan', 'shield'),
('quality', 'Quality Assurance & Control', 'Jaminan dan pengendalian kualitas', 'check_circle')
ON CONFLICT (category_code) DO UPDATE SET
    category_name = EXCLUDED.category_name,
    description = EXCLUDED.description,
    icon_key = EXCLUDED.icon_key,
    is_active = EXCLUDED.is_active;

ALTER TABLE department_categories ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_department_categories ON department_categories;
CREATE POLICY read_department_categories ON department_categories
    FOR SELECT USING (auth.role() = 'authenticated');
DROP POLICY IF EXISTS manage_department_categories ON department_categories;
CREATE POLICY manage_department_categories ON department_categories
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');

-- ====================================================================
-- BAGIAN B — JOB LEVEL CATALOG (9 Level Hierarki Resmi)
-- ====================================================================
CREATE TABLE IF NOT EXISTS job_level_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    level_code TEXT UNIQUE NOT NULL,
    level_name TEXT NOT NULL,
    hierarchy_order INT NOT NULL,
    description TEXT
);

INSERT INTO job_level_catalog (level_code, level_name, hierarchy_order, description) VALUES
('owner', 'Owner / Pendiri', 1, 'Pemilik perusahaan atau pendiri utama'),
('direksi', 'Direksi / C-Level (CEO, COO, CFO, CTO, CMO, CHRO)', 2, 'Jajaran eksekutif tertinggi perusahaan'),
('vp', 'Vice President / Senior Director', 3, 'Kepemimpinan senior lintas divisi'),
('gm', 'General Manager', 4, 'Kepala operasional unit bisnis/cabang'),
('manajer', 'Manajer / Head of Department', 5, 'Kepala departemen fungsional'),
('supervisor', 'Supervisor / Team Lead / Koordinator', 6, 'Pemimpin tim operasional harian'),
('staff_senior', 'Staff Senior / Spesialis', 7, 'Karyawan berpengalaman dengan keahlian spesifik'),
('staff', 'Staff / Karyawan', 8, 'Karyawan operasional umum'),
('magang', 'Magang / Intern', 9, 'Peserta magang/pelatihan kerja')
ON CONFLICT (level_code) DO UPDATE SET
    level_name = EXCLUDED.level_name,
    hierarchy_order = EXCLUDED.hierarchy_order,
    description = EXCLUDED.description;

ALTER TABLE job_level_catalog ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_job_level_catalog ON job_level_catalog;
CREATE POLICY read_job_level_catalog ON job_level_catalog
    FOR SELECT USING (auth.role() = 'authenticated');
DROP POLICY IF EXISTS manage_job_level_catalog ON job_level_catalog;
CREATE POLICY manage_job_level_catalog ON job_level_catalog
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');

-- ====================================================================
-- BAGIAN C — JOB SUB TITLE CATALOG (Komprehensif 14 Dept x Level)
-- ====================================================================
CREATE TABLE IF NOT EXISTS job_sub_title_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_level_id UUID REFERENCES job_level_catalog(id),
    department_category_id UUID REFERENCES department_categories(id),
    sub_title_name TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uq_job_sub_title UNIQUE (parent_level_id, department_category_id, sub_title_name)
);

-- 1. Executive
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Chief Executive Officer (CEO)', 'Chief Operating Officer (COO)', 'Chief Financial Officer (CFO)', 'Chief Technology Officer (CTO)', 'Chief Marketing Officer (CMO)', 'Chief Human Resources Officer (CHRO)']) AS title
WHERE jl.level_code = 'direksi' AND dc.category_code = 'executive'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Founder & Chief Commissioner', 'Owner & Managing Partner', 'Co-Founder & Executive Director']) AS title
WHERE jl.level_code = 'owner' AND dc.category_code = 'executive'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['VP Corporate Strategy', 'VP Business Transformation', 'Senior Director of Operations']) AS title
WHERE jl.level_code = 'vp' AND dc.category_code = 'executive'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 2. Sales & Business Development
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Sales Manager', 'Business Development Manager', 'Key Account Manager', 'Territory Sales Manager']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'sales'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Sales Supervisor', 'SDR Team Lead', 'Account Executive Supervisor']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'sales'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Senior Account Executive', 'Senior Business Development Specialist', 'Strategic Closer']) AS title
WHERE jl.level_code = 'staff_senior' AND dc.category_code = 'sales'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Sales Representative', 'Sales Development Rep (SDR)', 'Inside Sales Officer']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'sales'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 3. Marketing & Brand
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Marketing Manager', 'Brand Manager', 'Digital Marketing Manager', 'Performance Marketing Lead']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'marketing'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Content Marketing Supervisor', 'Social Media Team Lead', 'Media Buying Supervisor']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'marketing'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Senior Performance Specialist', 'Senior Copywriter', 'Creative Art Director']) AS title
WHERE jl.level_code = 'staff_senior' AND dc.category_code = 'marketing'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Digital Marketing Officer', 'Content Creator', 'Graphic Designer', 'Social Media Specialist']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'marketing'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 4. Customer Service & Success
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Customer Service Manager', 'Customer Success Manager', 'Contact Center Manager']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'customer_service'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Customer Service Supervisor', 'Support Desk Team Lead', 'Retention Team Lead']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'customer_service'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Customer Service Representative', 'Helpdesk Officer', 'Omnichannel Chat Agent', 'Customer Success Specialist']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'customer_service'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 5. Human Resources & People
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['HR Manager', 'Talent Acquisition Manager', 'People & Culture Manager', 'HR Operations Manager']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'hr'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['HR Supervisor', 'Recruitment Team Lead', 'Payroll & Benefits Supervisor']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'hr'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['HR Generalist', 'Recruitment Specialist', 'HR Operations Officer', 'People Development Staff']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'hr'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 6. Finance & Accounting
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Finance Manager', 'Accounting Manager', 'Tax & Treasury Manager', 'Financial Planning & Analysis (FP&A) Manager']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'finance'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Finance Supervisor', 'Accounting Supervisor', 'Billing & Collection Supervisor']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'finance'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Staff Akuntansi', 'Staff Finance / Kasir', 'Tax Specialist', 'Accounts Payable / Receivable Staff']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'finance'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 7. Operations & Production
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Operations Manager', 'Production Manager', 'Plant Manager', 'Fleet & Logistics Operations Manager']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'operations'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Operations Supervisor', 'Production Shift Lead', 'Maintenance Supervisor', 'Fleet Supervisor']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'operations'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Staff Operasional', 'Teknisi Pemeliharaan / Maintenance', 'Operator Produksi', 'Fleet Controller']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'operations'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 8. Procurement & Supply Chain
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Procurement Manager', 'Supply Chain Manager', 'Purchasing & Sourcing Lead']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'procurement'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Procurement Supervisor', 'Warehouse & Inventory Supervisor', 'Vendor Management Lead']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'procurement'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Purchasing Officer', 'Buyer / Sourcing Staff', 'Warehouse Officer', 'Inventory Controller']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'procurement'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 9. Project & Program Management
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Project Manager', 'Program Manager', 'PMO Lead', 'Agile Delivery Lead']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'project'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Project Supervisor', 'Scrum Master', 'Project Coordinator Lead']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'project'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Project Coordinator', 'PMO Staff', 'Project Scheduler', 'Project Admin']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'project'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 10. IT & Engineering
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['IT Manager', 'Software Engineering Manager', 'Infrastructure & Security Manager', 'DevOps & Cloud Lead']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'it_engineering'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['IT Tech Lead', 'DevOps Supervisor', 'IT Support Team Lead', 'Database Administrator Lead']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'it_engineering'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Full Stack Developer', 'Mobile Engineer (Android/iOS)', 'DevOps Engineer', 'IT Support Specialist', 'System Administrator']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'it_engineering'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 11. Legal & Compliance
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Legal Manager', 'Compliance & Regulatory Affairs Manager', 'Corporate Secretary Lead']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'legal'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Legal Supervisor', 'Compliance Team Lead', 'Contract Management Lead']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'legal'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Corporate Legal Counsel', 'Legal Specialist', 'Compliance Officer', 'Contract Specialist']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'legal'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 12. Research & Development
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['R&D Manager', 'Chief Scientist / Research Lead', 'Innovation & Product Development Manager']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'research'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Research Supervisor', 'Lab Team Lead', 'Innovation Project Lead']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'research'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Research Scientist', 'Product Innovation Specialist', 'Lab Analyst', 'R&D Staff']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'research'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 13. Health, Safety & Environment (HSE)
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['HSE Manager', 'Safety & Environmental Health Manager', 'K3 Corporate Manager']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'hse'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['HSE Supervisor', 'Field Safety Team Lead', 'Environmental Compliance Supervisor']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'hse'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['HSE Officer / K3 Specialist', 'Environmental Safety Officer', 'Field Safety Inspector', 'HSE Admin']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'hse'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

-- 14. Quality Assurance & Control
INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Quality Assurance (QA) Manager', 'Quality Control (QC) Manager', 'Quality Management Representative']) AS title
WHERE jl.level_code = 'manajer' AND dc.category_code = 'quality'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['QA Supervisor', 'QC Team Lead', 'Quality Audit Supervisor']) AS title
WHERE jl.level_code = 'supervisor' AND dc.category_code = 'quality'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

INSERT INTO job_sub_title_catalog (parent_level_id, department_category_id, sub_title_name)
SELECT jl.id, dc.id, title FROM job_level_catalog jl, department_categories dc,
UNNEST(ARRAY['Quality Assurance Specialist', 'QC Inspector', 'Quality Auditor', 'QA/QC Staff']) AS title
WHERE jl.level_code = 'staff' AND dc.category_code = 'quality'
ON CONFLICT (parent_level_id, department_category_id, sub_title_name) DO NOTHING;

ALTER TABLE job_sub_title_catalog ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_job_sub_title ON job_sub_title_catalog;
CREATE POLICY read_job_sub_title ON job_sub_title_catalog
    FOR SELECT USING (auth.role() = 'authenticated');
DROP POLICY IF EXISTS manage_job_sub_title ON job_sub_title_catalog;
CREATE POLICY manage_job_sub_title ON job_sub_title_catalog
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');

-- ====================================================================
-- BAGIAN D — INDUSTRY CATALOG (20 Kategori Industri Resmi)
-- ====================================================================
CREATE TABLE IF NOT EXISTS industry_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    industry_code TEXT UNIQUE NOT NULL,
    industry_name TEXT NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO industry_catalog (industry_code, industry_name, description) VALUES
('fnb', 'Food & Beverage', 'Restoran, kafe, katering, dan industri makanan-minuman'),
('retail', 'Retail & E-Commerce', 'Perdagangan eceran dan toko online'),
('healthcare', 'Kesehatan & Rumah Sakit', 'Rumah sakit, klinik, apotek, dan layanan kesehatan'),
('construction', 'Konstruksi & Real Estate', 'Kontraktor bangunan, properti, dan pengembangan lahan'),
('manufacturing', 'Manufaktur & Industri', 'Pabrik produksi barang dan industri berat'),
('startup_tech', 'Startup Teknologi & SaaS', 'Perusahaan rintisan berbasis teknologi digital'),
('umkm', 'UMKM (Usaha Mikro, Kecil, Menengah)', 'Usaha skala kecil-menengah lintas sektor'),
('professional_services', 'Jasa Profesional (Konsultan, Hukum, Akuntansi)', 'Penyedia jasa keahlian profesional'),
('education', 'Pendidikan & Pelatihan', 'Sekolah, lembaga kursus, dan pelatihan'),
('logistics', 'Logistik & Transportasi', 'Pengiriman, ekspedisi, dan transportasi barang'),
('mining_energy', 'Pertambangan & Energi', 'Ekstraksi sumber daya alam dan energi'),
('agriculture', 'Pertanian & Perkebunan', 'Produksi pertanian, perkebunan, dan agribisnis'),
('finance_banking', 'Keuangan & Perbankan', 'Bank, fintech, asuransi, dan lembaga keuangan'),
('automotive', 'Otomotif', 'Dealer, bengkel, dan industri kendaraan'),
('hospitality_tourism', 'Perhotelan & Pariwisata', 'Hotel, resor, dan biro perjalanan'),
('fashion_beauty', 'Fashion & Kecantikan', 'Industri busana, kosmetik, dan perawatan diri'),
('media_entertainment', 'Media & Hiburan', 'Produksi konten, penyiaran, dan hiburan'),
('agency_creative', 'Digital Agency & Kreatif', 'Agensi pemasaran digital dan kreatif'),
('nonprofit', 'Organisasi Nirlaba', 'Yayasan, NGO, dan organisasi sosial'),
('other', 'Lainnya', 'Kategori industri di luar daftar resmi di atas')
ON CONFLICT (industry_code) DO UPDATE SET
    industry_name = EXCLUDED.industry_name,
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active;

ALTER TABLE industry_catalog ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_industry_catalog ON industry_catalog;
CREATE POLICY read_industry_catalog ON industry_catalog
    FOR SELECT USING (auth.role() = 'authenticated');
DROP POLICY IF EXISTS manage_industry_catalog ON industry_catalog;
CREATE POLICY manage_industry_catalog ON industry_catalog
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');

-- ====================================================================
-- BAGIAN E — SUBSCRIPTION PLANS (Paket Langganan Kompetitif)
-- ====================================================================
CREATE TABLE IF NOT EXISTS subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_code TEXT UNIQUE NOT NULL,
    plan_name TEXT NOT NULL,
    tier_level INT NOT NULL,
    price_monthly NUMERIC NOT NULL,
    max_ai_agents INT,
    max_human_seats INT,
    max_channel_accounts INT,
    monthly_credit_quota NUMERIC,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO subscription_plans (plan_code, plan_name, tier_level, price_monthly, max_ai_agents, max_human_seats, max_channel_accounts, monthly_credit_quota, description) VALUES
('trial', 'Trial 7 Hari', 0, 0, 2, 3, 1, 50, 'Uji coba gratis 7 hari, limit 5 perintah/hari (Fase 70.D)'),
('starter', 'Starter Growth', 1, 299000, 3, 5, 2, 500, 'Untuk UMKM dan tim kecil memulai otomasi AI'),
('professional', 'Professional Growth', 2, 799000, 10, 25, 5, 2000, 'Untuk bisnis berkembang dengan kebutuhan multi-channel'),
('business', 'Business Advanced', 3, 1999000, 25, 75, 15, 6000, 'Untuk perusahaan menengah dengan tim lintas departemen'),
('enterprise', 'Enterprise Sovereign', 4, 4999000, 50, 150, 30, 15000, 'Fitur Enterprise penuh: Integration Fabric, AI Chief of Staff, Continuous Learning'),
('custom', 'Custom Enterprise Dedicated', 5, 0, NULL, NULL, NULL, NULL, 'Kontrak khusus, kuota dan harga dinegosiasikan sesuai kebutuhan')
ON CONFLICT (plan_code) DO UPDATE SET
    plan_name = EXCLUDED.plan_name,
    tier_level = EXCLUDED.tier_level,
    price_monthly = EXCLUDED.price_monthly,
    max_ai_agents = EXCLUDED.max_ai_agents,
    max_human_seats = EXCLUDED.max_human_seats,
    max_channel_accounts = EXCLUDED.max_channel_accounts,
    monthly_credit_quota = EXCLUDED.monthly_credit_quota,
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active;

ALTER TABLE subscription_plans ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_subscription_plans ON subscription_plans;
CREATE POLICY read_subscription_plans ON subscription_plans
    FOR SELECT USING (true);
DROP POLICY IF EXISTS manage_subscription_plans ON subscription_plans;
CREATE POLICY manage_subscription_plans ON subscription_plans
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');

-- ====================================================================
-- BAGIAN F — FEATURE CAPABILITIES (15 Kapabilitas Sistem)
-- ====================================================================
CREATE TABLE IF NOT EXISTS feature_capabilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    capability_key TEXT UNIQUE NOT NULL,
    capability_name TEXT NOT NULL,
    min_tier_level INT NOT NULL,
    description TEXT
);

INSERT INTO feature_capabilities (capability_key, capability_name, min_tier_level, description) VALUES
('ai_execution_layer', 'AI Agent Execution Layer', 0, 'Eksekusi otomatis oleh AI Agent (All-Tier)'),
('ai_action_orchestration', 'AI Action Orchestration', 0, 'Orkestrasi aksi AI dengan approval gate'),
('ai_monitoring_loop', 'AI Monitoring Loop', 0, 'Closed-loop monitoring task otomatis'),
('continuous_learning_core', 'Continuous Learning Core', 0, 'Pembelajaran berkelanjutan AI Agent'),
('ai_finance_intelligence', 'AI Finance Intelligence', 0, 'Analisis keuangan otomatis'),
('ai_knowledge_operational_fusion', 'AI Knowledge + Operational Data', 0, 'Fusi pengetahuan dan data operasional'),
('ai_event_engine', 'AI Event Engine', 0, 'Event-driven AI activation'),
('omnichannel_sales_marketing', 'Omnichannel Sales & Marketing', 1, 'Modul penjualan multi-channel'),
('generative_studio', 'Generative Studio', 1, 'Generate gambar, dokumen, konten'),
('integration_fabric', 'Third-Party Integration Fabric', 4, 'Integrasi sistem enterprise eksternal (Enterprise+)'),
('company_context_fabric', 'Company Context Fabric', 4, 'Konteks perusahaan lintas sistem (Enterprise+)'),
('specialist_agents_heavy_industry', 'Specialist AI Agents Industri Berat', 4, 'AI Agent Maintenance/Fleet/HSE (Enterprise+)'),
('ai_chief_of_staff', 'AI Chief of Staff', 4, 'Koordinator eksekutif AI tertinggi (Enterprise+)'),
('enterprise_command_center', 'Enterprise Command Center Dashboard', 4, 'Dashboard kontrol enterprise (Enterprise+)'),
('custom_plugin_skills', 'Custom Plugin & Skills Upload', 3, 'Upload plugin skill kustom AI Agent (Business+)')
ON CONFLICT (capability_key) DO UPDATE SET
    capability_name = EXCLUDED.capability_name,
    min_tier_level = EXCLUDED.min_tier_level,
    description = EXCLUDED.description;

ALTER TABLE feature_capabilities ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_feature_capabilities ON feature_capabilities;
CREATE POLICY read_feature_capabilities ON feature_capabilities
    FOR SELECT USING (auth.role() = 'authenticated');
DROP POLICY IF EXISTS manage_feature_capabilities ON feature_capabilities;
CREATE POLICY manage_feature_capabilities ON feature_capabilities
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');

-- ====================================================================
-- BAGIAN G — CREATIVE LAYOUT TEMPLATES (Layout Studio)
-- ====================================================================
CREATE TABLE IF NOT EXISTS creative_layout_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope TEXT NOT NULL DEFAULT 'global',
    template_type TEXT NOT NULL UNIQUE,
    template_name TEXT NOT NULL,
    slots JSONB NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO creative_layout_templates (scope, template_type, template_name, slots) VALUES
('global', 'company_profile_page', 'Company Profile Standar 4 Halaman', 
 '{"pages": [{"type":"cover","slots":[{"type":"ai_image","area":"background"},{"type":"fixed_logo"},{"type":"fixed_text","field":"company_name"}]},{"type":"about","slots":[{"type":"fixed_text","field":"company_description"},{"type":"ai_image","area":"illustration"}]},{"type":"services","slots":[{"type":"data_table","field":"products"}]},{"type":"contact","slots":[{"type":"fixed_text","field":"contact_info"},{"type":"fixed_logo"}]}]}'::jsonb),
('global', 'premium_content_cover', 'Cover Konten Premium', 
 '{"slots":[{"type":"ai_image","area":"full_background"},{"type":"fixed_text","field":"title"},{"type":"fixed_logo","position":"bottom_right"}]}'::jsonb),
('global', 'banner_standard', 'Banner Sosial Media Standar',
 '{"slots":[{"type":"ai_image","area":"full_background"},{"type":"fixed_text","field":"headline","optional":true}]}'::jsonb)
ON CONFLICT (template_type) DO UPDATE SET
    template_name = EXCLUDED.template_name,
    slots = EXCLUDED.slots,
    is_active = EXCLUDED.is_active;

ALTER TABLE creative_layout_templates ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_creative_templates ON creative_layout_templates;
CREATE POLICY read_creative_templates ON creative_layout_templates
    FOR SELECT USING (auth.role() = 'authenticated');
DROP POLICY IF EXISTS manage_creative_templates ON creative_layout_templates;
CREATE POLICY manage_creative_templates ON creative_layout_templates
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');

-- ====================================================================
-- BAGIAN H — AI JOB TITLES (15 Jabatan Baku Utama AI Agent)
-- ====================================================================
CREATE TABLE IF NOT EXISTS ai_job_titles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_code TEXT UNIQUE NOT NULL,
    job_name TEXT NOT NULL,
    icon_key TEXT,
    star_rating INT DEFAULT 4,
    description TEXT NOT NULL,
    maps_to_persona_type TEXT NOT NULL,
    is_top_coordinator BOOLEAN DEFAULT FALSE,
    relevant_department_category_id UUID REFERENCES department_categories(id)
);

-- Chief of Staff (5 Bintang & Top Coordinator)
INSERT INTO ai_job_titles (job_code, job_name, icon_key, star_rating, description, maps_to_persona_type, is_top_coordinator, relevant_department_category_id)
SELECT 'chief_of_staff', 'AI Chief of Staff', 'crown', 5,
  'Mengoordinasikan seluruh Staff AI Agent, mensintesis Executive Briefing, riset & R&D lintas departemen, memantau kinerja perusahaan penuh (AI + Human)',
  'CHIEF_OF_STAFF', TRUE, dc.id FROM department_categories dc WHERE dc.category_code = 'executive'
ON CONFLICT (job_code) DO UPDATE SET
    job_name = EXCLUDED.job_name,
    star_rating = EXCLUDED.star_rating,
    description = EXCLUDED.description,
    maps_to_persona_type = EXCLUDED.maps_to_persona_type,
    is_top_coordinator = EXCLUDED.is_top_coordinator,
    relevant_department_category_id = EXCLUDED.relevant_department_category_id;

-- 14 Specialist & Departmental AI Job Titles
INSERT INTO ai_job_titles (job_code, job_name, icon_key, star_rating, description, maps_to_persona_type, is_top_coordinator, relevant_department_category_id)
SELECT 'company_intelligence', 'AI Company Intelligence', 'radar', 4,
  'Memantau kompetitor, tren pasar (World Monitor), sinyal prospek (Vibe Prospecting), korelasi lintas sistem perusahaan',
  'COMPANY_INTELLIGENCE_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'executive'
UNION ALL SELECT 'operations', 'AI Operations', 'settings', 4,
  'Memantau produksi, equipment, fleet, kualitas, downtime, efisiensi operasional harian',
  'OPERATIONS_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'operations'
UNION ALL SELECT 'sales', 'AI Sales', 'trending_up', 4,
  'Discovery, rekomendasi produk, upsell/cross-sell, objection handling, closing',
  'SALES_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'sales'
UNION ALL SELECT 'customer_service', 'AI Customer Service', 'headset', 4,
  'Menjawab FAQ, menangani komplain, refund, return, warranty, eskalasi ke manusia bila perlu',
  'CUSTOMER_SERVICE_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'customer_service'
UNION ALL SELECT 'hr_recruitment', 'AI HR & Recruitment', 'people', 4,
  'Analisis kebutuhan tenaga kerja, monitoring kehadiran, rekrutmen, employee data dari HRIS',
  'HR_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'hr'
UNION ALL SELECT 'finance', 'AI Finance', 'wallet', 4,
  'Cash flow analysis, AR/AP, budget variance, revenue forecasting',
  'FINANCE_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'finance'
UNION ALL SELECT 'marketing', 'AI Marketing', 'megaphone', 4,
  'Membuat & menjalankan campaign, content generation, brand voice, atribusi revenue',
  'MARKETING_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'marketing'
UNION ALL SELECT 'knowledge_document', 'AI Knowledge & Document', 'book', 4,
  'Mengelola Company Brain, ekstraksi dokumen (SOP/manual/kontrak), menjaga akurasi pengetahuan perusahaan',
  'KNOWLEDGE_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'it_engineering'
UNION ALL SELECT 'task_workflow', 'AI Task & Workflow', 'kanban', 4,
  'Membuat task otomatis, memonitor closed-loop, mengelola papan kerja Human+AI',
  'WORKFLOW_ORCHESTRATOR_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'project'
UNION ALL SELECT 'crm_customer_success', 'AI CRM & Customer Success', 'heart_handshake', 4,
  'Mengelola relasi pelanggan jangka panjang, retensi, kesehatan akun, reaktivasi',
  'CUSTOMER_SUCCESS_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'customer_service'
UNION ALL SELECT 'procurement', 'AI Procurement', 'truck', 4,
  'Memantau PO, RFQ, supplier, delivery, stok kritikal',
  'PROCUREMENT_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'procurement'
UNION ALL SELECT 'project', 'AI Project', 'flag', 4,
  'Project Health Score, jadwal, biaya, manpower, deteksi risiko keterlambatan',
  'PROJECT_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'project'
UNION ALL SELECT 'research', 'AI Research', 'flask', 4,
  'Riset mendalam sesuai Knowledge Priority Hierarchy, menemukan pola & mengusulkan Knowledge Rule baru',
  'RESEARCH_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'research'
UNION ALL SELECT 'reporting', 'AI Reporting', 'chart_bar', 4,
  'Menyusun Daily/Weekly/Monthly/Management Report otomatis, proactive reporting, visualisasi data',
  'REPORTING_AGENT', FALSE, dc.id FROM department_categories dc WHERE dc.category_code = 'executive'
ON CONFLICT (job_code) DO UPDATE SET
    job_name = EXCLUDED.job_name,
    icon_key = EXCLUDED.icon_key,
    description = EXCLUDED.description,
    maps_to_persona_type = EXCLUDED.maps_to_persona_type,
    relevant_department_category_id = EXCLUDED.relevant_department_category_id;

ALTER TABLE ai_job_titles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_ai_job_titles ON ai_job_titles;
CREATE POLICY read_ai_job_titles ON ai_job_titles
    FOR SELECT USING (auth.role() = 'authenticated');
DROP POLICY IF EXISTS manage_ai_job_titles ON ai_job_titles;
CREATE POLICY manage_ai_job_titles ON ai_job_titles
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');

-- ====================================================================
-- BAGIAN I — AI STRUCTURAL ROLES (20 Sub-Jabatan Spesifik)
-- ====================================================================
CREATE TABLE IF NOT EXISTS ai_structural_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_job_title_id UUID REFERENCES ai_job_titles(id),
    structural_code TEXT UNIQUE NOT NULL,
    structural_name TEXT NOT NULL,
    skill_summary TEXT NOT NULL,
    maps_to_persona_type TEXT NOT NULL
);

INSERT INTO ai_structural_roles (parent_job_title_id, structural_code, structural_name, skill_summary, maps_to_persona_type)
SELECT jt.id, 'sdr', 'AI SDR', 'Kualifikasi lead awal, penggalian kebutuhan', 'SDR' FROM ai_job_titles jt WHERE jt.job_code = 'sales'
UNION ALL SELECT jt.id, 'sales_consultant', 'AI Sales Consultant', 'Sales discovery mendalam', 'SALES_CONSULTANT' FROM ai_job_titles jt WHERE jt.job_code = 'sales'
UNION ALL SELECT jt.id, 'product_advisor', 'AI Product Advisor', 'Rekomendasi produk, upsell, cross-sell', 'PRODUCT_ADVISOR' FROM ai_job_titles jt WHERE jt.job_code = 'sales'
UNION ALL SELECT jt.id, 'closer', 'AI Closer', 'Objection handling dan closing', 'CLOSER' FROM ai_job_titles jt WHERE jt.job_code = 'sales'
UNION ALL SELECT jt.id, 'follow_up', 'AI Follow-Up Agent', 'Follow-up lead dingin, abandoned cart', 'FOLLOW_UP_AGENT' FROM ai_job_titles jt WHERE jt.job_code = 'sales'
UNION ALL SELECT jt.id, 'campaign_specialist', 'AI Campaign Specialist', 'Eksekusi campaign marketing', 'CAMPAIGN_SPECIALIST' FROM ai_job_titles jt WHERE jt.job_code = 'marketing'
UNION ALL SELECT jt.id, 'content_creator', 'AI Content Creator', 'Pembuatan konten kreatif', 'CONTENT_CREATOR' FROM ai_job_titles jt WHERE jt.job_code = 'marketing'
UNION ALL SELECT jt.id, 'receptionist', 'AI Receptionist', 'Menyambut dan klasifikasi kebutuhan awal customer', 'RECEPTIONIST' FROM ai_job_titles jt WHERE jt.job_code = 'customer_service'
UNION ALL SELECT jt.id, 'complaint_handler', 'AI Complaint Handler', 'Penanganan komplain dan eskalasi', 'COMPLAINT_HANDLER' FROM ai_job_titles jt WHERE jt.job_code = 'customer_service'
UNION ALL SELECT jt.id, 'retention_specialist', 'AI Retention Specialist', 'Reaktivasi dan retensi customer lama', 'RETENTION_AGENT' FROM ai_job_titles jt WHERE jt.job_code = 'crm_customer_success'
UNION ALL SELECT jt.id, 'maintenance_officer', 'AI Maintenance Officer', 'Analisis maintenance equipment', 'MAINTENANCE_AGENT' FROM ai_job_titles jt WHERE jt.job_code = 'operations'
UNION ALL SELECT jt.id, 'fleet_analyst', 'AI Fleet Analyst', 'Analisis fleet dan telematika kendaraan', 'FLEET_AGENT' FROM ai_job_titles jt WHERE jt.job_code = 'operations'
UNION ALL SELECT jt.id, 'recruitment_screener', 'AI Recruitment Screener', 'Penyaringan kandidat rekrutmen', 'RECRUITMENT_AGENT' FROM ai_job_titles jt WHERE jt.job_code = 'hr_recruitment'
UNION ALL SELECT jt.id, 'attendance_officer', 'AI Attendance Officer', 'Monitoring kehadiran staff', 'ATTENDANCE_AGENT' FROM ai_job_titles jt WHERE jt.job_code = 'hr_recruitment'
UNION ALL SELECT jt.id, 'cashflow_analyst', 'AI Cash Flow Analyst', 'Analisis arus kas perusahaan', 'CASHFLOW_AGENT' FROM ai_job_titles jt WHERE jt.job_code = 'finance'
UNION ALL SELECT jt.id, 'po_tracker', 'AI PO Tracker', 'Pelacakan status purchase order', 'PO_TRACKER_AGENT' FROM ai_job_titles jt WHERE jt.job_code = 'procurement'
UNION ALL SELECT jt.id, 'schedule_analyst', 'AI Schedule Analyst', 'Analisis jadwal dan risiko keterlambatan proyek', 'SCHEDULE_AGENT' FROM ai_job_titles jt WHERE jt.job_code = 'project'
UNION ALL SELECT jt.id, 'document_analyst', 'AI Document Analyst', 'Ekstraksi dan analisis dokumen', 'DOCUMENT_ANALYST' FROM ai_job_titles jt WHERE jt.job_code = 'knowledge_document'
UNION ALL SELECT jt.id, 'pattern_finder', 'AI Pattern Finder', 'Penemuan pola dari data historis', 'PATTERN_FINDER_AGENT' FROM ai_job_titles jt WHERE jt.job_code = 'research'
UNION ALL SELECT jt.id, 'data_viz_agent', 'AI Data Visualization Agent', 'Visualisasi data untuk laporan', 'DATA_VIZ_AGENT' FROM ai_job_titles jt WHERE jt.job_code = 'reporting'
ON CONFLICT (structural_code) DO UPDATE SET
    structural_name = EXCLUDED.structural_name,
    skill_summary = EXCLUDED.skill_summary,
    maps_to_persona_type = EXCLUDED.maps_to_persona_type;

ALTER TABLE ai_structural_roles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_ai_structural_roles ON ai_structural_roles;
CREATE POLICY read_ai_structural_roles ON ai_structural_roles
    FOR SELECT USING (auth.role() = 'authenticated');
DROP POLICY IF EXISTS manage_ai_structural_roles ON ai_structural_roles;
CREATE POLICY manage_ai_structural_roles ON ai_structural_roles
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');

-- ====================================================================
-- BAGIAN J — APPROVED EXTERNAL SOURCES (Dimulai Kosong, Managed via Super Admin)
-- ====================================================================
CREATE TABLE IF NOT EXISTS approved_external_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_name TEXT NOT NULL,
    source_domain TEXT NOT NULL,
    category TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    added_by_super_admin_id UUID
);

ALTER TABLE approved_external_sources ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_approved_sources ON approved_external_sources;
CREATE POLICY read_approved_sources ON approved_external_sources
    FOR SELECT USING (auth.role() = 'authenticated');
DROP POLICY IF EXISTS manage_approved_sources ON approved_external_sources;
CREATE POLICY manage_approved_sources ON approved_external_sources
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');

-- ====================================================================
-- Audit Ledger Entry
-- ====================================================================
INSERT INTO schema_migration_audit_ledger (id, version_tag, migration_name, applied_by, notes)
VALUES (
    gen_random_uuid()::text,
    'V91',
    'fase91_master_data_reconstruction',
    'system_orchestrator',
    'Full Reconstruction of 10 Master Tables: Department Categories, Job Level Catalog, Job Sub Titles, Industry Catalog, Subscription Plans, Feature Capabilities, Creative Layout Templates, AI Job Titles, AI Structural Roles, Approved External Sources'
) ON CONFLICT (id) DO NOTHING;
