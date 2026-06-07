package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'knowledge_nodes' table.
 * Represents a node in the course knowledge graph.
 */
@Entity
@Table(name = "knowledge_nodes")
@Data
@NoArgsConstructor
public class KnowledgeNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "node_type", nullable = false, length = 30)
    private String nodeType; // SUBJECT, UNIT, TOPIC, CONCEPT

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType; // SYLLABUS, DETECTED, MANUAL

    @Column(name = "source_id")
    private Long sourceId;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
