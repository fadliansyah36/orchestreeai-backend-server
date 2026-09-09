CREATE TABLE "public"."board_columns" (
  "id"         character varying(64)    NOT NULL,
  "board_id"   character varying(64)    NOT NULL,
  "tenant_id"  character varying(64)    NOT NULL,
  "name"       character varying(64)    NOT NULL,
  "label"      character varying(128)   NOT NULL,
  "position"   integer                  NOT NULL DEFAULT 0,
  "color_hex"  character varying(16)    NOT NULL DEFAULT '#1E6FE0'::character varying,
  "is_default" boolean                  NOT NULL DEFAULT false,
  "created_at" timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "board_columns_pkey" PRIMARY KEY (id),
  CONSTRAINT "board_columns_board_id_fkey" FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE,
  CONSTRAINT "board_columns_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."board_columns"
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE "public"."board_columns"
  REPLICA IDENTITY FULL;

CREATE POLICY "tenant_isolation_board_columns" ON "public"."board_columns"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."board_columns" TO "anon", "authenticated", "postgres", "service_role";
