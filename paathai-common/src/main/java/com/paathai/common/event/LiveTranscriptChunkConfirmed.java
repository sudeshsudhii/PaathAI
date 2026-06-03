package com.paathai.common.event;

/**
 * Published when a PROVISIONAL transcript chunk is promoted to CONFIRMED
 * (after additional context validates it).
 * Consumed by: services that want stable text only (e.g., search indexing).
 */
public class LiveTranscriptChunkConfirmed extends BaseEvent {

    private final Long sessionId;
    private final int sequenceNumber;
    private final String text;
    private final int startOffsetMs;
    private final int endOffsetMs;

    public LiveTranscriptChunkConfirmed(Long sessionId, int sequenceNumber, String text,
                                         int startOffsetMs, int endOffsetMs) {
        super("LIVE");
        this.sessionId = sessionId;
        this.sequenceNumber = sequenceNumber;
        this.text = text;
        this.startOffsetMs = startOffsetMs;
        this.endOffsetMs = endOffsetMs;
    }

    public Long getSessionId() { return sessionId; }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getText() { return text; }
    public int getStartOffsetMs() { return startOffsetMs; }
    public int getEndOffsetMs() { return endOffsetMs; }
}
