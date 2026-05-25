package com.planbvalidator.llm;

/** Long-form explanatory fields produced by Gemini (nuanced depth). */
public record DeepNarrative(
        String personalitySummary,
        String expectedFailureMode,
        String safestNextMove,
        String suggestedFallbackPlan
) {
}
