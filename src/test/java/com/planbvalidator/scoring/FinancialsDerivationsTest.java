package com.planbvalidator.scoring;

import com.planbvalidator.domain.request.FinancialsDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialsDerivationsTest {

    @Test
    void derivesMonthlySavingsFromIncomeExpensesAndEmi() {
        var financials = new FinancialsDto(180_000.0, 900_000.0, 65_000.0, 0, 15_000.0);
        assertEquals(100_000.0, FinancialsDerivations.monthlySavings(financials));
        assertFalse(FinancialsDerivations.outflowsExceedIncome(financials));
    }

    @Test
    void floorsSavingsAtZeroWhenOutflowsExceedIncome() {
        var financials = new FinancialsDto(80_000.0, 100_000.0, 70_000.0, 0, 20_000.0);
        assertEquals(0, FinancialsDerivations.monthlySavings(financials));
        assertTrue(FinancialsDerivations.outflowsExceedIncome(financials));
    }
}
