package com.paathai.lecture.service;

import com.paathai.common.entity.LiveSession;
import com.paathai.common.event.LiveRecordingStarted;
import com.paathai.common.event.LiveRecordingStopped;
import com.paathai.common.repository.LiveSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class LiveSessionService {

    private static final Logger log = LoggerFactory.getLogger(LiveSessionService.class);

    private final LiveSessionRepository liveSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public LiveSessionService(LiveSessionRepository liveSessionRepository, ApplicationEventPublisher eventPublisher) {
        this.liveSessionRepository = liveSessionRepository;
        this.eventPublisher = eventPublisher;
    }

    public Long startSession(Long userId, String title, Long courseId) {
        LiveSession session = new LiveSession();
        session.setUserId(userId);
        session.setTitle(title);
        session.setCourseId(courseId);
        session.setStatus("RECORDING");
        session.setStartedAt(LocalDateTime.now());
        
        session = liveSessionRepository.save(session);
        log.info("Live session started: {}", session.getId());

        eventPublisher.publishEvent(new LiveRecordingStarted(
                session.getId(), userId, courseId, title));

        return session.getId();
    }

    public void pauseSession(Long sessionId) {
        liveSessionRepository.findById(sessionId).ifPresent(session -> {
            if ("RECORDING".equals(session.getStatus())) {
                session.setStatus("PAUSED");
                session.setPausedAt(LocalDateTime.now());
                liveSessionRepository.save(session);
                log.info("Live session paused: {}", sessionId);
            }
        });
    }

    public void resumeSession(Long sessionId) {
        liveSessionRepository.findById(sessionId).ifPresent(session -> {
            if ("PAUSED".equals(session.getStatus())) {
                session.setStatus("RECORDING");
                
                // Accumulate duration before clearing pausedAt
                if (session.getPausedAt() != null) {
                    long currentDuration = session.getTotalDurationMs() != null ? session.getTotalDurationMs() : 0;
                    long activeDuration = Duration.between(session.getStartedAt(), session.getPausedAt()).toMillis();
                    session.setTotalDurationMs(currentDuration + activeDuration);
                }
                
                // Reset startedAt to now for the next active segment
                session.setStartedAt(LocalDateTime.now());
                session.setPausedAt(null);
                
                liveSessionRepository.save(session);
                log.info("Live session resumed: {}", sessionId);
            }
        });
    }

    public void stopSession(Long sessionId) {
        liveSessionRepository.findById(sessionId).ifPresent(session -> {
            if (!"COMPLETED".equals(session.getStatus()) && !"FAILED".equals(session.getStatus())) {
                session.setStatus("COMPLETED");
                session.setEndedAt(LocalDateTime.now());
                
                long currentDuration = session.getTotalDurationMs() != null ? session.getTotalDurationMs() : 0;
                long activeDuration = 0;
                
                if ("RECORDING".equals(session.getStatus()) || session.getPausedAt() == null) {
                     activeDuration = Duration.between(session.getStartedAt(), session.getEndedAt()).toMillis();
                } else if (session.getPausedAt() != null) {
                     // Was paused, already accumulated before pause
                     activeDuration = Duration.between(session.getStartedAt(), session.getPausedAt()).toMillis();
                }
                
                long totalDuration = currentDuration + activeDuration;
                session.setTotalDurationMs(totalDuration);
                liveSessionRepository.save(session);
                log.info("Live session completed: {}", sessionId);

                eventPublisher.publishEvent(new LiveRecordingStopped(
                        session.getId(), session.getUserId(), totalDuration));
            }
        });
    }
}
