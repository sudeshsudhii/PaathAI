package com.paathai.common.exception;

/**
 * Thrown when an LLM request exceeds the hard token limit for its feature type.
 */
public class TokenBudgetExceededException extends RuntimeException {

    private final String featureType;
    private final int requestedTokens;
    private final int maxTokens;

    public TokenBudgetExceededException(String featureType, int requestedTokens, int maxTokens) {
        super(String.format("Token budget exceeded for %s: requested %d, max %d",
                featureType, requestedTokens, maxTokens));
        this.featureType = featureType;
        this.requestedTokens = requestedTokens;
        this.maxTokens = maxTokens;
    }

    public String getFeatureType() { return featureType; }
    public int getRequestedTokens() { return requestedTokens; }
    public int getMaxTokens() { return maxTokens; }
}
