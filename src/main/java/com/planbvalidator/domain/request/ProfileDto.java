package com.planbvalidator.domain.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * Career profile. Fields may be omitted or left blank when a resume PDF is uploaded — they will be parsed from the resume.
 */
public record ProfileDto(
        @Size(max = 120) String currentProfession,
        @Size(max = 120) String industry,
        @DecimalMin("0") Double yearsExperience,
        @Size(max = 80) String country,
        @Size(max = 80) String city
) {
}
