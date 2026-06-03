package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'live_topic_timeline' table.
 * Tracks topic transitions detected during a live session.
 */
@Entity
@Table(name = "live_topic_timeline")
@Data
@NoArgsConstructor
public class LiveTopicTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private String topic;

    private String subtopic;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "detected_at_ms", nullable = false)
    private Integer detectedAtMs;

    @Column(name = "syllabus_topic_id")
    private Long syllabusTopicId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
