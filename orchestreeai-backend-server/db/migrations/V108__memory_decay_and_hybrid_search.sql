-- ====================================================================
-- OrchestreeAI Database Migration V108
-- Memory Consolidator Threshold Gate, Automated Decay & Hybrid Search Re-ranking
-- PRD Master Bagian 17.2, PRD Fase 66 Langkah 4.2
-- ====================================================================

-- 1. Extend memory_documents with decay, archiving, and scoring columns
ALTER TABLE memory_documents
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(64) DEFAULT 'episodic',
    ADD COLUMN IF NOT EXISTS relevance_weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    ADD COLUMN IF NOT EXISTS is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS importance_score DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    ADD COLUMN IF NOT EXISTS novelty_score DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    ADD COLUMN IF NOT EXISTS specificity_score DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ;

-- 2. Indexes for efficient decay scanning and active candidate retrieval
CREATE INDEX IF NOT EXISTS idx_memory_docs_decay ON memory_documents(tenant_id, is_archived, source_type, relevance_weight);
CREATE INDEX IF NOT EXISTS idx_memory_docs_created ON memory_documents(tenant_id, created_at DESC);
