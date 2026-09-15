import re

with open("FINAL_API_ENDPOINTS_DOCUMENTATION.md") as f:
    text = f.read()

# Pattern for the 16 main domains: ## 1. Core Infrastructure ... up to ## 16. Super Admin ...
domain_headers = list(re.finditer(r"^## (\d{1,2})\.\s+([^\n]+)", text, re.MULTILINE))

print(f"Found {len(domain_headers)} domain headers matching ^## \\d{{1,2}}\\.")
for m in domain_headers:
    print(f"Domain {m.group(1)}: {m.group(2)}")

endpoints_by_domain = []

for idx, match in enumerate(domain_headers):
    d_num = int(match.group(1))
    d_name = match.group(2).strip()
    start_pos = match.end()
    end_pos = domain_headers[idx + 1].start() if idx + 1 < len(domain_headers) else len(text)
    
    d_text = text[start_pos:end_pos]
    
    ep_matches = list(re.finditer(r"^### (\d+)\.\s+`([A-Z]+)`\s+([^\n]+)", d_text, re.MULTILINE))
    
    ep_list = []
    for ep_idx, ep_m in enumerate(ep_matches):
        ep_num = ep_m.group(1)
        method = ep_m.group(2)
        path = ep_m.group(3).strip()
        ep_start = ep_m.end()
        ep_end = ep_matches[ep_idx + 1].start() if ep_idx + 1 < len(ep_matches) else len(d_text)
        ep_block = d_text[ep_start:ep_end]
        
        status_m = re.search(r"- \*\*Status Engine Terhubung\*\*:\s*(.+)", ep_block)
        table_m = re.search(r"- \*\*Tabel Supabase Terpengaruh\*\*:\s*(.+)", ep_block)
        
        status = status_m.group(1).strip() if status_m else "Unknown"
        table = table_m.group(1).strip() if table_m else "Unknown"
        
        ep_list.append({
            "num": ep_num,
            "method": method,
            "path": path,
            "status": status,
            "table": table
        })
        
    endpoints_by_domain.append({
        "domain_num": d_num,
        "name": d_name,
        "endpoints": ep_list
    })

total_eps = sum(len(d["endpoints"]) for d in endpoints_by_domain)
print(f"\nTotal endpoints parsed across 16 domains: {total_eps}")
for d in endpoints_by_domain:
    print(f"Domain {d['domain_num']}: {d['name']} -> {len(d['endpoints'])} endpoints")
