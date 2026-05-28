package com.paathai.common.exception;

/**
 * Thrown when a student or institution has exhausted their daily/monthly LLM budget.
 */
public class BudgetExhaustedException extends RuntimeException {

    private final Long entityId;
    private final String budgetType;
    private final double usedAmount;
    private final double maxAmount;

    public BudgetExhaustedException(Long entityId, String budgetType,
                                     double usedAmount, double maxAmount) {
        super(String.format("Budget exhausted for %s (ID: %d): used %.4f of %.4f",
                budgetType, entityId, usedAmount, maxAmount));
        this.entityId = entityId;
        this.budgetType = budgetType;
        this.usedAmount = usedAmount;
        this.maxAmount = maxAmount;
    }

    public Long getEntityId() { return entityId; }
    public String getBudgetType() { return budgetType; }
    public double getUsedAmount() { return usedAmount; }
    public double getMaxAmount() { return maxAmount; }
}
