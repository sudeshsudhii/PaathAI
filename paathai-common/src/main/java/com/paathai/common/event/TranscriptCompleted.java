package com.paathai.common.event;

/**
 * Published when the full transcript for a lecture is completed.
 * Fan-out consumers: Topic Detection, Notes Generation, Search Indexing.
 * Each consumer is independent — failure in one does NOT block others.
 */
public class TranscriptCompleted extends BaseEvent {

    private final Long lectureId;
    private final Long transcriptId;
    private final int totalChunks;
    private final int estimatedTokens;

    public TranscriptCompleted(Long lectureId, Long transcriptId,
                                int totalChunks, int estimatedTokens) {
        super("LECTURE");
        this.lectureId = lectureId;
        this.transcriptId = transcriptId;
        this.totalChunks = totalChunks;
        this.estimatedTokens = estimatedTokens;
    }

    public Long getLectureId() { return lectureId; }
    public Long getTranscriptId() { return transcriptId; }
    public int getTotalChunks() { return totalChunks; }
    public int getEstimatedTokens() { return estimatedTokens; }
}
