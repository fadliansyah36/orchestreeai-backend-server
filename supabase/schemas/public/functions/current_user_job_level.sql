CREATE OR REPLACE FUNCTION public.current_user_job_level()
  RETURNS character varying
  LANGUAGE plpgsql
  STABLE
  AS $function$
BEGIN
    RETURN COALESCE(
        current_setting('app.current_user_job_level', true),
        (current_setting('request.jwt.claims', true)::jsonb ->> 'job_level'),
        (SELECT LOWER(job_level) FROM user_persona WHERE user_id = current_user_id() LIMIT 1),
        (SELECT LOWER(job_title) FROM staff_profiles WHERE user_id = current_user_id() LIMIT 1),
        'staff'
    );
END;
$function$;

GRANT EXECUTE ON FUNCTION "public"."current_user_job_level"() TO PUBLIC, "anon", "authenticated", "postgres", "service_role";
