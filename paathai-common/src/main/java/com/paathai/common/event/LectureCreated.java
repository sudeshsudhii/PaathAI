package com.paathai.common.event;

/**
 * Published when a new lecture is created (audio uploaded).
 * Consumed by: Transcription Service.
 */
public class LectureCreated extends BaseEvent {

    private final Long lectureId;
    private final Long studentId;
    private final Long courseId;
    private final String audioPath;

    public LectureCreated(Long lectureId, Long studentId, Long courseId, String audioPath) {
        super("LECTURE");
        this.lectureId = lectureId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.audioPath = audioPath;
    }

    public Long getLectureId() { return lectureId; }
    public Long getStudentId() { return studentId; }
    public Long getCourseId() { return courseId; }
    public String getAudioPath() { return audioPath; }
}
