package com.planbvalidator.domain.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FinancialsDto(
        @NotNull @DecimalMin("0") Double monthlyIncome,
        @NotNull @DecimalMin("0") Double liquidSavings,
        @NotNull @DecimalMin(value = "0.01", message = "monthlyExpenses must be greater than 0") Double monthlyExpenses,
        @NotNull @Min(0) Integer dependents,
        @NotNull @DecimalMin("0") Double debtObligations
) {
}
