package com.paathai.rag.embedding;

import java.util.List;

/**
 * Interface for storing and retrieving vector embeddings.
 * MVP implementation: PostgreSQL + pgvector.
 * Future: can be swapped to Qdrant, Weaviate, etc.
 */
public interface EmbeddingStore {

    /**
     * Store an embedding vector with its associated content and metadata.
     *
     * @param id        Unique identifier for the content
     * @param content   The original text content
     * @param embedding The embedding vector
     * @param metadata  Additional metadata (source type, lecture ID, etc.)
     */
    void store(String id, String content, float[] embedding, java.util.Map<String, Object> metadata);

    /**
     * Search for the most similar embeddings to the given query vector.
     *
     * @param queryEmbedding The query embedding vector
     * @param maxResults     Maximum number of results to return
     * @param minSimilarity  Minimum cosine similarity threshold
     * @return List of search results ordered by similarity
     */
    List<EmbeddingSearchResult> search(float[] queryEmbedding, int maxResults, double minSimilarity);

    /**
     * Represents a single embedding search result.
     */
    record EmbeddingSearchResult(
        String id,
        String content,
        double similarity,
        java.util.Map<String, Object> metadata
    ) {}
}
