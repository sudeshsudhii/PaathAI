package com.paathai.ai.costcontroller;

import com.paathai.common.dto.FeatureType;

/**
 * Interface for the LLM client abstraction layer.
 * Implementations: GeminiLlmClient, OllamaLlmClient.
 *
 * This interface prevents vendor lock-in — any LLM provider
 * can be swapped by implementing this interface.
 */
public interface LlmClient {

    /**
     * Send a prompt to the LLM and receive a response.
     *
     * @param prompt       The fully assembled prompt (system + context + user query)
     * @param featureType  The feature requesting the LLM call (for model routing)
     * @return The LLM response text
     */
    LlmResponse execute(String prompt, FeatureType featureType);

    /**
     * Estimate the token count for a given text without making an API call.
     *
     * @param text The text to estimate tokens for
     * @return Estimated token count
     */
    int estimateTokens(String text);

    /**
     * Get the model name this client uses.
     */
    String getModelName();
}
