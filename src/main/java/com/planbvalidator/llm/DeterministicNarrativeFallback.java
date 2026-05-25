package com.planbvalidator.llm;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DeterministicNarrativeFallback {

    public CoreNarrative coreFallback(LlmReasoningInput input) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Runway months: " + input.runwayMonths());
        reasons.add("Feasibility score: " + input.feasibilityScore() + " / 100");
        reasons.add("Risk score: " + input.riskScore() + " / 100");

        List<String> redFlags = new ArrayList<>();
        if (input.runwayMonths() < 6) {
            redFlags.add("Runway is below 6 months");
        }
        if (input.scoreBreakdown().marketFeasibility() < 50) {
            redFlags.add("Market feasibility signal is weak");
        }

        String summary = switch (input.verdict()) {
            case TAKE_THE_LEAP -> "Conditions look workable, but validate demand before committing fully.";
            case TAKE_WITH_CAUTION -> "Proceed in phases while protecting income and measuring traction.";
            case DELAY -> "Delay the full leap until runway or validation improves.";
            case DO_NOT_TAKE_NOW -> "Current constraints suggest waiting and strengthening your safety net first.";
        };

        return new CoreNarrative(
                summary,
                reasons,
                redFlags,
                List.of(
                        "Define minimum traction criteria before a full transition",
                        "Run a 30-day validation cycle with measurable milestones",
                        "Reassess after collecting real customer or hiring signals"
                ),
                JsonResponseParser.emptyIfNull(input.assumptions()),
                JsonResponseParser.emptyIfNull(input.dataGaps())
        );
    }

    public DeepNarrative deepFallback(LlmReasoningInput input) {
        return new DeepNarrative(
                input.psychologySummary(),
                "Slow demand validation or income ramp may extend the transition timeline.",
                "Keep stable income while testing demand incrementally.",
                "Pause the leap and revisit after improving runway or proof of demand."
        );
    }
}
