CREATE TABLE "public"."ai_credit_topup_packages" (
  "id"              uuid    NOT NULL DEFAULT gen_random_uuid(),
  "package_tier"    text    NOT NULL,
  "credit_amount"   numeric NOT NULL,
  "price"           numeric NOT NULL,
  "validity_months" integer DEFAULT 12,
  "is_active"       boolean DEFAULT true,
  CONSTRAINT "ai_credit_topup_packages_pkey" PRIMARY KEY (id),
  CONSTRAINT "uq_topup_package" UNIQUE (package_tier, credit_amount, price)
);

ALTER TABLE "public"."ai_credit_topup_packages"
  ENABLE ROW LEVEL SECURITY;

CREATE POLICY "manage_ai_credit_topup_packages" ON "public"."ai_credit_topup_packages"
  FOR ALL
  TO PUBLIC
  USING ((public.current_user_role() = 'SUPER_ADMIN'::text));

CREATE POLICY "read_ai_credit_topup_packages" ON "public"."ai_credit_topup_packages"
  FOR SELECT
  TO PUBLIC
  USING (((auth.role() = 'authenticated'::text) OR (auth.role() = 'anon'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_credit_topup_packages" TO "anon", "authenticated", "postgres", "service_role";
