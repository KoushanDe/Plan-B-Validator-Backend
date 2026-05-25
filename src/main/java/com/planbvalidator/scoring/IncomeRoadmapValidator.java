package com.planbvalidator.scoring;

import com.planbvalidator.pipeline.AnalyzeContextFields;
import com.planbvalidator.config.IncomeRampProperties;
import com.planbvalidator.domain.request.AnalyzeRequest;
import com.planbvalidator.research.ResearchContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Compares the user's Plan B income roadmap (3/6/12 month projections + timeline)
 * against web-backed market benchmarks from research.
 */
@Component
public class IncomeRoadmapValidator {

    private final SalaryRangeParser salaryRangeParser;
    private final IncomeRampProperties incomeRamp;

    public IncomeRoadmapValidator(SalaryRangeParser salaryRangeParser, IncomeRampProperties incomeRamp) {
        this.salaryRangeParser = salaryRangeParser;
        this.incomeRamp = incomeRamp;
    }

    public IncomeRoadmapAssessment assess(AnalyzeRequest request, ResearchContext research) {
        double user3 = request.planB().expectedIncome3Months();
        double user6 = request.planB().expectedIncome6Months();
        double user12 = request.planB().expectedIncome12Months();
        int userTimeline = request.planB().timelineMonths();

        if (research == null || !research.hasPlanBMarketIncome()) {
            return IncomeRoadmapAssessment.unavailable(user3, user6, user12, userTimeline);
        }

        Optional<Double> market12Opt = salaryRangeParser.monthlyMidpointInr(research.planBRealisticIncomeRange());
        if (market12Opt.isEmpty()) {
            return IncomeRoadmapAssessment.unavailable(user3, user6, user12, userTimeline);
        }

        double market12 = market12Opt.get();
        double market3 = market12 * incomeRamp.at3MonthsFraction();
        double market6 = market12 * incomeRamp.at6MonthsFraction();

        int realismScore = weightedRealismScore(user3, user6, user12, market3, market6, market12);

        int typicalMonths = research.typicalMonthsToMeaningfulIncome() != null
                && research.typicalMonthsToMeaningfulIncome() > 0
                ? research.typicalMonthsToMeaningfulIncome()
                : defaultTypicalMonths(market12);

        int timelineAdjustment = timelineAdjustment(userTimeline, typicalMonths, user3, user6, user12);

        List<String> flags = buildFlags(user3, user6, user12, market3, market6, market12, userTimeline, typicalMonths);

        String summary = buildSummary(realismScore, user12, market12, userTimeline, typicalMonths, flags);

        return new IncomeRoadmapAssessment(
                realismScore,
                market3,
                market6,
                market12,
                user3,
                user6,
                user12,
                typicalMonths,
                timelineAdjustment,
                flags,
                summary
        );
    }

    private static int weightedRealismScore(double user3,
                                            double user6,
                                            double user12,
                                            double market3,
                                            double market6,
                                            double market12) {
        double s3 = milestoneRealism(user3, market3);
        double s6 = milestoneRealism(user6, market6);
        double s12 = milestoneRealism(user12, market12);
        return clamp((int) Math.round(s3 * 0.30 + s6 * 0.30 + s12 * 0.40));
    }

    private static double milestoneRealism(double userIncome, double marketBenchmark) {
        if (marketBenchmark <= 0) {
            return userIncome <= 0 ? 75 : 40;
        }
        if (userIncome <= 0) {
            return marketBenchmark <= 20_000 ? 80 : 55;
        }
        double ratio = userIncome / marketBenchmark;
        if (ratio <= 1.0) {
            return 90 + (1.0 - ratio) * 10;
        }
        if (ratio <= 1.5) {
            return 90 - (ratio - 1.0) * 40;
        }
        if (ratio <= 2.5) {
            return 70 - (ratio - 1.5) * 35;
        }
        return Math.max(5, 35 - (ratio - 2.5) * 15);
    }

