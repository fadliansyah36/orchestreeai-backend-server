CREATE TABLE "public"."company_brain_documents" (
  "id"                 character varying(255) NOT NULL,
  "tenant_id"          character varying(255) NOT NULL,
  "file_name"          character varying(255) NOT NULL,
  "file_type"          character varying(50)  NOT NULL,
  "document_category"  character varying(100) NOT NULL,
  "object_storage_url" text                   NOT NULL,
  "extraction_status"  character varying(50)  NOT NULL DEFAULT 'processing'::character varying,
  "uploaded_by"        character varying(255) NOT NULL DEFAULT 'CEO / Super Admin'::character varying,
  "uploaded_at"        bigint                 NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  "file_size_bytes"    bigint                 NOT NULL DEFAULT 0,
  "extracted_summary"  text,
  "chunks_count"       integer                NOT NULL DEFAULT 0,
  "error_message"      text,
  CONSTRAINT "company_brain_documents_pkey" PRIMARY KEY (id),
  CONSTRAINT "company_brain_documents_tenant_id_fkey" FOREIGN KEY (tenant_id) REFERENCES public.tenants(id) ON DELETE CASCADE
);

ALTER TABLE "public"."company_brain_documents"
  ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_company_brain_docs_category ON public.company_brain_documents USING btree (document_category);

CREATE INDEX idx_company_brain_docs_status ON public.company_brain_documents USING btree (extraction_status);

CREATE INDEX idx_company_brain_docs_tenant ON public.company_brain_documents USING btree (tenant_id);

CREATE INDEX idx_company_brain_docs_uploaded_at ON public.company_brain_documents USING btree (uploaded_at);

CREATE POLICY "Tenant isolation for company_brain_documents" ON "public"."company_brain_documents"
  FOR ALL
  TO PUBLIC
  USING (true);

CREATE POLICY "service_role_company_brain_documents" ON "public"."company_brain_documents"
  FOR ALL
  TO "service_role"
  USING (true)
  WITH CHECK (true);

CREATE POLICY "tenant_isolation_company_brain_documents" ON "public"."company_brain_documents"
  FOR ALL
  TO PUBLIC
  USING (public.app_has_tenant_access(tenant_id))
  WITH CHECK (public.app_has_tenant_access(tenant_id));

GRANT DELETE, INSERT, MAINTAIN, REFERENCES, SELECT, TRIGGER, TRUNCATE, UPDATE ON TABLE "public"."company_brain_documents" TO "anon", "authenticated", "postgres", "service_role";
