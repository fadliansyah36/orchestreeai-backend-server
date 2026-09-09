CREATE TABLE "public"."approved_external_sources" (
  "id"               character varying(64)    NOT NULL DEFAULT gen_random_uuid(),
  "tenant_id"        character varying(64)    NOT NULL,
  "source_name"      character varying(255)   NOT NULL,
  "domain"           character varying(255)   NOT NULL,
  "category"         character varying(64)    NOT NULL,
  "base_url"         text                     NOT NULL,
  "description"      text                     NOT NULL DEFAULT ''::text,
  "is_active"        boolean                  NOT NULL DEFAULT true,
  "trust_score"      double precision         NOT NULL DEFAULT 0.95,
  "auth_config_json" jsonb                    NOT NULL DEFAULT '{}'::jsonb,
  "created_at"       timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at"       timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "approved_external_sources_pkey" PRIMARY KEY (id),
  CONSTRAINT "approved_external_sources_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."approved_external_sources"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_approved_sources_domain ON public.approved_external_sources USING btree (tenant_id, DOMAIN);

CREATE INDEX idx_approved_sources_tenant ON public.approved_external_sources USING btree (tenant_id, is_active);

CREATE POLICY "manage_approved_sources" ON "public"."approved_external_sources"
  FOR ALL
  TO PUBLIC
  USING ((public.current_user_role() = 'SUPER_ADMIN'::text));

CREATE POLICY "read_approved_sources" ON "public"."approved_external_sources"
  FOR SELECT
  TO PUBLIC
  USING ((auth.role() = 'authenticated'::text));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."approved_external_sources" TO "anon", "authenticated", "postgres", "service_role";
