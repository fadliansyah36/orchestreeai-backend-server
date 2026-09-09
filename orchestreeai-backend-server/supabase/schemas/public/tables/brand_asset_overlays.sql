CREATE TABLE "public"."brand_asset_overlays" (
  "id"                   character varying(255) NOT NULL,
  "tenant_id"            character varying(255) NOT NULL,
  "asset_name"           character varying(255) NOT NULL,
  "asset_type"           character varying(50)  NOT NULL,
  "asset_file_url"       text                   NOT NULL,
  "default_position"     character varying(50)  NOT NULL DEFAULT 'TOP_RIGHT'::character varying,
  "custom_pos_x_percent" real                   NOT NULL DEFAULT 0.85,
  "custom_pos_y_percent" real                   NOT NULL DEFAULT 0.05,
  "target_scale_percent" real                   NOT NULL DEFAULT 0.15,
  "opacity"              real                   NOT NULL DEFAULT 1.0,
  "blend_mode"           character varying(50)  NOT NULL DEFAULT 'NORMAL'::character varying,
  "min_margin_px"        integer                NOT NULL DEFAULT 32,
  "is_default_active"    boolean                NOT NULL DEFAULT true,
  "created_at"           bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "updated_at"           bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "brand_asset_overlays_pkey" PRIMARY KEY (id),
  CONSTRAINT "brand_asset_overlays_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."brand_asset_overlays"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_brand_asset_overlays_active ON public.brand_asset_overlays USING btree (is_default_active);

CREATE INDEX idx_brand_asset_overlays_tenant ON public.brand_asset_overlays USING btree (tenant_id);

CREATE INDEX idx_brand_asset_overlays_type ON public.brand_asset_overlays USING btree (asset_type);

CREATE POLICY "Tenant isolation for brand_asset_overlays" ON "public"."brand_asset_overlays"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_brand_asset_overlays" ON "public"."brand_asset_overlays"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

CREATE POLICY "tenant_isolation_brand_asset_overlays" ON "public"."brand_asset_overlays"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id))
  WITH CHECK (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."brand_asset_overlays" TO "anon", "authenticated", "postgres", "service_role";
