CREATE TABLE "public"."brand_style_references" (
  "id"                  character varying(255) NOT NULL,
  "tenant_id"           character varying(255) NOT NULL,
  "name"                character varying(255) NOT NULL,
  "reference_image_url" text                   NOT NULL,
  "style_notes"         text                   NOT NULL DEFAULT ''::text,
  "weight"              real                   NOT NULL DEFAULT 0.85,
  "is_locked"           boolean                NOT NULL DEFAULT true,
  "category"            character varying(100) NOT NULL DEFAULT 'GENERAL'::character varying,
  "created_at"          bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "updated_at"          bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "brand_style_references_pkey" PRIMARY KEY (id),
  CONSTRAINT "brand_style_references_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."brand_style_references"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_brand_style_refs_locked ON public.brand_style_references USING btree (is_locked);

CREATE INDEX idx_brand_style_refs_tenant ON public.brand_style_references USING btree (tenant_id);

CREATE POLICY "Tenant isolation for brand_style_references" ON "public"."brand_style_references"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_brand_style_references" ON "public"."brand_style_references"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

CREATE POLICY "tenant_isolation_brand_style_references" ON "public"."brand_style_references"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id))
  WITH CHECK (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."brand_style_references" TO "anon", "authenticated", "postgres", "service_role";
