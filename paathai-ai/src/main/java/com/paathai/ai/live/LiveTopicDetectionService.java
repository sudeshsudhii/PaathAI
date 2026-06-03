package com.paathai.ai.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paathai.ai.costcontroller.AICostController;
import com.paathai.ai.costcontroller.LlmResponse;
import com.paathai.common.dto.FeatureType;
import com.paathai.common.entity.LiveSession;
import com.paathai.common.entity.LiveTopicTimeline;
import com.paathai.common.entity.LiveTranscriptChunk;
import com.paathai.common.event.LiveTopicDetected;
import com.paathai.common.event.LiveTranscriptChunkGenerated;
import com.paathai.common.repository.LiveSessionRepository;
import com.paathai.common.repository.LiveTopicTimelineRepository;
import com.paathai.common.repository.LiveTranscriptChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Live topic detection with content-based triggering.
 *
 * <p>Replaces the previous 45-second fixed timer with a multi-condition buffer:
 * <ul>
 *   <li>≥5 transcript chunks accumulated</li>
 *   <li>≥1000 estimated tokens accumulated</li>
 *   <li>≥15 seconds since the first buffered chunk</li>
 * </ul>
 * Whichever fires first triggers topic detection.</p>
 */
@Service
public class LiveTopicDetectionService {

    private static final Logger log = LoggerFactory.getLogger(LiveTopicDetectionService.class);

    private static final int CHUNK_THRESHOLD = 5;
    private static final int TOKEN_THRESHOLD = 1000;
    private static final long TIME_THRESHOLD_MS = 15_000;

    private final LiveTranscriptChunkRepository transcriptChunkRepository;
    private final LiveTopicTimelineRepository topicTimelineRepository;
    private final LiveSessionRepository sessionRepository;
    private final AICostController aiCostController;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /** Per-session chunk buffer. */
    private final Map<Long, TopicChunkBuffer> sessionBuffers = new ConcurrentHashMap<>();

    public LiveTopicDetectionService(LiveTranscriptChunkRepository transcriptChunkRepository,
                                     LiveTopicTimelineRepository topicTimelineRepository,
                                     LiveSessionRepository sessionRepository,
                                     AICostController aiCostController,
                                     ApplicationEventPublisher eventPublisher,
                                     ObjectMapper objectMapper) {
        this.transcriptChunkRepository = transcriptChunkRepository;
        this.topicTimelineRepository = topicTimelineRepository;
        this.sessionRepository = sessionRepository;
        this.aiCostController = aiCostController;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ //
    //  Event Listener
    // ------------------------------------------------------------------ //

    @EventListener
    @Async("liveProcessingExecutor")
    public void onTranscriptChunkGenerated(LiveTranscriptChunkGenerated event) {
        Long sessionId = event.getSessionId();

        TopicChunkBuffer buffer = sessionBuffers.computeIfAbsent(sessionId, k -> new TopicChunkBuffer());

        int estimatedTokens = (int) Math.ceil(event.getText().length() / 4.0);

        synchronized (buffer) {
            buffer.texts.add(event.getText());
            buffer.accumulatedTokens += estimatedTokens;
            buffer.latestOffsetMs = event.getEndOffsetMs();
            if (buffer.firstChunkTime == null) {
                buffer.firstChunkTime = Instant.now();
            }

            boolean chunkTrigger = buffer.texts.size() >= CHUNK_THRESHOLD;
            boolean tokenTrigger = buffer.accumulatedTokens >= TOKEN_THRESHOLD;
            boolean timeTrigger = Instant.now().toEpochMilli() - buffer.firstChunkTime.toEpochMilli() >= TIME_THRESHOLD_MS;

            if (!chunkTrigger && !tokenTrigger && !timeTrigger) {
                return; // Not ready yet
            }

            // Flush
            int currentMs = buffer.latestOffsetMs;
            buffer.texts.clear();
            buffer.accumulatedTokens = 0;
            buffer.firstChunkTime = null;

            // Run detection outside synchronized block
            runDetection(sessionId, currentMs);
        }
    }

    // ------------------------------------------------------------------ //
    //  Topic Detection Logic
    // ------------------------------------------------------------------ //

    private void runDetection(Long sessionId, int currentMs) {
        log.info("Running content-triggered topic detection for session {} at {}ms", sessionId, currentMs);

        try {
            // Get last 20 chunks (~80 seconds of audio)
            List<LiveTranscriptChunk> recentChunks = transcriptChunkRepository
                    .findTop20BySessionIdOrderBySequenceNumberDesc(sessionId);

            String recentTranscript = recentChunks.stream()
                    .sorted((a, b) -> a.getSequenceNumber().compareTo(b.getSequenceNumber()))
                    .map(LiveTranscriptChunk::getContent)
                    .collect(Collectors.joining(" "));

            // Get previous topics
            List<LiveTopicTimeline> pastTopics = topicTimelineRepository
                    .findBySessionIdOrderByDetectedAtMs(sessionId);

            String previousTopicsStr = pastTopics.isEmpty() ? "None" :
                    pastTopics.stream()
                            .map(t -> t.getTopic() + (t.getSubtopic() != null ? " - " + t.getSubtopic() : ""))
                            .distinct()
                            .collect(Collectors.joining(", "));

            // Get user ID
            Long userId = sessionRepository.findById(sessionId)
                    .map(LiveSession::getUserId)
                    .orElse(1L);

            // Construct prompt
            String prompt = String.format("""
                You are analyzing a live lecture in progress. Based on the recent transcript segment below, \
                identify the current academic topic and subtopic being discussed.

                Respond in JSON format exactly like this, with no markdown wrappers:
                {"topic": "...", "subtopic": "...", "confidence": 0.95}

                Previously detected topics:
                %s

                Recent transcript (last 60 seconds):
                %s
                """, previousTopicsStr, recentTranscript);

            LlmResponse response = aiCostController.execute(prompt, FeatureType.LIVE_TOPIC_DETECTION, userId);

            String content = response.content().trim();
            if (content.startsWith("```json")) {
                content = content.substring(7);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }

            JsonNode root = objectMapper.readTree(content.trim());
            String topic = root.has("topic") ? root.get("topic").asText() : "General";
            String subtopic = root.has("subtopic") ? root.get("subtopic").asText() : "";
            double confidence = root.has("confidence") ? root.get("confidence").asDouble() : 0.8;

            LiveTopicTimeline timeline = new LiveTopicTimeline();
            timeline.setSessionId(sessionId);
            timeline.setTopic(topic);
            timeline.setSubtopic(subtopic);
            timeline.setConfidence(BigDecimal.valueOf(confidence));
            timeline.setDetectedAtMs(currentMs);

            topicTimelineRepository.save(timeline);

            eventPublisher.publishEvent(new LiveTopicDetected(
                    sessionId, topic, subtopic, confidence, currentMs));

        } catch (Exception e) {
            log.error("Live topic detection failed for session {}", sessionId, e);
        }
    }

    // ------------------------------------------------------------------ //
    //  Buffer
    // ------------------------------------------------------------------ //

    private static class TopicChunkBuffer {
        final List<String> texts = new ArrayList<>();
        int accumulatedTokens = 0;
        int latestOffsetMs = 0;
        Instant firstChunkTime = null;
    }
}
