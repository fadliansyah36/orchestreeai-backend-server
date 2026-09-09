CREATE TABLE "public"."company_code_history" (
  "id"           text   NOT NULL,
  "company_code" text   NOT NULL,
  "company_name" text   NOT NULL,
  "domain"       text   NOT NULL,
  "verified_at"  bigint NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "company_code_history_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."company_code_history"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_code_hist_code ON public.company_code_history USING btree (company_code);

CREATE POLICY "Tenant isolation for company_code_history" ON "public"."company_code_history"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "p_company_code_hist_select" ON "public"."company_code_history"
  FOR SELECT
  TO PUBLIC
  USING (true);

CREATE POLICY "p_company_code_hist_service" ON "public"."company_code_history"
  FOR ALL
  TO PUBLIC
  USING ((auth.role() = 'service_role'::text));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."company_code_history" TO "anon", "authenticated", "postgres", "service_role";
