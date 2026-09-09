CREATE TABLE "public"."ai_credit_ledger" (
  "id"              uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "tenant_id"       character varying(64)    NOT NULL,
  "idempotency_key" text                     NOT NULL,
  "ledger_type"     text                     NOT NULL,
  "amount"          numeric                  NOT NULL,
  "balance_after"   numeric                  NOT NULL,
  "source_bucket"   text,
  "reference_type"  text,
  "reference_id"    uuid,
  "operator_id"     uuid,
  "reason"          text,
  "created_at"      timestamp with time zone DEFAULT now(),
  CONSTRAINT "ai_credit_ledger_idempotency_key_key" UNIQUE (idempotency_key),
  CONSTRAINT "ai_credit_ledger_ledger_type_check"
    CHECK
    ((ledger_type = ANY (ARRAY['CREDIT_GRANTED'::text, 'CREDIT_RESERVED'::text, 'CREDIT_CONSUMED'::text, 'CREDIT_RELEASED'::text, 'CREDIT_REFUNDED'::text, 'CREDIT_TOPUP'::text,
    'CREDIT_EXPIRED'::text, 'CREDIT_ADJUSTMENT'::text, 'CREDIT_BONUS'::text]))),
  CONSTRAINT "ai_credit_ledger_pkey" PRIMARY KEY (id),
  CONSTRAINT "ai_credit_ledger_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."ai_credit_ledger"
  ENABLE ROW LEVEL SECURITY;

CREATE TRIGGER trg_ai_credit_ledger_immutable
  BEFORE DELETE OR UPDATE ON public.ai_credit_ledger
  FOR EACH ROW
  EXECUTE FUNCTION public.prevent_ai_credit_ledger_mutation();

CREATE POLICY "ledger_insert_only" ON "public"."ai_credit_ledger"
  FOR INSERT
  TO PUBLIC
  WITH CHECK (public.app_has_tenant_access(tenant_id));

CREATE POLICY "ledger_no_delete" ON "public"."ai_credit_ledger"
  FOR DELETE
  TO PUBLIC
  USING (false);

CREATE POLICY "ledger_no_update" ON "public"."ai_credit_ledger"
  FOR UPDATE
  TO PUBLIC
  USING (false);

CREATE POLICY "ledger_tenant_read" ON "public"."ai_credit_ledger"
  FOR SELECT
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_credit_ledger" TO "anon", "authenticated", "postgres", "service_role";
