-- ====================================================================
-- OrchestreeAI Database Migration V2: Official System Configuration Seed
-- Seeds ONLY real system configurations (Roles, Permissions, Plans, Master Tools)
-- STRICTLY ZERO FAKE OPERATIONAL DATA per PRD and Core Rules
-- ====================================================================

-- 1. Master System Roles (Tabel 4.1)
INSERT INTO roles (id, name, label, description, is_system_role) VALUES
('role-super-admin', 'SUPER_ADMIN', 'Super Admin', 'Global Platform Control & Multi-Tenant Management', TRUE),
('role-tenant-owner', 'TENANT_OWNER', 'Tenant Owner', 'Company & Billing Owner with Full Workspace Authority', TRUE),
('role-tenant-admin', 'TENANT_ADMIN', 'Tenant Admin / HR', 'Staff, Department & Operations Administration', TRUE),
('role-dept-manager', 'DEPT_MANAGER', 'Dept Manager', 'Department Tasks, Resource Allocation & Risk Approvals', TRUE),
('role-staff-human', 'STAFF_HUMAN', 'Staff Human', 'Operational Worker executing collaborative tasks', TRUE),
('role-ai-agent', 'AI_AGENT', 'AI Agent', 'Autonomous Digital Worker executing bounded workflows', TRUE)
ON CONFLICT (name) DO NOTHING;

-- 2. Master Capability Matrix (Tabel 4.2 Matriks Akses Fitur Utama)
INSERT INTO permissions (id, capability_string, category, description) VALUES
('perm-01', 'dashboard.view', 'Dashboard', 'Melihat dashboard operasional perusahaan'),
('perm-02', 'dashboard.superadmin', 'Platform', 'Akses ke Super Admin Control Plane global'),
('perm-03', 'task.create', 'Task & Workflow', 'Membuat task dan dispatch workflow intent baru'),
('perm-04', 'task.assign_agent', 'Task & Workflow', 'Menugaskan task ke AI Agent tertentu'),
('perm-05', 'task.approve_high_risk', 'Governance', 'Memberikan persetujuan (human approval) untuk task berisiko tinggi'),
('perm-06', 'workforce.view', 'Workforce', 'Melihat daftar tenaga kerja Human & AI'),
('perm-07', 'workforce.manage_human', 'Workforce', 'Mengelola staff, departemen, dan undangan akun'),
('perm-08', 'workforce.manage_agent', 'Workforce', 'Mengonfigurasi AI Agent, skill, dan tool granting'),
('perm-09', 'competitor.view', 'Competitor Intel', 'Melihat target pemantauan dan insight kompetitor'),
('perm-10', 'competitor.add_target', 'Competitor Intel', 'Mendaftarkan target kompetitor baru'),
('perm-11', 'competitor.trigger_scan', 'Competitor Intel', 'Memicu pemindaian inteligensi on-demand'),
('perm-12', 'analytics.view_tenant', 'Analytics', 'Melihat leaderboard performa tenant'),
('perm-13', 'analytics.view_global', 'Platform', 'Melihat metrik agregat platform global'),
('perm-14', 'integration.view', 'Integrations', 'Melihat status koneksi platform eksternal'),
('perm-15', 'integration.manage', 'Integrations', 'Menghubungkan atau memutus integrasi API pihak ketiga'),
('perm-16', 'proactive.view', 'Proactive Messaging', 'Melihat jadwal dan log pesan proaktif'),
('perm-17', 'proactive.manage', 'Proactive Messaging', 'Mengatur kanal WhatsApp/Telegram dan jadwal pesan'),
('perm-18', 'brain.view', 'Company Brain', 'Pencarian semantik dan membaca knowledge base perusahaan'),
('perm-19', 'brain.write_sop', 'Company Brain', 'Menambah dan mengedit SOP atau memori permanen'),
('perm-20', 'admin.rbac_matrix_edit', 'Governance', 'Mengubah matriks permission role dinamis'),
('perm-21', 'admin.mcp_killswitch', 'Security', 'Mengaktifkan atau menonaktifkan killswitch MCP tool global'),
('perm-22', 'admin.llm_routing_edit', 'AI Governance', 'Mengonfigurasi aturan routing LLM multi-provider'),
('perm-23', 'billing.view', 'Billing', 'Melihat tagihan dan penggunaan kuota LLM'),
('perm-24', 'billing.manage', 'Billing', 'Mengubah paket langganan dan metode pembayaran')
ON CONFLICT (capability_string) DO NOTHING;

