package com.planbvalidator.research;

import com.planbvalidator.pipeline.AnalysisPipelineMemory;

import java.util.Optional;

public interface ResearchService {

    boolean isConfigured();

    Optional<ResearchContext> fetchMarketContext(AnalysisPipelineMemory memory);
}
