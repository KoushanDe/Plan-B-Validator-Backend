package com.planbvalidator.scoring;

public record ScoreBreakdown(
        int financialRunway,
        int marketFeasibility,
        int riskTolerance,
        int timelinePressure,
        int reversibility
) {
}
