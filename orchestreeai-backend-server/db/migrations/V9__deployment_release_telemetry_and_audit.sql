-- ====================================================================
-- OrchestreeAI Database Migration V9: Deployment & Infrastructure Telemetry
-- ====================================================================

-- 1. Deployment Release Logs Table
CREATE TABLE IF NOT EXISTS deployment_releases (
    id VARCHAR(64) PRIMARY KEY,
    environment VARCHAR(32) NOT NULL, -- dev, staging, prod
    release_version VARCHAR(64) NOT NULL,
    commit_sha VARCHAR(64) NOT NULL,
    strategy VARCHAR(32) NOT NULL DEFAULT 'CANARY', -- CANARY, BLUE_GREEN, ROLLING
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS', -- IN_PROGRESS, SUCCESS, FAILED, ROLLED_BACK
    deployed_by VARCHAR(128) NOT NULL,
    deployed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    traffic_weight_pct INT NOT NULL DEFAULT 100,
    health_check_status VARCHAR(32) NOT NULL DEFAULT 'HEALTHY',
    rollback_target_version VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_deployment_env_status ON deployment_releases(environment, status);

-- 2. Disaster Recovery Test Logs Table
CREATE TABLE IF NOT EXISTS disaster_recovery_drill_logs (
    id VARCHAR(64) PRIMARY KEY,
    drill_type VARCHAR(64) NOT NULL, -- DATABASE_PITR_RESTORE, REGION_FAILOVER_DRILL
    source_region VARCHAR(32) NOT NULL DEFAULT 'asia-southeast2',
    target_region VARCHAR(32) NOT NULL DEFAULT 'asia-southeast1',
    measured_rto_seconds INT NOT NULL,
    measured_rpo_seconds INT NOT NULL,
    target_rto_seconds INT NOT NULL DEFAULT 900,
    target_rpo_seconds INT NOT NULL DEFAULT 300,
    status VARCHAR(32) NOT NULL DEFAULT 'PASSED',
    executed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_dr_drill_type ON disaster_recovery_drill_logs(drill_type, status);
