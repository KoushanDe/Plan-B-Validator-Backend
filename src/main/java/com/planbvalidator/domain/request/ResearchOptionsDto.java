package com.planbvalidator.domain.request;

public record ResearchOptionsDto(Boolean enableResearch) {
    public boolean isEnabled() {
        return enableResearch == null || enableResearch;
    }
}
