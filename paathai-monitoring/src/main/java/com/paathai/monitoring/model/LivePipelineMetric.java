package com.paathai.monitoring.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Records pipeline stage latency metrics during live sessions.
 * Populated by {@code LiveSessionMonitoringService}.
 */
@Entity
@Table(name = "live_pipeline_metrics")
@Data
@NoArgsConstructor
public class LivePipelineMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "pipeline_stage", nullable = false, length = 50)
    private String pipelineStage;

    @Column(name = "chunk_seq")
    private Integer chunkSeq;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Column(nullable = false)
    private Boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
