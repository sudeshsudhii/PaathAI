package com.paathai.lecture.service;

import com.paathai.common.entity.Lecture;
import com.paathai.common.entity.LiveSession;
import com.paathai.common.entity.LiveTranscriptChunk;
import com.paathai.common.entity.Transcript;
import com.paathai.common.event.LiveRecordingStopped;
import com.paathai.common.event.TranscriptCompleted;
import com.paathai.common.repository.LectureRepository;
import com.paathai.common.repository.LiveSessionRepository;
import com.paathai.common.repository.LiveTranscriptChunkRepository;
import com.paathai.common.repository.TranscriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SessionFinalizationService {

    private static final Logger log = LoggerFactory.getLogger(SessionFinalizationService.class);

    private final LiveSessionRepository liveSessionRepository;
    private final LiveTranscriptChunkRepository liveTranscriptChunkRepository;
    private final LectureRepository lectureRepository;
    private final TranscriptRepository transcriptRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SessionFinalizationService(LiveSessionRepository liveSessionRepository,
                                      LiveTranscriptChunkRepository liveTranscriptChunkRepository,
                                      LectureRepository lectureRepository,
                                      TranscriptRepository transcriptRepository,
                                      ApplicationEventPublisher eventPublisher) {
        this.liveSessionRepository = liveSessionRepository;
        this.liveTranscriptChunkRepository = liveTranscriptChunkRepository;
        this.lectureRepository = lectureRepository;
        this.transcriptRepository = transcriptRepository;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    @Async("generalEventExecutor")
    public void onLiveRecordingStopped(LiveRecordingStopped event) {
        Long sessionId = event.getSessionId();
        log.info("Finalizing live session {}", sessionId);

        try {
            LiveSession session = liveSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Live session not found: " + sessionId));

            // Promote all chunks to FINAL status
            int promoted = liveTranscriptChunkRepository.finalizeAllChunks(sessionId);
            log.info("Promoted {} transcript chunks to FINAL for session {}", promoted, sessionId);

            List<LiveTranscriptChunk> chunks = liveTranscriptChunkRepository
                    .findBySessionIdOrderBySequenceNumber(sessionId);

            String fullText = chunks.stream()
                    .map(LiveTranscriptChunk::getContent)
                    .collect(Collectors.joining(" "));

            // Create Lecture record
            Lecture lecture = new Lecture();
            lecture.setCourseId(session.getCourseId() != null ? session.getCourseId() : 1L);
            lecture.setUploadedBy(session.getUserId());
            lecture.setTitle(session.getTitle() != null ? session.getTitle() : "Live Session " + sessionId);
            lecture.setAudioPath("live://" + sessionId); // Virtual path
            lecture.setDurationMs(session.getTotalDurationMs());
            lecture.setStatus("COMPLETED");
            
            lecture = lectureRepository.save(lecture);
            
            // Link session to lecture
            session.setLectureId(lecture.getId());
            liveSessionRepository.save(session);

            // Create Transcript record
            Transcript transcript = new Transcript();
            transcript.setLectureId(lecture.getId());
            transcript.setFullText(fullText);
            
            transcript = transcriptRepository.save(transcript);

            // Estimate tokens
            int estimatedTokens = (int) Math.ceil(fullText.length() / 4.0);

            // Trigger batch pipeline (embedding, final notes, search index)
            eventPublisher.publishEvent(new TranscriptCompleted(
                    lecture.getId(), transcript.getId(), 0, estimatedTokens
            ));

            log.info("Live session {} finalized and linked to lecture {}", sessionId, lecture.getId());

        } catch (Exception e) {
            log.error("Failed to finalize live session {}", sessionId, e);
        }
    }
}
