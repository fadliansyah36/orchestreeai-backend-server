CREATE TABLE "public"."company_activity_stream" (
  "id"               character varying(64)    NOT NULL,
  "tenant_id"        character varying(64)    NOT NULL,
  "connection_id"    character varying(64)    NOT NULL,
  "system_type"      character varying(64)    NOT NULL,
  "record_id"        character varying(64),
  "entity_reference" character varying(128)   NOT NULL,
  "activity_type"    character varying(64)    NOT NULL,
  "summary"          text                     NOT NULL,
  "data_mode"        character varying(32)    NOT NULL DEFAULT 'NEAR_REALTIME'::character varying,
  "event_timestamp"  timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "metadata_json"    jsonb                    NOT NULL DEFAULT '{}'::jsonb,
  "created_at"       timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "company_activity_stream_pkey" PRIMARY KEY (id),
  CONSTRAINT "company_activity_stream_record_id_fkey" FOREIGN KEY (record_id) REFERENCES public.enterprise_ingested_records(id) ON DELETE CASCADE,
  CONSTRAINT "company_activity_stream_connection_id_fkey" FOREIGN KEY (connection_id) REFERENCES public.enterprise_system_connections(id) ON DELETE CASCADE,
  CONSTRAINT "company_activity_stream_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."company_activity_stream"
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE "public"."company_activity_stream"
  REPLICA IDENTITY FULL;

CREATE INDEX idx_activity_stream_tenant_conn ON public.company_activity_stream USING btree (tenant_id, connection_id);

CREATE INDEX idx_activity_stream_tenant_entity ON public.company_activity_stream USING btree (tenant_id, entity_reference, event_timestamp);

CREATE INDEX idx_activity_stream_tenant_time ON public.company_activity_stream USING btree (tenant_id, event_timestamp DESC);

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."company_activity_stream" TO "anon", "authenticated", "postgres", "service_role";
