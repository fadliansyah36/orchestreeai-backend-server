-- V37: Enable Supabase Realtime Logical Replication for OrchestreeAI
-- PRD Master Fase 101 Bagian 7: Supabase Realtime as Cross-Component Sync Backbone

DO $$
BEGIN
    -- Ensure publication supabase_realtime exists
    IF NOT EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        CREATE PUBLICATION supabase_realtime;
    END IF;
END $$;

-- Enable REPLICA IDENTITY FULL and add to supabase_realtime publication
DO $$
DECLARE
    tbl text;
    tables text[] := ARRAY[
        'tasks',
        'notifications',
        'conversations',
        'conversation_messages',
        'competitor_insights',
        'chief_of_staff_briefings',
        'proactive_messages_log',
        'tenants',
        'usage_records',
        'audit_logs',
        'llm_routing_rules',
        'mcp_tools'
    ];
BEGIN
    FOREACH tbl IN ARRAY tables LOOP
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = tbl) THEN
            EXECUTE format('ALTER TABLE public.%I REPLICA IDENTITY FULL;', tbl);
            BEGIN
                EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I;', tbl);
            EXCEPTION WHEN duplicate_object THEN
                NULL;
            END;
        END IF;
    END LOOP;
END $$;
