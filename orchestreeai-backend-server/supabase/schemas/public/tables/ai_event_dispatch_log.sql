CREATE TABLE "public"."ai_event_dispatch_log" (
  "id"                        character varying(255) NOT NULL,
  "tenant_id"                 character varying(255) NOT NULL,
  "event_instance_id"         character varying(255) NOT NULL,
  "dispatched_to_agent_id"    character varying(255) NOT NULL,
  "target_persona"            character varying(100) NOT NULL,
  "dispatched_at"             bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "response_summary"          text                   NOT NULL DEFAULT ''::text,
  "execution_success"         boolean                NOT NULL DEFAULT true,
  "is_collaborative_dispatch" boolean                NOT NULL DEFAULT false,
  CONSTRAINT "ai_event_dispatch_log_pkey" PRIMARY KEY (id),
  CONSTRAINT "ai_event_dispatch_log_event_instance_id_fkey" FOREIGN KEY (event_instance_id) REFERENCES public.ai_event_instances(id) ON DELETE CASCADE,
  CONSTRAINT "ai_event_dispatch_log_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."ai_event_dispatch_log"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_ai_event_disp_agent ON public.ai_event_dispatch_log USING btree (dispatched_to_agent_id);

CREATE INDEX idx_ai_event_disp_at ON public.ai_event_dispatch_log USING btree (dispatched_at);

CREATE INDEX idx_ai_event_disp_inst ON public.ai_event_dispatch_log USING btree (event_instance_id);

CREATE INDEX idx_ai_event_disp_tenant ON public.ai_event_dispatch_log USING btree (tenant_id);

CREATE POLICY "Tenant isolation for ai_event_dispatch_log" ON "public"."ai_event_dispatch_log"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_ai_event_dispatch_log" ON "public"."ai_event_dispatch_log"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

CREATE POLICY "tenant_isolation_ai_event_dispatch_log" ON "public"."ai_event_dispatch_log"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id))
  WITH CHECK (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_event_dispatch_log" TO "anon", "authenticated", "postgres", "service_role";
