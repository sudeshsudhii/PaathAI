package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'syllabi' table.
 * Each course has at most one syllabus (UNIQUE constraint on course_id).
 */
@Entity
@Table(name = "syllabi")
@Data
@NoArgsConstructor
public class Syllabus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false, unique = true)
    private Long courseId;

    private String title;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType; // PDF, JSON, MANUAL

    @Column(name = "raw_file_path", length = 500)
    private String rawFilePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
