package com.planbvalidator.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        ScoringWeightsProperties.class,
        ScoringThresholdsProperties.class,
        RunwayThresholdsProperties.class,
        PsychologyThresholdsProperties.class,
        PlanBProperties.class,
        CurrencyRatesProperties.class,
        IncomeRampProperties.class
})
public class Config {
}
