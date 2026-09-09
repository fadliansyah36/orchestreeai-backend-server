CREATE TABLE "public"."campaigns" (
  "id"                     character varying(255) NOT NULL,
  "tenant_id"              character varying(255) NOT NULL,
  "name"                   character varying(255) NOT NULL,
  "prompt_instruction"     text                   NOT NULL,
  "target_segment_code"    character varying(100),
  "target_channel_type"    character varying(50)  NOT NULL DEFAULT 'ALL'::character varying,
  "channel_account_id"     character varying(255),
  "generated_query"        text                   NOT NULL DEFAULT ''::text,
  "status"                 character varying(50)  NOT NULL DEFAULT 'DRAFT'::character varying,
  "total_audience"         integer                NOT NULL DEFAULT 0,
  "sent_count"             integer                NOT NULL DEFAULT 0,
  "delivered_count"        integer                NOT NULL DEFAULT 0,
  "read_count"             integer                NOT NULL DEFAULT 0,
  "converted_count"        integer                NOT NULL DEFAULT 0,
  "failed_count"           integer                NOT NULL DEFAULT 0,
  "risk_score"             double precision       NOT NULL DEFAULT 0.0,
  "risk_evaluation_status" character varying(50)  NOT NULL DEFAULT 'PASSED'::character varying,
  "brand_voice_score"      double precision       NOT NULL DEFAULT 95.0,
  "scheduled_at"           bigint,
  "executed_at"            bigint,
  "completed_at"           bigint,
  "created_by"             character varying(255) NOT NULL DEFAULT 'Admin'::character varying,
  "created_at"             bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "updated_at"             bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "campaigns_pkey" PRIMARY KEY (id),
  CONSTRAINT "campaigns_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."campaigns"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_campaigns_created_at ON public.campaigns USING btree (created_at);

CREATE INDEX idx_campaigns_status ON public.campaigns USING btree (status);

CREATE INDEX idx_campaigns_tenant ON public.campaigns USING btree (tenant_id);

CREATE POLICY "Tenant isolation for campaigns" ON "public"."campaigns"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_campaigns" ON "public"."campaigns"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

CREATE POLICY "tenant_isolation_campaigns" ON "public"."campaigns"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id))
  WITH CHECK (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."campaigns" TO "anon", "authenticated", "postgres", "service_role";
