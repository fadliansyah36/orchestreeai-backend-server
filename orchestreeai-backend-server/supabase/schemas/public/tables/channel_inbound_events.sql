CREATE TABLE "public"."channel_inbound_events" (
  "id"              character varying(64)    NOT NULL DEFAULT (gen_random_uuid())::text,
  "tenant_id"       character varying(64),
  "source_platform" character varying(64)    NOT NULL,
  "event_type"      character varying(128)   NOT NULL,
  "raw_payload"     jsonb                    NOT NULL,
  "received_at"     timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "processed_at"    timestamp with time zone,
  "status"          character varying(32)    NOT NULL DEFAULT 'RECEIVED'::character varying,
  "retry_count"     integer                  NOT NULL DEFAULT 0,
  "error_log"       text,
  CONSTRAINT "channel_inbound_events_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."channel_inbound_events"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_channel_inbound_status ON public.channel_inbound_events USING btree (status, received_at);

CREATE POLICY "allow_service_role_all_inbound_events" ON "public"."channel_inbound_events"
  FOR ALL
  TO "authenticated"
  USING (true)
  WITH CHECK (true);

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."channel_inbound_events" TO "anon", "authenticated", "postgres", "service_role";

COMMENT ON TABLE "public"."channel_inbound_events" IS 'Fail-closed event ingestion queue for external channel webhooks and fintech callbacks.';
