import os, re

def check_file_evidence(path, terms):
    if not os.path.exists(path):
        return "NOT_FOUND", "File not found"
    with open(path) as f:
        content = f.read()
    
    found_lines = []
    lines = content.split('\n')
    for term in terms:
        for i, line in enumerate(lines):
            if re.search(term, line, re.IGNORECASE):
                # get a clean snippet
                snippet = line.strip()
                if len(snippet) > 120:
                    snippet = snippet[:120] + "..."
                found_lines.append((i+1, snippet))
                break
    return "FOUND", found_lines

print("Testing python analysis...")
