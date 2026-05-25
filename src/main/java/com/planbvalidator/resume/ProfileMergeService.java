package com.planbvalidator.resume;

import com.planbvalidator.domain.request.PlanBDto;
import com.planbvalidator.domain.request.ProfileDto;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProfileMergeService {

    public ProfileMergeResult merge(ProfileDto formProfile, PlanBDto formPlanB, ResumeProfileExtraction fromResume) {
        Map<String, String> sources = new LinkedHashMap<>();

        String profession = pick(formProfile.currentProfession(), fromResume.currentProfession(), "currentProfession", sources);
        String industry = pick(formProfile.industry(), fromResume.industry(), "industry", sources);
        Double years = pickYears(formProfile.yearsExperience(), fromResume.yearsExperience(), sources);
        String country = pick(formProfile.country(), fromResume.country(), "country", sources);
        String city = pick(formProfile.city(), fromResume.city(), "city", sources);

        ProfileDto mergedProfile = new ProfileDto(profession, industry, years, country, city);

        String targetCountry = pickNullable(formPlanB.targetCountry(), fromResume.targetCountry(), "targetCountry", sources);
        String targetCity = pickNullable(formPlanB.targetCity(), fromResume.targetCity(), "targetCity", sources);

        PlanBDto mergedPlanB = new PlanBDto(
                formPlanB.title(),
                formPlanB.description(),
                formPlanB.reason(),
                formPlanB.timelineMonths(),
                formPlanB.expectedIncome3Months(),
                formPlanB.expectedIncome6Months(),
                formPlanB.expectedIncome12Months(),
                formPlanB.iWillQuitMyJob(),
                targetCountry,
                targetCity
        );

        return new ProfileMergeResult(mergedProfile, mergedPlanB, Map.copyOf(sources));
    }

    private static String pick(String formValue, String resumeValue, String field, Map<String, String> sources) {
        if (hasText(formValue)) {
            sources.put(field, "form");
            return formValue.trim();
        }
        if (hasText(resumeValue)) {
            sources.put(field, "resume");
            return resumeValue.trim();
        }
        sources.put(field, "missing");
        return "";
    }

    private static String pickNullable(String formValue, String resumeValue, String field, Map<String, String> sources) {
        if (hasText(formValue)) {
            sources.put(field, "form");
            return formValue.trim();
        }
        if (hasText(resumeValue)) {
            sources.put(field, "resume");
            return resumeValue.trim();
        }
        sources.put(field, "missing");
        return null;
    }

    private static Double pickYears(Double formYears, Double resumeYears, Map<String, String> sources) {
        if (formYears != null && formYears > 0) {
            sources.put("yearsExperience", "form");
            return formYears;
        }
        if (resumeYears != null && resumeYears > 0) {
            sources.put("yearsExperience", "resume");
            return resumeYears;
        }
        sources.put("yearsExperience", "missing");
        return 0.0;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
