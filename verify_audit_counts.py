import re

with open("audit_step0_report.md") as f:
    text = f.read()

domains = re.split(r'###\s+Domain\s+', text)[1:]

print(f"Total domains in audit_step0_report.md: {len(domains)}")

stats = {"Masih Hardcode": 0, "Belum Terhubung": 0, "Sudah Terhubung": 0, "Stateless": 0}

for d in domains:
    lines = d.strip().split("\n")
    header = lines[0]
    ep_count = 0
    for line in lines:
        if line.startswith("|") and not line.startswith("| No") and not line.startswith("|---"):
            cols = [c.strip() for c in line.split("|")[1:-1]]
            if len(cols) >= 5:
                ep_count += 1
                status = cols[2]
                if "Hardcode" in status:
                    stats["Masih Hardcode"] += 1
                elif "Sudah" in status:
                    stats["Sudah Terhubung"] += 1
                elif "Stateless" in status:
                    stats["Stateless"] += 1
                else:
                    stats["Belum Terhubung"] += 1
    print(f"Domain: {header[:50]} -> {ep_count} endpoints")

print("Overall Status Breakdown:", stats)
print("Total verified endpoints:", sum(stats.values()))
