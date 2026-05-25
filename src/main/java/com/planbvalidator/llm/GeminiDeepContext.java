package com.planbvalidator.llm;

import com.planbvalidator.market.MarketValueAssessment;
import com.planbvalidator.pipeline.AnalysisPipelineMemory;
import com.planbvalidator.research.ResearchContext;

/**
 * Context for Gemini deep narrative: synthesizes web research, OpenAI core narrative, resume market value, and scores.
 */
public record GeminiDeepContext(
        AnalysisPipelineMemory memory,
        ResearchContext internetResearch,
        CoreNarrative openAiCoreNarrative,
        MarketValueAssessment marketValue,
        boolean openAiCoreFromProvider
) {
}
