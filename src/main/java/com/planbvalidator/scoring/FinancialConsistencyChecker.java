package com.planbvalidator.scoring;

import com.planbvalidator.domain.request.AnalyzeRequest;
import com.planbvalidator.market.MarketValueAssessment;
import com.planbvalidator.research.ResearchContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class FinancialConsistencyChecker {

    private static final double CORPORATE_INCOME_MISMATCH_RATIO = 0.40;

    public List<String> check(AnalyzeRequest request,
                              ResearchContext research,
                              OpportunityCostMetric opportunityCost) {
        List<String> flags = new ArrayList<>();
        checkCrossBorder(request, research, flags);
        checkCorporateIncomeMismatch(request, opportunityCost, flags);
        return flags;
    }

    private static void checkCrossBorder(AnalyzeRequest request,
                                         ResearchContext research,
                                         List<String> flags) {
        if (research == null) {
            return;
        }
        String profileCountry = normalizeCountry(request.profile().country());
        String planBCountry = normalizeCountry(PlanBLocation.effectiveCountry(request.profile(), request.planB()));
        if (profileCountry.equals(planBCountry)) {
            return;
        }
        if (research.hasWebSalaryData() || research.hasPlanBMarketIncome()) {
            flags.add("Plan B target country differs from current profile country; web comp normalized to INR — "
                    + "verify user-entered amounts use the same currency basis");
        }
    }

    private static void checkCorporateIncomeMismatch(AnalyzeRequest request,
                                                     OpportunityCostMetric opportunityCost,
                                                     List<String> flags) {
        if (opportunityCost == null) {
            return;
        }
        if (!"web_research_market".equals(opportunityCost.corporateBaselineSource())
                && !"resume_market_inference".equals(opportunityCost.corporateBaselineSource())) {
            return;
        }
        double stated = request.financials().monthlyIncome();
        if (stated <= 0) {
            return;
        }
        double baseline = opportunityCost.monthlyCorporateBaseline();
        double ratio = Math.abs(baseline - stated) / Math.max(stated, 1);
        if (ratio > CORPORATE_INCOME_MISMATCH_RATIO) {
            flags.add(String.format(Locale.ROOT,
                    "Stated monthly income (₹%.0f) differs materially from market corporate baseline (₹%.0f)",
                    stated, baseline));
        }
    }

    private static String normalizeCountry(String country) {
        if (country == null) {
            return "";
        }
        return country.trim().toLowerCase(Locale.ROOT);
    }
}
