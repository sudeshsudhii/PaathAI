package com.paathai.ai.live;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paathai.ai.costcontroller.AICostController;
import com.paathai.ai.costcontroller.LlmResponse;
import com.paathai.common.dto.FeatureType;
import com.paathai.common.entity.LiveSession;
import com.paathai.common.entity.LiveTranscriptChunk;
import com.paathai.common.event.LiveAcademicInsight;
import com.paathai.common.event.LiveTopicDetected;
import com.paathai.common.repository.LiveSessionRepository;
import com.paathai.common.repository.LiveTranscriptChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates real-time academic intelligence for detected topics.
 *
 * <p>On each {@link LiveTopicDetected} event, constructs a prompt that asks the LLM
 * to produce structured academic metadata: key concepts, definitions, prerequisites,
 * and exam importance. The result is published as a {@link LiveAcademicInsight} event
 * and pushed to the student's WebSocket.</p>
 */
@Service
public class AcademicIntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(AcademicIntelligenceService.class);

    private final LiveTranscriptChunkRepository transcriptChunkRepository;
    private final LiveSessionRepository sessionRepository;
    private final AICostController aiCostController;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public AcademicIntelligenceService(LiveTranscriptChunkRepository transcriptChunkRepository,
                                       LiveSessionRepository sessionRepository,
                                       AICostController aiCostController,
                                       ApplicationEventPublisher eventPublisher,
                                       ObjectMapper objectMapper) {
        this.transcriptChunkRepository = transcriptChunkRepository;
        this.sessionRepository = sessionRepository;
        this.aiCostController = aiCostController;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @EventListener
    @Async("academicIntelligenceExecutor")
    public void onTopicDetected(LiveTopicDetected event) {
        Long sessionId = event.getSessionId();
        log.info("Generating academic intelligence for topic '{}' in session {}", event.getTopic(), sessionId);

        try {
            // Get recent transcript context
            List<LiveTranscriptChunk> recentChunks = transcriptChunkRepository
                    .findTop20BySessionIdOrderBySequenceNumberDesc(sessionId);

            String recentTranscript = recentChunks.stream()
                    .sorted((a, b) -> a.getSequenceNumber().compareTo(b.getSequenceNumber()))
                    .map(LiveTranscriptChunk::getContent)
                    .collect(Collectors.joining(" "));

            Long userId = sessionRepository.findById(sessionId)
                    .map(LiveSession::getUserId)
                    .orElse(1L);

            String prompt = String.format("""
                You are an academic intelligence assistant analyzing a live university lecture.
                
                Current Topic: %s
                Subtopic: %s
                
                Recent transcript:
                %s
                
                Provide structured academic intelligence in JSON format with NO markdown wrappers:
                {
                  "keyConcepts": ["concept1", "concept2", "concept3"],
                  "definitions": [
                    {"term": "Term Name", "definition": "Clear definition"}
                  ],
                  "prerequisites": ["prerequisite1", "prerequisite2"],
                  "examImportance": "HIGH",
                  "coverageStatus": "IN_PROGRESS"
                }
                
                Rules:
                - keyConcepts: 3-5 key concepts being discussed
                - definitions: 1-3 important terms with clear definitions
                - prerequisites: topics the student should already know
                - examImportance: HIGH, MEDIUM, or LOW
                - coverageStatus: COVERED, IN_PROGRESS, or UPCOMING
                """,
                    event.getTopic(),
                    event.getSubtopic() != null ? event.getSubtopic() : "General",
                    recentTranscript);

            LlmResponse response = aiCostController.execute(prompt, FeatureType.ACADEMIC_INTELLIGENCE, userId);

            String content = response.content().trim();
            if (content.startsWith("```json")) {
                content = content.substring(7);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }

            JsonNode root = objectMapper.readTree(content.trim());

            List<String> keyConcepts = root.has("keyConcepts")
                    ? objectMapper.convertValue(root.get("keyConcepts"), new TypeReference<List<String>>() {})
                    : List.of();

            List<Map<String, String>> definitions = root.has("definitions")
                    ? objectMapper.convertValue(root.get("definitions"), new TypeReference<List<Map<String, String>>>() {})
                    : List.of();

            List<String> prerequisites = root.has("prerequisites")
                    ? objectMapper.convertValue(root.get("prerequisites"), new TypeReference<List<String>>() {})
                    : List.of();

            String examImportance = root.has("examImportance") ? root.get("examImportance").asText() : "MEDIUM";
            String coverageStatus = root.has("coverageStatus") ? root.get("coverageStatus").asText() : "IN_PROGRESS";

            eventPublisher.publishEvent(new LiveAcademicInsight(
                    sessionId,
                    event.getTopic(),
                    event.getSubtopic(),
                    keyConcepts,
                    definitions,
                    prerequisites,
                    examImportance,
                    coverageStatus));

            log.info("Academic intelligence generated for session {}: {} concepts, {} definitions",
                    sessionId, keyConcepts.size(), definitions.size());

        } catch (Exception e) {
            log.error("Academic intelligence generation failed for session {}", sessionId, e);
        }
    }
}
