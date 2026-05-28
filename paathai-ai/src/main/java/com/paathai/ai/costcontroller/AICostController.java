package com.paathai.ai.costcontroller;

import com.paathai.common.dto.FeatureType;
import com.paathai.common.exception.TokenBudgetExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Central gateway for all LLM requests.
 * Every AI feature MUST go through this controller.
 *
 * Pipeline: Estimate Tokens → Check Budget → Compress → Route → Execute → Log
 */
@Service
public class AICostController {

    private static final Logger log = LoggerFactory.getLogger(AICostController.class);

    private final TokenEstimator tokenEstimator;
    private final BudgetEnforcer budgetEnforcer;
    private final ContextCompressor contextCompressor;
    private final ModelRouter modelRouter;

    public AICostController(TokenEstimator tokenEstimator,
                            BudgetEnforcer budgetEnforcer,
                            ContextCompressor contextCompressor,
                            ModelRouter modelRouter) {
        this.tokenEstimator = tokenEstimator;
        this.budgetEnforcer = budgetEnforcer;
        this.contextCompressor = contextCompressor;
        this.modelRouter = modelRouter;
    }

    /**
     * Execute an LLM request with full cost governance.
     *
     * @param prompt      The raw prompt text
     * @param featureType The feature requesting the call
     * @param studentId   The student making the request (for budget tracking)
     * @return LLM response
     * @throws TokenBudgetExceededException if context exceeds hard limits even after compression
     */
    public LlmResponse execute(String prompt, FeatureType featureType, Long studentId) {
        // 1. Estimate tokens
        int estimatedTokens = tokenEstimator.estimate(prompt);
        log.debug("Token estimate for {}: {} tokens", featureType, estimatedTokens);

        // 2. Check hard limit
        if (estimatedTokens > featureType.getMaxTokens()) {
            // 3. Try compression
            String compressed = contextCompressor.compress(prompt, featureType.getMaxTokens());
            estimatedTokens = tokenEstimator.estimate(compressed);

            if (estimatedTokens > featureType.getMaxTokens()) {
                throw new TokenBudgetExceededException(
                    featureType.name(), estimatedTokens, featureType.getMaxTokens());
            }
            prompt = compressed;
            log.info("Compressed context for {}: {} → {} tokens",
                     featureType, tokenEstimator.estimate(prompt), estimatedTokens);
        }

        // 4. Check student budget
        budgetEnforcer.validateBudget(studentId, estimatedTokens);

        // 5. Route to model and execute
        LlmClient client = modelRouter.route(featureType, estimatedTokens);
        log.info("Routing {} to model: {}", featureType, client.getModelName());

        LlmResponse response = client.execute(prompt, featureType);

        // 6. Record usage (monitoring aspect will also log, but we update budget here)
        budgetEnforcer.recordUsage(studentId, response.totalTokens());

        return response;
    }
}
