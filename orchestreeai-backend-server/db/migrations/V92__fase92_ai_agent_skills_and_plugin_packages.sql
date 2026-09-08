-- ====================================================================
-- OrchestreeAI Database Migration V92 / Fase 92
-- AI AGENT SKILLS, CAPABILITIES & PLUGIN PACKAGES ENGINE
-- Architecture: PRD Master, Addendum 1, Addendum 2 (Enterprise AI Workforce)
-- ====================================================================

-- 1. AI SKILL PLUGIN PACKAGES TABLE (Bagian C)
CREATE TABLE IF NOT EXISTS ai_skill_plugin_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_name TEXT NOT NULL,
    source_origin TEXT DEFAULT 'manual_upload',   -- 'github_zip'/'manual_upload'/'other'
    original_zip_url TEXT,                         -- referensi asal (link release / path)
    manifest_data JSONB NOT NULL,
    skill_definition_content TEXT NOT NULL,         -- isi skill_definition.md
    tool_manifests JSONB,                          -- daftar tool custom jika ada
    storage_zip_path TEXT NOT NULL,                 -- lokasi file .zip asli di Supabase Storage
    validation_status TEXT DEFAULT 'pending',       -- pending/validated/rejected
    validation_notes TEXT,
    uploaded_by_super_admin_id UUID,
    uploaded_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE ai_skill_plugin_packages ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS manage_plugin_packages ON ai_skill_plugin_packages;
CREATE POLICY manage_plugin_packages ON ai_skill_plugin_packages 
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');
DROP POLICY IF EXISTS read_plugin_packages ON ai_skill_plugin_packages;
CREATE POLICY read_plugin_packages ON ai_skill_plugin_packages 
    FOR SELECT USING (auth.role() = 'authenticated');

-- 2. AI AGENT SKILLS TABLE (Bagian A)
CREATE TABLE IF NOT EXISTS ai_agent_skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_code TEXT UNIQUE NOT NULL,
    skill_name TEXT NOT NULL,
    skill_category TEXT NOT NULL,                  -- 'core'/'plugin'/'library'
    description TEXT NOT NULL,
    applicable_job_title_ids UUID[],              -- job title mana saja yang bisa memakai skill ini
    applicable_structural_role_ids UUID[],         -- structural role mana saja yang bisa memakai skill ini
    required_mcp_tools TEXT[],                     -- daftar tool_code dari MCP Tool Registry
    is_active BOOLEAN DEFAULT TRUE,
    source_type TEXT DEFAULT 'built_in',           -- 'built_in'/'uploaded_plugin'
    plugin_package_id UUID REFERENCES ai_skill_plugin_packages(id) ON DELETE SET NULL,
    skill_definition_content TEXT DEFAULT '',
    risk_tier TEXT DEFAULT 'low',
    version TEXT DEFAULT '1.0.0',
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE ai_agent_skills ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS read_ai_agent_skills ON ai_agent_skills;
CREATE POLICY read_ai_agent_skills ON ai_agent_skills 
    FOR SELECT USING (auth.role() = 'authenticated');
DROP POLICY IF EXISTS manage_ai_agent_skills ON ai_agent_skills;
CREATE POLICY manage_ai_agent_skills ON ai_agent_skills 
    FOR ALL USING (current_user_role() = 'SUPER_ADMIN');

-- 3. SEED BUILT-IN CORE SKILLS FOR ALL 15 MAIN JOB TITLES & STRUCTURAL ROLES
-- 3.1 Chief of Staff
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'executive_synthesis', 'Executive Briefing Synthesis', 'core',
  'Mensintesis data lintas Specialist Agent dan performa Human menjadi Executive Briefing harian & mingguan',
  ARRAY[jt.id], NULL, ARRAY['analytics.query','memory.search','notification.send'], 'built_in',
  '## EXECUTIVE SYNTHESIS DIRECTIVE\nAnalyze high-level cross-department KPIs and human-AI collaboration matrices. Condense critical risks, achievements, and actionable leadership decisions into clear executive briefings.'
