CREATE TABLE "public"."ai_structural_roles" (
  "id"                   character varying(64)    NOT NULL DEFAULT gen_random_uuid(),
  "parent_job_title_id"  character varying(64),
  "structural_code"      character varying(64)    NOT NULL,
  "structural_name"      character varying(128)   NOT NULL,
  "skill_summary"        text                     NOT NULL DEFAULT ''::text,
  "maps_to_persona_type" character varying(64)    NOT NULL,
  "created_at"           timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "is_active"            boolean                  DEFAULT true,
  CONSTRAINT "ai_structural_roles_parent_job_title_id_fkey" FOREIGN KEY (parent_job_title_id) REFERENCES public.ai_job_titles(id) ON DELETE CASCADE,
  CONSTRAINT "ai_structural_roles_pkey" PRIMARY KEY (id),
  CONSTRAINT "ai_structural_roles_structural_code_key" UNIQUE (structural_code)
);

ALTER TABLE "public"."ai_structural_roles"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_ai_structural_parent ON public.ai_structural_roles USING btree (parent_job_title_id);

CREATE POLICY "Public read ai_structural_roles" ON "public"."ai_structural_roles"
  FOR SELECT
  TO PUBLIC
  USING (true);

CREATE POLICY "manage_ai_structural_roles" ON "public"."ai_structural_roles"
  FOR ALL
  TO PUBLIC
  USING ((public.current_user_role() = 'SUPER_ADMIN'::text));

CREATE POLICY "read_ai_structural_roles" ON "public"."ai_structural_roles"
  FOR SELECT
  TO PUBLIC
  USING ((auth.role() = 'authenticated'::text));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_structural_roles" TO "anon", "authenticated", "postgres", "service_role";
