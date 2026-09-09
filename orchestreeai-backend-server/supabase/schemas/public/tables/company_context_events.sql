CREATE TABLE "public"."company_context_events" (
  "id"                      character varying(64)    NOT NULL,
  "tenant_id"               character varying(64)    NOT NULL,
  "event_code"              character varying(64)    NOT NULL,
  "title"                   character varying(256)   NOT NULL,
  "description"             text                     NOT NULL,
  "entity_reference"        character varying(128)   NOT NULL,
  "contributing_systems"    jsonb                    NOT NULL DEFAULT '[]'::jsonb,
  "contributing_stream_ids" jsonb                    NOT NULL DEFAULT '[]'::jsonb,
  "risk_score"              double precision         NOT NULL DEFAULT 0.0,
  "confidence_score"        double precision         NOT NULL DEFAULT 0.0,
  "impact_level"            character varying(32)    NOT NULL DEFAULT 'MEDIUM'::character varying,
  "status"                  character varying(32)    NOT NULL DEFAULT 'NEW'::character varying,
  "recommended_action"      text                     NOT NULL DEFAULT ''::text,
  "assigned_persona_code"   character varying(64)    NOT NULL DEFAULT 'CHIEF_OF_STAFF'::character varying,
  "created_at"              timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at"              timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "company_context_events_pkey" PRIMARY KEY (id),
  CONSTRAINT "company_context_events_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."company_context_events"
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE "public"."company_context_events"
  REPLICA IDENTITY FULL;

CREATE INDEX idx_context_events_tenant_created ON public.company_context_events USING btree (tenant_id, created_at DESC);

CREATE INDEX idx_context_events_tenant_entity ON public.company_context_events USING btree (tenant_id, entity_reference);

CREATE INDEX idx_context_events_tenant_status ON public.company_context_events USING btree (tenant_id, status);

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."company_context_events" TO "anon", "authenticated", "postgres", "service_role";
