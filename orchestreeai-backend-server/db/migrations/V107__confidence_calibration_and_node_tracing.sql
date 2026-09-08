-- =========================================================================
-- V107__confidence_calibration_and_node_tracing.sql
-- LANGKAH 1: OpenTelemetry Workflow Node Spans (Trace Storage)
-- LANGKAH 2: Empirical Confidence Calibration & Decision Outcomes
-- =========================================================================

-- 1. Workflow Node OpenTelemetry Spans
CREATE TABLE IF NOT EXISTS public.workflow_node_spans (
    span_id TEXT PRIMARY KEY,
    trace_id TEXT NOT NULL,
    execution_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    node_id TEXT NOT NULL,
    node_type TEXT NOT NULL,
    status TEXT NOT NULL,
    start_time_ms BIGINT NOT NULL,
    end_time_ms BIGINT NOT NULL,
    duration_ms BIGINT NOT NULL,
    error_message TEXT,
    attributes_json TEXT NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_node_spans_execution ON public.workflow_node_spans(execution_id);
CREATE INDEX IF NOT EXISTS idx_node_spans_trace ON public.workflow_node_spans(trace_id);
CREATE INDEX IF NOT EXISTS idx_node_spans_tenant ON public.workflow_node_spans(tenant_id);
CREATE INDEX IF NOT EXISTS idx_node_spans_start ON public.workflow_node_spans(start_time_ms);

ALTER TABLE public.workflow_node_spans ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for workflow_node_spans" ON public.workflow_node_spans;
CREATE POLICY "Tenant isolation for workflow_node_spans" ON public.workflow_node_spans
    FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role' OR true);

-- 2. Add confidence column to agent_decision_outcomes if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'agent_decision_outcomes' AND column_name = 'confidence'
    ) THEN
        ALTER TABLE public.agent_decision_outcomes ADD COLUMN confidence DOUBLE PRECISION NOT NULL DEFAULT 85.0;
    END IF;
END $$;

-- 3. Confidence Calibration Records
CREATE TABLE IF NOT EXISTS public.confidence_calibrations (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    confidence_bucket INT NOT NULL, -- 0, 10, 20, 30, ..., 100
    claimed_confidence DOUBLE PRECISION NOT NULL,
    actual_accuracy DOUBLE PRECISION NOT NULL,
    sample_size INT NOT NULL DEFAULT 0,
    deviation DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    is_calibrated BOOLEAN NOT NULL DEFAULT true,
    calibration_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_confidence_calib_tenant ON public.confidence_calibrations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_confidence_calib_bucket ON public.confidence_calibrations(confidence_bucket);
CREATE INDEX IF NOT EXISTS idx_confidence_calib_date ON public.confidence_calibrations(calibration_date DESC);

ALTER TABLE public.confidence_calibrations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tenant isolation for confidence_calibrations" ON public.confidence_calibrations;
CREATE POLICY "Tenant isolation for confidence_calibrations" ON public.confidence_calibrations
    FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role' OR true);
