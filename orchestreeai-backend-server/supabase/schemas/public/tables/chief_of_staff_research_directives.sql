CREATE TABLE "public"."chief_of_staff_research_directives" (
  "id"                        character varying(255) NOT NULL,
  "tenant_id"                 character varying(255) NOT NULL,
  "title"                     character varying(255) NOT NULL,
  "objective"                 text                   NOT NULL,
  "target_department"         character varying(100) NOT NULL,
  "participating_agent_ids"   text                   NOT NULL,
  "participating_agent_names" text                   NOT NULL DEFAULT ''::text,
  "status"                    character varying(50)  NOT NULL DEFAULT 'ACTIVE'::character varying,
  "pattern_identified"        text                   NOT NULL,
  "proposed_knowledge_rule"   text,
  "knowledge_rule_status"     character varying(50)  NOT NULL DEFAULT 'NONE'::character varying,
  "created_at"                bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "updated_at"                bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "chief_of_staff_research_directives_pkey" PRIMARY KEY (id),
  CONSTRAINT "chief_of_staff_research_directives_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."chief_of_staff_research_directives"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_cos_directives_tenant_created ON public.chief_of_staff_research_directives USING btree (tenant_id, created_at);

CREATE INDEX idx_cos_directives_tenant_status ON public.chief_of_staff_research_directives USING btree (tenant_id, status);

CREATE POLICY "Tenant isolation for chief_of_staff_research_directives" ON "public"."chief_of_staff_research_directives"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_chief_of_staff_research_directives" ON "public"."chief_of_staff_research_directives"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

CREATE POLICY "tenant_isolation_chief_of_staff_research_directives" ON "public"."chief_of_staff_research_directives"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id))
  WITH CHECK (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE
  ON TABLE "public"."chief_of_staff_research_directives"
  TO "anon", "authenticated", "postgres", "service_role";
