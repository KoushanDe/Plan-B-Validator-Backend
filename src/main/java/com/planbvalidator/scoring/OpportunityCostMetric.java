package com.planbvalidator.scoring;

import com.planbvalidator.pipeline.AnalyzeContextFields;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deterministic opportunity cost of pursuing Plan B vs staying on the corporate trajectory.
 * Higher {@code score} (0–100) means more is given up financially and career-wise.
 */
public record OpportunityCostMetric(
        int score,
        String band,
        double monthlyCorporateBaseline,
        double monthlyPlanBAtHorizon,
        double userProjectedPlanBAtHorizon,
        double monthlyIncomeSacrifice,
        double incomeSacrificePercent,
        String corporateBaselineSource,
        String planBIncomeSource,
        String engagementMode,
        Map<String, Integer> components,
        String summary
) {
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("score", score);
        m.put("band", band);
        m.put("monthly_corporate_baseline", AnalyzeContextFields.roundMonths(monthlyCorporateBaseline));
        m.put("monthly_plan_b_at_horizon", AnalyzeContextFields.roundMonths(monthlyPlanBAtHorizon));
        m.put("user_projected_plan_b_at_horizon", AnalyzeContextFields.roundMonths(userProjectedPlanBAtHorizon));
        m.put("monthly_income_sacrifice", AnalyzeContextFields.roundMonths(monthlyIncomeSacrifice));
        m.put("income_sacrifice_percent", AnalyzeContextFields.roundMonths(incomeSacrificePercent));
        m.put("corporate_baseline_source", corporateBaselineSource);
        m.put("plan_b_income_source", planBIncomeSource);
        m.put("engagement_mode", engagementMode);
        m.put("components", components);
        m.put("summary", summary);
        return m;
    }
}
