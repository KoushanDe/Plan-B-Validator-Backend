package com.planbvalidator.scoring;

import com.planbvalidator.TestScoringSupport;
import com.planbvalidator.domain.common.ConfidenceLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.research.MarketSentiment;
import com.planbvalidator.research.ResearchContext;
import com.planbvalidator.psychology.PsychologyEngine;
import com.planbvalidator.psychology.PsychologyQuestionCatalog;
import com.planbvalidator.config.PsychologyThresholdsProperties;
import com.planbvalidator.domain.request.*;
import com.planbvalidator.market.MarketValueAssessment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringEngineTest {

    private final ScoringEngine engine = TestScoringSupport.scoringEngine();
    private final PsychologyEngine psychologyEngine = newPsychologyEngine();

    private static PsychologyEngine newPsychologyEngine() {
        try {
            return new PsychologyEngine(PsychologyThresholdsProperties.defaults(), new PsychologyQuestionCatalog(new ObjectMapper()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldComputeFeasibilityRiskAndVerdict() {
        var psychology = psychologyEngine.score(new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4));

        AnalyzeRequest request = new AnalyzeRequest(
                new ProfileDto("Backend Engineer", "Fintech", 2.5, "India", "Bengaluru"),
                new FinancialsDto(180000.0, 900000.0, 65000.0, 0, 0.0),
                new PlanBDto("Freelance AI", "Build freelancing", "Autonomy", 6,
                        20000.0, 60000.0, 120000.0, false, null, null),
                new ConstraintsDto("Stable clients", "Income instability", "Delay", 100000.0, 4, 3),
                new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4),
                new ResearchOptionsDto(true)
        );

        MarketValueAssessment marketValue = new MarketValueAssessment(
                "strong", 75, "₹35-50 LPA", "Strong corporate path", "Plan B ROI plausible", 45,
                List.of("IIT"), List.of(),
                List.of("Flipkart"), List.of("SDE3"), List.of("SDE3 salary Bengaluru")
        );

        ResearchContext research = new ResearchContext(
                "Demand moderate",
                MarketSentiment.MODERATELY_POSITIVE,
                "AmbitionBox shows ₹38-48 LPA",
                List.of("competition"),
                "₹38-48 LPA",
                "Freelance AI demand growing",
                List.of("AmbitionBox"),
                72,
                false
        );

        var result = engine.compute(request, 13.8, 13.8, psychology, marketValue, research, true, true, true, null);
        assertTrue(result.feasibilityScore() > 50);
        assertTrue(result.riskScore() < 70);
    }

    @Test
    void shouldFlagSalaryDisagreementInDataGaps() {
        var psychology = psychologyEngine.score(new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4));
        AnalyzeRequest request = new AnalyzeRequest(
                new ProfileDto("Engineer", "Tech", 5.0, "India", "Mumbai"),
                new FinancialsDto(200000.0, 0.0, 80000.0, 0, 0.0),
                new PlanBDto("Startup", "Build product", "Growth", 12,
                        0.0, 50000.0, 100000.0, true, null, null),
                new ConstraintsDto("Runway", "Risk", "Wait", 80000.0, 3, 3),
                new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4),
                new ResearchOptionsDto(true)
        );
        MarketValueAssessment marketValue = new MarketValueAssessment(
                "strong", 80, "₹60-80 LPA", "", "", 80, List.of(), List.of(),
                List.of(), List.of(), List.of()
        );
        ResearchContext research = new ResearchContext(
                "Summary", MarketSentiment.NEUTRAL, "notes", List.of(),
                "₹35-45 LPA", "", List.of("AmbitionBox"), 55, true
        );

        var result = engine.compute(request, 8, 8, psychology, marketValue, research, true, true, true, null);
        assertTrue(result.dataGaps().stream().anyMatch(g -> g.contains("differs from resume-inferred")));
    }

    @Test
    void lowRiskTakingShouldReduceEffectiveRunwayVersusHighRiskTaking() {
        var anxious = psychologyEngine.score(new PsychologyDto(2, 3, 2, 5, 2, 3, 2, 2, 2, 2));
        var resilient = psychologyEngine.score(new PsychologyDto(5, 4, 5, 2, 2, 4, 5, 5, 5, 5));

        double rawRunway = 12.0;
        double effectiveAnxious = engine.effectiveRunwayMonths(rawRunway, anxious);
        double effectiveResilient = engine.effectiveRunwayMonths(rawRunway, resilient);

        assertTrue(effectiveResilient > effectiveAnxious,
                "Higher risk-taking should increase effective runway months for the same savings");
    }

    @Test
    void highOpportunityCostShouldLowerMarketScoreAndRaiseRisk() {
        var psychology = psychologyEngine.score(new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4));
        AnalyzeRequest highSacrifice = leapRequest(10_000, 30_000, 50_000);
        AnalyzeRequest lowSacrifice = leapRequest(400_000, 450_000, 500_000);

        MarketValueAssessment marketValue = new MarketValueAssessment(
                "strong", 85, "₹60-80 LPA", "", "", 80, List.of(), List.of(),
                List.of(), List.of(), List.of());
        ResearchContext research = new ResearchContext(
                "Summary", MarketSentiment.NEUTRAL, "notes", List.of(),
                "₹60-80 LPA", "Plan B notes", List.of("AmbitionBox"), 70, false);

        var highCost = engine.compute(highSacrifice, 18, 18, psychology, null, research, true, false, false, null);
        var lowCost = engine.compute(lowSacrifice, 18, 18, psychology, null, research, true, false, false, null);

        assertTrue(highCost.opportunityCost().score() > lowCost.opportunityCost().score());
        assertTrue(highCost.scoreBreakdown().marketFeasibility() < lowCost.scoreBreakdown().marketFeasibility());
        assertTrue(highCost.opportunityCost().score() >= 55);
    }

    @Test
    void shouldNotUseMinimumSalaryBonusWhenResearchAvailableWithoutRoadmap() {
        var psychology = psychologyEngine.score(new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4));
        AnalyzeRequest request = new AnalyzeRequest(
                new ProfileDto("Engineer", "Tech", 4.0, "India", "Bengaluru"),
                new FinancialsDto(150_000.0, 1_200_000.0, 60_000.0, 0, 0.0),
                new PlanBDto("Consulting", "Independent", "Autonomy", 12,
                        20_000.0, 120_000.0, 150_000.0, false, null, null),
                new ConstraintsDto("Clients", "Income", "Delay", 100_000.0, 6, 2),
                new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4),
                new ResearchOptionsDto(true)
        );
        ResearchContext researchWithoutPlanBIncome = new ResearchContext(
                "Summary", MarketSentiment.NEUTRAL, "notes", List.of(),
                "₹38-48 LPA", "notes", List.of("AmbitionBox"), 65, false);

        var withResearch = engine.compute(request, 20, 20, psychology, null, researchWithoutPlanBIncome, true, false, false, null);
        var withoutResearch = engine.compute(request, 20, 20, psychology, null, null, false, false, false, null);

        assertTrue(withResearch.scoreBreakdown().marketFeasibility() <= withoutResearch.scoreBreakdown().marketFeasibility());
    }

    @Test
    void confidenceShouldFollowTierRules() {
        var psychology = psychologyEngine.score(new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4));
        AnalyzeRequest base = new AnalyzeRequest(
                new ProfileDto("Engineer", "Tech", 6.0, "India", "Bengaluru"),
                new FinancialsDto(580_000.0, 2_400_000.0, 70_000.0, 0, 0.0),
                new PlanBDto("Consulting", "Independent", "Autonomy", 12,
                        10_000.0, 50_000.0, 125_000.0, false, null, null),
                new ConstraintsDto("Clients", "Income", "Delay", 120_000.0, 6, 2),
                new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4),
                new ResearchOptionsDto(true)
        );
        MarketValueAssessment marketValue = new MarketValueAssessment(
                "strong", 80, "₹60-80 LPA", "", "", 50, List.of(), List.of(),
                List.of(), List.of(), List.of());
        ResearchContext completeResearch = new ResearchContext(
                "Summary", MarketSentiment.POSITIVE, "notes", List.of(),
                "₹60-80 LPA", "Plan B notes", List.of("AmbitionBox"), 75, false,
                "₹12-18 LPA", 6);

        AnalyzeRequest missingEarlyIncome = new AnalyzeRequest(
                base.profile(), base.financials(),
                new PlanBDto("Consulting", "Independent", "Autonomy", 12,
                        0.0, 100_000.0, 150_000.0, false, null, null),
                base.constraints(), base.psychology(), base.researchOptions());
        var low = engine.compute(missingEarlyIncome, 24, 24, psychology, marketValue, null, false, false, false, null);
        assertEquals(ConfidenceLevel.LOW, low.confidence());

        var medium = engine.compute(base, 24, 24, psychology, marketValue, completeResearch, true, false, false, null);
        assertEquals(ConfidenceLevel.MEDIUM, medium.confidence());

        var high = engine.compute(base, 24, 24, psychology, marketValue, completeResearch, true, true, true, null);
        assertEquals(ConfidenceLevel.HIGH, high.confidence());
    }

    private static AnalyzeRequest leapRequest(double income3, double income6, double income12) {
        return new AnalyzeRequest(
                new ProfileDto("Engineer", "Fintech", 6.0, "India", "Bengaluru"),
                new FinancialsDto(200_000.0, 1_800_000.0, 80_000.0, 0, 0.0),
                new PlanBDto("Consulting", "Independent practice", "Autonomy", 12,
                        income3, income6, income12, true, null, null),
                new ConstraintsDto("Clients", "Income", "Delay", 120_000.0, 6, 2),
                new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4),
                new ResearchOptionsDto(true)
        );
    }
}
