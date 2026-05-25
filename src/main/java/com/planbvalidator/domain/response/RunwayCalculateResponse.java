package com.planbvalidator.domain.response;

public record RunwayCalculateResponse(
        double runwayMonths,
        double monthlyBurn,
        double netBurn,
        String runwayMode,
        String riskClassification
) {
}
