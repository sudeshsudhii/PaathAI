package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'live_sessions' table.
 * Represents a real-time lecture recording session.
 * Lifecycle: RECORDING → PAUSED → RECORDING → COMPLETED / FAILED
 */
@Entity
@Table(name = "live_sessions")
@Data
@NoArgsConstructor
public class LiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "lecture_id")
    private Long lectureId;

    private String title;

    @Column(nullable = false, length = 20)
    private String status = "RECORDING";

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "total_duration_ms")
    private Long totalDurationMs = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
