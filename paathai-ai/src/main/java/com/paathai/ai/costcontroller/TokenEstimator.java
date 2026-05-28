package com.paathai.ai.costcontroller;

import org.springframework.stereotype.Component;

/**
 * Estimates token count for a given text using a heuristic.
 * MVP uses character-based estimation; can be replaced with
 * a proper tokenizer (e.g., Tiktoken-compatible) later.
 */
@Component
public class TokenEstimator {

    /**
     * Heuristic: ~4 characters per token for English text.
     * This is a reasonable approximation for Gemini models.
     */
    private static final double CHARS_PER_TOKEN = 4.0;

    /**
     * Estimate token count for the given text.
     *
     * @param text The text to estimate
     * @return Estimated token count
     */
    public int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }
}
