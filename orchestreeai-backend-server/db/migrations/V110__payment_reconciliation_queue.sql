-- ====================================================================
-- FASE 110: Payment Reconciliation Queue & Automated Job Anomaly Detection
-- LANGKAH 1 — SKEMA DATA
-- ====================================================================

-- 1. Base Payments Table (jika belum ada)
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    gateway_reference_id TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 2. Payment Reconciliation Queue (PRD Master / User Request)
CREATE TABLE IF NOT EXISTS payment_reconciliation_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID,
    order_id TEXT,
    tenant_id TEXT,
    detected_issue TEXT NOT NULL,  -- 'webhook_not_received'/'signature_mismatch'/
                                     -- 'gateway_reports_paid_but_local_pending'/
                                     -- 'amount_mismatch'
    gateway_reported_status TEXT,  -- hasil cek LANGSUNG ke API gateway
    local_status TEXT,             -- status di database kita saat ini
    resolution_status TEXT DEFAULT 'pending_review',  -- pending_review/
                                                          -- resolved_confirmed/
                                                          -- resolved_rejected
    resolved_by_super_admin_id UUID,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payments_status_created_at ON payments(status, created_at);
CREATE INDEX IF NOT EXISTS idx_payments_gateway_ref ON payments(gateway_reference_id);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);

CREATE INDEX IF NOT EXISTS idx_prq_resolution_status ON payment_reconciliation_queue(resolution_status);
CREATE INDEX IF NOT EXISTS idx_prq_tenant_id ON payment_reconciliation_queue(tenant_id);
CREATE INDEX IF NOT EXISTS idx_prq_payment_id ON payment_reconciliation_queue(payment_id);
CREATE INDEX IF NOT EXISTS idx_prq_order_id ON payment_reconciliation_queue(order_id);
CREATE INDEX IF NOT EXISTS idx_prq_created_at ON payment_reconciliation_queue(created_at DESC);

-- RLS
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_reconciliation_queue ENABLE ROW LEVEL SECURITY;