FROM ai_job_titles jt WHERE jt.job_code = 'chief_of_staff'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'research_directive', 'Proactive Research Directive', 'core',
  'Menjadwalkan riset lintas departemen dan mendeteksi pola berulang untuk diusulkan menjadi Knowledge Rule',
  ARRAY[jt.id], NULL, ARRAY['memory.search','knowledge_rule.propose'], 'built_in',
  '## RESEARCH DIRECTIVE INSTRUCTION\nIdentify organizational knowledge gaps and initiate systematic research directives according to Knowledge Priority Hierarchy.'
FROM ai_job_titles jt WHERE jt.job_code = 'chief_of_staff'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'cross_agent_orchestration', 'Cross-Agent Multi-Department Orchestration', 'core',
  'Mengoordinasikan delegasi tugas multi-agent swarm secara berurutan dan paralel',
  ARRAY[jt.id], NULL, ARRAY['task.create','task.assign','workflow.dispatch'], 'built_in',
  '## MULTI-AGENT SWARM ORCHESTRATION\nCoordinate specialist agents across departments with clear handoff protocols and feedback loops.'
FROM ai_job_titles jt WHERE jt.job_code = 'chief_of_staff'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.2 AI Company Intelligence
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'competitor_monitoring', 'Competitor Intelligence & Price Crawling', 'core',
  'Memantau pergerakan harga, katalog, dan kampanye promosi kompetitor di marketplace & web',
  ARRAY[jt.id], NULL, ARRAY['web.fetch','marketplace.scrape','analytics.query'], 'built_in',
  '## COMPETITOR INTELLIGENCE PROTOCOL\nExtract and normalize competitor pricing, SKU adjustments, and promotion signals to detect market threats.'
FROM ai_job_titles jt WHERE jt.job_code = 'company_intelligence'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'world_trend_monitoring', 'World Monitor & Industry Trends', 'core',
  'Menganalisis klaster berita global, tren regulasi, dan sinyal ekonomi makro',
  ARRAY[jt.id], NULL, ARRAY['world_monitor.trend_lookup','news.fetch'], 'built_in',
  '## WORLD MONITORING ENGINE\nCluster global news and macroeconomic signals into actionable corporate risks and strategic opportunities.'
FROM ai_job_titles jt WHERE jt.job_code = 'company_intelligence'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'vibe_prospecting', 'Vibe Prospecting & Signal Analysis', 'core',
  'Mendeteksi sinyal kebutuhan dan minat pembelian prospek dari media sosial dan kanal publik',
  ARRAY[jt.id], NULL, ARRAY['vibe.analyze','social.fetch'], 'built_in',
  '## VIBE PROSPECTING PROTOCOL\nEvaluate sentiment, intent tags, and buying authority signals to generate high-propensity prospect leads.'
FROM ai_job_titles jt WHERE jt.job_code = 'company_intelligence'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.3 AI Sales
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'lead_qualification', 'Lead Qualification & Scoring', 'core',
  'Kualifikasi dan skoring lead dari percakapan sales secara otomatis (BANT framework)',
  ARRAY[jt.id], NULL, ARRAY['lead.create','lead.update_score'], 'built_in',
  '## LEAD QUALIFICATION & BANT SCORING\nIdentify Budget, Authority, Need, and Timeline from conversational inputs. Automatically score lead quality.'
FROM ai_job_titles jt WHERE jt.job_code = 'sales'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'sales_discovery', 'Conversational Sales Discovery', 'core',
  'Penggalian kebutuhan mendalam calon pelanggan berbasis konteks industri',
  ARRAY[jt.id], NULL, ARRAY['knowledge.lookup','customer.get_profile'], 'built_in',
  '## SALES DISCOVERY GUIDELINE\nAsk open-ended probing questions to reveal pain points and recommend matching solutions from catalog.'
