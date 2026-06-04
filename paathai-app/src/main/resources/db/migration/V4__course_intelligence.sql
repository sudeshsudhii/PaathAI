-- V4: Course Intelligence Platform
-- Adds course status, topic coverage tracking, knowledge graph, and semester memory.

-- =====================================================
-- COURSE STATUS
-- =====================================================

ALTER TABLE courses ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_courses_status ON courses(status);

-- =====================================================
-- TOPIC COVERAGE TRACKING
-- =====================================================

ALTER TABLE syllabus_topics ADD COLUMN IF NOT EXISTS coverage_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED';
CREATE INDEX IF NOT EXISTS idx_syllabus_topics_coverage ON syllabus_topics(coverage_status);

-- =====================================================
-- KNOWLEDGE GRAPH
-- =====================================================

CREATE TABLE IF NOT EXISTS knowledge_nodes (
    id              BIGSERIAL PRIMARY KEY,
    course_id       BIGINT       NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    node_type       VARCHAR(30)  NOT NULL,  -- SUBJECT, UNIT, TOPIC, CONCEPT
    source_type     VARCHAR(30)  NOT NULL,  -- SYLLABUS, DETECTED, MANUAL
    source_id       BIGINT,
    label           VARCHAR(255) NOT NULL,
    description     TEXT,
    embedding       vector(768),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS knowledge_edges (
    id              BIGSERIAL PRIMARY KEY,
    course_id       BIGINT       NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    source_node_id  BIGINT       NOT NULL REFERENCES knowledge_nodes(id) ON DELETE CASCADE,
    target_node_id  BIGINT       NOT NULL REFERENCES knowledge_nodes(id) ON DELETE CASCADE,
    edge_type       VARCHAR(30)  NOT NULL,  -- PREREQUISITE, RELATED, PARENT_CHILD
    weight          DECIMAL(5,4) DEFAULT 1.0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(source_node_id, target_node_id, edge_type)
);

-- =====================================================
-- SEMESTER MEMORY
-- =====================================================

CREATE TABLE IF NOT EXISTS semester_summaries (
    id              BIGSERIAL PRIMARY KEY,
    course_id       BIGINT       NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    summary_type    VARCHAR(30)  NOT NULL,  -- LECTURE, UNIT, SUBJECT
    source_id       BIGINT,
    title           VARCHAR(255),
    content         TEXT         NOT NULL,
    estimated_tokens INT,
    embedding       vector(768),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_knowledge_nodes_course ON knowledge_nodes(course_id, node_type);
CREATE INDEX IF NOT EXISTS idx_knowledge_edges_source ON knowledge_edges(source_node_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_edges_target ON knowledge_edges(target_node_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_edges_course ON knowledge_edges(course_id, edge_type);
CREATE INDEX IF NOT EXISTS idx_semester_summaries_course ON semester_summaries(course_id, summary_type);
CREATE INDEX IF NOT EXISTS idx_semester_summaries_source ON semester_summaries(source_id, summary_type);
