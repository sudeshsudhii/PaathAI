-- V1: Enable pgvector extension and create core authentication tables
CREATE EXTENSION IF NOT EXISTS vector;

-- =====================================================
-- AUTHENTICATION & CORE
-- =====================================================

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',  -- STUDENT, TEACHER, ADMIN
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE courses (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    code            VARCHAR(50),
    semester        VARCHAR(50),
    instructor_id   BIGINT       REFERENCES users(id),
    description     TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE enrollments (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT       NOT NULL REFERENCES users(id),
    course_id       BIGINT       NOT NULL REFERENCES courses(id),
    enrolled_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(student_id, course_id)
);

-- =====================================================
-- SYLLABUS
-- =====================================================

CREATE TABLE syllabi (
    id              BIGSERIAL PRIMARY KEY,
    course_id       BIGINT       NOT NULL REFERENCES courses(id) UNIQUE,
    title           VARCHAR(255),
    source_type     VARCHAR(20)  NOT NULL,  -- PDF, JSON, MANUAL
    raw_file_path   VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE subjects (
    id              BIGSERIAL PRIMARY KEY,
    syllabus_id     BIGINT       NOT NULL REFERENCES syllabi(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE units (
    id              BIGSERIAL PRIMARY KEY,
    subject_id      BIGINT       NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE syllabus_topics (
    id              BIGSERIAL PRIMARY KEY,
    unit_id         BIGINT       NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    sort_order      INT          NOT NULL DEFAULT 0,
    embedding       vector(768),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =====================================================
-- LECTURES & TRANSCRIPTION
-- =====================================================

CREATE TABLE lectures (
    id              BIGSERIAL PRIMARY KEY,
    course_id       BIGINT       NOT NULL REFERENCES courses(id),
    uploaded_by     BIGINT       NOT NULL REFERENCES users(id),
    title           VARCHAR(255),
    audio_path      VARCHAR(500) NOT NULL,
    duration_ms     BIGINT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING, TRANSCRIBING, COMPLETED, FAILED
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE transcripts (
    id              BIGSERIAL PRIMARY KEY,
    lecture_id      BIGINT       NOT NULL REFERENCES lectures(id) UNIQUE,
    full_text       TEXT         NOT NULL,
    language        VARCHAR(10)  DEFAULT 'en',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE transcript_chunks (
    id              BIGSERIAL PRIMARY KEY,
    transcript_id   BIGINT       NOT NULL REFERENCES transcripts(id) ON DELETE CASCADE,
    chunk_index     INT          NOT NULL,
    content         TEXT         NOT NULL,
    start_offset_ms INT,
    end_offset_ms   INT,
    estimated_tokens INT,
    embedding       vector(768),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE chunk_summaries (
    id              BIGSERIAL PRIMARY KEY,
    chunk_id        BIGINT       NOT NULL REFERENCES transcript_chunks(id) ON DELETE CASCADE,
    summary         TEXT         NOT NULL,
    estimated_tokens INT,
    embedding       vector(768),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE lecture_summaries (
    id              BIGSERIAL PRIMARY KEY,
    lecture_id      BIGINT       NOT NULL REFERENCES lectures(id) UNIQUE,
    summary         TEXT         NOT NULL,
    estimated_tokens INT,
    embedding       vector(768),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =====================================================
-- STUDY MATERIALS (Notes only for MVP)
-- =====================================================

CREATE TABLE notes (
    id              BIGSERIAL PRIMARY KEY,
    lecture_id      BIGINT       NOT NULL REFERENCES lectures(id),
    content         TEXT         NOT NULL,
    format          VARCHAR(20)  DEFAULT 'MARKDOWN',
    estimated_tokens INT,
    embedding       vector(768),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =====================================================
-- AI & PROMPTS
-- =====================================================

CREATE TABLE prompt_versions (
    id              BIGSERIAL PRIMARY KEY,
    prompt_type     VARCHAR(50)  NOT NULL,
    version         INT          NOT NULL,
    content         TEXT         NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(prompt_type, version)
);

CREATE TABLE prompt_metrics (
    id              BIGSERIAL PRIMARY KEY,
    prompt_type     VARCHAR(50)  NOT NULL,
    prompt_version  INT          NOT NULL,
    total_calls     INT          NOT NULL DEFAULT 0,
    avg_input_tokens DECIMAL(10,2),
    avg_output_tokens DECIMAL(10,2),
    avg_latency_ms  DECIMAL(10,2),
    success_rate    DECIMAL(5,4),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(prompt_type, prompt_version)
);

-- =====================================================
-- MONITORING & OPERATIONS
-- =====================================================

CREATE TABLE llm_request_logs (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT,
    feature_type    VARCHAR(50)  NOT NULL,
    model_used      VARCHAR(100),
    input_tokens    INT,
    output_tokens   INT,
    total_tokens    INT,
    estimated_cost_usd DECIMAL(10,6),
    latency_ms      BIGINT,
    status          VARCHAR(20)  NOT NULL,
    error_message   TEXT,
    prompt_type     VARCHAR(50),
    prompt_version  INT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE token_usage (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT       NOT NULL REFERENCES users(id),
    date            DATE         NOT NULL,
    feature_type    VARCHAR(50)  NOT NULL,
    total_tokens    INT          NOT NULL DEFAULT 0,
    request_count   INT          NOT NULL DEFAULT 0,
    estimated_cost_usd DECIMAL(10,6) DEFAULT 0,
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(student_id, date, feature_type)
);

CREATE TABLE retrieval_logs (
    id              BIGSERIAL PRIMARY KEY,
    query_text      TEXT,
    course_id       BIGINT,
    student_id      BIGINT,
    feature_type    VARCHAR(50),
    sources_consulted INT,
    sources_used    TEXT,          -- comma-separated source names
    context_tokens  INT,
    total_results   INT,
    latency_ms      BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE processing_failures (
    id              BIGSERIAL PRIMARY KEY,
    lecture_id      BIGINT,
    service_name    VARCHAR(100) NOT NULL,
    error_message   TEXT,
    retry_count     INT          DEFAULT 0,
    resolved        BOOLEAN      DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMP
);

-- =====================================================
-- INDEXES
-- =====================================================

-- Syllabus topic search
CREATE INDEX idx_syllabus_topics_unit ON syllabus_topics(unit_id);
CREATE INDEX idx_syllabus_topics_embedding ON syllabus_topics USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Transcript chunk search
CREATE INDEX idx_transcript_chunks_transcript ON transcript_chunks(transcript_id);
CREATE INDEX idx_transcript_chunks_embedding ON transcript_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Notes search
CREATE INDEX idx_notes_lecture ON notes(lecture_id);
CREATE INDEX idx_notes_embedding ON notes USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Monitoring
CREATE INDEX idx_llm_logs_student_date ON llm_request_logs(student_id, created_at);
CREATE INDEX idx_llm_logs_feature ON llm_request_logs(feature_type);
CREATE INDEX idx_token_usage_student_date ON token_usage(student_id, date);
CREATE INDEX idx_processing_failures_unresolved ON processing_failures(resolved) WHERE resolved = FALSE;

-- Lectures
CREATE INDEX idx_lectures_course ON lectures(course_id);
CREATE INDEX idx_lectures_status ON lectures(status);
