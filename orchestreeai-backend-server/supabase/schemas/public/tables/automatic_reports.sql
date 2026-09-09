CREATE TABLE "public"."automatic_reports" (
  "id"                      text             NOT NULL,
  "tenant_id"               text             NOT NULL,
  "report_type"             text             NOT NULL,
  "scope"                   text             NOT NULL DEFAULT 'company'::text,
  "title"                   text             NOT NULL,
  "executive_summary"       text             NOT NULL,
  "content_ref"             text,
  "delivered_channels_json" text             NOT NULL DEFAULT '["APP_FEED"]'::text,
  "status"                  text             NOT NULL DEFAULT 'DELIVERED'::text,
  "date_string"             text             NOT NULL DEFAULT ''::text,
  "data_points_count"       integer          NOT NULL DEFAULT 0,
  "overall_health_score"    double precision NOT NULL DEFAULT 90.0,
  "risk_severity"           text             NOT NULL DEFAULT 'LOW'::text,
  "generated_at"            bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "delivered_at"            bigint           DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "automatic_reports_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_auto_reports_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."automatic_reports"
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE "public"."automatic_reports"
  REPLICA IDENTITY FULL;

CREATE INDEX idx_auto_reports_scope ON public.automatic_reports USING btree (scope);

CREATE INDEX idx_auto_reports_tenant_gen ON public.automatic_reports USING btree (tenant_id, generated_at);

CREATE INDEX idx_auto_reports_tenant ON public.automatic_reports USING btree (tenant_id);

CREATE INDEX idx_auto_reports_type_gen ON public.automatic_reports USING btree (tenant_id, report_type, generated_at);

CREATE INDEX idx_auto_reports_type ON public.automatic_reports USING btree (report_type);

CREATE POLICY "Tenant isolation for automatic_reports" ON "public"."automatic_reports"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_auto_reports" ON "public"."automatic_reports"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."automatic_reports" TO "anon", "authenticated", "postgres", "service_role";
