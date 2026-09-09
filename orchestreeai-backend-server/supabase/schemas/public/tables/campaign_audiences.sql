CREATE TABLE "public"."campaign_audiences" (
  "id"                           character varying(255) NOT NULL,
  "campaign_id"                  character varying(255) NOT NULL,
  "tenant_id"                    character varying(255) NOT NULL,
  "customer_id"                  character varying(255) NOT NULL,
  "customer_display_name"        character varying(255) NOT NULL,
  "primary_channel"              character varying(50)  NOT NULL,
  "destination_identifier"       character varying(255) NOT NULL,
  "matched_criteria"             text                   NOT NULL,
  "personalized_attributes_json" jsonb                  NOT NULL DEFAULT '{}'::jsonb,
  "status"                       character varying(50)  NOT NULL DEFAULT 'RESOLVED'::character varying,
  "created_at"                   bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "campaign_audiences_pkey" PRIMARY KEY (id),
  CONSTRAINT "campaign_audiences_campaign_id_fkey" FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE,
  CONSTRAINT "campaign_audiences_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."campaign_audiences"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_campaign_audiences_campaign ON public.campaign_audiences USING btree (campaign_id);

CREATE INDEX idx_campaign_audiences_customer ON public.campaign_audiences USING btree (customer_id);

CREATE INDEX idx_campaign_audiences_status ON public.campaign_audiences USING btree (status);

CREATE INDEX idx_campaign_audiences_tenant ON public.campaign_audiences USING btree (tenant_id);

CREATE POLICY "Tenant isolation for campaign_audiences" ON "public"."campaign_audiences"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_campaign_audiences" ON "public"."campaign_audiences"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

CREATE POLICY "tenant_isolation_campaign_audiences" ON "public"."campaign_audiences"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id))
  WITH CHECK (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."campaign_audiences" TO "anon", "authenticated", "postgres", "service_role";
