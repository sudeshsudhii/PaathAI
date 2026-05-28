package com.paathai.ai.costcontroller;

import com.paathai.common.dto.FeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Routes LLM requests to the appropriate model based on feature type and complexity.
 *
 * Routing strategy:
 * - Low/Medium complexity → Gemini Flash
 * - High complexity → Gemini Pro
 * - Fallback → Ollama (if available)
 */
@Component
public class ModelRouter {

    private static final Logger log = LoggerFactory.getLogger(ModelRouter.class);

    private final List<LlmClient> availableClients;

    public ModelRouter(List<LlmClient> availableClients) {
        this.availableClients = availableClients;
    }

    /**
     * Route a request to the most appropriate LLM client.
     *
     * @param featureType    The feature requesting the call
     * @param estimatedTokens Estimated input tokens
     * @return The selected LLM client
     */
    public LlmClient route(FeatureType featureType, int estimatedTokens) {
        String preferredModel = featureType.getDefaultModel();

        return availableClients.stream()
                .filter(client -> client.getModelName().contains(preferredModel.toLowerCase().replace(" ", "")))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Preferred model '{}' not available, using first available", preferredModel);
                    return availableClients.get(0);
                });
    }
}
