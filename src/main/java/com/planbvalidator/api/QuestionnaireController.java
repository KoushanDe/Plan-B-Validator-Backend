package com.planbvalidator.api;

import com.planbvalidator.domain.request.QuestionnaireScoreRequest;
import com.planbvalidator.domain.response.PsychologyQuestionsResponse;
import com.planbvalidator.domain.response.QuestionnaireScoreResponse;
import com.planbvalidator.psychology.PsychologyEngine;
import com.planbvalidator.psychology.PsychologyQuestion;
import com.planbvalidator.psychology.PsychologyQuestionCatalog;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/questionnaire")
public class QuestionnaireController {

    private final PsychologyEngine psychologyEngine;
    private final PsychologyQuestionCatalog questionCatalog;

    public QuestionnaireController(PsychologyEngine psychologyEngine, PsychologyQuestionCatalog questionCatalog) {
        this.psychologyEngine = psychologyEngine;
        this.questionCatalog = questionCatalog;
    }

    /**
     * Returns the 10 Likert questions for the UI (plain language). Submit answers using field names in POST /score or /analyze.
     */
    @GetMapping("/questions")
    public PsychologyQuestionsResponse questions() {
        List<PsychologyQuestionsResponse.QuestionItem> items = questionCatalog.questions().stream()
                .map(this::toItem)
                .toList();
        return new PsychologyQuestionsResponse(questionCatalog.scaleLabels(), items);
    }

    @PostMapping("/score")
    public QuestionnaireScoreResponse score(@Valid @RequestBody QuestionnaireScoreRequest request) {
        return psychologyEngine.score(request.psychology());
    }

    private PsychologyQuestionsResponse.QuestionItem toItem(PsychologyQuestion q) {
        return new PsychologyQuestionsResponse.QuestionItem(
                q.id(),
                q.field(),
                q.question(),
                q.invert(),
                1,
                5
        );
    }
}
