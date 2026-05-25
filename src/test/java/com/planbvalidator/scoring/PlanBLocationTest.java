package com.planbvalidator.scoring;

import com.planbvalidator.domain.request.PlanBDto;
import com.planbvalidator.domain.request.ProfileDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanBLocationTest {

    @Test
    void usesPlanBTargetWhenSet() {
        var profile = new ProfileDto("Eng", "Tech", 5.0, "India", "Mumbai");
        var planB = new PlanBDto("T", "D", "R", 6, 0.0, 0.0, 0.0, false, "United States", "Austin");
        assertEquals("Austin", PlanBLocation.effectiveCity(profile, planB));
        assertEquals("United States", PlanBLocation.effectiveCountry(profile, planB));
    }

    @Test
    void fallsBackToProfileLocation() {
        var profile = new ProfileDto("Eng", "Tech", 5.0, "India", "Mumbai");
        var planB = new PlanBDto("T", "D", "R", 6, 0.0, 0.0, 0.0, false, null, null);
        assertEquals("Mumbai", PlanBLocation.effectiveCity(profile, planB));
        assertEquals("India", PlanBLocation.effectiveCountry(profile, planB));
    }
}
