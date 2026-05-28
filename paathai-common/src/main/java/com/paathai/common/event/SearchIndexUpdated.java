package com.paathai.common.event;

/**
 * Published when search index has been updated for a lecture.
 * Terminal event — no downstream consumers.
 */
public class SearchIndexUpdated extends BaseEvent {

    private final Long lectureId;
    private final int chunksIndexed;

    public SearchIndexUpdated(Long lectureId, int chunksIndexed) {
        super("RAG");
        this.lectureId = lectureId;
        this.chunksIndexed = chunksIndexed;
    }

    public Long getLectureId() { return lectureId; }
    public int getChunksIndexed() { return chunksIndexed; }
}
