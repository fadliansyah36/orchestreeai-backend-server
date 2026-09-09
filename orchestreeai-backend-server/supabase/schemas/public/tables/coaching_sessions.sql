CREATE TABLE "public"."coaching_sessions" (
  "id"                  text   NOT NULL,
  "tenant_id"           text   NOT NULL,
  "staff_id"            text   NOT NULL,
  "staff_name"          text   NOT NULL,
  "department_name"     text   NOT NULL,
  "coach_type"          text   NOT NULL DEFAULT 'AI_PERFORMANCE_COACH'::text,
  "focus_area"          text   NOT NULL,
  "trigger_metric"      text   NOT NULL,
  "coaching_tips"       text   NOT NULL,
  "action_items_json"   text   NOT NULL DEFAULT '[]'::text,
  "status"              text   NOT NULL DEFAULT 'ACTIVE'::text,
  "scheduled_follow_up" bigint NOT NULL DEFAULT (((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint + 604800000),
  "created_at"          bigint NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "coaching_sessions_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_coaching_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE,
  CONSTRAINT "fk_coaching_staff" FOREIGN KEY (staff_id) REFERENCES public.users(id) ON DELETE CASCADE
);

ALTER TABLE "public"."coaching_sessions"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_coaching_staff ON public.coaching_sessions USING btree (staff_id);

CREATE INDEX idx_coaching_tenant ON public.coaching_sessions USING btree (tenant_id);

CREATE POLICY "Tenant isolation for coaching_sessions" ON "public"."coaching_sessions"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_coaching" ON "public"."coaching_sessions"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."coaching_sessions" TO "anon", "authenticated", "postgres", "service_role";
