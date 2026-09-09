CREATE TABLE "public"."agent_skill_assignments" (
  "agent_id"        character varying(64)    NOT NULL,
  "skill_id"        character varying(64)    NOT NULL,
  "proficiency_pct" integer                  NOT NULL DEFAULT 95,
  "assigned_at"     timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "agent_skill_assignments_pkey" PRIMARY KEY (agent_id, skill_id),
  CONSTRAINT "agent_skill_assignments_skill_id_fkey" FOREIGN KEY (skill_id) REFERENCES public.agent_skills(id) ON DELETE CASCADE,
  CONSTRAINT "agent_skill_assignments_agent_id_fkey" FOREIGN KEY (agent_id) REFERENCES public.ai_agents(id) ON DELETE CASCADE
);

ALTER TABLE "public"."agent_skill_assignments"
  ENABLE ROW LEVEL SECURITY;

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."agent_skill_assignments" TO "anon", "authenticated", "postgres", "service_role";
