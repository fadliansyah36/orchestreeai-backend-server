CREATE TABLE "public"."ai_agents" (
  "id"                      character varying(64)    NOT NULL,
  "tenant_id"               character varying(64)    NOT NULL,
  "name"                    character varying(128)   NOT NULL,
  "role_title"              character varying(128)   NOT NULL,
  "department_id"           character varying(64),
  "avatar_icon"             character varying(64)    NOT NULL DEFAULT 'robot'::character varying,
  "status"                  character varying(32)    NOT NULL DEFAULT 'ONLINE'::character varying,
  "skills"                  text                     NOT NULL DEFAULT ''::text,
  "tools_granted"           text                     NOT NULL DEFAULT ''::text,
  "current_live_action"     text                     NOT NULL DEFAULT 'Siap menerima penugasan'::text,
  "completed_tasks_count"   integer                  NOT NULL DEFAULT 0,
  "quality_rating"          numeric(5,2)             NOT NULL DEFAULT 95.0,
  "uptime_percent"          numeric(5,2)             NOT NULL DEFAULT 99.9,
  "created_at"              timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "job_title_id"            character varying(64),
  "structural_role_id"      character varying(64),
  "industry_specialization" character varying(64)    DEFAULT 'general'::character varying,
  CONSTRAINT "ai_agents_pkey" PRIMARY KEY (id),
  CONSTRAINT "ai_agents_job_title_id_fkey" FOREIGN KEY (job_title_id) REFERENCES public.ai_job_titles(id) ON DELETE SET NULL,
  CONSTRAINT "ai_agents_structural_role_id_fkey" FOREIGN KEY (structural_role_id) REFERENCES public.ai_structural_roles(id) ON DELETE SET NULL,
  CONSTRAINT "ai_agents_department_id_fkey" FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE SET NULL,
  CONSTRAINT "ai_agents_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."ai_agents"
  ENABLE ROW LEVEL SECURITY;

CREATE POLICY "tenant_isolation_ai_agents" ON "public"."ai_agents"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_agents" TO "anon", "authenticated", "postgres", "service_role";
