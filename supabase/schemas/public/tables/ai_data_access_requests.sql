CREATE TABLE "public"."ai_data_access_requests" (
  "id"                     character varying(64)    NOT NULL,
  "tenant_id"              character varying(64)    NOT NULL,
  "agent_id"               character varying(64)    NOT NULL,
  "connection_id"          character varying(64)    NOT NULL,
  "requested_access_level" character varying(32)    NOT NULL DEFAULT 'READ_ONLY'::character varying,
  "requested_scope"        jsonb                    NOT NULL DEFAULT '["*"]'::jsonb,
  "business_reason"        text                     NOT NULL,
  "status"                 character varying(32)    NOT NULL DEFAULT 'PENDING'::character varying,
  "reviewed_by_user_id"    character varying(64),
  "reviewed_at"            timestamp with time zone,
  "rejection_reason"       text                     NOT NULL DEFAULT ''::text,
  "created_at"             timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "ai_data_access_requests_pkey" PRIMARY KEY (id),
  CONSTRAINT "ai_data_access_requests_connection_id_fkey" FOREIGN KEY (connection_id) REFERENCES public.enterprise_system_connections(id) ON DELETE CASCADE,
  CONSTRAINT "ai_data_access_requests_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."ai_data_access_requests"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_ai_data_req_tenant_agent ON public.ai_data_access_requests USING btree (tenant_id, agent_id, status);

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_data_access_requests" TO "anon", "authenticated", "postgres", "service_role";
