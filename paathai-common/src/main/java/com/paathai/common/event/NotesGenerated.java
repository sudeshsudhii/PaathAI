package com.paathai.common.event;

/**
 * Published when notes are generated for a lecture.
 * Consumed by: Progress Service, Search Indexing.
 */
public class NotesGenerated extends BaseEvent {

    private final Long lectureId;
    private final Long notesId;
    private final int tokenCount;

    public NotesGenerated(Long lectureId, Long notesId, int tokenCount) {
        super("STUDY");
        this.lectureId = lectureId;
        this.notesId = notesId;
        this.tokenCount = tokenCount;
    }

    public Long getLectureId() { return lectureId; }
    public Long getNotesId() { return notesId; }
    public int getTokenCount() { return tokenCount; }
}
