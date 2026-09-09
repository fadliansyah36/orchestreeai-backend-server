CREATE OR REPLACE FUNCTION public.prevent_ai_credit_ledger_mutation()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  AS $function$
BEGIN
    RAISE EXCEPTION 'ai_credit_ledger is strictly append-only. UPDATE and DELETE are prohibited.';
END;
$function$;

GRANT EXECUTE ON FUNCTION "public"."prevent_ai_credit_ledger_mutation"() TO PUBLIC, "anon", "authenticated", "postgres", "service_role";
