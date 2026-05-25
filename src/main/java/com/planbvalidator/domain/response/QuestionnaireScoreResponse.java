package com.planbvalidator.domain.response;

import java.util.Map;

public record QuestionnaireScoreResponse(
        String riskProfile,
        Map<String, Integer> scores,
        String summary,
        int riskTakingPotential,
        int founderMindset
) {
}