FROM ai_job_titles jt WHERE jt.job_code = 'sales'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'product_recommendation', 'Product & Variant Recommendation', 'core',
  'Rekomendasi spesifikasi produk dan varian persis sesuai stok real-time',
  ARRAY[jt.id], NULL, ARRAY['product.search','product.recommend','inventory.check_stock'], 'built_in',
  '## PRODUCT RECOMMENDATION ENGINE\nMatch customer requirements to active inventory SKUs. Validate live stock quantities before promising availability.'
FROM ai_job_titles jt WHERE jt.job_code = 'sales'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'upsell_cross_sell', 'Intelligent Upsell, Cross-Sell & Bundling', 'core',
  'Menyarankan add-on, bundling produk komplementer, dan paket hemat yang relevan',
  ARRAY[jt.id], NULL, ARRAY['product.get_upsell_candidates','product.get_complementary','product.get_bundle_options'], 'built_in',
  '## UPSELL & BUNDLE ENGINE\nProactively suggest complementary accessories, warranty upgrades, or volume bundles during purchase consideration.'
FROM ai_job_titles jt WHERE jt.job_code = 'sales'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'objection_handling', 'Objection Handling & Sales Playbook', 'core',
  'Menjawab keberatan harga, komparasi produk kompetitor, dan garansi dengan playbook resmi',
  ARRAY[jt.id], NULL, ARRAY['knowledge.lookup','playbook.search'], 'built_in',
  '## OBJECTION HANDLING PLAYBOOK\nEmpathize, reframe value proposition, provide official guarantees, and present flexible payment terms.'
FROM ai_job_titles jt WHERE jt.job_code = 'sales'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'order_closing', 'Order Closing & Quotation Generation', 'core',
  'Membuat penawaran resmi (Quotation), keranjang belanja (Cart), dan checkout invoice',
  ARRAY[jt.id], NULL, ARRAY['cart.create','order.create','quotation.create'], 'built_in',
  '## ORDER CLOSING PROTOCOL\nGenerate structured quotation items, apply authorized discount matrices, and issue payment link securely.'
FROM ai_job_titles jt WHERE jt.job_code = 'sales'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'abandoned_cart_recovery', 'Abandoned Cart Recovery & Re-engagement', 'core',
  'Menghubungi kembali prospek dengan keranjang tertunda menggunakan pesan persuasif',
  ARRAY[jt.id], NULL, ARRAY['notification.send','conversation.reply'], 'built_in',
  '## CART RECOVERY DIRECTIVE\nRe-engage abandoned carts with gentle reminder nudges, addressing doubts, and time-limited checkout incentives.'
FROM ai_job_titles jt WHERE jt.job_code = 'sales'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.4 AI Marketing
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'campaign_execution', 'Natural Language Campaign Execution', 'core',
  'Membuat dan mengeksekusi kampanye pemasaran multi-channel berbasis segmentasi audiens dinamis',
  ARRAY[jt.id], NULL, ARRAY['campaign.create','customer.segment_query','campaign.send'], 'built_in',
  '## CAMPAIGN EXECUTION PROTOCOL\nTranslate marketing goals into targeted audience filters, localized message copy, and automated delivery schedules.'
FROM ai_job_titles jt WHERE jt.job_code = 'marketing'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'content_generation', 'Brand-Grounded Content Generation', 'core',
  'Membuat artikel, caption, copy promo, dan materi marketing sesuai brand voice perusahaan',
  ARRAY[jt.id], NULL, ARRAY['content.generate','brand.guideline_lookup'], 'built_in',
  '## BRAND-GROUNDED COPYWRITING\nAdhere strictly to company tone, value propositions, approved terminology, and forbidden claims.'
FROM ai_job_titles jt WHERE jt.job_code = 'marketing'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'visual_asset_prompting', 'Visual Creative Studio Prompting', 'core',
  'Merancang prompt visual berkualitas tinggi untuk AI Image Generator sesuai template brand',
  ARRAY[jt.id], NULL, ARRAY['image_generation.create'], 'built_in',
  '## VISUAL PROMPT GENERATOR\nCraft compositionally rich prompts with exact lighting, camera aspect ratio, palette, and typography placeholders.'
