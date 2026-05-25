package com.planbvalidator.psychology;

import com.planbvalidator.config.PsychologyThresholdsProperties;
import com.planbvalidator.domain.request.PsychologyDto;
import com.planbvalidator.domain.response.QuestionnaireScoreResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PsychologyEngine {

    private final PsychologyThresholdsProperties thresholds;
    private final PsychologyQuestionCatalog catalog;

    public PsychologyEngine(PsychologyThresholdsProperties thresholds, PsychologyQuestionCatalog catalog) {
        this.thresholds = thresholds;
        this.catalog = catalog;
    }

    public QuestionnaireScoreResponse score(PsychologyDto dto) {
        Map<String, Integer> scored = new LinkedHashMap<>();
        for (PsychologyQuestion question : catalog.questions()) {
            int raw = catalog.answerFor(question, dto);
            scored.put(question.id(), normalize(raw, question.invert()));
        }

        int average = (int) Math.round(scored.values().stream().mapToInt(i -> i).average().orElse(0));
        int riskTaking = compositeScore(scored, thresholds.riskTakingDimensions());
        int founderMindset = compositeScore(scored, thresholds.founderMindsetDimensions());

        String profile = profile(average);
        String summary = buildSummary(profile, riskTaking, founderMindset);

        return new QuestionnaireScoreResponse(profile, scored, summary, riskTaking, founderMindset);
    }

    private int compositeScore(Map<String, Integer> scored, List<String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return 5;
        }
        double avg = dimensions.stream()
                .mapToInt(d -> scored.getOrDefault(d, 50))
                .average()
                .orElse(50);
        return clamp0to10((int) Math.round(avg / 10.0));
    }

    private String profile(int average) {
        var bands = thresholds.profileBands();
        if (average < bands.conservativeMax()) {
            return "conservative";
        }
        if (average > bands.aggressiveMin()) {
            return "aggressive_experimenter";
        }
        return "moderate_risk_taker";
    }

    private static String buildSummary(String profile, int riskTaking, int founderMindset) {
        String base = switch (profile) {
            case "conservative" -> "You may prefer lower volatility and stronger safety margins before transitioning.";
            case "aggressive_experimenter" -> "You appear resilient in uncertainty and may handle iterative experimentation better.";
            default -> "You can handle moderate uncertainty but should maintain structure during prolonged transition phases.";
        };
        return base + " Risk-taking potential " + riskTaking + "/10, founder mindset " + founderMindset + "/10.";
    }

    private static int normalize(int raw, boolean invert) {
        int scaled = (int) Math.round(((raw - 1) / 4.0) * 100);
        return invert ? 100 - scaled : scaled;
    }

    private static int clamp0to10(int value) {
        return Math.max(0, Math.min(10, value));
    }
}
