package com.paathai.common.dto;

/**
 * Enumerates the AI feature types, each with a hard token limit.
 * Used by AICostController, TokenEstimator, and ContextAssembler.
 */
public enum FeatureType {

    TOPIC_DETECTION(1500, "Gemini Flash"),
    SEARCH(2000, "Gemini Flash"),
    NOTES_GENERATION(6000, "Gemini Pro"),
    FLASHCARD_GENERATION(2000, "Gemini Flash"),
    QUIZ_GENERATION(3000, "Gemini Flash"),
    TUTOR(4000, "Gemini Pro"),
    SYLLABUS_PARSING(2000, "Gemini Flash"),
    LIVE_TOPIC_DETECTION(1500, "Gemini Flash"),
    LIVE_NOTES_UPDATE(1500, "Gemini Flash"),
    ACADEMIC_INTELLIGENCE(2000, "Gemini Flash");

    private final int maxTokens;
    private final String defaultModel;

    FeatureType(int maxTokens, String defaultModel) {
        this.maxTokens = maxTokens;
        this.defaultModel = defaultModel;
    }

    public int getMaxTokens() { return maxTokens; }
    public String getDefaultModel() { return defaultModel; }
}