FROM ai_job_titles jt WHERE jt.job_code = 'marketing'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'social_scheduler', 'Multi-Platform Post Scheduling', 'core',
  'Menjadwalkan publikasi konten media sosial otomatis ke Instagram, LinkedIn, Facebook, dan X',
  ARRAY[jt.id], NULL, ARRAY['integration.schedule_post','content_calendar.save'], 'built_in',
  '## SOCIAL SCHEDULER ENGINE\nDistribute approved visual calendar items to authenticated platform channels at optimal engagement windows.'
FROM ai_job_titles jt WHERE jt.job_code = 'marketing'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.5 AI Customer Service
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'omnichannel_reception', 'Omnichannel Reception & Intent Routing', 'core',
  'Menyambut pesan masuk dari seluruh kanal (WhatsApp, Telegram, Web Widget) dan routing intent',
  ARRAY[jt.id], NULL, ARRAY['conversation.reply','knowledge.lookup'], 'built_in',
  '## OMNICHANNEL RECEPTION\nInstantly greet incoming conversations with warm empathy, detect language, identify user intention, and answer queries.'
FROM ai_job_titles jt WHERE jt.job_code = 'customer_service'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'complaint_resolution', 'Complaint & Service Request Management', 'core',
  'Menangani komplain, garansi, return barang, dan menerbitkan tiket Service Request terstruktur',
  ARRAY[jt.id], NULL, ARRAY['service_request.create','knowledge.lookup'], 'built_in',
  '## COMPLAINT RESOLUTION PROTOCOL\nAcknowledge customer frustration calmly, gather problem evidence, verify warranty status, and generate service tickets.'
FROM ai_job_titles jt WHERE jt.job_code = 'customer_service'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'human_handover_escalation', 'Context-Aware Human Handover', 'core',
  'Eskalasi mulus ke agen manusia ketika percakapan memerlukan penanganan emosional atau otorisasi tinggi',
  ARRAY[jt.id], NULL, ARRAY['handover.escalate','notification.send'], 'built_in',
  '## CONTEXT-AWARE HANDOVER\nSummarize conversation history, tag urgency level, and notify human supervisor while keeping user reassured.'
FROM ai_job_titles jt WHERE jt.job_code = 'customer_service'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.6 AI CRM & Customer Success
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'order_tracking_updates', 'Realtime Order & Shipping Tracking', 'core',
  'Memberikan informasi status pesanan, resi pengiriman, dan estimasi tiba secara akurat',
  ARRAY[jt.id], NULL, ARRAY['order.get_status','shipping.get_tracking'], 'built_in',
  '## SHIPPING TRACKING ENGINE\nFetch live courier milestone logs and communicate transit status clearly with delivery ETA.'
FROM ai_job_titles jt WHERE jt.job_code = 'crm_customer_success'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'customer_retention', 'Customer Retention & Habitual Reorder Reminder', 'core',
  'Mendeteksi siklus habis pakai produk pelanggan dan mengirimkan pengingat reorder otomatis',
  ARRAY[jt.id], NULL, ARRAY['customer.get_purchase_history','campaign.trigger'], 'built_in',
  '## REORDER RETENTION CADENCE\nCalculate average product consumption cycle per customer and send personalized reorder reminders before stock runs out.'
FROM ai_job_titles jt WHERE jt.job_code = 'crm_customer_success'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.7 AI Operations
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'maintenance_telemetry', 'Equipment Telemetry & Predictive Maintenance', 'core',
  'Memantau sensor IoT alat berat/mesin, getaran, suhu, dan memprediksi kebutuhan maintenance',
  ARRAY[jt.id], NULL, ARRAY['cmms.get_equipment','sensor.read_telemetry'], 'built_in',
  '## PREDICTIVE MAINTENANCE TELEMETRY\nAnalyze equipment run hours, vibration harmonics, and thermal anomalies to trigger preventative work orders.'
