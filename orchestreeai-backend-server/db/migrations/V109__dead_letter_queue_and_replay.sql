-- FASE 109: Dead-Letter Queue untuk Scheduler Jobs & Replay Isolation
-- LANGKAH 1 — DEAD-LETTER QUEUE UNTUK JOB YANG GAGAL BERULANG
CREATE TABLE IF NOT EXISTS dead_letter_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type TEXT NOT NULL,
    original_payload JSONB NOT NULL,
    failure_reason TEXT,
    failed_at TIMESTAMPTZ DEFAULT now(),
    reprocessed BOOLEAN DEFAULT FALSE,
    reprocessed_at TIMESTAMPTZ,
    reprocess_result TEXT
);

CREATE INDEX IF NOT EXISTS idx_dlq_job_type_reprocessed ON dead_letter_queue(job_type, reprocessed);
CREATE INDEX IF NOT EXISTS idx_dlq_failed_at ON dead_letter_queue(failed_at DESC);
