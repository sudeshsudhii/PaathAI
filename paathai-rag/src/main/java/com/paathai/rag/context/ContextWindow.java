package com.paathai.rag.context;

import com.paathai.rag.retrieval.RetrievalSource.RetrievalResult;

import java.util.List;

/**
 * Assembled context window ready for LLM consumption.
 * Contains the retrieved content and metadata about the assembly process.
 */
public record ContextWindow(
    List<RetrievalResult> results,
    int totalTokensUsed,
    int sourcesConsulted,
    List<String> sourcesUsed
) {
    /**
     * Concatenate all retrieval results into a single context string.
     */
    public String toContextString() {
        StringBuilder sb = new StringBuilder();
        for (RetrievalResult result : results) {
            sb.append("--- Source: ").append(result.sourceName()).append(" ---\n");
            sb.append(result.content()).append("\n\n");
        }
        return sb.toString().trim();
    }

    /**
     * Check if the context used any external (non-course-specific) knowledge.
     */
    public boolean usesExternalKnowledge() {
        return sourcesUsed.contains("GENERAL_LLM");
    }
}
