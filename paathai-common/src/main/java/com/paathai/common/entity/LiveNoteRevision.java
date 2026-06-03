package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'live_note_revisions' table.
 * Stores incremental note updates generated during a live session.
 * Each revision contains both the full notes document and the delta (new content only).
 */
@Entity
@Table(name = "live_note_revisions")
@Data
@NoArgsConstructor
public class LiveNoteRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "delta_content", columnDefinition = "TEXT")
    private String deltaContent;

    @Column(name = "trigger_topic")
    private String triggerTopic;

    @Column(name = "estimated_tokens")
    private Integer estimatedTokens;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
