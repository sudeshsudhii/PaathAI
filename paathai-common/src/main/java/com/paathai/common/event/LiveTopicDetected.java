package com.paathai.common.event;

/**
 * Published when topic detection identifies a topic during a live session.
 * Distinct from the existing TopicDetected event (which is for batch upload flow).
 * Consumed by: LiveNotesGenerationService, LiveSyllabusMappingService, WebSocket push.
 */
public class LiveTopicDetected extends BaseEvent {

    private final Long sessionId;
    private final String topic;
    private final String subtopic;
    private final double confidence;
    private final int detectedAtMs;

    public LiveTopicDetected(Long sessionId, String topic, String subtopic,
                              double confidence, int detectedAtMs) {
        super("LIVE");
        this.sessionId = sessionId;
        this.topic = topic;
        this.subtopic = subtopic;
        this.confidence = confidence;
        this.detectedAtMs = detectedAtMs;
    }

    public Long getSessionId() { return sessionId; }
    public String getTopic() { return topic; }
    public String getSubtopic() { return subtopic; }
    public double getConfidence() { return confidence; }
    public int getDetectedAtMs() { return detectedAtMs; }
}
