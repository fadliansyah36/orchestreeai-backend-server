CREATE TABLE "public"."agent_skill_growth_log" (
  "id"                 text             NOT NULL,
  "tenant_id"          text             NOT NULL,
  "agent_id"           text             NOT NULL,
  "skill_id"           text             NOT NULL,
  "previous_score"     double precision NOT NULL,
  "new_score"          double precision NOT NULL,
  "delta"              double precision NOT NULL,
  "trigger_outcome_id" text             NOT NULL,
  "reason"             text             NOT NULL,
  "logged_at"          bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "agent_skill_growth_log_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_agent_growth_agent" FOREIGN KEY (agent_id) REFERENCES public.ai_agents(id) ON DELETE CASCADE,
  CONSTRAINT "fk_agent_growth_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."agent_skill_growth_log"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_agent_growth_agent ON public.agent_skill_growth_log USING btree (agent_id);

CREATE INDEX idx_agent_growth_skill ON public.agent_skill_growth_log USING btree (skill_id);

CREATE INDEX idx_agent_growth_tenant ON public.agent_skill_growth_log USING btree (tenant_id);

CREATE POLICY "Tenant isolation for agent_skill_growth_log" ON "public"."agent_skill_growth_log"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_agent_growth" ON "public"."agent_skill_growth_log"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."agent_skill_growth_log" TO "anon", "authenticated", "postgres", "service_role";
