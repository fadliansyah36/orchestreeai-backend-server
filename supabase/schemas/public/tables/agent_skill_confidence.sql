CREATE TABLE "public"."agent_skill_confidence" (
  "id"                       text             NOT NULL,
  "tenant_id"                text             NOT NULL,
  "agent_id"                 text             NOT NULL,
  "skill_id"                 text             NOT NULL,
  "skill_name"               text             NOT NULL,
  "current_confidence_score" double precision NOT NULL DEFAULT 50.0,
  "sample_size"              integer          NOT NULL DEFAULT 0,
  "reinforce_count"          integer          NOT NULL DEFAULT 0,
  "correct_count"            integer          NOT NULL DEFAULT 0,
  "rejection_count"          integer          NOT NULL DEFAULT 0,
  "last_evaluated_at"        bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "decay_factor"             double precision NOT NULL DEFAULT 0.98,
  CONSTRAINT "agent_skill_confidence_pkey" PRIMARY KEY (id),
  CONSTRAINT "uq_agent_skill_confidence" UNIQUE (tenant_id, agent_id, skill_id),
  CONSTRAINT "fk_agent_conf_agent" FOREIGN KEY (agent_id) REFERENCES public.ai_agents(id) ON DELETE CASCADE,
  CONSTRAINT "fk_agent_conf_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."agent_skill_confidence"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_agent_conf_agent ON public.agent_skill_confidence USING btree (agent_id);

CREATE INDEX idx_agent_conf_tenant ON public.agent_skill_confidence USING btree (tenant_id);

CREATE POLICY "Tenant isolation for agent_skill_confidence" ON "public"."agent_skill_confidence"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_agent_confidence" ON "public"."agent_skill_confidence"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."agent_skill_confidence" TO "anon", "authenticated", "postgres", "service_role";
