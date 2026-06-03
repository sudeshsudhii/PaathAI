package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'live_transcript_chunks' table.
 * Stores incremental transcript segments produced during live transcription.
 */
@Entity
@Table(name = "live_transcript_chunks")
@Data
@NoArgsConstructor
public class LiveTranscriptChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "start_offset_ms")
    private Integer startOffsetMs;

    @Column(name = "end_offset_ms")
    private Integer endOffsetMs;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    /** Transcript stability state: PROVISIONAL → CONFIRMED → FINAL */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PROVISIONAL";

    /** pgvector embedding for live search (populated async by LiveSearchIndexingService). */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
