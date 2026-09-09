CREATE TABLE "public"."brand_guidelines" (
  "id"                   text    NOT NULL,
  "tenant_id"            text    NOT NULL,
  "name"                 text    NOT NULL,
  "description"          text    NOT NULL,
  "primary_color_hex"    text    NOT NULL,
  "secondary_color_hex"  text    NOT NULL,
  "accent_color_hex"     text    NOT NULL,
  "background_color_hex" text    NOT NULL,
  "tone_voice"           text    NOT NULL,
  "typography"           text    NOT NULL,
  "negative_keywords"    text    NOT NULL,
  "is_default"           boolean NOT NULL DEFAULT false,
  CONSTRAINT "brand_guidelines_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_brand_guide_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."brand_guidelines"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_brand_guide_tenant ON public.brand_guidelines USING btree (tenant_id);

CREATE POLICY "Tenant isolation for brand_guidelines" ON "public"."brand_guidelines"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_brand_guide" ON "public"."brand_guidelines"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."brand_guidelines" TO "anon", "authenticated", "postgres", "service_role";
