package com.paathai.common.exception;

/**
 * Generic resource not found exception.
 * HTTP status mapping is handled by each module's @ControllerAdvice.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s not found with ID: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() { return resourceType; }
    public Object getResourceId() { return resourceId; }
}
