package com.planbvalidator.pipeline;

import com.planbvalidator.config.RunwayThresholdsProperties;
import com.planbvalidator.domain.request.AnalyzeRequest;
import com.planbvalidator.domain.response.QuestionnaireScoreResponse;
import com.planbvalidator.llm.CoreNarrative;
import com.planbvalidator.market.MarketValueAssessment;
import com.planbvalidator.research.CurrentMarketConditionForHiring;
import com.planbvalidator.research.ResearchContext;
import com.planbvalidator.resume.ProfileMergeResult;
import com.planbvalidator.resume.ResumeProfileExtraction;
import com.planbvalidator.scoring.PlanBLocation;
import com.planbvalidator.scoring.ScoringResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-request memory accumulated step-by-step so each LLM call receives only a compact slice of context.
 * Avoids stuffing the full request + resume into every API call (context window limits).
 */
public class AnalysisPipelineMemory {

    private final RunwayThresholdsProperties runwayThresholds;
    private AnalyzeRequest request;
    private String resumeText;
    private ResumeProfileExtraction resumeProfileExtraction;
    private ProfileMergeResult profileMergeResult;
    private Double runwayMonths;
    private Double runwayNetBurn;
    private String runwayMode;
    private QuestionnaireScoreResponse psychology;
    private MarketValueAssessment marketValue;
    private ResearchContext research;
    private CurrentMarketConditionForHiring hiringReentry;
    private ScoringResult scoring;
    private CoreNarrative coreNarrative;

    public AnalysisPipelineMemory(AnalyzeRequest request) {
        this(request, RunwayThresholdsProperties.defaults());
    }

    public AnalysisPipelineMemory(AnalyzeRequest request, RunwayThresholdsProperties runwayThresholds) {
        this.request = request;
        this.runwayThresholds = runwayThresholds;
    }

    public AnalyzeRequest request() {
        return request;
    }

    public void setRequest(AnalyzeRequest request) {
        this.request = request;
    }

    public ResumeProfileExtraction resumeProfileExtraction() {
        return resumeProfileExtraction;
    }

    public void setResumeProfileExtraction(ResumeProfileExtraction resumeProfileExtraction) {
        this.resumeProfileExtraction = resumeProfileExtraction;
    }

    public ProfileMergeResult profileMergeResult() {
        return profileMergeResult;
    }

    public void setProfileMergeResult(ProfileMergeResult profileMergeResult) {
        this.profileMergeResult = profileMergeResult;
    }

    public String resumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public boolean hasResume() {
        return resumeText != null && !resumeText.isBlank();
    }

    public Double runwayMonths() {
        return runwayMonths;
    }

    public void setRunwayMonths(double runwayMonths) {
        this.runwayMonths = runwayMonths;
    }

    public void setRunwayContext(double runwayMonths, double netBurn, String runwayMode) {
        this.runwayMonths = runwayMonths;
        this.runwayNetBurn = netBurn;
        this.runwayMode = runwayMode;
    }

    public QuestionnaireScoreResponse psychology() {
        return psychology;
    }

    public void setPsychology(QuestionnaireScoreResponse psychology) {
        this.psychology = psychology;
    }

    public MarketValueAssessment marketValue() {
        return marketValue;
    }

    public void setMarketValue(MarketValueAssessment marketValue) {
        this.marketValue = marketValue;
    }

    public ResearchContext research() {
        return research;
    }

    public void setResearch(ResearchContext research) {
        this.research = research;
    }

    public CurrentMarketConditionForHiring hiringReentry() {
        return hiringReentry;
    }

    public void setHiringReentry(CurrentMarketConditionForHiring hiringReentry) {
        this.hiringReentry = hiringReentry;
    }

    public ScoringResult scoring() {
        return scoring;
    }

    public void setScoring(ScoringResult scoring) {
        this.scoring = scoring;
    }

    public CoreNarrative coreNarrative() {
        return coreNarrative;
    }

    public void setCoreNarrative(CoreNarrative coreNarrative) {
        this.coreNarrative = coreNarrative;
    }

