package com.planbvalidator;

import com.planbvalidator.config.CurrencyRatesProperties;
import com.planbvalidator.config.IncomeRampProperties;
import com.planbvalidator.config.RunwayThresholdsProperties;
import com.planbvalidator.config.ScoringThresholdsProperties;
import com.planbvalidator.config.ScoringWeightsProperties;
import com.planbvalidator.scoring.FinancialConsistencyChecker;
import com.planbvalidator.scoring.IncomeRoadmapValidator;
import com.planbvalidator.scoring.OpportunityCostCalculator;
import com.planbvalidator.scoring.SalaryRangeParser;
import com.planbvalidator.scoring.ScoringEngine;

public final class TestScoringSupport {

    private TestScoringSupport() {}

    public static ScoringEngine scoringEngine() {
        SalaryRangeParser salaryRangeParser = new SalaryRangeParser(new CurrencyRatesProperties(null));
        IncomeRampProperties incomeRamp = IncomeRampProperties.defaults();
        return new ScoringEngine(
                defaultWeights(),
                ScoringThresholdsProperties.defaults(),
                RunwayThresholdsProperties.defaults(),
                new OpportunityCostCalculator(salaryRangeParser),
                new IncomeRoadmapValidator(salaryRangeParser, incomeRamp),
                new FinancialConsistencyChecker(),
                incomeRamp);
    }

    public static ScoringWeightsProperties defaultWeights() {
        return new ScoringWeightsProperties(0.35, 0.2, 0.2, 0.15, 0.1);
    }
}
