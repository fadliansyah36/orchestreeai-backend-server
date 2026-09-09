CREATE TABLE "public"."api_keys" (
  "id"         character varying(64)    NOT NULL,
  "tenant_id"  character varying(64)    NOT NULL,
  "name"       character varying(128)   NOT NULL,
  "key_prefix" character varying(16)    NOT NULL,
  "key_hash"   character varying(255)   NOT NULL,
  "scopes"     character varying(255)   NOT NULL,
  "is_active"  boolean                  NOT NULL DEFAULT true,
  "expires_at" timestamp with time zone,
  "created_at" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "api_keys_pkey" PRIMARY KEY (id),
  CONSTRAINT "api_keys_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."api_keys"
  ENABLE ROW LEVEL SECURITY;

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."api_keys" TO "anon", "authenticated", "postgres", "service_role";
