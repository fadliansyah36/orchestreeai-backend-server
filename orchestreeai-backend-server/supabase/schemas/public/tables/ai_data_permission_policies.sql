CREATE TABLE "public"."ai_data_permission_policies" (
  "id"                      character varying(64)    NOT NULL,
  "tenant_id"               character varying(64)    NOT NULL,
  "agent_id"                character varying(64)    NOT NULL,
  "connection_id"           character varying(64)    NOT NULL,
  "access_level"            character varying(32)    NOT NULL DEFAULT 'READ_ONLY'::character varying,
  "allowed_tables_or_types" jsonb                    NOT NULL DEFAULT '["*"]'::jsonb,
  "allowed_fields"          jsonb                    NOT NULL DEFAULT '["*"]'::jsonb,
  "condition_rules"         jsonb                    NOT NULL DEFAULT '{}'::jsonb,
  "granted_by_user_id"      character varying(64)    NOT NULL DEFAULT 'system'::character varying,
  "granted_at"              timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at"              timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "ai_data_permission_policies_pkey" PRIMARY KEY (id),
  CONSTRAINT "uk_ai_data_policy" UNIQUE (tenant_id, agent_id, connection_id),
  CONSTRAINT "ai_data_permission_policies_connection_id_fkey" FOREIGN KEY (connection_id) REFERENCES public.enterprise_system_connections(id) ON DELETE CASCADE,
  CONSTRAINT "ai_data_permission_policies_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."ai_data_permission_policies"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_ai_data_policies_lookup ON public.ai_data_permission_policies USING btree (tenant_id, agent_id, connection_id);

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_data_permission_policies" TO "anon", "authenticated", "postgres", "service_role";
