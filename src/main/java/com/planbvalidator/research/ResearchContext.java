package com.planbvalidator.research;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ResearchContext(
        String marketSummary,
        MarketSentiment marketSentiment,
        String salaryNotes,
        List<String> riskFactors,
        String corporateSalaryRange,
        String planBMarketNotes,
        List<String> salarySources,
        int webCompetitivenessScore,
        boolean salaryDisagreementWithResume,
        String planBRealisticIncomeRange,
        Integer typicalMonthsToMeaningfulIncome
) {
    public ResearchContext(
            String marketSummary,
            MarketSentiment marketSentiment,
            String salaryNotes,
            List<String> riskFactors,
            String corporateSalaryRange,
            String planBMarketNotes,
            List<String> salarySources,
            int webCompetitivenessScore,
            boolean salaryDisagreementWithResume) {
        this(marketSummary, marketSentiment, salaryNotes, riskFactors, corporateSalaryRange, planBMarketNotes,
                salarySources, webCompetitivenessScore, salaryDisagreementWithResume, "", null);
    }

    public boolean hasWebSalaryData() {
        return corporateSalaryRange != null
                && !corporateSalaryRange.isBlank()
                && !"unknown".equalsIgnoreCase(corporateSalaryRange.trim());
    }

    public boolean hasPlanBMarketIncome() {
        return planBRealisticIncomeRange != null
                && !planBRealisticIncomeRange.isBlank()
                && !"unknown".equalsIgnoreCase(planBRealisticIncomeRange.trim());
    }

    public Map<String, Object> toCompactMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("market_summary", marketSummary == null ? "" : marketSummary);
        m.put("market_sentiment", marketSentiment.name().toLowerCase());
        m.put("corporate_salary_range", corporateSalaryRange == null ? "" : corporateSalaryRange);
        m.put("plan_b_market_notes", planBMarketNotes == null ? "" : planBMarketNotes);
        m.put("plan_b_realistic_income_range", planBRealisticIncomeRange == null ? "" : planBRealisticIncomeRange);
        m.put("typical_months_to_meaningful_income", typicalMonthsToMeaningfulIncome);
        m.put("salary_notes", salaryNotes == null ? "" : salaryNotes);
        m.put("salary_sources", salarySources == null ? List.of() : salarySources);
        m.put("web_competitiveness_score", webCompetitivenessScore);
        m.put("salary_disagreement_with_resume_inference", salaryDisagreementWithResume);
        m.put("risk_factors", riskFactors == null ? List.of() : riskFactors);
        return m;
    }
}
