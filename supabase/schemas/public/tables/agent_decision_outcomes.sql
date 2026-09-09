CREATE TABLE "public"."agent_decision_outcomes" (
  "id"                     text             NOT NULL,
  "tenant_id"              text             NOT NULL,
  "agent_id"               text             NOT NULL,
  "agent_name"             text             NOT NULL,
  "node_id"                text             NOT NULL,
  "execution_id"           text             NOT NULL,
  "workflow_id"            text             NOT NULL,
  "scenario_context"       text             NOT NULL,
  "action_type"            text             NOT NULL,
  "predicted_impact"       text             NOT NULL,
  "actual_outcome"         text             NOT NULL,
  "outcome_source"         text             NOT NULL,
  "outcome_classification" text             NOT NULL,
  "verified_by"            text             NOT NULL,
  "confidence_delta"       double precision NOT NULL DEFAULT 0.0,
  "metric_impact_json"     text             NOT NULL DEFAULT '{}'::text,
  "created_at"             bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "confidence"             double precision NOT NULL DEFAULT 85.0,
  CONSTRAINT "agent_decision_outcomes_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_agent_outcome_agent" FOREIGN KEY (agent_id) REFERENCES public.ai_agents(id) ON DELETE CASCADE,
  CONSTRAINT "fk_agent_outcome_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."agent_decision_outcomes"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_agent_outcome_action ON public.agent_decision_outcomes USING btree (action_type);

CREATE INDEX idx_agent_outcome_agent ON public.agent_decision_outcomes USING btree (agent_id);

CREATE INDEX idx_agent_outcome_tenant ON public.agent_decision_outcomes USING btree (tenant_id);

CREATE INDEX idx_agent_outcome_workflow ON public.agent_decision_outcomes USING btree (workflow_id);

CREATE POLICY "Tenant isolation for agent_decision_outcomes" ON "public"."agent_decision_outcomes"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_agent_outcomes" ON "public"."agent_decision_outcomes"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."agent_decision_outcomes" TO "anon", "authenticated", "postgres", "service_role";
