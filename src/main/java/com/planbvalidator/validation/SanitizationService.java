package com.planbvalidator.validation;

import com.planbvalidator.domain.request.*;
import org.springframework.stereotype.Service;

@Service
public class SanitizationService {

    public AnalyzeRequest sanitize(AnalyzeRequest req) {
        return new AnalyzeRequest(
                new ProfileDto(
                        cleanNullable(req.profile().currentProfession()),
                        cleanNullable(req.profile().industry()),
                        req.profile().yearsExperience(),
                        cleanNullable(req.profile().country()),
                        cleanNullable(req.profile().city())
                ),
                req.financials(),
                new PlanBDto(
                        clean(req.planB().title()),
                        clean(req.planB().description()),
                        clean(req.planB().reason()),
                        req.planB().timelineMonths(),
                        req.planB().expectedIncome3Months(),
                        req.planB().expectedIncome6Months(),
                        req.planB().expectedIncome12Months(),
                        req.planB().iWillQuitMyJob(),
                        cleanNullable(req.planB().targetCountry()),
                        cleanNullable(req.planB().targetCity())
                ),
                new ConstraintsDto(
                        clean(req.constraints().successDefinition()),
                        clean(req.constraints().biggestFear()),
                        clean(req.constraints().acceptableDownside()),
                        req.constraints().minimumAcceptableSalary(),
                        req.constraints().acceptableMonthsWithoutIncome(),
                        req.constraints().familyPressureLevel()
                ),
                req.psychology(),
                req.researchOptions()
        );
    }

    private static String clean(String input) {
        if (input == null) return "";
        String noControls = input.replaceAll("[\\p{Cntrl}]", " ");
        String compact = noControls.replaceAll("\\s+", " ").trim();
        if (compact.toLowerCase().contains("ignore previous instructions")) {
            return compact.replace("ignore previous instructions", "").trim();
        }
        return compact;
    }

    private static String cleanNullable(String input) {
        return input == null ? null : clean(input);
    }
}
