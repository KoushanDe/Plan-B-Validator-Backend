package com.planbvalidator.scoring;

import com.planbvalidator.domain.request.AnalyzeRequest;
import com.planbvalidator.market.MarketValueAssessment;
import com.planbvalidator.research.ResearchContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class OpportunityCostCalculator {

    /** Share of corporate cash flow retained when keeping the current job (side hustle). */
    private static final double SIDE_HUSTLE_CASHFLOW_RETENTION = 0.70;

    private final SalaryRangeParser salaryRangeParser;

    public OpportunityCostCalculator(SalaryRangeParser salaryRangeParser) {
        this.salaryRangeParser = salaryRangeParser;
    }

    public OpportunityCostMetric compute(AnalyzeRequest request,
                                         MarketValueAssessment marketValue,
                                         ResearchContext research) {
        Baseline corporate = resolveCorporateBaseline(request, marketValue, research);
        PlanBIncome planB = resolvePlanBIncome(request, research);
        boolean sideHustle = request.planB().willKeepJob();

        double rawGap = Math.max(0, corporate.monthly() - planB.monthly());
        double sacrificePercent = corporate.monthly() > 0
                ? (rawGap / corporate.monthly()) * 100.0
                : 0.0;

        if (sideHustle) {
            double retentionFactor = 1.0 - SIDE_HUSTLE_CASHFLOW_RETENTION;
            rawGap *= retentionFactor;
            sacrificePercent *= retentionFactor;
        }

        int incomeSacrifice = clamp((int) Math.round(sacrificePercent));

        int trajectoryPremium = 50;
        if (marketValue != null) {
            trajectoryPremium = clamp((int) Math.round(
                    0.55 * marketValue.marketValueScore() + 0.45 * marketValue.opportunityCostRisk()));
        }

        int experienceLockIn = experienceLockInScore(request.profile().yearsExperience());
        int engagementAdjustment = sideHustle ? 10 : 28;

        double blended = 0.42 * incomeSacrifice
                + 0.33 * trajectoryPremium
                + 0.15 * experienceLockIn
                + 0.10 * engagementAdjustment;

        if (planB.monthly() >= corporate.monthly() && corporate.monthly() > 0) {
            blended = Math.min(blended, 32);
        }

        int score = clamp((int) Math.round(blended));
        String band = bandFor(score);
        String engagementMode = request.planB().engagementMode().name().toLowerCase();

        Map<String, Integer> components = new LinkedHashMap<>();
        components.put("income_sacrifice", incomeSacrifice);
        components.put("trajectory_premium", trajectoryPremium);
        components.put("experience_lock_in", experienceLockIn);
        components.put("engagement_adjustment", engagementAdjustment);

        String summary = buildSummary(
                score,
                band,
                corporate,
                planB,
                rawGap,
                sacrificePercent,
                sideHustle,
                marketValue != null
        );

        return new OpportunityCostMetric(
                score,
                band,
                corporate.monthly(),
                planB.monthly(),
                planB.userProjection(),
                rawGap,
                sacrificePercent,
                corporate.source(),
                planB.source(),
                engagementMode,
                Map.copyOf(components),
                summary
        );
    }

    private Baseline resolveCorporateBaseline(AnalyzeRequest request,
                                              MarketValueAssessment marketValue,
                                              ResearchContext research) {
        if (research != null && research.hasWebSalaryData()) {
            var parsed = salaryRangeParser.monthlyMidpointInr(research.corporateSalaryRange());
            if (parsed.isPresent()) {
                return new Baseline(parsed.get(), "web_research_market");
            }
        }
        if (marketValue != null && marketValue.estimatedSalaryRange() != null) {
            var parsed = salaryRangeParser.monthlyMidpointInr(marketValue.estimatedSalaryRange());
            if (parsed.isPresent()) {
                return new Baseline(parsed.get(), "resume_market_inference");
            }
        }
        return new Baseline(request.financials().monthlyIncome(), "stated_current_income");
    }

    private PlanBIncome resolvePlanBIncome(AnalyzeRequest request, ResearchContext research) {
        double userProjection = Math.max(
                request.planB().expectedIncome12Months(),
                request.planB().expectedIncome6Months());
        if (research != null && research.hasPlanBMarketIncome()) {
            var parsed = salaryRangeParser.monthlyMidpointInr(research.planBRealisticIncomeRange());
            if (parsed.isPresent()) {
                return new PlanBIncome(parsed.get(), userProjection, "web_research_market");
            }
        }
        return new PlanBIncome(userProjection, userProjection, "user_projection");
    }

    private static int experienceLockInScore(double years) {
        if (years >= 12) {
            return 82;
        }
        if (years >= 8) {
            return 72;
        }
        if (years >= 5) {
            return 58;
        }
        if (years >= 2) {
            return 42;
        }
        return 28;
    }

    private static String bandFor(int score) {
        if (score >= 75) {
            return "very_high";
        }
        if (score >= 55) {
            return "high";
        }
        if (score >= 35) {
            return "moderate";
        }
        return "low";
    }

    private static String buildSummary(int score,
                                       String band,
                                       Baseline corporate,
                                       PlanBIncome planB,
                                       double monthlySacrifice,
                                       double sacrificePercent,
                                       boolean sideHustle,
                                       boolean hasResumeSignals) {
        String mode = sideHustle ? "keeping your current job (side hustle)" : "quitting for Plan B full-time";
        String baselineNote = switch (corporate.source()) {
            case "web_research_market" -> "using web-backed corporate market comp";
            case "resume_market_inference" -> "using resume-inferred corporate market comp";
            default -> "using your stated current monthly income";
        };
        String planBNote = switch (planB.source()) {
            case "web_research_market" -> "web-backed Plan B market income at ~12 months";
            default -> "your projected Plan B income at horizon";
        };

        if (planB.monthly() >= corporate.monthly() && corporate.monthly() > 0) {
            return String.format(Locale.ROOT,
                    "Opportunity cost is %s (%d/100): market-realistic Plan B income (%s) meets or exceeds corporate market baseline (%s), "
                            + "so financial sacrifice is limited while %s.",
                    band.replace('_', ' '), score, planBNote, baselineNote, mode);
        }

        String trajectoryNote = hasResumeSignals
                ? "plus trajectory premium from your resume/credential signals"
                : "with limited resume trajectory data";

        return String.format(Locale.ROOT,
                "Opportunity cost is %s (%d/100): estimated ₹%.0f/month corporate market baseline (%s) vs ₹%.0f/month Plan B (%s), "
                        + "≈%.0f%% income sacrifice while %s, %s.",
                band.replace('_', ' '),
                score,
                corporate.monthly(),
                baselineNote,
                planB.monthly(),
                planBNote,
                sacrificePercent,
                mode,
                trajectoryNote);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record Baseline(double monthly, String source) {
    }

    private record PlanBIncome(double monthly, double userProjection, String source) {
    }
}