FROM ai_job_titles jt WHERE jt.job_code = 'operations'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'fleet_analysis', 'Fleet Telematics & Fuel Anomaly Detection', 'core',
  'Memantau konsumsi BBM armada, rute GPS, idle time, dan mendeteksi anomali bahan bakar',
  ARRAY[jt.id], NULL, ARRAY['fleet.get_metrics','gps.track'], 'built_in',
  '## FLEET TELEMATICS & FUEL AUDIT\nCorrelate odometer distances with fuel tank level sensors to pinpoint unauthorized fuel siphoning and idling.'
FROM ai_job_titles jt WHERE jt.job_code = 'operations'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'downtime_anomaly_detection', 'Downtime & Production Anomaly Detection', 'core',
  'Mendeteksi bottleneck jalur produksi, stop line darurat, dan deviasi target output',
  ARRAY[jt.id], NULL, ARRAY['operations.get_metrics','alert.publish'], 'built_in',
  '## PRODUCTION BOTTLENECK MONITOR\nTrack hourly manufacturing output vs planned targets to flag line stoppage root causes immediately.'
FROM ai_job_titles jt WHERE jt.job_code = 'operations'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.8 AI HR & Recruitment
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'candidate_screening', 'Automated Candidate Screening', 'core',
  'Penyaringan berkas lamaran, matching kualifikasi posisi, dan ranking kandidat berbasis rubrik',
  ARRAY[jt.id], NULL, ARRAY['hris.get_applicants','resume.evaluate'], 'built_in',
  '## CANDIDATE SCREENING RUBRIC\nEvaluate candidate resumes objectively against job requirements, experience thresholds, and skill matrices.'
FROM ai_job_titles jt WHERE jt.job_code = 'hr_recruitment'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'attendance_anomaly_audit', 'Attendance & Geofence Verification', 'core',
  'Audit log kehadiran staff, validasi radius geofence GPS, dan deteksi anomali keterlambatan',
  ARRAY[jt.id], NULL, ARRAY['attendance.verify_geofence','attendance.audit_logs'], 'built_in',
  '## ATTENDANCE & GEOFENCE AUDITING\nValidate check-in GPS coordinates against assigned workplace geofence radius and flag spoofing patterns.'
FROM ai_job_titles jt WHERE jt.job_code = 'hr_recruitment'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.9 AI Finance
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'cashflow_pressure_forecast', 'Cash Flow Pressure & AR/AP Aging Forecast', 'core',
  'Proyeksi arus kas masa depan, peringatan piutang jatuh tempo, dan optimalisasi pembayaran hutang',
  ARRAY[jt.id], NULL, ARRAY['finance.get_invoices','cashflow.forecast'], 'built_in',
  '## CASH FLOW FORECASTING MODEL\nModel incoming receivables aging vs mandatory outgoing vendor disbursements to predict cash crunches.'
FROM ai_job_titles jt WHERE jt.job_code = 'finance'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'budget_variance_audit', 'Departmental Budget vs Actual Variance Audit', 'core',
  'Audit realisasi pengeluaran anggaran per departemen dan identifikasi pemborosan over-budget',
  ARRAY[jt.id], NULL, ARRAY['finance.get_budget_variance','analytics.query'], 'built_in',
  '## BUDGET VARIANCE AUDIT\nHighlight budget overruns exceeding tolerance thresholds with drill-downs into cost driver line items.'
FROM ai_job_titles jt WHERE jt.job_code = 'finance'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'payment_reconciliation', 'Payment Gateway Reconciliation', 'core',
  'Verifikasi tanda tangan webhook payment gateway dan rekonsiliasi transaksi otomatis',
  ARRAY[jt.id], NULL, ARRAY['payment.verify_webhook','order.update_status'], 'built_in',
  '## PAYMENT RECONCILIATION GATEWAY\nVerify cryptographic payload signatures from payment processors and update order payment states.'
