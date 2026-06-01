package com.paathai.rag.retrieval;

import com.paathai.ai.client.GeminiEmbeddingClient;
import com.paathai.common.entity.TranscriptChunk;
import com.paathai.common.repository.TranscriptChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Retrieval source that queries transcript chunks by vector similarity.
 * Priority 3 (after syllabus and notes).
 */
@Component
public class TranscriptRetrievalSource implements RetrievalSource {

    private static final Logger log = LoggerFactory.getLogger(TranscriptRetrievalSource.class);

    private final TranscriptChunkRepository chunkRepository;
    private final GeminiEmbeddingClient embeddingClient;

    public TranscriptRetrievalSource(TranscriptChunkRepository chunkRepository,
                                      GeminiEmbeddingClient embeddingClient) {
        this.chunkRepository = chunkRepository;
        this.embeddingClient = embeddingClient;
    }

    @Override
    public int priority() {
        return 3; // Transcript chunks
    }

    @Override
    public boolean supports(RetrievalQuery query) {
        return true; // Always available
    }

    @Override
    public List<RetrievalResult> retrieve(RetrievalQuery query, int maxResults) {
        log.debug("Retrieving transcript chunks for query: {}", query.queryText());

        // Generate embedding for the query
        float[] queryEmbedding = embeddingClient.embed(query.queryText());
        String vectorString = GeminiEmbeddingClient.toVectorString(queryEmbedding);

        // Find similar chunks
        List<TranscriptChunk> chunks = chunkRepository.findSimilarChunks(vectorString, maxResults);

        return chunks.stream()
                .map(chunk -> new RetrievalResult(
                    chunk.getContent(),
                    "Transcript",
                    1.0, // Similarity score (pgvector returns ordered results)
                    chunk.getEstimatedTokens() != null ? chunk.getEstimatedTokens() : 0,
                    Map.of(
                        "chunk_id", chunk.getId(),
                        "transcript_id", chunk.getTranscriptId(),
                        "chunk_index", chunk.getChunkIndex()
                    )
                ))
                .toList();
    }

    @Override
    public String sourceName() {
        return "Transcript";
    }
}

