package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'lectures' table.
 * Tracks audio upload and transcription status lifecycle:
 * PENDING → TRANSCRIBING → COMPLETED / FAILED
 */
@Entity
@Table(name = "lectures")
@Data
@NoArgsConstructor
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    private String title;

    @Column(name = "audio_path", nullable = false, length = 500)
    private String audioPath;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
