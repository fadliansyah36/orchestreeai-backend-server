import os
import re
import glob

with open("methodendpoint2.txt") as f:
    text = f.read()

domains = re.findall(r'##\s+(\d+\.\s+[^\n]+)\n(.*?)(?=\n##\s+\d+\.|\Z)', text, re.DOTALL)

print(f"Total domains found: {len(domains)}")

endpoint_list = []

for d_title, d_body in domains:
    # Match endpoint headers like: ### 1. `GET` /api/v1/health
    ep_matches = re.finditer(r'###\s+(\d+)\.\s+`([A-Z]+)`\s+([^\n]+)\n(.*?)(?=\n###\s+\d+\.|\Z)', d_body, re.DOTALL)
    count = 0
    for m in ep_matches:
        count += 1
        ep_no = m.group(1)
        method = m.group(2)
        path = m.group(3).strip()
        body = m.group(4)
        
        grep_line = re.search(r'-\s+\*\*Grep Confirmation\*\*:\s+Line\s+(\d+)\s+in\s+`([^`]+)`', body)
        grep_file = grep_line.group(2) if grep_line else ""
        grep_lineno = grep_line.group(1) if grep_line else ""
        
        status_line = re.search(r'-\s+\*\*Status Engine Terhubung\*\*:\s+([^\n]+)', body)
        status_engine = status_line.group(1).strip() if status_line else ""
        
        table_line = re.search(r'-\s+\*\*Tabel Supabase Terpengaruh\*\*:\s+([^\n]+)', body)
        table_supabase = table_line.group(1).strip() if table_line else ""
        
        endpoint_list.append({
            "domain": d_title,
            "no": ep_no,
            "method": method,
            "path": path,
            "file": grep_file,
            "lineno": grep_lineno,
            "status_engine": status_engine,
            "table_supabase": table_supabase,
            "full_body": body
        })
    print(f"Domain '{d_title}': {count} endpoints")

print(f"Total endpoints parsed: {len(endpoint_list)}")
