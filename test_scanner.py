import os, re

def get_lines(path, start_pattern, num_lines=4):
    if not os.path.exists(path):
        return f"FILE NOT FOUND: {path}"
    with open(path) as f:
        lines = f.readlines()
    for i, line in enumerate(lines):
        if re.search(start_pattern, line, re.IGNORECASE):
            selected = [l.strip() for l in lines[i:i+num_lines]]
            return f"{path}:{i+1} -> " + " | ".join(selected)
    return f"{path}: pattern '{start_pattern}' not found (total {len(lines)} lines)"

print("1. Core Infra:")
print("Startup:", get_lines("orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/startup/StartupValidator.kt", "validateStartupEnvironment|class StartupValidator"))
print("Supabase:", get_lines("orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/database/SupabaseClientProvider.kt", "fun queryTable|class SupabaseClientProvider"))
print("Redis:", get_lines("orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/database/RedisService.kt", "class RedisService|fun get"))
print("JWT:", get_lines("orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Authentication.kt", "jwt|configureAuthentication"))
print("CORS:", get_lines("orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/HTTP.kt", "install\(CORS\)|CORS"))
print("RateLimiter:", get_lines("orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/security/RateLimiter.kt", "class RateLimiter|fun checkRateLimit"))
print("EncryptionService:", get_lines("orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/security/EncryptionService.kt", "fun encrypt|class EncryptionService"))
print("AuditLogger:", get_lines("orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/security/AuditLogger.kt", "fun log|persistToDatabase"))