    private static int timelineAdjustment(int userTimeline,
                                          int typicalMonths,
                                          double user3,
                                          double user6,
                                          double user12) {
        int adjustment = 0;
        if (userTimeline < typicalMonths * 0.75) {
            adjustment -= 12;
        } else if (userTimeline < typicalMonths) {
            adjustment -= 6;
        } else if (userTimeline >= typicalMonths + 3) {
            adjustment += 4;
        }

        boolean expectsEarlyIncome = user3 > 0 || user6 > 0;
        if (expectsEarlyIncome && typicalMonths >= 5 && userTimeline < typicalMonths - 1) {
            adjustment -= 8;
        }
        if (user12 > 0 && userTimeline > typicalMonths + 6) {
            adjustment -= 4;
        }
        return adjustment;
    }

    private static int defaultTypicalMonths(double market12Monthly) {
        if (market12Monthly >= 200_000) {
            return 9;
        }
        if (market12Monthly >= 80_000) {
            return 6;
        }
        return 4;
    }

    private static List<String> buildFlags(double user3,
                                           double user6,
                                           double user12,
                                           double market3,
                                           double market6,
                                           double market12,
                                           int userTimeline,
                                           int typicalMonths) {
        List<String> flags = new ArrayList<>();
        if (user3 > market3 * 1.5 && user3 > 0) {
            flags.add("3-month income projection exceeds typical market ramp for this Plan B");
        }
        if (user6 > market6 * 1.5 && user6 > 0) {
            flags.add("6-month income projection exceeds typical market ramp for this Plan B");
        }
        if (user12 > market12 * 1.5 && user12 > 0) {
            flags.add("12-month income projection exceeds web-backed Plan B market range");
        }
        if (userTimeline < typicalMonths) {
            flags.add("Self-declared timeline is shorter than typical market time-to-meaningful-income (~"
                    + typicalMonths + " months)");
        }
        return flags;
    }

    private static String buildSummary(int realismScore,
                                       double user12,
                                       double market12,
                                       int userTimeline,
                                       int typicalMonths,
                                       List<String> flags) {
        if (flags.isEmpty()) {
            return String.format(Locale.ROOT,
                    "Income roadmap realism is strong (%d/100): projections align with web-backed Plan B market benchmarks "
                            + "(≈₹%.0f/month at 12 months vs your ₹%.0f/month target).",
                    realismScore, market12, user12);
        }
        return String.format(Locale.ROOT,
                "Income roadmap realism is moderate (%d/100): your timeline (%d months) and/or income targets diverge "
                        + "from market norms (typical ramp ~%d months; market ≈₹%.0f/month at 12 months).",
                realismScore, userTimeline, typicalMonths, market12);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public record IncomeRoadmapAssessment(
            int realismScore,
            double marketMonthlyAt3m,
            double marketMonthlyAt6m,
            double marketMonthlyAt12m,
            double userMonthlyAt3m,
            double userMonthlyAt6m,
            double userMonthlyAt12m,
            int typicalMonthsToMeaningfulIncome,
            int timelineAdjustment,
            List<String> flags,
            String summary
    ) {
        public boolean available() {
            return marketMonthlyAt12m > 0;
        }

        public static IncomeRoadmapAssessment unavailable(double user3, double user6, double user12, int userTimeline) {
            return new IncomeRoadmapAssessment(
                    50, 0, 0, 0, user3, user6, user12, 0, 0, List.of(),
                    "Plan B market income benchmark unavailable; roadmap not validated against web research."
            );
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("realism_score", realismScore);
            m.put("market_monthly_at_3m", AnalyzeContextFields.roundMonths(marketMonthlyAt3m));
            m.put("market_monthly_at_6m", AnalyzeContextFields.roundMonths(marketMonthlyAt6m));
            m.put("market_monthly_at_12m", AnalyzeContextFields.roundMonths(marketMonthlyAt12m));
            m.put("user_monthly_at_3m", AnalyzeContextFields.roundMonths(userMonthlyAt3m));
            m.put("user_monthly_at_6m", AnalyzeContextFields.roundMonths(userMonthlyAt6m));
            m.put("user_monthly_at_12m", AnalyzeContextFields.roundMonths(userMonthlyAt12m));
            m.put("typical_months_to_meaningful_income", typicalMonthsToMeaningfulIncome);
            m.put("timeline_adjustment", timelineAdjustment);
            m.put("flags", flags);
            m.put("summary", summary);
            m.put("validated_against_market", available());
            return m;
        }
    }
}
