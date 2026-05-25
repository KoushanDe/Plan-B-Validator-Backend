package com.planbvalidator.market;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM-derived resume signals for corporate market value and Plan B opportunity cost.
 * Salary range here is preliminary; web-backed comp lives in {@link com.planbvalidator.research.ResearchContext}.
 */
public record MarketValueAssessment(
        String credentialTier,
        int marketValueScore,
        String estimatedSalaryRange,
        String corporateOpportunitySummary,
        String planBRoiSummary,
        int opportunityCostRisk,
        List<String> keySignals,
        List<String> assumptions,
        List<String> recentEmployers,
        List<String> recentJobTitles,
        List<String> compSearchQueries
) {
    public MarketValueAssessment(
            String credentialTier,
            int marketValueScore,
            String estimatedSalaryRange,
            String corporateOpportunitySummary,
            String planBRoiSummary,
            int opportunityCostRisk,
            List<String> keySignals,
            List<String> assumptions) {
        this(credentialTier, marketValueScore, estimatedSalaryRange, corporateOpportunitySummary,
                planBRoiSummary, opportunityCostRisk, keySignals, assumptions, List.of(), List.of(), List.of());
    }

    public Map<String, Object> toCompactMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("credential_tier", credentialTier);
        m.put("market_value_score", marketValueScore);
        m.put("inferred_salary_range_preliminary", estimatedSalaryRange);
        m.put("corporate_opportunity_summary", corporateOpportunitySummary);
        m.put("plan_b_roi_summary", planBRoiSummary);
        m.put("opportunity_cost_risk", opportunityCostRisk);
        m.put("key_signals", keySignals == null ? List.of() : keySignals);
        m.put("recent_employers", recentEmployers == null ? List.of() : recentEmployers);
        m.put("recent_job_titles", recentJobTitles == null ? List.of() : recentJobTitles);
        m.put("comp_search_queries", compSearchQueries == null ? List.of() : compSearchQueries);
        return m;
    }
}
