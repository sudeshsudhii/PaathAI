package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'semester_summaries' table.
 * Stores hierarchical summaries: LECTURE → UNIT → SUBJECT.
 */
@Entity
@Table(name = "semester_summaries")
@Data
@NoArgsConstructor
public class SemesterSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "summary_type", nullable = false, length = 30)
    private String summaryType; // LECTURE, UNIT, SUBJECT

    @Column(name = "source_id")
    private Long sourceId; // FK to lectures.id, units.id, or subjects.id

    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "estimated_tokens")
    private Integer estimatedTokens;

    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
