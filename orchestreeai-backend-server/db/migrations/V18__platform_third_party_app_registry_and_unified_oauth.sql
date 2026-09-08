-- ====================================================================
-- OrchestreeAI Database Migration V18: Platform-Level Third-Party App Registry & Unified OAuth
-- Architecture Revision: Centralized OrchestreeAI Platform App Credentials
-- ====================================================================

-- 1. Third-Party App Registry Table (Super Admin Platform-Level Management)
CREATE TABLE IF NOT EXISTS third_party_app_registry (
    id VARCHAR(64) PRIMARY KEY,
    platform_code VARCHAR(64) NOT NULL UNIQUE,
    platform_name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'PRODUCTIVITY', -- 'MESSAGING', 'SOCIAL', 'MARKETPLACE', 'PRODUCTIVITY', 'ENTERPRISE'
    auth_method VARCHAR(64) NOT NULL DEFAULT 'OAUTH2', -- 'OAUTH2', 'BOT_TOKEN', 'API_KEY', 'WEBHOOK', 'MANUAL_LINK'
    client_id VARCHAR(255) NOT NULL DEFAULT '',
    client_secret_ref TEXT NOT NULL DEFAULT '', -- Envelope encrypted KMS secret
    default_scopes TEXT NOT NULL DEFAULT '',
    oauth_authorize_url TEXT NOT NULL DEFAULT '',
    oauth_token_url TEXT NOT NULL DEFAULT '',
    supports_oauth BOOLEAN NOT NULL DEFAULT TRUE,
    requires_qr_session BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE', 'MAINTENANCE', 'DEPRECATED'
    description TEXT NOT NULL DEFAULT '',
    icon_key VARCHAR(64) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_app_reg_category ON third_party_app_registry(category);
CREATE INDEX IF NOT EXISTS idx_app_reg_status ON third_party_app_registry(status);

-- 2. OAuth State Table (Anti-CSRF & One-Time Token Verification)
CREATE TABLE IF NOT EXISTS oauth_state (
    id VARCHAR(64) PRIMARY KEY,
    state_token VARCHAR(128) NOT NULL UNIQUE,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    app_registry_id VARCHAR(64) NOT NULL REFERENCES third_party_app_registry(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    redirect_uri TEXT NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_oauth_state_token ON oauth_state(state_token);
CREATE INDEX IF NOT EXISTS idx_oauth_state_tenant ON oauth_state(tenant_id);

-- 3. Alter Existing Integration and Channel Tables to link to App Registry
ALTER TABLE integrations 
    ADD COLUMN IF NOT EXISTS app_registry_id VARCHAR(64) REFERENCES third_party_app_registry(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS connection_mode VARCHAR(64) NOT NULL DEFAULT 'oauth_platform_app';

ALTER TABLE enterprise_system_connections 
    ADD COLUMN IF NOT EXISTS app_registry_id VARCHAR(64) REFERENCES third_party_app_registry(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS connection_mode VARCHAR(64) NOT NULL DEFAULT 'oauth_platform_app';

ALTER TABLE channel_accounts 
    ADD COLUMN IF NOT EXISTS app_registry_id VARCHAR(64) REFERENCES third_party_app_registry(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS connection_mode VARCHAR(64) NOT NULL DEFAULT 'oauth_platform_app';

-- 4. Seed Official Platform Configurations (Production-Grade Specs)
INSERT INTO third_party_app_registry (
    id, platform_code, platform_name, category, auth_method, client_id, client_secret_ref, 
    default_scopes, oauth_authorize_url, oauth_token_url, supports_oauth, requires_qr_session, status, description, icon_key
) VALUES
-- WhatsApp Business (Meta Graph API)
('app-reg-whatsapp', 'whatsapp_business', 'WhatsApp Business Cloud API', 'MESSAGING', 'OAUTH2', 
 'meta_app_991823019283', 'enc:v1:meta_sec_waba_cloud_official_2026', 
 'whatsapp_business_messaging,whatsapp_business_management', 
 'https://www.facebook.com/v19.0/dialog/oauth', 'https://graph.facebook.com/v19.0/oauth/access_token', 
 TRUE, FALSE, 'ACTIVE', 'Official Meta Cloud API integration for WhatsApp Business verification, template messaging, and AI customer conversational routing.', 'whatsapp'),

-- Telegram Bot API
('app-reg-telegram', 'telegram', 'Telegram Bot Platform', 'MESSAGING', 'BOT_TOKEN', 
 'orchestree_official_bot_platform', 'enc:v1:tg_bot_token_manager_2026', 
 'bot_messages,webhooks,channel_broadcast', 
 '', '', 
 FALSE, FALSE, 'ACTIVE', 'Official Telegram Bot API integration with BotFather token validation and instant webhook callback dispatcher.', 'telegram'),

-- Instagram Graph API
('app-reg-instagram', 'instagram', 'Instagram Business & Creator', 'SOCIAL', 'OAUTH2', 
 'meta_app_991823019283', 'enc:v1:meta_sec_instagram_graph_official_2026', 
 'instagram_basic,instagram_content_publish,instagram_manage_comments,instagram_manage_messages', 
 'https://www.instagram.com/oauth/authorize', 'https://api.instagram.com/oauth/access_token', 
 TRUE, FALSE, 'ACTIVE', 'Official Instagram Graph API for automated feed scheduling, Reel publishing, comment moderation, and DM sales lead routing.', 'instagram'),

-- TikTok for Business
('app-reg-tiktok', 'tiktok', 'TikTok for Business API', 'SOCIAL', 'OAUTH2', 
 'tiktok_app_client_88291039', 'enc:v1:tiktok_sec_open_api_key_2026', 
 'user.info.basic,video.list,video.upload,comment.list,comment.publish', 
 'https://www.tiktok.com/v2/auth/authorize/', 'https://open.tiktokapis.com/v2/oauth/token/', 
 TRUE, FALSE, 'ACTIVE', 'Official TikTok Open API integration for video publishing, live commerce interaction, and engagement monitoring.', 'tiktok'),

-- Meta Ads (Marketing API)
('app-reg-meta-ads', 'meta_ads', 'Meta Ads Platform', 'SOCIAL', 'OAUTH2', 
 'meta_app_991823019283', 'enc:v1:meta_sec_marketing_ads_api_2026', 
 'ads_read,ads_management,read_insights', 
 'https://www.facebook.com/v19.0/dialog/oauth', 'https://graph.facebook.com/v19.0/oauth/access_token', 
 TRUE, FALSE, 'ACTIVE', 'Official Meta Marketing API for automated campaign budgeting, ad performance tracking, and conversion attribution.', 'meta_ads'),

-- Shopee Open Platform
('app-reg-shopee', 'shopee', 'Shopee Open Platform', 'MARKETPLACE', 'OAUTH2', 
 'shopee_partner_id_2001928', 'enc:v1:shopee_partner_sec_live_2026', 
 'item.get,item.update_price,order.get,order.update,logistics.get', 
 'https://partner.shopeemobile.com/api/v2/shop/auth_partner', 'https://partner.shopeemobile.com/api/v2/public/get_token_by_resend_code', 
 TRUE, FALSE, 'ACTIVE', 'Official Shopee Open API for two-way inventory sync, order fulfillment, pricing rules, and AI sales chat automation.', 'shopee'),

-- TikTok Shop Open API
('app-reg-tiktok-shop', 'tiktok_shop', 'TikTok Shop Partner Platform', 'MARKETPLACE', 'OAUTH2', 
 'tiktok_shop_partner_6619283', 'enc:v1:tiktok_shop_sec_partner_2026', 
 'seller.order.read,seller.product.read,seller.product.write,seller.fulfillment.write', 
 'https://services.tiktokshop.com/open/authorize', 'https://auth.tiktok-shops.com/api/v2/token/get', 
 TRUE, FALSE, 'ACTIVE', 'Official TikTok Shop Partner API for catalog management, live stream product showcase, and real-time order processing.', 'tiktok_shop'),

-- Slack Platform
('app-reg-slack', 'slack', 'Slack Workspace App', 'PRODUCTIVITY', 'OAUTH2', 
 '182930192.99182301', 'enc:v1:slack_oauth_secret_app_orchestree_2026', 
 'chat:write,channels:read,channels:history,users:read,app_mentions:read', 
 'https://slack.com/oauth/v2/authorize', 'https://slack.com/api/oauth.v2.access', 
 TRUE, FALSE, 'ACTIVE', 'Official Slack App with bot notifications, command triggers, channel monitoring (metadata-only), and AI collaborative task dispatch.', 'slack'),

-- Trello Platform
('app-reg-trello', 'trello', 'Trello Atlassian Platform', 'PRODUCTIVITY', 'OAUTH2', 
 'trello_api_key_88192019a', 'enc:v1:trello_oauth_secret_token_2026', 
 'read,write,account', 
 'https://trello.com/1/authorize', 'https://trello.com/1/OAuthGetAccessToken', 
 TRUE, FALSE, 'ACTIVE', 'Official Atlassian Trello Power-Up OAuth for board synchronization, card movements, and human workforce activity tracking.', 'trello'),

-- Microsoft Teams (Microsoft Graph API)
('app-reg-ms-teams', 'ms_teams', 'Microsoft Teams & 365 Graph', 'PRODUCTIVITY', 'OAUTH2', 
 'azure_ad_client_ms_graph_2026', 'enc:v1:azure_client_secret_entra_id_2026', 
 'OnlineMeetings.Read,Presence.Read,ChatMessage.Send,User.Read', 
 'https://login.microsoftonline.com/common/oauth2/v2.0/authorize', 'https://login.microsoftonline.com/common/oauth2/v2.0/token', 
 TRUE, FALSE, 'ACTIVE', 'Official Microsoft Entra ID & Graph API for Teams meetings, presence status, executive briefs, and corporate collaboration.', 'ms_teams'),

-- Jira Atlassian Cloud
('app-reg-jira', 'jira', 'Jira Cloud Platform', 'PRODUCTIVITY', 'OAUTH2', 
 'jira_cloud_app_9918230', 'enc:v1:jira_oauth2_secret_cloud_2026', 
 'read:jira-work,write:jira-work,read:jira-user,manage:jira-project', 
 'https://auth.atlassian.com/authorize', 'https://auth.atlassian.com/oauth/token', 
 TRUE, FALSE, 'ACTIVE', 'Official Atlassian Cloud 3-legged OAuth for enterprise issue tracking, sprint synchronizations, and incident management.', 'jira'),

-- Tokopedia (Deprecated Public API - Redirect to TikTok Shop / Manual Link)
('app-reg-tokopedia', 'tokopedia', 'Tokopedia Merchant Store', 'MARKETPLACE', 'MANUAL_LINK', 
 '', '', 
 'product.read,order.read', 
 '', '', 
 FALSE, FALSE, 'MAINTENANCE', 'Tokopedia Public Open API is sunsetted in favor of TikTok Shop & Enterprise Partner Gateway. Direct Manual Store Linking is provided.', 'tokopedia'),

-- HubSpot CRM
('app-reg-hubspot', 'crm_hubspot', 'HubSpot Enterprise CRM', 'ENTERPRISE', 'OAUTH2', 
 'hubspot_client_app_8819201', 'enc:v1:hubspot_sec_oauth_token_2026', 
 'crm.objects.contacts.read,crm.objects.contacts.write,crm.objects.deals.read,crm.objects.companies.read', 
 'https://app.hubspot.com/oauth/authorize', 'https://api.hubapi.com/oauth/v1/token', 
 TRUE, FALSE, 'ACTIVE', 'Official HubSpot CRM OAuth for omnichannel customer profile synchronizations, lead deal pipeline management, and contact enrichment.', 'hubspot'),

-- Workday HRIS
('app-reg-workday', 'hris_workday', 'Workday Cloud HRIS', 'ENTERPRISE', 'OAUTH2', 
 'workday_rest_client_2026', 'enc:v1:workday_api_client_secret_2026', 
 'staffing,workers,compensation,time_tracking', 
 'https://wd2-impl-services1.workday.com/ccx/oauth2/authorize', 'https://wd2-impl-services1.workday.com/ccx/oauth2/token', 
 TRUE, FALSE, 'ACTIVE', 'Official Workday REST OAuth for employee profiles, org chart sync, attendance telemetry, and workforce capacity planning.', 'workday'),

-- Geotab Fleet Telematics
('app-reg-geotab', 'fleet_geotab', 'Geotab Fleet Management', 'ENTERPRISE', 'API_KEY', 
 'geotab_partner_feed_gateway', 'enc:v1:geotab_database_session_token_2026', 
 'vehicle.telemetry,fuel.monitoring,gps.track,diagnostics.read', 
 '', '', 
 FALSE, FALSE, 'ACTIVE', 'Enterprise Geotab Fleet Telematics API for heavy machinery fuel monitoring, vehicle health telemetry, and GPS geofence tracks.', 'geotab'),

-- Moka POS
('app-reg-moka', 'pos_moka', 'Moka POS Retail & F&B', 'ENTERPRISE', 'OAUTH2', 
 'moka_pos_app_id_991823', 'enc:v1:moka_secret_pos_key_2026', 
 'transactions.read,items.read,customers.read,outlets.read', 
 'https://service.mokapos.com/oauth/authorize', 'https://service.mokapos.com/oauth/token', 
 TRUE, FALSE, 'ACTIVE', 'Official Moka POS Open Platform for real-time in-store sales transactions, inventory deductions, and customer loyalty sync.', 'moka')

ON CONFLICT (platform_code) DO UPDATE SET
    platform_name = EXCLUDED.platform_name,
    category = EXCLUDED.category,
    auth_method = EXCLUDED.auth_method,
    client_id = EXCLUDED.client_id,
    client_secret_ref = EXCLUDED.client_secret_ref,
    default_scopes = EXCLUDED.default_scopes,
    oauth_authorize_url = EXCLUDED.oauth_authorize_url,
    oauth_token_url = EXCLUDED.oauth_token_url,
    supports_oauth = EXCLUDED.supports_oauth,
    requires_qr_session = EXCLUDED.requires_qr_session,
    status = EXCLUDED.status,
    description = EXCLUDED.description,
    icon_key = EXCLUDED.icon_key,
    updated_at = CURRENT_TIMESTAMP;

-- 5. One-Time Data Migration & Backfill for Existing Connections & Channels
-- Backfill Integrations Table
UPDATE integrations SET 
    app_registry_id = CASE 
        WHEN platform = 'TELEGRAM' THEN 'app-reg-telegram'
        WHEN platform = 'INSTAGRAM' THEN 'app-reg-instagram'
        WHEN platform = 'MARKETPLACE_SHOPEE' THEN 'app-reg-shopee'
        WHEN platform = 'SLACK' THEN 'app-reg-slack'
        WHEN platform = 'MS_TEAMS' THEN 'app-reg-ms-teams'
        WHEN platform = 'TRELLO' THEN 'app-reg-trello'
        WHEN platform = 'META_ADS' THEN 'app-reg-meta-ads'
        WHEN platform = 'MARKETPLACE_TOKOPEDIA' THEN 'app-reg-tokopedia'
        WHEN platform = 'TIKTOK' THEN 'app-reg-tiktok'
        WHEN platform = 'FACEBOOK' THEN 'app-reg-meta-ads'
        ELSE 'app-reg-slack'
    END,
    connection_mode = CASE
        WHEN platform = 'TELEGRAM' THEN 'official_orchestree_channel'
        WHEN platform = 'MARKETPLACE_TOKOPEDIA' THEN 'manual_link'
        ELSE 'oauth_platform_app'
    END
WHERE app_registry_id IS NULL;

-- Backfill Enterprise System Connections Table
UPDATE enterprise_system_connections SET
    app_registry_id = CASE
        WHEN system_type = 'erp' OR system_type = 'finance' THEN 'app-reg-moka'
        WHEN system_type = 'hris' THEN 'app-reg-workday'
        WHEN system_type = 'crm' THEN 'app-reg-hubspot'
        WHEN system_type = 'fleet' OR system_type = 'cmms' OR system_type = 'fms' THEN 'app-reg-geotab'
        WHEN system_type = 'project' THEN 'app-reg-jira'
        WHEN system_type = 'messaging' THEN 'app-reg-whatsapp'
        ELSE 'app-reg-hubspot'
    END,
    connection_mode = CASE
        WHEN connector_kind = 'OAUTH_CONNECTOR' THEN 'oauth_platform_app'
        WHEN connector_kind = 'MESSAGING_CONNECTOR' THEN 'official_orchestree_channel'
        ELSE 'manual_link'
    END
WHERE app_registry_id IS NULL;

-- Backfill Channel Accounts Table
UPDATE channel_accounts SET
    app_registry_id = CASE
        WHEN channel_type = 'WHATSAPP' THEN 'app-reg-whatsapp'
        WHEN channel_type = 'TELEGRAM' THEN 'app-reg-telegram'
        WHEN channel_type = 'INSTAGRAM' THEN 'app-reg-instagram'
        WHEN channel_type = 'TIKTOK' THEN 'app-reg-tiktok'
        WHEN channel_type = 'SHOPEE' THEN 'app-reg-shopee'
        WHEN channel_type = 'TOKOPEDIA' THEN 'app-reg-tokopedia'
        WHEN channel_type = 'FACEBOOK' THEN 'app-reg-meta-ads'
        ELSE 'app-reg-whatsapp'
    END,
    connection_mode = CASE
        WHEN channel_type = 'TELEGRAM' THEN 'official_orchestree_channel'
        WHEN channel_type = 'TOKOPEDIA' THEN 'manual_link'
        ELSE 'oauth_platform_app'
    END
WHERE app_registry_id IS NULL;
