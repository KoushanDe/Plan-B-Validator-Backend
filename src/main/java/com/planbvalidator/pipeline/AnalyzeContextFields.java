package com.planbvalidator.pipeline;

import com.planbvalidator.config.RunwayThresholdsProperties;
import com.planbvalidator.domain.request.AnalyzeRequest;
import com.planbvalidator.domain.request.FinancialsDto;
import com.planbvalidator.domain.request.RunwayCalculateRequest;
import com.planbvalidator.scoring.FinancialsDerivations;

import java.util.Map;

/**
 * Shared user narrative + runway fields for LLM compact payloads.
 */
public final class AnalyzeContextFields {

    private AnalyzeContextFields() {
    }

    public static void putEngagementMode(Map<String, Object> target, AnalyzeRequest request) {
        target.put("engagement_mode", request.planB().engagementMode().name().toLowerCase());
    }

    public static void putPlanBNarrative(Map<String, Object> target, AnalyzeRequest request) {
        target.put("plan_b_reason", request.planB().reason());
        target.put("plan_b_description", request.planB().description());
    }

    public static void putConstraintsNarrative(Map<String, Object> target, AnalyzeRequest request) {
        target.put("success_definition", request.constraints().successDefinition());
        target.put("biggest_fear", request.constraints().biggestFear());
        target.put("acceptable_downside", request.constraints().acceptableDownside());
        target.put("minimum_acceptable_salary", request.constraints().minimumAcceptableSalary());
        target.put("acceptable_months_without_income", request.constraints().acceptableMonthsWithoutIncome());
        target.put("family_pressure_level", request.constraints().familyPressureLevel());
    }

    /**
     * Runway from liquid savings and monthly burn (expenses + EMI). Used instead of a separate emergency-fund target field.
     */
    public static void putRunwayContext(Map<String, Object> target,
                                        AnalyzeRequest request,
                                        double runwayMonths,
                                        RunwayThresholdsProperties thresholds) {
        putRunwayContext(target, request, runwayMonths, thresholds, null, null);
    }

    public static void putRunwayContext(Map<String, Object> target,
                                        AnalyzeRequest request,
                                        double runwayMonths,
                                        RunwayThresholdsProperties thresholds,
                                        Double netBurn,
                                        String runwayMode) {
        FinancialsDto financials = request.financials();
        double monthlyBurn = monthlyBurn(financials);
        target.put("runway_months", roundMonths(runwayMonths));
        target.put("runway_classification", classifyRunway(runwayMonths, thresholds));
        target.put("liquid_savings", financials.liquidSavings());
        target.put("monthly_burn", monthlyBurn);
        if (netBurn != null) {
            target.put("net_burn", roundMonths(netBurn));
        }
        if (runwayMode != null) {
            target.put("runway_mode", runwayMode);
        }
        target.put("monthly_expenses", financials.monthlyExpenses());
        target.put("monthly_debt_payments", financials.debtObligations());
        target.put("current_monthly_income", financials.monthlyIncome());
        target.put("derived_monthly_savings", FinancialsDerivations.monthlySavings(financials));
        target.put("dependents", financials.dependents());
        target.put("runway_vs_acceptable_gap_months",
                roundMonths(request.constraints().acceptableMonthsWithoutIncome() - runwayMonths));
    }

    public static double monthlyBurn(FinancialsDto financials) {
        return financials.monthlyExpenses() + financials.debtObligations();
    }

    public static double monthlyBurn(RunwayCalculateRequest request) {
        double debt = request.debtObligations() != null ? request.debtObligations() : 0;
        return request.monthlyExpenses() + debt;
    }

    public static double computeRunwayMonths(FinancialsDto financials) {
        return roundMonths(financials.liquidSavings() / monthlyBurn(financials));
    }

    public static double roundMonths(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public static String classifyRunway(double runwayMonths, RunwayThresholdsProperties thresholds) {
        if (runwayMonths < thresholds.classificationSevereMaxMonths()) {
            return "severe";
        }
        if (runwayMonths < thresholds.classificationHighRiskMaxMonths()) {
            return "high_risk";
        }
        if (runwayMonths < thresholds.classificationModerateMaxMonths()) {
            return "moderate";
        }
        return "stable";
    }
}
