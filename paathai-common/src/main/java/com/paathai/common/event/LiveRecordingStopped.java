package com.paathai.common.event;

/**
 * Published when the user stops a live recording session.
 * Consumed by: SessionFinalizationService (creates Lecture + Transcript from live data).
 */
public class LiveRecordingStopped extends BaseEvent {

    private final Long sessionId;
    private final Long userId;
    private final long totalDurationMs;

    public LiveRecordingStopped(Long sessionId, Long userId, long totalDurationMs) {
        super("LIVE");
        this.sessionId = sessionId;
        this.userId = userId;
        this.totalDurationMs = totalDurationMs;
    }

    public Long getSessionId() { return sessionId; }
    public Long getUserId() { return userId; }
    public long getTotalDurationMs() { return totalDurationMs; }
}
