package com.paathai.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Full nested syllabus tree response: Syllabus → Subjects → Units → Topics.
 */
@Data
@Builder
public class SyllabusTreeResponse {
    private Long syllabusId;
    private Long courseId;
    private String title;
    private String sourceType;
    private List<SubjectNode> subjects;

    @Data
    @Builder
    public static class SubjectNode {
        private Long id;
        private String name;
        private int sortOrder;
        private List<UnitNode> units;
    }

    @Data
    @Builder
    public static class UnitNode {
        private Long id;
        private String name;
        private int sortOrder;
        private List<TopicNode> topics;
    }

    @Data
    @Builder
    public static class TopicNode {
        private Long id;
        private String name;
        private String description;
        private int sortOrder;
        private String coverageStatus;
    }
}
