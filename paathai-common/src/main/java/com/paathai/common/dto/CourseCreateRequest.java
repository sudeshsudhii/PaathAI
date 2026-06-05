package com.paathai.common.dto;

import lombok.Data;

/**
 * Request body for creating or updating a course.
 */
@Data
public class CourseCreateRequest {
    private String name;
    private String code;
    private String semester;
    private String description;
}
