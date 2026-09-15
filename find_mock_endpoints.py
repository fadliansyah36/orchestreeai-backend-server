import os, re

api_dir = "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api"
files = [f for f in os.listdir(api_dir) if f.endswith(".kt")]

for f in sorted(files):
    path = os.path.join(api_dir, f)
    with open(path) as fp:
        lines = fp.readlines()
    
    current_endpoint = None
    for i, line in enumerate(lines):
        line_s = line.strip()
        if re.match(r'^(get|post|put|delete|patch)\s*\(', line_s):
            current_endpoint = line_s
        if "listOf(" in line and current_endpoint:
            if "emptyList()" not in line and "listOf()" not in line:
                print(f"{f}:{i+1} [{current_endpoint}] -> {line_s[:100]}")
