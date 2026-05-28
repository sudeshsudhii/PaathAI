package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'transcript_chunks' table.
 * Stores chunked transcript text with pgvector embeddings for similarity search.
 */
@Entity
@Table(name = "transcript_chunks")
@Data
@NoArgsConstructor
public class TranscriptChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transcript_id", nullable = false)
    private Long transcriptId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "start_offset_ms")
    private Integer startOffsetMs;

    @Column(name = "end_offset_ms")
    private Integer endOffsetMs;

    @Column(name = "estimated_tokens")
    private Integer estimatedTokens;

    /**
     * pgvector embedding column — stored as a string representation
     * that pgvector can parse, e.g. "[0.1,0.2,...]".
     * Native queries handle the vector type directly.
     */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
