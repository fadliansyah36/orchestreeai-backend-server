import os
import glob
import re

target_tables = [
    "subscription_plans", "feature_capabilities", "tenant_capability_overrides",
    "enterprise_system_connections", "enterprise_data_sync_jobs", "enterprise_ingested_records",
    "ai_data_permission_policies", "ai_data_access_requests"
]

print("=== CHECKING TABLES IN MIGRATIONS & SCHEMAS ===")
migration_files = glob.glob("orchestreeai-backend-server/db/migrations/*.sql") + glob.glob("orchestreeai-backend-server/supabase/schemas/**/*.sql", recursive=True)

for t in target_tables:
    found = []
    for f in migration_files:
        with open(f) as fh:
            c = fh.read()
            if re.search(r'\b' + re.escape(t) + r'\b', c, re.IGNORECASE):
                found.append(f)
    print(f"TABLE: {t} -> {'Found in ' + str(found) if found else 'TIDAK ADA'}")

print("\n=== CHECKING CODE REFERENCES ===")
search_patterns = [
    "isCapabilityEnabled", "enforceCapabilityGate", "checkAiDataPermission",
    "CapabilityNotAvailableException", "DENIED_NO_POLICY", "McpToolRegistry"
]

all_kt = glob.glob("orchestreeai-backend-server/src/**/*.kt", recursive=True)

for p in search_patterns:
    matches = []
    for f in all_kt:
        with open(f) as fh:
            c = fh.read()
            if p in c:
                # get line numbers
                for idx, l in enumerate(c.splitlines()):
                    if p in l:
                        matches.append(f"{f}:{idx+1}: {l.strip()[:80]}")
    print(f"PATTERN: {p} -> {len(matches)} occurrences")
    for m in matches[:5]:
        print(f"   {m}")
