package com.planbvalidator.resume;

import com.planbvalidator.domain.request.ProfileDto;

import java.util.List;
import java.util.Map;

/**
 * Profile fields inferred from resume text (OpenAI).
 */
public record ResumeProfileExtraction(
        String currentProfession,
        String industry,
        Double yearsExperience,
        String country,
        String city,
        String targetCountry,
        String targetCity,
        List<String> assumptions
) {
    public ProfileDto toProfileDto() {
        return new ProfileDto(
                currentProfession,
                industry,
                yearsExperience,
                country,
                city
        );
    }

    public Map<String, Object> toCompactMap() {
        return Map.of(
                "current_profession", nullToEmpty(currentProfession),
                "industry", nullToEmpty(industry),
                "years_experience", yearsExperience == null ? 0 : yearsExperience,
                "country", nullToEmpty(country),
                "city", nullToEmpty(city),
                "target_country", targetCountry == null ? "" : targetCountry,
                "target_city", targetCity == null ? "" : targetCity,
                "assumptions", assumptions == null ? List.of() : assumptions
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
