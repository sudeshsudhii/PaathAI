package com.paathai.syllabus.service;

import com.paathai.common.event.LiveSyllabusMapped;
import com.paathai.common.repository.LiveTopicTimelineRepository;
import com.paathai.common.repository.SyllabusTopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Calculates syllabus coverage percentage for a live session.
 *
 * <p>Listens to {@link LiveSyllabusMapped} events and computes:
 * {@code coverage% = distinctMatchedTopics / totalSyllabusTopics × 100}</p>
 *
 * <p>For the MVP, coverage is logged and sent to the WebSocket
 * via the existing event push mechanism.</p>
 */
@Service
public class CoverageEngine {

    private static final Logger log = LoggerFactory.getLogger(CoverageEngine.class);

    private final LiveTopicTimelineRepository topicTimelineRepository;
    private final SyllabusTopicRepository syllabusTopicRepository;

    public CoverageEngine(LiveTopicTimelineRepository topicTimelineRepository,
                          SyllabusTopicRepository syllabusTopicRepository) {
        this.topicTimelineRepository = topicTimelineRepository;
        this.syllabusTopicRepository = syllabusTopicRepository;
    }

    @EventListener
    @Async("generalEventExecutor")
    public void onSyllabusMapped(LiveSyllabusMapped event) {
        Long sessionId = event.getSessionId();

        try {
            // Count distinct matched syllabus topics for this session
            long matchedTopics = topicTimelineRepository
                    .findBySessionIdOrderByDetectedAtMs(sessionId)
                    .stream()
                    .filter(t -> t.getSyllabusTopicId() != null)
                    .map(t -> t.getSyllabusTopicId())
                    .distinct()
                    .count();

            // Count total syllabus topics (simplified: all topics in the repo)
            long totalTopics = syllabusTopicRepository.count();

            double coveragePercent = totalTopics > 0
                    ? (double) matchedTopics / totalTopics * 100.0
                    : 0.0;

            log.info("Session {} coverage: {}/{} topics = {:.1f}%",
                    sessionId, matchedTopics, totalTopics, coveragePercent);

            // Note: coverage updates reach the frontend via the LiveSyllabusMapped
            // event already handled by LiveSessionWebSocketHandler.

        } catch (Exception e) {
            log.error("Coverage calculation failed for session {}", sessionId, e);
        }
    }
}
