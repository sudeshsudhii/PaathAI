package com.paathai.rag.chunking;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for text chunking.
 * Implementations: FixedSizeChunker, SemanticChunker, TranscriptChunker.
 */
public interface ChunkingStrategy {

    /**
     * Split text into chunks according to this strategy.
     *
     * @param text   The text to chunk
     * @param config Chunking configuration (max tokens, overlap, etc.)
     * @return List of text chunks with metadata
     */
    List<TextChunk> chunk(String text, ChunkingConfig config);

    /**
     * Represents a single chunk of text with metadata.
     */
    record TextChunk(
        String content,
        int startOffset,
        int endOffset,
        int estimatedTokens,
        Map<String, Object> metadata
    ) {}

    /**
     * Configuration for chunking behavior.
     */
    record ChunkingConfig(
        int maxTokensPerChunk,
        int overlapTokens,
        boolean respectSentenceBoundaries
    ) {
        public static ChunkingConfig defaults() {
            return new ChunkingConfig(500, 50, true);
        }
    }
}
