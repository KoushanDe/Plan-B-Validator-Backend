package com.planbvalidator.scoring;

import com.planbvalidator.config.CurrencyRatesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalaryRangeParserTest {

    private SalaryRangeParser parser;

    @BeforeEach
    void setUp() {
        parser = new SalaryRangeParser(new CurrencyRatesProperties(null));
    }

    @Test
    void shouldParseInrLpaRange() {
        var parsed = parser.parse("₹38-48 LPA");
        assertTrue(parsed.isPresent());
        assertEquals(358333.3, parsed.get().monthlyMidInr(), 500.0);
        assertEquals("₹38-48 LPA", parsed.get().normalizedLpaRange());
    }

    @Test
    void shouldConvertAedAnnualRangeToInrLpa() {
        var parsed = parser.parse("AED 180,000 - AED 300,000 per year");
        assertTrue(parsed.isPresent());
        assertEquals(450_000.0, parsed.get().monthlyMidInr(), 5_000.0);
        assertEquals("₹41-68 LPA", parsed.get().normalizedLpaRange());
        assertEquals("AED", parsed.get().sourceCurrency());
    }

    @Test
    void shouldConvertUsdAnnualRangeToInrLpa() {
        var parsed = parser.parse("$120,000 - $150,000 per year");
        assertTrue(parsed.isPresent());
        assertEquals(933_750.0, parsed.get().monthlyMidInr(), 5_000.0);
        assertEquals("₹100-125 LPA", parsed.get().normalizedLpaRange());
    }

    @Test
    void shouldTreatLargeInrAmountWithoutUnitAsAnnual() {
        var parsed = parser.parse("₹3,600,000 - ₹4,800,000");
        assertTrue(parsed.isPresent());
        assertEquals(350_000.0, parsed.get().monthlyMidInr(), 1_000.0);
        assertEquals("₹36-48 LPA", parsed.get().normalizedLpaRange());
    }

    @Test
    void shouldTreatSmallInrAmountWithoutUnitAsMonthly() {
        var parsed = parser.parse("₹180,000 per month");
        assertTrue(parsed.isPresent());
        assertEquals(180_000.0, parsed.get().monthlyMidInr(), 1.0);
        assertEquals("₹22 LPA", parsed.get().normalizedLpaRange());
    }

    @Test
    void shouldReturnEmptyForUnknownForeignCurrency() {
        var parsed = parser.parse("XYZ 100,000 - 200,000 per year");
        assertTrue(parsed.isEmpty());
    }

    @Test
    void shouldConvertThbAnnualRangeToInrLpa() {
        var parsed = parser.parse("THB 1,200,000 - THB 1,800,000 per year");
        assertTrue(parsed.isPresent());
        assertEquals(287_500.0, parsed.get().monthlyMidInr(), 5_000.0);
        assertEquals("THB", parsed.get().sourceCurrency());
    }
}
