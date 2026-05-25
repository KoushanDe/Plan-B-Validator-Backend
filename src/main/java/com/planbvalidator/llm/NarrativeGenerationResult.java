package com.planbvalidator.llm;

import java.util.Map;

public record NarrativeGenerationResult(
        LlmNarrativeResult narrative,
        Map<String, String> providerStatus,
        long openAiMs,
        long geminiMs
) {
}
