package com.planbvalidator.pipeline;

public enum PipelineStage {
    SANITIZE("Validating input"),
    RUNWAY("Calculating financial runway"),
    PSYCHOLOGY("Scoring risk questionnaire"),
    RESUME_PROFILE("Parsing profile from resume"),
    RESUME_MARKET_VALUE("Assessing resume and market value"),
    RESEARCH("Gathering market research from the web"),
    CURRENT_MARKET_CONDITION_FOR_HIRING("Assessing corporate re-hire market if you quit"),
    SCORING("Computing feasibility and risk scores"),
    OPENAI_CORE("Generating core recommendation"),
    GEMINI_DEEP("Writing detailed analysis"),
    COMPLETE("Analysis complete");

    private final String defaultMessage;

    PipelineStage(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
