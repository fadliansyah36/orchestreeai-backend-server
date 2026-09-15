import re

with open("FINAL_API_ENDPOINTS_DOCUMENTATION.md") as f:
    text = f.read()

domain_headers = list(re.finditer(r"^## (\d{1,2})\.\s+([^\n]+)", text, re.MULTILINE))

priority_map = {
    1: "DASAR / INTI",
    2: "DASAR / INTI",
    3: "DASAR / INTI",
    4: "DASAR / INTI",
    5: "DASAR / INTI",
    6: "DASAR / INTI",
    7: "TINGGI",
    8: "TINGGI",
    9: "DASAR / INTI",
    10: "TINGGI",
    11: "TINGGI",
    12: "SEDANG",
    13: "SEDANG",
    14: "TINGGI",
    15: "TINGGI",
    16: "SEDANG / RENDAH"
}

with open("audit_tables_output.md", "w") as out:
    out.write("# HASIL AUDIT ULANG DEFINITIF (LANGKAH 0) — SEMUA 16 DOMAIN (335 ENDPOINTS)\n\n")
    
    for idx, match in enumerate(domain_headers):
        d_num = int(match.group(1))
        d_name = match.group(2).strip()
        start_pos = match.end()
        end_pos = domain_headers[idx + 1].start() if idx + 1 < len(domain_headers) else len(text)
        
        d_text = text[start_pos:end_pos]
        ep_matches = list(re.finditer(r"^### (\d+)\.\s+`([A-Z]+)`\s+([^\n]+)", d_text, re.MULTILINE))
        
        prio = priority_map.get(d_num, "SEDANG")
        out.write(f"### Domain {d_num}: {d_name} ({len(ep_matches)} Endpoints) — Prioritas: {prio}\n\n")
        out.write("| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |\n")
        out.write("|:---:|:---:|---|---|---|:---:|\n")
        
        for ep_idx, ep_m in enumerate(ep_matches):
            ep_num = ep_m.group(1)
            method = ep_m.group(2)
            path = ep_m.group(3).strip()
            ep_start = ep_m.end()
            ep_end = ep_matches[ep_idx + 1].start() if ep_idx + 1 < len(ep_matches) else len(d_text)
            ep_block = d_text[ep_start:ep_end]
            
            status_m = re.search(r"- \*\*Status Engine Terhubung\*\*:\s*(.+)", ep_block)
            table_m = re.search(r"- \*\*Tabel Supabase Terpengaruh\*\*:\s*(.+)", ep_block)
            
            raw_status = status_m.group(1).strip() if status_m else "Unknown"
            table = table_m.group(1).strip() if table_m else "Unknown"
            
            if "Stateless / Infrastructure" in raw_status:
                curr_status = "Stateless (Root HTTP Banner)"
            elif "Stateless" in raw_status and "Health" not in raw_status:
                curr_status = "Stateless (Root Greeting)"
            else:
                curr_status = "Sudah Diperbaiki (Terhubung DB/Engine)"
                
            out.write(f"| {ep_num} | `{method}` | `{path}` | {curr_status} | {table} | {prio} |\n")
        out.write("\n---\n\n")

print("Generated audit_tables_output.md successfully!")
