package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'syllabus_topics' table.
 * Represents a topic within a unit, with an optional pgvector embedding.
 */
@Entity
@Table(name = "syllabus_topics")
@Data
@NoArgsConstructor
public class SyllabusTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** pgvector embedding — stored as TEXT for JPA compatibility. */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "coverage_status", nullable = false, length = 20)
    private String coverageStatus = "NOT_STARTED"; // NOT_STARTED, IN_PROGRESS, COMPLETED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
