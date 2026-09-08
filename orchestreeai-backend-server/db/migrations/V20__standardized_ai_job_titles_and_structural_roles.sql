-- ====================================================================
-- OrchestreeAI Database Migration V20: Standardized 15 AI Job Titles & Structural Roles
-- Architecture: PRD Addendum 2 Section 81 (Job Title Catalog & Org Chart Integration)
-- ====================================================================

-- 1. Master AI Job Titles Catalog Table (PRD Section 81.4)
CREATE TABLE IF NOT EXISTS ai_job_titles (
    id VARCHAR(64) PRIMARY KEY,
    job_code VARCHAR(64) NOT NULL UNIQUE,
    job_name VARCHAR(128) NOT NULL,
    icon_key VARCHAR(64) NOT NULL DEFAULT 'smart_toy',
    star_rating INT DEFAULT 4,
    description TEXT NOT NULL DEFAULT '',
    maps_to_persona_type VARCHAR(64) NOT NULL,
    is_top_coordinator BOOLEAN NOT NULL DEFAULT FALSE,
    relevant_department_category VARCHAR(64) NOT NULL DEFAULT 'GENERAL',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_job_titles_code ON ai_job_titles(job_code);
CREATE INDEX IF NOT EXISTS idx_ai_job_titles_coord ON ai_job_titles(is_top_coordinator);

-- 2. Master AI Structural Roles (Sub-Jabatan) Table (PRD Section 81.4)
CREATE TABLE IF NOT EXISTS ai_structural_roles (
    id VARCHAR(64) PRIMARY KEY,
    parent_job_title_id VARCHAR(64) NOT NULL REFERENCES ai_job_titles(id) ON DELETE CASCADE,
    structural_code VARCHAR(64) NOT NULL UNIQUE,
    structural_name VARCHAR(128) NOT NULL,
    skill_summary TEXT NOT NULL DEFAULT '',
    maps_to_persona_type VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_structural_parent ON ai_structural_roles(parent_job_title_id);

-- 3. Extend ai_agents with FK references
ALTER TABLE ai_agents 
    ADD COLUMN IF NOT EXISTS job_title_id VARCHAR(64) REFERENCES ai_job_titles(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS structural_role_id VARCHAR(64) REFERENCES ai_structural_roles(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS industry_specialization VARCHAR(64) DEFAULT 'general';

-- 4. Seed Official 15 Master AI Job Titles (PRD Section 81.2)
INSERT INTO ai_job_titles (id, job_code, job_name, icon_key, star_rating, description, maps_to_persona_type, is_top_coordinator, relevant_department_category)
VALUES
('job-chief-of-staff', 'CHIEF_OF_STAFF', 'AI Chief of Staff (5 Bintang)', 'stars', 5, 'Mengoordinasikan seluruh Staff AI Agent, mensintesis Executive Briefing, riset & R&D lintas departemen, memantau kinerja perusahaan penuh (AI + Human)', 'EXECUTIVE_AGENT', TRUE, 'EXECUTIVE'),
('job-intelligence', 'COMPANY_INTELLIGENCE', 'AI Company Intelligence', 'troubleshoot', 4, 'Memantau kompetitor, tren pasar (World Monitor), sinyal prospek (Vibe Prospecting), korelasi lintas sistem perusahaan', 'INTELLIGENCE_AGENT', FALSE, 'STRATEGY'),
('job-operations', 'OPERATIONS', 'AI Operations', 'precision_manufacturing', 4, 'Memantau produksi, equipment, fleet, kualitas, downtime, efisiensi operasional harian', 'OPERATIONS_AGENT', FALSE, 'OPERATIONS'),
('job-sales', 'SALES', 'AI Sales', 'point_of_sale', 4, 'Discovery, rekomendasi produk, upsell/cross-sell, objection handling, closing', 'SALES_CONSULTANT', FALSE, 'SALES'),
('job-customer-service', 'CUSTOMER_SERVICE', 'AI Customer Service', 'support_agent', 4, 'Menjawab FAQ, menangani komplain, refund, return, warranty, eskalasi ke manusia bila perlu', 'CUSTOMER_SUCCESS', FALSE, 'CUSTOMER_SERVICE'),
('job-hr', 'HR_RECRUITMENT', 'AI HR & Recruitment', 'badge', 4, 'Analisis kebutuhan tenaga kerja, monitoring kehadiran, rekrutmen, employee data dari HRIS', 'HR_AGENT', FALSE, 'HR'),
('job-finance', 'FINANCE', 'AI Finance', 'account_balance', 4, 'Cash flow analysis, AR/AP, budget variance, revenue forecasting', 'FINANCE_AGENT', FALSE, 'FINANCE'),
('job-marketing', 'MARKETING', 'AI Marketing', 'campaign', 4, 'Membuat & menjalankan campaign, content generation, brand voice, atribusi revenue', 'MARKETING_AGENT', FALSE, 'MARKETING'),
('job-knowledge', 'KNOWLEDGE_DOCUMENT', 'AI Knowledge & Document', 'menu_book', 4, 'Mengelola Company Brain, ekstraksi dokumen (SOP/manual/kontrak), menjaga akurasi pengetahuan perusahaan', 'RESEARCH_AGENT', FALSE, 'LEGAL_COMPLIANCE'),
('job-task-workflow', 'TASK_WORKFLOW', 'AI Task & Workflow', 'account_tree', 4, 'Membuat task otomatis, memonitor closed-loop, mengelola papan kerja Human+AI', 'WORKFLOW_ORCHESTRATOR', FALSE, 'OPERATIONS'),
('job-crm-success', 'CRM_CUSTOMER_SUCCESS', 'AI CRM & Customer Success', 'loyalty', 4, 'Mengelola relasi pelanggan jangka panjang, retensi, kesehatan akun, reaktivasi', 'RETENTION_AGENT', FALSE, 'SALES'),
('job-procurement', 'PROCUREMENT', 'AI Procurement', 'shopping_cart_checkout', 4, 'Memantau PO, RFQ, supplier, delivery, stok kritikal', 'PROCUREMENT_AGENT', FALSE, 'PROCUREMENT'),
('job-project', 'PROJECT', 'AI Project', 'engineering', 4, 'Project Health Score, jadwal, biaya, manpower, deteksi risiko keterlambatan', 'PROJECT_AGENT', FALSE, 'PROJECT_MANAGEMENT'),
('job-research', 'RESEARCH', 'AI Research', 'biotech', 4, 'Riset mendalam sesuai Knowledge Priority Hierarchy, menemukan pola & mengusulkan Knowledge Rule baru', 'RESEARCH_AGENT', FALSE, 'R_AND_D'),
('job-reporting', 'REPORTING', 'AI Reporting', 'analytics', 4, 'Menyusun Daily/Weekly/Monthly/Management Report otomatis, proactive reporting, visualisasi data', 'REPORTING_AGENT', FALSE, 'EXECUTIVE')
ON CONFLICT (job_code) DO UPDATE SET
    job_name = EXCLUDED.job_name,
    icon_key = EXCLUDED.icon_key,
    description = EXCLUDED.description,
    is_top_coordinator = EXCLUDED.is_top_coordinator,
    relevant_department_category = EXCLUDED.relevant_department_category;

-- 5. Seed Official Structural Sub-Roles (PRD Section 81.3)
INSERT INTO ai_structural_roles (id, parent_job_title_id, structural_code, structural_name, skill_summary, maps_to_persona_type)
VALUES
('role-sdr', 'job-sales', 'AI_SDR', 'AI SDR', 'Lead qualification & discovery pertanyaan kebutuhan komersial', 'SDR'),
('role-closer', 'job-sales', 'AI_CLOSER', 'AI Closer', 'Objection handling, kalkulasi diskon & pembuatan order checkout', 'CLOSER'),
('role-product-advisor', 'job-sales', 'AI_PRODUCT_ADVISOR', 'AI Product Advisor', 'Rekomendasi varian produk katalog & cross-selling', 'PRODUCT_ADVISOR'),
('role-receptionist', 'job-customer-service', 'AI_RECEPTIONIST', 'AI Receptionist', 'Penyambutan ramah pesan masuk multi-channel & intent routing', 'RECEPTIONIST'),
('role-maintenance-officer', 'job-operations', 'AI_MAINTENANCE_OFFICER', 'AI Maintenance Officer', 'Predictive failure analysis & CMMS telemetry monitor', 'MAINTENANCE_AGENT'),
('role-fleet-analyst', 'job-operations', 'AI_FLEET_ANALYST', 'AI Fleet Analyst', 'Fuel consumption monitoring & GPS/FMS telemetry anomaly', 'FLEET_AGENT'),
('role-cashflow-analyst', 'job-finance', 'AI_CASH_FLOW_ANALYST', 'AI Cash Flow Analyst', 'Deteksi tekanan arus kas & aging invoice AP/AR', 'FINANCE_AGENT'),
('role-campaign-specialist', 'job-marketing', 'AI_CAMPAIGN_SPECIALIST', 'AI Campaign Specialist', 'Natural language audience segmentation & content scheduler', 'MARKETING_AGENT')
ON CONFLICT (structural_code) DO UPDATE SET
    structural_name = EXCLUDED.structural_name,
    skill_summary = EXCLUDED.skill_summary;
