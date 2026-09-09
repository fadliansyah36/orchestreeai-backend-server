CREATE TABLE "public"."cart_items" (
  "id"              text             NOT NULL,
  "cart_id"         text             NOT NULL,
  "tenant_id"       text             NOT NULL,
  "product_id"      text             NOT NULL,
  "variant_id"      text,
  "product_name"    text             NOT NULL,
  "variant_name"    text,
  "sku"             text             NOT NULL,
  "unit_price"      double precision NOT NULL,
  "quantity"        integer          NOT NULL DEFAULT 1,
  "subtotal"        double precision NOT NULL,
  "discount_amount" double precision NOT NULL DEFAULT 0.0,
  "total_amount"    double precision NOT NULL,
  "attributes_json" text             NOT NULL DEFAULT '{}'::text,
  "created_at"      bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "cart_items_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_cart_items_cart" FOREIGN KEY (cart_id) REFERENCES public.carts(id) ON DELETE CASCADE,
  CONSTRAINT "fk_cart_items_product" FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE CASCADE,
  CONSTRAINT "fk_cart_items_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."cart_items"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_cart_items_cart_id ON public.cart_items USING btree (cart_id);

CREATE INDEX idx_cart_items_product_id ON public.cart_items USING btree (product_id);

CREATE INDEX idx_cart_items_tenant_id ON public.cart_items USING btree (tenant_id);

CREATE POLICY "Tenant isolation for cart_items" ON "public"."cart_items"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_cart_items" ON "public"."cart_items"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."cart_items" TO "anon", "authenticated", "postgres", "service_role";
