CREATE TABLE "public"."agent_templates" (
  "id"                text    NOT NULL,
  "role"              text    NOT NULL,
  "default_name"      text    NOT NULL,
  "description"       text    NOT NULL,
  "category"          text    NOT NULL,
  "icon_res"          text    NOT NULL DEFAULT 'robot'::text,
  "base_prompt"       text    NOT NULL,
  "default_risk_tier" text    NOT NULL DEFAULT 'LOW'::text,
  "allowed_mcp_tools" text    NOT NULL DEFAULT 'web.fetch, company_brain.query'::text,
  "version"           text    NOT NULL DEFAULT 'v1.0.0'::text,
  "status"            text    NOT NULL DEFAULT 'ACTIVE'::text,
  "usage_count"       integer NOT NULL DEFAULT 1,
  CONSTRAINT "agent_templates_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."agent_templates"
  ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Tenant isolation for agent_templates" ON "public"."agent_templates"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "p_agent_templates_select" ON "public"."agent_templates"
  FOR SELECT
  TO PUBLIC
  USING (true);

CREATE POLICY "p_agent_templates_service" ON "public"."agent_templates"
  FOR ALL
  TO PUBLIC
  USING ((auth.role() = 'service_role'::text));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."agent_templates" TO "anon", "authenticated", "postgres", "service_role";
