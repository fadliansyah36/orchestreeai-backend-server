CREATE TABLE "public"."audit_logs" (
  "id"            character varying(64)    NOT NULL,
  "tenant_id"     character varying(64)    NOT NULL,
  "actor_name"    character varying(255)   NOT NULL,
  "actor_role"    character varying(64)    NOT NULL,
  "action"        character varying(128)   NOT NULL,
  "entity_target" character varying(255)   NOT NULL,
  "details"       text                     NOT NULL,
  "timestamp"     timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "audit_logs_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."audit_logs"
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE "public"."audit_logs"
  REPLICA IDENTITY FULL;

CREATE POLICY "tenant_isolation_audit" ON "public"."audit_logs"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."audit_logs" TO "anon", "authenticated", "postgres", "service_role";
