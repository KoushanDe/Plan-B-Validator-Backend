package com.planbvalidator.domain.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Answers to the 10-question psychology questionnaire (Likert 1–5 each).
 * Question text: GET /v1/questionnaire/questions
 */
public record PsychologyDto(
        @NotNull @Min(1) @Max(5) Integer uncertaintyTolerance,
        @NotNull @Min(1) @Max(5) Integer discipline,
        @NotNull @Min(1) @Max(5) Integer stressRecovery,
        @NotNull @Min(1) @Max(5) Integer validationDependency,
        @NotNull @Min(1) @Max(5) Integer impulsiveness,
        @NotNull @Min(1) @Max(5) Integer routineAdherence,
        @NotNull @Min(1) @Max(5) Integer setbackRecovery,
        @NotNull @Min(1) @Max(5) Integer uncertaintyStamina,
        @NotNull @Min(1) @Max(5) Integer financialResilience,
        @NotNull @Min(1) @Max(5) Integer selfDirectedMotivation
) {
}
