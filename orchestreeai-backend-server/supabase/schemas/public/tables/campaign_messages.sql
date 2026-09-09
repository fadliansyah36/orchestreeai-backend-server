CREATE TABLE "public"."campaign_messages" (
  "id"                     character varying(255) NOT NULL,
  "campaign_id"            character varying(255) NOT NULL,
  "tenant_id"              character varying(255) NOT NULL,
  "channel_type"           character varying(50)  NOT NULL,
  "message_template"       text                   NOT NULL,
  "generated_copy"         text                   NOT NULL,
  "media_url"              text,
  "cta_button_text"        character varying(255),
  "cta_button_url"         text,
  "risk_check_result_json" jsonb                  NOT NULL DEFAULT '{}'::jsonb,
  "is_approved"            boolean                NOT NULL DEFAULT true,
  "created_at"             bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "campaign_messages_pkey" PRIMARY KEY (id),
  CONSTRAINT "campaign_messages_campaign_id_fkey" FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE,
  CONSTRAINT "campaign_messages_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."campaign_messages"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_campaign_messages_campaign ON public.campaign_messages USING btree (campaign_id);

CREATE INDEX idx_campaign_messages_channel ON public.campaign_messages USING btree (channel_type);

CREATE INDEX idx_campaign_messages_tenant ON public.campaign_messages USING btree (tenant_id);

CREATE POLICY "Tenant isolation for campaign_messages" ON "public"."campaign_messages"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_campaign_messages" ON "public"."campaign_messages"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

CREATE POLICY "tenant_isolation_campaign_messages" ON "public"."campaign_messages"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id))
  WITH CHECK (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."campaign_messages" TO "anon", "authenticated", "postgres", "service_role";
