package com.paathai.common.event;

/**
 * Published when incremental notes are updated during a live session.
 * Consumed by: WebSocket push to client.
 */
public class LiveNotesUpdated extends BaseEvent {

    private final Long sessionId;
    private final int revisionNumber;
    private final String content;
    private final String deltaContent;

    public LiveNotesUpdated(Long sessionId, int revisionNumber, String content, String deltaContent) {
        super("LIVE");
        this.sessionId = sessionId;
        this.revisionNumber = revisionNumber;
        this.content = content;
        this.deltaContent = deltaContent;
    }

    public Long getSessionId() { return sessionId; }
    public int getRevisionNumber() { return revisionNumber; }
    public String getContent() { return content; }
    public String getDeltaContent() { return deltaContent; }
}
