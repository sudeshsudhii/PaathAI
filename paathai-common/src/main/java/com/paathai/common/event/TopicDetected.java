package com.paathai.common.event;

import java.util.List;

/**
 * Published when topics are detected from a transcript.
 * Fan-out consumers: Knowledge Graph (Phase 2), Flashcards (Phase 2), Quiz (Phase 2).
 */
public class TopicDetected extends BaseEvent {

    private final Long lectureId;
    private final List<String> topics;
    private final Long courseId;

    public TopicDetected(Long lectureId, List<String> topics, Long courseId) {
        super("AI");
        this.lectureId = lectureId;
        this.topics = List.copyOf(topics);
        this.courseId = courseId;
    }

    public Long getLectureId() { return lectureId; }
    public List<String> getTopics() { return topics; }
    public Long getCourseId() { return courseId; }
}
