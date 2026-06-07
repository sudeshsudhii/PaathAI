package com.paathai.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Hierarchical coverage response: Course → Subjects → Units → Topics.
 * All percentages are deterministic (no LLM calls).
 */
@Data
@Builder
public class CourseCoverageResponse {
    private Long courseId;
    private double overallPercent;
    private int totalTopics;
    private int coveredTopics;
    private List<SubjectCoverage> subjects;

    @Data
    @Builder
    public static class SubjectCoverage {
        private Long id;
        private String name;
        private double percent;
        private String status; // NOT_STARTED, IN_PROGRESS, COMPLETED
        private List<UnitCoverage> units;
    }

    @Data
    @Builder
    public static class UnitCoverage {
        private Long id;
        private String name;
        private double percent;
        private String status;
        private List<TopicCoverage> topics;
    }

    @Data
    @Builder
    public static class TopicCoverage {
        private Long id;
        private String name;
        private String status;
    }
}
