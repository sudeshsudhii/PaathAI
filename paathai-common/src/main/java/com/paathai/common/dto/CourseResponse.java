package com.paathai.common.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for course details including computed fields.
 */
@Data
@Builder
public class CourseResponse {
    private Long id;
    private String name;
    private String code;
    private String semester;
    private String description;
    private String status;
    private long lectureCount;
    private boolean syllabusAttached;
    private String createdAt;
    private String updatedAt;
}
