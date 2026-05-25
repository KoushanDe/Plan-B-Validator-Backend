package com.planbvalidator;

import com.planbvalidator.domain.request.*;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static AnalyzeRequest minimalAnalyzeRequest() {
        return new AnalyzeRequest(
                new ProfileDto("Engineer", "Tech", 5.0, "IN", "BLR"),
                new FinancialsDto(100_000.0, 500_000.0, 40_000.0, 0, 0.0),
                new PlanBDto("T", "D", "R", 6,
                        0.0, 0.0, 0.0, false, null, null),
                new ConstraintsDto("S", "F", "D", 80_000.0, 3, 2),
                new PsychologyDto(3, 3, 3, 3, 3, 3, 3, 3, 3, 3),
                null);
    }

    public static String minimalAnalyzeRequestJson() {
        return """
                {
                  "profile": {"currentProfession":"Engineer","industry":"Tech","yearsExperience":5,"country":"IN","city":"BLR"},
                  "financials": {"monthlyIncome":100000,"liquidSavings":500000,"monthlyExpenses":40000,"dependents":0,"debtObligations":0},
                  "planB": {"title":"T","description":"D","reason":"R","timelineMonths":6,"expectedIncome3Months":0,"expectedIncome6Months":0,"expectedIncome12Months":0,"iWillQuitMyJob":false},
                  "constraints": {"successDefinition":"S","biggestFear":"F","acceptableDownside":"D","minimumAcceptableSalary":80000,"acceptableMonthsWithoutIncome":3,"familyPressureLevel":2},
                  "psychology": {"uncertaintyTolerance":3,"discipline":3,"stressRecovery":3,"validationDependency":3,"impulsiveness":3,"routineAdherence":3,"setbackRecovery":3,"uncertaintyStamina":3,"financialResilience":3,"selfDirectedMotivation":3}
                }
                """;
    }

    public static AnalyzeRequest sampleAnalyzeRequest() {
        return new AnalyzeRequest(
                new ProfileDto("Backend Engineer", "Fintech", 8.0, "India", "Bengaluru"),
                new FinancialsDto(500_000.0, 0.0, 150_000.0, 0, 0.0),
                new PlanBDto("AI consulting", "Freelance AI projects", "Autonomy", 6,
                        50_000.0, 100_000.0, 200_000.0, false, null, null),
                new ConstraintsDto("Clients", "Volatility", "Delay", 120_000.0, 3, 3),
                new PsychologyDto(4, 3, 4, 2, 2, 4, 4, 3, 4, 4),
                new ResearchOptionsDto(true));
    }
}
