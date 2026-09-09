CREATE TABLE "public"."channel_account_permissions" (
  "id"                 character varying(64)    NOT NULL,
  "channel_account_id" character varying(64)    NOT NULL,
  "user_id"            character varying(64)    NOT NULL,
  "permission_level"   character varying(32)    NOT NULL DEFAULT 'OPERATE'::character varying,
  "granted_by"         character varying(64)    NOT NULL,
  "granted_at"         timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "channel_account_permissions_pkey" PRIMARY KEY (id),
  CONSTRAINT "uk_channel_acc_user_perm" UNIQUE (channel_account_id, user_id),
  CONSTRAINT "channel_account_permissions_channel_account_id_fkey" FOREIGN KEY (channel_account_id) REFERENCES public.channel_accounts(id) ON DELETE CASCADE,
  CONSTRAINT "channel_account_permissions_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

ALTER TABLE "public"."channel_account_permissions"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_channel_acc_perm_user ON public.channel_account_permissions USING btree (user_id);

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."channel_account_permissions" TO "anon", "authenticated", "postgres", "service_role";
