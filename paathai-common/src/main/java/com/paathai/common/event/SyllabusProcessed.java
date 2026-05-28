package com.paathai.common.event;

/**
 * Published when a syllabus has been fully processed (parsed + validated + stored).
 * Consumed by: Knowledge Graph (initial CONTAINS/NEXT_IN_UNIT edges).
 */
public class SyllabusProcessed extends BaseEvent {

    private final Long syllabusId;
    private final Long courseId;
    private final int subjectCount;
    private final int unitCount;
    private final int topicCount;

    public SyllabusProcessed(Long syllabusId, Long courseId,
                              int subjectCount, int unitCount, int topicCount) {
        super("SYLLABUS");
        this.syllabusId = syllabusId;
        this.courseId = courseId;
        this.subjectCount = subjectCount;
        this.unitCount = unitCount;
        this.topicCount = topicCount;
    }

    public Long getSyllabusId() { return syllabusId; }
    public Long getCourseId() { return courseId; }
    public int getSubjectCount() { return subjectCount; }
    public int getUnitCount() { return unitCount; }
    public int getTopicCount() { return topicCount; }
}
