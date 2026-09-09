CREATE TABLE "public"."channel_account_persona_assignments" (
  "id"                 character varying(64)    NOT NULL,
  "channel_account_id" character varying(64)    NOT NULL,
  "ai_agent_id"        character varying(64)    NOT NULL,
  "is_primary"         boolean                  NOT NULL DEFAULT false,
  "assigned_at"        timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "channel_account_persona_assignments_ai_agent_id_fkey" FOREIGN KEY (ai_agent_id) REFERENCES public.ai_agents(id) ON DELETE CASCADE,
  CONSTRAINT "channel_account_persona_assignments_pkey" PRIMARY KEY (id),
  CONSTRAINT "uk_channel_acc_agent_assignment" UNIQUE (channel_account_id, ai_agent_id),
  CONSTRAINT "channel_account_persona_assignments_channel_account_id_fkey" FOREIGN KEY (channel_account_id) REFERENCES public.channel_accounts(id) ON DELETE CASCADE
);

ALTER TABLE "public"."channel_account_persona_assignments"
  ENABLE ROW LEVEL SECURITY;

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE
  ON TABLE "public"."channel_account_persona_assignments"
  TO "anon", "authenticated", "postgres", "service_role";
