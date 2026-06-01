package com.paathai.rag.embedding;

import com.paathai.ai.client.GeminiEmbeddingClient;
import com.paathai.common.entity.TranscriptChunk;
import com.paathai.common.repository.TranscriptChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * PostgreSQL + pgvector implementation of EmbeddingStore.
 * Stores and retrieves vector embeddings using the transcript_chunks table.
 */
@Component
public class PgVectorEmbeddingStore implements EmbeddingStore {

    private static final Logger log = LoggerFactory.getLogger(PgVectorEmbeddingStore.class);

    private final TranscriptChunkRepository chunkRepository;

    public PgVectorEmbeddingStore(TranscriptChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    @Override
    public void store(String id, String content, float[] embedding, Map<String, Object> metadata) {
        // This is handled directly by the EmbeddingPipelineService
        // which creates TranscriptChunk entities with embeddings
        log.debug("Store called for id={}, content length={}", id, content.length());
    }

    @Override
    public List<EmbeddingSearchResult> search(float[] queryEmbedding, int maxResults, double minSimilarity) {
        String vectorString = GeminiEmbeddingClient.toVectorString(queryEmbedding);

        List<TranscriptChunk> chunks = chunkRepository.findSimilarChunks(vectorString, maxResults);

        return chunks.stream()
                .map(chunk -> new EmbeddingSearchResult(
                    chunk.getId().toString(),
                    chunk.getContent(),
                    1.0, // Cosine distance doesn't directly give similarity, but ordering is correct
                    Map.of(
                        "transcript_id", chunk.getTranscriptId(),
                        "chunk_index", chunk.getChunkIndex()
                    )
                ))
                .toList();
    }
}

