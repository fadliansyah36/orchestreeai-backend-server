CREATE TABLE "public"."agent_skills" (
  "id"          character varying(64)    NOT NULL,
  "name"        character varying(128)   NOT NULL,
  "category"    character varying(64)    NOT NULL,
  "description" text,
  "created_at"  timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "agent_skills_name_key" UNIQUE (name),
  CONSTRAINT "agent_skills_pkey" PRIMARY KEY (id)
);

ALTER TABLE "public"."agent_skills"
  ENABLE ROW LEVEL SECURITY;

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."agent_skills" TO "anon", "authenticated", "postgres", "service_role";
