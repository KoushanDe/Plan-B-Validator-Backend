package com.planbvalidator.llm;

import com.planbvalidator.domain.common.ConfidenceLevel;
import com.planbvalidator.domain.common.Verdict;
import com.planbvalidator.research.ResearchContext;
import com.planbvalidator.scoring.ScoreBreakdown;

import java.util.List;

public record LlmReasoningInput(
        Verdict verdict,
        int feasibilityScore,
        int riskScore,
        double runwayMonths,
        ConfidenceLevel confidence,
        ScoreBreakdown scoreBreakdown,
        String psychologySummary,
        String riskProfile,
        ResearchContext researchContext,
        List<String> assumptions,
        List<String> dataGaps
) {
}
