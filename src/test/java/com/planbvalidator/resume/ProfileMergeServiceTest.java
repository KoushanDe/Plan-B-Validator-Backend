package com.planbvalidator.resume;

import com.planbvalidator.domain.request.PlanBDto;
import com.planbvalidator.domain.request.ProfileDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
class ProfileMergeServiceTest {

    private final ProfileMergeService mergeService = new ProfileMergeService();

    private static PlanBDto planB(String targetCountry, String targetCity) {
        return new PlanBDto(
                "Plan B",
                "Description",
                "Reason",
                6,
                0.0,
                0.0,
                0.0,
                false,
                targetCountry,
                targetCity
        );
    }

    @Test
    void shouldPreferFormValuesWhenPresent() {
        ProfileDto form = new ProfileDto("Engineer", "Fintech", 5.0, "India", "");
        ResumeProfileExtraction resume = new ResumeProfileExtraction(
                "Senior Engineer", "IT Services", 8.0, "India", "Bengaluru", null, null, null);

        var result = mergeService.merge(form, planB(null, null), resume);

        assertEquals("Engineer", result.mergedProfile().currentProfession());
        assertEquals("form", result.fieldSources().get("currentProfession"));
        assertEquals("resume", result.fieldSources().get("city"));
        assertEquals("Bengaluru", result.mergedProfile().city());
    }

    @Test
    void shouldFillFromResumeWhenFormBlank() {
        ProfileDto form = new ProfileDto("", "", null, "", "");
        ResumeProfileExtraction resume = new ResumeProfileExtraction(
                "Product Manager", "SaaS", 6.0, "India", "Pune", null, null, null);

        var result = mergeService.merge(form, planB(null, null), resume);

        assertEquals("Product Manager", result.mergedProfile().currentProfession());
        assertEquals("resume", result.fieldSources().get("currentProfession"));
        assertEquals(6.0, result.mergedProfile().yearsExperience());
    }

    @Test
    void shouldMergeTargetLocationIntoPlanB() {
        ProfileDto form = new ProfileDto("Engineer", "Tech", 5.0, "India", "Mumbai");
        ResumeProfileExtraction resume = new ResumeProfileExtraction(
                null, null, null, null, null, "United States", "San Francisco", null);

        var result = mergeService.merge(form, planB("", ""), resume);

        assertEquals("United States", result.mergedPlanB().targetCountry());
        assertEquals("San Francisco", result.mergedPlanB().targetCity());
        assertEquals("resume", result.fieldSources().get("targetCountry"));
        assertEquals("Mumbai", result.mergedProfile().city());
    }
}
