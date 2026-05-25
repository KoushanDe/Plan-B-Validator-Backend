package com.planbvalidator.domain.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RunwayCalculateRequest(
        @NotNull @DecimalMin("0") Double liquidSavings,
        @NotNull @DecimalMin(value = "0.01", message = "monthlyExpenses must be greater than 0") Double monthlyExpenses,
        @DecimalMin("0") Double debtObligations,
        @DecimalMin("0") Double monthlyIncome,
        Boolean sideHustle
) {
    public RunwayCalculateRequest(Double liquidSavings, Double monthlyExpenses, Double debtObligations) {
        this(liquidSavings, monthlyExpenses, debtObligations, 0.0, false);
    }

    public static RunwayCalculateRequest from(FinancialsDto financials) {
        return from(financials, false);
    }

    public static RunwayCalculateRequest from(FinancialsDto financials, boolean sideHustle) {
        return new RunwayCalculateRequest(
                financials.liquidSavings(),
                financials.monthlyExpenses(),
                financials.debtObligations(),
                financials.monthlyIncome(),
                sideHustle);
    }
}
