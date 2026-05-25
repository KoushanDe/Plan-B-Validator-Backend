package com.planbvalidator.reporting;

import com.planbvalidator.config.PlanBProperties;
import com.planbvalidator.domain.common.ConfidenceLevel;
import com.planbvalidator.domain.common.Verdict;
import com.planbvalidator.llm.LlmNarrativeResult;
import com.planbvalidator.market.MarketValueAssessment;
import com.planbvalidator.pipeline.AnalysisPipelineMemory;
import com.planbvalidator.research.MarketSentiment;
import com.planbvalidator.research.ResearchContext;
import com.planbvalidator.scoring.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportComposerTest {

    private final ReportComposer composer = new ReportComposer(
            new PlanBProperties(
                    new PlanBProperties.Research(true, "gemini", 1000, "", ""),
                    new PlanBProperties.OpenAi(true, "gpt", 1000, ""),
                    new PlanBProperties.Gemini(true, "gemini", 1000, ""),
                    new PlanBProperties.RateLimit(1, 5, 5),
                    "disclaimer"));

    @Test
    void shouldPreferWebPlanBNotesForRoiSummary() {
        ResearchContext research = new ResearchContext(
                "Summary", MarketSentiment.POSITIVE, "notes", List.of(),
                "₹60-80 LPA", "Web Plan B notes", List.of(), 70, false,
                "₹10-15 LPA", 6);
        MarketValueAssessment marketValue = new MarketValueAssessment(
                "strong", 80, "₹60 LPA", "", "Resume ROI", 70,
                List.of(), List.of(), List.of(), List.of(), List.of());
        ScoringResult scoring = sampleScoring();
        AnalysisPipelineMemory memory = new AnalysisPipelineMemory(
                com.planbvalidator.TestFixtures.sampleAnalyzeRequest());

        var response = composer.compose(
                "id", 1L, Map.of(), Map.of(), scoring,
                emptyNarrative(), research, null, marketValue, memory);

        assertEquals("Web Plan B notes", response.planBRoiSummary());
    }

    @Test
    void shouldKeepScoringDataGapsFirstWhenMerging() {
        ScoringResult scoring = new ScoringResult(
                Verdict.DELAY, 50, 50, 10, ConfidenceLevel.MEDIUM,
                new ScoreBreakdown(50, 50, 50, 50, 50),
                sampleOpportunityCost(),
                IncomeRoadmapValidator.IncomeRoadmapAssessment.unavailable(0, 0, 0, 6),
                List.of("Deterministic gap"),
                List.of("Deterministic gap"));
        LlmNarrativeResult narrative = new LlmNarrativeResult(
                "Summary", List.of(), List.of(), List.of(), List.of(), List.of("LLM gap"),
                "", "", "", "");

        var response = composer.compose(
                "id", 1L, Map.of(), Map.of(), scoring, narrative, null, null, null,
                new AnalysisPipelineMemory(com.planbvalidator.TestFixtures.sampleAnalyzeRequest()));

        assertEquals("Deterministic gap", response.dataGaps().getFirst());
        assertTrue(response.dataGaps().size() <= 7);
    }

    private static ScoringResult sampleScoring() {
        return new ScoringResult(
                Verdict.TAKE_WITH_CAUTION, 65, 40, 10, ConfidenceLevel.MEDIUM,
                new ScoreBreakdown(80, 60, 70, 55, 85),
                sampleOpportunityCost(),
                IncomeRoadmapValidator.IncomeRoadmapAssessment.unavailable(0, 0, 0, 6),
                List.of(), List.of());
    }

    private static OpportunityCostMetric sampleOpportunityCost() {
        return new OpportunityCostMetric(
                50, "moderate", 200_000, 100_000, 100_000, 100_000, 50.0,
                "web_research_market", "web_research_market", "full_time_leap",
                Map.of(), "summary");
    }

    private static LlmNarrativeResult emptyNarrative() {
        return new LlmNarrativeResult(
                "Summary", List.of(), List.of(), List.of(), List.of(), List.of(),
                "", "", "", "");
    }
}
