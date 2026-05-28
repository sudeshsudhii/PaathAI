package com.paathai.rag.retrieval;

import java.util.List;
import java.util.Map;

/**
 * Interface for retrieval sources in the RAG pipeline.
 * Each source represents a different data layer (syllabus, notes, transcript, etc.)
 * and is consulted in priority order during context assembly.
 *
 * Implementations must be stateless and thread-safe.
 */
public interface RetrievalSource {

    /**
     * Priority of this source (lower = higher priority).
     * Syllabus = 1, Notes = 2, Transcript = 3, etc.
     */
    int priority();

    /**
     * Whether this source can handle the given query.
     * Sources may opt out based on query type or feature.
     */
    boolean supports(RetrievalQuery query);

    /**
     * Retrieve relevant content from this source.
     *
     * @param query      The retrieval query
     * @param maxResults Maximum number of results
     * @return List of retrieval results
     */
    List<RetrievalResult> retrieve(RetrievalQuery query, int maxResults);

    /**
     * Human-readable source name for attribution.
     */
    String sourceName();

    /**
     * Represents a retrieval query with context.
     */
    record RetrievalQuery(
        String queryText,
        Long courseId,
        Long studentId,
        String featureType
    ) {}

    /**
     * Represents a single retrieval result.
     */
    record RetrievalResult(
        String content,
        String sourceName,
        double relevanceScore,
        int estimatedTokens,
        Map<String, Object> metadata
    ) {}
}
