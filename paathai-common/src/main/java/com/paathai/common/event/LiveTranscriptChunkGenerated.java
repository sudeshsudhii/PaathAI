package com.paathai.common.event;

/**
 * Published when Whisper returns a transcript for a live audio chunk.
 * Consumed by: LiveTopicDetectionService, LiveNotesGenerationService,
 *              LiveSearchIndexingService, WebSocket push to client.
 */
public class LiveTranscriptChunkGenerated extends BaseEvent {

    private final Long sessionId;
    private final int sequenceNumber;
    private final String text;
    private final int startOffsetMs;
    private final int endOffsetMs;
    private final String status; // PROVISIONAL, CONFIRMED, FINAL

    public LiveTranscriptChunkGenerated(Long sessionId, int sequenceNumber, String text,
                                         int startOffsetMs, int endOffsetMs) {
        this(sessionId, sequenceNumber, text, startOffsetMs, endOffsetMs, "PROVISIONAL");
    }

    public LiveTranscriptChunkGenerated(Long sessionId, int sequenceNumber, String text,
                                         int startOffsetMs, int endOffsetMs, String status) {
        super("LIVE");
        this.sessionId = sessionId;
        this.sequenceNumber = sequenceNumber;
        this.text = text;
        this.startOffsetMs = startOffsetMs;
        this.endOffsetMs = endOffsetMs;
        this.status = status;
    }

    public Long getSessionId() { return sessionId; }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getText() { return text; }
    public int getStartOffsetMs() { return startOffsetMs; }
    public int getEndOffsetMs() { return endOffsetMs; }
    public String getStatus() { return status; }
}
