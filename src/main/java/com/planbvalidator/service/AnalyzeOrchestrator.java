package com.planbvalidator.service;

import com.planbvalidator.domain.request.AnalyzeRequest;
import com.planbvalidator.domain.request.PlanBDto;
import com.planbvalidator.domain.request.ProfileDto;
import com.planbvalidator.domain.request.RunwayCalculateRequest;
import com.planbvalidator.domain.response.AnalyzeResponse;
import com.planbvalidator.domain.response.QuestionnaireScoreResponse;
import com.planbvalidator.llm.LlmReasoningService;
import com.planbvalidator.llm.NarrativeGenerationResult;
import com.planbvalidator.market.MarketValueAssessment;
import com.planbvalidator.market.OpenAiResumeMarketValueService;
import com.planbvalidator.observability.PipelineLogger;
import com.planbvalidator.pipeline.AnalysisPipelineMemory;
import com.planbvalidator.pipeline.PipelineProgressListener;
import com.planbvalidator.pipeline.PipelineStage;
import com.planbvalidator.psychology.PsychologyEngine;
import com.planbvalidator.reporting.ReportComposer;
import com.planbvalidator.research.CurrentMarketConditionForHiring;
import com.planbvalidator.research.HiringReentryResearchService;
import com.planbvalidator.research.ResearchContext;
import com.planbvalidator.research.ResearchService;
import com.planbvalidator.resume.OpenAiResumeProfileParserService;
import com.planbvalidator.resume.ProfileCompletenessValidator;
import com.planbvalidator.resume.ProfileMergeService;
import com.planbvalidator.resume.ResumeProfileExtraction;
import com.planbvalidator.scoring.RunwayService;
import com.planbvalidator.scoring.ScoringEngine;
import com.planbvalidator.scoring.ScoringResult;
import com.planbvalidator.validation.SanitizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AnalyzeOrchestrator {

    private final RunwayService runwayService;
    private final PsychologyEngine psychologyEngine;
    private final ScoringEngine scoringEngine;
    private final ResearchService researchService;
    private final HiringReentryResearchService hiringReentryResearchService;
    private final OpenAiResumeProfileParserService resumeProfileParserService;
    private final OpenAiResumeMarketValueService resumeMarketValueService;
    private final ProfileMergeService profileMergeService;
    private final ProfileCompletenessValidator profileCompletenessValidator;
    private final LlmReasoningService llmReasoningService;
    private final ReportComposer reportComposer;
    private final SanitizationService sanitizationService;

    public AnalyzeOrchestrator(RunwayService runwayService,
                               PsychologyEngine psychologyEngine,
                               ScoringEngine scoringEngine,
                               ResearchService researchService,
                               HiringReentryResearchService hiringReentryResearchService,
                               OpenAiResumeProfileParserService resumeProfileParserService,
                               OpenAiResumeMarketValueService resumeMarketValueService,
                               ProfileMergeService profileMergeService,
                               ProfileCompletenessValidator profileCompletenessValidator,
                               LlmReasoningService llmReasoningService,
                               ReportComposer reportComposer,
                               SanitizationService sanitizationService) {
        this.runwayService = runwayService;
        this.psychologyEngine = psychologyEngine;
        this.scoringEngine = scoringEngine;
        this.researchService = researchService;
        this.hiringReentryResearchService = hiringReentryResearchService;
        this.resumeProfileParserService = resumeProfileParserService;
        this.resumeMarketValueService = resumeMarketValueService;
        this.profileMergeService = profileMergeService;
        this.profileCompletenessValidator = profileCompletenessValidator;
        this.llmReasoningService = llmReasoningService;
        this.reportComposer = reportComposer;
        this.sanitizationService = sanitizationService;
    }

    public AnalyzeResponse analyze(String requestId,
                                   AnalyzeRequest request,
                                   String resumeTextFromPdf,
                                   PipelineProgressListener progress) {
        boolean hasResume = resumeTextFromPdf != null && !resumeTextFromPdf.isBlank();
        boolean researchEnabled = request.researchOptions() == null || request.researchOptions().isEnabled();
        String planBTitle = request.planB() != null ? request.planB().title() : null;
        PipelineLogger.analyzeStarted(requestId, hasResume, researchEnabled, planBTitle);

        long start = System.currentTimeMillis();
        var timings = new LinkedHashMap<String, Long>();
        var aiProviders = new LinkedHashMap<String, String>();
        PipelineStage currentStage = PipelineStage.SANITIZE;

        try {
            currentStage = PipelineStage.SANITIZE;
            progress.onProgress(currentStage, "running", currentStage.defaultMessage(), null);
            PipelineLogger.stageStarted(currentStage, hasResume ? "resumePdf=true" : "resumePdf=false");
            AnalyzeRequest sanitizedRequest = sanitizationService.sanitize(request);
            AnalysisPipelineMemory memory = new AnalysisPipelineMemory(sanitizedRequest);
            if (hasResume) {
                memory.setResumeText(resumeTextFromPdf);
            }

            final AnalyzeRequest activeRequest = resolveProfile(sanitizedRequest, memory, progress, timings, aiProviders);
            memory.setRequest(activeRequest);
            profileCompletenessValidator.requireComplete(activeRequest.profile());
            progress.onProgress(currentStage, "complete", "Input validated", null);
            PipelineLogger.stageCompleted(currentStage, 0, "profileSource=" + summarizeProfileSources(memory));

            currentStage = PipelineStage.RUNWAY;
            long t1 = System.currentTimeMillis();
            progress.onProgress(currentStage, "running", currentStage.defaultMessage(), null);
            PipelineLogger.stageStarted(currentStage, null);
            var runwayResult = runwayService.calculate(
                    RunwayCalculateRequest.from(
                            activeRequest.financials(),
                            activeRequest.planB().willKeepJob()));
            double emergencyRunwayMonths = runwayService.calculate(
                    RunwayCalculateRequest.from(activeRequest.financials())).runwayMonths();
            memory.setRunwayContext(runwayResult.runwayMonths(), runwayResult.netBurn(), runwayResult.runwayMode());
            double runwayMonths = runwayResult.runwayMonths();
            timings.put("runway_ms", System.currentTimeMillis() - t1);
            progress.onProgress(currentStage, "complete", "Runway: " + runwayMonths + " months", null);
            PipelineLogger.stageCompleted(currentStage, timings.get("runway_ms"), "runwayMonths=" + runwayMonths);

            currentStage = PipelineStage.PSYCHOLOGY;
            t1 = System.currentTimeMillis();
            progress.onProgress(currentStage, "running", currentStage.defaultMessage(), null);
            PipelineLogger.stageStarted(currentStage, null);
            QuestionnaireScoreResponse psychologyScore = psychologyEngine.score(activeRequest.psychology());
            memory.setPsychology(psychologyScore);
            timings.put("psychology_ms", System.currentTimeMillis() - t1);
            progress.onProgress(currentStage, "complete", "Risk profile: " + psychologyScore.riskProfile(), null);
            PipelineLogger.stageCompleted(currentStage, timings.get("psychology_ms"),
                    "riskProfile=" + psychologyScore.riskProfile()
                            + " riskTaking=" + psychologyScore.riskTakingPotential()
                            + " founderMindset=" + psychologyScore.founderMindset());

            MarketValueAssessment marketValue = null;
            boolean resumeMarketValueAssessed = false;
            if (memory.hasResume()) {
                currentStage = PipelineStage.RESUME_MARKET_VALUE;
                t1 = System.currentTimeMillis();
                progress.onProgress(currentStage, "running", currentStage.defaultMessage(), "openai");
                PipelineLogger.stageStarted(currentStage, "provider=openai");
                Optional<MarketValueAssessment> assessed = resumeMarketValueService.assess(memory);
                marketValue = assessed.orElse(null);
                resumeMarketValueAssessed = assessed.isPresent();
                memory.setMarketValue(marketValue);
                timings.put("resume_market_value_ms", System.currentTimeMillis() - t1);
                String marketStatus = assessed.isPresent() ? "success" :
                        (resumeMarketValueService.isConfigured() ? "failed" : "not_configured");
                aiProviders.put("openai_resume", marketStatus);
                progress.onProgress(currentStage, "complete",
                        marketValue != null ? "Credential tier: " + marketValue.credentialTier()
                                : "Resume market value unavailable", "openai");
                if (assessed.isPresent()) {
                    PipelineLogger.stageCompleted(currentStage, timings.get("resume_market_value_ms"),
                            "credentialTier=" + marketValue.credentialTier() + " marketValueScore=" + marketValue.marketValueScore());
                } else {
                    PipelineLogger.stageDegraded(currentStage, timings.get("resume_market_value_ms"),
                            "status=" + marketStatus + " usingFallback=true", null);
                }
            } else {
                PipelineLogger.stageSkipped(PipelineStage.RESUME_MARKET_VALUE, "no resume PDF");
            }

            currentStage = PipelineStage.RESEARCH;
            t1 = System.currentTimeMillis();
            boolean researchRequested = researchEnabled;
            progress.onProgress(currentStage, "running", currentStage.defaultMessage(), "gemini");
            PipelineLogger.stageStarted(currentStage, "requested=" + researchRequested + " configured=" + researchService.isConfigured());
            Optional<ResearchContext> research = researchRequested
                    ? researchService.fetchMarketContext(memory)
                    : Optional.empty();
            research.ifPresent(memory::setResearch);
            timings.put("research_ms", System.currentTimeMillis() - t1);
            String researchStatus = resolveResearchStatus(researchRequested, research.isPresent());
            aiProviders.put("gemini_research", researchStatus);
            progress.onProgress(currentStage, "complete",
                    research.isPresent() ? "Market research gathered" : "Research skipped or unavailable", "gemini");
            if (!researchRequested) {
                PipelineLogger.stageSkipped(currentStage, "enableResearch=false");
            } else if (research.isPresent()) {
                PipelineLogger.stageCompleted(currentStage, timings.get("research_ms"),
                        "sentiment=" + research.get().marketSentiment());
            } else {
                PipelineLogger.stageDegraded(currentStage, timings.get("research_ms"),
                        "status=" + researchStatus, null);
            }

            CurrentMarketConditionForHiring hiringReentry = null;
            if (activeRequest.planB().iWillQuitMyJob()) {
                currentStage = PipelineStage.CURRENT_MARKET_CONDITION_FOR_HIRING;
                t1 = System.currentTimeMillis();
                progress.onProgress(currentStage, "running", currentStage.defaultMessage(), "gemini");
                PipelineLogger.stageStarted(currentStage, "fullTimeLeap=true");
                Optional<CurrentMarketConditionForHiring> hiringResult = researchRequested
                        ? hiringReentryResearchService.assess(memory)
                        : Optional.empty();
                hiringReentry = hiringResult.orElse(null);
                hiringResult.ifPresent(memory::setHiringReentry);
                timings.put("hiring_reentry_ms", System.currentTimeMillis() - t1);
                String hiringStatus = resolveHiringReentryStatus(researchRequested, hiringResult.isPresent());
                aiProviders.put("gemini_hiring_reentry", hiringStatus);
                progress.onProgress(currentStage, "complete",
                        hiringResult.isPresent() ? "Corporate re-hire outlook assessed" : "Re-hire assessment unavailable",
                        "gemini");
                if (!researchRequested) {
                    PipelineLogger.stageSkipped(currentStage, "enableResearch=false");
                } else if (hiringResult.isPresent()) {
                    PipelineLogger.stageCompleted(currentStage, timings.get("hiring_reentry_ms"),
                            "overallBand=" + hiringReentry.overallBand()
                                    + " score=" + hiringReentry.overallReentryScore());
                } else {
                    PipelineLogger.stageDegraded(currentStage, timings.get("hiring_reentry_ms"),
                            "status=" + hiringStatus, null);
                }
            } else {
                PipelineLogger.stageSkipped(PipelineStage.CURRENT_MARKET_CONDITION_FOR_HIRING, "side_hustle");
            }

            currentStage = PipelineStage.SCORING;
            t1 = System.currentTimeMillis();
            progress.onProgress(currentStage, "running", currentStage.defaultMessage(), null);
            PipelineLogger.stageStarted(currentStage, null);
            ScoringResult scoringResult = scoringEngine.compute(
                    activeRequest,
                    runwayMonths,
                    emergencyRunwayMonths,
                    psychologyScore,
                    marketValue,
                    research.orElse(null),
                    research.isPresent(),
                    memory.hasResume(),
                    resumeMarketValueAssessed,
                    hiringReentry);
            memory.setScoring(scoringResult);
            timings.put("scoring_ms", System.currentTimeMillis() - t1);
            progress.onProgress(currentStage, "complete",
                    "Feasibility " + scoringResult.feasibilityScore() + ", risk " + scoringResult.riskScore(), null);
            PipelineLogger.stageCompleted(currentStage, timings.get("scoring_ms"),
                    "verdict=" + scoringResult.verdict()
                            + " feasibility=" + scoringResult.feasibilityScore()
                            + " risk=" + scoringResult.riskScore()
                            + " confidence=" + scoringResult.confidence());

            currentStage = PipelineStage.OPENAI_CORE;
            t1 = System.currentTimeMillis();
            progress.onProgress(currentStage, "running", currentStage.defaultMessage(), "openai");
            PipelineLogger.stageStarted(currentStage, "providers=openai+gemini");
            NarrativeGenerationResult narrativeResult = llmReasoningService.generate(memory);
            timings.put("llm_ms", System.currentTimeMillis() - t1);
            timings.put("openai_ms", narrativeResult.openAiMs());
            timings.put("gemini_ms", narrativeResult.geminiMs());
            aiProviders.putAll(narrativeResult.providerStatus());
            progress.onProgress(currentStage, "complete", "Core narrative ready", "openai");
            logNarrativeProviders(narrativeResult, timings.get("llm_ms"));

            progress.onProgress(PipelineStage.GEMINI_DEEP, "complete",
                    narrativeResult.providerStatus().getOrDefault("gemini", "skipped"), "gemini");

            long processingMs = System.currentTimeMillis() - start;
            progress.onProgress(PipelineStage.COMPLETE, "complete", "Report ready", null);

            AnalyzeResponse response = reportComposer.compose(
                    requestId,
                    processingMs,
                    timings,
                    aiProviders,
                    scoringResult,
                    narrativeResult.narrative(),
                    research.orElse(null),
                    memory.hiringReentry(),
                    marketValue,
                    memory
            );

            PipelineLogger.analyzeCompleted(
                    requestId,
                    processingMs,
                    response.overallVerdict().name(),
                    response.feasibilityScore(),
                    response.riskScore(),
                    aiProviders,
                    timings
            );
            return response;
        } catch (Exception e) {
            PipelineLogger.analyzeFailed(requestId, currentStage, e);
            throw e;
        }
    }

    private void logNarrativeProviders(NarrativeGenerationResult narrativeResult, long llmMs) {
        Map<String, String> status = narrativeResult.providerStatus();
        String openai = status.getOrDefault("openai", "unknown");
        String gemini = status.getOrDefault("gemini", "unknown");
        if ("success".equals(openai) && ("success".equals(gemini) || gemini.startsWith("skipped"))) {
            PipelineLogger.stageCompleted(PipelineStage.OPENAI_CORE, llmMs,
                    "openai=" + openai + " gemini=" + gemini
                            + " openaiMs=" + narrativeResult.openAiMs()
                            + " geminiMs=" + narrativeResult.geminiMs());
        } else {
            PipelineLogger.stageDegraded(PipelineStage.OPENAI_CORE, llmMs,
                    "openai=" + openai + " gemini=" + gemini
                            + " openaiMs=" + narrativeResult.openAiMs()
                            + " geminiMs=" + narrativeResult.geminiMs(),
                    null);
        }
    }

    private static String summarizeProfileSources(AnalysisPipelineMemory memory) {
        if (memory.profileMergeResult() == null) {
            return "formOnly";
        }
        return "sources=" + memory.profileMergeResult().fieldSources();
    }

    private AnalyzeRequest resolveProfile(AnalyzeRequest request,
                                          AnalysisPipelineMemory memory,
                                          PipelineProgressListener progress,
                                          Map<String, Long> timings,
                                          Map<String, String> aiProviders) {
        if (!memory.hasResume()) {
            profileCompletenessValidator.requireComplete(request.profile());
            PipelineLogger.stageSkipped(PipelineStage.RESUME_PROFILE, "no resume PDF");
            return request;
        }

        PipelineStage stage = PipelineStage.RESUME_PROFILE;
        long t0 = System.currentTimeMillis();
        progress.onProgress(stage, "running", stage.defaultMessage(), "openai");
        PipelineLogger.stageStarted(stage, "resumeChars=" + memory.resumeText().length());

        Optional<ResumeProfileExtraction> parsed = resumeProfileParserService.parse(memory.resumeText());
        parsed.ifPresent(memory::setResumeProfileExtraction);

        ResumeProfileExtraction fromResume = parsed.orElse(emptyExtraction());
        var mergeResult = profileMergeService.merge(request.profile(), request.planB(), fromResume);
        memory.setProfileMergeResult(mergeResult);

        long duration = System.currentTimeMillis() - t0;
        timings.put("resume_profile_ms", duration);
        String profileStatus = parsed.isPresent() ? "success" :
                (resumeProfileParserService.isConfigured() ? "failed" : "not_configured");
        aiProviders.put("openai_resume_profile", profileStatus);
        progress.onProgress(stage, "complete",
                "Profile: " + mergeResult.mergedProfile().currentProfession(), "openai");

        if (parsed.isPresent()) {
            PipelineLogger.stageCompleted(stage, duration,
                    "profession=" + mergeResult.mergedProfile().currentProfession()
                            + " sources=" + mergeResult.fieldSources());
        } else if (!profileCompletenessValidator.isComplete(mergeResult.mergedProfile())) {
            throw resumeProfileExtractionFailed(profileStatus);
        } else {
            PipelineLogger.stageDegraded(stage, duration,
                    "status=" + profileStatus + " mergedFromFormOnly=" + mergeResult.fieldSources(), null);
        }

        return replaceProfileAndPlanB(request, mergeResult.mergedProfile(), mergeResult.mergedPlanB());
    }

    private static ResponseStatusException resumeProfileExtractionFailed(String profileStatus) {
        String message = "not_configured".equals(profileStatus)
                ? "Resume profile parsing is not available. Provide profile fields in the form or configure OpenAI."
                : "Could not extract profile from resume after 3 attempts. Please try again or provide profile fields in the form.";
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    private static AnalyzeRequest replaceProfileAndPlanB(AnalyzeRequest request, ProfileDto profile, PlanBDto planB) {
        return new AnalyzeRequest(
                profile,
                request.financials(),
                planB,
                request.constraints(),
                request.psychology(),
                request.researchOptions()
        );
    }

    private static ResumeProfileExtraction emptyExtraction() {
        return new ResumeProfileExtraction(null, null, null, null, null, null, null, null);
    }

    private String resolveHiringReentryStatus(boolean researchRequested, boolean success) {
        if (!researchRequested) {
            return "skipped_disabled";
        }
        if (!hiringReentryResearchService.isConfigured()) {
            return "not_configured";
        }
        return success ? "success" : "failed";
    }

    private String resolveResearchStatus(boolean requested, boolean success) {
        if (!requested) {
            return "skipped_disabled";
        }
        if (!researchService.isConfigured()) {
            return "not_configured";
        }
        return success ? "success" : "failed";
    }
}
