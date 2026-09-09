CREATE OR REPLACE FUNCTION public.current_user_collaboration_scope()
  RETURNS text[]
  LANGUAGE plpgsql
  STABLE
  AS $function$
BEGIN
    RETURN COALESCE(
        (SELECT array_agg(t.job_code) FROM (
            SELECT unnest(collaborating_ai_job_title_ids)::text AS job_code 
            FROM proactive_collaboration_scope 
            WHERE staff_id = current_user_id()
        ) t),
        ARRAY[]::TEXT[]
    );
END;
$function$;

GRANT EXECUTE ON FUNCTION "public"."current_user_collaboration_scope"() TO PUBLIC, "anon", "authenticated", "postgres", "service_role";
