import os
import re
import json

# 1. Map all data classes
models = {}
for root, _, files in os.walk("orchestreeai-backend-server/src/main/kotlin"):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path) as f:
                content = f.read()
            for m in re.finditer(r"@Serializable\s*(?:(?:private|internal|public)\s+)?data class\s+([A-Za-z0-9_]+)\s*\((.*?)\)", content, re.DOTALL):
                name = m.group(1)
                body = " ".join(m.group(2).split())
                models[name] = body

def get_model_desc(name):
    if not name or name == "None":
        return "None (No Request Body)"
    clean = name.replace("List<", "").replace(">", "").strip()
    if clean in models:
        return f"`{name}`: `{models[clean]}`"
    return f"`{name}`"

KNOWN_ENGINES = [
    ("OrchestrationEngine", "OrchestrationEngine (Workflow DAG & Autonomous Execution)"),
    ("ModelRouter", "ModelRouter (Multi-LLM Routing, Latency-Cost Optimization & Fallbacks)"),
    ("SelectionEngine", "SelectionEngine (Universal Data Selection, Matching & Document Understanding)"),
    ("CustomerIdentityResolutionEngine", "CustomerIdentityResolutionEngine (Cross-channel Identity Stitching & Resolution)"),
    ("CentralCreditLedger", "CentralCreditLedger (Atomic Real-time Credit Quota, Ledger & Deductions)"),
    ("CommercialCreditEngine", "CommercialCreditEngine (Commercial Metering, Dynamic Rate Cards & Pricing Matrix)"),
    ("DunningEngine", "DunningEngine (Invoice Dunning, Grace Periods & Payment Recovery)"),
    ("EntitlementEngine", "EntitlementEngine (Tier-based Feature Gates, Limits & Quota Enforcement)"),
    ("PaymentReconciliationEngine", "PaymentReconciliationEngine (Payment Matching, Mutation Tracking & Order Settling)"),
    ("GeofenceEngine", "GeofenceEngine (Polygon & Haversine GPS Radius Verification)"),
    ("AttendanceAnomalyEngine", "AttendanceAnomalyEngine (Facial Liveness, Shift Deviation & Spoof Detection)"),
    ("GenerativeStudioService", "GenerativeStudioService (Multimodal Asset, Prompt Composition & Image Synthesis)"),
    ("BrandAssetService", "BrandAssetService (Tenant Brand Voice, Logo Storage & Visual Style Injection)"),
    ("MemoryConsolidator", "MemoryConsolidator (Autonomous Short-to-Long Term Semantic Consolidation)"),
    ("MemoryDecayEngine", "MemoryDecayEngine (Ebbinghaus Forgetting Curve & Importance Weight Decay)"),
    ("HybridSearchEngine", "HybridSearchEngine (Vector Semantic Similarity + BM25 Lexical Hybrid Search)"),
    ("ChiefOfStaffEngine", "ChiefOfStaffEngine (Strategic Synthesis, Executive Briefings & Directives)"),
    ("SalesPersonaEngine", "SalesPersonaEngine (Adaptive Dynamic Tone, Persona & Sales Guardrails)"),
    ("LeadQualificationEngine", "LeadQualificationEngine (Real-time BANT Scoring & Prospect Segmentation)"),
    ("AbandonedCartRecoveryEngine", "AbandonedCartRecoveryEngine (Automated Cart Abandonment Sequences & Recovery)"),
    ("CourierTrackingEngine", "CourierTrackingEngine (Real-time Waybill Logistics Tracking & Multi-courier Webhooks)"),
    ("ProactiveEngine", "ProactiveEngine (Autonomous Event-driven Proactive Workforce Dispatch)"),
    ("ProactiveToneRiskEngine", "ProactiveToneRiskEngine (Outbound Message Quality, Risk & Tone Guardrail)"),
    ("ContinuousLearningCore", "ContinuousLearningCore (Reinforcement Feedback Loop, Node Outcomes & Skill Evolution)"),
    ("McpGovernanceEngine", "McpGovernanceEngine (MCP Tool Sandboxing, Permission Scopes & Emergency Kill-Switch)"),
    ("CompetitorIntelligenceEngine", "CompetitorIntelligenceEngine (Autonomous Competitor Crawling & Market Radar)"),
    ("AdminSecurityService", "AdminSecurityService (IP Allowlisting, Step-up MFA & CSRF Token Validation)"),
    ("PresenceService", "PresenceService (Facial Liveness, Anti-spoofing & Biometric Verification)"),
    ("ChannelGateway", "ChannelGateway (Unified Omnichannel Inbound/Outbound Message Routing & Event Gateway)")
]

