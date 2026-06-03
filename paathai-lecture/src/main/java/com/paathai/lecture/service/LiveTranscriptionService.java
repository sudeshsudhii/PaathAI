package com.paathai.lecture.service;

import com.paathai.common.entity.LiveTranscriptChunk;
import com.paathai.common.event.AudioChunkReceived;
import com.paathai.common.event.LiveTranscriptChunkConfirmed;
import com.paathai.common.event.LiveTranscriptChunkGenerated;
import com.paathai.common.repository.LiveTranscriptChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

/**
 * Transcription service for live audio chunks.
 *
 * <p>Implements transcript stability: new chunks arrive as PROVISIONAL,
 * then get promoted to CONFIRMED once 2 additional chunks have been
 * received for the same session (providing additional context validation).
 * All chunks are promoted to FINAL on session end by
 * {@link SessionFinalizationService}.</p>
 */
@Service
public class LiveTranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(LiveTranscriptionService.class);

    /** Number of subsequent chunks needed before a PROVISIONAL chunk is CONFIRMED. */
    private static final int CONFIRMATION_LOOKBACK = 2;

    private final LiveTranscriptChunkRepository transcriptChunkRepository;
    private final RestTemplate restTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${paathai.whisper.url:http://localhost:9000}")
    private String whisperUrl;

    public LiveTranscriptionService(LiveTranscriptChunkRepository transcriptChunkRepository,
                                    RestTemplate restTemplate,
                                    ApplicationEventPublisher eventPublisher) {
        this.transcriptChunkRepository = transcriptChunkRepository;
        this.restTemplate = restTemplate;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    @Async("liveTranscriptionExecutor")
    public void onAudioChunkReceived(AudioChunkReceived event) {
        log.debug("Processing audio chunk: session {}, seq {}", event.getSessionId(), event.getSequenceNumber());

        String audioPath = event.getAudioPath();
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            log.error("Live audio file not found: {}", audioPath);
            return;
        }

        try {
            String transcribedText = callWhisperService(audioFile);

            if (transcribedText != null && !transcribedText.trim().isEmpty()) {
                int startOffsetMs = event.getSequenceNumber() * 4000;
                int endOffsetMs = startOffsetMs + 4000;

                // Save as PROVISIONAL
                LiveTranscriptChunk chunk = new LiveTranscriptChunk();
                chunk.setSessionId(event.getSessionId());
                chunk.setSequenceNumber(event.getSequenceNumber());
                chunk.setContent(transcribedText.trim());
                chunk.setStartOffsetMs(startOffsetMs);
                chunk.setEndOffsetMs(endOffsetMs);
                chunk.setConfidence(new BigDecimal("0.9500"));
                chunk.setStatus("PROVISIONAL");

                transcriptChunkRepository.save(chunk);

                // Publish with PROVISIONAL status
                eventPublisher.publishEvent(new LiveTranscriptChunkGenerated(
                        event.getSessionId(), event.getSequenceNumber(),
                        chunk.getContent(), startOffsetMs, endOffsetMs, "PROVISIONAL"));

                // Check if older chunks can be promoted to CONFIRMED
                promoteOlderChunks(event.getSessionId(), event.getSequenceNumber());
            }

        } catch (Exception e) {
            log.error("Live transcription failed for session {}, seq {}: {}",
                    event.getSessionId(), event.getSequenceNumber(), e.getMessage());
        }
    }

    /**
     * Promote PROVISIONAL chunks to CONFIRMED once enough subsequent chunks exist.
     * A chunk at seqN is confirmed when seqN+{@value CONFIRMATION_LOOKBACK} has been received.
     */
    private void promoteOlderChunks(Long sessionId, int currentSeqNum) {
        int targetSeq = currentSeqNum - CONFIRMATION_LOOKBACK;
        if (targetSeq < 0) return;

        try {
            List<LiveTranscriptChunk> provisionalChunks = transcriptChunkRepository
                    .findBySessionIdAndStatusAndSequenceNumberLessThanEqual(
                            sessionId, "PROVISIONAL", targetSeq);

            for (LiveTranscriptChunk chunk : provisionalChunks) {
                chunk.setStatus("CONFIRMED");
                transcriptChunkRepository.save(chunk);

                eventPublisher.publishEvent(new LiveTranscriptChunkConfirmed(
                        sessionId, chunk.getSequenceNumber(), chunk.getContent(),
                        chunk.getStartOffsetMs(), chunk.getEndOffsetMs()));

                log.debug("Promoted chunk session={} seq={} to CONFIRMED", sessionId, chunk.getSequenceNumber());
            }
        } catch (Exception e) {
            log.warn("Failed to promote chunks for session {}: {}", sessionId, e.getMessage());
        }
    }

    private String callWhisperService(File audioFile) {
        try {
            String url = whisperUrl + "/asr?output=txt&language=en";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio_file", new FileSystemResource(audioFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Whisper returned status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Live Whisper service unavailable, using mock transcription: {}", e.getMessage());
            return " [Audio segment] ";
        }
    }
}
