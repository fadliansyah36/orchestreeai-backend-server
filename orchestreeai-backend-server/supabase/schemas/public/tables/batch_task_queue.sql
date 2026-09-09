CREATE TABLE "public"."batch_task_queue" (
  "id"                        text             NOT NULL,
  "tenant_id"                 text             NOT NULL,
  "task_type"                 text             NOT NULL,
  "payload_json"              text             NOT NULL DEFAULT '{}'::text,
  "status"                    text             NOT NULL DEFAULT 'PENDING'::text,
  "batch_job_id"              text,
  "result_json"               text,
  "discount_pct"              double precision NOT NULL DEFAULT 50.0,
  "cost_without_discount_usd" double precision NOT NULL DEFAULT 0.0,
  "actual_cost_usd"           double precision NOT NULL DEFAULT 0.0,
  "created_at"                bigint           NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "completed_at"              bigint,
  CONSTRAINT "batch_task_queue_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_batch_task_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."batch_task_queue"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_batch_task_status ON public.batch_task_queue USING btree (status);

CREATE INDEX idx_batch_task_tenant ON public.batch_task_queue USING btree (tenant_id);

CREATE INDEX idx_batch_task_type ON public.batch_task_queue USING btree (task_type);

CREATE POLICY "Tenant isolation for batch_task_queue" ON "public"."batch_task_queue"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_batch_tasks" ON "public"."batch_task_queue"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."batch_task_queue" TO "anon", "authenticated", "postgres", "service_role";