TABLE_MAPPINGS = [
    "users", "user_sessions", "tenants", "departments", "staff", "ai_agents",
    "tasks", "task_checklists", "task_attachments", "task_activities",
    "presence_logs", "user_presence_logs", "biometric_enrollments",
    "attendance_records", "attendance_anomalies", "geofences",
    "prospect_registrations", "commercial_plans", "commercial_entitlements",
    "tenant_commercial_overrides", "credit_metering_rules", "credit_cost_factors",
    "ai_credit_wallets", "ai_credit_ledger", "ai_credit_topup_packages",
    "invoices", "payment_reconciliations", "reconciliation_orders",
    "channel_accounts", "channel_inbound_events", "customers", "customer_identities",
    "leads", "products", "inventory_variants", "orders", "carts", "cart_items",
    "campaigns", "service_requests", "ab_experiments",
    "workflow_definitions", "workflow_executions", "dead_letter_queue",
    "selection_requests", "selection_results", "selection_auto_configs", "selection_calibration_datasets",
    "company_brain_documents", "generative_studio_assets", "brand_logos",
    "enterprise_connections", "ai_data_permission_policies", "knowledge_rules",
    "chief_of_staff_briefings", "security_anomalies", "data_subject_requests",
    "audit_logs", "llm_providers", "mcp_tools", "app_registry", "system_ip_allowlist"
]

def extract_handler_block(lines, start_idx):
    # Find the opening brace of handler
    brace_count = 0
    started = False
    block_lines = []
    
    for i in range(start_idx, len(lines)):
        line = lines[i]
        block_lines.append(line)
        for ch in line:
            if ch == '{':
                brace_count += 1
                started = True
            elif ch == '}':
                brace_count -= 1
                if started and brace_count == 0:
                    return "".join(block_lines)
    return "".join(block_lines[:40])