    public Map<String, Object> compactForResumeAnalysis() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("profession", request.profile().currentProfession());
        m.put("industry", request.profile().industry());
        m.put("years_experience", request.profile().yearsExperience());
        m.put("country", request.profile().country());
        m.put("city", request.profile().city());
        putRunwayIfKnown(m);
        AnalyzeContextFields.putPlanBNarrative(m, request);
        AnalyzeContextFields.putConstraintsNarrative(m, request);
        m.put("plan_b_title", request.planB().title());
        AnalyzeContextFields.putEngagementMode(m, request);
        m.put("plan_b_expected_income_12m", request.planB().expectedIncome12Months());
        if (hasResume()) {
            m.put("resume_text", truncate(resumeText, 12000));
        }
        return m;
    }

    public Map<String, Object> compactForResearch() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("profession", request.profile().currentProfession());
        m.put("industry", request.profile().industry());
        m.put("years_experience", request.profile().yearsExperience());
        m.put("current_location", request.profile().city() + ", " + request.profile().country());
        m.put("plan_b_location", PlanBLocation.effectiveCity(request.profile(), request.planB())
                + ", " + PlanBLocation.effectiveCountry(request.profile(), request.planB()));
        putRunwayIfKnown(m);
        AnalyzeContextFields.putPlanBNarrative(m, request);
        AnalyzeContextFields.putConstraintsNarrative(m, request);
        m.put("plan_b_title", request.planB().title());
        AnalyzeContextFields.putEngagementMode(m, request);
        m.put("plan_b_timeline_months", request.planB().timelineMonths());
        m.put("user_claimed_income_3m", request.planB().expectedIncome3Months());
        m.put("user_claimed_income_6m", request.planB().expectedIncome6Months());
        m.put("user_claimed_income_12m", request.planB().expectedIncome12Months());
        m.put("resume_signals_available", marketValue != null && hasResumeSignals(marketValue));
        if (marketValue != null) {
            m.put("credential_tier", marketValue.credentialTier());
            m.put("resume_inferred_salary_range_preliminary", marketValue.estimatedSalaryRange());
            m.put("market_value_score", marketValue.marketValueScore());
            m.put("opportunity_cost_risk", marketValue.opportunityCostRisk());
            m.put("key_signals", marketValue.keySignals());
            m.put("recent_employers", marketValue.recentEmployers());
            m.put("recent_job_titles", marketValue.recentJobTitles());
            m.put("comp_search_queries", marketValue.compSearchQueries());
        }
        if (hasResume() && (marketValue == null || !hasResumeSignals(marketValue))) {
            m.put("resume_excerpt", truncate(resumeText, 4000));
        }
        return m;
    }

    /** Corporate re-hire difficulty after quitting (full-time leap only). */
    public Map<String, Object> compactForHiringReentry() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("profession", request.profile().currentProfession());
        m.put("industry", request.profile().industry());
        m.put("years_experience", request.profile().yearsExperience());
        m.put("current_location", request.profile().city() + ", " + request.profile().country());
        m.put("plan_b_title", request.planB().title());
        m.put("engagement_mode", "full_time_leap");
        m.put("resume_signals_available", marketValue != null && hasResumeSignals(marketValue));
        if (marketValue != null) {
            m.put("credential_tier", marketValue.credentialTier());
            m.put("resume_inferred_salary_range_preliminary", marketValue.estimatedSalaryRange());
            m.put("recent_employers", marketValue.recentEmployers());
            m.put("recent_job_titles", marketValue.recentJobTitles());
            m.put("comp_search_queries", marketValue.compSearchQueries());
            m.put("key_signals", marketValue.keySignals());
        }
        if (research != null && research.hasWebSalaryData()) {
            m.put("web_corporate_salary_range", research.corporateSalaryRange());
            m.put("web_competitiveness_score", research.webCompetitivenessScore());
        }
        if (hasResume() && (marketValue == null || !hasResumeSignals(marketValue))) {
            m.put("resume_excerpt", truncate(resumeText, 4000));
        }
        return m;
    }

    private static boolean hasResumeSignals(MarketValueAssessment mv) {
        return (mv.recentEmployers() != null && !mv.recentEmployers().isEmpty())
                || (mv.recentJobTitles() != null && !mv.recentJobTitles().isEmpty())
                || (mv.compSearchQueries() != null && !mv.compSearchQueries().isEmpty())
                || (mv.keySignals() != null && !mv.keySignals().isEmpty());
    }

    public Map<String, Object> compactForCoreNarrative() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (scoring != null) {
            m.put("verdict", scoring.verdict().name());
            m.put("feasibility_score", scoring.feasibilityScore());
            m.put("risk_score", scoring.riskScore());
            m.put("runway_months", scoring.runwayMonths());
            m.put("score_breakdown", scoring.scoreBreakdown());
            m.put("opportunity_cost", scoring.opportunityCost().toMap());
            m.put("income_roadmap_validation", scoring.incomeRoadmap().toMap());
            m.put("scoring_assumptions", scoring.assumptions());
            m.put("scoring_data_gaps", scoring.dataGaps());
        } else {
            putRunwayIfKnown(m);
        }
        AnalyzeContextFields.putPlanBNarrative(m, request);
        AnalyzeContextFields.putConstraintsNarrative(m, request);
        if (psychology != null) {
            m.put("psychology_summary", psychology.summary());
            m.put("risk_profile", psychology.riskProfile());
            m.put("risk_taking_potential", psychology.riskTakingPotential());
            m.put("founder_mindset", psychology.founderMindset());
        }
        AnalyzeContextFields.putEngagementMode(m, request);
        if (marketValue != null) {
            m.put("market_value", marketValue.toCompactMap());
        }
        if (research != null) {
            m.put("research_summary", research.marketSummary());
            m.put("market_sentiment", research.marketSentiment().name());
            if (research.hasWebSalaryData()) {
                m.put("web_corporate_salary_range", research.corporateSalaryRange());
                m.put("web_competitiveness_score", research.webCompetitivenessScore());
            }
            m.put("plan_b_market_notes", research.planBMarketNotes());
            m.put("plan_b_realistic_income_range", research.planBRealisticIncomeRange());
            m.put("typical_months_to_meaningful_income", research.typicalMonthsToMeaningfulIncome());
            m.put("risk_factors", research.riskFactors());
        }
        if (hiringReentry != null && hiringReentry.available()) {
            m.put("current_market_condition_for_hiring", hiringReentry.toMap());
        }
        m.put("plan_b_roi_summary", resolvePlanBRoiSummary());
        return m;
    }

    private String resolvePlanBRoiSummary() {
        if (research != null && research.planBMarketNotes() != null && !research.planBMarketNotes().isBlank()) {
            return research.planBMarketNotes();
        }
        return marketValue != null ? marketValue.planBRoiSummary() : null;
    }

    private void putRunwayIfKnown(Map<String, Object> target) {
        if (runwayMonths != null) {
            AnalyzeContextFields.putRunwayContext(target, request, runwayMonths, runwayThresholds,
                    runwayNetBurn, runwayMode);
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }
}
