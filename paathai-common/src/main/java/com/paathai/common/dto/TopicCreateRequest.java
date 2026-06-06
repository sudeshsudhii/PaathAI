package com.paathai.common.dto;

import lombok.Data;

/**
 * Request body for creating a new topic within a unit.
 */
@Data
public class TopicCreateRequest {
    private String name;
    private String description;
    private Integer sortOrder;
}
