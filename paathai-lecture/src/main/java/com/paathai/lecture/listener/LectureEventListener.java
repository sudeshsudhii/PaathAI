package com.paathai.lecture.listener;

import com.paathai.common.event.LectureCreated;
import com.paathai.lecture.service.TranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for LectureCreated events and triggers transcription.
 */
@Component
public class LectureEventListener {

    private static final Logger log = LoggerFactory.getLogger(LectureEventListener.class);

    private final TranscriptionService transcriptionService;

    public LectureEventListener(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @EventListener
    @Async("generalEventExecutor")
    public void onLectureCreated(LectureCreated event) {
        log.info("Received LectureCreated event for lecture {}, triggering transcription",
                 event.getLectureId());
        transcriptionService.transcribeAsync(event.getLectureId(), event.getAudioPath());
    }
}
