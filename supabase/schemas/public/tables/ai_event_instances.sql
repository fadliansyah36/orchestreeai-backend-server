CREATE TABLE "public"."ai_event_instances" (
  "id"                           character varying(255) NOT NULL,
  "tenant_id"                    character varying(255) NOT NULL,
  "event_code"                   character varying(100) NOT NULL,
  "entity_reference"             character varying(255) NOT NULL,
  "source_system"                character varying(100) NOT NULL DEFAULT 'INTERNAL'::character varying,
  "payload_json"                 jsonb                  NOT NULL DEFAULT '{}'::jsonb,
  "status"                       character varying(50)  NOT NULL DEFAULT 'NEW'::character varying,
  "severity"                     character varying(50)  NOT NULL DEFAULT 'HIGH'::character varying,
  "is_multi_agent_collaborative" boolean                NOT NULL DEFAULT false,
  "triggered_at"                 bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "handled_at"                   bigint,
  CONSTRAINT "ai_event_instances_pkey" PRIMARY KEY (id),
  CONSTRAINT "ai_event_instances_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."ai_event_instances"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_ai_event_inst_code ON public.ai_event_instances USING btree (event_code);

CREATE INDEX idx_ai_event_inst_ref ON public.ai_event_instances USING btree (entity_reference);

CREATE INDEX idx_ai_event_inst_status ON public.ai_event_instances USING btree (status);

CREATE INDEX idx_ai_event_inst_tenant ON public.ai_event_instances USING btree (tenant_id);

CREATE INDEX idx_ai_event_inst_triggered ON public.ai_event_instances USING btree (triggered_at);

CREATE POLICY "Tenant isolation for ai_event_instances" ON "public"."ai_event_instances"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_ai_event_instances" ON "public"."ai_event_instances"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

CREATE POLICY "tenant_isolation_ai_event_instances" ON "public"."ai_event_instances"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id))
  WITH CHECK (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_event_instances" TO "anon", "authenticated", "postgres", "service_role";
