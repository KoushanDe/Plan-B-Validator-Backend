package com.planbvalidator.scoring;

import com.planbvalidator.domain.request.FinancialsDto;

/**
 * Derived financial metrics from submitted inputs (not collected separately).
 */
public final class FinancialsDerivations {

    private FinancialsDerivations() {
    }

    /**
     * Discretionary savings per month: income minus living expenses and monthly debt payments (EMI).
     * Floored at zero when outflows exceed income.
     */
    public static double monthlySavings(FinancialsDto financials) {
        double derived = financials.monthlyIncome()
                - financials.monthlyExpenses()
                - financials.debtObligations();
        return Math.max(0, derived);
    }

    public static boolean outflowsExceedIncome(FinancialsDto financials) {
        return financials.monthlyIncome() < financials.monthlyExpenses() + financials.debtObligations();
    }
}