FROM ai_job_titles jt WHERE jt.job_code = 'finance'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.10 AI Procurement
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'po_risk_tracking', 'Purchase Order Delay & Critical Stock Risk Tracking', 'core',
  'Pelacakan status pengiriman PO supplier dan deteksi dini keterlambatan barang kritis',
  ARRAY[jt.id], NULL, ARRAY['procurement.get_po_status','supplier.evaluate'], 'built_in',
  '## PO DELIVERY RISK TRACKING\nMonitor vendor fulfillment commitments against manufacturing schedules to prevent stockout bottlenecks.'
FROM ai_job_titles jt WHERE jt.job_code = 'procurement'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'inventory_stock_replenishment', 'Automated Stock Threshold & Reorder Proposal', 'core',
  'Peringatan otomatis saat stok menyentuh reorder point dan penyusunan draft PO pengadaan',
  ARRAY[jt.id], NULL, ARRAY['inventory.check_stock','po.create_draft'], 'built_in',
  '## AUTOMATED REORDER PROPOSAL\nCalculate economic order quantities (EOQ) and submit structured requisition drafts for management approval.'
FROM ai_job_titles jt WHERE jt.job_code = 'procurement'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.11 AI Project
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'project_health_calculation', 'Project Health Score Calculation', 'core',
  'Kalkulasi skor kesehatan proyek gabungan (Schedule, Cost, Manpower, HSE)',
  ARRAY[jt.id], NULL, ARRAY['project.get_status','project.calculate_health'], 'built_in',
  '## PROJECT HEALTH SCORECARD\nCompute composite health indices across Schedule Performance Index (SPI), Cost Index (CPI), manpower, and safety incidents.'
FROM ai_job_titles jt WHERE jt.job_code = 'project'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'schedule_delay_prediction', 'Milestone Delay Prediction & Critical Path Analysis', 'core',
  'Analisis jalur kritis (Critical Path) dan prediksi risiko keterlambatan milestone proyek',
  ARRAY[jt.id], NULL, ARRAY['project.get_schedule','risk.predict'], 'built_in',
  '## CRITICAL PATH DELAY ANALYSIS\nIdentify task dependency blockers on critical path and propose task resequencing options to recover lost time.'
FROM ai_job_titles jt WHERE jt.job_code = 'project'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.12 AI Knowledge & Document
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'sop_extraction_curation', 'SOP, Contract & Policy Semantic Ingestion', 'core',
  'Ekstraksi dokumen SOP, manual kerja, kontrak hukum, dan kurasi ke dalam Company Brain',
  ARRAY[jt.id], NULL, ARRAY['document.extract_text','memory.upsert'], 'built_in',
  '## SEMANTIC DOCUMENT CURATION\nParse unstructured PDFs, extract structured clauses, SOP workflows, and embed into searchable semantic memory.'
FROM ai_job_titles jt WHERE jt.job_code = 'knowledge_document'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'knowledge_compliance_eval', 'Knowledge Rule vs Live Data Compliance Evaluation', 'core',
  'Evaluasi kesesuaian operasional harian terhadap Knowledge Rules & SOP perusahaan',
  ARRAY[jt.id], NULL, ARRAY['knowledge_rule.evaluate','compliance.check'], 'built_in',
  '## KNOWLEDGE COMPLIANCE EVALUATOR\nCross-reference live operational execution events against authoritative corporate compliance rules.'
FROM ai_job_titles jt WHERE jt.job_code = 'knowledge_document'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.13 AI Research
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'hierarchical_knowledge_research', 'Knowledge Priority Hierarchy Research Engine', 'core',
  'Riset mendalam berurutan: SOP internal -> Data enterprise -> Sumber eksternal terpercaya',
  ARRAY[jt.id], NULL, ARRAY['memory.search','enterprise.query_records','web.search'], 'built_in',
  '## HIERARCHICAL RESEARCH ENGINE\nSearch authoritative internal SOPs first, followed by internal data records, before querying approved external sources.'
