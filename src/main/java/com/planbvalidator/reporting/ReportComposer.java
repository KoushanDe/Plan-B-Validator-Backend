package com.planbvalidator.reporting;

import com.planbvalidator.config.PlanBProperties;
import com.planbvalidator.domain.request.PlanBDto;
import com.planbvalidator.domain.request.ProfileDto;
import com.planbvalidator.domain.response.AnalyzeResponse;
import com.planbvalidator.llm.LlmNarrativeResult;
import com.planbvalidator.market.MarketValueAssessment;
import com.planbvalidator.pipeline.AnalysisPipelineMemory;
import com.planbvalidator.research.CurrentMarketConditionForHiring;
import com.planbvalidator.research.ResearchContext;
import com.planbvalidator.resume.ProfileMergeResult;
import com.planbvalidator.resume.ResumeProfileExtraction;
import com.planbvalidator.scoring.IncomeRoadmapValidator;
import com.planbvalidator.scoring.ScoringResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReportComposer {

    private final PlanBProperties properties;

    public ReportComposer(PlanBProperties properties) {
        this.properties = properties;
    }

    public AnalyzeResponse compose(String requestId,
                                   long processingMs,
                                   Map<String, Long> timings,
                                   Map<String, String> aiProviders,
                                   ScoringResult scoringResult,
                                   LlmNarrativeResult narrative,
                                   ResearchContext researchContext,
                                   CurrentMarketConditionForHiring hiringReentry,
                                   MarketValueAssessment marketValue,
                                   AnalysisPipelineMemory memory) {
        Map<String, Object> researchPayload = researchContext == null
                ? Map.of()
                : researchPayload(researchContext, scoringResult.incomeRoadmap());
        Map<String, Object> hiringPayload = hiringReentry == null || !hiringReentry.available()
                ? Map.of()
                : hiringReentry.toMap();

        String planBRoi = resolvePlanBRoiSummary(researchContext, marketValue);
        Map<String, Object> marketValueMap = marketValue == null ? Map.of() : marketValuePayload(marketValue);

        ProfileDto resolved = memory.request().profile();
        var resolvedPlanB = memory.request().planB();
        ProfileMergeResult mergeResult = memory.profileMergeResult();
        ResumeProfileExtraction resumeProfile = memory.resumeProfileExtraction();

        return new AnalyzeResponse(
                requestId,
                processingMs,
                scoringResult.verdict(),
                scoringResult.feasibilityScore(),
                scoringResult.riskScore(),
                scoringResult.confidence(),
                scoringResult.runwayMonths(),
                scoringResult.scoreBreakdown(),
                scoringResult.opportunityCost().toMap(),
                narrative.recommendationSummary(),
                limit(narrative.majorReasons()),
                limit(narrative.redFlags()),
                limit(narrative.nextSteps()),
                mergeLists(scoringResult.assumptions(), narrative.assumptions(), 7),
                mergeLists(scoringResult.dataGaps(), narrative.dataGaps(), 3),
                narrative.personalitySummary(),
                narrative.expectedFailureMode(),
                narrative.safestNextMove(),
                narrative.suggestedFallbackPlan(),
                planBRoi,
                marketValueMap,
                properties.disclaimer(),
                timings,
                aiProviders,
                researchPayload,
                hiringPayload,
                profileToMap(resolved),
                planBToMap(resolvedPlanB),
                mergeResult == null ? Map.of() : mergeResult.fieldSources(),
                resumeProfile == null ? Map.of() : resumeProfile.toCompactMap()
        );
    }

    private static Map<String, Object> profileToMap(ProfileDto profile) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("currentProfession", profile.currentProfession());
        m.put("industry", profile.industry());
        m.put("yearsExperience", profile.yearsExperience());
        m.put("country", profile.country());
        m.put("city", profile.city());
        return m;
    }

    private static Map<String, Object> planBToMap(PlanBDto planB) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", planB.title());
        m.put("targetCountry", planB.targetCountry());
        m.put("targetCity", planB.targetCity());
        m.put("iWillQuitMyJob", planB.iWillQuitMyJob());
        m.put("timelineMonths", planB.timelineMonths());
        return m;
    }

    private static Map<String, Object> researchPayload(ResearchContext research,
                                                       IncomeRoadmapValidator.IncomeRoadmapAssessment incomeRoadmap) {
        Map<String, Object> m = new LinkedHashMap<>(research.toCompactMap());
        if (incomeRoadmap != null) {
            m.put("income_roadmap_validation", incomeRoadmap.toMap());
        }
        return m;
    }

    private static Map<String, Object> marketValuePayload(MarketValueAssessment mv) {
        Map<String, Object> m = new LinkedHashMap<>(mv.toCompactMap());
        m.put("estimated_salary_range", mv.estimatedSalaryRange());
        m.put("assumptions", mv.assumptions());
        return m;
    }

    private static List<String> limit(List<String> input) {
        if (input == null) return List.of();
        return input.stream().filter(s -> s != null && !s.isBlank()).distinct().limit(7).toList();
    }

    private static String resolvePlanBRoiSummary(ResearchContext research, MarketValueAssessment marketValue) {
        if (research != null && research.planBMarketNotes() != null && !research.planBMarketNotes().isBlank()) {
            return research.planBMarketNotes();
        }
        return marketValue != null ? marketValue.planBRoiSummary() : null;
    }

    private static List<String> mergeLists(List<String> primary, List<String> supplemental, int supplementalCap) {
        LinkedHashMap<String, Boolean> map = new LinkedHashMap<>();
        if (primary != null) {
            primary.stream().filter(s -> s != null && !s.isBlank()).forEach(s -> map.put(s, true));
        }
        if (supplemental != null && supplementalCap > 0) {
            supplemental.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .filter(s -> !map.containsKey(s))
                    .limit(supplementalCap)
                    .forEach(s -> map.put(s, true));
        }
        return map.keySet().stream().limit(7).toList();
    }
}
