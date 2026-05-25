package com.planbvalidator.domain.response;

import java.util.List;
import java.util.Map;

public record PsychologyQuestionsResponse(
        Map<String, String> scale,
        List<QuestionItem> questions
) {
    public record QuestionItem(
            String id,
            String field,
            String question,
            boolean invertScoring,
            int minRating,
            int maxRating
    ) {
    }
}
