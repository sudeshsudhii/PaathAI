package com.paathai.ai.costcontroller;

/**
 * Represents a response from the LLM, including metadata for cost tracking.
 */
public record LlmResponse(
    String content,
    int inputTokens,
    int outputTokens,
    String modelUsed,
    long latencyMs
) {
    public int totalTokens() {
        return inputTokens + outputTokens;
    }
}
