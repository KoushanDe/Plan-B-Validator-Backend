package com.planbvalidator.scoring;

import com.planbvalidator.domain.request.RunwayCalculateRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RunwayCalculatorTest {

    private final RunwayService service = new RunwayService(com.planbvalidator.config.RunwayThresholdsProperties.defaults());

    @Test
    void shouldCalculateRunway() {
        var result = service.calculate(new RunwayCalculateRequest(900000.0, 65000.0, 0.0));
        assertEquals(13.8, result.runwayMonths());
        assertEquals(65000.0, result.monthlyBurn());
        assertEquals(65000.0, result.netBurn());
        assertEquals("emergency_full_burn", result.runwayMode());
        assertEquals("stable", result.riskClassification());
    }

    @Test
    void shouldIncludeMonthlyDebtInBurn() {
        var result = service.calculate(new RunwayCalculateRequest(900000.0, 65000.0, 15000.0));
        assertEquals(11.3, result.runwayMonths());
        assertEquals(80000.0, result.monthlyBurn());
        assertEquals(80000.0, result.netBurn());
        assertEquals("moderate", result.riskClassification());
    }

    @Test
    void sideHustleShouldUseNetBurnAfterIncome() {
        var result = service.calculate(new RunwayCalculateRequest(
                900000.0, 65000.0, 0.0, 50000.0, true));
        assertEquals(60.0, result.runwayMonths(), 0.1);
        assertEquals(65000.0, result.monthlyBurn());
        assertEquals(15000.0, result.netBurn());
        assertEquals("side_hustle_net_burn", result.runwayMode());
    }

    @Test
    void sideHustleWhenIncomeCoversBurnShouldCapRunway() {
        var result = service.calculate(new RunwayCalculateRequest(
                500000.0, 65000.0, 0.0, 100000.0, true));
        assertEquals(RunwayService.INCOME_COVERS_BURN_RUNWAY_CAP, result.runwayMonths());
    }
}
