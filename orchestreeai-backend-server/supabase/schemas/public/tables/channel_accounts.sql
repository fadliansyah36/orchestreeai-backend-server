CREATE TABLE "public"."channel_accounts" (
  "id"                       character varying(64)    NOT NULL,
  "tenant_id"                character varying(64)    NOT NULL,
  "channel_type"             character varying(64)    NOT NULL,
  "account_label"            character varying(255)   NOT NULL,
  "external_identifier"      character varying(512)   NOT NULL,
  "external_identifier_hash" character varying(64)    NOT NULL,
  "credential_ref"           character varying(255),
  "credentials_encrypted"    text                     NOT NULL DEFAULT ''::text,
  "department_id"            character varying(64),
  "status"                   character varying(64)    NOT NULL DEFAULT 'PENDING_VERIFICATION'::character varying,
  "created_by_user_id"       character varying(64)    NOT NULL,
  "requires_owner_approval"  boolean                  NOT NULL DEFAULT false,
  "total_credit_used"        numeric(12,2)            NOT NULL DEFAULT 0.00,
  "created_at"               timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at"               timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "app_registry_id"          character varying(64),
  "connection_mode"          character varying(64)    NOT NULL DEFAULT 'oauth_platform_app'::character varying,
  "operation_mode"           text                     NOT NULL DEFAULT 'customer_facing_omnichannel'::text,
  CONSTRAINT "channel_accounts_pkey" PRIMARY KEY (id),
  CONSTRAINT "chk_channel_account_operation_mode" CHECK ((operation_mode = ANY (ARRAY['customer_facing_omnichannel'::text, 'internal_proactive_reporting'::text]))),
  CONSTRAINT "uk_tenant_channel_account" UNIQUE (tenant_id, channel_type, external_identifier_hash),
  CONSTRAINT "channel_accounts_department_id_fkey" FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE SET NULL,
  CONSTRAINT "channel_accounts_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE,
  CONSTRAINT "channel_accounts_app_registry_id_fkey" FOREIGN KEY (app_registry_id) REFERENCES public.third_party_app_registry(id) ON DELETE SET NULL
);

ALTER TABLE "public"."channel_accounts"
  ENABLE ROW LEVEL SECURITY;

ALTER TABLE "public"."channel_accounts"
  REPLICA IDENTITY FULL;

CREATE INDEX idx_channel_acc_lookup ON public.channel_accounts USING btree (tenant_id, channel_type, external_identifier_hash);

CREATE INDEX idx_channel_acc_operation_mode ON public.channel_accounts USING btree (tenant_id, operation_mode, status);

CREATE INDEX idx_channel_acc_tenant ON public.channel_accounts USING btree (tenant_id, status);

CREATE POLICY "channel_accounts_tenant_isolation" ON "public"."channel_accounts"
  FOR ALL
  TO PUBLIC
  USING (((tenant_id)::text = current_setting('app.current_tenant_id'::text, true)));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."channel_accounts" TO "anon", "authenticated", "postgres", "service_role";
