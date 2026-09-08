-- FASE 106: Workflow Executions Persistent Checkpoint & Crash Recovery
-- PRD Master Bagian 20.1: Checkpoint Persisten & Recovery Job

ALTER TABLE workflow_executions ADD COLUMN IF NOT EXISTS current_state_snapshot JSONB;
ALTER TABLE workflow_executions ADD COLUMN IF NOT EXISTS last_completed_node_id TEXT;
ALTER TABLE workflow_executions ADD COLUMN IF NOT EXISTS last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'workflow_executions' AND column_name = 'execution_status'
    ) THEN
        ALTER TABLE workflow_executions ADD COLUMN execution_status TEXT DEFAULT 'running';
    END IF;
END $$;

-- Enforce CHECK constraint on execution_status
DO $$
BEGIN
    ALTER TABLE workflow_executions DROP CONSTRAINT IF EXISTS chk_wf_exec_execution_status;
    ALTER TABLE workflow_executions ADD CONSTRAINT chk_wf_exec_execution_status
        CHECK (execution_status IN ('running', 'paused_for_approval', 'completed', 'failed', 'crashed_recoverable'));
EXCEPTION
    WHEN OTHERS THEN NULL;
END $$;

CREATE INDEX IF NOT EXISTS idx_wf_exec_checkpoint_recovery 
    ON workflow_executions(execution_status, last_updated_at);
