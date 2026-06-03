package com.paathai.monitoring.service;

import com.paathai.common.event.*;
import com.paathai.monitoring.model.LivePipelineMetric;
import com.paathai.monitoring.repository.LivePipelineMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Observability service for live lecture pipelines.
 *
 * <p>Listens to key pipeline events, calculates the latency from event timestamp
 * to receipt time, and persists {@link LivePipelineMetric} records.</p>
 */
@Service
public class LiveSessionMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(LiveSessionMonitoringService.class);

    private final LivePipelineMetricRepository metricRepository;

    public LiveSessionMonitoringService(LivePipelineMetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    @EventListener
    @Async("generalEventExecutor")
    public void onTranscriptChunkGenerated(LiveTranscriptChunkGenerated event) {
        recordMetric(event.getSessionId(), "TRANSCRIPTION", event.getSequenceNumber(),
                event.getTimestamp(), true, null);
    }

    @EventListener
    @Async("generalEventExecutor")
    public void onTopicDetected(LiveTopicDetected event) {
        recordMetric(event.getSessionId(), "TOPIC_DETECTION", null,
                event.getTimestamp(), true, null);
    }

    @EventListener
    @Async("generalEventExecutor")
    public void onNotesUpdated(LiveNotesUpdated event) {
        recordMetric(event.getSessionId(), "NOTES_GENERATION", null,
                event.getTimestamp(), true, null);
    }

    @EventListener
    @Async("generalEventExecutor")
    public void onSyllabusMapped(LiveSyllabusMapped event) {
        recordMetric(event.getSessionId(), "SYLLABUS_MAPPING", null,
                event.getTimestamp(), true, null);
    }

    private void recordMetric(Long sessionId, String stage, Integer chunkSeq,
                               Instant eventTimestamp, boolean success, String errorMessage) {
        try {
            long latencyMs = Duration.between(eventTimestamp, Instant.now()).toMillis();

            LivePipelineMetric metric = new LivePipelineMetric();
            metric.setSessionId(sessionId);
            metric.setPipelineStage(stage);
            metric.setChunkSeq(chunkSeq);
            metric.setLatencyMs(latencyMs);
            metric.setSuccess(success);
            metric.setErrorMessage(errorMessage);

            metricRepository.save(metric);

            if (latencyMs > 5000) {
                log.warn("High latency detected: session={} stage={} latency={}ms", sessionId, stage, latencyMs);
            } else {
                log.debug("Metric recorded: session={} stage={} latency={}ms", sessionId, stage, latencyMs);
            }
        } catch (Exception e) {
            log.error("Failed to record pipeline metric: {}", e.getMessage());
        }
    }
}
