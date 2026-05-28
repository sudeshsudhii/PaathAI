package com.paathai.ai.costcontroller;

import org.springframework.stereotype.Component;

/**
 * Compresses context to fit within token budgets.
 * Strategies: truncation, summarization, key-sentence extraction.
 */
@Component
public class ContextCompressor {

    /**
     * Compress the given text to fit within the target token count.
     * MVP strategy: simple truncation at sentence boundaries.
     *
     * @param text      The text to compress
     * @param maxTokens Target maximum token count
     * @return Compressed text
     */
    public String compress(String text, int maxTokens) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Heuristic: ~4 chars per token
        int maxChars = maxTokens * 4;

        if (text.length() <= maxChars) {
            return text;
        }

        // Truncate at the last sentence boundary before maxChars
        String truncated = text.substring(0, maxChars);
        int lastPeriod = truncated.lastIndexOf('.');
        if (lastPeriod > maxChars / 2) {
            return truncated.substring(0, lastPeriod + 1);
        }

        return truncated;
    }
}
