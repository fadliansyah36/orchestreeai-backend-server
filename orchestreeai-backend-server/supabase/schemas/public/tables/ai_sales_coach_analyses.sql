CREATE TABLE "public"."ai_sales_coach_analyses" (
  "id"                          text             NOT NULL,
  "tenant_id"                   text             NOT NULL,
  "period"                      text             NOT NULL DEFAULT 'Current 30 Days'::text,
  "total_staff_evaluated"       integer          NOT NULL DEFAULT 0,
  "top_performer_staff_id"      text,
  "top_performer_name"          text,
  "top_conversion_rate"         double precision NOT NULL DEFAULT 0.0,
  "low_performer_staff_id"      text,
  "low_performer_name"          text,
  "low_conversion_rate"         double precision NOT NULL DEFAULT 0.0,
  "winning_tactics_json"        text             NOT NULL DEFAULT '[]'::text,
  "common_mistakes_json"        text             NOT NULL DEFAULT '[]'::text,
  "recommended_playbook_doc_id" text,
  "playbook_title"              text             NOT NULL DEFAULT ''::text,
  "playbook_summary"            text             NOT NULL DEFAULT ''::text,
  "analyzed_by_model"           text             NOT NULL DEFAULT 'gemini-3.5-pro'::text,
  "analyzed_at"                 bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "ai_sales_coach_analyses_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_sales_coach_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."ai_sales_coach_analyses"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_sales_coach_analyzed_at ON public.ai_sales_coach_analyses USING btree (analyzed_at);

CREATE INDEX idx_sales_coach_tenant_id ON public.ai_sales_coach_analyses USING btree (tenant_id);

CREATE POLICY "Tenant isolation for ai_sales_coach_analyses" ON "public"."ai_sales_coach_analyses"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_sales_coach" ON "public"."ai_sales_coach_analyses"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_sales_coach_analyses" TO "anon", "authenticated", "postgres", "service_role";
