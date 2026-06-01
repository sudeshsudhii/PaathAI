package com.paathai.rag.chunking;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sentence-boundary-aware text chunker.
 * Splits text at sentence boundaries (. ! ?) respecting max token limits
 * with configurable overlap for context continuity.
 */
@Component
public class SentenceChunker implements ChunkingStrategy {

    private static final double CHARS_PER_TOKEN = 4.0;

    @Override
    public List<TextChunk> chunk(String text, ChunkingConfig config) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<TextChunk> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");

        int maxCharsPerChunk = (int) (config.maxTokensPerChunk() * CHARS_PER_TOKEN);
        StringBuilder currentChunk = new StringBuilder();
        int currentStartOffset = 0;
        int currentOffset = 0;

        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.isEmpty()) continue;

            // If adding this sentence would exceed the limit, finalize current chunk
            if (currentChunk.length() > 0 &&
                (currentChunk.length() + trimmedSentence.length() + 1) > maxCharsPerChunk) {

                String chunkText = currentChunk.toString().trim();
                int estimatedTokens = (int) Math.ceil(chunkText.length() / CHARS_PER_TOKEN);

                chunks.add(new TextChunk(
                    chunkText,
                    currentStartOffset,
                    currentOffset,
                    estimatedTokens,
                    Map.of("chunk_index", chunks.size())
                ));

                // Start new chunk (with overlap if configured)
                if (config.overlapTokens() > 0) {
                    int overlapChars = (int) (config.overlapTokens() * CHARS_PER_TOKEN);
                    String overlapText = chunkText.length() > overlapChars
                        ? chunkText.substring(chunkText.length() - overlapChars)
                        : chunkText;
                    currentChunk = new StringBuilder(overlapText);
                } else {
                    currentChunk = new StringBuilder();
                }
                currentStartOffset = currentOffset;
            }

            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(trimmedSentence);
            currentOffset += trimmedSentence.length() + 1;
        }

        // Don't forget the last chunk
        if (currentChunk.length() > 0) {
            String chunkText = currentChunk.toString().trim();
            int estimatedTokens = (int) Math.ceil(chunkText.length() / CHARS_PER_TOKEN);

            chunks.add(new TextChunk(
                chunkText,
                currentStartOffset,
                currentOffset,
                estimatedTokens,
                Map.of("chunk_index", chunks.size())
            ));
        }

        return chunks;
    }
}
