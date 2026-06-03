-- V2: Live Session schema for real-time lecture intelligence
-- No existing tables are altered.

-- =====================================================
-- LIVE SESSIONS
-- =====================================================

CREATE TABLE live_sessions (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users(id),
    course_id         BIGINT       REFERENCES courses(id),
    lecture_id        BIGINT       REFERENCES lectures(id),
    title             VARCHAR(255),
    status            VARCHAR(20)  NOT NULL DEFAULT 'RECORDING',
    started_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    paused_at         TIMESTAMP,
    ended_at          TIMESTAMP,
    total_duration_ms BIGINT       DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE live_audio_chunks (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT       NOT NULL REFERENCES live_sessions(id) ON DELETE CASCADE,
    sequence_number INT          NOT NULL,
    audio_path      VARCHAR(500) NOT NULL,
    duration_ms     INT,
    size_bytes      INT,
    processed       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE live_transcript_chunks (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT       NOT NULL REFERENCES live_sessions(id) ON DELETE CASCADE,
    sequence_number INT          NOT NULL,
    content         TEXT         NOT NULL,
    start_offset_ms INT,
    end_offset_ms   INT,
    confidence      DECIMAL(5,4),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE live_topic_timeline (
    id                BIGSERIAL PRIMARY KEY,
    session_id        BIGINT       NOT NULL REFERENCES live_sessions(id) ON DELETE CASCADE,
    topic             VARCHAR(255) NOT NULL,
    subtopic          VARCHAR(255),
    confidence        DECIMAL(5,4),
    detected_at_ms    INT          NOT NULL,
    syllabus_topic_id BIGINT       REFERENCES syllabus_topics(id),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE live_note_revisions (
    id               BIGSERIAL PRIMARY KEY,
    session_id       BIGINT       NOT NULL REFERENCES live_sessions(id) ON DELETE CASCADE,
    revision_number  INT          NOT NULL,
    content          TEXT         NOT NULL,
    delta_content    TEXT,
    trigger_topic    VARCHAR(255),
    estimated_tokens INT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_live_audio_session ON live_audio_chunks(session_id, sequence_number);
CREATE INDEX idx_live_transcript_session ON live_transcript_chunks(session_id, sequence_number);
CREATE INDEX idx_live_topic_session ON live_topic_timeline(session_id, detected_at_ms);
CREATE INDEX idx_live_notes_session ON live_note_revisions(session_id, revision_number);
CREATE INDEX idx_live_sessions_user ON live_sessions(user_id);
CREATE INDEX idx_live_sessions_status ON live_sessions(status);
