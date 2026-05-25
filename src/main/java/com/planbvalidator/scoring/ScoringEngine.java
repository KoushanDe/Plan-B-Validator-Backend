package com.planbvalidator.scoring;

import com.planbvalidator.config.IncomeRampProperties;
import com.planbvalidator.config.RunwayThresholdsProperties;
import com.planbvalidator.config.ScoringThresholdsProperties;
import com.planbvalidator.config.ScoringWeightsProperties;
import com.planbvalidator.domain.common.ConfidenceLevel;
import com.planbvalidator.domain.common.Verdict;
import com.planbvalidator.domain.request.AnalyzeRequest;
import com.planbvalidator.domain.response.QuestionnaireScoreResponse;
import com.planbvalidator.market.MarketValueAssessment;
import com.planbvalidator.pipeline.AnalyzeContextFields;
import com.planbvalidator.research.CurrentMarketConditionForHiring;
import com.planbvalidator.research.ResearchContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ScoringEngine {

    private final ScoringWeightsProperties weights;
    private final ScoringThresholdsProperties thresholds;
    private final RunwayThresholdsProperties runwayThresholds;
    private final OpportunityCostCalculator opportunityCostCalculator;
    private final IncomeRoadmapValidator incomeRoadmapValidator;
    private final FinancialConsistencyChecker financialConsistencyChecker;
    private final IncomeRampProperties incomeRamp;

    public ScoringEngine(ScoringWeightsProperties weights,
                         ScoringThresholdsProperties thresholds,
                         RunwayThresholdsProperties runwayThresholds,
                         OpportunityCostCalculator opportunityCostCalculator,
                         IncomeRoadmapValidator incomeRoadmapValidator,
                         FinancialConsistencyChecker financialConsistencyChecker,
                         IncomeRampProperties incomeRamp) {
        this.weights = weights;
        this.thresholds = thresholds;
        this.runwayThresholds = runwayThresholds;
        this.opportunityCostCalculator = opportunityCostCalculator;
        this.incomeRoadmapValidator = incomeRoadmapValidator;
        this.financialConsistencyChecker = financialConsistencyChecker;
        this.incomeRamp = incomeRamp;
    }

    public ScoringResult compute(AnalyzeRequest request,
                                 double runwayMonths,
                                 double emergencyRunwayMonths,
                                 QuestionnaireScoreResponse psychologyScore,
                                 MarketValueAssessment marketValue,
                                 ResearchContext researchContext,
                                 boolean researchAvailable,
                                 boolean resumeProvided,
                                 boolean resumeMarketValueAssessed,
                                 CurrentMarketConditionForHiring hiringReentry) {
        OpportunityCostMetric opportunityCost = opportunityCostCalculator.compute(
                request, marketValue, researchContext);
        IncomeRoadmapValidator.IncomeRoadmapAssessment incomeRoadmap =
                incomeRoadmapValidator.assess(request, researchContext);

        int runwayScore = runwaySubScore(runwayMonths, psychologyScore);
        int reversibilityScore = reversibilitySubScore(request, hiringReentry);
        int timelineScore = timelineSubScore(request, runwayMonths, incomeRoadmap);
        int marketScore = marketSubScore(request, marketValue, researchContext, incomeRoadmap,
                opportunityCost, researchAvailable);
        int psychologyRiskTolerance = psychologyFeasibilityScore(psychologyScore);

        int feasibility = (int) Math.round(
                runwayScore * weights.financialRunway()
                        + reversibilityScore * weights.reversibility()
                        + marketScore * weights.marketFeasibility()
                        + psychologyRiskTolerance * weights.riskTolerance()
                        + timelineScore * weights.timelinePressure()
        );

        int riskAdjustments = computeRiskAdjustments(request, runwayMonths, marketValue, psychologyScore,
                opportunityCost);
        int risk = clamp(100 - feasibility + riskAdjustments);
        Verdict verdict = mapVerdict(request, feasibility, risk, runwayMonths, emergencyRunwayMonths);

        List<String> assumptions = buildAssumptions(request, psychologyScore, researchContext, incomeRoadmap,
                researchAvailable, resumeProvided);
        List<String> dataGaps = buildDataGaps(request, runwayMonths, marketValue, researchContext,
                researchAvailable, resumeProvided, resumeMarketValueAssessed, incomeRoadmap, opportunityCost,
                hiringReentry);

        ConfidenceLevel confidence = confidenceLevel(request, researchAvailable, dataGaps, incomeRoadmap,
                researchContext, resumeProvided, resumeMarketValueAssessed);

        return new ScoringResult(
                verdict,
                clamp(feasibility),
                risk,
                AnalyzeContextFields.roundMonths(runwayMonths),
                confidence,
                new ScoreBreakdown(runwayScore, marketScore, psychologyRiskTolerance, timelineScore, reversibilityScore),
                opportunityCost,
                incomeRoadmap,
                assumptions,
                dataGaps
        );
    }

    private List<String> buildAssumptions(AnalyzeRequest request,
                                          QuestionnaireScoreResponse psychologyScore,
                                          ResearchContext researchContext,
                                          IncomeRoadmapValidator.IncomeRoadmapAssessment incomeRoadmap,
                                          boolean researchAvailable,
                                          boolean resumeProvided) {
        List<String> assumptions = new ArrayList<>();
        assumptions.add("Monthly expenses remain stable during transition");
        assumptions.add("No major one-time emergency costs are included");
        if (request.financials().debtObligations() > 0) {
            assumptions.add("Runway uses total monthly burn: living expenses plus monthly debt payments (EMI)");
        }
        if (request.planB().willKeepJob()) {
            assumptions.add("Side-hustle runway uses net burn (expenses + EMI minus continuing job income)");
        } else {
            assumptions.add("Full-time leap runway uses emergency burn (expenses + EMI) without salary; hard veto uses the same emergency runway");
        }
        assumptions.add("Monthly savings derived as income minus expenses minus monthly debt payments (not user-entered)");
        assumptions.add("Runway scoring uses psychology-adjusted effective months (risk-taking "
                + psychologyScore.riskTakingPotential() + "/10, founder mindset "
                + psychologyScore.founderMindset() + "/10)");
        if (resumeProvided) {
            assumptions.add("Profile may include fields parsed from resume PDF where form fields were blank");
        }
        if (request.planB().willKeepJob()) {
            assumptions.add("Keeping current job (side hustle) assumes continued primary income and higher dual-workload stress");
        } else {
            assumptions.add("Quitting current job assumes higher income gap risk during transition");
        }
        if (researchContext != null && researchContext.hasWebSalaryData()) {
            assumptions.add("Corporate comp signal prefers web research over resume-inferred salary");
        }
        if (incomeRoadmap.available()) {
            assumptions.add("Income roadmap scored against web-backed Plan B market income benchmarks");
        }
        if (researchAvailable) {
            assumptions.add("Opportunity cost uses market-backed corporate comp vs market-backed Plan B income when research provides both");
        }
        if (!researchAvailable) {
            assumptions.add("Income milestone bonuses may use your minimum acceptable salary when web research is unavailable");
        }
        return assumptions;
    }

    private List<String> buildDataGaps(AnalyzeRequest request,
                                       double runwayMonths,
                                       MarketValueAssessment marketValue,
                                       ResearchContext researchContext,
                                       boolean researchAvailable,
                                       boolean resumeProvided,
                                       boolean resumeMarketValueAssessed,
                                       IncomeRoadmapValidator.IncomeRoadmapAssessment incomeRoadmap,
                                       OpportunityCostMetric opportunityCost,
                                       CurrentMarketConditionForHiring hiringReentry) {
        List<String> dataGaps = new ArrayList<>();
        addRunwayDataGaps(dataGaps, request, runwayMonths);
        if (request.planB().iWillQuitMyJob()
                && (hiringReentry == null || !hiringReentry.available())) {
            dataGaps.add("Corporate re-hire market assessment unavailable; reversibility uses conservative defaults");
        }
        if (FinancialsDerivations.outflowsExceedIncome(request.financials())) {
            dataGaps.add("Monthly expenses and debt payments exceed income; derived monthly savings treated as zero");
        }
        if (!researchAvailable) {
            dataGaps.add("Market research not available");
        } else if (researchContext != null && !researchContext.hasWebSalaryData()) {
            dataGaps.add("Web-backed corporate salary range not found in market research");
        }
        if (researchContext != null && researchContext.salaryDisagreementWithResume()) {
            dataGaps.add("Web salary research differs from resume-inferred range; scoring prefers web-backed comp");
        }
        if (researchAvailable && researchContext != null && !researchContext.hasPlanBMarketIncome()) {
            dataGaps.add("Web-backed Plan B income benchmark not found; income roadmap not fully validated against market");
        }
        if (incomeRoadmap.available() && incomeRoadmap.realismScore() < 45) {
            dataGaps.add("User income roadmap appears optimistic vs web-backed Plan B market benchmarks (realism "
                    + incomeRoadmap.realismScore() + "/100)");
        }
        for (String flag : incomeRoadmap.flags()) {
            if (!dataGaps.contains(flag)) {
                dataGaps.add(flag);
            }
        }
        addIncomeRoadmapShapeGaps(dataGaps, request);
        if (request.planB().expectedIncome3Months() == 0) {
            dataGaps.add("No income validation signal for first 3 months");
        }
        if (!resumeProvided) {
            dataGaps.add("No resume PDF provided for credential-based market value assessment");
        }
        if (resumeProvided && !resumeMarketValueAssessed) {
            dataGaps.add("Resume market value unavailable; scoring uses web research and profile fields only");
        }
        financialConsistencyChecker.check(request, researchContext, opportunityCost)
                .forEach(gap -> {
                    if (!dataGaps.contains(gap)) {
                        dataGaps.add(gap);
                    }
                });
        return dataGaps;
    }

    private static void addIncomeRoadmapShapeGaps(List<String> dataGaps, AnalyzeRequest request) {
        double i3 = request.planB().expectedIncome3Months();
        double i6 = request.planB().expectedIncome6Months();
        double i12 = request.planB().expectedIncome12Months();
        if (i6 < i3) {
            dataGaps.add("6-month income projection is lower than 3-month projection (non-monotonic roadmap)");
        }
        if (i12 < i6) {
            dataGaps.add("12-month income projection is lower than 6-month projection (non-monotonic roadmap)");
        }
    }

    private int runwaySubScore(double runwayMonths, QuestionnaireScoreResponse psychology) {
        double effectiveMonths = effectiveRunwayMonths(runwayMonths, psychology);
        var buckets = thresholds.runwayBuckets().stream()
                .sorted(Comparator.comparingDouble(ScoringThresholdsProperties.RunwayBucket::maxEffectiveMonths))
                .toList();
        for (var bucket : buckets) {
            if (effectiveMonths < bucket.maxEffectiveMonths()) {
                int score = bucket.score();
                var psych = thresholds.psychology();
                int combined = psychology.riskTakingPotential() + psychology.founderMindset();
                int bonus = (int) Math.round((combined - psych.scoreMidpoint() * 2) * psych.runwaySubscoreBonusPerCombinedPoint());
                return clamp(score + bonus);
            }
        }
        return clamp(buckets.get(buckets.size() - 1).score());
    }

    double effectiveRunwayMonths(double runwayMonths, QuestionnaireScoreResponse psychology) {
        var psych = thresholds.psychology();
        double adjustment = (psychology.riskTakingPotential() - psych.scoreMidpoint()) * psych.effectiveMonthsPerRiskPoint()
                + (psychology.founderMindset() - psych.scoreMidpoint()) * psych.effectiveMonthsPerFounderPoint();
        return Math.max(0, runwayMonths + adjustment);
    }

    private static int psychologyFeasibilityScore(QuestionnaireScoreResponse psychology) {
        return clamp((psychology.riskTakingPotential() + psychology.founderMindset()) * 5);
    }

    private int reversibilitySubScore(AnalyzeRequest request,
                                      CurrentMarketConditionForHiring hiringReentry) {
        var r = thresholds.reversibility();
        int score = r.baseScore();
        if (request.planB().willKeepJob()) {
            score += r.keepJobBonus();
        } else {
            score += r.fullTimeLeapBonus();
            score += reentryReversibilityAdjustment(r, hiringReentry);
        }
        if (request.profile().yearsExperience() >= r.experiencedYearsMin()) {
            score += r.experiencedBonus();
        }
        return clamp(score);
    }

    private static int reentryReversibilityAdjustment(ScoringThresholdsProperties.Reversibility r,
                                                      CurrentMarketConditionForHiring hiringReentry) {
        if (hiringReentry == null || !hiringReentry.available()) {
            return r.reentryUnavailablePenalty();
        }
        int reentryScore = hiringReentry.overallReentryScore();
        if (reentryScore >= r.reentryScoreHighThreshold()) {
            return r.reentryHighBonus();
        }
        if (reentryScore >= r.reentryScoreModerateThreshold()) {
            return r.reentryModerateBonus();
        }
        if (reentryScore < r.reentryScoreLowThreshold()) {
            return r.reentryLowPenalty();
        }
        return 0;
    }

    private int timelineSubScore(AnalyzeRequest request,
                                 double runwayMonths,
                                 IncomeRoadmapValidator.IncomeRoadmapAssessment incomeRoadmap) {
        var t = thresholds.timeline();
        int timeline = request.planB().timelineMonths();
        int score = t.baseScore();
        if (timeline <= t.aggressiveTimelineMonths()) {
            score -= t.aggressivePenalty();
        } else if (timeline <= t.moderateTimelineMonths()) {
            score -= t.moderatePenalty();
        } else if (timeline <= t.relaxedTimelineMonths()) {
            score -= t.relaxedPenalty();
        }

        if (runwayMonths < t.shortRunwayMonths() && timeline <= t.moderateTimelineMonths()) {
            score -= t.shortRunwayAggressiveTimelinePenalty();
        }
        if (request.planB().expectedIncome3Months() == 0) {
            score -= t.noIncomeAt3MonthsPenalty();
        }
        if (request.planB().iWillQuitMyJob()) {
            score -= t.fullTimeLeapPenalty();
        }
        if (incomeRoadmap.available()) {
            score += incomeRoadmap.timelineAdjustment();
        }
        return clamp(score);
    }

    private int marketSubScore(AnalyzeRequest request,
                               MarketValueAssessment marketValue,
                               ResearchContext researchContext,
                               IncomeRoadmapValidator.IncomeRoadmapAssessment incomeRoadmap,
                               OpportunityCostMetric opportunityCost,
                               boolean researchAvailable) {
        var m = thresholds.market();
        int score = m.baseScore();
        double minIncome = request.constraints().minimumAcceptableSalary();
        double i3 = request.planB().expectedIncome3Months();
        double i6 = request.planB().expectedIncome6Months();
        double i12 = request.planB().expectedIncome12Months();

        if (i3 == 0) {
            score -= m.noIncomeAt3MonthsPenalty();
        }

        if (incomeRoadmap.available()) {
            score += (int) Math.round((incomeRoadmap.realismScore() - 50) * m.incomeRoadmapRealismWeight());
            double market12 = incomeRoadmap.marketMonthlyAt12m();
            if (i6 >= market12 * incomeRamp.at6MonthsFraction()) {
                score += m.incomeMeetsMinAt6MonthsBonus();
            }
            if (i12 >= market12) {
                score += m.incomeMeetsMinAt12MonthsBonus();
            }
        } else if (!researchAvailable) {
            if (i6 >= minIncome) {
                score += m.incomeMeetsMinAt6MonthsBonus();
            }
            if (i12 >= minIncome) {
                score += m.incomeMeetsMinAt12MonthsBonus();
            }
        }

        boolean webSalary = researchContext != null && researchContext.hasWebSalaryData();
        if (webSalary) {
            score += (int) Math.round((researchContext.webCompetitivenessScore() - 50) * m.webCompetitivenessWeight());
            if (marketValue != null) {
                score += (int) Math.round((marketValue.marketValueScore() - 50) * m.resumeMarketValueWeightWithWeb());
            }
        } else if (marketValue != null) {
            score += (int) Math.round((marketValue.marketValueScore() - 50) * m.resumeMarketValueWeightWithoutWeb());
        }

        score = applyOpportunityCostMarketAdjustments(score, m, opportunityCost, marketValue, i12);

        if (researchContext != null) {
            if (researchContext.salaryDisagreementWithResume()) {
                score -= m.salaryDisagreementPenalty();
            }
            List<String> risks = researchContext.riskFactors();
            if (risks != null && !risks.isEmpty()) {
                score -= Math.min(m.riskFactorPenaltyCap(), risks.size() * m.riskFactorPenaltyEach());
            }
            int sentimentModifier = switch (researchContext.marketSentiment()) {
                case POSITIVE -> m.sentimentPositive();
                case MODERATELY_POSITIVE -> m.sentimentModeratelyPositive();
                case NEUTRAL -> 0;
                case MODERATELY_NEGATIVE -> m.sentimentModeratelyNegative();
                case NEGATIVE -> m.sentimentNegative();
            };
            score += sentimentModifier;
        }
        return clamp(score);
    }

    private int applyOpportunityCostMarketAdjustments(int score,
                                                        ScoringThresholdsProperties.Market m,
                                                        OpportunityCostMetric opportunityCost,
                                                        MarketValueAssessment marketValue,
                                                        double i12) {
        int adjusted = score;
        int oppScore = resolveOpportunityCostScore(opportunityCost, marketValue);
        if (oppScore >= m.opportunityCostScoreHighThreshold()) {
            double corporateBaseline = opportunityCost.monthlyCorporateBaseline();
            if (i12 < corporateBaseline) {
                adjusted -= m.highOpportunityCostPenalty();
            }
        }
        if (oppScore <= m.opportunityCostScoreLowThreshold()) {
            adjusted += m.lowOpportunityCostBonus();
        }
        return adjusted;
    }

    private static int resolveOpportunityCostScore(OpportunityCostMetric opportunityCost,
                                                   MarketValueAssessment marketValue) {
        if (usesUserStatedBaselines(opportunityCost) && marketValue != null) {
            return marketValue.opportunityCostRisk();
        }
        return opportunityCost.score();
    }

    private static boolean usesUserStatedBaselines(OpportunityCostMetric opportunityCost) {
        return "stated_current_income".equals(opportunityCost.corporateBaselineSource())
                || "user_projection".equals(opportunityCost.planBIncomeSource());
    }

    private int computeRiskAdjustments(AnalyzeRequest request,
                                       double runwayMonths,
                                       MarketValueAssessment marketValue,
                                       QuestionnaireScoreResponse psychology,
                                       OpportunityCostMetric opportunityCost) {
        var r = thresholds.riskAdjustments();
        int adjustment = 0;
        int oppScore = resolveOpportunityCostScore(opportunityCost, marketValue);
        if (oppScore >= r.highOpportunityCostThreshold()) {
            adjustment += r.highOpportunityCostBonus();
        }
        adjustment += Math.min(r.dependentCap(), request.financials().dependents() * r.dependentPointsEach());
        if (request.financials().debtObligations() > 0) {
            double totalMonthlyBurn = request.financials().monthlyExpenses() + request.financials().debtObligations();
            double debtShareOfBurn = request.financials().debtObligations() / Math.max(1, totalMonthlyBurn);
            adjustment += (int) Math.min(r.debtCap(), Math.round(debtShareOfBurn * r.debtRatioMultiplier()));
        }
        if (request.constraints().familyPressureLevel() >= r.familyPressureMinLevel()) {
            adjustment += r.familyPressureBonus();
        }
        if (runwayMonths < request.constraints().acceptableMonthsWithoutIncome()) {
            adjustment += r.runwayBelowAcceptablePenalty();
        }
        if (request.planB().willKeepJob()) {
            adjustment += r.sideHustleBonus();
        } else {
            adjustment += r.fullTimeLeapBonus();
        }
        int riskGap = 10 - psychology.riskTakingPotential();
        adjustment += Math.min(10, Math.max(0, riskGap));
        int founderGap = 10 - psychology.founderMindset();
        adjustment += Math.min(10, Math.max(0, founderGap)) * r.founderMindsetRiskPointsEach() / 10;
        return adjustment;
    }

    private Verdict mapVerdict(AnalyzeRequest request,
                               int feasibility,
                               int risk,
                               double runwayMonths,
                               double emergencyRunwayMonths) {
        var v = thresholds.verdict();
        double vetoRunway = request.planB().iWillQuitMyJob() ? emergencyRunwayMonths : runwayMonths;
        if (vetoRunway < v.hardVetoRunwayMonths()) {
            return Verdict.DO_NOT_TAKE_NOW;
        }
        if (feasibility >= v.takeLeapMinFeasibility() && risk <= v.takeLeapMaxRisk()) {
            return Verdict.TAKE_THE_LEAP;
        }
        if (feasibility >= v.cautionMinFeasibility() && risk <= v.cautionMaxRisk()) {
            return Verdict.TAKE_WITH_CAUTION;
        }
        if (feasibility >= v.delayMinFeasibility() || runwayMonths >= v.delayMinRunwayMonths()) {
            return Verdict.DELAY;
        }
        return Verdict.DO_NOT_TAKE_NOW;
    }

    private ConfidenceLevel confidenceLevel(AnalyzeRequest request,
                                            boolean researchAvailable,
                                            List<String> dataGaps,
                                            IncomeRoadmapValidator.IncomeRoadmapAssessment incomeRoadmap,
                                            ResearchContext researchContext,
                                            boolean resumeProvided,
                                            boolean resumeMarketValueAssessed) {
        boolean missingIncomeValidation = request.planB().expectedIncome3Months() == 0
                || request.planB().expectedIncome6Months() == 0;
        if (!researchAvailable && missingIncomeValidation) {
            return ConfidenceLevel.LOW;
        }
        boolean researchComplete = researchAvailable
                && researchContext != null
                && researchContext.hasWebSalaryData()
                && researchContext.hasPlanBMarketIncome();
        if (researchComplete && incomeRoadmap.available() && dataGaps.isEmpty()
                && (!resumeProvided || resumeMarketValueAssessed)) {
            return ConfidenceLevel.HIGH;
        }
        return ConfidenceLevel.MEDIUM;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private void addRunwayDataGaps(List<String> dataGaps, AnalyzeRequest request, double runwayMonths) {
        String classification = AnalyzeContextFields.classifyRunway(runwayMonths, runwayThresholds);
        int acceptableWithoutIncome = request.constraints().acceptableMonthsWithoutIncome();
        if (runwayMonths < acceptableWithoutIncome) {
            dataGaps.add("Computed runway is " + AnalyzeContextFields.roundMonths(runwayMonths) + " months ("
                    + classification + ") — below your acceptable "
                    + acceptableWithoutIncome + " months without income");
        }
        if (request.planB().iWillQuitMyJob()
                && runwayMonths < runwayThresholds.classificationModerateMaxMonths()) {
            dataGaps.add("Quitting for Plan B full-time with under "
                    + (int) runwayThresholds.classificationModerateMaxMonths()
                    + " months computed runway (" + classification + ")");
        }
    }
}
