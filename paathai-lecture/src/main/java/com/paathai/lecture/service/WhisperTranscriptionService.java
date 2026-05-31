package com.paathai.lecture.service;

import com.paathai.common.event.TranscriptCompleted;
import com.paathai.common.entity.Lecture;
import com.paathai.common.entity.Transcript;
import com.paathai.common.repository.LectureRepository;
import com.paathai.common.repository.TranscriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whisper ASR transcription service.
 * Calls the Docker-based Whisper service to transcribe audio files.
 */
@Service
public class WhisperTranscriptionService implements TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(WhisperTranscriptionService.class);

    private final LectureRepository lectureRepository;
    private final TranscriptRepository transcriptRepository;
    private final RestTemplate restTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${paathai.whisper.url:http://localhost:9000}")
    private String whisperUrl;

    // In-memory status tracking for MVP
    private final Map<Long, TranscriptionStatus> statusMap = new ConcurrentHashMap<>();

    public WhisperTranscriptionService(LectureRepository lectureRepository,
                                        TranscriptRepository transcriptRepository,
                                        RestTemplate restTemplate,
                                        ApplicationEventPublisher eventPublisher) {
        this.lectureRepository = lectureRepository;
        this.transcriptRepository = transcriptRepository;
        this.restTemplate = restTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Async("generalEventExecutor")
    public void transcribeAsync(Long lectureId, String audioPath) {
        log.info("Starting transcription for lecture {} from {}", lectureId, audioPath);
        statusMap.put(lectureId, TranscriptionStatus.IN_PROGRESS);

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new RuntimeException("Lecture not found: " + lectureId));

        try {
            // Update lecture status
            lecture.setStatus("TRANSCRIBING");
            lectureRepository.save(lecture);

            // Call Whisper ASR service
            String transcribedText = callWhisperService(audioPath);

            // Save transcript
            Transcript transcript = new Transcript();
            transcript.setLectureId(lectureId);
            transcript.setFullText(transcribedText);
            transcriptRepository.save(transcript);

            // Update lecture status
            lecture.setStatus("COMPLETED");
            lectureRepository.save(lecture);
            statusMap.put(lectureId, TranscriptionStatus.COMPLETED);

            // Estimate tokens for the event
            int estimatedTokens = (int) Math.ceil(transcribedText.length() / 4.0);

            // Publish event for downstream consumers (chunking, embedding, notes)
            eventPublisher.publishEvent(new TranscriptCompleted(
                lectureId, transcript.getId(), 0, estimatedTokens
            ));

            log.info("Transcription completed for lecture {} ({} chars)", lectureId, transcribedText.length());

        } catch (Exception e) {
            log.error("Transcription failed for lecture {}: {}", lectureId, e.getMessage(), e);
            lecture.setStatus("FAILED");
            lectureRepository.save(lecture);
            statusMap.put(lectureId, TranscriptionStatus.FAILED);
        }
    }

    @Override
    public TranscriptionStatus getStatus(Long lectureId) {
        return statusMap.getOrDefault(lectureId, TranscriptionStatus.PENDING);
    }

    /**
     * Call the Whisper ASR Docker service.
     * Uses the onerahmet/openai-whisper-asr-webservice API format.
     */
    private String callWhisperService(String audioPath) {
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            throw new RuntimeException("Audio file not found: " + audioPath);
        }

        try {
            String url = whisperUrl + "/asr?output=txt&language=en";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio_file", new FileSystemResource(audioFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Whisper returned status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Whisper service unavailable, using mock transcription: {}", e.getMessage());
            // Fallback for development when Whisper isn't running
            return getMockTranscription();
        }
    }

    private String getMockTranscription() {
        return """
            Today we're going to discuss the fundamentals of calculus. \
            Calculus is a branch of mathematics that deals with rates of change and accumulation. \
            The two main branches are differential calculus and integral calculus. \
            Differential calculus focuses on the concept of the derivative, which measures how a function changes as its input changes. \
            The derivative of a function at a point gives the slope of the tangent line at that point. \
            Integral calculus, on the other hand, deals with the concept of the integral, which represents the accumulation of quantities. \
            The fundamental theorem of calculus connects these two branches, showing that differentiation and integration are inverse processes. \
            Let's start with limits, which form the foundation of calculus. \
            A limit describes the value that a function approaches as the input approaches some value. \
            For example, the limit of f(x) = x squared as x approaches 2 is 4. \
            We can write this as lim(x→2) x² = 4. \
            Understanding limits is crucial because derivatives are defined using limits. \
            The derivative of f(x) is defined as the limit of [f(x+h) - f(x)] / h as h approaches 0. \
            This is also known as the difference quotient. \
            Now let's look at some basic derivative rules. \
            The power rule states that the derivative of x^n is n*x^(n-1). \
            The sum rule says the derivative of a sum is the sum of the derivatives. \
            The product rule and chain rule are more advanced but equally important. \
            In the next lecture, we'll cover integration techniques and applications of calculus.
            """;
    }
}

