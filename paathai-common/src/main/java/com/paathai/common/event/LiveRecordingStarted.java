package com.paathai.common.event;

/**
 * Published when a user starts a live recording session.
 * Consumed by: Session state management.
 */
public class LiveRecordingStarted extends BaseEvent {

    private final Long sessionId;
    private final Long userId;
    private final Long courseId;
    private final String title;

    public LiveRecordingStarted(Long sessionId, Long userId, Long courseId, String title) {
        super("LIVE");
        this.sessionId = sessionId;
        this.userId = userId;
        this.courseId = courseId;
        this.title = title;
    }

    public Long getSessionId() { return sessionId; }
    public Long getUserId() { return userId; }
    public Long getCourseId() { return courseId; }
    public String getTitle() { return title; }
}
