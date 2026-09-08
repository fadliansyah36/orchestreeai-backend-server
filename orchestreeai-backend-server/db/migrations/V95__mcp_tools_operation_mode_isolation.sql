-- ====================================================================
-- OrchestreeAI Database Migration V95: MCP Tools Operation Mode Domain Isolation
-- PRD Addendum 1 & 2 - 5-Layer Isolation Architecture (Langkah 1: Skema Data & Klasifikasi Tool)
-- Enforces strict domain isolation for tool executions between customer_facing_omnichannel and internal_proactive_reporting
-- ====================================================================

-- 1. Tambahkan kolom pada mcp_tools
ALTER TABLE mcp_tools 
    ADD COLUMN IF NOT EXISTS restricted_to_operation_mode TEXT NOT NULL DEFAULT 'shared'
    CONSTRAINT chk_mcp_tools_restricted_mode 
    CHECK (restricted_to_operation_mode IN ('customer_facing_omnichannel', 'internal_proactive_reporting', 'shared'));

-- 2. KLASIFIKASIKAN TOOL YANG SUDAH ADA:

-- HANYA untuk customer_facing_omnichannel (TIDAK BOLEH dipanggil Proactive Agent):
UPDATE mcp_tools 
SET restricted_to_operation_mode = 'customer_facing_omnichannel'
WHERE tool_code IN (
    'conversation.reply_to_customer',
    'cart.create',
    'order.create',
    'payment.generate_qr',
    'product.recommend',
    'lead.create',
    'checkout_link',
    'payment_create',
    'quote_generate',
    'discount_negotiate',
    'catalog_search',
    'solution_recommend',
    'product_compare',
    'lead_qualify',
    'lead_score',
    'bantd_extract',
    'faq_lookup',
    'demo_scheduler',
    'technical_specs',
    'inventory_check',
    'compatibility_check',
    'cart_recovery',
    'quote_reminder',
    'promotion_send',
    'onboarding_guide',
    'csat_collect',
    'ticket_create',
    'churn_risk_detect',
    'loyalty_offer',
    'winback_campaign',
    'satisfaction_survey',
    'campaign_broadcast',
    'segment_match',
    'content_distribute',
    'click_track',
    'pipeline_analyze',
    'conversion_report',
    'sales.apply_discount',
    'sales.process_refund',
    'sales.cancel_order',
    'sales.custom_quote',
    'invoice.generate',
    'shipping.calculate_rates',
    'product.get_upsell_candidates',
    'product.get_complementary',
    'product.get_bundle_options'
);

-- HANYA untuk internal_proactive_reporting (TIDAK BOLEH dipanggil Omnichannel Agent untuk membalas customer):
UPDATE mcp_tools 
SET restricted_to_operation_mode = 'internal_proactive_reporting'
WHERE tool_code IN (
    'analytics.query_company_kpi',
    'competitor_intel.get_insights',
    'chief_of_staff.compose_briefing',
    'hr_data.query',
    'finance_data.query_cashflow',
    'notification.send_daily_report_to_staff',
    'analytics.calculate',
    'web.scrape_competitor',
    'social.listen',
    'news.fetch',
    'task.sync',
    'agent_performance',
    'forecast_revenue',
    'channel.publish',
    'vibe_prospecting.get_leads',
    'world_monitor.get_relevant_trends'
);

-- 'shared' HANYA untuk tool yang benar-benar netral (mis. knowledge.lookup untuk FAQ umum)
UPDATE mcp_tools 
SET restricted_to_operation_mode = 'shared'
WHERE tool_code IN (
    'web.fetch',
    'company_brain.query',
    'knowledge_search',
    'knowledge_base',
    'channel_routing',
    'crm_update',
    'shipping.prepare',
    'payment.webhook_verify',
    'enterprise.query_records',
    'erp.get_inventory',
    'erp.reserve_stock',
    'crm.get_customer_record',
    'crm.update_stage',
    'pos.get_order_status',
    'pos.create_draft_sale',
    'image_generation.create',
    'notification.send.telegram'
);

-- 3. Create Index for fast filtering by operation mode
CREATE INDEX IF NOT EXISTS idx_mcp_tools_op_mode ON mcp_tools(restricted_to_operation_mode);