FROM ai_job_titles jt WHERE jt.job_code = 'research'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'pattern_discovery_rule_proposal', 'Historical Pattern Finding & Knowledge Rule Proposal', 'core',
  'Menganalisis anomali historis dan mengusulkan Knowledge Rule baru ke Chief of Staff',
  ARRAY[jt.id], NULL, ARRAY['analytics.query','knowledge_rule.propose'], 'built_in',
  '## PATTERN FINDING & RULE FORMULATION\nDetect recurring anomalies in operational logs and formulate draft corporate rules with statistical confidence evidence.'
FROM ai_job_titles jt WHERE jt.job_code = 'research'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.14 AI Reporting
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'automated_enterprise_reports', 'Automated Daily, Weekly & Executive Reporting', 'core',
  'Menyusun laporan operasional, laporan manajemen, dan distribusi proactive message',
  ARRAY[jt.id], NULL, ARRAY['report.generate','analytics.aggregate','notification.send'], 'built_in',
  '## AUTOMATED ENTERPRISE REPORTING\nAggregate cross-department metric snapshots into cleanly structured executive and operational markdown digests.'
FROM ai_job_titles jt WHERE jt.job_code = 'reporting'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'data_visualization_synthesis', 'Data Visualization & Chart Rendering Synthesis', 'core',
  'Merancang konfigurasi chart dan visualisasi visual data analitik yang presisi',
  ARRAY[jt.id], NULL, ARRAY['analytics.query','chart.render'], 'built_in',
  '## DATA VISUALIZATION ENGINE\nSelect appropriate chart typologies (time series, stacked bar, donut) to communicate complex datasets intuitively.'
FROM ai_job_titles jt WHERE jt.job_code = 'reporting'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 3.15 AI Task & Workflow
INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'autonomous_task_dispatch', 'Autonomous Task Creation & Multi-Agent Dispatch', 'core',
  'Membuat tiket tugas otomatis dan mendistribusikan ke AI Agent atau Human staff',
  ARRAY[jt.id], NULL, ARRAY['task.create','task.assign','workflow.dispatch'], 'built_in',
  '## AUTONOMOUS TASK DISPATCHER\nGenerate unambiguous task specs with acceptance criteria, assigning to appropriate agent or human based on capability.'
FROM ai_job_titles jt WHERE jt.job_code = 'task_workflow'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

INSERT INTO ai_agent_skills (skill_code, skill_name, skill_category, description, applicable_job_title_ids, applicable_structural_role_ids, required_mcp_tools, source_type, skill_definition_content)
SELECT 'closed_loop_remediation', 'Closed-Loop Remediation & Objective Verification', 'core',
  'Memantau penyelesaian tugas hingga verifikasi hasil akhir (Closed-Loop Outcome)',
  ARRAY[jt.id], NULL, ARRAY['monitoring_loop.track','system.verify_resolution'], 'built_in',
  '## CLOSED-LOOP REMEDIATION ENGINE\nTrack task lifecycle from anomaly detection to verified operational resolution before marking resolved.'
FROM ai_job_titles jt WHERE jt.job_code = 'task_workflow'
ON CONFLICT (skill_code) DO UPDATE SET 
  skill_name = EXCLUDED.skill_name,
  description = EXCLUDED.description,
  applicable_job_title_ids = EXCLUDED.applicable_job_title_ids,
  required_mcp_tools = EXCLUDED.required_mcp_tools;

-- 4. Audit ledger entry
INSERT INTO schema_migration_audit_ledger (id, version_tag, migration_name, applied_by, notes)
VALUES (
    gen_random_uuid()::text,
    'V92',
    'fase92_ai_agent_skills_and_plugin_packages',
    'system_orchestrator',
    'Created ai_agent_skills and ai_skill_plugin_packages tables with RLS and seeded built-in skills for all 15 AI Job Titles & Structural Roles'
) ON CONFLICT (id) DO NOTHING;
