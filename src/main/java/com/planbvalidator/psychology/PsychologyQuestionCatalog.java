package com.planbvalidator.psychology;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.domain.request.PsychologyDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class PsychologyQuestionCatalog {

    private final List<PsychologyQuestion> questions;
    private final Map<String, String> scaleLabels;
    private final Map<String, Function<PsychologyDto, Integer>> valueExtractors;

    public PsychologyQuestionCatalog(ObjectMapper objectMapper) throws IOException {
        try (InputStream in = new ClassPathResource("psychology/questionnaire.json").getInputStream()) {
            QuestionnaireFile file = objectMapper.readValue(in, QuestionnaireFile.class);
            this.questions = List.copyOf(file.questions());
            this.scaleLabels = Map.copyOf(file.scale());
        }
        this.valueExtractors = buildExtractors();
        if (questions.size() != 10) {
            throw new IllegalStateException("Psychology questionnaire must define exactly 10 questions, found " + questions.size());
        }
    }

    public List<PsychologyQuestion> questions() {
        return questions;
    }

    public Map<String, String> scaleLabels() {
        return scaleLabels;
    }

    public int answerFor(PsychologyQuestion question, PsychologyDto dto) {
        Function<PsychologyDto, Integer> extractor = valueExtractors.get(question.field());
        if (extractor == null) {
            throw new IllegalStateException("No PsychologyDto mapping for field: " + question.field());
        }
        return extractor.apply(dto);
    }

    private Map<String, Function<PsychologyDto, Integer>> buildExtractors() {
        Map<String, Function<PsychologyDto, Integer>> map = new LinkedHashMap<>();
        map.put("uncertaintyTolerance", PsychologyDto::uncertaintyTolerance);
        map.put("discipline", PsychologyDto::discipline);
        map.put("stressRecovery", PsychologyDto::stressRecovery);
        map.put("validationDependency", PsychologyDto::validationDependency);
        map.put("impulsiveness", PsychologyDto::impulsiveness);
        map.put("routineAdherence", PsychologyDto::routineAdherence);
        map.put("setbackRecovery", PsychologyDto::setbackRecovery);
        map.put("uncertaintyStamina", PsychologyDto::uncertaintyStamina);
        map.put("financialResilience", PsychologyDto::financialResilience);
        map.put("selfDirectedMotivation", PsychologyDto::selfDirectedMotivation);
        return Map.copyOf(map);
    }

    private record QuestionnaireFile(Map<String, String> scale, List<PsychologyQuestion> questions) {
    }
}
