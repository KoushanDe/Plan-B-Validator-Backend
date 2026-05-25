package com.planbvalidator.domain.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * JSON body for analyze. Resume/CV must be uploaded as a separate PDF file (multipart), not in JSON.
 */
public record AnalyzeRequest(
        @NotNull @Valid ProfileDto profile,
        @NotNull @Valid FinancialsDto financials,
        @NotNull @Valid PlanBDto planB,
        @NotNull @Valid ConstraintsDto constraints,
        @NotNull @Valid PsychologyDto psychology,
        @Valid ResearchOptionsDto researchOptions
) {
}
