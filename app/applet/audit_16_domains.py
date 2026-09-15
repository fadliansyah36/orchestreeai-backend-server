import re
import os

with open("methodendpoint2.txt", "r") as f:
    text = f.read()

domain_sections = re.findall(r'##\s+(\d+\..*?)\n(.*?)(?=\n##\s+\d+\.|\Z)', text, re.DOTALL)
print(f"Discovered domain sections: {len(domain_sections)}")

total = 0
for title, body in domain_sections:
    # Look for ### 1. `METHOD` path
    endpoints = re.findall(r'###\s+\d+\.\s+`([A-Z]+)`\s+([^\n]+)', body)
    total += len(endpoints)
    print(f"{title}: {len(endpoints)} endpoints")

print(f"TOTAL: {total}")
