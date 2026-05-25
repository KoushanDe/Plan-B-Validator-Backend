package com.planbvalidator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "planb.psychology")
public record PsychologyThresholdsProperties(
        List<String> riskTakingDimensions,
        List<String> founderMindsetDimensions,
        ProfileBands profileBands
) {
    public record ProfileBands(int conservativeMax, int aggressiveMin) {}

    public static PsychologyThresholdsProperties defaults() {
        return new PsychologyThresholdsProperties(
                List.of("uncertainty_tolerance", "uncertainty_stamina", "stress_recovery", "financial_resilience"),
                List.of("discipline", "routine_adherence", "setback_recovery", "uncertainty_stamina", "self_directed_motivation"),
                new ProfileBands(45, 70)
        );
    }
}
