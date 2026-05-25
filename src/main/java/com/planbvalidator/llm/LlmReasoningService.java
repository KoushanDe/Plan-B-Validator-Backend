package com.planbvalidator.llm;

import com.planbvalidator.domain.common.ConfidenceLevel;
import com.planbvalidator.pipeline.AnalysisPipelineMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates OpenAI (core) then Gemini (deep), each using compact pipeline memory slices.
 */
@Service
public class LlmReasoningService {

    private static final Logger log = LoggerFactory.getLogger(LlmReasoningService.class);

    private final OpenAiCoreNarrativeService openAiCore;
    private final GeminiDeepNarrativeService geminiDeep;
    private final DeterministicNarrativeFallback fallback;

    public LlmReasoningService(OpenAiCoreNarrativeService openAiCore,
                               GeminiDeepNarrativeService geminiDeep,
                               DeterministicNarrativeFallback fallback) {
        this.openAiCore = openAiCore;
        this.geminiDeep = geminiDeep;
        this.fallback = fallback;
    }

    public NarrativeGenerationResult generate(AnalysisPipelineMemory memory) {
        Map<String, String> status = new LinkedHashMap<>();
        long openAiMs = 0;
        long geminiMs = 0;

        LlmReasoningInput input = buildInput(memory);

        long t0 = System.currentTimeMillis();
        log.info("llm event=openai_core_started configured={}", openAiCore.isConfigured());
        Optional<CoreNarrative> core = openAiCore.generateCore(memory);
        openAiMs = System.currentTimeMillis() - t0;
        status.put("openai", resolveStatus(openAiCore.isConfigured(), core.isPresent()));
        log.info("llm event=openai_core_finished durationMs={} success={}", openAiMs, core.isPresent());

        boolean coreFromOpenAi = core.isPresent();
        CoreNarrative coreNarrative = core.orElseGet(() -> fallback.coreFallback(input));
        if (!coreFromOpenAi && openAiCore.isConfigured()) {
            status.put("openai", "failed");
        }
        memory.setCoreNarrative(coreNarrative);

        t0 = System.currentTimeMillis();
        Optional<DeepNarrative> deep = Optional.empty();
        boolean runGemini = shouldRunGeminiDeep(memory);
        log.info("llm event=gemini_deep_started configured={} willRun={} confidence={}",
                geminiDeep.isConfigured(), runGemini, memory.scoring().confidence());
        if (runGemini) {
            GeminiDeepContext ctx = new GeminiDeepContext(
                    memory,
                    memory.research(),
                    coreNarrative,
                    memory.marketValue(),
                    coreFromOpenAi
            );
            deep = geminiDeep.generateDeep(ctx);
        }
        geminiMs = System.currentTimeMillis() - t0;
        status.put("gemini", resolveStatus(geminiDeep.isConfigured(), deep.isPresent()));
        log.info("llm event=gemini_deep_finished durationMs={} success={} status={}",
                geminiMs, deep.isPresent(), status.get("gemini"));

        DeepNarrative deepNarrative = deep.orElseGet(() -> fallback.deepFallback(input));
        if (runGemini && deep.isEmpty() && geminiDeep.isConfigured()) {
            status.put("gemini", "failed");
        } else if (!runGemini) {
            status.put("gemini", geminiDeep.isConfigured() ? "skipped_low_priority" : "skipped_not_configured");
        }

        return new NarrativeGenerationResult(merge(coreNarrative, deepNarrative), status, openAiMs, geminiMs);
    }

    private static LlmReasoningInput buildInput(AnalysisPipelineMemory memory) {
        var scoring = memory.scoring();
        var psych = memory.psychology();
        return new LlmReasoningInput(
                scoring.verdict(),
                scoring.feasibilityScore(),
                scoring.riskScore(),
                scoring.runwayMonths(),
                scoring.confidence(),
                scoring.scoreBreakdown(),
                psych.summary(),
                psych.riskProfile(),
                memory.research(),
                scoring.assumptions(),
                scoring.dataGaps()
        );
    }

    private boolean shouldRunGeminiDeep(AnalysisPipelineMemory memory) {
        if (!geminiDeep.isConfigured()) {
            return false;
        }
        ConfidenceLevel confidence = memory.scoring().confidence();
        return switch (confidence) {
            case LOW, MEDIUM -> true;
            case HIGH -> memory.research() != null
                    && memory.research().marketSummary() != null
                    && !memory.research().marketSummary().isBlank();
        };
    }

    private static String resolveStatus(boolean configured, boolean success) {
        if (!configured) {
            return "not_configured";
        }
        return success ? "success" : "failed";
    }

    private static LlmNarrativeResult merge(CoreNarrative core, DeepNarrative deep) {
        return new LlmNarrativeResult(
                core.recommendationSummary(),
                JsonResponseParser.emptyIfNull(core.majorReasons()),
                JsonResponseParser.emptyIfNull(core.redFlags()),
                JsonResponseParser.emptyIfNull(core.nextSteps()),
                JsonResponseParser.emptyIfNull(core.assumptions()),
                JsonResponseParser.emptyIfNull(core.dataGaps()),
                deep.personalitySummary(),
                deep.expectedFailureMode(),
                deep.safestNextMove(),
                deep.suggestedFallbackPlan()
        );
    }
}
