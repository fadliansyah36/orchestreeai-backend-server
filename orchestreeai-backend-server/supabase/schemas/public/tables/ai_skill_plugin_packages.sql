CREATE TABLE "public"."ai_skill_plugin_packages" (
  "id"                uuid                     NOT NULL DEFAULT gen_random_uuid(),
  "package_code"      text                     NOT NULL,
  "package_name"      text                     NOT NULL,
  "version_tag"       text                     NOT NULL DEFAULT '1.0.0'::text,
  "publisher_role"    text                     NOT NULL DEFAULT 'SUPER_ADMIN'::text,
  "category"          text                     NOT NULL DEFAULT 'general'::text,
  "description"       text,
  "required_min_tier" integer                  DEFAULT 0,
  "skills_count"      integer                  DEFAULT 1,
  "is_verified"       boolean                  DEFAULT true,
  "is_active"         boolean                  DEFAULT true,
  "created_at"        timestamp with time zone DEFAULT now(),
  CONSTRAINT "ai_skill_plugin_packages_package_code_key" UNIQUE (package_code),
  CONSTRAINT "ai_skill_plugin_packages_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."ai_skill_plugin_packages"
  ENABLE ROW LEVEL SECURITY;

CREATE POLICY "manage_plugin_packages" ON "public"."ai_skill_plugin_packages"
  FOR ALL
  TO PUBLIC
  USING ((public.current_user_role() = 'SUPER_ADMIN'::text));

CREATE POLICY "read_plugin_packages" ON "public"."ai_skill_plugin_packages"
  FOR SELECT
  TO PUBLIC
  USING ((auth.role() = 'authenticated'::text));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_skill_plugin_packages" TO "anon", "authenticated", "postgres", "service_role";
