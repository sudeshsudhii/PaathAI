-- V3: Live pipeline improvements
-- Adds transcript stability states, live embedding support, and pipeline metrics.

-- =====================================================
-- TRANSCRIPT STABILITY (Issue #4)
-- =====================================================

ALTER TABLE live_transcript_chunks ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PROVISIONAL';
CREATE INDEX idx_live_transcript_status ON live_transcript_chunks(session_id, status);

-- =====================================================
-- LIVE SEARCH INDEXING (Issue #3)
-- =====================================================

ALTER TABLE live_transcript_chunks ADD COLUMN embedding vector(768);
CREATE INDEX idx_live_transcript_embedding ON live_transcript_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);

-- =====================================================
-- PIPELINE METRICS (Issue #8)
-- =====================================================

CREATE TABLE live_pipeline_metrics (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT       NOT NULL REFERENCES live_sessions(id) ON DELETE CASCADE,
    pipeline_stage  VARCHAR(50)  NOT NULL,
    chunk_seq       INT,
    latency_ms      BIGINT       NOT NULL,
    success         BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_live_metrics_session ON live_pipeline_metrics(session_id, pipeline_stage);

-- =====================================================
-- ADDITIONAL OPTIMIZATION INDEXES (Issue #9)
-- =====================================================

CREATE INDEX idx_live_topic_syllabus ON live_topic_timeline(session_id, syllabus_topic_id);
CREATE INDEX idx_live_audio_unprocessed ON live_audio_chunks(session_id, processed);
CREATE INDEX idx_live_notes_latest ON live_note_revisions(session_id, revision_number DESC);
CREATE INDEX idx_live_sessions_dates ON live_sessions(user_id, started_at DESC);
