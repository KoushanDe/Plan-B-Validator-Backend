package com.planbvalidator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runway calculation and classification thresholds (months).
 * Defaults align with common personal-finance guidance: &lt;3 mo emergency-critical,
 * 3–6 mo high risk for income loss, 6–12 mo transitional buffer, 12+ mo stable.
 */
@ConfigurationProperties(prefix = "planb.runway")
public record RunwayThresholdsProperties(
        double classificationSevereMaxMonths,
        double classificationHighRiskMaxMonths,
        double classificationModerateMaxMonths
) {
    public static RunwayThresholdsProperties defaults() {
        return new RunwayThresholdsProperties(3, 6, 12);
    }
}
