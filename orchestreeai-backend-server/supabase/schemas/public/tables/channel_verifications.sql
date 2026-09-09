CREATE TABLE "public"."channel_verifications" (
  "id"          character varying(64)    NOT NULL,
  "tenant_id"   character varying(64)    NOT NULL,
  "user_id"     character varying(64)    NOT NULL,
  "staff_name"  character varying(255)   NOT NULL,
  "channel"     character varying(32)    NOT NULL,
  "destination" character varying(128)   NOT NULL,
  "otp_code"    character varying(16)    NOT NULL,
  "is_verified" boolean                  NOT NULL DEFAULT false,
  "expires_at"  timestamp with time zone NOT NULL,
  "created_at"  timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "channel_verifications_pkey" PRIMARY KEY (id),
  CONSTRAINT "channel_verifications_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE,
  CONSTRAINT "channel_verifications_user_id_fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

ALTER TABLE "public"."channel_verifications"
  ENABLE ROW LEVEL SECURITY;

CREATE POLICY "tenant_isolation_channel_verifications" ON "public"."channel_verifications"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."channel_verifications" TO "anon", "authenticated", "postgres", "service_role";