def parse_file(domain_title, filepath, base_url_prefix, auth_default):
    with open(filepath, "r") as f:
        lines = f.readlines()

    endpoints = []
    stack = []
    current_brace_level = 0

    for idx, line in enumerate(lines):
        line_no = idx + 1
        
        # Route block
        m_route = re.search(r'\broute\s*\(\s*"([^"]*)"\s*\)', line)
        if m_route:
            stack.append((current_brace_level, m_route.group(1)))

        # Verb
        m_verb = re.search(r'^\s*(get|post|put|patch|delete)\s*(?:\(\s*"([^"]*)"\s*\)|\s*\{)', line)
        if m_verb:
            verb = m_verb.group(1).upper()
            subpath = m_verb.group(2) if m_verb.group(2) is not None else ""

            # compute full path
            prefix_parts = [item[1] for item in stack]
            full_route = base_url_prefix
            for p in prefix_parts:
                if p:
                    if not p.startswith("/"):
                        p = "/" + p
                    p = p.rstrip("/")
                    full_route += p
            if subpath:
                if not subpath.startswith("/"):
                    subpath = "/" + subpath
                full_route += subpath
            if not full_route:
                full_route = "/"

            # Get exact handler block
            snippet = extract_handler_block(lines, idx)

            # Receives
            recvs = re.findall(r'call\.receive(?:Text|Parameters)?(?:<([^>]+)>)?', snippet)
            recv_class = recvs[0] if recvs else "None"
            if recv_class == "":
                recv_class = "String (text/raw body)"

            # Responds
            resps = re.findall(r'call\.respond(?:Text)?\s*\(([^,\n\)]+)?(?:,\s*([^,\n\)]+))?', snippet)
            resp_class = ""
            if resps:
                status, payload = resps[0]
                status = status.strip() if status else ""
                payload = payload.strip() if payload else ""
                if payload:
                    resp_class = f"{status}: `{payload}`"
                else:
                    resp_class = f"`{status}`"
            else:
                resp_class = "`HttpStatusCode.OK`"

            # Headers
            headers_set = set()
            if auth_default:
                headers_set.add("Authorization: Bearer <jwt>")
                headers_set.add("X-Tenant-Id: <tenant-uuid>")
            
            # Detect explicit header reads in handler
            raw_hdrs = re.findall(r'call\.request\.(?:headers\["([^"]+)"\]|header\("([^"]+)"\))', snippet)
            for h in raw_hdrs:
                hdr_name = h[0] or h[1]
                if "tenant" in hdr_name.lower():
                    headers_set.add(f"{hdr_name}: <tenant-uuid>")
                elif "user" in hdr_name.lower():
                    headers_set.add(f"{hdr_name}: <user-id>")
                elif "auth" in hdr_name.lower():
                    headers_set.add(f"{hdr_name}: Bearer <jwt>")
                elif "device" in hdr_name.lower():
                    headers_set.add(f"{hdr_name}: <device-fingerprint>")
                elif "signature" in hdr_name.lower():
                    headers_set.add(f"{hdr_name}: <signature-hash>")
                elif "operator" in hdr_name.lower():
                    headers_set.add(f"{hdr_name}: <operator-id>")
                elif "forwarded" in hdr_name.lower():
                    headers_set.add(f"{hdr_name}: <ip-address>")
                elif "secret" in hdr_name.lower():
                    headers_set.add(f"{hdr_name}: <webhook-secret>")
                else:
                    headers_set.add(f"{hdr_name}: <required-value>")

            if "receive<" in snippet:
                headers_set.add("Content-Type: application/json")

            # Detect Engines
            engines_found = []
            for eng_name, eng_desc in KNOWN_ENGINES:
                if eng_name in snippet:
                    engines_found.append(eng_desc)

            # Detect Supabase Tables and Ops
            tables_found = set()
            for t in TABLE_MAPPINGS:
                if f'"{t}"' in snippet or f" {t} " in snippet or f" {t}(" in snippet or f" {t}\n" in snippet:
                    op = []
                    if "insertRecord" in snippet or "INSERT INTO" in snippet or "ON CONFLICT" in snippet:
                        op.append("INSERT")
                    if "updateRecord" in snippet or "UPDATE" in snippet:
                        op.append("UPDATE")
                    if "deleteRecord" in snippet or "DELETE FROM" in snippet:
                        op.append("DELETE")
                    if "queryTable" in snippet or "SELECT" in snippet:
                        op.append("SELECT")
                    if not op:
                        op.append("SELECT")
                    tables_found.add(f"`{t}` ({'/'.join(sorted(set(op)))})")

            endpoints.append({
                "line": line_no,
                "verb": verb,
                "full_route": full_route,
                "recv_class": recv_class,
                "resp_class": resp_class,
                "headers": sorted(list(headers_set)),
                "engines": engines_found,
                "tables": sorted(list(tables_found))
            })

        open_b = line.count("{")
        close_b = line.count("}")
        current_brace_level += (open_b - close_b)
        while stack and current_brace_level <= stack[-1][0]:
            stack.pop()

    return endpoints

DOMAINS = [
    {
        "title": "1. Core Domain - System & Webhook Gateways",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt",
        "base_url": "",
        "auth": False
    },
    {
        "title": "2. Core Domain - Authentication & Profile Lifecycle",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt",
        "base_url": "",
        "auth": False
    },
    {
        "title": "3. Core Domain - Master Data & Public Catalog",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt",
        "base_url": "/api/v1/public",
        "auth": False
    },
    {
        "title": "4. Core Domain - Biometric Presence & Liveness",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt",
        "base_url": "/api/v1/presence",
        "auth": False
    },
    {
        "title": "5. Core Domain - Attendance & Geofencing",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt",
        "base_url": "/api/v1/attendance",
        "auth": False
    },
    {
        "title": "6. Core Domain - Prospect Registration & Onboarding",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt",
        "base_url": "/api/v1/prospect",
        "auth": False
    },
    {
        "title": "7. Core Domain - Tenants, Hybrid Workforce, Taskboard & Intel",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt",
        "base_url": "/api/v1",
        "auth": True
    },
    {
        "title": "8. Orchestration Domain - Workflow DAG, Checkpoints & MCP Execution",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt",
        "base_url": "/api/v1/orchestration",
        "auth": True
    },
    {
        "title": "9. Chat Domain - Agent Direct Conversation & Company Brain Knowledge",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt",
        "base_url": "/api/v1",
        "auth": True
    },
    {
        "title": "10. Selection Domain - Universal Selection, Understanding & Calibration",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt",
        "base_url": "/api/v1/selection",
        "auth": True
    },
    {
        "title": "11. Generative Studio Domain - Creative Assets & Multimodal Synthesis",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt",
        "base_url": "/api/v1/studio",
        "auth": True
    },
    {
        "title": "12. Enterprise Domain - Governance, Context Fabric & Chief of Staff",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt",
        "base_url": "/api/v1/tenants/{id}",
        "auth": True
    },
    {
        "title": "13. Memory Domain - Autonomous Consolidation, Decay & Hybrid Search",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt",
        "base_url": "/api/v1/memory",
        "auth": True
    },
    {
        "title": "14. Omnichannel & Sales Domain - Channels, CRM, Catalog & Commerce",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt",
        "base_url": "/api/v1",
        "auth": True
    },
    {
        "title": "15. Billing Domain - Commercial Plans, Credits, Quota & Dunning",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt",
        "base_url": "/api/v1",
        "auth": True
    },
    {
        "title": "16. Admin Domain - Operations, Security, MCP & Financial Command",
        "file": "orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt",
        "base_url": "/admin",
        "auth": False
    }
]

