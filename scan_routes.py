import os
import re

api_dir = "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api"
prospect_dir = "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect"
plugins_dir = "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins"

files_to_scan = [
    os.path.join(plugins_dir, "Routing.kt"),
    os.path.join(api_dir, "AuthRoutes.kt"),
    os.path.join(api_dir, "PresenceRoutes.kt"),
    os.path.join(api_dir, "BillingRoutes.kt"),
    os.path.join(api_dir, "MasterDataRoutes.kt"),
    os.path.join(prospect_dir, "ProspectRoutes.kt"),
    os.path.join(api_dir, "TenantRoutes.kt"),
    os.path.join(api_dir, "OmnichannelSalesRoutes.kt"),
    os.path.join(api_dir, "EnterpriseRoutes.kt"),
    os.path.join(api_dir, "OrchestrationRoutes.kt"),
    os.path.join(api_dir, "ChatRoutes.kt"),
    os.path.join(api_dir, "GenerativeStudioRoutes.kt"),
    os.path.join(api_dir, "AdminRoutes.kt"),
    os.path.join(api_dir, "AttendanceRoutes.kt"),
    os.path.join(api_dir, "MemoryRoutes.kt"),
    os.path.join(api_dir, "SelectionRoutes.kt")
]

for fpath in files_to_scan:
    if not os.path.exists(fpath):
        print(f"MISSING: {fpath}")
        continue
    print(f"\n=================== {os.path.basename(fpath)} ===================")
    with open(fpath, "r", encoding="utf-8") as f:
        lines = f.readlines()
    
    for i, line in enumerate(lines):
        line_str = line.strip()
        # Find route, get, post, put, patch, delete
        m = re.search(r'(\broute|\bget|\bpost|\bput|\bpatch|\bdelete)\s*\(\s*"([^"]*)"', line_str)
        if m:
            verb, path = m.group(1), m.group(2)
            # Peek context
            context_lines = [lines[j].strip() for j in range(i, min(len(lines), i + 25))]
            # Check headers
            headers = [re.search(r'header\(["\']([^"\']+)["\']\)|headers\[["\']([^"\']+)["\']\]', l) for l in context_lines]
            hdrs = set()
            for h in headers:
                if h:
                    hdrs.add(h.group(1) or h.group(2))
            
            # Check receive
            rec = None
            for l in context_lines:
                rm = re.search(r'call\.receive<([^>]+)>\(\)|call\.receiveText\(\)|call\.receiveNullable<([^>]+)>\(\)', l)
                if rm:
                    rec = rm.group(1) or rm.group(2) or "String (raw text)"
                    break
            
            # Check respond
            resp = None
            for l in context_lines:
                rsp = re.search(r'call\.respond\s*\(([^)]+)\)|call\.respondText\s*\(([^)]+)\)', l)
                if rsp:
                    resp = rsp.group(1) or rsp.group(2)
                    break

            print(f"Line {i+1}: {verb.upper()} \"{path}\" | Headers: {list(hdrs)} | Receive: {rec} | Respond: {resp[:60] if resp else None}")
