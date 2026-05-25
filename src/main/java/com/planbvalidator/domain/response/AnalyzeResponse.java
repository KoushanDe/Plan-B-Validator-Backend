package com.planbvalidator.domain.response;

import com.planbvalidator.domain.common.ConfidenceLevel;
import com.planbvalidator.domain.common.Verdict;
import com.planbvalidator.scoring.ScoreBreakdown;

import java.util.List;
import java.util.Map;

public record AnalyzeResponse(
        String requestId,
        long processingMs,
        Verdict overallVerdict,
        int feasibilityScore,
        int riskScore,
        ConfidenceLevel confidence,
        double runwayMonths,
        ScoreBreakdown scoreBreakdown,
        Map<String, Object> opportunityCost,
        String recommendationSummary,
        List<String> majorReasons,
        List<String> redFlags,
        List<String> nextSteps,
        List<String> assumptions,
        List<String> dataGaps,
        String personalitySummary,
        String expectedFailureMode,
        String safestNextMove,
        String suggestedFallbackPlan,
        String planBRoiSummary,
        Map<String, Object> marketValueAssessment,
        String disclaimer,
        Map<String, Long> timings,
        Map<String, String> aiProviders,
        Map<String, Object> researchContext,
        Map<String, Object> currentMarketConditionForHiring,
        Map<String, Object> resolvedProfile,
        Map<String, Object> resolvedPlanB,
        Map<String, String> profileFieldSources,
        Map<String, Object> resumeProfileExtraction
) {
}
