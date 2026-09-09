CREATE TABLE "public"."ai_job_title_operation_mode_permission" (
  "id"                     uuid                  NOT NULL DEFAULT gen_random_uuid(),
  "job_title_id"           character varying(64),
  "allowed_operation_mode" text                  NOT NULL,
  CONSTRAINT "ai_job_title_operation_mode_permis_allowed_operation_mode_check"
    CHECK ((allowed_operation_mode = ANY (ARRAY['customer_facing_omnichannel'::text, 'internal_proactive_reporting'::text]))),
  CONSTRAINT "ai_job_title_operation_mode_permission_pkey" PRIMARY KEY (id),
  CONSTRAINT "uq_job_title_operation_mode" UNIQUE (job_title_id, allowed_operation_mode),
  CONSTRAINT "ai_job_title_operation_mode_permission_job_title_id_fkey" FOREIGN KEY (job_title_id) REFERENCES public.ai_job_titles(id) ON DELETE CASCADE
);

ALTER TABLE "public"."ai_job_title_operation_mode_permission"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_job_title_op_mode_perm ON public.ai_job_title_operation_mode_permission USING btree (job_title_id, allowed_operation_mode);

CREATE POLICY "manage_ai_job_title_operation_mode_permission" ON "public"."ai_job_title_operation_mode_permission"
  FOR ALL
  TO PUBLIC
  USING ((public.current_user_role() = 'SUPER_ADMIN'::text));

CREATE POLICY "read_ai_job_title_operation_mode_permission" ON "public"."ai_job_title_operation_mode_permission"
  FOR SELECT
  TO PUBLIC
  USING ((auth.role() = 'authenticated'::text));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE
  ON TABLE "public"."ai_job_title_operation_mode_permission"
  TO "anon", "authenticated", "postgres", "service_role";
