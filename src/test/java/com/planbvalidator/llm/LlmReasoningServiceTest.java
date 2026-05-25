package com.planbvalidator.llm;

import com.planbvalidator.domain.common.ConfidenceLevel;
import com.planbvalidator.domain.common.Verdict;
import com.planbvalidator.domain.request.*;
import com.planbvalidator.domain.response.QuestionnaireScoreResponse;
import com.planbvalidator.pipeline.AnalysisPipelineMemory;
import com.planbvalidator.scoring.ScoreBreakdown;
import com.planbvalidator.scoring.IncomeRoadmapValidator;
import com.planbvalidator.scoring.OpportunityCostMetric;
import com.planbvalidator.scoring.ScoringResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmReasoningServiceTest {

    @Mock
    private OpenAiCoreNarrativeService openAiCore;

    @Mock
    private GeminiDeepNarrativeService geminiDeep;

    @Mock
    private DeterministicNarrativeFallback fallback;

    @InjectMocks
    private LlmReasoningService service;

    @Test
    void shouldMergeCoreAndDeepNarratives() {
        AnalysisPipelineMemory memory = sampleMemory(ConfidenceLevel.MEDIUM);

        when(openAiCore.isConfigured()).thenReturn(true);
        when(openAiCore.generateCore(memory)).thenReturn(Optional.of(
                new CoreNarrative("Summary", List.of("reason"), List.of(), List.of("step"), List.of(), List.of())
        ));
        when(geminiDeep.isConfigured()).thenReturn(true);
        when(geminiDeep.generateDeep(any(GeminiDeepContext.class))).thenReturn(Optional.of(
                new DeepNarrative("personality", "failure", "safest", "fallback")
        ));

        NarrativeGenerationResult result = service.generate(memory);

        assertEquals("Summary", result.narrative().recommendationSummary());
        assertEquals("success", result.providerStatus().get("openai"));
    }

    private static AnalysisPipelineMemory sampleMemory(ConfidenceLevel confidence) {
        AnalyzeRequest request = new AnalyzeRequest(
                new ProfileDto("Engineer", "Tech", 3.0, "India", "Mumbai"),
                new FinancialsDto(100000.0, 500000.0, 50000.0, 0, 0.0),
                new PlanBDto("Startup", "Build product", "Growth", 12,
                        0.0, 50000.0, 100000.0, false, null, null),
                new ConstraintsDto("Revenue", "Fail", "Delay", 80000.0, 6, 2),
                new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4),
                null
        );
        AnalysisPipelineMemory memory = new AnalysisPipelineMemory(request);
        memory.setPsychology(new QuestionnaireScoreResponse("moderate_risk_taker", Map.of(), "summary", 6, 6));
        memory.setScoring(new ScoringResult(
                Verdict.TAKE_WITH_CAUTION, 65, 40, 10.0, confidence,
                new ScoreBreakdown(80, 60, 70, 55, 85),
                sampleOpportunityCost(),
                IncomeRoadmapValidator.IncomeRoadmapAssessment.unavailable(0, 50_000, 100_000, 12),
                List.of("assumption"), List.of("gap")
        ));
        return memory;
    }

    private static OpportunityCostMetric sampleOpportunityCost() {
        return new OpportunityCostMetric(
                50,
                "moderate",
                200_000,
                100_000,
                100_000,
                100_000,
                50.0,
                "stated_current_income",
                "user_projection",
                "side_hustle",
                Map.of("income_sacrifice", 50, "trajectory_premium", 50, "experience_lock_in", 42, "engagement_adjustment", 10),
                "Sample opportunity cost"
        );
    }
}
