package com.paathai.common.dto;

/**
 * Enumerates the prompt types used by the PromptRegistry.
 * Each type maps to a directory under /prompts/.
 */
public enum PromptType {

    TOPIC_DETECTION("topic_detection"),
    NOTES_GENERATION("notes_generation"),
    SEARCH("search"),
    FLASHCARD_GENERATION("flashcard_generation"),
    QUIZ_GENERATION("quiz_generation"),
    TUTOR("tutor"),
    ROADMAP("roadmap"),
    COVERAGE("coverage"),
    SYLLABUS_PARSING("syllabus_parsing"),
    LIVE_TOPIC_DETECTION("live_topic_detection"),
    LIVE_NOTES_UPDATE("live_notes_update");

    private final String directoryName;

    PromptType(String directoryName) {
        this.directoryName = directoryName;
    }

    public String getDirectoryName() { return directoryName; }
}
