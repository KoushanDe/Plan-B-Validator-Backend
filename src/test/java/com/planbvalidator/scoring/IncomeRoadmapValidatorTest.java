package com.planbvalidator.scoring;

import com.planbvalidator.config.CurrencyRatesProperties;
import com.planbvalidator.config.IncomeRampProperties;
import com.planbvalidator.domain.request.*;
import com.planbvalidator.market.MarketValueAssessment;
import com.planbvalidator.research.MarketSentiment;
import com.planbvalidator.research.ResearchContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncomeRoadmapValidatorTest {

    private IncomeRoadmapValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IncomeRoadmapValidator(
                new SalaryRangeParser(new CurrencyRatesProperties(null)),
                IncomeRampProperties.defaults());
    }

    @Test
    void optimisticRoadmapShouldScoreLowerThanConservativeRoadmap() {
        AnalyzeRequest optimistic = baseRequest(150_000, 200_000, 250_000, 3);
        AnalyzeRequest conservative = baseRequest(10_000, 50_000, 80_000, 9);
        ResearchContext research = new ResearchContext(
                "Summary", MarketSentiment.POSITIVE, "notes", List.of(),
                "₹60-80 LPA", "Agency demand moderate", List.of("Glassdoor"),
                70, false, "₹8-12 LPA", 6);

        int optimisticScore = validator.assess(optimistic, research).realismScore();
        int conservativeScore = validator.assess(conservative, research).realismScore();

        assertTrue(conservativeScore > optimisticScore);
        assertTrue(validator.assess(optimistic, research).timelineAdjustment() < 0);
    }

    @Test
    void shouldUseMarketPlanBIncomeForOpportunityCost() {
        SalaryRangeParser parser = new SalaryRangeParser(new CurrencyRatesProperties(null));
        OpportunityCostCalculator calculator = new OpportunityCostCalculator(parser);
        AnalyzeRequest request = baseRequest(0, 150_000, 150_000, 6);
        ResearchContext research = new ResearchContext(
                "Summary", MarketSentiment.NEUTRAL, "notes", List.of(),
                "₹60-80 LPA", "notes", List.of("AmbitionBox"),
                70, false, "₹10-15 LPA", 6);
        MarketValueAssessment marketValue = new MarketValueAssessment(
                "strong", 80, "₹60-80 LPA", "", "", 70, List.of(), List.of(),
                List.of(), List.of(), List.of());

        OpportunityCostMetric metric = calculator.compute(request, marketValue, research);

        assertEquals("web_research_market", metric.planBIncomeSource());
        assertEquals("web_research_market", metric.corporateBaselineSource());
        assertTrue(metric.monthlyPlanBAtHorizon() < metric.userProjectedPlanBAtHorizon());
        assertTrue(metric.score() >= 55);
    }

    private static AnalyzeRequest baseRequest(double i3, double i6, double i12, int timeline) {
        return new AnalyzeRequest(
                new ProfileDto("Engineer", "Fintech", 3.0, "India", "Bengaluru"),
                new FinancialsDto(180_000.0, 1_000_000.0, 80_000.0, 0, 0.0),
                new PlanBDto("AI agency", "Voice AI", "Autonomy", timeline,
                        i3, i6, i12, true, "UAE", "Dubai"),
                new ConstraintsDto("Clients", "Income", "Delay", 120_000.0, 4, 2),
                new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4),
                null
        );
    }
}
