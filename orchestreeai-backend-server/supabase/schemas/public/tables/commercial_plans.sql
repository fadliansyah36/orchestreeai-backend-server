CREATE TABLE "public"."commercial_plans" (
  "id"                uuid    NOT NULL DEFAULT gen_random_uuid(),
  "plan_code"         text    NOT NULL,
  "plan_name"         text    NOT NULL,
  "billing_interval"  text    DEFAULT 'monthly'::text,
  "price"             numeric,
  "currency"          text    DEFAULT 'IDR'::text,
  "credit_allocation" numeric,
  "human_seat_limit"  integer,
  "ai_agent_limit"    integer,
  "is_price_visible"  boolean DEFAULT true,
  "is_active"         boolean DEFAULT true,
  "sort_order"        integer,
  CONSTRAINT "commercial_plans_pkey" PRIMARY KEY (id),
  CONSTRAINT "commercial_plans_plan_code_key" UNIQUE (plan_code)
);

ALTER TABLE "public"."commercial_plans"
  ENABLE ROW LEVEL SECURITY;

CREATE POLICY "manage_commercial_plans" ON "public"."commercial_plans"
  FOR ALL
  TO PUBLIC
  USING ((public.current_user_role() = 'SUPER_ADMIN'::text));

CREATE POLICY "read_commercial_plans" ON "public"."commercial_plans"
  FOR SELECT
  TO PUBLIC
  USING (((auth.role() = 'authenticated'::text) OR (auth.role() = 'anon'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."commercial_plans" TO "anon", "authenticated", "postgres", "service_role";
