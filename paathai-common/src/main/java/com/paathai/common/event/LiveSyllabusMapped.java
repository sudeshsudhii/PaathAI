package com.paathai.common.event;

/**
 * Published when a live topic is matched to a syllabus topic via pgvector.
 * Consumed by: CoverageEngine, WebSocket push.
 */
public class LiveSyllabusMapped extends BaseEvent {

    private final Long sessionId;
    private final String topicName;
    private final Long matchedSyllabusTopicId;
    private final String syllabusPath; // "Subject > Unit > Topic"
    private final double matchConfidence;

    public LiveSyllabusMapped(Long sessionId, String topicName, Long matchedSyllabusTopicId,
                               String syllabusPath, double matchConfidence) {
        super("SYLLABUS");
        this.sessionId = sessionId;
        this.topicName = topicName;
        this.matchedSyllabusTopicId = matchedSyllabusTopicId;
        this.syllabusPath = syllabusPath;
        this.matchConfidence = matchConfidence;
    }

    public Long getSessionId() { return sessionId; }
    public String getTopicName() { return topicName; }
    public Long getMatchedSyllabusTopicId() { return matchedSyllabusTopicId; }
    public String getSyllabusPath() { return syllabusPath; }
    public double getMatchConfidence() { return matchConfidence; }
}
