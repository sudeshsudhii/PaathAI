package com.paathai.common.event;

import java.time.Instant;

/**
 * Published when any async event consumer fails.
 * Consumed by: Monitoring Service (logging, optional retry).
 * This event ensures failure isolation — the failed service is logged
 * but does not prevent other fan-out consumers from completing.
 */
public class ProcessingFailed extends BaseEvent {

    private final Long lectureId;
    private final String serviceName;
    private final String errorMessage;
    private final Instant failedAt;

    public ProcessingFailed(Long lectureId, String serviceName, String errorMessage) {
        super("SYSTEM");
        this.lectureId = lectureId;
        this.serviceName = serviceName;
        this.errorMessage = errorMessage;
        this.failedAt = Instant.now();
    }

    public Long getLectureId() { return lectureId; }
    public String getServiceName() { return serviceName; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getFailedAt() { return failedAt; }
}
