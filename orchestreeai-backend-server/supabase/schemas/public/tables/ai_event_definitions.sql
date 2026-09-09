CREATE TABLE "public"."ai_event_definitions" (
  "id"                       character varying(255) NOT NULL,
  "event_code"               character varying(100) NOT NULL,
  "responsible_persona_type" character varying(100) NOT NULL,
  "severity_default"         character varying(50)  NOT NULL DEFAULT 'HIGH'::character varying,
  "description"              text                   NOT NULL,
  "is_multi_agent_candidate" boolean                NOT NULL DEFAULT false,
  CONSTRAINT "ai_event_definitions_event_code_key" UNIQUE (event_code),
  CONSTRAINT "ai_event_definitions_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."ai_event_definitions"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_ai_event_defs_code ON public.ai_event_definitions USING btree (event_code);

CREATE POLICY "Tenant isolation for ai_event_definitions" ON "public"."ai_event_definitions"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "select_ai_event_definitions" ON "public"."ai_event_definitions"
  FOR SELECT
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_ai_event_definitions" ON "public"."ai_event_definitions"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."ai_event_definitions" TO "anon", "authenticated", "postgres", "service_role";
