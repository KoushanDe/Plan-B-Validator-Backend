package com.planbvalidator.scoring;

import com.planbvalidator.config.RunwayThresholdsProperties;
import com.planbvalidator.domain.request.RunwayCalculateRequest;
import com.planbvalidator.domain.response.RunwayCalculateResponse;
import com.planbvalidator.pipeline.AnalyzeContextFields;
import org.springframework.stereotype.Service;

@Service
public class RunwayService {

    static final double INCOME_COVERS_BURN_RUNWAY_CAP = 999.0;

    private final RunwayThresholdsProperties thresholds;

    public RunwayService(RunwayThresholdsProperties thresholds) {
        this.thresholds = thresholds;
    }

    /**
     * {@code debtObligations} is monthly debt service (EMI, loan payments), not total outstanding debt.
     * Side hustle: net burn = max(0.01, gross burn − monthly income). Full-time leap: gross burn only.
     */
    public RunwayCalculateResponse calculate(RunwayCalculateRequest request) {
        double grossBurn = AnalyzeContextFields.monthlyBurn(request);
        boolean sideHustle = Boolean.TRUE.equals(request.sideHustle());
        double income = request.monthlyIncome() != null ? request.monthlyIncome() : 0.0;

        double netBurn;
        String runwayMode;
        if (sideHustle) {
            runwayMode = "side_hustle_net_burn";
            netBurn = Math.max(0.01, grossBurn - income);
        } else {
            runwayMode = "emergency_full_burn";
            netBurn = grossBurn;
        }

        double runwayMonths;
        if (sideHustle && grossBurn <= income) {
            runwayMonths = INCOME_COVERS_BURN_RUNWAY_CAP;
        } else {
            runwayMonths = request.liquidSavings() / netBurn;
        }

        return new RunwayCalculateResponse(
                AnalyzeContextFields.roundMonths(runwayMonths),
                grossBurn,
                netBurn,
                runwayMode,
                classifyRunway(runwayMonths));
    }

    public String classifyRunway(double runwayMonths) {
        return AnalyzeContextFields.classifyRunway(runwayMonths, thresholds);
    }
}
