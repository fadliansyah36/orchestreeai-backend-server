import os, re

api_dir = "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api"
files = sorted([f for f in os.listdir(api_dir) if f.endswith(".kt")])

for f in files:
    path = os.path.join(api_dir, f)
    with open(path) as fp:
        lines = fp.readlines()
    
    # check occurrences of mock keywords
    mock_lines = []
    for i, l in enumerate(lines):
        if any(w in l.lower() for w in ["mock", "fake", "dummy", "sample-", "demo-"]):
            if not any(ign in l for ign in ["tenant-demo-sandbox", "sample-", "import", "//", "fun mock", "Mock"]):
                mock_lines.append((i+1, l.strip()))
    if mock_lines:
        print(f"=== {f} ({len(mock_lines)} occurrences) ===")
        for ln, text in mock_lines[:5]:
            print(f"  Line {ln}: {text[:100]}")
