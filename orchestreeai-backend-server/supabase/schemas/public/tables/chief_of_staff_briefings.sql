CREATE TABLE "public"."chief_of_staff_briefings" (
  "id"                        character varying(255) NOT NULL,
  "tenant_id"                 character varying(255) NOT NULL,
  "period_title"              character varying(255) NOT NULL,
  "headline"                  character varying(255) NOT NULL,
  "executive_summary"         text                   NOT NULL,
  "key_findings"              text                   NOT NULL,
  "strategic_recommendations" text                   NOT NULL,
  "human_workforce_summary"   text                   NOT NULL,
  "ai_workforce_summary"      text                   NOT NULL,
  "data_availability_state"   character varying(50)  NOT NULL DEFAULT 'AVAILABLE'::character varying,
  "data_limitations_notice"   text                   NOT NULL DEFAULT ''::text,
  "grounding_sources_json"    jsonb                  NOT NULL DEFAULT '[]'::jsonb,
  "explainability_reasoning"  text                   NOT NULL DEFAULT ''::text,
  "requires_human_approval"   boolean                NOT NULL DEFAULT false,
  "approval_status"           character varying(50)  NOT NULL DEFAULT 'NOT_REQUIRED'::character varying,
  "approved_by"               character varying(255),
  "approval_timestamp"        bigint,
  "created_at"                bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "chief_of_staff_briefings_pkey" PRIMARY KEY (id),
  CONSTRAINT "chief_of_staff_briefings_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."chief_of_staff_briefings"
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE "public"."chief_of_staff_briefings"
  REPLICA IDENTITY FULL;

CREATE INDEX idx_cos_briefings_period ON public.chief_of_staff_briefings USING btree (tenant_id, period_title);

CREATE INDEX idx_cos_briefings_tenant_approval ON public.chief_of_staff_briefings USING btree (tenant_id, approval_status);

CREATE INDEX idx_cos_briefings_tenant_created ON public.chief_of_staff_briefings USING btree (tenant_id, created_at);

CREATE POLICY "Tenant isolation for chief_of_staff_briefings" ON "public"."chief_of_staff_briefings"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_chief_of_staff_briefings" ON "public"."chief_of_staff_briefings"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

CREATE POLICY "tenant_isolation_chief_of_staff_briefings" ON "public"."chief_of_staff_briefings"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id))
  WITH CHECK (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."chief_of_staff_briefings" TO "anon", "authenticated", "postgres", "service_role";
