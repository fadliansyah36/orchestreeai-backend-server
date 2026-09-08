-- ====================================================================
-- OrchestreeAI Database Migration V3: Core Workforce & Task Board Schema
-- Compliant with PRD Section 8.4, 5.2.1, and 16.2
-- ====================================================================

-- 1. Departments enhancements (Soft delete)
ALTER TABLE departments ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ DEFAULT NULL;

-- 2. Staff Profiles
CREATE TABLE IF NOT EXISTS staff_profiles (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    department_id VARCHAR(64) REFERENCES departments(id) ON DELETE SET NULL,
    job_title VARCHAR(128) NOT NULL DEFAULT 'Staff Operasional',
    phone VARCHAR(32) DEFAULT '',
    telegram_chat_id VARCHAR(64) DEFAULT '',
    avatar_url TEXT DEFAULT '',
    skills TEXT DEFAULT '',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Agent Skills & Assignments
CREATE TABLE IF NOT EXISTS agent_skills (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    category VARCHAR(64) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_skill_assignments (
    agent_id VARCHAR(64) NOT NULL REFERENCES ai_agents(id) ON DELETE CASCADE,
    skill_id VARCHAR(64) NOT NULL REFERENCES agent_skills(id) ON DELETE CASCADE,
    proficiency_pct INT NOT NULL DEFAULT 95,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (agent_id, skill_id)
);

-- 4. Kanban Boards & Board Columns
CREATE TABLE IF NOT EXISTS boards (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL,
    description TEXT DEFAULT '',
    is_default BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS board_columns (
    id VARCHAR(64) PRIMARY KEY,
    board_id VARCHAR(64) NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(64) NOT NULL,
    label VARCHAR(128) NOT NULL,
    position INT NOT NULL DEFAULT 0,
    color_hex VARCHAR(16) NOT NULL DEFAULT '#1E6FE0',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Task Events, Comments & Attachments
CREATE TABLE IF NOT EXISTS task_events (
    id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    actor_id VARCHAR(64) NOT NULL,
    actor_name VARCHAR(128) NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    from_column VARCHAR(64),
    to_column VARCHAR(64),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_comments (
    id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    author_id VARCHAR(64) NOT NULL,
    author_name VARCHAR(128) NOT NULL,
    author_role VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_attachments (
    id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    uploaded_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Official Master Agent Skills Seed
INSERT INTO agent_skills (id, name, category, description) VALUES
('skill-web-scraping', 'Web Scraping & DOM Parsing', 'Data Ingestion', 'Kemampuan mengekstrak data dari web publik sesuai robots.txt'),
('skill-market-intel', 'Marketplace & Price Monitoring', 'Competitive Intel', 'Kemampuan mendeteksi pergeseran harga dan diskon e-commerce'),
('skill-content-copy', 'Copywriting & Content Ideation', 'Creative Marketing', 'Kemampuan menyusun naskah iklan, artikel, dan caption bernada brand'),
('skill-image-design', 'Visual Asset & Image Synthesis', 'Creative Studio', 'Kemampuan menghasilkan visual promosi dengan prompt visual terkontrol'),
('skill-proactive-wa', 'WhatsApp Cloud Notification', 'Proactive Ops', 'Kemampuan menyusun pesan brief dan dispatch ke nomor terverifikasi Meta'),
('skill-proactive-tg', 'Telegram Bot Dispatch', 'Proactive Ops', 'Kemampuan mengirim notifikasi real-time ke grup / chat ID staff'),
('skill-brain-query', 'Company Brain Semantic Retrieval', 'Knowledge Management', 'Kemampuan mencari SOP dan dokumen perusahaan menggunakan vektor embedding 768-dim'),
('skill-risk-eval', 'High-Risk Action Governance', 'Security & Compliance', 'Kemampuan mengevaluasi risiko tindakan dan meminta persetujuan manusia')
ON CONFLICT (name) DO NOTHING;

-- 7. Enable RLS on New Tables
ALTER TABLE staff_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE boards ENABLE ROW LEVEL SECURITY;
ALTER TABLE board_columns ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_attachments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_staff ON staff_profiles;
DROP POLICY IF EXISTS tenant_isolation_staff ON staff_profiles;
CREATE POLICY tenant_isolation_staff ON staff_profiles USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_boards ON boards;
DROP POLICY IF EXISTS tenant_isolation_boards ON boards;
CREATE POLICY tenant_isolation_boards ON boards USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_board_columns ON board_columns;
DROP POLICY IF EXISTS tenant_isolation_board_columns ON board_columns;
CREATE POLICY tenant_isolation_board_columns ON board_columns USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_task_events ON task_events;
DROP POLICY IF EXISTS tenant_isolation_task_events ON task_events;
CREATE POLICY tenant_isolation_task_events ON task_events USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_task_comments ON task_comments;
DROP POLICY IF EXISTS tenant_isolation_task_comments ON task_comments;
CREATE POLICY tenant_isolation_task_comments ON task_comments USING (app_has_tenant_access(tenant_id));

DROP POLICY IF EXISTS tenant_isolation_task_attachments ON task_attachments;
DROP POLICY IF EXISTS tenant_isolation_task_attachments ON task_attachments;
CREATE POLICY tenant_isolation_task_attachments ON task_attachments USING (app_has_tenant_access(tenant_id));
