package com.paathai.rag.context;

import com.paathai.common.dto.FeatureType;

/**
 * Token budget for context assembly.
 * Each feature type has a maximum token limit for the assembled context.
 */
public record ContextBudget(
    FeatureType featureType,
    int maxTokens
) {
    /**
     * Create a budget from the feature type's default max tokens.
     */
    public static ContextBudget forFeature(FeatureType featureType) {
        return new ContextBudget(featureType, featureType.getMaxTokens());
    }
}
