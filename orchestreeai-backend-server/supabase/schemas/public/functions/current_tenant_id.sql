CREATE OR REPLACE FUNCTION public.current_tenant_id()
  RETURNS character varying
  LANGUAGE plpgsql
  STABLE
  AS $function$
BEGIN
    RETURN COALESCE(
        current_setting('app.current_tenant_id', true),
        (current_setting('request.jwt.claims', true)::jsonb ->> 'tenant_id'),
        ''
    );
END;
$function$;

GRANT EXECUTE ON FUNCTION "public"."current_tenant_id"() TO PUBLIC, "anon", "authenticated", "postgres", "service_role";
