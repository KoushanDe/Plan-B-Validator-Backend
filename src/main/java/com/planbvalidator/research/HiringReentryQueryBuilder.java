package com.planbvalidator.research;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planbvalidator.pipeline.AnalysisPipelineMemory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HiringReentryQueryBuilder {

    private final ObjectMapper objectMapper;

    public HiringReentryQueryBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildUserPayload(AnalysisPipelineMemory memory) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(memory.compactForHiringReentry());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize hiring reentry context", e);
        }
    }
}
