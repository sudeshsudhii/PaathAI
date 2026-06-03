package com.paathai.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'live_audio_chunks' table.
 * Stores metadata for raw audio chunks received via WebSocket during live recording.
 */
@Entity
@Table(name = "live_audio_chunks")
@Data
@NoArgsConstructor
public class LiveAudioChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "audio_path", nullable = false, length = 500)
    private String audioPath;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "size_bytes")
    private Integer sizeBytes;

    @Column(nullable = false)
    private Boolean processed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
