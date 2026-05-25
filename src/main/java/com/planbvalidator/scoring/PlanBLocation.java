package com.planbvalidator.scoring;

import com.planbvalidator.domain.request.PlanBDto;
import com.planbvalidator.domain.request.ProfileDto;

/**
 * Effective Plan B market location: optional target city/country, else current profile location.
 */
public final class PlanBLocation {

    private PlanBLocation() {
    }

    public static String effectiveCity(ProfileDto profile, PlanBDto planB) {
        if (hasText(planB.targetCity())) {
            return planB.targetCity().trim();
        }
        return profile.city();
    }

    public static String effectiveCountry(ProfileDto profile, PlanBDto planB) {
        if (hasText(planB.targetCountry())) {
            return planB.targetCountry().trim();
        }
        return profile.country();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
