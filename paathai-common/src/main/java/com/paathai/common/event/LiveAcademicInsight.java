package com.paathai.common.event;

import java.util.List;
import java.util.Map;

/**
 * Published when academic intelligence is generated for a live topic.
 * Consumed by: WebSocket push to client.
 */
public class LiveAcademicInsight extends BaseEvent {

    private final Long sessionId;
    private final String currentTopic;
    private final String currentSubtopic;
    private final List<String> keyConcepts;
    private final List<Map<String, String>> definitions; // [{term, definition}]
    private final List<String> prerequisites;
    private final String examImportance; // HIGH, MEDIUM, LOW
    private final String coverageStatus; // COVERED, IN_PROGRESS, UPCOMING

    public LiveAcademicInsight(Long sessionId, String currentTopic, String currentSubtopic,
                                List<String> keyConcepts, List<Map<String, String>> definitions,
                                List<String> prerequisites, String examImportance,
                                String coverageStatus) {
        super("AI");
        this.sessionId = sessionId;
        this.currentTopic = currentTopic;
        this.currentSubtopic = currentSubtopic;
        this.keyConcepts = keyConcepts;
        this.definitions = definitions;
        this.prerequisites = prerequisites;
        this.examImportance = examImportance;
        this.coverageStatus = coverageStatus;
    }

    public Long getSessionId() { return sessionId; }
    public String getCurrentTopic() { return currentTopic; }
    public String getCurrentSubtopic() { return currentSubtopic; }
    public List<String> getKeyConcepts() { return keyConcepts; }
    public List<Map<String, String>> getDefinitions() { return definitions; }
    public List<String> getPrerequisites() { return prerequisites; }
    public String getExamImportance() { return examImportance; }
    public String getCoverageStatus() { return coverageStatus; }
}