doc_lines = []
doc_lines.append("# DOKUMEN METHOD & ENDPOINT FINAL (GREP-CONFIRMED)")
doc_lines.append("> **Definitive Reference Document**: Dihasilkan secara langsung dari verifikasi grep kode sumber aktual Ktor backend server (`ai.orchestree.backend`). Dokumen ini menggantikan `methodendpoint.txt` dan seluruh dokumen audit parsial sebelumnya.")
doc_lines.append("")
doc_lines.append("## Ringkasan Eksekutif & Statistik Endpoint")

total_endpoints = 0
domain_stats = []
all_domain_data = []

for dom in DOMAINS:
    eps = parse_file(dom["title"], dom["file"], dom["base_url"], dom["auth"])
    total_endpoints += len(eps)
    domain_stats.append((dom["title"], len(eps), dom["file"]))
    all_domain_data.append((dom, eps))

doc_lines.append(f"- **Total Endpoint Terverifikasi (Grep-Confirmed)**: **{total_endpoints} Endpoint**")
doc_lines.append(f"- **Total Domain Operasional**: **{len(DOMAINS)} Domain**")
doc_lines.append("")
doc_lines.append("| Domain | File Sumber | Jumlah Endpoint |")
doc_lines.append("|---|---|:---:|")
for t, c, f in domain_stats:
    doc_lines.append(f"| {t} | `{f}` | **{c}** |")
doc_lines.append(f"| **TOTAL** | | **{total_endpoints}** |")
doc_lines.append("")
doc_lines.append("---")
doc_lines.append("")

# Generate details per domain
for dom, eps in all_domain_data:
    doc_lines.append(f"## {dom['title']}")
    doc_lines.append(f"- **File Sumber**: `{dom['file']}`")
    doc_lines.append(f"- **Jumlah Endpoint**: **{len(eps)}**")
    doc_lines.append("")
    
    for idx, ep in enumerate(eps):
        doc_lines.append(f"### {idx+1}. `{ep['verb']}` {ep['full_route']}")
        doc_lines.append(f"- **Grep Confirmation**: Line {ep['line']} in `{dom['file']}`")
        
        # Headers
        hdrs_str = ", ".join([f"`{h}`" for h in ep['headers']]) if ep['headers'] else "None (No special headers required)"
        doc_lines.append(f"- **Header Wajib**: {hdrs_str}")
        
        # Request body
        doc_lines.append(f"- **Request Body Schema**: {get_model_desc(ep['recv_class'])}")
        
        # Response body
        doc_lines.append(f"- **Response Body Schema**: {ep['resp_class']}")
        
        # Engine
        eng_status = "Ya" if ep['engines'] else "Tidak (Direct Service/Repo Call)"
        if ep['engines']:
            eng_status += f" — Terkoneksi ke {', '.join(ep['engines'])}"
        doc_lines.append(f"- **Status Engine Terhubung**: {eng_status}")
        
        # Supabase tables
        tbl_status = ", ".join(ep['tables']) if ep['tables'] else "Tidak langsung / Stateless"
        doc_lines.append(f"- **Tabel Supabase Terpengaruh**: {tbl_status}")
        doc_lines.append("")

output_path = "orchestreeai-backend-server/docs/FINAL_API_ENDPOINTS_DOCUMENTATION.md"
os.makedirs(os.path.dirname(output_path), exist_ok=True)
with open(output_path, "w") as f:
    f.write("\n".join(doc_lines))

with open("orchestreeai-backend-server/methodendpoint.txt", "w") as f:
    f.write("\n".join(doc_lines))

print(f"SUCCESS: Generated documentation with {total_endpoints} endpoints into {output_path} and orchestreeai-backend-server/methodendpoint.txt")
