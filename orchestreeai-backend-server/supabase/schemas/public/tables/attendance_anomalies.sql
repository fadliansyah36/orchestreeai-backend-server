CREATE TABLE "public"."attendance_anomalies" (
  "id"           text    NOT NULL,
  "tenant_id"    text    NOT NULL,
  "user_id"      text    NOT NULL,
  "user_name"    text    NOT NULL,
  "anomaly_type" text    NOT NULL,
  "details"      text    NOT NULL,
  "severity"     text    NOT NULL DEFAULT 'HIGH'::text,
  "resolved"     boolean NOT NULL DEFAULT false,
  "resolved_by"  text,
  "resolved_at"  bigint,
  "created_at"   bigint  NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "attendance_anomalies_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_att_anom_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE,
  CONSTRAINT "fk_att_anom_user" FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

ALTER TABLE "public"."attendance_anomalies"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_att_anom_tenant ON public.attendance_anomalies USING btree (tenant_id);

CREATE INDEX idx_att_anom_user ON public.attendance_anomalies USING btree (user_id);

CREATE POLICY "Tenant isolation for attendance_anomalies" ON "public"."attendance_anomalies"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_att_anom" ON "public"."attendance_anomalies"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."attendance_anomalies" TO "anon", "authenticated", "postgres", "service_role";
