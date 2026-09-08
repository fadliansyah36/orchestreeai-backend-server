-- ==============================================================================
-- OrchestreeAI Migration: Tahap 7 — Multi-Agent Swarm, Tool Execution & Safety Engine
-- File: V27__tahap7_multi_agent_swarm_and_tool_execution.sql
-- PRD Source: Master Bagian 17 (Autonomous Swarms), Bagian 20 (MCP Tool Architecture), Bagian 21 (Dynamic Model Routing)
-- Deskripsi: Skema orkestrasi multi-agent swarm, voting konsensus, eksekusi tool MCP aman, metrik server MCP, dan model routing dinamis.
-- ==============================================================================

-- 1. Orchestration Swarms (Multi-Agent Collaborative Problem Solving Swarms)
CREATE TABLE IF NOT EXISTS orchestration_swarms (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    swarm_name TEXT NOT NULL,
    objective TEXT NOT NULL,
    leader_agent_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'INITIALIZING', -- 'INITIALIZING', 'ACTIVE', 'CONSENSUS_REACHED', 'COMPLETED', 'FAILED', 'CANCELLED'
    consensus_mode TEXT NOT NULL DEFAULT 'MAJORITY_VOTE', -- 'LEADER_DECIDES', 'MAJORITY_VOTE', 'UNANIMOUS', 'WEIGHTED_CONFIDENCE'
    max_iterations INT NOT NULL DEFAULT 5,
    current_iteration INT NOT NULL DEFAULT 0,
    task_id TEXT,
    context_metadata_json TEXT NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_swarms_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_swarms_leader FOREIGN KEY (leader_agent_id)
        REFERENCES ai_agents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_swarms_tenant_id ON orchestration_swarms(tenant_id);
CREATE INDEX IF NOT EXISTS idx_swarms_leader ON orchestration_swarms(leader_agent_id);
CREATE INDEX IF NOT EXISTS idx_swarms_status ON orchestration_swarms(status);
CREATE INDEX IF NOT EXISTS idx_swarms_task_id ON orchestration_swarms(task_id);

-- 2. Swarm Agent Assignments (Penugasan Anggota Agen dalam Swarm)
CREATE TABLE IF NOT EXISTS swarm_agent_assignments (
    id TEXT PRIMARY KEY,
    swarm_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    agent_id TEXT NOT NULL,
    role_in_swarm TEXT NOT NULL, -- 'LEADER', 'RESEARCHER', 'CRITIC', 'EXECUTOR', 'VALIDATOR'
    assigned_subtask TEXT NOT NULL,
    contribution_summary TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'ASSIGNED', -- 'ASSIGNED', 'IN_PROGRESS', 'CONTRIBUTED', 'BLOCKED'
    assigned_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    completed_at BIGINT,
    CONSTRAINT fk_swarm_assignments_swarm FOREIGN KEY (swarm_id)
        REFERENCES orchestration_swarms(id) ON DELETE CASCADE,
    CONSTRAINT fk_swarm_assignments_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_swarm_assignments_agent FOREIGN KEY (agent_id)
        REFERENCES ai_agents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_swarm_assign_swarm_id ON swarm_agent_assignments(swarm_id);
CREATE INDEX IF NOT EXISTS idx_swarm_assign_tenant_id ON swarm_agent_assignments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_swarm_assign_agent_id ON swarm_agent_assignments(agent_id);

-- 3. Swarm Consensus Votes (Audit Trail & Bukti Voting Musyawarah AI Swarm)
CREATE TABLE IF NOT EXISTS swarm_consensus_votes (
    id TEXT PRIMARY KEY,
    swarm_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    proposal_id TEXT NOT NULL,
    agent_id TEXT NOT NULL,
    vote_decision TEXT NOT NULL, -- 'APPROVE', 'REJECT', 'ABSTAIN', 'REQUEST_REVISION'
    confidence_score DOUBLE PRECISION NOT NULL DEFAULT 0.85,
    reasoning TEXT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_swarm_votes_swarm FOREIGN KEY (swarm_id)
        REFERENCES orchestration_swarms(id) ON DELETE CASCADE,
    CONSTRAINT fk_swarm_votes_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_swarm_votes_agent FOREIGN KEY (agent_id)
        REFERENCES ai_agents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_swarm_votes_swarm_id ON swarm_consensus_votes(swarm_id);
CREATE INDEX IF NOT EXISTS idx_swarm_votes_tenant_id ON swarm_consensus_votes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_swarm_votes_proposal ON swarm_consensus_votes(proposal_id);

-- 4. MCP Tool Execution Logs (Audit Trail Eksekusi Tool Protokol MCP)
CREATE TABLE IF NOT EXISTS mcp_tool_execution_logs (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    tool_id TEXT NOT NULL,
    tool_name TEXT NOT NULL,
    caller_agent_id TEXT,
    task_id TEXT,
    workflow_execution_id TEXT,
    input_params_json TEXT NOT NULL DEFAULT '{}',
    output_result_json TEXT NOT NULL DEFAULT '{}',
    execution_time_ms BIGINT NOT NULL DEFAULT 0,
    is_success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    risk_level TEXT NOT NULL DEFAULT 'LOW', -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    human_approved_by TEXT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_mcp_exec_logs_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mcp_logs_tenant_id ON mcp_tool_execution_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mcp_logs_tool_name ON mcp_tool_execution_logs(tool_name);
CREATE INDEX IF NOT EXISTS idx_mcp_logs_caller ON mcp_tool_execution_logs(caller_agent_id);
CREATE INDEX IF NOT EXISTS idx_mcp_logs_task_id ON mcp_tool_execution_logs(task_id);
CREATE INDEX IF NOT EXISTS idx_mcp_logs_risk_level ON mcp_tool_execution_logs(risk_level);

-- 5. MCP Server Health Metrics (Telemetri Ketersediaan Server MCP Eksternal & Internal)
CREATE TABLE IF NOT EXISTS mcp_server_health_metrics (
    id TEXT PRIMARY KEY,
    server_name TEXT NOT NULL,
    server_url TEXT NOT NULL,
    is_alive BOOLEAN NOT NULL DEFAULT TRUE,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    error_rate_pct DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_requests BIGINT NOT NULL DEFAULT 0,
    active_tools_count INT NOT NULL DEFAULT 0,
    last_heartbeat_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_mcp_server_name ON mcp_server_health_metrics(server_name);
CREATE INDEX IF NOT EXISTS idx_mcp_server_alive ON mcp_server_health_metrics(is_alive);

-- 6. Model Routing Rules (Konfigurasi Dinamis LLM Router Multi-Provider)
CREATE TABLE IF NOT EXISTS model_routing_rules (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL DEFAULT 'GLOBAL',
    task_category TEXT NOT NULL, -- 'CLASSIFICATION', 'REASONING', 'IMAGE_GEN', 'PROACTIVE_BRIEF', 'GENERAL_CHAT'
    sensitivity_tier TEXT NOT NULL DEFAULT 'INTERNAL', -- 'PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED'
    task_complexity_tier TEXT NOT NULL DEFAULT 'MODERATE', -- 'TRIVIAL', 'SIMPLE', 'MODERATE', 'COMPLEX'
    min_confidence DOUBLE PRECISION NOT NULL DEFAULT 0.75,
    preferred_provider_id TEXT NOT NULL DEFAULT 'gemini',
    preferred_model_id TEXT NOT NULL DEFAULT 'gemini-3.5-flash',
    fallback_model_id TEXT NOT NULL DEFAULT 'gemini-2.5-flash',
    routing_strategy TEXT NOT NULL DEFAULT 'QUALITY_FIRST', -- 'COST_FIRST', 'SPEED_FIRST', 'QUALITY_FIRST', 'BALANCED'
    max_latency_ms BIGINT NOT NULL DEFAULT 5000,
    max_budget_per_call_usd DOUBLE PRECISION NOT NULL DEFAULT 0.05,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_model_routing_tenant ON model_routing_rules(tenant_id);
CREATE INDEX IF NOT EXISTS idx_model_routing_category ON model_routing_rules(task_category);
CREATE INDEX IF NOT EXISTS idx_model_routing_strategy ON model_routing_rules(routing_strategy);

-- 7. Model Routing Performance Logs (Audit Latensi, Token & Biaya LLM)
CREATE TABLE IF NOT EXISTS model_routing_performance_logs (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    rule_id TEXT,
    task_category TEXT NOT NULL,
    provider_id TEXT NOT NULL,
    model_id TEXT NOT NULL,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    estimated_cost_usd DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    is_success BOOLEAN NOT NULL DEFAULT TRUE,
    fallback_triggered BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_routing_perf_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_routing_perf_tenant_id ON model_routing_performance_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_routing_perf_category ON model_routing_performance_logs(task_category);
CREATE INDEX IF NOT EXISTS idx_routing_perf_model ON model_routing_performance_logs(model_id);
CREATE INDEX IF NOT EXISTS idx_routing_perf_created ON model_routing_performance_logs(created_at);

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================
ALTER TABLE orchestration_swarms ENABLE ROW LEVEL SECURITY;
ALTER TABLE swarm_agent_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE swarm_consensus_votes ENABLE ROW LEVEL SECURITY;
ALTER TABLE mcp_tool_execution_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE mcp_server_health_metrics ENABLE ROW LEVEL SECURITY;
ALTER TABLE model_routing_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE model_routing_performance_logs ENABLE ROW LEVEL SECURITY;

-- 1. Policies for orchestration_swarms
DROP POLICY IF EXISTS tenant_isolation_swarms ON orchestration_swarms;
CREATE POLICY tenant_isolation_swarms ON orchestration_swarms FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 2. Policies for swarm_agent_assignments
DROP POLICY IF EXISTS tenant_isolation_swarm_assignments ON swarm_agent_assignments;
CREATE POLICY tenant_isolation_swarm_assignments ON swarm_agent_assignments FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 3. Policies for swarm_consensus_votes
DROP POLICY IF EXISTS tenant_isolation_swarm_votes ON swarm_consensus_votes;
CREATE POLICY tenant_isolation_swarm_votes ON swarm_consensus_votes FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 4. Policies for mcp_tool_execution_logs
DROP POLICY IF EXISTS tenant_isolation_mcp_logs ON mcp_tool_execution_logs;
CREATE POLICY tenant_isolation_mcp_logs ON mcp_tool_execution_logs FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 5. Policies for mcp_server_health_metrics (Public read, service_role write)
DROP POLICY IF EXISTS p_mcp_health_select ON mcp_server_health_metrics;
CREATE POLICY p_mcp_health_select ON mcp_server_health_metrics FOR SELECT USING (true);
DROP POLICY IF EXISTS p_mcp_health_service ON mcp_server_health_metrics;
CREATE POLICY p_mcp_health_service ON mcp_server_health_metrics FOR ALL USING (auth.role() = 'service_role');

-- 6. Policies for model_routing_rules (Tenant or GLOBAL read, tenant or service_role write)
DROP POLICY IF EXISTS p_model_routing_rules_access ON model_routing_rules;
CREATE POLICY p_model_routing_rules_access ON model_routing_rules FOR ALL USING (
    tenant_id = 'GLOBAL' 
    OR app_has_tenant_access(tenant_id) 
    OR auth.role() = 'service_role'
);

-- 7. Policies for model_routing_performance_logs
DROP POLICY IF EXISTS tenant_isolation_routing_perf ON model_routing_performance_logs;
CREATE POLICY tenant_isolation_routing_perf ON model_routing_performance_logs FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- ==============================================================================
-- DEFAULT SEED DATA (GLOBAL MODEL ROUTING RULES & MCP HEALTH BENCHMARK)
-- ==============================================================================
INSERT INTO model_routing_rules (
    id, tenant_id, task_category, sensitivity_tier, task_complexity_tier, 
    min_confidence, preferred_provider_id, preferred_model_id, fallback_model_id, 
    routing_strategy, max_latency_ms, max_budget_per_call_usd, is_active
) VALUES 
    ('rule-global-complex', 'GLOBAL', 'REASONING', 'INTERNAL', 'COMPLEX', 0.85, 'gemini', 'gemini-3.5-pro', 'gemini-3.5-flash', 'QUALITY_FIRST', 8000, 0.10, true),
    ('rule-global-class', 'GLOBAL', 'CLASSIFICATION', 'PUBLIC', 'SIMPLE', 0.70, 'gemini', 'gemini-3.5-flash', 'gemini-2.5-flash', 'SPEED_FIRST', 2000, 0.01, true),
    ('rule-global-creative', 'GLOBAL', 'IMAGE_GEN', 'INTERNAL', 'MODERATE', 0.80, 'gemini', 'imagen-3.0-generate-002', 'gemini-3.5-flash', 'QUALITY_FIRST', 10000, 0.15, true),
    ('rule-global-proactive', 'GLOBAL', 'PROACTIVE_BRIEF', 'INTERNAL', 'MODERATE', 0.80, 'gemini', 'gemini-3.5-flash', 'gemini-2.5-flash', 'BALANCED', 4000, 0.03, true),
    ('rule-global-chat', 'GLOBAL', 'GENERAL_CHAT', 'PUBLIC', 'SIMPLE', 0.75, 'gemini', 'gemini-3.5-flash', 'gemini-2.5-flash', 'SPEED_FIRST', 3000, 0.02, true)
ON CONFLICT (id) DO UPDATE SET
    preferred_model_id = EXCLUDED.preferred_model_id,
    fallback_model_id = EXCLUDED.fallback_model_id,
    routing_strategy = EXCLUDED.routing_strategy;

INSERT INTO mcp_server_health_metrics (
    id, server_name, server_url, is_alive, latency_ms, error_rate_pct, total_requests, active_tools_count
) VALUES
    ('mcp-srv-core', 'Core Orchestree MCP Server', 'http://127.0.0.1:8080/mcp/v1', true, 45, 0.0, 1540, 8),
    ('mcp-srv-brain', 'Company Brain Vector MCP Server', 'http://127.0.0.1:8081/mcp/v1', true, 85, 0.1, 890, 4)
ON CONFLICT (id) DO UPDATE SET
    is_alive = EXCLUDED.is_alive,
    latency_ms = EXCLUDED.latency_ms,
    active_tools_count = EXCLUDED.active_tools_count;
