package com.planbvalidator.domain.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConstraintsDto(
        @NotBlank @Size(max = 1000) String successDefinition,
        @NotBlank @Size(max = 1000) String biggestFear,
        @NotBlank @Size(max = 1000) String acceptableDownside,
        @NotNull @DecimalMin("0") Double minimumAcceptableSalary,
        @NotNull @Min(0) @Max(120) Integer acceptableMonthsWithoutIncome,
        @NotNull @Min(1) @Max(5) Integer familyPressureLevel
) {
}
