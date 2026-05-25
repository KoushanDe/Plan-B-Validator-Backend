package com.planbvalidator.scoring;

import com.planbvalidator.TestScoringSupport;
import com.planbvalidator.domain.common.Verdict;
import com.planbvalidator.domain.request.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.psychology.PsychologyEngine;
import com.planbvalidator.psychology.PsychologyQuestionCatalog;
import com.planbvalidator.config.PsychologyThresholdsProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerdictMapperTest {

    @Test
    void shouldReturnDoNotTakeNowWhenRunwaySevere() throws Exception {
        var engine = TestScoringSupport.scoringEngine();
        var request = new AnalyzeRequest(
                new ProfileDto("Engineer", "IT", 2.0, "India", "Delhi"),
                new FinancialsDto(50000.0, 20000.0, 30000.0, 0, 0.0),
                new PlanBDto("Switch", "Switching", "Growth", 4,
                        0.0, 10000.0, 20000.0, true, null, null),
                new ConstraintsDto("Income", "Failure", "Delay", 40000.0, 3, 2),
                new PsychologyDto(3, 3, 3, 3, 3, 3, 3, 3, 3, 3),
                new ResearchOptionsDto(false)
        );

        var psych = new PsychologyEngine(PsychologyThresholdsProperties.defaults(),
                new PsychologyQuestionCatalog(new ObjectMapper())).score(request.psychology());
        var result = engine.compute(request, 2.0, 2.0, psych, null, null, false, false, false, null);
        assertEquals(Verdict.DO_NOT_TAKE_NOW, result.verdict());
    }
}
