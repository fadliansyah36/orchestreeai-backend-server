CREATE OR REPLACE FUNCTION public.current_user_role()
  RETURNS text
  LANGUAGE plpgsql
  SECURITY DEFINER
  AS $function$
BEGIN
    RETURN COALESCE(current_setting('request.jwt.claims', true)::json->>'role', 'authenticated');
END;
$function$;

GRANT EXECUTE ON FUNCTION "public"."current_user_role"() TO PUBLIC, "anon", "authenticated", "postgres", "service_role";
