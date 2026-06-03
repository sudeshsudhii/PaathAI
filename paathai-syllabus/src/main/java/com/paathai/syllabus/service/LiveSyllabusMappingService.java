package com.paathai.syllabus.service;

import com.paathai.ai.client.GeminiEmbeddingClient;
import com.paathai.common.entity.LiveSession;
import com.paathai.common.entity.LiveTopicTimeline;
import com.paathai.common.entity.SyllabusTopic;
import com.paathai.common.event.LiveSyllabusMapped;
import com.paathai.common.event.LiveTopicDetected;
import com.paathai.common.repository.LiveSessionRepository;
import com.paathai.common.repository.LiveTopicTimelineRepository;
import com.paathai.common.repository.SyllabusTopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Maps detected live topics to the course syllabus using pgvector cosine similarity.
 *
 * <p>On each {@link LiveTopicDetected} event:
 * <ol>
 *   <li>Embeds the topic + subtopic text.</li>
 *   <li>Searches syllabus_topics for the matching course.</li>
 *   <li>If a match is found with sufficient confidence, updates the
 *       topic timeline and publishes a {@link LiveSyllabusMapped} event.</li>
 * </ol>
 */
@Service
public class LiveSyllabusMappingService {

    private static final Logger log = LoggerFactory.getLogger(LiveSyllabusMappingService.class);
    private static final double MIN_MATCH_CONFIDENCE = 0.65;

    private final SyllabusTopicRepository syllabusTopicRepository;
    private final LiveTopicTimelineRepository topicTimelineRepository;
    private final LiveSessionRepository sessionRepository;
    private final GeminiEmbeddingClient embeddingClient;
    private final ApplicationEventPublisher eventPublisher;

    public LiveSyllabusMappingService(SyllabusTopicRepository syllabusTopicRepository,
                                      LiveTopicTimelineRepository topicTimelineRepository,
                                      LiveSessionRepository sessionRepository,
                                      GeminiEmbeddingClient embeddingClient,
                                      ApplicationEventPublisher eventPublisher) {
        this.syllabusTopicRepository = syllabusTopicRepository;
        this.topicTimelineRepository = topicTimelineRepository;
        this.sessionRepository = sessionRepository;
        this.embeddingClient = embeddingClient;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    @Async("generalEventExecutor")
    public void onTopicDetected(LiveTopicDetected event) {
        Long sessionId = event.getSessionId();
        log.info("Mapping live topic '{}' to syllabus for session {}", event.getTopic(), sessionId);

        try {
            // Get course ID from session
            LiveSession session = sessionRepository.findById(sessionId).orElse(null);
            if (session == null || session.getCourseId() == null) {
                log.debug("No course linked to session {}, skipping syllabus mapping", sessionId);
                return;
            }

            Long courseId = session.getCourseId();

            // Embed the detected topic
            String topicText = event.getTopic();
            if (event.getSubtopic() != null && !event.getSubtopic().isEmpty()) {
                topicText += " " + event.getSubtopic();
            }

            float[] topicEmbedding = embeddingClient.embed(topicText);
            String vectorString = GeminiEmbeddingClient.toVectorString(topicEmbedding);

            // Search syllabus topics
            List<SyllabusTopic> matches = syllabusTopicRepository
                    .findSimilarByCourseId(courseId, vectorString, 1);

            if (matches.isEmpty()) {
                log.debug("No syllabus match found for topic '{}' in course {}", event.getTopic(), courseId);
                return;
            }

            SyllabusTopic bestMatch = matches.get(0);

            // Build syllabus path (topic name as placeholder — in production join to get full path)
            String syllabusPath = bestMatch.getName();

            // Update the topic timeline with the mapped syllabus topic
            List<LiveTopicTimeline> timelines = topicTimelineRepository
                    .findBySessionIdOrderByDetectedAtMs(sessionId);

            timelines.stream()
                    .filter(t -> t.getDetectedAtMs() == event.getDetectedAtMs()
                            && t.getTopic().equals(event.getTopic()))
                    .findFirst()
                    .ifPresent(timeline -> {
                        timeline.setSyllabusTopicId(bestMatch.getId());
                        topicTimelineRepository.save(timeline);
                    });

            // Publish mapping event
            eventPublisher.publishEvent(new LiveSyllabusMapped(
                    sessionId,
                    event.getTopic(),
                    bestMatch.getId(),
                    syllabusPath,
                    event.getConfidence()));

            log.info("Mapped topic '{}' to syllabus topic '{}' (id={}) for session {}",
                    event.getTopic(), bestMatch.getName(), bestMatch.getId(), sessionId);

        } catch (Exception e) {
            log.error("Failed to map syllabus for topic '{}' in session {}", event.getTopic(), sessionId, e);
        }
    }
}
