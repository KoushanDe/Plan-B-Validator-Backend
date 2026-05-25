package com.planbvalidator.scoring;

import com.planbvalidator.domain.common.ConfidenceLevel;
import com.planbvalidator.domain.common.Verdict;

import java.util.List;

public record ScoringResult(
        Verdict verdict,
        int feasibilityScore,
        int riskScore,
        double runwayMonths,
        ConfidenceLevel confidence,
        ScoreBreakdown scoreBreakdown,
        OpportunityCostMetric opportunityCost,
        IncomeRoadmapValidator.IncomeRoadmapAssessment incomeRoadmap,
        List<String> assumptions,
        List<String> dataGaps
) {
}
