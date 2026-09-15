import subprocess
import json

token_proc = subprocess.run(
    ['java', '-cp', '.:orchestreeai-backend-server/build/libs/orchestreeai-backend-server-1.0.0-all.jar', 'GenToken'],
    capture_output=True, text=True
)
token = token_proc.stdout.strip()

test_cases = [
    ("Health Check", "GET", "http://localhost:3000/health", {}),
    ("Super Admin Tenants", "GET", "http://localhost:3000/api/v1/admin/tenants", {"Authorization": f"Bearer {token}"}),
    ("Admin Workforce Summary", "GET", "http://localhost:3000/api/v1/admin/analytics/tenant-workforce-summary", {"Authorization": f"Bearer {token}"}),
    ("Admin Studio Templates", "GET", "http://localhost:3000/api/v1/admin/studio/templates", {"Authorization": f"Bearer {token}"}),
    ("Tenant Dashboard Overview", "GET", "http://localhost:3000/api/v1/tenants/tenant-default/dashboard/overview", {"Authorization": f"Bearer {token}"}),
    ("Sales Coach Analytics", "GET", "http://localhost:3000/api/v1/tenants/tenant-default/analytics/sales-coach", {"Authorization": f"Bearer {token}"}),
    ("Attendance History", "GET", "http://localhost:3000/api/v1/attendance/history?tenantId=tenant-default", {"Authorization": f"Bearer {token}"}),
    ("Tenant Geofences", "GET", "http://localhost:3000/api/v1/tenants/tenant-default/geofences", {"Authorization": f"Bearer {token}"}),
    ("Chief of Staff Briefings", "GET", "http://localhost:3000/api/v1/tenants/tenant-default/chief-of-staff/briefings", {"Authorization": f"Bearer {token}"}),
]

print("=================================================================")
print("             COMPREHENSIVE RUNTIME VERIFICATION RESULTS          ")
print("=================================================================")

for name, method, url, headers in test_cases:
    cmd = ["curl", "-s", "-w", "\n%{http_code}", "-X", method, url]
    for k, v in headers.items():
        cmd.extend(["-H", f"{k}: {v}"])
    
    res = subprocess.run(cmd, capture_output=True, text=True)
    parts = res.stdout.strip().split("\n")
    http_code = parts[-1]
    body = "\n".join(parts[:-1])
    
    print(f"\n[TEST] {name}")
    print(f"URL: {url}")
    print(f"Status Code: {http_code}")
    print(f"Response Body (preview): {body[:200]}...")

print("\n=================================================================")
print("                   RAW SUPABASE DATABASE CHECK                  ")
print("=================================================================")
db_check = subprocess.run(
    ["java", "-cp", ".:orchestreeai-backend-server/build/libs/orchestreeai-backend-server-1.0.0-all.jar", "DbCheck"],
    capture_output=True, text=True
)
print(db_check.stdout)
