CREATE TABLE "public"."attendance_records" (
  "id"                        text             NOT NULL,
  "tenant_id"                 text             NOT NULL,
  "user_id"                   text             NOT NULL,
  "user_name"                 text             NOT NULL,
  "department_name"           text             NOT NULL DEFAULT 'Operasional'::text,
  "attendance_date"           text             NOT NULL,
  "check_in_time"             bigint,
  "check_in_lat"              double precision,
  "check_in_lng"              double precision,
  "check_in_photo_url"        text,
  "check_in_face_verified"    boolean          NOT NULL DEFAULT false,
  "check_in_inside_geofence"  boolean          NOT NULL DEFAULT false,
  "check_out_time"            bigint,
  "check_out_lat"             double precision,
  "check_out_lng"             double precision,
  "check_out_photo_url"       text,
  "check_out_face_verified"   boolean          NOT NULL DEFAULT false,
  "check_out_inside_geofence" boolean          NOT NULL DEFAULT false,
  "status"                    text             NOT NULL DEFAULT 'PRESENT'::text,
  "work_duration_minutes"     integer          NOT NULL DEFAULT 0,
  "notes"                     text             NOT NULL DEFAULT ''::text,
  "anomaly_flag"              boolean          NOT NULL DEFAULT false,
  "anomaly_reason"            text,
  "created_at"                bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "attendance_records_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_att_rec_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE,
  CONSTRAINT "fk_att_rec_user" FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

ALTER TABLE "public"."attendance_records"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_att_rec_date ON public.attendance_records USING btree (attendance_date);

CREATE INDEX idx_att_rec_tenant_date ON public.attendance_records USING btree (tenant_id, attendance_date);

CREATE INDEX idx_att_rec_tenant ON public.attendance_records USING btree (tenant_id);

CREATE INDEX idx_att_rec_user ON public.attendance_records USING btree (user_id);

CREATE POLICY "Tenant isolation for attendance_records" ON "public"."attendance_records"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_att_rec" ON "public"."attendance_records"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."attendance_records" TO "anon", "authenticated", "postgres", "service_role";
