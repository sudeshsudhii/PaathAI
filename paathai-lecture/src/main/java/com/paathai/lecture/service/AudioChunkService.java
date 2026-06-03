package com.paathai.lecture.service;

import com.paathai.common.entity.LiveAudioChunk;
import com.paathai.common.event.AudioChunkReceived;
import com.paathai.common.repository.LiveAudioChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AudioChunkService {

    private static final Logger log = LoggerFactory.getLogger(AudioChunkService.class);

    private final LiveAudioChunkRepository liveAudioChunkRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Path storagePath;

    public AudioChunkService(LiveAudioChunkRepository liveAudioChunkRepository,
                             ApplicationEventPublisher eventPublisher,
                             @Value("${paathai.live.audio-storage-path:${java.io.tmpdir}/paathai-live-audio}") String storagePathStr) {
        this.liveAudioChunkRepository = liveAudioChunkRepository;
        this.eventPublisher = eventPublisher;
        this.storagePath = Paths.get(storagePathStr);
        
        try {
            Files.createDirectories(this.storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize live audio storage directory", e);
        }
    }

    public void processChunk(Long sessionId, int sequenceNumber, byte[] audioBytes) {
        if (audioBytes == null || audioBytes.length == 0) {
            log.warn("Empty audio chunk received for session {}, seq {}", sessionId, sequenceNumber);
            return;
        }

        try {
            // Save to disk
            String filename = String.format("session_%d_chunk_%d.webm", sessionId, sequenceNumber);
            Path filePath = storagePath.resolve(filename);
            Files.write(filePath, audioBytes);

            // Save to DB
            LiveAudioChunk chunk = new LiveAudioChunk();
            chunk.setSessionId(sessionId);
            chunk.setSequenceNumber(sequenceNumber);
            chunk.setAudioPath(filePath.toString());
            chunk.setSizeBytes(audioBytes.length);
            chunk.setProcessed(false);
            
            chunk = liveAudioChunkRepository.save(chunk);
            log.debug("Saved audio chunk: session {}, seq {}, {} bytes", sessionId, sequenceNumber, audioBytes.length);

            // Publish event for TranscriptionService
            eventPublisher.publishEvent(new AudioChunkReceived(
                    sessionId, sequenceNumber, filePath.toString(), audioBytes.length));
                    
        } catch (IOException e) {
            log.error("Failed to process audio chunk for session {}, seq {}", sessionId, sequenceNumber, e);
        }
    }
}
