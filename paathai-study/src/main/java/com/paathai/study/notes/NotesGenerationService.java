package com.paathai.study.notes;

/**
 * Service for generating AI-powered study notes from lecture transcripts.
 * Uses the AICostController pipeline for all LLM calls.
 */
public interface NotesGenerationService {

    /**
     * Generate notes for a lecture using its transcript and syllabus context.
     *
     * @param lectureId The lecture to generate notes for
     * @return The generated notes ID
     */
    Long generateNotes(Long lectureId);
}
