package com.planbvalidator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "planb.scoring.weights")
public record ScoringWeightsProperties(
        double financialRunway,
        double reversibility,
        double marketFeasibility,
        double riskTolerance,
        double timelinePressure
) {
}
