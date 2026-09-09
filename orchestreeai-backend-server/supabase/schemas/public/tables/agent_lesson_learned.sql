CREATE TABLE "public"."agent_lesson_learned" (
  "id"                  text             NOT NULL,
  "tenant_id"           text             NOT NULL,
  "agent_id"            text             NOT NULL,
  "skill_id"            text             NOT NULL,
  "domain_context"      text             NOT NULL,
  "failure_pattern"     text             NOT NULL,
  "root_cause_analysis" text             NOT NULL,
  "corrective_guidance" text             NOT NULL,
  "sample_count"        integer          NOT NULL DEFAULT 1,
  "confidence_score"    double precision NOT NULL DEFAULT 0.85,
  "status"              text             NOT NULL DEFAULT 'ACTIVE'::text,
  "validated_by"        text             NOT NULL DEFAULT 'CONTINUOUS_LEARNING_CORE'::text,
  "created_at"          bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "updated_at"          bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "agent_lesson_learned_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_agent_lesson_agent" FOREIGN KEY (agent_id) REFERENCES public.ai_agents(id) ON DELETE CASCADE,
  CONSTRAINT "fk_agent_lesson_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."agent_lesson_learned"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_agent_lesson_agent ON public.agent_lesson_learned USING btree (agent_id);

CREATE INDEX idx_agent_lesson_skill ON public.agent_lesson_learned USING btree (skill_id);

CREATE INDEX idx_agent_lesson_status ON public.agent_lesson_learned USING btree (status);

CREATE INDEX idx_agent_lesson_tenant ON public.agent_lesson_learned USING btree (tenant_id);

CREATE POLICY "Tenant isolation for agent_lesson_learned" ON "public"."agent_lesson_learned"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_agent_lessons" ON "public"."agent_lesson_learned"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."agent_lesson_learned" TO "anon", "authenticated", "postgres", "service_role";
