package com.planbvalidator.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.TestFixtures;
import com.planbvalidator.domain.request.AnalyzeRequest;
import com.planbvalidator.market.MarketValueAssessment;
import com.planbvalidator.pipeline.AnalysisPipelineMemory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResearchQueryBuilderTest {

    private final ResearchQueryBuilder builder = new ResearchQueryBuilder(new ObjectMapper());

    @Test
    void shouldIncludeResumeSignalsAndPreliminarySalaryForResearch() throws Exception {
        AnalyzeRequest request = TestFixtures.sampleAnalyzeRequest();
        AnalysisPipelineMemory memory = new AnalysisPipelineMemory(request);
        memory.setRunwayMonths(8.5);
        memory.setResumeText("Worked at Google as Staff Engineer");
        memory.setMarketValue(new MarketValueAssessment(
                "elite", 85, "₹55-70 LPA", "Strong path", "ROI ok", 70,
                List.of("Google", "Staff Engineer"),
                List.of(),
                List.of("Google"),
                List.of("Staff Engineer"),
                List.of("Staff Engineer salary Bengaluru AmbitionBox")
        ));

        String json = builder.buildUserPayload(memory);
        assertTrue(json.contains("Google"));
        assertTrue(json.contains("Staff Engineer"));
        assertTrue(json.contains("comp_search_queries"));
        assertTrue(json.contains("resume_inferred_salary_range_preliminary"));
        assertFalse(json.contains("\"estimated_salary_range\""));
        assertTrue(json.contains("plan_b_reason"));
        assertTrue(json.contains("success_definition"));
        assertTrue(json.contains("runway_months"));
    }

    @Test
    void shouldIncludeSeparateCurrentAndPlanBLocations() {
        AnalyzeRequest request = TestFixtures.sampleAnalyzeRequest();
        AnalysisPipelineMemory memory = new AnalysisPipelineMemory(request);

        String json = builder.buildUserPayload(memory);
        assertTrue(json.contains("current_location"));
        assertTrue(json.contains("plan_b_location"));
        assertFalse(json.contains("\"location\""));
        assertTrue(json.contains("Bengaluru, India"));
    }

    @Test
    void shouldIncludeResumeExcerptWhenSignalsMissing() {
        AnalyzeRequest request = TestFixtures.sampleAnalyzeRequest();
        AnalysisPipelineMemory memory = new AnalysisPipelineMemory(request);
        memory.setResumeText("Acme Corp — Product Manager — 2020–present");

        String json = builder.buildUserPayload(memory);
        assertTrue(json.contains("resume_excerpt"));
        assertTrue(json.contains("Acme Corp"));
    }

}
