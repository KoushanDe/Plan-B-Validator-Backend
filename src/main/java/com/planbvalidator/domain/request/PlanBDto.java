package com.planbvalidator.domain.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.planbvalidator.domain.common.PlanBEngagementMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param iWillQuitMyJob UI checkbox: true = quit current job for Plan B; false = keep job (side hustle).
 */
public record PlanBDto(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotBlank @Size(max = 1000) String reason,
        @NotNull @Min(1) @Max(120) Integer timelineMonths,
        @NotNull @DecimalMin("0") Double expectedIncome3Months,
        @NotNull @DecimalMin("0") Double expectedIncome6Months,
        @NotNull @DecimalMin("0") Double expectedIncome12Months,
        @NotNull Boolean iWillQuitMyJob,
        @Size(max = 80) String targetCountry,
        @Size(max = 80) String targetCity
) {
    @JsonIgnore
    public boolean willKeepJob() {
        return !iWillQuitMyJob;
    }

    /** Derived for prompts and legacy compact payloads — not sent in request JSON. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public PlanBEngagementMode engagementMode() {
        return iWillQuitMyJob ? PlanBEngagementMode.FULL_TIME_LEAP : PlanBEngagementMode.SIDE_HUSTLE;
    }
}