-- Bind Permissions to SUPER_ADMIN (All capabilities)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-super-admin', id FROM permissions
ON CONFLICT DO NOTHING;

-- Bind Permissions to TENANT_OWNER
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-tenant-owner', id FROM permissions
WHERE capability_string IN (
    'dashboard.view', 'task.create', 'task.assign_agent', 'task.approve_high_risk',
    'workforce.view', 'workforce.manage_human', 'workforce.manage_agent',
    'competitor.view', 'competitor.add_target', 'competitor.trigger_scan',
    'analytics.view_tenant', 'integration.view', 'integration.manage',
    'proactive.view', 'proactive.manage', 'brain.view', 'brain.write_sop',
    'billing.view', 'billing.manage'
)
ON CONFLICT DO NOTHING;

-- Bind Permissions to TENANT_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-tenant-admin', id FROM permissions
WHERE capability_string IN (
    'dashboard.view', 'task.create', 'task.assign_agent', 'task.approve_high_risk',
    'workforce.view', 'workforce.manage_human', 'workforce.manage_agent',
    'competitor.view', 'analytics.view_tenant', 'integration.view',
    'proactive.view', 'proactive.manage', 'brain.view', 'brain.write_sop', 'billing.view'
)
ON CONFLICT DO NOTHING;

-- Bind Permissions to DEPT_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-dept-manager', id FROM permissions
WHERE capability_string IN (
    'dashboard.view', 'task.create', 'task.assign_agent', 'task.approve_high_risk',
    'workforce.view', 'competitor.view', 'analytics.view_tenant',
    'integration.view', 'proactive.view', 'brain.view', 'brain.write_sop'
)
ON CONFLICT DO NOTHING;

-- Bind Permissions to STAFF_HUMAN
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-staff-human', id FROM permissions
WHERE capability_string IN (
    'dashboard.view', 'task.create', 'workforce.view',
    'competitor.view', 'analytics.view_tenant', 'brain.view', 'proactive.view'
)
ON CONFLICT DO NOTHING;

-- Bind Permissions to AI_AGENT
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-ai-agent', id FROM permissions
WHERE capability_string IN (
    'task.create', 'competitor.trigger_scan', 'brain.view'
)
ON CONFLICT DO NOTHING;

-- 3. Official Subscription Plans (PRD Section 5.1 Pricing)
INSERT INTO subscription_plans (id, name, tier, price_idr_monthly, price_idr_yearly, max_agents, max_human_seats, monthly_llm_token_limit, features_json, is_active) VALUES
(
    'plan-starter',
    'Starter Growth',
    'STARTER',
    2499000.00,
    24990000.00,
    2,
    5,
    5000000,
    '["2 AI Agents Aktif", "Hingga 5 Human Staff", "Kanban & Task Management", "Pemantauan 3 Target Kompetitor", "Integrasi WhatsApp Proaktif", "Support Komunitas"]'::jsonb,
    TRUE
),
(
    'plan-pro',
    'Professional Scale',
    'PROFESSIONAL',
    5999000.00,
    59990000.00,
    6,
    20,
    20000000,
    '["6 AI Agents Multidisiplin", "Hingga 20 Human Staff", "World Monitor Kompetitor Realtime", "Human-in-the-Loop Approval Gate", "Company Brain Semantic Search", "WhatsApp & Telegram Proaktif", "Prioritas Latensi LLM", "Dedicated Account Specialist"]'::jsonb,
    TRUE
),
(
    'plan-enterprise',
    'Enterprise Sovereign',
    'ENTERPRISE',
    14999000.00,
    149990000.00,
    25,
    100,
    100000000,
    '["Unlimited AI Workforce Cluster", "Custom Human Seats", "Row-Level Security Terisolasi", "Multi-LLM Sovereign Routing (Gemini/Claude/OpenAI)", "Custom MCP Tool Sandboxing", "SLA Uptime 99.9% Bergaransi", "Audit Log Kepatuhan SOC2/ISO", "Private Model Hosting Support"]'::jsonb,
    TRUE
)
ON CONFLICT (id) DO UPDATE SET
    price_idr_monthly = EXCLUDED.price_idr_monthly,
    price_idr_yearly = EXCLUDED.price_idr_yearly,
    features_json = EXCLUDED.features_json;

