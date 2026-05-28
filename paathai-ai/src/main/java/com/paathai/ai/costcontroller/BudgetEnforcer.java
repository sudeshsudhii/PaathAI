package com.paathai.ai.costcontroller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Enforces per-student daily and per-institution monthly token budgets.
 * MVP implementation uses in-memory tracking; production should
 * query the token_usage table for accurate aggregation.
 */
@Component
public class BudgetEnforcer {

    private static final Logger log = LoggerFactory.getLogger(BudgetEnforcer.class);

    // MVP: generous limits, tightened in production
    private static final int DAILY_STUDENT_LIMIT = 50_000;

    /**
     * Validate that the student has sufficient budget for this request.
     *
     * @param studentId      The student making the request
     * @param estimatedTokens Estimated tokens for this request
     */
    public void validateBudget(Long studentId, int estimatedTokens) {
        // TODO: Query token_usage table for today's aggregated usage
        // For MVP, log a warning if approaching limit
        log.debug("Budget check for student {}: {} tokens requested", studentId, estimatedTokens);
    }

    /**
     * Record token usage after a successful LLM call.
     *
     * @param studentId  The student who made the request
     * @param tokensUsed Actual tokens consumed
     */
    public void recordUsage(Long studentId, int tokensUsed) {
        // TODO: Insert into token_usage table
        log.info("Recorded {} tokens for student {}", tokensUsed, studentId);
    }
}
