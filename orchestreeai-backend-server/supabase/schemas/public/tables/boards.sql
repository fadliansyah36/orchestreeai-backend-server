CREATE TABLE "public"."boards" (
  "id"          character varying(64)    NOT NULL,
  "tenant_id"   character varying(64)    NOT NULL,
  "name"        character varying(128)   NOT NULL,
  "description" text                     DEFAULT ''::text,
  "is_default"  boolean                  NOT NULL DEFAULT true,
  "created_at"  timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at"  timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "boards_pkey" PRIMARY KEY (id),
  CONSTRAINT "boards_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."boards"
  ENABLE ROW LEVEL SECURITY;

CREATE POLICY "tenant_isolation_boards" ON "public"."boards"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."boards" TO "anon", "authenticated", "postgres", "service_role";
