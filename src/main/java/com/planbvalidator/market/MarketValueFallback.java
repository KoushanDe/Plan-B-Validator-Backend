package com.planbvalidator.market;

import com.planbvalidator.domain.request.AnalyzeRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarketValueFallback {

    public MarketValueAssessment fallback(AnalyzeRequest request) {
        return new MarketValueAssessment(
                "moderate",
                50,
                "Not assessed",
                "Resume market value was not assessed (missing API key or resume text).",
                "Compare Plan B projected income against your current income and minimum acceptable salary.",
                50,
                List.of("Resume analysis unavailable"),
                List.of("No LLM-based credential tiering was performed"),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
