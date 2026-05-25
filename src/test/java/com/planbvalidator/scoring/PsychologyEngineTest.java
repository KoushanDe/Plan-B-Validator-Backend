package com.planbvalidator.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.config.PsychologyThresholdsProperties;
import com.planbvalidator.domain.request.PsychologyDto;
import com.planbvalidator.psychology.PsychologyEngine;
import com.planbvalidator.psychology.PsychologyQuestionCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PsychologyEngineTest {

    private final PsychologyEngine engine = newEngine();

    @Test
    void shouldScoreAndClassify() throws Exception {
        var response = engine.score(new PsychologyDto(4, 4, 4, 2, 2, 4, 4, 4, 4, 4));
        assertTrue(response.scores().get("uncertainty_tolerance") >= 70);
        assertTrue(response.riskProfile().equals("moderate_risk_taker") || response.riskProfile().equals("aggressive_experimenter"));
        assertTrue(response.riskTakingPotential() >= 0 && response.riskTakingPotential() <= 10);
        assertTrue(response.founderMindset() >= 0 && response.founderMindset() <= 10);
    }

    @Test
    void shouldExposeTenDimensions() throws Exception {
        var response = engine.score(new PsychologyDto(3, 3, 3, 3, 3, 3, 3, 3, 3, 3));
        assertEquals(10, response.scores().size());
    }

    private static PsychologyEngine newEngine() {
        try {
            var catalog = new PsychologyQuestionCatalog(new ObjectMapper());
            return new PsychologyEngine(PsychologyThresholdsProperties.defaults(), catalog);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
