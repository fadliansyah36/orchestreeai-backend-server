CREATE TABLE "public"."ai_agent_skills" (
  "id"                             uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "package_id"                     uuid,
  "skill_code"                     text                     NOT NULL,
  "skill_name"                     text                     NOT NULL,
  "skill_category"                 text                     NOT NULL DEFAULT 'core'::text,
  "description"                    text,
  "applicable_job_title_ids"       text[],
  "applicable_structural_role_ids" text[],
  "required_mcp_tools"             text[],
  "source_type"                    text                     NOT NULL DEFAULT 'built_in'::text,
  "skill_definition_content"       text,
  "is_active"                      boolean                  DEFAULT true,
  "created_at"                     timestamp with time zone DEFAULT now(),
  CONSTRAINT "ai_agent_skills_pkey" PRIMARY KEY (id),
  CONSTRAINT "ai_agent_skills_skill_code_key" UNIQUE (skill_code),
  CONSTRAINT "ai_agent_skills_package_id_fkey" FOREIGN KEY (package_id) REFERENCES public.ai_skill_plugin_packages(id)
);

ALTER TABLE "public"."ai_agent_skills"
  ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read ai_agent_skills" ON "public"."ai_agent_skills"
  FOR SELECT
  TO PUBLIC
  USING (true);

CREATE POLICY "manage_ai_agent_skills" ON "public"."ai_agent_skills"
  FOR ALL
  TO PUBLIC
  USING ((public.current_user_role() = 'SUPER_ADMIN'::text));

CREATE POLICY "read_ai_agent_skills" ON "public"."ai_agent_skills"
  FOR SELECT
  TO PUBLIC
  USING ((auth.role() = 'authenticated'::text));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_agent_skills" TO "anon", "authenticated", "postgres", "service_role";
