CREATE OR REPLACE FUNCTION public.current_user_id()
  RETURNS character varying
  LANGUAGE plpgsql
  STABLE
  AS $function$
BEGIN
    RETURN COALESCE(
        current_setting('app.current_user_id', true),
        (current_setting('request.jwt.claims', true)::jsonb ->> 'sub'),
        ''
    );
END;
$function$;

GRANT EXECUTE ON FUNCTION "public"."current_user_id"() TO PUBLIC, "anon", "authenticated", "postgres", "service_role";
