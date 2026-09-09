CREATE TABLE "public"."ai_credit_wallets" (
  "id"                   uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "tenant_id"            character varying(64)    NOT NULL,
  "subscription_balance" numeric                  DEFAULT 0,
  "topup_balance"        numeric                  DEFAULT 0,
  "bonus_balance"        numeric                  DEFAULT 0,
  "reserved_balance"     numeric                  DEFAULT 0,
  "used_balance"         numeric                  DEFAULT 0,
  "expired_balance"      numeric                  DEFAULT 0,
  "is_unlimited"         boolean                  DEFAULT false,
  "updated_at"           timestamp with time zone DEFAULT now(),
  CONSTRAINT "ai_credit_wallets_pkey" PRIMARY KEY (id),
  CONSTRAINT "ai_credit_wallets_tenant_id_key" UNIQUE (tenant_id),
  CONSTRAINT "ai_credit_wallets_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."ai_credit_wallets"
  ENABLE ROW LEVEL SECURITY;

CREATE POLICY "tenant_isolation_ai_credit_wallets" ON "public"."ai_credit_wallets"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_credit_wallets" TO "anon", "authenticated", "postgres", "service_role";
