package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'knowledge_edges' table.
 * Represents a directed edge between two knowledge nodes.
 */
@Entity
@Table(name = "knowledge_edges", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"source_node_id", "target_node_id", "edge_type"})
})
@Data
@NoArgsConstructor
public class KnowledgeEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "source_node_id", nullable = false)
    private Long sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private Long targetNodeId;

    @Column(name = "edge_type", nullable = false, length = 30)
    private String edgeType; // PREREQUISITE, RELATED, PARENT_CHILD

    @Column(precision = 5, scale = 4)
    private BigDecimal weight = BigDecimal.ONE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
