CREATE TABLE "public"."chat_messages" (
  "id"                    text   NOT NULL,
  "tenant_id"             text   NOT NULL,
  "sender_type"           text   NOT NULL,
  "sender_name"           text   NOT NULL,
  "message"               text   NOT NULL,
  "channel"               text   NOT NULL DEFAULT 'WEB'::text,
  "thread_id"             text   NOT NULL DEFAULT 'default-thread'::text,
  "media_url"             text,
  "artifact_type"         text,
  "artifact_id"           text,
  "workflow_execution_id" text,
  "task_id"               text,
  "status"                text   NOT NULL DEFAULT 'SENT'::text,
  "timestamp"             bigint NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  CONSTRAINT "chat_messages_pkey" PRIMARY KEY (id),
  CONSTRAINT "fk_chat_messages_tenant" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."chat_messages"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_chat_messages_tenant_id ON public.chat_messages USING btree (tenant_id);

CREATE INDEX idx_chat_messages_thread_id ON public.chat_messages USING btree (thread_id);

CREATE INDEX idx_chat_messages_timestamp ON public.chat_messages USING btree ("timestamp");

CREATE POLICY "Tenant isolation for chat_messages" ON "public"."chat_messages"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "tenant_isolation_chat_messages" ON "public"."chat_messages"
  FOR ALL
  TO PUBLIC
  USING ((public.app_has_tenant_access((tenant_id)::character varying) OR (auth.role() = 'service_role'::text)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."chat_messages" TO "anon", "authenticated", "postgres", "service_role";
