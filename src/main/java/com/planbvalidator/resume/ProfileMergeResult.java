package com.planbvalidator.resume;

import com.planbvalidator.domain.request.PlanBDto;
import com.planbvalidator.domain.request.ProfileDto;

import java.util.Map;

public record ProfileMergeResult(
        ProfileDto mergedProfile,
        PlanBDto mergedPlanB,
        Map<String, String> fieldSources
) {
}
