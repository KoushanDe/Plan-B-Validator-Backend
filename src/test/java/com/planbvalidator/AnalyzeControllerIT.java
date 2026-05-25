package com.planbvalidator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyzeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAnalyzeReport() throws Exception {
        String payload = """
                {
                  "profile": {"currentProfession":"Backend Engineer","industry":"Fintech","yearsExperience":2.5,"country":"India","city":"Bengaluru"},
                  "financials": {"monthlyIncome":180000,"liquidSavings":900000,"monthlyExpenses":65000,"dependents":0,"debtObligations":0},
                  "planB": {"title":"Freelance AI","description":"Build freelancing","reason":"Autonomy","timelineMonths":6,"iWillQuitMyJob":false,"expectedIncome3Months":10000,"expectedIncome6Months":60000,"expectedIncome12Months":120000},
                  "constraints": {"successDefinition":"Stable clients","biggestFear":"Income instability","acceptableDownside":"Delay","minimumAcceptableSalary":100000,"acceptableMonthsWithoutIncome":4,"familyPressureLevel":3},
                  "psychology": {"uncertaintyTolerance":4,"discipline":3,"stressRecovery":4,"validationDependency":2,"impulsiveness":2,"routineAdherence":4,"setbackRecovery":4,"uncertaintyStamina":3,"financialResilience":4,"selfDirectedMotivation":4},
                  "researchOptions": {"enableResearch": false}
                }
                """;

        mockMvc.perform(post("/v1/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.feasibilityScore").isNumber())
                .andExpect(jsonPath("$.riskScore").isNumber())
                .andExpect(jsonPath("$.scoreBreakdown.financialRunway").isNumber())
                .andExpect(jsonPath("$.opportunityCost.score").isNumber())
                .andExpect(jsonPath("$.opportunityCost.band").exists())
                .andExpect(jsonPath("$.aiProviders.gemini_research").exists())
                .andExpect(jsonPath("$.aiProviders.openai").exists())
                .andExpect(jsonPath("$.aiProviders.gemini").exists());
    }
}
