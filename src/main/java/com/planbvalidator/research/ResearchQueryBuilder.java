package com.planbvalidator.research;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.pipeline.AnalysisPipelineMemory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResearchQueryBuilder {

    private final ObjectMapper objectMapper;

    public ResearchQueryBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildUserPayload(AnalysisPipelineMemory memory) {
        return toJson(memory.compactForResearch());
    }

    private String toJson(Map<String, Object> context) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize research context", e);
        }
    }
}
