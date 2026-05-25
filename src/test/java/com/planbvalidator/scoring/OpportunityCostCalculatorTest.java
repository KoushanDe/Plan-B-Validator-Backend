package com.planbvalidator.scoring;

import com.planbvalidator.config.CurrencyRatesProperties;
import com.planbvalidator.domain.request.*;
import com.planbvalidator.market.MarketValueAssessment;
import com.planbvalidator.research.MarketSentiment;
import com.planbvalidator.research.ResearchContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpportunityCostCalculatorTest {

    private final SalaryRangeParser salaryRangeParser = new SalaryRangeParser(new CurrencyRatesProperties(null));
    private final OpportunityCostCalculator calculator = new OpportunityCostCalculator(salaryRangeParser);

    @Test
    void shouldParseLpaRangeForCorporateBaseline() {
        var parsed = salaryRangeParser.monthlyMidpointInr("₹38-48 LPA");
        assertTrue(parsed.isPresent());
        assertEquals(358333.3, parsed.get(), 500.0);
    }

    @Test
    void fullTimeLeapWithLargeIncomeGapShouldScoreHigh() {
        AnalyzeRequest request = baseRequest(true, 10_000, 50_000, 80_000);
        MarketValueAssessment marketValue = new MarketValueAssessment(
                "strong", 80, "₹60-80 LPA", "", "", 78, List.of(), List.of(),
                List.of(), List.of(), List.of());
        ResearchContext research = new ResearchContext(
                "Summary", MarketSentiment.NEUTRAL, "notes", List.of(),
                "₹60-80 LPA", "", List.of(), 70, false);

        OpportunityCostMetric metric = calculator.compute(request, marketValue, research);

        assertTrue(metric.score() >= 55, "Large gap + quit job should raise opportunity cost");
        assertEquals("full_time_leap", metric.engagementMode());
        assertEquals("web_research_market", metric.corporateBaselineSource());
    }

    @Test
    void sideHustleWithStrongPlanBIncomeShouldScoreLowerThanFullTimeLeap() {
        AnalyzeRequest sideHustle = baseRequest(false, 120_000, 150_000, 180_000);
        AnalyzeRequest fullLeap = baseRequest(true, 120_000, 150_000, 180_000);
        MarketValueAssessment marketValue = new MarketValueAssessment(
                "moderate", 55, "₹25-35 LPA", "", "", 40, List.of(), List.of(),
                List.of(), List.of(), List.of());

        int sideScore = calculator.compute(sideHustle, marketValue, null).score();
        int leapScore = calculator.compute(fullLeap, marketValue, null).score();

        assertTrue(sideScore < leapScore);
        assertEquals("side_hustle", calculator.compute(sideHustle, marketValue, null).engagementMode());
    }

    private static AnalyzeRequest baseRequest(boolean quitJob,
                                              double income3,
                                              double income6,
                                              double income12) {
        return new AnalyzeRequest(
                new ProfileDto("Engineer", "Fintech", 6.0, "India", "Bengaluru"),
                new FinancialsDto(200_000.0, 1_000_000.0, 80_000.0, 0, 0.0),
                new PlanBDto("Consulting", "Independent practice", "Autonomy", 12,
                        income3, income6, income12, quitJob, null, null),
                new ConstraintsDto("Clients", "Income", "Delay", 120_000.0, 4, 2),
                new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4),
                null
        );
    }
}
