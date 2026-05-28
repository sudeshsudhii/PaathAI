package com.paathai.common.event;

/**
 * Published when a single transcript chunk is created during streaming transcription.
 * Consumed by: Search Indexing (streaming).
 */
public class TranscriptChunkCreated extends BaseEvent {

    private final Long lectureId;
    private final int chunkIndex;
    private final String text;
    private final int startOffsetMs;
    private final int endOffsetMs;

    public TranscriptChunkCreated(Long lectureId, int chunkIndex, String text,
                                   int startOffsetMs, int endOffsetMs) {
        super("LECTURE");
        this.lectureId = lectureId;
        this.chunkIndex = chunkIndex;
        this.text = text;
        this.startOffsetMs = startOffsetMs;
        this.endOffsetMs = endOffsetMs;
    }

    public Long getLectureId() { return lectureId; }
    public int getChunkIndex() { return chunkIndex; }
    public String getText() { return text; }
    public int getStartOffsetMs() { return startOffsetMs; }
    public int getEndOffsetMs() { return endOffsetMs; }
}
