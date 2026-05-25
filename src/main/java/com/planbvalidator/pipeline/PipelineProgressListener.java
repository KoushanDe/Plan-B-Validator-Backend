package com.planbvalidator.pipeline;

@FunctionalInterface
public interface PipelineProgressListener {

    PipelineProgressListener NOOP = (stage, status, message, provider) -> {};

    void onProgress(PipelineStage stage, String status, String message, String provider);
}
