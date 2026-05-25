package com.planbvalidator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "planb.scoring.income-ramp")
public record IncomeRampProperties(
        double at3MonthsFraction,
        double at6MonthsFraction
) {
    public IncomeRampProperties {
        if (at3MonthsFraction <= 0) {
            at3MonthsFraction = 0.10;
        }
        if (at6MonthsFraction <= 0) {
            at6MonthsFraction = 0.45;
        }
    }

    public static IncomeRampProperties defaults() {
        return new IncomeRampProperties(0.10, 0.45);
    }
}