-- 4. Master MCP Tools Official Definitions
INSERT INTO mcp_tools (id, name, version, description, category, input_schema_json, risk_level, is_enabled_globally, total_invocations, error_rate_pct, is_kill_switched) VALUES
('tool-web-fetch', 'web.fetch', 'v2.1.0', 'Mengambil dan mem-parsing konten halaman web publik terstruktur sesuai robots.txt', 'Scraping & Ingestion', '{"url": "string", "extract_text": "boolean"}'::jsonb, 'LOW', TRUE, 0, 0.0, FALSE),
('tool-mkt-scrape', 'marketplace.scrape', 'v1.8.0', 'Menginspeksi katalog produk, harga publik, dan varian e-commerce', 'Competitive Intel', '{"target_url": "string", "category": "string"}'::jsonb, 'MEDIUM', TRUE, 0, 0.0, FALSE),
('tool-image-gen', 'image_generation.create', 'v3.0.0', 'Menghasilkan visual kreatif berkualitas tinggi sesuai prompt dan tema brand', 'Creative Studio', '{"prompt": "string", "aspect_ratio": "string"}'::jsonb, 'LOW', TRUE, 0, 0.0, FALSE),
('tool-wa-send', 'whatsapp.send', 'v2.4.0', 'Mengirim pesan WhatsApp terverifikasi melalui Cloud API resmi Meta', 'Proactive Messaging', '{"phone": "string", "template": "string", "params": "object"}'::jsonb, 'HIGH', TRUE, 0, 0.0, FALSE),
('tool-tg-send', 'telegram.send', 'v1.6.0', 'Mengirim notifikasi dan insight ke Telegram Bot per staff chat ID', 'Proactive Messaging', '{"chat_id": "string", "message": "string"}'::jsonb, 'MEDIUM', TRUE, 0, 0.0, FALSE),
('tool-brain-query', 'company_brain.query', 'v2.0.0', 'Pencarian semantik pada SOP, brand guidelines, dan memori permanen perusahaan', 'Semantic Memory', '{"query": "string", "top_k": "integer"}'::jsonb, 'LOW', TRUE, 0, 0.0, FALSE)
ON CONFLICT (name) DO NOTHING;

-- 5. Master LLM Routing Official Rules
INSERT INTO llm_routing_rules (id, task_type, preferred_provider, model_name, fallback_provider, max_tokens, temperature, cost_per_million_tokens_usd) VALUES
('rule-reasoning', 'Complex Reasoning & Planning', 'GOOGLE_GEMINI', 'gemini-3.1-pro-preview', 'ANTHROPIC', 8192, 0.20, 1.2500),
('rule-fast-exec', 'Classification & Fast Execution', 'GOOGLE_GEMINI', 'gemini-3.5-flash', 'OPENAI', 4096, 0.50, 0.1500),
('rule-creative-image', 'Image Generation & Creative Studio', 'GOOGLE_GEMINI', 'gemini-2.5-flash-image', 'OPENAI', 2048, 0.70, 2.0000),
('rule-proactive-brief', 'Proactive Brief & Personalization', 'GOOGLE_GEMINI', 'gemini-3.5-flash', 'ANTHROPIC', 2048, 0.40, 0.1500)
ON CONFLICT (task_type) DO NOTHING;
