CREATE TABLE "public"."carts" (
  "id"                              text             NOT NULL,
  "tenant_id"                       text             NOT NULL,
  "customer_id"                     text             NOT NULL,
  "conversation_id"                 text,
  "channel_account_id"              text,
  "status"                          text             NOT NULL DEFAULT 'ACTIVE'::text,
  "subtotal"                        double precision NOT NULL DEFAULT 0.0,
  "discount_amount"                 double precision NOT NULL DEFAULT 0.0,
  "tax_amount"                      double precision NOT NULL DEFAULT 0.0,
  "shipping_fee"                    double precision NOT NULL DEFAULT 0.0,
  "total_amount"                    double precision NOT NULL DEFAULT 0.0,
  "currency"                        text             NOT NULL DEFAULT 'IDR'::text,
  "notes"                           text             NOT NULL DEFAULT ''::text,
  "last_activity_at"                bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "abandoned_recovery_triggered_at" bigint,
  "created_at"                      bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "updated_at"                      bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "carts_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_carts_customer" FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE,
  CONSTRAINT "fk_carts_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."carts"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_carts_conversation_id ON public.carts USING btree (conversation_id);

CREATE INDEX idx_carts_customer_id ON public.carts USING btree (customer_id);

CREATE INDEX idx_carts_last_activity ON public.carts USING btree (last_activity_at);

CREATE INDEX idx_carts_status ON public.carts USING btree (status);

CREATE INDEX idx_carts_tenant_id ON public.carts USING btree (tenant_id);

CREATE POLICY "Tenant isolation for carts" ON "public"."carts"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_carts" ON "public"."carts"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."carts" TO "anon", "authenticated", "postgres", "service_role";
