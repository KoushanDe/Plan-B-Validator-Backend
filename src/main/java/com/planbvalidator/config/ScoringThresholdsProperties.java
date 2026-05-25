package com.planbvalidator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "planb.scoring.thresholds")
public record ScoringThresholdsProperties(
        List<RunwayBucket> runwayBuckets,
        PsychologyInfluence psychology,
        Reversibility reversibility,
        Timeline timeline,
        Market market,
        VerdictRules verdict,
        RiskAdjustments riskAdjustments
) {
    public record RunwayBucket(double maxEffectiveMonths, int score) {}

    public record PsychologyInfluence(
            double scoreMidpoint,
            double effectiveMonthsPerRiskPoint,
            double effectiveMonthsPerFounderPoint,
            int runwaySubscoreBonusPerCombinedPoint
    ) {}

    public record Reversibility(
            int baseScore,
            int keepJobBonus,
            int fullTimeLeapBonus,
            int reentryScoreHighThreshold,
            int reentryHighBonus,
            int reentryScoreModerateThreshold,
            int reentryModerateBonus,
            int reentryScoreLowThreshold,
            int reentryLowPenalty,
            int reentryUnavailablePenalty,
            int experiencedYearsMin,
            int experiencedBonus
    ) {}

    public record Timeline(
            int baseScore,
            int aggressiveTimelineMonths,
            int aggressivePenalty,
            int moderateTimelineMonths,
            int moderatePenalty,
            int relaxedTimelineMonths,
            int relaxedPenalty,
            double shortRunwayMonths,
            int shortRunwayAggressiveTimelinePenalty,
            int noIncomeAt3MonthsPenalty,
            int fullTimeLeapPenalty
    ) {}

    public record Market(
            int baseScore,
            int incomeMeetsMinAt6MonthsBonus,
            int incomeMeetsMinAt12MonthsBonus,
            int noIncomeAt3MonthsPenalty,
            double webCompetitivenessWeight,
            double resumeMarketValueWeightWithWeb,
            double resumeMarketValueWeightWithoutWeb,
            int opportunityCostScoreHighThreshold,
            int highOpportunityCostPenalty,
            int opportunityCostScoreLowThreshold,
            int lowOpportunityCostBonus,
            int sentimentPositive,
            int sentimentModeratelyPositive,
            int sentimentModeratelyNegative,
            int sentimentNegative,
            double incomeRoadmapRealismWeight,
            int salaryDisagreementPenalty,
            int riskFactorPenaltyEach,
            int riskFactorPenaltyCap
    ) {}

    public record VerdictRules(
            double hardVetoRunwayMonths,
            int takeLeapMinFeasibility,
            int takeLeapMaxRisk,
            int cautionMinFeasibility,
            int cautionMaxRisk,
            int delayMinFeasibility,
            double delayMinRunwayMonths
    ) {}

    public record RiskAdjustments(
            int highOpportunityCostThreshold,
            int highOpportunityCostBonus,
            int founderMindsetRiskPointsEach,
            int dependentPointsEach,
            int dependentCap,
            double debtRatioMultiplier,
            int debtCap,
            int familyPressureMinLevel,
            int familyPressureBonus,
            int runwayBelowAcceptablePenalty,
            int sideHustleBonus,
            int fullTimeLeapBonus
    ) {}

    public static ScoringThresholdsProperties defaults() {
        return new ScoringThresholdsProperties(
                List.of(
                        new RunwayBucket(3, 20),
                        new RunwayBucket(6, 35),
                        new RunwayBucket(12, 58),
                        new RunwayBucket(999, 85)
                ),
                new PsychologyInfluence(5, 0.6, 0.3, 0),
                new Reversibility(50, 35, 5, 70, 15, 50, 8, 35, -5, -3, 5, 5),
                new Timeline(80, 3, 35, 6, 20, 9, 10, 6, 20, 15, 15),
                new Market(60, 10, 10, 12, 0.20, 0.05, 0.15, 55, 10, 35, 5, 8, 4, -5, -10, 0.30, 6, 2, 10),
                new VerdictRules(3, 75, 40, 55, 65, 40, 6),
                new RiskAdjustments(55, 8, 3, 5, 15, 10, 15, 4, 8, 15, 5, 12)
        );
    }
}
