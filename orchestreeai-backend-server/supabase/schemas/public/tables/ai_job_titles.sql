CREATE TABLE "public"."ai_job_titles" (
  "id"                              character varying(64)    NOT NULL DEFAULT gen_random_uuid(),
  "job_code"                        character varying(64)    NOT NULL,
  "job_name"                        character varying(128)   NOT NULL,
  "icon_key"                        character varying(64)    NOT NULL DEFAULT 'smart_toy'::character varying,
  "star_rating"                     integer                  DEFAULT 4,
  "description"                     text                     NOT NULL DEFAULT ''::text,
  "maps_to_persona_type"            character varying(64)    NOT NULL,
  "is_top_coordinator"              boolean                  NOT NULL DEFAULT false,
  "relevant_department_category"    character varying(64)    DEFAULT 'GENERAL'::character varying,
  "created_at"                      timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at"                      timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "relevant_department_category_id" uuid,
  "is_active"                       boolean                  DEFAULT true,
  CONSTRAINT "ai_job_titles_job_code_key" UNIQUE (job_code),
  CONSTRAINT "ai_job_titles_pkey" PRIMARY KEY (id),
  CONSTRAINT "ai_job_titles_relevant_department_category_id_fkey" FOREIGN KEY (relevant_department_category_id) REFERENCES public.department_categories(id)
);

ALTER TABLE "public"."ai_job_titles"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_ai_job_titles_code ON public.ai_job_titles USING btree (job_code);

CREATE INDEX idx_ai_job_titles_coord ON public.ai_job_titles USING btree (is_top_coordinator);

CREATE POLICY "Public read ai_job_titles" ON "public"."ai_job_titles"
  FOR SELECT
  TO PUBLIC
  USING (true);

CREATE POLICY "manage_ai_job_titles" ON "public"."ai_job_titles"
  FOR ALL
  TO PUBLIC
  USING ((public.current_user_role() = 'SUPER_ADMIN'::text));

CREATE POLICY "read_ai_job_titles" ON "public"."ai_job_titles"
  FOR SELECT
  TO PUBLIC
  USING ((auth.role() = 'authenticated'::text));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_job_titles" TO "anon", "authenticated", "postgres", "service_role";
