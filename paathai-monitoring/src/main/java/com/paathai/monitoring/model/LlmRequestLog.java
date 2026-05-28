package com.paathai.monitoring.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity recording every LLM API call for observability and cost tracking.
 * Populated by the LlmRequestLoggingAspect.
 */
@Entity
@Table(name = "llm_request_logs")
@Data
@NoArgsConstructor
public class LlmRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "feature_type", nullable = false, length = 50)
    private String featureType;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "estimated_cost_usd")
    private Double estimatedCostUsd;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "prompt_type", length = 50)
    private String promptType;

    @Column(name = "prompt_version")
    private Integer promptVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
