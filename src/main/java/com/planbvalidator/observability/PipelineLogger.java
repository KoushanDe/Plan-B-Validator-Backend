package com.planbvalidator.observability;

import com.planbvalidator.pipeline.PipelineStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Structured logs for the analyze pipeline. Grep Docker logs for {@code pipeline} or filter JSON field
 * {@code logger_name=com.planbvalidator.pipeline.AnalyzePipeline}. {@code request_id} comes from MDC.
 */
public final class PipelineLogger {

    private static final Logger log = LoggerFactory.getLogger("com.planbvalidator.pipeline.AnalyzePipeline");

    private PipelineLogger() {
    }

    public static void analyzeStarted(String requestId, boolean hasResume, boolean researchEnabled, String planBTitle) {
        log.info("pipeline event=analyze_started requestId={} hasResume={} researchEnabled={} planBTitle={}",
                requestId, hasResume, researchEnabled, sanitize(planBTitle));
    }

    public static void analyzeCompleted(String requestId,
                                        long processingMs,
                                        String verdict,
                                        int feasibilityScore,
                                        int riskScore,
                                        Map<String, String> aiProviders,
                                        Map<String, Long> timings) {
        log.info("pipeline event=analyze_completed requestId={} processingMs={} verdict={} feasibility={} risk={} aiProviders={} timings={}",
                requestId, processingMs, verdict, feasibilityScore, riskScore, formatMap(aiProviders), formatMap(timings));
    }

    public static void analyzeFailed(String requestId, PipelineStage failedStage, Throwable error) {
        if (error == null) {
            log.error("pipeline event=analyze_failed requestId={} stage={}", requestId, stageName(failedStage));
        } else {
            log.error("pipeline event=analyze_failed requestId={} stage={} error={}",
                    requestId, stageName(failedStage), error.toString(), error);
        }
    }

    public static void stageStarted(PipelineStage stage, String detail) {
        MDC.put("pipeline_stage", stage.name());
        if (detail == null || detail.isBlank()) {
            log.info("pipeline event=stage_started stage={}", stage.name());
        } else {
            log.info("pipeline event=stage_started stage={} detail={}", stage.name(), detail);
        }
    }

    public static void stageCompleted(PipelineStage stage, long durationMs, String detail) {
        try {
            if (detail == null || detail.isBlank()) {
                log.info("pipeline event=stage_completed stage={} durationMs={}", stage.name(), durationMs);
            } else {
                log.info("pipeline event=stage_completed stage={} durationMs={} detail={}", stage.name(), durationMs, detail);
            }
        } finally {
            MDC.remove("pipeline_stage");
        }
    }

    public static void stageSkipped(PipelineStage stage, String reason) {
        MDC.remove("pipeline_stage");
        log.info("pipeline event=stage_skipped stage={} reason={}", stage.name(), reason);
    }

    /**
     * Step did not throw but returned empty / fallback (e.g. LLM or research unavailable).
     */
    public static void stageDegraded(PipelineStage stage, long durationMs, String reason, Throwable cause) {
        try {
            if (cause == null) {
                log.warn("pipeline event=stage_degraded stage={} durationMs={} reason={}", stage.name(), durationMs, reason);
            } else {
                log.warn("pipeline event=stage_degraded stage={} durationMs={} reason={} error={}",
                        stage.name(), durationMs, reason, cause.toString(), cause);
            }
        } finally {
            MDC.remove("pipeline_stage");
        }
    }

    private static String stageName(PipelineStage stage) {
        return stage == null ? "unknown" : stage.name();
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "(none)";
        }
        String trimmed = value.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) + "…" : trimmed;
    }

    private static String formatMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }
}
