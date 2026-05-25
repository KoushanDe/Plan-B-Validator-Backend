package com.planbvalidator.llm;

import java.util.List;

/** Structured narrative fields produced by OpenAI (concise decision support). */
public record CoreNarrative(
        String recommendationSummary,
        List<String> majorReasons,
        List<String> redFlags,
        List<String> nextSteps,
        List<String> assumptions,
        List<String> dataGaps
) {
}
