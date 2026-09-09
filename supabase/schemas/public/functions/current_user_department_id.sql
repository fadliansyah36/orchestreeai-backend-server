CREATE OR REPLACE FUNCTION public.current_user_department_id()
  RETURNS character varying
  LANGUAGE plpgsql
  STABLE
  AS $function$
BEGIN
    RETURN COALESCE(
        current_setting('app.current_user_department_id', true),
        (current_setting('request.jwt.claims', true)::jsonb ->> 'department_id'),
        (SELECT department_id FROM users WHERE id = current_user_id() LIMIT 1),
        (SELECT department_id FROM staff_profiles WHERE user_id = current_user_id() LIMIT 1),
        ''
    );
END;
$function$;

GRANT EXECUTE ON FUNCTION "public"."current_user_department_id"() TO PUBLIC, "anon", "authenticated", "postgres", "service_role";
