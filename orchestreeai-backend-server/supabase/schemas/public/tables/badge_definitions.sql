CREATE TABLE "public"."badge_definitions" (
  "id"              character varying(64)    NOT NULL,
  "name"            character varying(128)   NOT NULL,
  "description"     text                     NOT NULL,
  "icon_key"        character varying(64)    NOT NULL DEFAULT 'military_tech'::character varying,
  "criteria_type"   character varying(64)    NOT NULL,
  "criteria_config" jsonb                    NOT NULL DEFAULT '{}'::jsonb,
  "tier"            character varying(16)    NOT NULL DEFAULT 'BRONZE'::character varying,
  "category"        character varying(32)    NOT NULL DEFAULT 'PERFORMANCE'::character varying,
  "order_index"     integer                  NOT NULL DEFAULT 0,
  "created_at"      timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "badge_definitions_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."badge_definitions"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_badge_criteria ON public.badge_definitions USING btree (criteria_type, tier);

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."badge_definitions" TO "anon", "authenticated", "postgres", "service_role";
