CREATE OR REPLACE FUNCTION public.app_has_tenant_access (
  record_tenant_id character varying
)
  RETURNS boolean
  LANGUAGE plpgsql
  SECURITY DEFINER
  AS $function$
BEGIN
    RETURN (
        current_setting('app.is_super_admin', true) = 'true'
        OR current_setting('app.current_tenant_id', true) = record_tenant_id
    );
END;
$function$;

GRANT EXECUTE ON FUNCTION "public"."app_has_tenant_access"(character varying) TO PUBLIC, "anon", "authenticated", "postgres", "service_role";
