package com.paathai.study.notes;

import com.paathai.ai.costcontroller.AICostController;
import com.paathai.ai.costcontroller.LlmResponse;
import com.paathai.common.dto.FeatureType;
import com.paathai.common.event.TranscriptCompleted;
import com.paathai.common.entity.Notes;
import com.paathai.common.entity.Transcript;
import com.paathai.common.repository.NotesRepository;
import com.paathai.common.repository.TranscriptRepository;
import com.paathai.common.repository.LectureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Generates AI-powered study notes from lecture transcripts.
 * Listens for TranscriptCompleted events and triggers notes generation.
 * Uses AICostController for all LLM calls to enforce budgets.
 */
@Service
public class NotesGenerationServiceImpl implements NotesGenerationService {

    private static final Logger log = LoggerFactory.getLogger(NotesGenerationServiceImpl.class);

    private final TranscriptRepository transcriptRepository;
    private final NotesRepository notesRepository;
    private final LectureRepository lectureRepository;
    private final AICostController aiCostController;

    public NotesGenerationServiceImpl(TranscriptRepository transcriptRepository,
                                       NotesRepository notesRepository,
                                       LectureRepository lectureRepository,
                                       AICostController aiCostController) {
        this.transcriptRepository = transcriptRepository;
        this.notesRepository = notesRepository;
        this.lectureRepository = lectureRepository;
        this.aiCostController = aiCostController;
    }

    @EventListener
    @Async("notesGenerationExecutor")
    public void onTranscriptCompleted(TranscriptCompleted event) {
        log.info("Notes generation triggered for lecture {}", event.getLectureId());
        try {
            generateNotes(event.getLectureId());
        } catch (Exception e) {
            log.error("Notes generation failed for lecture {}: {}",
                      event.getLectureId(), e.getMessage(), e);
        }
    }

    @Override
    public Long generateNotes(Long lectureId) {
        log.info("Generating notes for lecture {}", lectureId);

        Transcript transcript = transcriptRepository.findByLectureId(lectureId)
                .orElseThrow(() -> new RuntimeException("No transcript found for lecture: " + lectureId));

        // Get the student ID from the lecture
        Long studentId = lectureRepository.findById(lectureId)
                .map(l -> l.getUploadedBy())
                .orElse(1L);

        String prompt = buildNotesPrompt(transcript.getFullText());

        // Call LLM through cost controller
        LlmResponse response = aiCostController.execute(prompt, FeatureType.NOTES_GENERATION, studentId);

        // Save generated notes
        Notes notes = new Notes();
        notes.setLectureId(lectureId);
        notes.setContent(response.content());
        notes.setFormat("MARKDOWN");
        notes.setEstimatedTokens(response.totalTokens());
        notesRepository.save(notes);

        log.info("Notes generated for lecture {} ({} tokens)", lectureId, response.totalTokens());
        return notes.getId();
    }

    private String buildNotesPrompt(String transcriptText) {
        return String.format("""
            You are an expert academic note-taker. Generate comprehensive, well-structured \
            study notes from the following lecture transcript. \
            
            Format the notes in Markdown with:
            - A clear **title** based on the lecture topic
            - An **overview** section (2-3 sentences summarizing the lecture)
            - **Key Concepts** with definitions and explanations
            - **Important Details** organized by topic
            - **Key Formulas or Rules** (if applicable)
            - A **Summary** section with bullet points
            - **Review Questions** (3-5 questions to test understanding)
            
            Make the notes student-friendly, clear, and exam-ready.
            
            === LECTURE TRANSCRIPT ===
            %s
            """, transcriptText);
    }
}

