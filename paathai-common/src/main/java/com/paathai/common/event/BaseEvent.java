package com.paathai.common.event;

import java.time.Instant;

/**
 * Base class for all PaathAI domain events.
 * Provides common metadata: event ID, timestamp, and source module.
 */
public abstract class BaseEvent {

    private final String eventId;
    private final Instant timestamp;
    private final String sourceModule;

    protected BaseEvent(String sourceModule) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.sourceModule = sourceModule;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSourceModule() {
        return sourceModule;
    }
}
