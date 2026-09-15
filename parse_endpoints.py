import re

with open("FINAL_API_ENDPOINTS_DOCUMENTATION.md") as f:
    text = f.read()

# Pattern for domains
domain_matches = list(re.finditer(r"## (\d+)\.\s+([^\n]+)", text))

endpoints_by_domain = {}

for idx, match in enumerate(domain_matches):
    d_num = int(match.group(1))
    d_name = match.group(2).strip()
    start_pos = match.end()
    end_pos = domain_matches[idx + 1].start() if idx + 1 < len(domain_matches) else len(text)
    
    d_text = text[start_pos:end_pos]
    
    # find all endpoints in this domain
    # ### <num>. `METHOD` <path>
    ep_matches = list(re.finditer(r"### (\d+)\.\s+`([A-Z]+)`\s+([^\n]+)", d_text))
    
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
        
    endpoints_by_domain[d_num] = {
        "name": d_name,
        "endpoints": ep_list
    }

print(f"Total domains parsed: {len(endpoints_by_domain)}")
total_eps = sum(len(d["endpoints"]) for d in endpoints_by_domain.values())
print(f"Total endpoints parsed: {total_eps}")
for d_num, d in sorted(endpoints_by_domain.items()):
    print(f"Domain {d_num}: {d['name']} -> {len(d['endpoints'])} endpoints")
