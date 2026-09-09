CREATE TABLE "public"."campaign_sends" (
  "id"                  character varying(255) NOT NULL,
  "campaign_id"         character varying(255) NOT NULL,
  "tenant_id"           character varying(255) NOT NULL,
  "audience_id"         character varying(255) NOT NULL,
  "customer_id"         character varying(255) NOT NULL,
  "customer_name"       character varying(255) NOT NULL,
  "channel_type"        character varying(50)  NOT NULL,
  "channel_account_id"  character varying(255),
  "destination"         character varying(255) NOT NULL,
  "sent_message_text"   text                   NOT NULL,
  "external_message_id" character varying(255),
  "send_status"         character varying(50)  NOT NULL DEFAULT 'SENT'::character varying,
  "error_message"       text,
  "sent_at"             bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "delivered_at"        bigint,
  "read_at"             bigint,
  "converted_at"        bigint,
  CONSTRAINT "campaign_sends_pkey" PRIMARY KEY (id),
  CONSTRAINT "campaign_sends_campaign_id_fkey" FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE,
  CONSTRAINT "campaign_sends_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."campaign_sends"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_campaign_sends_audience ON public.campaign_sends USING btree (audience_id);

CREATE INDEX idx_campaign_sends_campaign ON public.campaign_sends USING btree (campaign_id);

CREATE INDEX idx_campaign_sends_customer ON public.campaign_sends USING btree (customer_id);

CREATE INDEX idx_campaign_sends_sent_at ON public.campaign_sends USING btree (sent_at);

CREATE INDEX idx_campaign_sends_status ON public.campaign_sends USING btree (send_status);

CREATE INDEX idx_campaign_sends_tenant ON public.campaign_sends USING btree (tenant_id);

CREATE POLICY "Tenant isolation for campaign_sends" ON "public"."campaign_sends"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_campaign_sends" ON "public"."campaign_sends"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

CREATE POLICY "tenant_isolation_campaign_sends" ON "public"."campaign_sends"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id))
  WITH CHECK (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."campaign_sends" TO "anon", "authenticated", "postgres", "service_role";
